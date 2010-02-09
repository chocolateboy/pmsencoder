use lib 't', 'lib';
use strict;
use warnings;
use Test::More tests => 1;
use IO::All;
use IO_All_Test;

my $s = io('$');
$s->append("write 1\n");
my $s1 = "IO::String ref: (".$s->string_ref.")";
$s->append("write 2\n");
my $s2 = "IO::String ref: (".$s->string_ref.")";

is($s1, $s2, "Don't create new string object with each write");
