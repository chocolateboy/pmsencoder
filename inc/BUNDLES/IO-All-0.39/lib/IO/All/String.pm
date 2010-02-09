package IO::All::String;
use strict;
use warnings;
use IO::All -base;
use IO::String;

const type => 'string';
proxy 'string_ref';

sub string {
    my $self = shift;
    bless $self, __PACKAGE__;
    $self->_init;
}

sub open {
    my $self = shift;
    $self->io_handle(IO::String->new);
    $self->set_binmode;
    $self->is_open(1);
}

=encoding utf8

=head1 NAME 

IO::All::String - String IO Support for IO::All

=head1 SYNOPSIS

See L<IO::All>.

=head1 DESCRIPTION

=head1 AUTHOR

Ingy döt Net <ingy@cpan.org>

=head1 COPYRIGHT

Copyright (c) 2004. Brian Ingerson. All rights reserved.

Copyright (c) 2006, 2008. Ingy döt Net. All rights reserved.

This program is free software; you can redistribute it and/or modify it
under the same terms as Perl itself.

See http://www.perl.com/perl/misc/Artistic.html

=cut

1;
