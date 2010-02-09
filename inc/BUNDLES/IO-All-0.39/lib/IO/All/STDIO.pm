package IO::All::STDIO;
use strict;
use warnings;
use IO::All -base;
use IO::File;

const type => 'stdio';

sub stdio {
    my $self = shift;
    bless $self, __PACKAGE__;
    return $self->_init;
}

sub stdin {
    my $self = shift;
    $self->open('<');
    return $self;
}

sub stdout {
    my $self = shift;
    $self->open('>');
    return $self;
}

sub stderr {
    my $self = shift;
    $self->open_stderr;
    return $self;
}

sub open {
    my $self = shift;
    $self->is_open(1);
    my $mode = shift || $self->mode || '<';
    my $fileno = $mode eq '>'
    ? fileno(STDOUT)
    : fileno(STDIN);
    $self->io_handle(IO::File->new);
    $self->io_handle->fdopen($fileno, $mode);
    $self->set_binmode;
}

sub open_stderr {
    my $self = shift;
    $self->is_open(1);
    $self->io_handle(IO::File->new);
    $self->io_handle->fdopen(fileno(STDERR), '>') ? $self : 0;
}

# XXX Add overload support

=encoding utf8

=head1 NAME 

IO::All::STDIO - STDIO Support for IO::All

=head1 SYNOPSIS

See L<IO::All>.

=head1 DESCRIPTION

=head1 AUTHOR

Ingy döt Net <ingy@cpan.org>

=head1 COPYRIGHT

Copyright (c) 2004. Brian Ingerson.

Copyright (c) 2006, 2008. Ingy döt Net.

This program is free software; you can redistribute it and/or modify it
under the same terms as Perl itself.

See http://www.perl.com/perl/misc/Artistic.html

=cut

1;
