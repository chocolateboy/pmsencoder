package HTTP::Simple;

use strict;
use warnings;

use constant MAX_REDIRECTS => 7; # LWP::UserAgent default XXX make this configurable?

our $VERSION = '0.01';
our @EXPORT_OK = qw(head get);

sub import {
    my ($class, @imports) = @_;
    my %export_ok = map { $_ => 1 } @EXPORT_OK;

    for my $import (@imports) {
        die "invalid import: $import" unless ($export_ok{$import});
    }

    my $caller = caller;
    my $exporter;

    if (eval { require LWP::Simple; 1 }) {
        $exporter = 'LWP::Simple';
    } else {
        require HTTP::Lite;
        $exporter = $class;
    }

    for my $import (@imports) {
        no strict 'refs';
        *{"$caller\::$import"} = $exporter->can($import);
    }
}

sub get($) {
    my $uri = shift;
    # $self->debug("GET: $uri");
    return _request('GET', $uri);
}

sub head($) {
    my $uri = shift;
    # $self->debug("HEAD: $uri");
    return _request('HEAD', $uri);
}

sub _request($$;$); # pre-declare to allow for recursion when handling redirects
sub _request($$;$) {
    my ($method, $uri, $count) = @_;
    my $max_redirects = MAX_REDIRECTS;

    $count = 0 unless (defined $count);

    if ($count >= $max_redirects) {
        # $self->debug("redirection limit reached: $max_redirects");
        return;
    }

    my $http = HTTP::Lite->new();

    $http->method($method);

    my $response = $http->request($uri);

    if (defined $response) {
        my $status = $http->status_message;

        # $self->debug("HTTP response: $response $status");

        if (($response >= 200) && ($response < 300)) {
            if ($method eq 'HEAD') {
                if (wantarray) {
                    # return $self->fatal("LWP::Simple-compatible get() in list context is not supported") 
                    return;
                } else {
                    # we need to return a true value; may as well return the headers array ref
                    return $http->headers_array;
                }
            } else {
                return $http->body;
            }
        # XXX LWP::UserAgent handles at least 2 more cases
        } elsif (($response == 303) || ($response == 307)) { # redirect 
            # header names aren't unique; there may be more than one location
            my $locations = $http->get_header('Location');

            # return $self->fatal("can't find Location header in response") unless ($locations);
            return unless ($locations);

            for my $location (@$locations) {
                # return $self->fatal("no value defined for Location header") unless ($location);
                return unless ($location);
                # $self->debug("redirecting to: $location");

                my $rv = _request($method, $location, $count + 1);

                if (defined $rv) {
                    return $rv;
                }
            }

            return;
        }
    } else {
        # $self->debug("couldn't perform HTTP request for: $uri");
        return; 
   }
}

1;

__END__

=head1 NAME

HTTP::Simple - a lightweight, portable implementation of the LWP::Simple API

=head1 SYNOPSIS

    use HTTP::Lite qw(get head);

    my $body = get($uri);
    my $head = head($uri);

=head1 DESCRIPTION

This module implements the L<LWP::Simple|LWP::Simple> API in a way that is intended to provide support
for B<HTTP> requests in all environments i.e. including those without LWP or a compiler. If C<LWP> is
available, then C<LWP::Simple> is used. Otherwise, this module falls back on HTTP::Lite, for which it
provides simple wrappers. Currently, C<get> and C<head> are supported and there is preliminary support
for redirection handling.

=head1 AUTHOR

chocolateboy <chocolate@cpan.org>

=head1 SEE ALSO

=over

=item * L<HTTP::Client|HTTP::Client>

=back

=head1 VERSION

0.60

=cut
