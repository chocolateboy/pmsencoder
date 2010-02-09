use lib 't', 'lib';
use strict;
use warnings;
use Test::More tests => 23;
use IO_All_Test;
use IO::All;

unlink('t/output/overload1');
unlink('t/output/overload2');
unlink('t/output/overload3');

my $data < io('t/mystuff');
test_file_contents($data, 't/mystuff');
my $data1 = $data;
my $data2 = $data . $data;
$data << io('t/mystuff');
is($data, $data2);
$data < io('t/mystuff');
is($data, $data1);

io('t/mystuff') > $data;
test_file_contents($data, 't/mystuff');
io('t/mystuff') >> $data;
is($data, $data2);
io('t/mystuff') > $data;
is($data, $data1);

$data > io('t/output/overload1');
test_file_contents($data, 't/output/overload1');
$data > io('t/output/overload1');
test_file_contents($data, 't/output/overload1');
$data >> io('t/output/overload1');
test_file_contents($data2, 't/output/overload1');

io('t/output/overload1') < $data;
test_file_contents($data, 't/output/overload1');
io('t/output/overload1') < $data;
test_file_contents($data, 't/output/overload1');
io('t/output/overload1') << $data;
test_file_contents($data2, 't/output/overload1');

$data > io('t/output/overload1');
test_file_contents($data, 't/output/overload1');
io('t/output/overload1') > io('t/output/overload2');
test_matching_files('t/output/overload1', 't/output/overload2');
io('t/output/overload3') < io('t/output/overload2');
test_matching_files('t/output/overload1', 't/output/overload3');
io('t/output/overload3') << io('t/output/overload2');
io('t/output/overload1') >> io('t/output/overload2');
test_matching_files('t/output/overload2', 't/output/overload3');
test_file_contents($data2, 't/output/overload3');

is(io('foo') . '', 'foo');

is("@{io 't/mydir'}", 
   flip_slash 
     't/mydir/dir1 t/mydir/dir2 t/mydir/file1 t/mydir/file2 t/mydir/file3',
);

is(join(' ', sort keys %{io 't/mydir'}), 
   'dir1 dir2 file1 file2 file3',
);

is(join(' ', sort map {"$_"} values %{io 't/mydir'}), 
   flip_slash
     't/mydir/dir1 t/mydir/dir2 t/mydir/file1 t/mydir/file2 t/mydir/file3',
);

${io('t/mystuff')} . ${io('t/mystuff')} > io('t/output/overload1');
test_file_contents2('t/output/overload1', $data2);

${io('t/mystuff')} . "xxx\n" . ${io('t/mystuff')} > io('t/output/overload1');
$data < io('t/mystuff');
my $cat3 = $data . "xxx\n" . $data;
test_file_contents2('t/output/overload1', $cat3);
