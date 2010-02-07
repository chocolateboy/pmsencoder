package App::PMSEncoder::HTTP::Client;

use 5.10.0;

use Mouse; # already loaded via Mouse::Tiny
use HTTP::Lite;

use constant MAX_REDIRECTS => 7;    # LWP::UserAgent default XXX make this configurable?

has debug_callback => (
    is       => 'rw',
    isa      => 'CodeRef',
    required => 0,
);

has fatal_callback => (
    is       => 'rw',
    isa      => 'CodeRef',
    required => 0,
);

has http => (
    isa     => 'HTTP::Lite',
    is      => 'rw',
    default => sub { HTTP::Lite->new },
);

has max_redirects => (
    isa     => 'Int',
    is      => 'rw',
    default => MAX_REDIRECTS,
);

sub get {
    my ($self, $uri) = @_;
    $self->debug("GET: $uri");
    return $self->request('GET', $uri);
}

sub head {
    my ($self, $uri) = @_;
    $self->debug("HEAD: $uri");
    return $self->request('HEAD', $uri);
}

sub debug {
    my ($self, $message) = @_;
    my $callback = $self->debug_callback;

    if ($callback) {
        return $callback->($message);
    } else {
        return undef;
    }
}

sub fatal {
    my ($self, $message) = @_;
    my $callback = $self->fatal_callback;

    if ($callback) {
        return $callback->($message);
    } else {
        return undef;
    }
}

sub request {
    my ($self, $method, $uri, $count) = @_;
    my $max_redirects = $self->max_redirects;

    $count = 0 unless (defined $count);

    if ($count >= $max_redirects) {
        $self->debug("redirection limit reached: $max_redirects");
        return undef;
    }

    my $http = $self->http;

    $http->reset();
    $http->method($method);

    my $response = $http->request($uri);

    if (defined $response) {
        my $status = $http->status_message;

        $self->debug("HTTP response: $response $status");

        if (($response >= 200) && ($response < 300)) {
            if ($method eq 'HEAD') {
                if (wantarray) {
                    return $self->fatal("LWP::Simple-compatible get() in list context is not supported") 
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

            return $self->fatal("can't find Location header in response") unless ($locations);

            for my $location (@$locations) {
                return $self->fatal("no value defined for Location header") unless ($location);
                $self->debug("redirecting to: $location");

                my $rv = $self->request($method, $location, $count + 1);

                if (defined $rv) {
                    return $rv;
                }
            }

            return undef;
        }
    } else {
        $self->debug("couldn't perform HTTP request for: $uri");
        return undef;
   }
}

1;

__END__

=head1 NAME

App::PMSEncoder::HTTP::Client - HTTP::Lite wrapper that adds support for redirects and debug/error callbacks

=head1 SYNOPSIS

    my $http = App::PMSEncoder::HTTP::Client->new();

    my $body = $http->get($uri);
    my $head = $http->head($uri);

=head1 DESCRIPTION

This is a simple wrapper around HTTTP::Lite that adds support for redirects and debug/error callbacks.

=head1 AUTHOR

chocolateboy <chocolate@cpan.org>

=head1 SEE ALSO

=over

=item * L<HTTP::Client|HTTP::Client>

=back

=head1 VERSION

0.60

=cut
