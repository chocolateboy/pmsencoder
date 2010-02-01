use 5.008001; # Moose minimum
use MooseX::Declare;

class App::PS3MEncoder {
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
    use IPC::Run; # this is optional, but let's try to be consistent across all platforms
    use List::MoreUtils qw(first_index);
    use Path::Class qw(file dir);
    # use LWP::Simple qw(get head); # XXX not always needed - make sure Cava picks this up
    use YAML::XS qw(Load);

    our $VERSION = '0.50'; # PS3MEncoder version: logged to aid diagnostics

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
        isa => 'IO::All::File',
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

    # position in @ARGV of the filename/URI
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

    method BUILD {
        my $logfile_path = $self->logfile_path(file(File::Spec->tmpdir, PS3MENCODER_LOG)->stringify);

        $self->logfile(io($logfile_path));
        $self->logfile->append($/) if (-s $logfile_path);
        $self->debug("PS3MEncoder $VERSION");

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
            $self->self_path(file(Cava::Pack::Resource(''), File::Spec->updir, PS3MENCODER_EXE)->absolute);
            $self->user_config_dir(dir($data_dir, PS3MENCODER)->stringify);
        } else {
            require File::ShareDir; # no need to worry about this not being picked up by Cava as it's non-Windows only
            $self->default_config_path(File::ShareDir::dist_file(DISTRO, PS3MENCODER_CONFIG)); 
            $self->user_config_dir(dir($data_dir, '.' . PS3MENCODER)->stringify);
        }

        $self->debug($self->self_path . (@ARGV ? " @ARGV" : ''));

        unless ($self->isdef('-prefer-ipv4')) { # $URI_INDEX defaults to 0
            $self->uri_index(4); # hardwired in net.pms.encoders.MEncoderVideo.launchTranscode
        }

        $self->config($self->process_config); # load the config and process matching profiles

        return $self;
    }

    # show the temp file path and the home directory path
    method help {
        my $user_config_dir = $self->user_config_dir || '<undef>'; # may be undef

        print STDOUT
             PS3MENCODER, ":           $VERSION", $/,
            'config file version:   ', eval { $self->config->{version} } || '<undef>', $/,
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
        if (exists $ENV{PS3MENCODER_CONF}) {
            my $config_file_path = $ENV{PS3MENCODER_CONF};
            if (-f $config_file_path) {
                return $config_file_path;
            } else {
                $self->fatal("invalid PS3MENCODER_CONF environment variable ($config_file_path): file not found"); 
            }
        }

        # second: search for it in the user's home directory e.g. ~/.ps3mencoder/ps3mencoder.yml
        my $user_config_dir = $self->user_config_dir();
        if ($user_config_dir) { # not guaranteed to be defined
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
            $self->fatal("invalid path for default config file: $default");
        }
    }

    method debug(Str $message) {
        my $now = strftime("%Y-%m-%d %H:%M:%S", localtime);
        $self->logfile->append("$now: $$: $message", $/);
    }

    method fatal (Str $message) {
        $self->debug("ERROR: $message");
        die $self->self_path . ": $VERSION: $$: ERROR: $message", $/;
    }

    method isdef (Str $name) {
        my $index = first_index { $_ eq $name } @ARGV;
        return ($index != -1);
    }

    method isopt(Str $arg) {
        return (defined($arg) && (substr($arg, 0, 1) eq '-'));
    }

    method run {
        # we look for mencoder in these places (in desceneding order of priority):
        #
        # 1) mencoder_path specified in the config file
        # 2) the path indicated by the environment variable $MENCODER_PATH
        # 3) the current working directory (prepended to the search path by IPC::Cmd::can_run)
        # 4) $PATH (via IPC::Cmd::can_run)

        my $mencoder = $self->mencoder_path();

        $self->debug("exec: $mencoder" . (@ARGV ? " @ARGV" : ''));

        $IPC::Cmd::USE_IPC_RUN = 1;

        my ($ok, $err) = IPC::Cmd::run(
            command => [ $mencoder, @ARGV ],
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
        my $uri = $ARGV[ $self->uri_index ];
        my $config_file = $self->config_file_path();

        $self->debug("loading config: $config_file");
        my $yaml = eval { io($config_file)->slurp() };
        $self->fatal("can't open config: $@") if ($@);
        my $config = eval { Load($yaml) };
        $self->fatal("can't load config: $@") if ($@);

        if ($config) {
            # FIXME: this blindly assumes the config file is sane at the moment
            # XXX use Kwalify?

            $self->debug("config file version: " . ($config->{version} || '<undef>'));

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

                                    if (ref $value) {
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
    method exec_youtube (ArrayRef $formats) {
        my $uri = $ARGV[ $self->uri_index ];
        my $stash = $self->stash;
        my ($id, $signature) = @{$stash}{qw(id signature)};
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
            my $media_uri = "http://www.youtube.com/get_video?fmt=$fmt&video_id=$id&t=$signature";
            next unless (LWP::Simple::head $media_uri);
            $self->exec_uri($media_uri);
            $found = 1;
            last;
        }

        $self->fatal("can't retrieve YouTube video from $uri") unless ($found);
    }

    method exec_set (Str $name, Str $value?) {
        $name = "-$name";

        my $index = first_index { $_ eq $name } @ARGV;

        if ($index == -1) {
            if (defined $value) {
                $self->debug("adding $name $value");
                push @ARGV, $name, $value;
            } else {
                $self->debug("adding $name");
                push @ARGV, $name;
            }
        } elsif (defined $value) {
            $self->debug("setting $name to $value");
            $ARGV[ $index + 1 ] = $value;
        }
    }

    method exec_replace (Str $name, Str $search, Str $replace) {
        $name = "-$name";

        my $index = first_index { $_ eq $name } @ARGV;

        if ($index != -1) {
            $self->debug("replacing $search with $replace in $name");
            $ARGV[ $index + 1 ] =~ s{$search}{$replace};
        }
    }

    method exec_remove (Str $name) {
        $name = "-$name";

        my @argv = @ARGV;
        my @keep;

        while (@argv) {
            my $arg = shift @argv;

            if ($self->isopt($arg)) { # -foo ...
                if (@argv && not($self->isopt($argv[0]))) { # -foo bar
                    my $value = shift @argv;

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

        if (@keep < @ARGV) {
            $self->debug("removing $name");
            @ARGV = @keep;
        }
    }

    # define a variable in the stash
    method exec_let (Str $name, Str $value) {
        $self->debug("setting \$$name to $value");
        $self->stash->{$name} = $value;
    }

    # define a variable in the stash by extracting a value from the document pointed to by the current URI
    method exec_get (Str $key, Str $value?) {
        my $uri = $ARGV[ $self->uri_index ];
        my $document = do {
            unless (exists $self->document->{$uri}) {
		require LWP::Simple;
                $self->document->{$uri} = LWP::Simple::get($uri) || $self->fatal("can't retrieve $uri");
            }
            $self->document->{$uri};
        };

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
    method exec_uri (Str $uri) {
        while (my ($key, $value) = each (%{ $self->stash })) {
            if ($uri =~ /\$$key\b/) {
                $self->debug("replacing \$$key with '$value' in $uri");
                $uri =~ s{(?:(?:\$$key\b)|(?:\$\{$key\}))}{$value}g;
            }
        }

        $ARGV[ $self->uri_index ] = $uri;
    }
}

1;

__END__

=head1 NAME

App::PS3MEncoder - MEncoder wrapper for PS3 Media Server

=head1 SYNOPSIS

my $ps3mencoder = App::PS3MEncoder->new();

$ps3mencoder->run();

=head1 DESCRIPTION

This is a helper script for PS3 Media Server that restores support for Web video streaming via mencoder.

=head1 AUTHOR

chocolateboy <chocolate@cpan.org>

=head1 SEE ALSO

=over

=item * L<FFmpeg|FFmpeg>

=back

=head1 VERSION

0.50

=cut
