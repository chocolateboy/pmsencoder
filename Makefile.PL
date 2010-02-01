use strict;
use warnings;

use inc::Module::Install;

name 'App-PS3MEncoder';
# perl_version '5.008';
author 'chocolateboy <chocolate@cpan.org>';
all_from 'lib/App/PS3MEncoder.pm';

requires 'File::ShareDir'  => '1.01';
requires 'IO::All'         => 0;
requires 'IPC::Cmd'        => '0.46';
requires 'List::MoreUtils' => 0;
requires 'LWP::Simple'     => 0;
requires 'MooseX::Declare' => '0.32';

test_requires 'Test::More' => 0;
install_share 'config';
install_script 'bin/ps3mencoder';

WriteAll;
