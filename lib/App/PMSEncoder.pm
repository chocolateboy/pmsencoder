package App::PMSEncoder;

use 5.10.0;

# 0.49 or above needed for the RT #54203 fix
use Mouse 0.49; # include strict and warnings

use constant PMSENCODER => 'pmsencoder';
use constant {
    CHECK_RESOURCE_EXISTS   => 1,
    DISTRO                  => 'App-PMSEncoder',
    MENCODER_EXE            => 'mencoder.exe',
    PMSENCODER_CONFIG       => PMSENCODER . '.yml',
    PMSENCODER_EXE          => PMSENCODER . '.exe',
    PMSENCODER_LOG          => PMSENCODER . '.log',
    REQUIRE_RESOURCE_EXISTS => 2,
    SHUTDOWN_SIGNALS        => [ qw(ALRM HUP INT PIPE POLL PROF TERM USR1 USR2 VTALRM STKFLT) ],
};

# core modules
use Config;
use File::Spec;
use POSIX qw(strftime);

# CPAN modules
use File::HomeDir; # technically, this is not always needed, but using it unconditionally simplifies the code slightly
use IO::All;
use IPC::Cmd qw(can_run);
use IPC::System::Simple 1.20 qw(systemx capturex);
use List::MoreUtils qw(first_index any indexes);
use Method::Signatures::Simple;
use Path::Class qw(file dir);
use YAML::Tiny qw(Load); # not the best YAML processor, but good enough, and the easiest to install

# use Cava::Pack;               # Windows only
# use File::ShareDir;           # not used on Windows
# use LWP::Simple qw(head get)  # loaded on demand

our $VERSION = '0.71';          # PMSEncoder version: logged to aid diagnostics
our $CONFIG_VERSION = '0.70';   # croak if the config file needs upgrading; XXX try not to change this too often

# mencoder arguments
has argv => (
    is         => 'rw',
    isa        => 'ArrayRef',
    auto_deref => 1,
    trigger    => method($argv) {
        $self->debug('argv: ' . (@$argv ? "@$argv" : ''));
    },
);

# is this the Windows build packaged with Cava?
has cava => (
    is      => 'ro',
    isa     => 'Bool',
    default => sub { eval { require Cava::Pack; 1 } }
);

# the YAML config file as a hash ref
has config => (
    is      => 'rw',
    isa     => 'HashRef',
    lazy    => 1,
    builder => '_load_config',
);

# valid extensions for the pmsencoder config file
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

# IO::All logfile handle
has logfile => (
    is => 'rw',
    # isa => 'IO::All::File',
    # XXX Mouse no likey
);

# full logfile path
has logfile_path => (
    is  => 'rw',
    isa => 'Str',
);

# full path to mencoder binary
has mencoder_path => (
    is      => 'ro',
    isa     => 'Str',
    lazy    => 1,
    builder => '_build_mencoder_path',
);

# is this running on Windows (e.g. the Cava build, ActivePerl, Strawberry Perl, Cygwin)
has mswin => (
    is      => 'ro',
    isa     => 'Bool',
    default => sub { $^O eq 'MSWin32' },
);

# full path to this executable
has self_path => (
    is      => 'rw',
    isa     => 'Str',
    default => $0
);

# symbol table containing user-defined variables and named captures
has stash => (
    is         => 'rw',
    isa        => 'HashRef',
    default    => sub { {} },
    auto_deref => 1,
);

# full path to the dir searched for the user's config file
has user_config_dir => (
    is  => 'rw',
    isa => 'Str',
);

# add support for a :Raw attribute used to indicate that a handler method
# bound to "operators" in the config file should have its argument passed unprocessed
# (normally hash and array elements are passed piecewise). Actually, we can use this to handle
# any attributes, but only :Raw is currently used.
# this is untidy (Attributes::Storage looks nicer), but attributes.pm is in core (from 5.6),
# so it's one less dependency to worry about

{
    # %ATTRIBUTES contains the coderef => attributes map e.g. \&App::PMSEncoder::youtube => [ 'Raw' ]
    # %ATTRIBUTES_OK allows quick lookup of handled attributes by mapping them to true e.g. Raw => 1
    my (%ATTRIBUTES, %ATTRIBUTES_OK);

    # this needs to be initialised before MODIFY_CODE_ATTRIBUTES is called i.e. at compile-time
    BEGIN { %ATTRIBUTES_OK = map { $_ => 1 } qw(Raw) }

    # needs to be a sub as it's called at compile-time
    sub MODIFY_CODE_ATTRIBUTES {
        my ($class, $code, @attrs) = @_;
        my (@keep, @discard);

        # partition attributes into those we handle (i.e. those listed in %ATTRIBUTES_OK) and those we don't
        for my $attr (@attrs) {
            if ($ATTRIBUTES_OK{$attr}) {
                push @keep, $attr;
            } else {
                push @discard, $attr;
            }
        }

        $ATTRIBUTES{$code} = [ @keep ];
        return @discard; # return any attributes we don't handle
    }

    # by keeping track of attributes in our own %ATTRIBUTES map,
    # we can bypass attributes::get and FETCH_CODE_ATTRIBUTES
    method has_attribute($code, $attribute) {
        any { $_ eq $attribute } @{ $ATTRIBUTES{$code} || [] };
    }
}

# initialize static data like platform-specific settings.
# most attributes are constructed lazily
method BUILD {
    my $logfile_path = $self->logfile_path(file(File::Spec->tmpdir, PMSENCODER_LOG)->stringify);
    my $logfile = $self->logfile(io($logfile_path));

    $logfile->append('')->autoflush(1); # force it, so we can turn on autoflush; XXX shd probably be fixed in IO::All
    $logfile->append($/) if (-s $logfile_path);
    $self->debug(PMSENCODER . " $VERSION ($^O)");

    # on Win32, it makes sense to use the bundled mencoder e.g. $PMS_HOME\win32\mencoder.exe
    # Unfortunately, PMS' registry entries are currently broken, so we can't rely on them (e.g. we
    # can't use Win32::TieRegistry):
    #
    #     http://code.google.com/p/ps3mediaserver/issues/detail?id=555
    #
    # Instead we bundle mencoder in $PMSENCODER_HOME/res.
    # we use the private _get_resource_path method to abstract away the platform-specifics

    # initialize resource handling and fixup our stored $0 on Windows
    if ($self->cava) {
        Cava::Pack::SetResourcePath('res');

        # declare a private method - at runtime!
        method _get_resource_path($name) {
            Cava::Pack::Resource($name)
        }

        # XXX squashed bug: make sure _get_resource_path is defined *before* (indirectly) using it to set self_path
        $self->self_path(file($self->get_resource_path(''), File::Spec->updir, PMSENCODER_EXE)->absolute->stringify);
    } else {
        require File::ShareDir; # no need to worry about this not being picked up by Cava as it's non-Windows only

        # declare a private method - at runtime!
        method _get_resource_path($name) {
	    # XXX bug fix: don't croak if the resource can't be found: we handle that ourselves
            eval { File::ShareDir::dist_file(DISTRO, $name) };
        }
    }

    my $data_dir = File::HomeDir->my_data;

    $self->user_config_dir(dir($data_dir, '.' . PMSENCODER)->stringify); # ~/.pmsencoder
    $self->default_config_path($self->get_resource_path(PMSENCODER_CONFIG, REQUIRE_RESOURCE_EXISTS));
    $self->debug('path: ' . $self->self_path);

=for comment
    # create callback hooks for transcoding events
    for my $event (qw(start end error)) {
        my $event_list = "$event\_callbacks";

        # arrayref of coderefs (callbacks) to be fired when transcoding ends
        has $event_list => (
            is         => 'ro',
            isa        => 'ArrayRef',
            lazy       => 1,
            auto_deref => 1,
            default    => sub { [] },
        );

        $self->meta->add_method("on_$event", sub {
            my $callbacks = $self->$event_list;

            for my $callback (@$callbacks) {
                $callback->();
            }
        });
    }
=cut

    return $self;
}

# basic regex matcher - URI::Split and Regexp::Common::URI are of no use
method is_uri ($path) {
    # from URI::Split, which goes out of its way to not DWYM
    return ($path =~ m{(?:(?:[^:/?#]+):)(?://([^/?#]*))([^?#]*)(?:\?([^#]*))?(?:#(.*))?}) ? 1 : 0;
}

# given a filename, return the full path e.g. pmsencoder.yml => /home/<username>/perl5/whatever/pmsencoder.yml
method get_resource_path ($name, $exists) {
    my $path = $self->_get_resource_path($name);

    if ($exists) {
        if ($exists == CHECK_RESOURCE_EXISTS) {
            return (defined($path) && (-f $path)) ? $path : undef;
        } elsif ($exists == REQUIRE_RESOURCE_EXISTS) {
            return (defined($path) && (-f $path)) ? $path : $self->fatal("can't find resource: $name");
        } else { # internal error - shouldn't get here
            $self->fatal("invalid flag for get_resource_path($name): $exists");
        }
    } else {
        return $path;
    }
}

# return a text resource as a string or list of lines, according to context
method get_resource ($name) {
    my $path = $self->get_resource_path($name, REQUIRE_RESOURCE_EXISTS);

    return io($path)->chomp->slurp();
}

# dump various config settings - useful for troubleshooting
method version {
    my $user_config_dir = $self->user_config_dir || '<undef>'; # may be undef according to the File::HomeDir docs

    print STDOUT
      PMSENCODER, ":            $VERSION ($^O $Config{osvers})", $/,
      'perl:                  ', sprintf('%vd', $^V),            $/,
      'config file:           ', $self->config_file_path(),      $/,
      'config file version:   ', $self->config->{version},       $/, # the version is sanity-checked by _process_config
      'default config file:   ', $self->default_config_path(),   $/,
      'logfile:               ', $self->logfile_path(),          $/,
      'mencoder path:         ', $self->mencoder_path(),         $/,
      'user config directory: ', $user_config_dir,               $/,
}

# encapsulate the mencoder lookup logic
method _build_mencoder_path {
    my $ext = $Config{_exe};

    # we look for mencoder in these places (in descending order of priority):
    #
    # 1) mencoder_path in the config file
    # 2) the path indicated by the environment variable $MENCODER_PATH
    # 3) the current working directory (prepended to the search path by IPC::Cmd::can_run)
    # 4) $PATH (via IPC::Cmd::can_run)
    # 5) the default (bundled) mencoder - currently only available on Windows

    $self->config->{mencoder_path}
      || $ENV{MENCODER_PATH}
      || can_run('mencoder')
      || $self->get_resource_path("mencoder$ext", CHECK_RESOURCE_EXISTS)
      || $self->fatal("can't find mencoder");
}

# encapsulate the config file lookup logic 
method _build_config_file_path {
    # first: check the environment variable (should contain the absolute path)
    if (exists $ENV{PMSENCODER_CONFIG}) {
        my $config_file_path = $ENV{PMSENCODER_CONFIG};

        if (-f $config_file_path) {
            return $config_file_path;
        } else {
            $self->fatal("invalid PMSENCODER_CONFIG environment variable ($config_file_path): file not found");
        }
    }

    # second: search for it in the user's home directory e.g. ~/.pmsencoder/pmsencoder.yml
    my $user_config_dir = $self->user_config_dir();

    if (defined $user_config_dir) { # not guaranteed to be defined
        for my $ext ($self->config_file_ext) { # allow .yml, .yaml, and .conf
            my $config_file_path = file($user_config_dir, PMSENCODER . ".$ext")->stringify;
            return $config_file_path if (-f $config_file_path);
        }
    } else {
        $self->debug("can't find user config dir"); # should usually be defined; worth noting if it's not
    }

    # finally, fall back on the config file installed with the distro - this should always be available
    my $default = $self->default_config_path() || $self->fatal("can't find default config file");

    if (-f $default) {
        return $default;
    } else { # XXX shouldn't happen
        $self->fatal("can't find default config file: $default");
    }
}

# print a diagnostic message to the logfile
method debug ($message) {
    my $now = strftime("%Y-%m-%d %H:%M:%S", localtime);

    $self->logfile->append("$now: $$: $message", $/);
}

# print a diagnostic messaage to the logfile, then die with an error
method fatal ($message) {
    $self->debug("ERROR: $message");
    die $self->self_path . ": $VERSION: $$: ERROR: $message", $/;
}

# return true if an option is set, false otherwise
method isdef ($name) {
    my $argv = $self->argv;
    my $index = first_index { $_ eq "-$name" } $self->argv;

    return ($index != -1);
}

# return true if the argument is an option name, false otherwise
method isopt ($arg) {
    return (defined($arg) && (substr($arg, 0, 1) eq '-'));
}

# run mencoder in the simplest way possible *that doesn't leave behind orphan processes*
# XXX this is still a work in progress
#
# on all platforms, we need to trap a SIGTERM (from the console on Windows or from PMS on *nixes),
# and SIGINT (from the console on *nixes)
#
# XXX note: Cygwin's rxvt-native is broken, and doesn't send (or perl doesn't receive) INT or TERM on Ctrl-C

method spawn($mencoder, $argv) {
    exec { $mencoder } $mencoder, @$argv; # since this is no longer supported, use the *nix solution
}

# run mencoder
method run {
    # possibly modify @ARGV ($self->argv) and/or the stash according to the recipes in the config file.
    # either way, load and process the config file in case it includes an mencoder path
    $self->process_config();

    # FIXME: allow this to be set via the stash i.e. conditionally,
    # XXX and remove the global mencoder_path setting?
    my $mencoder = $self->mencoder_path();

    # XXX obviously, this must be retrieved *after* process_config has (possibly)
    # performed any @ARGV modifications
    my $argv = $self->argv();

    # if defined, update the URI from the value in the stash
    my $stash = $self->stash;

    unshift(@$argv, $stash->{uri}) if (defined $stash->{uri}); # always set it as the first argument

    $self->debug("exec: $mencoder" . (@$argv ? " @$argv" : ''));
    my $error = $self->spawn($mencoder, $argv);

    if ($error) {
        $self->debug("error running $mencoder: $error");
    } else {
        $self->debug("ok");
    }
}

# load the (YAML) config file as (typically) a hash ref
# XXX this is the only builder whose name doesn't begin with _build
method _load_config {
    my $config_file = $self->config_file_path();

    # XXX Try::Tiny?
    $self->debug("loading config: $config_file");
    my $yaml = eval { io($config_file)->slurp() };
    $self->fatal("can't open config: $@") if ($@);
    my $config = eval { Load($yaml) };
    $self->fatal("can't load config: $@") if ($@);
    return $config || $self->fatal("config is undefined");
}

# return a true value if the config file defines a profile that matches the current command
# may modify the stash as a side effect
# if the match succeeds, returns a closure that logs the operations performed during the match
# (this prevents us logging a bunch of side effects (e.g. stash changes) that then become obsolete
# (i.e. are rolled back) if one of the conditions of the match subsequently fails)
method match($hash) {
    my $old_stash = { $self->stash() }; # shallow copy - good enough as there are no reference values (yet)
    my $stash = $self->{stash};
    my $match = 1;
    my @debug;

    while (my ($key, $value) = each (%$hash)) { 
        if ((defined $key) && (defined $value) && (exists $stash->{$key}) && ($stash->{$key} =~ $value)) {
            # merge and log any named captures
            while (my ($named_capture_key, $named_capture_value) = each(%+)) {
                push @debug, $self->exec_let($named_capture_key, $named_capture_value); # updates $stash
            }
        } else {
            $match = 0;
            last;
        }
    }

    if ($match) {
        return sub {
            for my $thunk (@debug) {
                $thunk->();
            }
        };
    } else {
        $self->stash($old_stash); # rollback
        return 0;
    }
}

# return a pretty-printed Data::Dumper dump of the supplied value
method ddump ($value) {
    require Data::Dumper;
    local $Data::Dumper::Indent = 0;
    local $Data::Dumper::Terse = local $Data::Dumper::Sortkeys = 1;
    chomp(my $dump = Data::Dumper::Dumper($value));
    return $dump;
}

=for comment

mencoder -really-quiet                                                                                                   \
         -msglevel cfgparser=7                                                                                           \
         'http://www.youtube.com/get_video?fmt=18&video_id=ZOU8GIRUd_g&t=vjVQa1PpcFPy7dX2O3oTUTznaQQVo2eH-iAbAzj7D5M%3D' \
         -prefer-ipv4                                                                                                    \
         -oac lavc                                                                                                       \
         -of lavf                                                                                                        \
         -lavfopts format=dvd                                                                                            \
         -ovc lavc                                                                                                       \
         -lavcopts vcodec=mpeg2video:vbitrate=4096:threads=1:acodec=ac3:abitrate=128                                     \
         -ofps 25                                                                                                        \
         -o deleteme.mpg                                                                                                 \
         -cache 16384                                                                                                    \
         -vf harddup                                                                                                     \
         -msglevel cfgparser=0                                                                                           \
         -exit
         
STDOUT:

Adding file http://www.youtube.com/get_video?fmt=18&video_id=ZOU8GIRUd_g&t=vjVQa1PpcFPy7dX2O3oTUTznaQQVo2eH-iAbAzj7D5M%3D
Checking prefer-ipv4=-oac
Setting oac=lavc
Setting of=lavf
Setting lavfopts=format=dvd
Setting ovc=lavc
Setting lavcopts=vcodec=mpeg2video:vbitrate=4096:threads=1:acodec=ac3:abitrate=128
Setting ofps=25
Setting o=deleteme.mpg
Checking cache=16384
Checking vf=harddup
Adding file input.mpg
Setting msglevel=cfgparser=0

=cut

# extract the filename/uri
#
# this does two things 1) determine the URIs 2) determine their indices in @ARGV so we can remove
# them now, possibly modify them via the stash, then restore them in run()
#
# we need to remove them, rather than modifying them in place, because option removals (exec_remove)
# means their indices aren't fixed. actually, we could replace removed arguments with dummy arguments e.g.
# -quiet, but we'd still need to track the indices to change the URI(s). removing them avoids
# clogging the args with noisy dummy arguments (which is what PMS does)
#
# XXX there may be more than one URI; for the time being we only extract the first
#
# XXX for the time being, we suppress the "-exit is not an MEncoder option" error message
# (so it's not sent to the debug log); it might make more sense to trap any mencoder error
# and raise it if it's not our expected -exit error
#
# in an ideal world, we would be able to determine the indices of the URI by parsing the output
# of mencoder's cfgparser module. Unfortunately, its logging is ambiguous i.e. if it sees an option like
#
#     -quiet foo
#
# before logging "Adding file foo", it inaccurately logs "Setting quiet=foo"
# TODO this really needs to be fixed in mencoder
#
# In light of this, there are 3 remaining options for determining the URI indices:
#
# 1) assume the URIs are unambiguous and use List::MoreUtils::indices
# 2) call mencoder repeatedly, adding an option at a time and logging the last index whenever an "Adding..." appears
# 3) copy the data from various mencoder headers to duplicate mencoder's option parsing
#
# For now, the first option is implemented. This should work 99% of the time: URIs don't resemble mencoder options
# and the filenames supplied by PMS are absolute paths. Colons are used in Mac OS filenames, but mencoder
# option values don't usually start with a colon
#
# For 2), we'd only need to call mencoder n times, where n = 1 + x times, where x is the number of URIs
# detected in the first pass. in addition, if the URI is unambiguous, then n = 1.
#
# FIXME this calls mencoder without any of the careful protection against signals in run_unix()
#
# TODO: add pathological examples to the test suite

method extract_uri {
    my $argv = $self->argv;
    my $mencoder = $self->mencoder_path;

    my @cfgparser = capturex(
        [ 1 ], # ignore exit code == 1
        $mencoder,
        '-really-quiet',
        '-msglevel' => 'cfgparser=7', # log cfgparser
        @$argv,
        '-msglevel' => 'cfgparser=0', # disable most logging i.e. don't log "-exit is not an MEncoder option"
        '-exit' # bogus argument to halt mencoder
    );

    # the "Adding file..." message is hardwired in English in parser-mecmd.c - though it probably shouldn't be
    my @uris = map { chomp; /^Adding file (.+)$/ ? $1 : () } @cfgparser;

    unless (@uris) {
        $self->debug('no URI supplied');
        return undef;
    }

    # for now, restrict pmsencoder to only handle one URI
    unless (@uris == 1) {
        my $uris = $self->ddump(\@uris);
        $self->fatal("multiple URIs are not currently supported: $uris") 
    }

    my $uri = $uris[0];

    # check for unambiguous URIs
    my @indices = indexes { $_ eq $uri } @$argv;

    unless (@indices == 1) {
        my $quoted_uri = $self->ddump($uri);
        my $indices = $self->ddump(\@indices);
        $self->fatal("ambiguous URIs are not currently supported: $quoted_uri found at multiple indices: $indices") 
    }

    my $index = $indices[0];

    splice @$argv, $index, 1; # *remove* the URI - restored in run()
    return $uri;
}

# initiialize the symbol table hashref
method initialize_stash {
    my $stash = $self->stash();
    my $argv  = $self->argv();

    # XXX: doesn't work under the test harness i.e. returns "PMS"
    my $context = ((-t STDIN) && (-t STDOUT))? 'CLI' : 'PMS'; # FIXME: doesn't detect CLI under Cygwin/rxvt-native

    # FIXME: should probably use a naming convention to distinguish
    # builtin names (uri, context &c.) from user-defined names (video_id, t &c.)
    $self->exec_let(context => $context); # use exec_let so it's logged; void context: logged immediately
    $self->exec_let(platform => $^O);

    my $uri = $self->extract_uri();

    # don't try to set the URI if none was supplied e.g. pmsencoder called with no args
    if (defined $uri) {
        $self->exec_let(uri => $uri);

        # XXX this is not going to agree with the criteria PMS uses (i.e. is it in WEB.conf)
        if ($self->is_uri($uri)) {
            $self->exec_let(mode => 'Web');
        } else {
            $self->exec_let(mode => 'File');
        }
    }
}

# load the config file and match the current command against any profiles
method process_config {
    # make sure we can load the config, and that it's sane, before initializing the stash
    my $config = $self->config();

    # FIXME: this assumes the config file is sane for the most part
    # XXX use Kwalify?

    my $version = $config->{version};

    $self->fatal("no version found in the config file") unless (defined $version);
    $self->debug("config file version: $version");
    $self->fatal("config file is out of date; please upgrade") unless ($version && ($version >= $CONFIG_VERSION));

    my $profiles = $config->{profiles};

    if ($profiles) {
        # initialize the stash i.e. set up entries for uri, context &c. that may be used as match criteria.
        # do this lazily; no point unless profiles are defined

        $self->initialize_stash;

        for my $profile (@$profiles) {
            my $profile_name = $profile->{name};

            unless (defined $profile_name) {
                $self->debug('profile name not defined');
                next;
            }

            my $match = $profile->{match};

            unless ($match) {
                $self->debug("invalid profile: no match supplied for: $profile_name");
                next;
            }

            # may update the stash if successful
            my $matched = $self->match($match);
            next unless ($matched);

            $self->debug("matched profile: $profile_name");

            # only show the exec_let log messages *after* we've announced the profile match.
            # otherwise we see:
            #
            #     setting video_id to ...
            #     matched profile: YouTube
            #
            # i.e. the effects of a successful profile match before we've logged the match - which is confusing

            $matched->(); # now we know the match has succeeded force/redeem the thunked log messages

            my $options = $profile->{options};

            unless ($options) {
                $self->debug("invalid profile: no options defined for: $profile_name");
                next;
            }

            $options = [ $options ] unless (ref($options) eq 'ARRAY');

            for my $hash (@$options) {
                while (my ($key, $value) = each(%$hash)) {
                    my $operator = $self->can("exec_$key");

                    $self->fatal("invalid operator: $key") unless ($operator);

                    if (ref($value) && not($self->has_attribute($operator, 'Raw'))) {
                        if ((ref $value) eq 'HASH') {
                            while (my ($k, $v) = each(%$value)) {
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
    } else {
        $self->debug('no profiles defined');
    }
}

################################# Operators ################################

# given (in the stash) the $video_id and $t values of a YouTube media URI (i.e. the URI of an .flv, .mp4 &c.),
# construct the full URI with various $fmt values in succession and set the stash $uri value to the first one
# that's valid (based on a HEAD request)
# see http://stackoverflow.com/questions/1883737/getting-an-flv-from-youtube-in-net
method exec_youtube ($formats) :Raw {
    my $stash = $self->stash;
    my $uri   = $stash->{uri};
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

    if (@$formats) {
        require LWP::Simple;

        for my $fmt (@$formats) {
            my $media_uri = "http://www.youtube.com/get_video?fmt=$fmt&video_id=$video_id&t=$t";
            next unless (LWP::Simple::head $media_uri);
            $self->exec_let(uri => $media_uri); # set the new URI; use exec_let so it's logged; void context: log it now
            $found = 1;
            last;
        }
    } else {
        $self->fatal("no formats defined for $uri");
    }

    $self->fatal("can't retrieve YouTube video from $uri") unless ($found);
}

# set an mencoder option - create it if it doesn't exist
method exec_set ($name, $value) {
    $name = "-$name";

    my $argv = $self->argv;
    my $index = first_index { $_ eq $name } @$argv;

    if ($index == -1) {
        if (defined $value) {
            $self->debug("adding $name $value");
            push @$argv, $name, $value;    # FIXME: encapsulate @argv handling
        } else {
            $self->debug("adding $name");
            push @$argv, $name;            # FIXME: encapsulate @argv handling
        }
    } elsif (defined $value) {
        $self->debug("setting $name to $value");
        $argv->[ $index + 1 ] = $value;    # FIXME: encapsulate @argv handling
    }
}

# perform a search and replace in the value of an mencoder option
# TODO: handle undef, a single hashref and an array of hashrefs
method exec_replace ($name, $hash) {
    $name = "-$name";

    my $argv = $self->argv();
    my $index = first_index { $_ eq $name } @$argv;

    if ($index != -1) {
        while (my ($search, $replace) = each(%$hash)) {
            $self->debug("replacing $search with $replace in $name");
            $argv->[ $index + 1 ] =~ s{$search}{$replace}; # FIXME: encapsulate @argv handling
        }
    }
}

# remove an mencoder option
method exec_remove ($name) {
    $name = "-$name";

    my $argv = $self->argv; # modify the reference to bypass the setter's logging if we change it
    my @argv  = @$argv; # but create a working copy we can modify in the meantime
    my $nargs = @argv;
    my @keep;

    while (@argv) {
        my $arg = shift @argv;

        if ($self->isopt($arg)) { # -foo ...
            if (@argv && not($self->isopt($argv[0]))) {    # -foo bar
                my $value = shift @argv;

                if ($arg ne $name) {
                    push @keep, $arg, $value;
                }
            } elsif ($arg ne $name) {                      # just -foo
                push @keep, $arg;
            }
        } else {
            push @keep, $arg;
        }
    }

    if (@keep < $nargs) {
        $self->debug("removing $name");
        @$argv = @keep; # bypass setter logging
    }
}

# define a variable in the stash, performing any variable substitutions
method exec_let ($name, $value) {
    my $stash = $self->stash();
    my $original_value = $value;
    my @debug = ();

    push @debug, sub { $self->debug("setting \$$name to $value") };

    while (my ($key, $replace) = each(%$stash)) {
        my $search = qr{(?:(?:\$$key\b)|(?:\$\{$key\}))};

        if ($value =~ $search) {
            push @debug, sub { $self->debug("replacing \$$key with '$replace' in $value") };
            $value =~ s{$search}{$replace}g;
        }
    }

    $stash->{$name} = $value;
    push @debug, sub { $self->debug("set \$$name to $value") unless ($value eq $original_value) };

    my $debug = sub {
        for my $thunk (@debug) {
            $thunk->();
        }
    };

    if (defined wantarray) {
        return $debug;
    } else { # void context: redeem debug promises
        $debug->();
    }
}

# define a variable in the stash by extracting its value from the document pointed to by the current URI
method exec_get ($key, $value) {
    my $stash = $self->stash;
    my $uri   = $stash->{uri} || $self->fatal("can't perform get op: no URI defined"); 

    my $document = do { # cache for subsequent matches
        unless (exists $self->document->{$uri}) {
            require LWP::Simple;
            $self->document->{$uri} = LWP::Simple::get($uri) || $self->fatal("can't retrieve URI: $uri");
        }
        $self->document->{$uri};
    };

    if (defined $value) { # extract the first parenthesized capture
        $self->debug("extracting \$$key from $uri");
        my ($extract) = $document =~ /$value/;
        $self->exec_let($key, $extract); # void context: log it now
    } else { # extract any named captures
        $document =~ /$key/;
        while (my ($named_capture_key, $named_capture_value) = (each %+)) {
            $self->exec_let($named_capture_key, $named_capture_value); # void context: log it now
        }
    }
}

# remove an entry from the stash
# XXX unused/untested
method exec_delete ($key) {
    my $stash = $self->stash;

    if (defined $key) {
        if (exists $stash->{$key}) {
            $self->debug("deleting stash entry: $key");
            delete $stash->{$key};
        } else {
            $self->debug("can't delete stash entry: no such key: $key");
        }
    } else {
        $self->debug("can't delete stash entry: undefined key");
    }
}

1;

__END__

=head1 NAME

App::PMSEncoder - MEncoder wrapper for PS3 Media Server

=head1 SYNOPSIS

    my $pmsencoder = App::PMSEncoder->new({ argv => \@ARGV });

    $pmsencoder->run();

=head1 DESCRIPTION

This is a helper script for PS3 Media Server that restores support for Web video streaming via mencoder.

See here for more details: http://github.com/chocolateboy/pmsencoder

=head1 AUTHOR

chocolateboy <chocolate@cpan.org>

=head1 SEE ALSO

=over

=item * L<FFmpeg|FFmpeg>

=back

=head1 VERSION

0.71

=cut
