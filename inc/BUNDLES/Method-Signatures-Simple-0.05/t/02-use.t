
use strict;
use warnings;

use Test::More tests => 7;
use_ok 'Method::Signatures::Simple';

{
    package My::Obj;
    use Method::Signatures::Simple;

    method make($class: %opts) {
        bless {%opts}, $class;
    }
    method first : lvalue {
        $self->{first};
    }
    method second {
        $self->first + 1;
    }
    method nth($inc) {
        $self->first + $inc;
    }
}

my $o = My::Obj->make(first => 1);
is $o->first, 1;
is $o->second, 2;
is $o->nth(10), 11;

$o->first = 10;

is $o->first, 10;
is $o->second, 11;
is $o->nth(10), 20;

