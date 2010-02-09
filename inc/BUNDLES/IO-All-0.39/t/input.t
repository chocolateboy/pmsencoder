use lib 't', 'lib';
use strict;
use warnings;
use Test::More tests => 12;
use IO::All;
use IO_All_Test;

io('t/input.t') > my $contents;
test_file_contents($contents, 't/input.t');

$contents < io 't/input.t';
test_file_contents($contents, 't/input.t');

my $io = io 't/input.t';
$contents = $$io;
test_file_contents($contents, 't/input.t');

$contents = $io->slurp;
test_file_contents($contents, 't/input.t');

$contents = $io->scalar;
test_file_contents($contents, 't/input.t');

$contents = join '', $io->getlines;
test_file_contents($contents, 't/input.t');

SKIP: {
    eval {require Tie::File};
    skip "requires Tie::File", 2	if $@;

    $io->rdonly;
    $contents = join '', map "$_\n", @$io;
    test_file_contents($contents, 't/input.t');
    $io->close;

    $io->tie;
    $contents = join '', <$io>;
    test_file_contents($contents, 't/input.t');
}

my @lines = io('t/input.t')->slurp;
ok(@lines > 36);
test_file_contents(join('', @lines), 't/input.t');

my $old_contents = $contents;
$contents << io('t/input.t');
is($contents, $old_contents . $old_contents);

is(io('t/input.t') >> $contents, ($old_contents x 3));
