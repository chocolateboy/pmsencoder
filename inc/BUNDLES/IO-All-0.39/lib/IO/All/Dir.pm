package IO::All::Dir;
use strict;
use warnings;
use IO::All::Filesys -base;
use IO::All -base;
use IO::Dir;

#===============================================================================
const type => 'dir';
option 'sort' => 1;
chain filter => undef;
option 'deep';
field 'chdir_from';

#===============================================================================
sub dir {
    my $self = shift;
    bless $self, __PACKAGE__;
    $self->name(shift) if @_;
    return $self->_init;
}

sub dir_handle {
    my $self = shift;
    bless $self, __PACKAGE__;
    $self->_handle(shift) if @_;
    return $self->_init;
}

#===============================================================================
sub assert_open {
    my $self = shift;
    return if $self->is_open;
    $self->open;
}

sub open {
    my $self = shift;
    $self->is_open(1);
    $self->assert_dirpath($self->pathname)
      if $self->pathname and $self->_assert;
    my $handle = IO::Dir->new;
    $self->io_handle($handle);
    $handle->open($self->pathname)
      or $self->throw($self->open_msg);
    return $self;
}

sub open_msg {
    my $self = shift;
    my $name = defined $self->pathname
      ? " '" . $self->pathname . "'"
      : '';
    return qq{Can't open directory$name:\n$!};
}

#===============================================================================
sub All {
    my $self = shift;
    $self->all(0);
}

sub all {
    my $self = shift;
    my $depth = @_ ? shift(@_) : $self->_deep ? 0 : 1;
    my $first = not @_;
    my @all;
    while (my $io = $self->next) {
        push @all, $io;
        push(@all, $io->all($depth - 1, 1))
          if $depth != 1 and $io->is_dir;
    }
    @all = grep {&{$self->filter}} @all
      if $self->filter;
    return @all unless $first and $self->_sort;
    return sort {$a->pathname cmp $b->pathname} @all;
}

sub All_Dirs {
    my $self = shift;
    $self->all_dirs(0);
}

sub all_dirs {
    my $self = shift;
    grep {$_->is_dir} $self->all(@_);
}

sub All_Files {
    my $self = shift;
    $self->all_files(0);
}

sub all_files {
    my $self = shift;
    grep {$_->is_file} $self->all(@_);
}

sub All_Links {
    my $self = shift;
    $self->all_links(0);
}

sub all_links {
    my $self = shift;
    grep {$_->is_link} $self->all(@_);
}

sub chdir {
    my $self = shift;
    require Cwd;
    $self->chdir_from(Cwd::cwd());
    CORE::chdir($self->pathname);
    return $self;
}

sub empty {
    my $self = shift;
    my $dh;
    opendir($dh, $self->pathname) or die;
    while (my $dir = readdir($dh)) {
       return 0 unless $dir =~ /^\.{1,2}$/;
    } 
    return 1;
}

sub mkdir {
    my $self = shift;
    defined($self->perms)
    ? CORE::mkdir($self->pathname, $self->perms)
    : CORE::mkdir($self->pathname);
    return $self;
}

sub mkpath {
    my $self = shift;
    require File::Path;
    File::Path::mkpath($self->pathname, @_);
    return $self;
}

sub next {
    my $self = shift;
    $self->assert_open;
    my $name = $self->readdir;
    return unless defined $name;
    my $io = $self->constructor->(File::Spec->catfile($self->pathname, $name));
    $io->absolute if $self->is_absolute;
    return $io;
}

sub readdir {
    my $self = shift;
    $self->assert_open;
    if (wantarray) {
        my @return = grep { 
            not /^\.{1,2}$/ 
        } $self->io_handle->read;
        $self->close;
        return @return;
    }
    my $name = '.'; 
    while ($name =~ /^\.{1,2}$/) {
        $name = $self->io_handle->read;
        unless (defined $name) {
            $self->close;
            return;
        }
    }
    return $name;
}

sub rmdir {
    my $self = shift;
    rmdir $self->pathname;
}

sub rmtree {
    my $self = shift;
    require File::Path;
    File::Path::rmtree($self->pathname, @_);
}

sub DESTROY {
    my $self = shift;
    CORE::chdir($self->chdir_from)
      if $self->chdir_from;
      # $self->SUPER::DESTROY(@_);
}

#===============================================================================
sub overload_table {
    (
        '@{} dir' => 'overload_as_array',
        '%{} dir' => 'overload_as_hash',
    )
}

sub overload_as_array {
    [ $_[1]->all ];
}

sub overload_as_hash {
    +{ 
        map {
            (my $name = $_->pathname) =~ s/.*[\/\\]//;
            ($name, $_);
        } $_[1]->all 
    };
}

=encoding utf8

=head1 NAME 

IO::All::Dir - Directory Support for IO::All

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
