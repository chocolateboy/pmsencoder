#!/usr/bin/env perl

use strict;
use warnings;

use Test::Command tests => 2;

stderr_like(
    [ qw(pmsencoder foo bar) ],
    qr{\bERROR: multiple URIs are not currently supported: \['foo','bar'\]}
);

stderr_like(
    [ qw(pmsencoder foo -o foo) ],
    qr{\bERROR: ambiguous URIs are not currently supported: 'foo' found at multiple indices: \[0,2\]}
);
