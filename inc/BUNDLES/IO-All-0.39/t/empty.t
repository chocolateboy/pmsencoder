use lib 't', 'lib';
use strict;
use warnings;
use Test::More tests => 5;
use IO::All;
use IO_All_Test;

my $d = io('t/output/empty');
ok($d->mkdir);
ok($d->empty);

my $f = io('t/output/file');
ok($f->touch->touch);
ok($f->empty);

eval {io('qwerty')->empty};
like($@, qr"Can't call empty");
