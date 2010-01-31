package App::PS3MEncoder;

use 5.10.0;
use strict;
use warnings;

# core modules
use Cwd qw(abs_path getcwd);
use File::Spec;
use IPC::Cmd 0.54 qw(can_run); # core since 5.10.0, but we need a version that escapes shell arguments correctly
use POSIX qw(strftime);

# CPAN modules
use IO::All;
use List::MoreUtils qw(first_index);
use LWP::Simple qw(get head);
use YAML::XS qw(Load);

our $VERSION = 0.50; # PS3MEncoder version: logged to aid diagnostics

my @CONFIG_FILE_EXT   = qw(conf yml yaml); # valid extensions for the ps3mencoder config file
my $CONFIG;                                # the YAML config file as a hash ref
my $CWD               = abs_path(getcwd);  # current working directory
my %DOCUMENT          = ();                # document cache for exec_get
my $LOGFILE;                               # IO::All logfile handle
my $LOGFILE_PATH      = 'ps3mencoder.log'; # full path to the logfile; leave as the logfile name if $CWD is writable
my $SELF              = $0;                # full path to this executable
my %STASH             = ();                # symbol table containing user-defined variables and named captures
my $URI_INDEX         = 0;                 # position in @ARGV of the filename/URI
my $WINDOWS           = $^O eq 'MSWin32';  # is this running on Windows?

unless (-w $LOGFILE_PATH) { # Windows 7 finally enters the 1970s by restricting write access to the installation dir
    $LOGFILE_PATH = File::Spec->catfile(File::Spec->tmpdir, $LOGFILE_PATH);
}

$LOGFILE = io($LOGFILE_PATH);

=for comment

    mencoder http://movies.apple.com/movies/foo.mov -prefer-ipv4 \
        -nocache -quiet -oac lavc -of lavf -lavfopts format=dvd -ovc lavc \
        -lavcopts vcodec=mpeg2video:vbitrate=4096:threads=2:acodec=ac3:abitrate=128 \
        -ofps 24000/1001 -o /tmp/javaps3media/mencoder1261801759641

=cut

###################################################################################

sub debug {
    my ($self, $message) = @_;
    my $now = strftime("%Y-%m-%d %H:%M:%S", localtime);

    $LOGFILE->append("$now: $$: $message", $/);
}

sub fatal {
    my ($self, $message) = @_;

    $self->debug("ERROR: $message");

    die "$SELF: $VERSION: $$: ERROR: $message", $/;
}

sub isdef {
    my ($self, $name) = @_;
    my $index = first_index { $_ eq $name } @ARGV;

    return ($index != -1);
}

sub isopt($) {
    my ($self, $arg) = @_;
    return (defined($arg) && (substr($arg, 0, 1) eq '-'));
}

sub run {
    my $self = shift;
    my $mencoder_path;

    # we look for mencoder in these places (in desceneding order of priority):
    #
    # 1) mencoder_path specified in the config file
    # 2) the path indicated by the environment variable $MENCODER_PATH
    # 3) the current working directory (prepended to the search path by IPC::Cmd::can_run)
    # 4) $PATH (via IPC::Cmd::can_run)

    my $mencoder = $CONFIG->{mencoder_path} ||
                   $ENV{MENCODER_PATH} ||
		   can_run('mencoder') ||
		   $self->fatal("can't find mencoder");

    $self->debug("exec: $mencoder" . (@ARGV ? " @ARGV" : ''));

    $IPC::Cmd::USE_IPC_RUN = 0;

    my ($ok, $err) = IPC::Cmd::run(
        command => [ $mencoder, @ARGV ],
        verbose => 1
    );

    if ($ok) {
        $self->debug('ok');
    } elsif ($?) {
	# fatal "can't exec mencoder: " . ($? >> 8);
	$self->fatal("can't exec mencoder: $err");
    } else {
        $self->debug("unknown error: $err");
    }

    exit 0;
}

sub process_config {
    my $self = shift;
    my $uri = $ARGV[$URI_INDEX];
    my (@config, $config);
    
    # explicitly specify the full path to the config file
    if (exists $ENV{PS3MENCODER_CONF}) {
       push @config, $ENV{PS3MENCODER_CONF};
    }

    # search for it in the PMS home directory, e.g. alongside PMS.conf, if $PMS_HOME is defined
    if (exists $ENV{PMS_HOME}) {
       push @config, map { File::Spec->catfile($ENV{PMS_HOME}, "ps3mencoder.$_") } @CONFIG_FILE_EXT;
    }

    # If all else fails, look in the current working directory. This will be $PMS_HOME if ps3mencoder
    # is called by PMS
    push @config, map { File::Spec->catfile($CWD, "ps3mencoder.$_") } @CONFIG_FILE_EXT;

    for my $config_file (@config) {
        if (-f $config_file) {
            $self->debug("loading config: $config_file");
            my $yaml = eval { io($config_file)->slurp() };
            $self->fatal("can't open config: $@") if ($@);
            $config = eval { Load($yaml) };
            $self->fatal("can't load config: $@") if ($@);
            last;
        }
    }

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
                            exec_let($named_capture_key, $named_capture_value);
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
        require Data::Dumper;
        $Data::Dumper::Terse = 1;
        $Data::Dumper::Indent = 0;
        $self->debug("can't find ps3mencoder config file in: " . Data::Dumper::Dumper(\@config));
    }

    return $config || {};
}

################################# MEncoder Options ################################

# extract the media URI - see http://stackoverflow.com/questions/1883737/getting-an-flv-from-youtube-in-net
sub exec_youtube {
    my ($self, $formats) = @_;
    my $uri = $ARGV[$URI_INDEX];
    my $id = $STASH{id};
    my $signature = $STASH{signature};
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
        my $media_uri = "http://www.youtube.com/get_video?fmt=$fmt&video_id=$id&t=$signature";
        next unless (head $media_uri);
        exec_uri($media_uri);
        $found = 1;
        last;
    }

    $self->fatal("can't retrieve YouTube video from $uri") unless ($found);
}

sub exec_set {
    my ($self, $name, $value) = @_;

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

sub exec_replace {
    my ($self, $name, $search, $replace) = @_;

    $name = "-$name";

    my $index = first_index { $_ eq $name } @ARGV;

    if ($index != -1) {
        $self->debug("replacing $search with $replace in $name");
        $ARGV[ $index + 1 ] =~ s{$search}{$replace};
    }
}

sub exec_remove {
    my ($self, $name) = @_;

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
sub exec_let {
    my ($self, $name, $value) = @_;
    $self->debug("setting \$$name to $value");
    $STASH{$name} = $value;
}

# define a variable in the stash by extracting a value from the document pointed to by the current URI
sub exec_get {
    my ($self, $key, $value) = @_;
    my $uri = $ARGV[$URI_INDEX];
    my $document = do {
        unless (exists $DOCUMENT{$uri}) {
            $DOCUMENT{$uri} = get($uri) || $self->fatal("can't retrieve $uri");
        }
        $DOCUMENT{$uri};
    };

    if (defined $value) {
        $self->debug("extracting \$$key from $uri");
        my ($extract) = $document =~ /$value/;
        exec_let($key, $extract);
    } else {
        $document =~ /$key/;
        while (my ($named_capture_key, $named_capture_value) = (each %+)) {
            exec_let($named_capture_key, $named_capture_value);
        }
    }
}

# set the URI, performing any variable substitutions
sub exec_uri {
    my ($self, $uri) = @_;

    while (my ($key, $value) = each (%STASH)) {
        if ($uri =~ /\$$key\b/) {
            $self->debug("replacing \$$key with '$value' in $uri");
            $uri =~ s{(?:(?:\$$key\b)|(?:\$\{$key\}))}{$value}g;
        }
    }

    $ARGV[$URI_INDEX] = $uri;
}

sub new {
    my $class = shift;
    my $options;

    if (@_) {
       	if (ref($_[0]) eq 'HASH') {
	    $options = shift;
	} else {
	    $options = { @_ };
	}
    } else {
	$options = {};
    }

    my $self = bless $options, ref($class) || $class;

    $| = 1; # unbuffer output
    $LOGFILE->append($/) if (-s $LOGFILE_PATH);
    $self->debug("PS3MEncoder $VERSION");

    # on Win32, the config file should be in $PMS_HOME, typically C:\Program Files\PS3 Media Server
    # Unfortunately, PMS' registry entries are currently broken, so we can't rely on them (e.g. we
    # can't use Win32::TieRegistry):
    #
    #     http://code.google.com/p/ps3mediaserver/issues/detail?id=555
    #
    # Instead we bundle the default config file (and mencoder.exe) in $PS3MENCODER_HOME/res
    # and ensure they're picked up with a reasonably high priority by setting them as
    # environment variables
    
    if ($WINDOWS) {
	eval 'use Cava::Pack';
	$self->fatal("can't load Cava::Pack: $@") if ($@);
	Cava::Pack::SetResourcePath('res');
	$ENV{PS3MENCODER_CONF} ||= Cava::Pack::Resource('ps3mencoder.conf');
	$ENV{MENCODER_PATH} ||= Cava::Pack::Resource('mencoder.exe');
	$SELF = abs_path(File::Spec->catfile(Cava::Pack::Resource(''), File::Spec->updir, 'ps3mencoder.exe'));
    }

    $self->debug($SELF . (@ARGV ? " @ARGV" : ''));

    unless ($self->isdef('-prefer-ipv4')) { # $URI_INDEX defaults to 0
	$URI_INDEX = 4; # hardwired in net.pms.encoders.MEncoderVideo.launchTranscode
    }

    $CONFIG = $self->process_config(); # load the config and process matching profiles
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
