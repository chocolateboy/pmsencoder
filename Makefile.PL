use 5.10.0;

use strict;
use warnings;

use inc::Module::Install;

name     'App-PMSEncoder';
author   'chocolateboy <chocolate@cpan.org>';
license  'artistic_2';
all_from 'lib/App/PMSEncoder.pm';
include  'Module::AutoInstall';

requires 'File::HomeDir'               => '0.89';
requires 'HTTP::Lite'                  => '2.2';
requires 'IO::All'                     => '0.39';
requires 'IPC::Cmd'                    => '0.56'; # core, but we want the latest version
requires 'Path::Class'                 => '0.18';
requires 'YAML::Tiny'                  => '1.41';

if ($ENV{PMSENCODER_STANDALONE}) {
    requires 'Mouse::Tiny' => '0.49';
    $ENV{LIST_MOREUTILS_PP} = 1; # force pure perl
    requires 'Mouse::Tiny' => '0.49';
} else {
    requires 'Mouse' => '0.49';
}

requires 'List::MoreUtils'             => '0.22';

# don't include anything that depends on an XS module (LWP depends on HTML::Parser)
# requires 'LWP::Simple';
# requires 'Method::Signatures::Simple';

# platform-specific access to resources e.g. default config file, bundled mencoder,
# the text file containing the mencoder args for --test
if ($ENV{PMSENCODER_WIN32}) {
    requires 'Cava::Pack';
} else {
    requires 'File::ShareDir' => '1.01';
    install_share 'conf';
}

install_script 'bin/pmsencoder';
repository     'http://github.com/chocolateboy/pmsencoder';
test_requires  'Test::More';

auto_install;

WriteAll;
