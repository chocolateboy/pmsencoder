use lib 't', 'lib';
use strict;
use warnings;
use Test::More tests => 2;
use IO::All;
use IO_All_Test;

my $io1 = io('t/output/print.t');
is($io1->print("one\n")->print("two\n")->close->scalar, "one\ntwo\n");
my $io2 = io('t/output/print.t');
is($io2->println("one")->println("two")->close->scalar, "one\ntwo\n");
