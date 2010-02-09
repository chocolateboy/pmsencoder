use lib 't', 'lib';
use strict;
use warnings;
use Test::More tests => 6;
use IO::All;
use IO_All_Test;

# Combine two files into a third
my $my_stuff = io('t/mystuff')->slurp;
test_file_contents($my_stuff, 't/mystuff');
my $more_stuff << io('t/morestuff');
test_file_contents($more_stuff, 't/morestuff');
io('t/allstuff')->print($my_stuff, $more_stuff);
ok(-f 't/allstuff');
ok(-s 't/allstuff');
test_file_contents($my_stuff . $more_stuff, 't/allstuff');
ok(unlink('t/allstuff'));
