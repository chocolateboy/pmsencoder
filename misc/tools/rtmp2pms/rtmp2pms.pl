#!/usr/bin/env perl

# rtmp2pms.pl -u: convert rtmpdump command lines to PMSEncoder rtmpdump:// URIs
# rtmp2pms.pl -p: convert simple text files with stream names, (optional) thumbnail URLs
#              and rtmpdump commands to PMS WEB.conf lines (see news.txt)
#
# usage:
#
#     rtmp2pms.pl -p news.txt ...
#
# or:
#
#     cat news.txt | rtmp2pms.pl -p
#
# or:
#
#     rtmp2pms.pl -u file1 file2 ...
#
# or:
#
#     cat command_lines.txt | rtmp2pms.pl -u

use strict;
use warnings;

use Getopt::Long qw(:config posix_default no_ignore_case bundling);
use URI::Escape qw(uri_escape);

our ($PMS, $URI);
our $FOLDER = 'Web,Live News';

{
    my ($name, $thumb);

    sub pms($) {
        my $line = shift;
        return unless ($line =~ /^(name|command|thumb):\s*(.+$)/);

        if ($1 eq 'name') {
            $name = $2;
        } elsif ($1 eq 'thumb') {
            $thumb = $2;
        } else {
            my $uri = uri($2);
            $thumb = $thumb ? ",$thumb" : '';
            print "videostream.$FOLDER=$name,$uri$thumb", $/;
        }
    }
}

sub param_escape($) {
    my $value = shift;
    my $escaped = uri_escape($value);
    $escaped =~ s{,}{%3D}g; # commas are special characters in WEB.conf (used to separate path components) so escape them
    return $escaped;
}

sub uri($) {
    my $command = shift;
    return unless ($command =~ s{^.*?rtmpdump(?:\.exe)?\s+}{}i);
    $command =~ s{\s+$}{}; # strip trailing space
    $command =~ s{(--?[a-zA-Z]+)\s+(?!-)(?:(?:"([^"]+)")|(?:'([^']+)')|(\S+))}{"$1=" . param_escape($+)}eg; # escape parameter values
    $command =~ s{\s+}{&}g; # replace spaces between parameters with &
    return "rtmpdump://rtmp2pmspl?$command";
}

GetOptions(
    'pms|p'      => \$PMS,
    'uri|u'      => \$URI,
    'folder|f=s' => \$FOLDER,
);

while (<>) {
    chomp;

    if ($PMS) {
        pms($_);
    } elsif ($URI) {
        print uri($_), $/;
    }
}
