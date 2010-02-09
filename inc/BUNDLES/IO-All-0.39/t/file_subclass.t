use lib 't', 'lib';
use strict;
use warnings;
use Test::More tests => 5;
use IO_Dumper;
use IO_All_Test;

my $hash = {
    red => 'square',
    yellow => 'circle',
    pink => 'triangle',
};

my $io = io->file('t/output/dump2')->dump($hash);
ok(-f 't/output/dump2');
ok($io->close);
ok(-s 't/output/dump2');

my $VAR1;
my $a = do 't/output/dump2';
my $b = eval join('',<DATA>);
is_deeply($a,$b);

ok($io->unlink);

package main;
__END__
$VAR1 = {
  'pink' => 'triangle',
  'red' => 'square',
  'yellow' => 'circle'
};
