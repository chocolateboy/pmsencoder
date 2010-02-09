use strict;
BEGIN {
	require 5.003_96;
	$main::VERSION = '1.41';
}
use ExtUtils::MakeMaker;

WriteMakefile1(
	NAME      => 'YAML::Tiny',
	ABSTRACT  => 'Read/Write YAML files with as little code as possible',
	VERSION_FROM   => 'lib/YAML/Tiny.pm',
	BUILD_REQUIRES => {
		'File::Spec' => '0.80',
		'Test::More' => '0.47',
		($ENV{AUTOMATED_TESTING} ? (
			'YAML'       => '0.68',
			'YAML::Perl' => '0.02',
			'YAML::Syck' => '1.07',
			'YAML::XS'   => '0.29',
		) : ()),
	},
	AUTHOR  => 'Adam Kennedy <adamk@cpan.org>',
	LICENSE => 'perl',
	MIN_PERL_VERSION => '5.004',
	META_MERGE => {
		resources => {
			repository => 'http://svn.ali.as/cpan/trunk/YAML-Tiny',
			bugtracker => 'http://rt.cpan.org/NoAuth/ReportBug.html?Queue=YAML-Tiny',
		},
	},
);

sub WriteMakefile1 {  #Written by Alexandr Ciornii, version 0.20
	my %params=@_;
	my $eumm_version=$ExtUtils::MakeMaker::VERSION;
	$eumm_version=eval $eumm_version;
	die "EXTRA_META is deprecated" if exists $params{EXTRA_META};
	die "License not specified" if not exists $params{LICENSE};
	if ($params{BUILD_REQUIRES}) { #and $eumm_version < 6.5503
		#Should be modified in future when EUMM will
		#correctly support BUILD_REQUIRES.
		#EUMM 6.5502 has problems with BUILD_REQUIRES
		$params{PREREQ_PM}={ %{$params{PREREQ_PM} || {}} , %{$params{BUILD_REQUIRES}} };
		delete $params{BUILD_REQUIRES};
	}
	delete $params{CONFIGURE_REQUIRES} if $eumm_version < 6.52;
	delete $params{MIN_PERL_VERSION} if $eumm_version < 6.48;
	delete $params{META_MERGE} if $eumm_version < 6.46;
	delete $params{META_ADD} if $eumm_version < 6.46;
	delete $params{LICENSE} if $eumm_version < 6.31;
	delete $params{AUTHOR} if $] < 5.005;
	delete $params{ABSTRACT_FROM} if $] < 5.005;
	delete $params{BINARY_LOCATION} if $] < 5.005;

	WriteMakefile(%params);
}
