use lib 't', 'lib';
use strict;
use warnings;
use Test::More;
use IO::All;
use IO_All_Test;

plan((eval {require MLDBM; 1})
    ? (tests => 4)
    : (skip_all => "requires MLDBM")
);

my $io = io('t/output/mldbm')->mldbm('SDBM_File', 'Data::Dumper');
$io->{test} = { qw( foo foolsgold bar bargain baz bazzarro ) };
$io->{test2} = [ 1..4 ];
$io->close;

my $io2 = io('t/output/mldbm')->mldbm('SDBM_File', 'Data::Dumper');
is(scalar(@{[%$io2]}), 4);
is(scalar(@{[%{$io2->{test}}]}), 6);
is($io2->{test}{bar}, 'bargain');
is($io2->{test2}[3], 4);
