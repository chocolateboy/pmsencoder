use lib 't', 'lib';
use strict;
use warnings;
use Test::More tests => 1;
use IO::All;
use IO_All_Test;

my $io = io('t/println.t');
my @lines = map {chomp; $_} $io->slurp;
my $temp = io('?');
$temp->println(@lines);
$temp->seek(0, 0);
my $text = $temp->slurp;

test_file_contents($text, 't/println.t');
