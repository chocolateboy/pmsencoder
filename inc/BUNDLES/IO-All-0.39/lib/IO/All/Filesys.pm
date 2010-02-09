package IO::All::Filesys;
use strict;
use warnings;
use IO::All::Base -base;
use Fcntl qw(:flock);

sub absolute {
    my $self = shift;
    $self->pathname(File::Spec->rel2abs($self->pathname))
      unless $self->is_absolute;
    $self->is_absolute(1);
    return $self;
}

sub exists { my $self = shift; -e $self->name }

sub filename {
    my $self = shift;
    my $filename;
    (undef, undef, $filename) = $self->splitpath;
    return $filename;
}

sub is_absolute {
    my $self = shift;
    return *$self->{is_absolute} = shift if @_;
    return *$self->{is_absolute} 
      if defined *$self->{is_absolute};
    *$self->{is_absolute} = IO::All::is_absolute($self) ? 1 : 0;
}

sub is_executable { my $self = shift; -x $self->name }
sub is_readable { my $self = shift; -r $self->name }
sub is_writable { my $self = shift; -w $self->name }
{
    no warnings 'once';
    *is_writeable = \&is_writable;
}

sub pathname {
    my $self = shift;
    return *$self->{pathname} = shift if @_;
    return *$self->{pathname} if defined *$self->{pathname};
    return $self->name;
}

sub relative {
    my $self = shift;
    $self->pathname(File::Spec->abs2rel($self->pathname))
      if $self->is_absolute;
    $self->is_absolute(0);
    return $self;
}

sub rename {
    my $self = shift;
    my $new = shift;
    rename($self->name, "$new")
      ? UNIVERSAL::isa($new, 'IO::All')
        ? $new
        : $self->constructor->($new)
      : undef;
}

sub set_lock {
    my $self = shift;
    return unless $self->_lock;
    my $io_handle = $self->io_handle;
    my $flag = $self->mode =~ /^>>?$/
    ? LOCK_EX
    : LOCK_SH;
    flock $io_handle, $flag;
}

sub stat {
    my $self = shift;
    return IO::All::stat($self, @_)
      if $self->is_open;
      CORE::stat($self->pathname);
}

sub touch {
    my $self = shift;
    $self->utime;
}

sub unlock {
    my $self = shift;
    flock $self->io_handle, LOCK_UN
      if $self->_lock;
}

sub utime {
    my $self = shift;
    my $atime = shift;
    my $mtime = shift;
    $atime = time unless defined $atime;
    $mtime = $atime unless defined $mtime;
    utime($atime, $mtime, $self->name);
    return $self;
}

=encoding utf8

=head1 NAME 

IO::All::Filesys - File System Methods Mixin for IO::All

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
