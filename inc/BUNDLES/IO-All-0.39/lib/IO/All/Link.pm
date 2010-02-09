package IO::All::Link;
use strict;
use warnings;
use IO::All::File -base;

const type => 'link';

sub link {
    my $self = shift;
    bless $self, __PACKAGE__;
    $self->name(shift) if @_;
    $self->_init;
}

sub readlink {
    my $self = shift;
    $self->constructor->(CORE::readlink($self->name));
}

sub symlink {
    my $self = shift;
    my $target = shift;
    $self->assert_filepath if $self->_assert;
    CORE::symlink($target, $self->pathname);
}

sub AUTOLOAD {
    my $self = shift;
    our $AUTOLOAD;
    (my $method = $AUTOLOAD) =~ s/.*:://;
    my $target = $self->target;
    unless ($target) {
        $self->throw("Can't call $method on symlink");
        return;
    }
    $target->$method(@_);
}

sub target {
    my $self = shift;
    return *$self->{target} if *$self->{target};
    my %seen;
    my $link = $self;
    my $new;
    while ($new = $link->readlink) {
        my $type = $new->type or return;
        last if $type eq 'file';
        last if $type eq 'dir';
        return unless $type eq 'link';
        return if $seen{$new->name}++;
        $link = $new;
    }
    *$self->{target} = $new;
}

=encoding utf8

=head1 NAME 

IO::All::Link - Symbolic Link Support for IO::All

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
