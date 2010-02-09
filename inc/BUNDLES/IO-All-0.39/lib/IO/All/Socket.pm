package IO::All::Socket;
use strict;
use warnings;
use IO::All -base;
use IO::Socket;

const type => 'socket';
field _listen => undef;
option 'fork';
const domain_default => 'localhost';
chain domain => undef;
chain port => undef;
proxy_open 'recv';
proxy_open 'send';

sub socket {
    my $self = shift;
    bless $self, __PACKAGE__;
    $self->name(shift) if @_;
    return $self->_init;
}

sub socket_handle {
    my $self = shift;
    bless $self, __PACKAGE__;
    $self->_handle(shift) if @_;
    return $self->_init;
}

sub accept {
    my $self = shift;
    use POSIX ":sys_wait_h";
    sub REAPER {
        while (waitpid(-1, WNOHANG) > 0) {}
        $SIG{CHLD} = \&REAPER;
    }
    local $SIG{CHLD};
    $self->_listen(1);
    $self->assert_open;
    my $server = $self->io_handle;
    my $socket; 
    while (1) {
        $socket = $server->accept;
        last unless $self->_fork;
        next unless defined $socket;
        $SIG{CHLD} = \&REAPER;
        my $pid = CORE::fork;
        $self->throw("Unable to fork for IO::All::accept")
          unless defined $pid;
        last unless $pid;
        close $socket;
        undef $socket;
    }
    close $server if $self->_fork;
    my $io = ref($self)->new->socket_handle($socket);
    $io->io_handle($socket);
    $io->is_open(1);
    return $io;
}

sub shutdown {
    my $self = shift;
    my $how = @_ ? shift : 2;
    my $handle = $self->io_handle;
    $handle->shutdown(2)
      if defined $handle;
}

sub assert_open {
    my $self = shift;
    return if $self->is_open;
    $self->mode(shift) unless $self->mode;
    $self->open;
}

sub open {
    my $self = shift;
    return if $self->is_open;
    $self->is_open(1);
    $self->get_socket_domain_port;
    my @args = $self->_listen
    ? (
        LocalAddr => $self->domain,
        LocalPort => $self->port,
        Proto => 'tcp',
        Listen => 1,
        Reuse => 1,
    )
    : (
        PeerAddr => $self->domain,
        PeerPort => $self->port,
        Proto => 'tcp',
    );
    my $socket = IO::Socket::INET->new(@args)
      or $self->throw("Can't open socket");
    $self->io_handle($socket);
    $self->set_binmode;
}

sub get_socket_domain_port {
    my $self = shift;
    my ($domain, $port);
    ($domain, $port) = split /:/, $self->name
      if defined $self->name;
    $self->domain($domain) unless defined $self->domain;
    $self->domain($self->domain_default) unless $self->domain;
    $self->port($port) unless defined $self->port;
    return $self;
}

sub overload_table {
    my $self = shift;
    (
        $self->SUPER::overload_table(@_),
        '&{} socket' => 'overload_socket_as_code',
    )
}

sub overload_socket_as_code {
    my $self = shift;
    sub {
        my $coderef = shift;
        while ($self->is_open) {
            $_ = $self->getline;
            &$coderef($self);
        }
    }
}

sub overload_any_from_any {
    my $self = shift;
    $self->SUPER::overload_any_from_any(@_);
    $self->close;
}

sub overload_any_to_any {
    my $self = shift;
    $self->SUPER::overload_any_to_any(@_);
    $self->close;
}

=encoding utf8;

=head1 NAME 

IO::All::Socket - Socket Support for IO::All

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
