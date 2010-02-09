use lib 't', 'lib';
use strict;
use warnings;
use Test::More tests => 9;
use IO::All;

my $io = io('t/mystuff');
my $x = 0;
while (my $line = $io->getline || $io->getline) {
    my $expected = <DATA>;
    is($line, $expected);
    last if ++$x >= 8;
}

is(<DATA>, "last line\n");

__DATA__
My stuff
is quite enough.
No bluff.
My stuff
is quite enough.
No bluff.
My stuff
is quite enough.
last line
