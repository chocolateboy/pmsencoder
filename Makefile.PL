use strict;
use warnings;

use inc::Module::Install;

name     'App-PS3MEncoder';
author   'chocolateboy <chocolate@cpan.org>';
license  'artistic_2';
all_from 'lib/App/PS3MEncoder.pm';

requires 'File::HomeDir'              => 0;
requires 'IO::All'                    => 0;
requires 'IPC::Cmd'                   => '0.46';
requires 'List::MoreUtils'            => '0.12'; # first_index wasn't exported prior to this
requires 'LWP::Simple'                => 0;
requires 'Method::Signatures::Simple' => '0.05';
requires 'Mouse'                      => '0.48';

unless ($^O eq 'MSWin32') {
    requires 'File::ShareDir' => '1.01';
}

install_script 'bin/ps3mencoder';
install_share  'conf';
repository     'http://github.com/chocolateboy/ps3mencoder';
test_requires  'Test::More';

WriteAll;
