package IO::All::Temp;
use strict;
use warnings;
use IO::All::File -base;

sub temp {
    my $self = shift;
    bless $self, __PACKAGE__;
    my $temp_file = IO::File::new_tmpfile()
      or $self->throw("Can't create temporary file");
    $self->io_handle($temp_file);
    $self->error_check;
    $self->autoclose(0);
    $self->is_open(1);
    return $self;
}

=encoding utf8

=head1 NAME 

IO::All::Temp - Temporary File Support for IO::All

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
