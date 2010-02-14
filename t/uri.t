#!/usr/bin/env perl

use strict;
use warnings;

use FindBin qw($Bin);
use Test::Command tests => 2;

my $pmsencoder = "$Bin/../bin/pmsencoder";

stderr_like(
    "$pmsencoder foo bar",
    qr{\bERROR: multiple URIs are not currently supported: \['foo','bar'\]}
);

stderr_like(
    "$pmsencoder foo -o foo",
    qr{\bERROR: ambiguous URIs are not currently supported: 'foo' found at multiple indices: \[0,2\]}
);
