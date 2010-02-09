use lib 't', 'lib';
use strict;
use warnings;
use Test::More tests => 3;
use IO::All;

# Write some data to a temporary file and retrieve all the paragraphs.
my $data = io('t/synopsis5.t')->slurp;

my $temp = io->temp;
ok($temp->print($data));
ok($temp->seek(0, 0));

my @paragraphs = $temp->getlines('');
is(scalar @paragraphs, 4);
