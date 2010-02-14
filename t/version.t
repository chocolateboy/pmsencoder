#!/usr/bin/env perl

use strict;
use warnings;

use Config;
use FindBin qw($Bin);
use Test::Command;
use Test::More tests => 17;

my $pmsencoder = "$Bin/../bin/pmsencoder";
my $cmd = Test::Command->new(cmd => "$pmsencoder --version");

my $version2    = qr{\d+\.\d+};
my $version3    = qr{\d+\.\d+\.\d+};
my $config_file = qr{.+?\bpmsencoder\.(?:yml|yaml|conf)}i;
my $logfile     = qr{.+?\bpmsencoder\.log}i;
my $mencoder    = qr{.+?\bmencoder$Config{_exe}}i;
my $config_dir  = qr{.+?\.pmsencoder}i;

my %version = (
    pmsencoder            => qr{$version2 \s+ \([^)]+\)}x,
    perl                  => $version3,
    config_file           => $config_file,
    config_file_version   => $version2,
    default_config_file   => $config_file,
    logfile               => $logfile,
    mencoder_path         => $mencoder,
    user_config_directory => $config_dir
);

for my $line (qx{$pmsencoder --version}) {
    chomp $line;
    my ($name, $value) = $line =~ m{^([^:]+):\s+(.+)$};
    $name =~ s{\s+}{_}g;
    ok (exists $version{$name}, "recognized version field: $name");
    like($line, $version{$name}, "valid pattern for $name");
}

$cmd->exit_is_num(0);
