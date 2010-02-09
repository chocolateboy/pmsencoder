#!/usr/bin/perl

# Testing of scalar-context calls to the compatibility functions

use strict;
BEGIN {
	$|  = 1;
	$^W = 1;
}

use File::Spec::Functions ':ALL';
use t::lib::Test;
use Test::More;
BEGIN {
	if ( t::lib::Test->have_yamlpm ) {
		plan( tests => 18 );
	} else {
		plan( skip_all => 'Requires YAML.pm' );
		exit(0);
	}
}

use YAML       ();
use YAML::Tiny ();





#####################################################################
# Sample documents

my $one = <<'END_YAML';
---
- foo
END_YAML

my $two = <<'END_YAML';
---
- foo
---
- bar
END_YAML





#####################################################################
# Match Listwise Behaviour

SCOPE: {
	my $one_list_pm   = [ YAML::Load( $one ) ];
	my $two_list_pm   = [ YAML::Load( $two ) ];
	my $one_list_tiny = [ YAML::Tiny::Load( $one ) ];
	my $two_list_tiny = [ YAML::Tiny::Load( $two ) ];

	is_deeply( $one_list_pm, [ [ 'foo' ] ],  'one: Parsed correctly'     );
	is_deeply( $one_list_pm, $one_list_tiny, 'one: List context matches' );

	is_deeply( $two_list_pm, [ [ 'foo' ], [ 'bar' ] ], 'two: Parsed correctly'     );
	is_deeply( $two_list_pm, $two_list_tiny,           'two: List context matches' );
}





#####################################################################
# Match Scalar Behaviour

SCOPE: {
	my $one_scalar_pm   = YAML::Load( $one );
	my $two_scalar_pm   = YAML::Load( $two );
	my $one_scalar_tiny = YAML::Tiny::Load( $one );
	my $two_scalar_tiny = YAML::Tiny::Load( $two );

	is_deeply( $one_scalar_pm, [ 'foo' ],        'one: Parsed correctly'       );
	is_deeply( $one_scalar_pm, $one_scalar_tiny, 'one: Scalar context matches' );

	is_deeply( $two_scalar_pm, [ 'bar' ],        'two: Parsed correctly'       );
	is_deeply( $two_scalar_pm, $two_scalar_tiny, 'two: Scalar context matches' );
}





#####################################################################
# Repeat for LoadFile

my $one_file = catfile(qw{ t data one.yml });
my $two_file = catfile(qw{ t data two.yml });
ok( -f $one_file, "Found $one_file" );
ok( -f $two_file, "Found $two_file" );
SCOPE: {
	my $one_list_pm   = [ YAML::LoadFile( $one_file ) ];
	my $two_list_pm   = [ YAML::LoadFile( $two_file ) ];
	my $one_list_tiny = [ YAML::Tiny::LoadFile( $one_file ) ];
	my $two_list_tiny = [ YAML::Tiny::LoadFile( $two_file ) ];

	is_deeply( $one_list_pm, [ [ 'foo' ] ],  'one: Parsed correctly'     );
	is_deeply( $one_list_pm, $one_list_tiny, 'one: List context matches' );

	is_deeply( $two_list_pm, [ [ 'foo' ], [ 'bar' ] ], 'two: Parsed correctly'     );
	is_deeply( $two_list_pm, $two_list_tiny,           'two: List context matches' );
}

SCOPE: {
	my $one_scalar_pm   = YAML::LoadFile( $one_file );
	my $two_scalar_pm   = YAML::LoadFile( $two_file );
	my $one_scalar_tiny = YAML::Tiny::LoadFile( $one_file );
	my $two_scalar_tiny = YAML::Tiny::LoadFile( $two_file );

	is_deeply( $one_scalar_pm, [ 'foo' ],        'one: Parsed correctly'       );
	is_deeply( $one_scalar_pm, $one_scalar_tiny, 'one: Scalar context matches' );

	is_deeply( $two_scalar_pm, [ 'bar' ],        'two: Parsed correctly'       );
	is_deeply( $two_scalar_pm, $two_scalar_tiny, 'two: Scalar context matches' );
}
