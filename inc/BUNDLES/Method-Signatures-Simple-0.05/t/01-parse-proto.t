
use strict;
use warnings;

use Test::More tests => 10;

my $mss = 'Method::Signatures::Simple';
use_ok $mss;
my $inst = $mss->new(invocant => '$self');

my @tests = (
    [''               => [ qr'my \$self = shift;'                                   ]],
    ['$class: %opts'  => [ qr'my \$class = shift;', qr'my \(\%opts\) = \@_;'        ]],
    ['@stuff'         => [ qr'my \$self = shift;',  qr'my \(\@stuff\) = \@_;'       ]],
    ['$foo, $bar'     => [ qr'my \$self = shift;',  qr'my \(\$foo, \$bar\) = \@_;'  ]],
    ["$/foo, $/bar$/" => [ qr'my \$self = shift;',  qr'my \(foo, bar\) = \@_;'      ]],
);

for my $t (@tests) {
    my $p = $inst->parse_proto($t->[0]);
    for my $match (@{$t->[1]}) {
        like $p, $match; # , "$t->[0] matches $match";
    }
}

