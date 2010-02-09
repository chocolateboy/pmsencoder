use lib 't', 'lib';
use strict;
use warnings;
use Test::More;
use IO::All;
use IO_All_Test;

my @lines = read_file_lines('t/mystuff');
plan(tests => 1 + @lines + 1);

my $io = io('t/mystuff')->tie;
is($io->autoclose(0) . '', 't/mystuff');
while (<$io>) {
    is($_, shift @lines);
}
ok(close $io);
