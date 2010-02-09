use lib 't', 'lib';
use strict;
use warnings;
use Test::More tests => 2;
use IO::All;
use IO_All_Test;

my $io = io('t/scalar.t');
my @list = $io->scalar;
ok(@list == 1);
test_file_contents($list[0], 't/scalar.t');
