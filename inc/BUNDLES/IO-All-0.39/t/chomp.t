use lib 't', 'lib';
use strict;
use warnings;
use Test::More 'no_plan';
use IO::All;
use IO_All_Test;

my $io = io('t/chomp.t')->chomp;
for ($io->slurp) {
    ok(not /\n/);
}
$io->close;

for ($io->chomp->separator('io')->getlines) {
    ok(not /io/);
}
