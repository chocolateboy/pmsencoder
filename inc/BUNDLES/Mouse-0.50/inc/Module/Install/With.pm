#line 1
package Module::Install::With;

# See POD at end for docs

use strict;
use Module::Install::Base ();

use vars qw{$VERSION @ISA $ISCORE};
BEGIN {
	$VERSION = '0.93';
	@ISA     = 'Module::Install::Base';
	$ISCORE  = 1;
}





#####################################################################
# Installer Target

# Are we targeting ExtUtils::MakeMaker (running as Makefile.PL)
sub eumm {
	!! ($0 =~ /Makefile.PL$/i);
}

# You should not be using this, but we'll keep the hook anyways
sub mb {
	!! ($0 =~ /Build.PL$/i);
}





#####################################################################
# Testing and Configuration Contexts

#line 49

sub interactive {
	# Treat things interactively ONLY based on input
	!! (-t STDIN and ! automated_testing());
}

#line 67

sub automated_testing {
	!! $ENV{AUTOMATED_TESTING};
}

#line 86

sub release_testing {
	!! $ENV{RELEASE_TESTING};
}

sub author_context {
	!! $Module::Install::AUTHOR;
}





#####################################################################
# Operating System Convenience

#line 114

sub win32 {
	!! ($^O eq 'MSWin32');
}

#line 131

sub winlike {
	!! ($^O eq 'MSWin32' or $^O eq 'cygwin');
}

1;

#line 159
