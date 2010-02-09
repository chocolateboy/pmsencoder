use lib 't', 'lib';
use strict;
use warnings;
use Test::More tests => 3;
use IO::All;
use IO_All_Test;
use diagnostics;

my $io = io($0);

$io->absolute;
is("$io", File::Spec->rel2abs($0));
$io->relative;
is($io->pathname, File::Spec->abs2rel($0));

ok(io('t')->absolute->next->is_absolute);
