use lib 't', 'lib';
use strict;
use warnings;
use Test::More tests => 7;
use IO::All;

my $data = join '', <DATA>;
my $io = io('t/output/subtleties1') < $data;
is("$io", 't/output/subtleties1');

ok($io->close);
ok(not $io->close);

my $data2 = $io->slurp;
$data2 .= $$io;
$data2 << $io;
is($data2, $data x 3);
ok(not $io->close);

my $io2 = io(io(io('xxx')));
ok(ref $io2);
ok($io2->isa('IO::All'));
# is("$io2", 'xxx');

__DATA__
test
data
