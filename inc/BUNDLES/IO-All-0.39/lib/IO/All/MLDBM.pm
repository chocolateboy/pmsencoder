package IO::All::MLDBM;
use strict;
use warnings;
use IO::All::DBM -base;

field _serializer => 'Data::Dumper';

sub mldbm {
    my $self = shift;
    bless $self, __PACKAGE__;    
    my ($serializer) = grep { /^(Storable|Data::Dumper|FreezeThaw)$/ } @_;
    $self->_serializer($serializer) if defined $serializer;
    my @dbm_list = grep { not /^(Storable|Data::Dumper|FreezeThaw)$/ } @_;
    $self->_dbm_list([@dbm_list]);
    return $self;
}

sub tie_dbm {
    my $self = shift;
    my $filename = $self->name;
    my $dbm_class = $self->_dbm_class;
    my $serializer = $self->_serializer;
    eval "use MLDBM qw($dbm_class $serializer)";
    $self->throw("Can't open '$filename' as MLDBM:\n$@") if $@;
    my $hash;
    my $db = tie %$hash, 'MLDBM', $filename, $self->mode, $self->perms, 
        @{$self->_dbm_extra}
      or $self->throw("Can't open '$filename' as MLDBM file:\n$!");
    $self->add_utf8_dbm_filter($db)
      if $self->_utf8;
    $self->tied_file($hash);
}

=encoding utf8

=head1 NAME 

IO::All::MLDBM - MLDBM Support for IO::All

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
