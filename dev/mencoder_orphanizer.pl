#!/usr/bin/env perl

use 5.010000;

use strict;
use warnings;

use ExtUtils::MakeMaker qw(prompt);
use File::Temp;
use IPC::Cmd qw(can_run);

# start x y-threaded MEncoders and kill each one with TERM then KILL and see what's left standing

my $MENCODER = can_run('mencoder') || die "can't find mencoder";
my @COMMAND = (
    $MENCODER,
    qw(
        http://www.youtube.com/get_video?fmt=18&video_id=ZOU8GIRUd_g&t=vjVQa1PpcFParmYxwHa8gwqebybHZy9HxS-YI-_najw%3D
        -really-quiet
        -prefer-ipv4
        -oac lavc
        -of lavf
        -lavfopts format=dvd
        -ovc lavc
        -lavcopts vcodec=mpeg2video:vbitrate=4096:threads=1:acodec=ac3:abitrate=128
        -ofps 25
        -cache 16384
        -vf harddup
    )
);

my @PIDS;
my $MENCODERS = shift(@ARGV) || 4;
my $THREADS = shift(@ARGV) || 2;

sub cleanup ($) {
    my $signal = shift;

    print STDERR $/;

    for my $pid (@PIDS) {
        warn "sending $signal signal to $pid", $/;
        kill($signal => $pid) or warn "couldn't send $signal signal to $pid: $!", $/;
    }
}

my $mencoders = ($MENCODERS == 1) ? 'mencoder' : 'mencoders';

die 'threads must be >= 2' unless ($THREADS >= 2);

say "This script will spawn $MENCODERS $mencoders running $THREADS threads each and will *FAIL* ",
    "to kill them cleanly, leaving orphan processes.";

my $continue = prompt(
    "Are you sure you want to do this (and know how to clean up afterwards e.g. killall -TERM mencoder)? Y/n",
    'n'
);

exit 0 unless ($continue =~ /^Y(es)?$/i);

my @TEMP_FILES; # keep these around until the program exits

for my $count (1 .. $MENCODERS) {
    my $pid = fork();

    unless (defined $pid) {
        cleanup 'TERM';
        die "can't fork";
    }

    my $temp_file = File::Temp->new(
        TEMPLATE => 'mencoder_orphanizer_XXXX',
        UNLINK   => 1,
        SUFFIX   => '.mpg',
        TMPDIR   => 1
    );

    push @TEMP_FILES, $temp_file; # these will be cleaned up on exit

    if ($pid) { # parent
        warn "created child: $pid", $/;
        push @PIDS, $pid;
    } else { # child
        warn "process $$: execing @COMMAND", $/; 
        (exec { $MENCODER } @COMMAND, '-o', $temp_file)
            or die "can't exec $MENCODER: $!"; # obscure syntax for bypassing the shell
    }
}

sleep 2;
cleanup('TERM'); # Process.destroy
cleanup('KILL'); # kill -9
