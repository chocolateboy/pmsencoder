use 5.10.0;
use strict;
use warnings;

use ExtUtils::MakeMaker;

WriteMakefile(
    NAME         => 'App::PS3MEncoder',
    VERSION_FROM => 'lib/App/PS3MEncoder.pm',
    PREREQ_PM    => {
        'IO::All'         => 0,
        'List::MoreUtils' => 0,
        'LWP::Simple'     => 0,
        'IPC::Cmd'        => '0.54',
    },
    (
        $] >= 5.005
        ? (
            ABSTRACT_FROM => 'lib/App/PS3MEncoder.pm',
            AUTHOR        => 'chocolateboy <chocolate@cpan.org>'
          )
        : ()
    ),
    ($ExtUtils::MakeMaker::VERSION >= 6.31 ? (LICENSE => 'artistic_2') : ()),
);
