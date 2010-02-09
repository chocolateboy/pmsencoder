use lib 't', 'lib';
use strict;
use warnings;
use Test::More tests => 1;

use IO::All;
use IO::All::Temp;
use IO::All::String;
use IO::All::Socket;
use IO::All::MLDBM;
use IO::All::Link;
use IO::All::Pipe;
use IO::All::Dir;
use IO::All::Filesys;
use IO::All::File;
use IO::All::DBM;
use IO::All::STDIO;
use IO::All::Base;

is($INC{'XXX.pm'}, undef, "Don't ship with XXX");
