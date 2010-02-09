use lib 't', 'lib';
use strict;
use warnings;
use Test::More tests => 2;
use IO::All;
use IO_All_Test;

ok("xxx" > io->devnull);
ok(io->devnull->print("yyy"));
