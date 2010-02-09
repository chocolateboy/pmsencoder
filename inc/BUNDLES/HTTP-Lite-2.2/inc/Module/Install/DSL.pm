#line 1
package Module::Install::DSL;

use strict;
use vars qw{$VERSION $ISCORE};
BEGIN {
	$VERSION = '0.91';
	$ISCORE  = 1;
	*inc::Module::Install::DSL::VERSION = *VERSION;
	@inc::Module::Install::DSL::ISA     = __PACKAGE__;
}

sub import {
	# Read in the rest of the Makefile.PL
	open 0 or die "Couldn't open $0: $!";
	my $dsl;
	SCOPE: {
		local $/ = undef;
		$dsl = join "", <0>;
	}

	# Change inc::Module::Install::DSL to the regular one.
	# Remove anything before the use inc::... line.
	$dsl =~ s/.*?^\s*use\s+(?:inc::)?Module::Install::DSL(\b[^;]*);\s*\n//sm;

	# Load inc::Module::Install as we would in a regular Makefile.Pl
	SCOPE: {
		package main;
		require inc::Module::Install;
		inc::Module::Install->import;
	}

	# Add the ::DSL plugin to the list of packages in /inc
	my $admin = $Module::Install::MAIN->{admin};
	if ( $admin ) {
		my $from = $INC{"$admin->{path}/DSL.pm"};
		my $to   = "$admin->{base}/$admin->{prefix}/$admin->{path}/DSL.pm";
		$admin->copy( $from => $to );
	}

	# Convert the basic syntax to code
	my $code = "package main;\n\n"
	         . dsl2code($dsl)
	         . "\n\nWriteAll();\n";

	# Execute the script
	eval $code;
	print STDERR "Failed to execute the generated code" if $@;

	exit(0);
}

sub dsl2code {
	my $dsl = shift;

	# Split into lines and strip blanks
	my @lines = grep { /\S/ } split /[\012\015]+/, $dsl;

	# Each line represents one command
	my @code = ();
	foreach my $line ( @lines ) {
		# Split the lines into tokens
		my @tokens = split /\s+/, $line;

		# The first word is the command
		my $command = shift @tokens;
		my @params  = ();
		my @suffix  = ();
		while ( @tokens ) {
			my $token = shift @tokens;
			if ( $token eq 'if' or $token eq 'unless' ) {
				# This is the beginning of a suffix
				push @suffix, $token;
				push @suffix, @tokens;
				last;
			} else {
				# Convert to a string
				$token =~ s/([\\\'])/\\$1/g;
				push @params, "'$token'";
			}	
		};

		# Merge to create the final line of code
		@tokens = ( $command, @params ? join( ', ', @params ) : (), @suffix );
		push @code, join( ' ', @tokens ) . ";\n";
	}

	# Join into the complete code block
	return join( '', @code );
}

1;
