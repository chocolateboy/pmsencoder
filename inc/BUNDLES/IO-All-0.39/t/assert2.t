use lib 't', 'lib';
use strict;
use warnings;
use Test::More tests => 4;
use IO::All;
use IO_All_Test;

ok(io('t/output/xxx/yyy/zzz.db')->dbm->assert->{foo} = "bar");
ok(-f 't/output/xxx/yyy/zzz.db' or -f 't/output/xxx/yyy/zzz.db.dir');
SKIP: {
    skip "requires MLDBM", 2
      unless eval { require MLDBM; 1};
    ok(io('t/output/xxx/yyy/zzz2.db')->assert->mldbm->{foo} = ["bar"]);
    ok(-f 't/output/xxx/yyy/zzz2.db' or -f 't/output/xxx/yyy/zzz.db.dir');
}
