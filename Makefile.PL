use 5.10.0;
use strict;
use warnings;

use inc::Module::Install;

name     'App-PMSEncoder';
author   'chocolateboy <chocolate@cpan.org>';
license  'artistic_2';
all_from 'lib/App/PMSEncoder.pm';

requires 'File::HomeDir';
requires 'IO::All';
requires 'IPC::Cmd'                    => '0.54'; # this is key, so let's have the latest version
requires 'List::MoreUtils'             => '0.12'; # first_index wasn't exported till 0.12
requires 'LWP::Simple';
requires 'Method::Signatures::Simple';
requires 'Mouse'                       => '0.49'; # 0.49 includes an update that fixes buggy interaction with Path::Class
requires 'Path::Class';
requires 'YAML::XS';

# not used on Windows
unless ($^O eq 'MSWin32') {
    requires 'File::ShareDir' => '1.01';
    install_share 'conf';
}

install_script 'bin/pmsencoder';
repository     'http://github.com/chocolateboy/pmsencoder';
test_requires  'Test::More';

WriteAll;
