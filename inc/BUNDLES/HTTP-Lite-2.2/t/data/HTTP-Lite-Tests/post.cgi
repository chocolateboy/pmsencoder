#!/usr/bin/perl

use CGI;

$cgi=new CGI;
$a=$cgi->param('a');
$b=$cgi->param('b');
print "Content-type: text/plain\n\n";
print "a=$a\n";
print "b=$b\n";
