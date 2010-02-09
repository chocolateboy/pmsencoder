use lib 't', 'lib';
use strict;
use warnings;
use Test::More tests => 2;
use IO::All;
use IO_All_Test;
use Cwd;

{
    my $dir = io('t')->chdir;
    is((io(io->curdir->absolute->pathname)->splitdir)[-1], 't');
}
like((io(io->curdir->absolute->pathname)->splitdir)[-1], qr'^IO-All');
