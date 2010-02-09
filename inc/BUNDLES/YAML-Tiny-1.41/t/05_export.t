#!/usr/bin/perl

# Testing of basic document structures

use strict;
BEGIN {
	$|  = 1;
	$^W = 1;
}

use Test::More tests => 6;
use YAML::Tiny;



ok defined &main::Load, 'Load is exported';
ok defined &main::Dump, 'Dump is exported';
ok not(defined &main::LoadFile), 'Load is exported';
ok not(defined &main::DumpFile), 'Dump is exported';

ok \&main::Load == \&YAML::Tiny::Load, 'Load is YAML::Tiny';
ok \&main::Dump == \&YAML::Tiny::Dump, 'Dump is YAML::Tiny';
