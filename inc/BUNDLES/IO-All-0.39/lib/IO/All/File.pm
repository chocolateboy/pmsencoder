package IO::All::File;
use strict;
use warnings;
use IO::All::Filesys -base;
use IO::All -base;
use IO::File;

#===============================================================================
const type => 'file';
field tied_file => undef;

#===============================================================================
sub file {
    my $self = shift;
    bless $self, __PACKAGE__;
    $self->name(shift) if @_;
    return $self->_init;
}

sub file_handle {
    my $self = shift;
    bless $self, __PACKAGE__;
    $self->_handle(shift) if @_;
    return $self->_init;
}

#===============================================================================
sub assert_filepath {
    my $self = shift;
    my $name = $self->pathname
      or return;
    my $directory;
    (undef, $directory) = File::Spec->splitpath($self->pathname);
    $self->assert_dirpath($directory);
}

sub assert_open_backwards {
    my $self = shift;
    return if $self->is_open;
    require File::ReadBackwards;
    my $file_name = $self->pathname;
    my $io_handle = File::ReadBackwards->new($file_name)
      or $self->throw("Can't open $file_name for backwards:\n$!");
    $self->io_handle($io_handle);
    $self->is_open(1);
}

sub assert_open {
    my $self = shift;
    return if $self->is_open;
    $self->mode(shift) unless $self->mode;
    $self->open;
}

sub assert_tied_file {
    my $self = shift;
    return $self->tied_file || do {
        eval {require Tie::File};
        $self->throw("Tie::File required for file array operations:\n$@") 
          if $@;
        my $array_ref = do { my @array; \@array };
        my $name = $self->pathname;
        my @options = $self->_rdonly ? (mode => O_RDONLY) : ();
        push @options, (recsep => "\n");
        tie @$array_ref, 'Tie::File', $name, @options;
        $self->throw("Can't tie 'Tie::File' to '$name':\n$!")
          unless tied @$array_ref;
        $self->tied_file($array_ref);
    };
}

sub open {
    my $self = shift;
    $self->is_open(1);
    $self->assert_filepath if $self->_assert;
    my ($mode, $perms) = @_;
    $self->mode($mode) if defined $mode;
    $self->mode('<') unless defined $self->mode;
    $self->perms($perms) if defined $perms;
    my @args = ($self->mode);
    push @args, $self->perms if defined $self->perms;
    if (defined $self->pathname) {
        $self->io_handle(IO::File->new);
        $self->io_handle->open($self->pathname, @args) 
          or $self->throw($self->open_msg);
    }
    elsif (defined $self->_handle and
           not $self->io_handle->opened
          ) {
        # XXX Not tested
        $self->io_handle->fdopen($self->_handle, @args);
    }
    $self->set_lock;
    $self->set_binmode;
}

my %mode_msg = (
    '>' => 'output',
    '<' => 'input',
    '>>' => 'append',
);
sub open_msg {
    my $self = shift;
    my $name = defined $self->pathname
      ? " '" . $self->pathname . "'"
      : '';
    my $direction = defined $mode_msg{$self->mode}
      ? ' for ' . $mode_msg{$self->mode}
      : '';
    return qq{Can't open file$name$direction:\n$!};
}

#===============================================================================
sub close {
    my $self = shift;
    return unless $self->is_open;
    $self->is_open(0);
    my $io_handle = $self->io_handle;
    $self->unlock;
    $self->io_handle(undef);
    $self->mode(undef);
    if (my $tied_file = $self->tied_file) {
        if (ref($tied_file) eq 'ARRAY') {
            untie @$tied_file;
        }
        else {
            untie %$tied_file;
        }
        $self->tied_file(undef);
        return 1;
    }
    $io_handle->close(@_)
      if defined $io_handle;
    return $self;
}

sub empty {
    my $self = shift;
    -z $self->pathname;
}

sub filepath {
    my $self = shift;
    my ($volume, $path) = $self->splitpath;
    return File::Spec->catpath($volume, $path, '');
}

sub getline_backwards {
    my $self = shift;
    $self->assert_open_backwards;
    return $self->io_handle->readline;
}

sub getlines_backwards {
    my $self = shift;
    my @lines;
    while (defined (my $line = $self->getline_backwards)) {
        push @lines, $line;
    }
    return @lines;
}

sub head {
    my $self = shift;
    my $lines = shift || 10;
    my @return;
    $self->close;
    while ($lines--) {
        push @return, ($self->getline or last);
    }
    $self->close;
    return wantarray ? @return : join '', @return;
}

sub tail {
    my $self = shift;
    my $lines = shift || 10;
    my @return;
    $self->close;
    while ($lines--) {
        unshift @return, ($self->getline_backwards or last);
    }
    $self->close;
    return wantarray ? @return : join '', @return;
}

sub touch {
    my $self = shift;
    return $self->SUPER::touch(@_)
      if -e $self->pathname;
    return $self if $self->is_open;
    my $mode = $self->mode;
    $self->mode('>>')->open->close;
    $self->mode($mode);
    return $self;
}

sub unlink {
    my $self = shift;
    unlink $self->pathname;
}

#===============================================================================
sub overload_table {
    my $self = shift;
    (
        $self->SUPER::overload_table(@_),
        'file > file' => 'overload_file_to_file',
        'file < file' => 'overload_file_from_file',
        '${} file' => 'overload_file_as_scalar',
        '@{} file' => 'overload_file_as_array',
        '%{} file' => 'overload_file_as_dbm',
    )
}

sub overload_file_to_file {
    require File::Copy;
    File::Copy::copy($_[1]->pathname, $_[2]->pathname);
    $_[2];
}

sub overload_file_from_file {
    require File::Copy;
    File::Copy::copy($_[2]->pathname, $_[1]->pathname);
    $_[1];
}

sub overload_file_as_array {
    $_[1]->assert_tied_file;
}

sub overload_file_as_dbm {
    $_[1]->dbm
      unless $_[1]->isa('IO::All::DBM');
    $_[1]->assert_open;
}

sub overload_file_as_scalar {
    my $scalar = $_[1]->scalar;
    return \$scalar;
}

=encoding utf8

=head1 NAME 

IO::All::File - File Support for IO::All

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
