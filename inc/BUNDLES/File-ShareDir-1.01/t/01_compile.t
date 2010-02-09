#!/usr/bin/perl

# Compile-testing for File::ShareDir

use strict;
BEGIN {
	$|  = 1;
	$^W = 1;
}

use Test::More tests => 2;

ok( $] > 5.005, 'Perl version is 5.004 or newer' );

use_ok( 'File::ShareDir' );
