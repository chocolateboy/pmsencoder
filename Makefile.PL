use 5.010000;

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
requires 'Mouse'                       => '0.49';
requires 'List::MoreUtils'             => '0.22';
requires 'Method::Signatures::Simple'  => '0.05';

recommends 'LWP::Simple';

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

# this doesn;t work, nor do any variations on it:
# "Module::CoreList has no information on perl 5.010"
# so for now include any dependencies manually
# requires(perl => '5.010000');

# auto bundling doesn't work, so let's try auto-installing i.e. via CPAN :-(
auto_install;

WriteAll;
