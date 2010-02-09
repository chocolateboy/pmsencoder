#!perl -T

use Test::More tests => 1;

BEGIN {
	use_ok( 'Method::Signatures::Simple' );
}

diag( "Testing Method::Signatures::Simple $Method::Signatures::Simple::VERSION, Perl $], $^X" );
