#!/usr/bin/env perl

use Modern::Perl;
use IO::All;

for my $file (io('src/test/resources')->readdir()) {
    say "searching for $file";
    my @count = qx{grep -rlwF '$file' src/test/groovy};
    say 'count: ', scalar(@count);
}
