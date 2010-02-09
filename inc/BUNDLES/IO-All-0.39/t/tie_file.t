use lib 't', 'lib';
use strict;
use warnings;
use Test::More;
use IO_All_Test;
use IO::All;

plan((eval {require Tie::File; 1})
    ? (tests => 2)
    : (skip_all => "requires Tie::File")
);

(io('t/output/tie_file1') < io('t/tie_file.t'))->close;
my $file = io('t/output/tie_file1')->rdonly;
is($file->[-1], 'bar');
is($file->[-2], 'foo');

"foo\n" x 3 > io('t/output/tie_file2');
io('t/output/tie_file2')->[1] = 'bar';


__END__
foo
bar
