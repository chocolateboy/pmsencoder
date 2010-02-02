package App::PS3MEncoder;

use 5.008001;

use constant PS3MENCODER => 'ps3mencoder';
use constant {
    DISTRO             => 'App-PS3MEncoder',
    MENCODER_EXE       => 'mencoder.exe',
    PS3MENCODER_CONFIG => PS3MENCODER . '.yml',
    PS3MENCODER_EXE    => PS3MENCODER . '.exe',
    PS3MENCODER_LOG    => PS3MENCODER . '.log',
};

# core modules
use File::Spec;
use IPC::Cmd 0.46 qw(can_run); # core since 5.10.0, but we need a version that escapes shell arguments correctly
use POSIX qw(strftime);

# CPAN modules
use File::HomeDir; # technically, this is not always used, but try to keep Cava happy
use IO::All;
use List::MoreUtils qw(first_index any);
use Method::Signatures::Simple;
use Mouse; # includes strict and warnings
use Path::Class qw(file dir);
use YAML::XS qw(Load);

# use LWP::Simple qw(get head); # XXX not always needed - make sure Cava picks this up
# use File::ShareDir;           # XXX not always needed - make sure Cava picks this up

our $VERSION = '0.50'; # PS3MEncoder version: logged to aid diagnostics

# arguments passed in @ARGV after any ps3mencoder-specific processing
has args => (
    is         => 'rw',
    isa        => 'ArrayRef',
    auto_deref => 1,
    required   => 1,
);

# valid extensions for the ps3mencoder config file
has config_file_ext => (
    is         => 'ro',
    isa        => 'ArrayRef',
    auto_deref => 1,
    default    => sub { [ qw(conf yml yaml) ] },
);

# full path to the config file - an exception is thrown if one can't be found
has config_file_path => (
    is      => 'ro',
    isa     => 'Str',
    lazy    => 1,
    builder => '_build_config_file_path',
);

# the YAML config file as a hash ref
has config => (
    is  => 'rw',
    isa => 'HashRef'
);

# the path to the default config file - used as a fallback if no custom config file is found
has default_config_path => (
    is  => 'rw',
    isa => 'Str'
);

# document cache for exec_get
has document => (
    is      => 'ro',
    isa     => 'HashRef',
    default => sub { {} },
);

# full logfile path
has logfile_path => (
    is      => 'rw',
    isa     => 'Str',
);

# IO::All logfile handle
has logfile => (
    is  => 'rw',
    # isa => 'IO::All::File',
    # XXX Mouse no likey
);

# full path to this executable
has self_path => (
    is      => 'ro',
    isa     => 'Str',
    default => $0
);

# symbol table containing user-defined variables and named captures
has stash => (
    is      => 'ro',
    isa     => 'HashRef',
    default => sub { {} },
);

# is this running on Windows?
has mswin => (
    is      => 'ro',
    isa     => 'Bool',
    default => ($^O eq 'MSWin32'),
);

# full path to mencoder binary
has mencoder_path => (
    is      => 'ro',
    isa     => 'Str',
    lazy    => 1,
    builder => '_build_mencoder_path',
);

# position in args of the filename/URI
has uri_index => (
    is      => 'rw',
    isa     => 'Int',
    default => 0,
);

# full path to the dir searched for the user's config file
has user_config_dir => (
    is      => 'rw',
    isa     => 'Str',
);

# add support for a :Raw attribute used to indicate that a handler method 
# bound to "operators" in the config file should have its argument passed unprocessed
# (normally hash and array elements are passed piecewise). Actually, we can use this to handle
# any attributes, but only :Raw is currently used. 
# this is untidy (Attributes::Storage looks nicer), but attributes.pm is in core (from 5.6),
# so it's one less dependency to worry about

{
    my %ATTRIBUTES;

    # this has to be a sub i.e. compile-time (Method::Signatures::Simple's methods are declared at runtime)
    sub MODIFY_CODE_ATTRIBUTES {
        my ($pkg, $code, @attrs) = @_;
        $ATTRIBUTES{$code} = [ @attrs ];
        return (); # return any attributes we don't handle - none in this case
    }

    # by using %ATTRIBUTES directly we can bypass attributes::get and FETCH_CODE_ATTRIBUTES
    method has_attribute($code, $attribute) {
        any { $_ eq $attribute } @{ $ATTRIBUTES{$code} || [] };
    }
}

method BUILD {
    # my $logfile_path = $self->logfile_path(file(File::Spec->tmpdir, PS3MENCODER_LOG)->stringify());
    # work around strange interaction with Path::Class::File::stringify:
    # https://rt.cpan.org/Ticket/Display.html?id=54203

    $self->logfile_path(file(File::Spec->tmpdir, PS3MENCODER_LOG)->stringify);

    my $logfile_path = $self->logfile_path();

    $self->logfile(io($logfile_path));
    $self->logfile->append($/) if (-s $logfile_path);
    $self->debug("PS3MEncoder $VERSION ($^O)");

    # on Win32, it might make sense for the config file to be in $PMS_HOME, typically C:\Program Files\PS3 Media Server
    # Unfortunately, PMS' registry entries are currently broken, so we can't rely on them (e.g. we
    # can't use Win32::TieRegistry):
    #
    #     http://code.google.com/p/ps3mediaserver/issues/detail?id=555
    #
    # Instead we bundle the default config file (and mencoder.exe) in $PS3MENCODER_HOME/res
    # and ensure they're picked up with the appropriate precedence by setting the former via
    # $self->default_config_path() and the latter via the $MENCODER_PATH environment variable
    
    my $data_dir = File::HomeDir->my_data;

    if ($self->mswin) {
        eval 'use Cava::Pack';
        $self->fatal("can't load Cava::Pack: $@") if ($@);
        Cava::Pack::SetResourcePath('res');
        $self->default_config_path(Cava::Pack::Resource(PS3MENCODER_CONFIG));
        $ENV{MENCODER_PATH} ||= Cava::Pack::Resource(MENCODER_EXE);
        $self->self_path(file(Cava::Pack::Resource(''), File::Spec->updir, PS3MENCODER_EXE)->absolute->stringify);
        $self->user_config_dir(dir($data_dir, PS3MENCODER)->stringify);
    } else {
        require File::ShareDir; # no need to worry about this not being picked up by Cava as it's non-Windows only
        $self->default_config_path(File::ShareDir::dist_file(DISTRO, PS3MENCODER_CONFIG)); 
        $self->user_config_dir(dir($data_dir, '.' . PS3MENCODER)->stringify);
    }

    my @args = $self->args();
    $self->debug($self->self_path . (@args ? " @args" : ''));

    unless ($self->isdef('-prefer-ipv4')) { # uri_index defaults to 0
        $self->uri_index(4); # hardwired in net.pms.encoders.MEncoderVideo.launchTranscode
    }

    # XXX at the moment, all options (--help, --test, and the implicit run option) need the config,
    # so it may as well be initialised here
    $self->config($self->process_config); # load the config and process matching profiles

    return $self;
}

# dump various config settings - useful for troubleshooting
method help {
    my $user_config_dir = $self->user_config_dir || '<undef>'; # may be undef according to the File::HomeDir docs

    print STDOUT
         PS3MENCODER, ":           $VERSION ($^O)", $/,
        'config file version:   ', $self->config->{version}, $/, # sanity-checked by process_config
        'config file:           ', $self->config_file_path(), $/,
        'default config file:   ', $self->default_config_path(), $/,
        'logfile:               ', $self->logfile_path(), $/,
        'mencoder path:         ', $self->mencoder_path(), $/,
        'user config directory: ', $user_config_dir, $/,
}

method _build_mencoder_path {
    $self->config->{mencoder_path} || $ENV{MENCODER_PATH} || can_run('mencoder') || $self->fatal("can't find mencoder");
}

method _build_config_file_path {
    # first: check the environment variable (should contain the absolute path)
    if (exists $ENV{PS3MENCODER_CONFIG}) {
        my $config_file_path = $ENV{PS3MENCODER_CONFIG};

        if (-f $config_file_path) {
            return $config_file_path;
        } else {
            $self->fatal("invalid PS3MENCODER_CONFIG environment variable ($config_file_path): file not found"); 
        }
    }

    # second: search for it in the user's home directory e.g. ~/.ps3mencoder/ps3mencoder.yml
    my $user_config_dir = $self->user_config_dir();
    if (defined $user_config_dir) { # not guaranteed to be defined
        for my $ext ($self->config_file_ext) {
            my $config_file_path = file($user_config_dir, "ps3mencoder.$ext");
            return $config_file_path if (-f $config_file_path);
        }
    } else {
        $self->debug("can't find user config dir"); # should usually be defined; worth noting if it's not
    }

    # finally, fall back on the config file installed with the distro - this should always be available
    my $default = $self->default_config_path() || $self->fatal("can't find default configuration file");
    if (-f $default) {
        return $default;
    } else { # XXX shouldn't happen
        $self->fatal("can't find default config file: $default");
    }
}

method debug($message) {
    my $now = strftime("%Y-%m-%d %H:%M:%S", localtime);
    $self->logfile->append("$now: $$: $message", $/);
}

method fatal ($message) {
    $self->debug("ERROR: $message");
    die $self->self_path . ": $VERSION: $$: ERROR: $message", $/;
}

method isdef ($name) {
    my $index = first_index { $_ eq $name } $self->args;
    return ($index != -1);
}

method isopt($arg) {
    return (defined($arg) && (substr($arg, 0, 1) eq '-'));
}

method run {
    # we look for mencoder in these places (in desceneding order of priority):
    #
    # 1) mencoder_path in the config file
    # 2) the path indicated by the environment variable $MENCODER_PATH
    # 3) the current working directory (prepended to the search path by IPC::Cmd::can_run)
    # 4) $PATH (via IPC::Cmd::can_run)

    my $mencoder = $self->mencoder_path();
    my @args = $self->args();

    $self->debug("exec: $mencoder" . (@args ? " @args" : ''));

    # IPC::Cmd's use of IPC::Run is broken: https://rt.cpan.org/Ticket/Display.html?id=54184
    $IPC::Cmd::USE_IPC_RUN = 0;

    my ($ok, $err) = IPC::Cmd::run(
        command => [ $mencoder, @args ],
        verbose => 1
    );

    if ($ok) {
        $self->debug('ok');
    } else {
        $self->fatal("can't exec mencoder: $err");
    }

    exit 0;
}

method process_config {
    my $uri = $self->args->[ $self->uri_index ];
    my $config_file = $self->config_file_path();

    # XXX Try::Tiny?
    $self->debug("loading config: $config_file");
    my $yaml = eval { io($config_file)->slurp() };
    $self->fatal("can't open config: $@") if ($@);
    my $config = eval { Load($yaml) };
    $self->fatal("can't load config: $@") if ($@);

    if ($config) {
        # FIXME: this blindly assumes the config file is sane for the most part
        # XXX use Kwalify?

        my $version = $config->{version};

        $self->fatal("no version found in the config file") unless (defined $version);
        $self->debug("config file version: $version");

        if (defined $uri) {
            my $profiles = $config->{profiles};

            if ($profiles) {
                for my $profile (@$profiles) {
                    my $profile_name = $profile->{name};
                    my $match        = $profile->{match};

                    if (defined($match) && ($uri =~ $match)) {
                        $self->debug("matched profile: $profile_name");

                        # merge and log any named captures
                        while (my ($named_capture_key, $named_capture_value) = each (%+)) {
                            $self->exec_let($named_capture_key, $named_capture_value);
                        }

                        my $options = $profile->{options};
                        $options = [ $options ] unless (ref($options) eq 'ARRAY');

                        for my $hash (@$options) {
                            while (my ($key, $value) = each(%$hash)) {
                                my $operator = __PACKAGE__->can("exec_$key");
                                $self->fatal("invalid operator: $key") unless ($operator);

                                if (ref($value) && not($self->has_attribute($operator, 'Raw'))) {
                                    if ((ref $value) eq 'HASH') {
                                        while (my($k, $v) = each (%$value)) {
                                            $operator->($self, $k, $v);
                                        }
                                    } else {
                                        for my $v (@$value) {
                                            $operator->($self, $v);
                                        }
                                    }
                                } elsif (defined $value) {
                                    $operator->($self, $value);
                                } else {
                                    $operator->($self);
                                }
                            }
                        }
                    }
                }
            } else {
                $self->debug('no profiles defined');
            }
        } else {
            $self->debug('no URI defined');
        }
    } else {
        $self->fatal("can't load config from $config_file");
    }

    return $config;
}

################################# MEncoder Options ################################

# extract the media URI - see http://stackoverflow.com/questions/1883737/getting-an-flv-from-youtube-in-net
method exec_youtube ($formats) :Raw {
    my $uri = $self->args->[ $self->uri_index ];
    my $stash = $self->stash;
    my ($video_id, $t) = @{$stash}{qw(video_id t)};
    my $found = 0;

    # via http://www.longtailvideo.com/support/forum/General-Chat/16851/Youtube-blocked-http-youtube-com-get-video
    #
    # No &fmt = FLV (very low)
    # &fmt=5  = FLV (very low)
    # &fmt=6  = FLV (doesn't always work)
    # &fmt=13 = 3GP (mobile phone)
    # &fmt=18 = MP4 (normal)
    # &fmt=22 = MP4 (hd)
    #
    # see also:
    #
    #     http://tinyurl.com/y8rdcoy
    #     http://userscripts.org/topics/18274

    for my $fmt (@$formats) {
        require LWP::Simple;
        my $media_uri = "http://www.youtube.com/get_video?fmt=$fmt&video_id=$video_id&t=$t";
        next unless (LWP::Simple::head $media_uri);
        $self->exec_uri($media_uri); # set the new URI
        $found = 1;
        last;
    }

    $self->fatal("can't retrieve YouTube video from $uri") unless ($found);
}

method exec_set ($name, $value) {
    $name = "-$name";

    my $args = $self->args;
    my $index = first_index { $_ eq $name } @$args;

    if ($index == -1) {
        if (defined $value) {
            $self->debug("adding $name $value");
            push @$args, $name, $value; # FIXME: encapsulate
        } else {
            $self->debug("adding $name");
            push @$args, $name; # FIXME: encapsulate
        }
    } elsif (defined $value) {
        $self->debug("setting $name to $value");
        $args->[ $index + 1 ] = $value; # FIXME: encapsulate
    }
}

method exec_replace ($name, $search, $replace) {
    $name = "-$name";

    my $args = $self->args();
    my $index = first_index { $_ eq $name } @$args;

    if ($index != -1) {
        $self->debug("replacing $search with $replace in $name");
        $args->[ $index + 1 ] =~ s{$search}{$replace}; # FIXME: encapsulate
    }
}

method exec_remove ($name) {
    $name = "-$name";

    my @args = $self->args;
    my $nargs = @args;
    my @keep;

    while (@args) {
        my $arg = shift @args;

        if ($self->isopt($arg)) { # -foo ...
            if (@args && not($self->isopt($args[0]))) { # -foo bar
                my $value = shift @args;

                if ($arg ne $name) {
                    push @keep, $arg, $value;
                }
            } elsif ($arg ne $name) { # just -foo
                push @keep, $arg;
            }
        } else {
            push @keep, $arg;
        }
    }

    if (@keep < $nargs) {
        $self->debug("removing $name");
        $self->args(\@keep);
    }
}

# define a variable in the stash
method exec_let ($name, $value) {
    $self->debug("setting \$$name to $value");
    $self->stash->{$name} = $value;
}

# define a variable in the stash by extracting a value from the document pointed to by the current URI
method exec_get ($key, $value) {
    my $uri = $self->args->[ $self->uri_index ]; # XXX need a uri attribute that does the right thing(s)
    my $document = do { # cache for subsequent matches
        unless (exists $self->document->{$uri}) {
            require LWP::Simple;
            $self->document->{$uri} = LWP::Simple::get($uri) || $self->fatal("can't retrieve $uri");
        }
        $self->document->{$uri};
    };

    # key: 'value (capture_me)'
    if (defined $value) {
        $self->debug("extracting \$$key from $uri");
        my ($extract) = $document =~ /$value/;
        $self->exec_let($key, $extract);
    } else {
        $document =~ /$key/;
        while (my ($named_capture_key, $named_capture_value) = (each %+)) {
            $self->exec_let($named_capture_key, $named_capture_value);
        }
    }
}

# set the URI, performing any variable substitutions
method exec_uri ($uri) {
    while (my ($key, $value) = each (%{ $self->stash })) {
        my $search = qr{(?:(?:\$$key\b)|(?:\$\{$key\}))};
        if ($uri =~ $search) {
            $self->debug("replacing \$$key with '$value' in $uri");
            $uri =~ s{$search}{$value}g;
        }
    }

    $self->args->[ $self->uri_index ] = $uri;
}

1;

__END__

=head1 NAME

App::PS3MEncoder - MEncoder wrapper for PS3 Media Server

=head1 SYNOPSIS

my $ps3mencoder = App::PS3MEncoder->new({ args => \@ARGV });

$ps3mencoder->run();

=head1 DESCRIPTION

This is a helper script for PS3 Media Server that restores support for Web video streaming via mencoder.

See here for more details: http://github.com/chocolateboy/ps3mencoder

=head1 AUTHOR

chocolateboy <chocolate@cpan.org>

=head1 SEE ALSO

=over

=item * L<FFmpeg|FFmpeg>

=back

=head1 VERSION

0.50

=cut
