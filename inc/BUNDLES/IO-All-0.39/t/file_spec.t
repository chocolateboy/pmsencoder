use lib 't', 'lib';
use strict;
use warnings;
use Test::More tests => 27;
use IO::All;
use IO_All_Test;

is(io('././t/file_spec.t')->canonpath, f 't/file_spec.t');
is(io('././t/bogus')->canonpath, f 't/bogus');
is(join(';', grep {! /CVS|\.svn/} io->catdir(qw(t mydir))->all), f 't/mydir/dir1;t/mydir/dir2;t/mydir/file1;t/mydir/file2;t/mydir/file3');
test_file_contents(io->catfile(qw(t mystuff))->scalar, 't/mystuff');
test_file_contents(io->join(qw(t mystuff))->scalar, 't/mystuff');
is(ref(io->devnull), 'IO::All::File');
ok(io->devnull->print('IO::All'));
# Not supporting class calls anymore. Objects only.
# ok(IO::All->devnull->print('IO::All'));
ok(io->rootdir->is_dir);
ok(io->tmpdir->is_dir);
ok(io->updir->is_dir);
like(io->case_tolerant, qr/^[01]$/);
ok(io('/foo/bar')->is_absolute);
ok(not io('foo/bar')->is_absolute);
my @path1 = io->path;
shift @path1 if $path1[0]->name eq '.';
my $path2 = $ENV{PATH};
$path2 =~ s/^\.[;:]//;
is(scalar(@path1), scalar(
    @{[split((($^O eq 'MSWin32') ? ';' : ':'), $path2)]}));
my ($v, $d, $f) = io('foo/bar')->splitpath;
is($d, 'foo/');
is($f, 'bar');
my @dirs = io('foo/bar/baz')->splitdir;
is(scalar(@dirs), 3);
is(join('+', @dirs), 'foo+bar+baz');
test_file_contents(io->catpath('', qw(t mystuff))->scalar, 't/mystuff');
is(io('/foo/bar/baz')->abs2rel('/foo'), f 'bar/baz');
is(io('foo/bar/baz')->rel2abs('/moo'), f '/moo/foo/bar/baz');

is(io->dir('doo/foo')->catdir('goo', 'hoo'), f 'doo/foo/goo/hoo');
is(io->dir->catdir('goo', 'hoo'), f 'goo/hoo');
is(io->catdir('goo', 'hoo'), f 'goo/hoo');

is(io->file('doo/foo')->catfile('goo', 'hoo'), f 'doo/foo/goo/hoo');
is(io->file->catfile('goo', 'hoo'), f 'goo/hoo');
is(io->catfile('goo', 'hoo'), f 'goo/hoo');
