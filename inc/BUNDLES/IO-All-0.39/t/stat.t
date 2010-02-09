use lib 't', 'lib';
use strict;
use warnings;
use Test::More tests => 14;
use IO::All;
use IO_All_Test;

my ($dev, $ino, $modes, $nlink, $uid, $gid, $rdev, 
    $size, $atime, $mtime, $ctime, $blksize, $blocks) = stat('t/stat.t');

my $io = io('t/stat.t');
is($io->device, $dev);
is($io->inode, $ino);
is($io->modes, $modes);
is($io->nlink, $nlink);
is($io->uid, $uid);
is($io->gid, $gid);
is($io->device_id, $rdev);
is($io->size, $size);
ok(($io->atime == $atime) || ($io->atime == ($atime+1)));
is($io->mtime, $mtime);
is($io->ctime, $ctime);
is($io->blksize, $blksize);
is($io->blocks, $blocks);

my @stat = $io->stat; 
ok(defined $stat[0]);
