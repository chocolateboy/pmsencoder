use lib 't', 'lib';
use strict;
use warnings;
use Test::More tests => 2;
use IO::All;
use IO_All_Test;

my $db = io('t/output/mydbm')->dbm('SDBM_File');
$db->{fortytwo} = 42;
$db->{foo} = 'bar';

is(join('', sort keys %$db), 'foofortytwo');
is(join('', sort values %$db), '42bar');
