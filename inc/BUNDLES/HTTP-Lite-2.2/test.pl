#!/usr/bin/perl

#
# HTTP::Lite - test.pl
#
# $Id: test.pl,v 1.5 2002/06/13 04:56:30 rhooper Exp rhooper $
#

# Before `make install' is performed this script should be runnable with
# `make test'. After `make install' it should work as `perl test.pl'

######################### We start with some black magic to print on failure.

# Change 1..1 below to 1..last_test_to_print .
# (It may become useful if the test is moved to ./t subdirectory.)

BEGIN { $| = 1; print "1..27\n"; }
END { print "not ok 1\n" unless $loaded; }
use HTTP::Lite;
$loaded = 1;

print "ok 1\n";

######################### End of black magic.

# Insert your test code below (better if it prints "ok 13"
# (correspondingly "not ok 13") depending on the success of chunk 13
# of the test code):

#print STDERR <<EOF;
#
#This is HTTP::Lite $HTTP::Lite::VERSION.
#
#This module requires either an internet connection, or access to an Apache
#1.3 server with Perl and the CGI module instaled.
#
#If you wish to perform tests on a local server, you must copy the contents
#of the test-data directory to the apache server (which may be local).  You
#must be using the 'AddHandler cgi-script .cgi' directive in order for tests
#to be successful, as one many tests currently requires a CGI script.
#
#What is the full URL for the above?  Enter 'none' to skip tests.
#EOF
#print STDERR "Location: [http://www.thetoybox.org/HTTP-Lite-Tests] ";
$testpath = 'none';
chomp($testpath);
$testpath = $testpath ? $testpath : "http://www.thetoybox.org/HTTP-Lite-Tests";

if ( $testpath =~ /\s*'*none'*\s*/ ) {

    # print STDERR "skipping all tests\n";
    $skip = 1;
}
else {
    print STDERR <<EOF;


HTTP::Lite now supports HTTP/1.0 or 1.1 Proxies.  

Enter the URL or hostname of the proxy server to use for testing.  Enter
'none' if you do not have a proxy server suitable for testing.
EOF
    print STDERR "Proxy: [none] ";
    $proxy = <>;

    chomp($proxy);
    $proxy = $proxy ? $proxy : "none";

    if ( $proxy =~ /\s*'*none'*\s*/ ) {

        # print STDERR "skipping proxy testing\n";
        $skipproxy = 1;
    }

}

$http = new HTTP::Lite;

#$http->{DEBUG} = 1;

$http->http11_mode(1);

# print "\n\n";
$testno = 2;

if ( !$skip ) {

    $url = "$testpath/test.txt";
    $res = $http->request($url);
    print "not " if !defined($res);
    print "ok $testno $url\n";
    $testno++;
    $doc = $http->body;
    print "not " if $doc ne "OK\n";
    print "ok $testno $url\n";
    $testno++;

    $http->reset;
    $url = "http://invalidhost.thetoybox.org/";
    $res = $http->request($url);
    print "not " if defined($res);
    print "ok $testno $url\n";
    $testno++;

    $http->reset;
    $url = "http://localhost:99999/";
    $res = $http->request($url);
    print "not " if defined($res);
    print "ok $testno $url\n";
    $testno++;

    $http->reset;
    %vars = (
        "a" => "abc",
        "b" => "hello world&",
    );
    $http->prepare_post( \%vars );
    $url = "$testpath/post.cgi";
    $res = $http->request($url);
    print "not " if !defined($res);
    print "ok $testno $url\n";
    $testno++;
    $doc = $http->body;
    print "not " if $doc ne "a=abc\nb=hello world&\n";
    print "ok $testno $url\n";
    $testno++;

    $http->reset;
    $url = "$testpath/chunked.cgi";
    $res = $http->request($url);
    $doc = $http->body;
    print "not " if length($doc) != 28;
    print "ok $testno $url\n";
    $testno++;
    print "not " if $doc ne "chunk1\nchunk2\nchunk3\nchunk4\n";
    print "ok $testno $url\n";
    $testno++;

    $http->reset;
    $url = "$testpath/chunked2.cgi";
    $res = $http->request($url);
    $doc = $http->body;
    print "not " if length($doc) != 26;
    print "ok $testno $url\n";
    $testno++;
    print "not " if $doc ne "chunk1\nchunk2\nchunk3chunk4";
    print "ok $testno $url\n";
    $testno++;

    $http->reset;
    $url = "$testpath/chunked3.cgi";
    $res = $http->request($url);
    $doc = $http->body;
    print "length not " if length($doc) != 34;
    print "ok $testno $url\n";
    $testno++;
    print "not " if $doc ne "chunk1\nchunk2\nchunk3chunk4chunk5\n\n";
    print "ok $testno $url\n";
    $testno++;

    $http->reset;
    $url = "$testpath/unchunked.html";
    $res = $http->request($url);
    $doc = $http->body;
    print "not " if length($doc) != 33;
    print "ok $testno $url\n";
    $testno++;
    print "not " if $doc ne "unchunked1\nunchunked2\nunchunked3\n";
    print "ok $testno $url\n";
    $testno++;

    $http->reset;
    $url = "$testpath/nonl.html";
    $res = $http->request($url);
    $doc = $http->body;
    print "not " if length($doc) != 17;
    print "ok $testno $url\n";
    $testno++;
    print "not " if $doc ne "line1\nline2\nline3";
    print "ok $testno $url\n";
    $testno++;

    $http->http11_mode(0);
    $http->reset;
    $url = "$testpath/nle.html";
    $res = $http->request($url);
    $doc = $http->body;
    print "not " if length($doc) != 19;
    print "ok $testno $url\n";
    $testno++;
    print "not " if $doc ne "line1\nline2\nline3\n\n";
    print "ok $testno $url\n";
    $testno++;
    $http->reset;
    $url = "$testpath/bigbinary.dat";
    $res = $http->request($url);
    $bin = $http->body;
    $http->reset;
    $url = "$testpath/bigbinary.dat.md5";
    $res = $http->request($url);
    chomp( $binsum = $http->body );
    eval "use Digest::MD5 qw(md5_hex);";

    if ($@) {
        print "ok $n (skipping test on this platform)\n";
    }
    else {
        $sum = md5_hex($bin);
        print "not " if $binsum ne $sum;
        print "ok $testno $url\n";
    }
    $testno++;

    $http->http11_mode(1);
    $http->reset;
    $url = "$testpath/nle.html";
    $res = $http->request($url);
    $doc = $http->body;
    print "not " if length($doc) != 19;
    print "ok $testno $url\n";
    $testno++;
    print "not " if $doc ne "line1\nline2\nline3\n\n";
    print "ok $testno $url\n";
    $testno++;
    $http->reset;
    $url = "$testpath/bigbinary.dat";
    $res = $http->request($url);
    $bin = $http->body;
    $http->reset;
    $url = "$testpath/bigbinary.dat.md5";
    $res = $http->request($url);
    chomp( $binsum = $http->body );
    eval "use Digest::MD5 qw(md5_hex);";

    if ($@) {
        print "ok $n (skipping test on this platform)\n";
    }
    else {
        $sum = md5_hex($bin);
        print "not " if $binsum ne $sum;
        print "ok $testno $url\n";
    }
    $testno++;

    $http->reset;
    $url     = "$testpath/bigtest.txt";
    $res     = $http->request($url);
    $bigtest = $http->body;

    $http->reset;
    $url = "$testpath/chunked4.cgi";
    $res = $http->request($url);
    $doc = $http->body;
    print "not " if $doc ne "$bigtest$bigtest${bigtest}chunk4chunk5\n";
    print "ok $testno $url\n";
    $testno++;

    $http->reset;
    $url = "$testpath/chunked5.cgi";
    $res = $http->request($url);
    $doc = $http->body;
    print "not " if $doc ne "$bigtest$bin${bigtest}chunk4chunk5\n";
    print "ok $testno $url\n";
    $testno++;

    # Callback test #1 - Unmodified callback
    sub callback1 {
        my ( $self, $dataref, $cbargs ) = @_;
        $cbbytes += length($$dataref);
        return $dataref;
    }

    $http->reset;
    $url = "$testpath/bigbinary.dat";
    $res = $http->request( $url, \&callback1 );
    $doc = $http->body;
    print "not " if $doc ne $bin;
    print "ok $testno $url\n";
    $testno++;
    print "not " if length($bin) != $cbbytes;
    print "ok $testno $url\n";
    $testno++;

    # Callback test #2 - Discard
    sub callback2 {
        my ( $self, $dataref, $cbargs ) = @_;
        $cbbytes += length($$dataref);
        return undef;
    }

    $http->reset;
    $url = "$testpath/bigbinary.dat";
    $res = $http->request( $url, \&callback2 );
    $doc = $http->body;
    print "not " if defined($doc);
    print "ok $testno $url\n";
    $testno++;

    # Callback test #3 - New callback syntax
    sub callback3 {
        my ( $self, $mode, $dataref, @args ) = @_;
        $cbbytes += length($$dataref);

        #  print STDERR "callback for $mode data is $dataref args are @args\n";
        return $dataref;
    }

    $http->reset;
    $url = "$testpath/bigbinary.dat";
    $http->set_callback( \&callback3, "arg1", "arg2", "arg3", ["arg4"] );
    $res = $http->request($url);
    $doc = $http->body;
    print "not " if $doc ne $bin;
    print "ok $testno $url\n";
    $testno++;

}
else {
    for ( $n = $testno ; $n < 20 ; $n++ ) {
        print "ok $n (skipping test on this platform)\n";
    }
    $testno = $n;
}

unless ( $skip || $skipproxy ) {
    $http->reset;
    $http->proxy($proxy);
    $url = "$testpath/test.txt";
    $res = $http->request($url);
    $doc = $http->body;
    print "not " if length($doc) != 3;
    print "ok $testno $url\n";
    $testno++;
    print "not " if $doc ne "OK\n";
    print "ok $testno $url\n";
    $testno++;
}
else {
    print "ok $testno (skipping test on this platform)\n";
    $testno++;
    print "ok $testno (skipping test on this platform)\n";
}
