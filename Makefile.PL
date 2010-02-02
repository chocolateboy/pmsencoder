use 5.008001;
use strict;
use warnings;

use inc::Module::Install;

name     'App-PS3MEncoder';
author   'chocolateboy <chocolate@cpan.org>';
license  'artistic_2';
all_from 'lib/App/PS3MEncoder.pm';

requires 'File::HomeDir';
requires 'IO::All';
requires 'IPC::Cmd'                   => '0.46';
requires 'List::MoreUtils'            => '0.12'; # first_index wasn't exported prior to this
requires 'LWP::Simple';
requires 'Method::Signatures::Simple' => '0.05';
requires 'Mouse'                      => '0.48';
requires 'Path::Class';
requires 'YAML::XS';

unless ($^O eq 'MSWin32') { # used for default config file - not used on Windows
    requires 'File::ShareDir' => '1.01';
}

install_script 'bin/ps3mencoder';
install_share  'conf';
repository     'http://github.com/chocolateboy/ps3mencoder';
test_requires  'Test::More';

WriteAll;
