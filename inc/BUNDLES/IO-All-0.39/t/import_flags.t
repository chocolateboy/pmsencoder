use lib 't', 'lib';
use strict;
use warnings;
use Test::More;
use IO_All_Test;

BEGIN {
    plan(($] < 5.008003)
          ? (skip_all => 'Broken on older perls')
          : (tests => 16)
    );
}

package One;
use IO::All -strict;


package Two;
use IO::All -utf8;


package Three;
use IO::All -strict, -utf8;


package Four;
use IO::All -foo;


package main;
main::ok(defined &One::io, 'io is exported to One');
main::ok(defined &Two::io, 'io is exported to Two');
main::ok(defined &Three::io, 'io is exported to Three');
main::ok(defined &Four::io, 'io is exported to Four');

my $io1 = One::io('xxx');
ok $io1->_strict,
   'strict flag set on object 1';
ok not($io1->_utf8),
   'utf8 flag not set on object 1';

my $io2 = Two::io('xxx');
ok not($io2->_strict),
   'strict flag not set on object 2';
ok $io2->_utf8,
   'utf8 flag set on object 2';

my $io3 = Three::io('xxx');
ok $io3->_strict,
   'strict flag set on object 3';
ok $io3->_utf8,
   'utf8 flag set on object 3';

eval "Four::io('xxx')";
like $@, qr/Can't find a class for method 'foo'/,
    '-foo flag causes error';

my $io2b = $io2->catfile('yyy');
is $io2b->name, f('xxx/yyy'),
   'catfile name is correct';
ok not($io2b->_strict),
   'strict flag not set on object 2b (propagated from 2)';
ok $io2b->_utf8,
   'utf8 flag set on object 2b (propagated from 2)';

my $io2c = Two::io('aaa')->curdir;
# use Data::Dumper;
# die Dumper \%{*$io2c};
ok not($io2c->_strict),
   'strict flag not set on object 2c (propagated from 2)';
ok $io2c->_utf8,
   'utf8 flag set on object 2c (propagated from 2)';

