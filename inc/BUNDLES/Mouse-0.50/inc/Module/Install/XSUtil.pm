#line 1
package Module::Install::XSUtil;

use 5.005_03;

$VERSION = '0.21';

use Module::Install::Base;
@ISA     = qw(Module::Install::Base);

use strict;

use Config;

use File::Spec;
use File::Find;

use constant _VERBOSE => $ENV{MI_VERBOSE} ? 1 : 0;

my %ConfigureRequires = (
    # currently nothing
);

my %BuildRequires = (
    'ExtUtils::ParseXS' => 2.21, # the newer, the better
);

my %Requires = (
    'XSLoader' => 0.10, # the newer, the better
);

my %ToInstall;

sub _verbose{
    print STDERR q{# }, @_, "\n";
}

sub _xs_debugging{
    return $ENV{XS_DEBUG} || scalar( grep{ $_ eq '-g' } @ARGV );
}

sub _xs_initialize{
    my($self) = @_;

    unless($self->{xsu_initialized}){
        $self->{xsu_initialized} = 1;

        if(!$self->cc_available()){
            print "This package requires a C compiler, but it's not available.\n";
            exit(0);
        }

        $self->configure_requires(%ConfigureRequires);
        $self->build_requires(%BuildRequires);
        $self->requires(%Requires);

        $self->makemaker_args(OBJECT => '$(O_FILES)');
        $self->clean_files('$(O_FILES)');

        if($self->_xs_debugging()){
            # override $Config{optimize}
            if(_is_msvc()){
                $self->makemaker_args(OPTIMIZE => '-Zi');
            }
            else{
                $self->makemaker_args(OPTIMIZE => '-g');
            }
            $self->cc_define('-DXS_ASSERT');
        }
    }
    return;
}

# GNU C Compiler
sub _is_gcc{
    return $Config{gccversion};
}

# Microsoft Visual C++ Compiler (cl.exe)
sub _is_msvc{
    return $Config{cc} =~ /\A cl \b /xmsi;
}

{
    my $cc_available;

    sub cc_available {
        return $cc_available if defined $cc_available;

        foreach my $arg(@ARGV){
            if($arg eq '--pp'){
                return $cc_available = 0;
            }
            elsif($arg eq '--xs'){
                return $cc_available = 1;
            }
        }

        return $cc_available = shift->can_cc();
    }
}

sub use_ppport{
    my($self, $dppp_version) = @_;

    $self->_xs_initialize();

    my $filename = 'ppport.h';

    $dppp_version ||= 0;
    $self->configure_requires('Devel::PPPort' => $dppp_version);

    print "Writing $filename\n";

    my $e = do{
        local $@;
        eval qq{
            use Devel::PPPort;
            Devel::PPPort::WriteFile(q{$filename});
        };
        $@;
    };
    if($e){
         print "Cannot create $filename because: $@\n";
    }

    if(-e $filename){
        $self->clean_files($filename);
        $self->cc_define('-DUSE_PPPORT');
        $self->cc_append_to_inc('.');
    }
    return;
}

sub cc_warnings{
    my($self) = @_;

    $self->_xs_initialize();

    if(_is_gcc()){
        # Note: MSVC++ doesn't support C99, so -Wdeclaration-after-statement helps ensure C89 specs.
        $self->cc_append_to_ccflags(qw(-Wall));

        no warnings 'numeric';
        if($Config{gccversion} >= 4.0){
            $self->cc_append_to_ccflags('-Wextra -Wdeclaration-after-statement');
            if($Config{gccversion} >= 4.1){
                $self->cc_append_to_ccflags('-Wc++-compat');
            }
        }
        else{
            $self->cc_append_to_ccflags('-W -Wno-comment');
        }
    }
    elsif(_is_msvc()){
        $self->cc_append_to_ccflags('-W3');
    }
    else{
        # TODO: support other compilers
    }

    return;
}


sub cc_append_to_inc{
    my($self, @dirs) = @_;

    $self->_xs_initialize();

    for my $dir(@dirs){
        unless(-d $dir){
            warn("'$dir' not found: $!\n");
            exit;
        }

        _verbose "inc: -I$dir" if _VERBOSE;
    }

    my $mm    = $self->makemaker_args;
    my $paths = join q{ }, map{ s{\\}{\\\\}g; qq{"-I$_"} } @dirs;

    if($mm->{INC}){
        $mm->{INC} .=  q{ } . $paths;
    }
    else{
        $mm->{INC}  = $paths;
    }
    return;
}

sub cc_append_to_libs{
    my($self, @libs) = @_;

    $self->_xs_initialize();

    my $mm = $self->makemaker_args;

    my $libs = join q{ }, map{
        my($name, $dir) = ref($_) eq 'ARRAY' ? @{$_} : ($_, undef);

        $dir = qq{-L$dir } if defined $dir;
        _verbose "libs: $dir-l$name" if _VERBOSE;
        $dir . qq{-l$name};
    } @libs;

    if($mm->{LIBS}){
        $mm->{LIBS} .= q{ } . $libs;
    }
    else{
        $mm->{LIBS} = $libs;
    }

    return;
}

sub cc_append_to_ccflags{
    my($self, @ccflags) = @_;

    $self->_xs_initialize();

    my $mm    = $self->makemaker_args;

    $mm->{CCFLAGS} ||= $Config{ccflags};
    $mm->{CCFLAGS}  .= q{ } . join q{ }, @ccflags;
    return;
}

sub cc_define{
    my($self, @defines) = @_;

    $self->_xs_initialize();

    my $mm = $self->makemaker_args;
    if(exists $mm->{DEFINE}){
        $mm->{DEFINE} .= q{ } . join q{ }, @defines;
    }
    else{
        $mm->{DEFINE}  = join q{ }, @defines;
    }
    return;
}

sub requires_xs{
    my $self  = shift;

    return $self->requires() unless @_;

    $self->_xs_initialize();

    my %added = $self->requires(@_);
    my(@inc, @libs);

    my $rx_lib    = qr{ \. (?: lib | a) \z}xmsi;
    my $rx_dll    = qr{ \. dll          \z}xmsi; # for Cygwin

    while(my $module = each %added){
        my $mod_basedir = File::Spec->join(split /::/, $module);
        my $rx_header = qr{\A ( .+ \Q$mod_basedir\E ) .+ \. h(?:pp)?     \z}xmsi;

        SCAN_INC: foreach my $inc_dir(@INC){
            my @dirs = grep{ -e } File::Spec->join($inc_dir, 'auto', $mod_basedir), File::Spec->join($inc_dir, $mod_basedir);

            next SCAN_INC unless @dirs;

            my $n_inc = scalar @inc;
            find(sub{
                if(my($incdir) = $File::Find::name =~ $rx_header){
                    push @inc, $incdir;
                }
                elsif($File::Find::name =~ $rx_lib){
                    my($libname) = $_ =~ /\A (?:lib)? (\w+) /xmsi;
                    push @libs, [$libname, $File::Find::dir];
                }
                elsif($File::Find::name =~ $rx_dll){
                    # XXX: hack for Cygwin
                    my $mm = $self->makemaker_args;
                    $mm->{macro}->{PERL_ARCHIVE_AFTER} ||= '';
                    $mm->{macro}->{PERL_ARCHIVE_AFTER}  .= ' ' . $File::Find::name;
                }
            }, @dirs);

            if($n_inc != scalar @inc){
                last SCAN_INC;
            }
        }
    }

    my %uniq = ();
    $self->cc_append_to_inc (grep{ !$uniq{ $_ }++ } @inc);

    %uniq = ();
    $self->cc_append_to_libs(grep{ !$uniq{ $_->[0] }++ } @libs);

    return %added;
}

sub cc_src_paths{
    my($self, @dirs) = @_;

    $self->_xs_initialize();

    return unless @dirs;

    my $mm     = $self->makemaker_args;

    my $XS_ref = $mm->{XS} ||= {};
    my $C_ref  = $mm->{C}  ||= [];

    my $_obj   = $Config{_o};

    my @src_files;
    find(sub{
        if(/ \. (?: xs | c (?: c | pp | xx )? ) \z/xmsi){ # *.{xs, c, cc, cpp, cxx}
            push @src_files, $File::Find::name;
        }
    }, @dirs);

    foreach my $src_file(@src_files){
        my $c = $src_file;
        if($c =~ s/ \.xs \z/.c/xms){
            $XS_ref->{$src_file} = $c;

            _verbose "xs: $src_file" if _VERBOSE;
        }
        else{
            _verbose "c: $c" if _VERBOSE;
        }

        push @{$C_ref}, $c unless grep{ $_ eq $c } @{$C_ref};
    }

    $self->cc_append_to_inc('.');

    return;
}

sub cc_include_paths{
    my($self, @dirs) = @_;

    $self->_xs_initialize();

    push @{ $self->{xsu_include_paths} ||= []}, @dirs;

    my $h_map = $self->{xsu_header_map} ||= {};

    foreach my $dir(@dirs){
        my $prefix = quotemeta( File::Spec->catfile($dir, '') );
        find(sub{
            return unless / \.h(?:pp)? \z/xms;

            (my $h_file = $File::Find::name) =~ s/ \A $prefix //xms;
            $h_map->{$h_file} = $File::Find::name;
        }, $dir);
    }

    $self->cc_append_to_inc(@dirs);

    return;
}

sub install_headers{
    my $self    = shift;
    my $h_files;
    if(@_ == 0){
        $h_files = $self->{xsu_header_map} or die "install_headers: cc_include_paths not specified.\n";
    }
    elsif(@_ == 1 && ref($_[0]) eq 'HASH'){
        $h_files = $_[0];
    }
    else{
        $h_files = +{ map{ $_ => undef } @_ };
    }

    $self->_xs_initialize();

    my @not_found;
    my $h_map = $self->{xsu_header_map} || {};

    while(my($ident, $path) = each %{$h_files}){
        $path ||= $h_map->{$ident} || File::Spec->join('.', $ident);
        $path   = File::Spec->canonpath($path);

        unless($path && -e $path){
            push @not_found, $ident;
            next;
        }

        $ToInstall{$path} = File::Spec->join('$(INST_ARCHAUTODIR)', $ident);

        _verbose "install: $path as $ident" if _VERBOSE;
        $self->_extract_functions_from_header_file($path);
    }

    if(@not_found){
        die "Header file(s) not found: @not_found\n";
    }

    return;
}

my $home_directory;

sub _extract_functions_from_header_file{
    my($self, $h_file) = @_;

    my @functions;

    ($home_directory) = <~> unless defined $home_directory;

    # get header file contents through cpp(1)
    my $contents = do {
        my $mm = $self->makemaker_args;

        my $cppflags = q{"-I}. File::Spec->join($Config{archlib}, 'CORE') . q{"};
        $cppflags    =~ s/~/$home_directory/g;

        $cppflags   .= ' ' . $mm->{INC} if $mm->{INC};

        $cppflags   .= ' ' . ($mm->{CCFLAGS} || $Config{ccflags});
        $cppflags   .= ' ' . $mm->{DEFINE} if $mm->{DEFINE};

        my $add_include = _is_msvc() ? '-FI' : '-include';
        $cppflags   .= ' ' . join ' ', map{ qq{$add_include "$_"} } qw(EXTERN.h perl.h XSUB.h);

        my $cppcmd = qq{$Config{cpprun} $cppflags $h_file};

        _verbose("extract functions from: $cppcmd") if _VERBOSE;
        `$cppcmd`;
    };

    unless(defined $contents){
        die "Cannot call C pre-processor ($Config{cpprun}): $! ($?)";
    }

    # remove other include file contents
    my $chfile = q/\# (?:line)? \s+ \d+ /;
    $contents =~ s{
        ^$chfile  \s+ (?!"\Q$h_file\E")
        .*?
        ^(?= $chfile)
    }{}xmsig;

    if(_VERBOSE){
        local *H;
        open H, "> $h_file.out"
            and print H $contents
            and close H;
    }

    while($contents =~ m{
            ([^\\;\s]+                # type
            \s+
            ([a-zA-Z_][a-zA-Z0-9_]*)  # function name
            \s*
            \( [^;#]* \)              # argument list
            [\w\s\(\)]*               # attributes or something
            ;)                        # end of declaration
        }xmsg){
            my $decl = $1;
            my $name = $2;

            next if $decl =~ /\b typedef \b/xms;
            next if $name =~ /^_/xms; # skip something private

            push @functions, $name;

            if(_VERBOSE){
                $decl =~ tr/\n\r\t / /s;
                $decl =~ s/ (\Q$name\E) /<$name>/xms;
                _verbose("decl: $decl");
            }
    }

    if(@functions){
        $self->cc_append_to_funclist(@functions);
    }

    return;
}


sub cc_append_to_funclist{
    my($self, @functions) = @_;

    $self->_xs_initialize();

    my $mm = $self->makemaker_args;

    push @{$mm->{FUNCLIST} ||= []}, @functions;
    $mm->{DL_FUNCS} ||= { '$(NAME)' => [] };

    return;
}


package
    MY;

# XXX: We must append to PM inside ExtUtils::MakeMaker->new().
sub init_PM{
    my $self = shift;

    $self->SUPER::init_PM(@_);

    while(my($k, $v) = each %ToInstall){
        $self->{PM}{$k} = $v;
    }
    return;
}

# append object file names to CCCMD
sub const_cccmd {
    my $self = shift;

    my $cccmd  = $self->SUPER::const_cccmd(@_);
    return q{} unless $cccmd;

    if (Module::Install::XSUtil::_is_msvc()){
        $cccmd .= ' -Fo$@';
    }
    else {
        $cccmd .= ' -o $@';
    }

    return $cccmd
}

1;
__END__

#line 688
