use 5.010000;

use strict;
use warnings;

BEGIN { $ENV{PERL_AUTOINSTALL_PREFER_CPAN} = 1 }

use inc::Module::Install;

name     'App-PMSEncoder';
author   'chocolateboy <chocolate@cpan.org>';
license  'artistic_2';
all_from 'lib/App/PMSEncoder.pm';
include  'Module::AutoInstall';

requires 'File::HomeDir'               => '0.89';
requires 'IO::All'                     => '0.39';
requires 'IPC::Cmd';
requires 'IPC::System::Simple'         => '1.20';
requires 'List::MoreUtils'             => '0.22';
requires 'LWP::Simple';
requires 'Method::Signatures::Simple'  => '0.05';
requires 'Mouse'                       => '0.49';
requires 'Path::Class'                 => '0.18';
requires 'Scope::Guard'                => '0.03';
requires 'YAML::Tiny'                  => '1.41';

# platform-specific access to resources e.g. default config file, bundled mencoder,
# the text file containing the mencoder args for --test

if ($ENV{PMSENCODER_WIN32}) { # only set this if, for some reason, you want to build pmsencoder for Windows (using Cava)
    requires 'Cava::Pack';
} else {
    requires 'File::ShareDir' => '1.01';
    install_share 'conf';
}

install_script 'bin/pmsencoder';
repository     'http://github.com/chocolateboy/pmsencoder';

test_requires  'Test::Command' => '0.08';
test_requires  'Test::More';

auto_install();
WriteAll();
