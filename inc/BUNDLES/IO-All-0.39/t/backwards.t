use lib 't', 'lib';
use strict;
use warnings;
use Test::More;
use IO::All;
use IO_All_Test;

plan((eval {require File::ReadBackwards; 1})
    ? (tests => 2)
    : (skip_all => "requires File::ReadBackwards")
);

my @reversed;
my $io = io('t/mystuff');
$io->backwards;
while (my $line = $io->getline) {
    push @reversed, $line;
}

test_file_contents(join('', reverse @reversed), 't/mystuff');

@reversed = io('t/mystuff')->backwards->getlines;

test_file_contents(join('', reverse @reversed), 't/mystuff');
