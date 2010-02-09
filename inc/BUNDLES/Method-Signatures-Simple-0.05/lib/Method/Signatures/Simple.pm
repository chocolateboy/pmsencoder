package Method::Signatures::Simple;
our $VERSION = '0.05';


use warnings;
use strict;

=head1 NAME

Method::Signatures::Simple - Basic method declarations with signatures, without source filters

=head1 VERSION

version 0.05

=cut

use base q/Devel::Declare::MethodInstaller::Simple/;

sub import {
    my $class = shift;
    my %opts  = @_;
    $opts{into}     ||= caller;
    $opts{invocant} ||= '$self';

    $class->install_methodhandler(
      name => 'method',
      %opts,
    );
}

sub parse_proto {
    my $self = shift;
    my ($proto) = @_;
    $proto ||= '';
    $proto =~ s/[\r\n]//g;
    my $invocant = $self->{invocant};

    $invocant = $1 if $proto =~ s{^(\$\w+):\s*}{};

    my $inject = "my ${invocant} = shift;";
    $inject .= "my ($proto) = \@_;" if defined $proto and length $proto;

    return $inject;
}


=head1 SYNOPSIS

    use Method::Signatures::Simple;

    method foo { $self->bar }

    # with signature
    method foo($bar, %opts) {
        $self->bar(reverse $bar) if $opts{rev};
    }

    # attributes
    method foo : lvalue { $self->{foo} }

    # change invocant name
    method foo ($class: $bar) { $class->bar($bar) }

=head1 RATIONALE

This module provides a basic C<method> keyword with simple signatures. It's intentionally simple,
and is supposed to be a stepping stone for its bigger brothers L<MooseX::Method::Signatures> and L<Method::Signatures>.
It only has a small benefit over regular subs, so if you want more features, look at those modules.
But if you're looking for a small amount of syntactic sugar, this might just be enough.

=head1 FEATURES

=over 4

=item * invocant

The C<method> keyword automatically injects the annoying C<my $self = shift;> for you. You can rename
the invocant with the first argument, followed by a colon:

    method ($this:) {}
    method ($this: $that) {}

=item * signature

The signature C<($sig)> is transformed into C<"my ($sig) = \@_;">. That way, we mimic perl's usual
argument handling.

    method foo ($bar, $baz, %opts) {

    # becomes

    sub foo {
        my $self = shift;
        my ($bar, $baz, %opts) = @_;

=back


=begin pod-coverage

=over 4

=item parse_proto

Overridden.

=back

=end pod-coverage

=head1 AUTHOR

Rhesa Rozendaal, C<< <rhesa at cpan.org> >>

=head1 BUGS

Please report any bugs or feature requests to C<bug-method-signatures-simple at rt.cpan.org>, or through
the web interface at L<http://rt.cpan.org/NoAuth/ReportBug.html?Queue=Method-Signatures-Simple>.  I will be notified, and then you'll
automatically be notified of progress on your bug as I make changes.

=head1 SUPPORT

You can find documentation for this module with the perldoc command.

    perldoc Method::Signatures::Simple


You can also look for information at:

=over 4

=item * RT: CPAN's request tracker

L<http://rt.cpan.org/NoAuth/Bugs.html?Dist=Method-Signatures-Simple>

=item * AnnoCPAN: Annotated CPAN documentation

L<http://annocpan.org/dist/Method-Signatures-Simple>

=item * CPAN Ratings

L<http://cpanratings.perl.org/d/Method-Signatures-Simple>

=item * Search CPAN

L<http://search.cpan.org/dist/Method-Signatures-Simple>

=back

=head1 ACKNOWLEDGEMENTS

=over 4

=item * MSTROUT

For writing L<Devel::Declare> and providing the core concepts.

=item * MSCHWERN

For writing L<Method::Signatures> and publishing about it. This is what got my attention.

=item * FLORA

For helping me abstracting the Devel::Declare bits and suggesting improvements.

=back

=head1 SEE ALSO

L<Devel::Declare>, L<Method::Signatures>, L<MooseX::Method::Signatures>.

=head1 COPYRIGHT & LICENSE

Copyright 2008 Rhesa Rozendaal, all rights reserved.

This program is free software; you can redistribute it and/or modify it
under the same terms as Perl itself.


=cut

1; # End of Method::Signatures::Simple