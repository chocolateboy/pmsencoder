use lib 't', 'lib';
use strict;
use warnings;
use Test::More tests => 1;
use IO::All;
use IO_All_Test;

my $io = io('t/tie.t')->tie;
my $file = join '', <$io>;
test_file_contents($file, 't/tie.t');
