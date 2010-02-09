use lib 't', 'lib';
use strict;
use warnings;
use Test::More tests => 1;
use IO::All;
use IO_All_Test;

my $string < (io('t/mystuff') > io('t/output/seek'));
my $io = io('t/output/seek');
$io->seek(index($string, 'quite'), 0);
is($io->getline, "quite enough.\n");
