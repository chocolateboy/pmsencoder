use lib 't', 'lib';
use strict;
use warnings;
use Test::More tests => 4;
use IO::All;
use IO_All_Test;

ok(not -e 't/output/newpath/hello.txt');
ok(not -e 't/output/newpath');
my $io = io('t/output/newpath/hello.txt')->assert;
ok(not -e 't/output/newpath');
"Hello\n" > $io;
ok(-f 't/output/newpath/hello.txt');
