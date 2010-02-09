use lib 't', 'lib';
use strict;
use warnings;
use Test::More tests => 2;
use IO::All;

my $t1 = io('quack');
eval {
    $t1->slurp;
};
like($@, qr{^Can't open file 'quack' for input:});

my $t2 = io('t/xxxxx');
eval {
    $t2->next;
};
like($@, qr{^Can't open directory 't/xxxxx':});
