use lib 't', 'lib';
use strict;

package IO::Dumper;
use IO::All -base;
use Data::Dumper;

our @EXPORT = 'io';

sub io { return IO::Dumper->new(@_) };

package IO::All::Filesys;
sub dump {
    my $self = shift;
    $self->print(Data::Dumper::Dumper(@_));
    return $self;
} 

package main;
use Test::More tests => 5;
use IO_All_Test;

IO::Dumper->import;

my $hash = {
    red => 'square',
    yellow => 'circle',
    pink => 'triangle',
};

die if -f 't/output/dump1';
my $io = io('t/output/dump1')->file->dump($hash);
ok(-f 't/output/dump1');
ok($io->close);
ok(-s 't/output/dump1');

my $VAR1;
my $a = do 't/output/dump1';
my $b = eval join('',<DATA>);
is_deeply($a,$b);

ok($io->unlink);

__END__
$VAR1 = {
  'pink' => 'triangle',
  'red' => 'square',
  'yellow' => 'circle'
};
