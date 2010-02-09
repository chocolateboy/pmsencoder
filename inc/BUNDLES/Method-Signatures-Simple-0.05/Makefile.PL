
use strict;
use warnings;

use ExtUtils::MakeMaker;

WriteMakefile(
  DISTNAME  => 'Method-Signatures-Simple',
  NAME      => 'Method::Signatures::Simple',
  AUTHOR    => 'Rhesa\ Rozendaal\ \<rhesa\@cpan\.org\>',
  ABSTRACT  => 'Basic method declarations with signatures, without source filters',
  VERSION   => '0.05',
  EXE_FILES => [ qw() ],
  (eval { ExtUtils::MakeMaker->VERSION(6.21) } ? (LICENSE => 'perl') : ()),
  PREREQ_PM    => {
    "Devel::Declare" => '0.003001',
  },
);
