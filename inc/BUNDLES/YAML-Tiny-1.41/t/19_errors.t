#!/usr/bin/perl

# Testing documents that should fail

use strict;
BEGIN {
	$|  = 1;
	$^W = 1;
}

use File::Spec::Functions ':ALL';
use t::lib::Test;
use Test::More tests => 1;
use YAML::Tiny ();





#####################################################################
# Missing Features

# We don't support raw nodes
yaml_error( <<'END_YAML', 'does not support a feature' );
---
version: !!perl/hash:version 
  original: v2.0.2
  qv: 1
  version: 
    - 2
    - 0
    - 2
END_YAML
