use lib 't', 'lib';
use strict;
use warnings;
use Test::More;
use IO::All;
use IO_All_Test;

# XXX This needs to be fixed!!!
plan( $^O !~ /^(cygwin|solaris|hpux)$/
    ? (tests => 3)
    : (skip_all => "XXX - locking problems on solaris/cygwin/hpux")
);

my $io1 = io('t/output/foo')->lock;
$io1->println('line 1');

fork and do {
    my $io2 = io('t/output/foo')->lock;
    is($io2->getline, "line 1\n");
    is($io2->getline, "line 2\n");
    is($io2->getline, "line 3\n");
    exit;
};

sleep 1;
$io1->println('line 2');
$io1->println('line 3');
$io1->unlock;

1;
