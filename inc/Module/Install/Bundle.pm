#line 1
package Module::Install::Bundle;

use strict;
use Cwd                   ();
use File::Find            ();
use File::Copy            ();
use File::Basename        ();
use Module::Install::Base ();

use vars qw{$VERSION @ISA $ISCORE};
BEGIN {
	$VERSION = '0.92';
	@ISA     = 'Module::Install::Base';
	$ISCORE  = 1;
}

sub auto_bundle {
    my $self = shift;

    # Flatten array of arrays into a single array
    my @core = map @$_, map @$_, grep ref, $self->requires;

    $self->bundle(@core);
}

sub bundle {
    my $self = shift;
    $self->admin->bundle(@_) if $self->is_admin;

    my $cwd = Cwd::cwd();
    my $bundles = $self->read_bundles;
    my $bundle_dir = $self->_top->{bundle};
    $bundle_dir =~ s/\W+/\\W+/g;

    while (my ($name, $version) = splice(@_, 0, 2)) {
        $version ||= 0;

        my $source = $bundles->{$name} or die "Cannot find bundle source for $name";
        my $target = File::Basename::basename($source);
        $self->bundles($name, $target);

        next if eval "use $name $version; 1";
        mkdir( $target, 0777 ) or die $! unless -d $target;

        # XXX - clean those directories upon "make clean"?
        File::Find::find({
            wanted => sub {
                my $out = $_;
                $out =~ s/$bundle_dir/./i;
                mkdir( $out, 0777 ) if -d;
                File::Copy::copy($_ => $out) unless -d;
            },
            no_chdir => 1,
        }, $source);
    }

    chdir $cwd;
}

sub read_bundles {
    my $self = shift;
    my %map;

    local *FH;
    open FH, $self->_top->{bundle} . ".yml" or return {};
    while (<FH>) {
        /^(.*?): (['"])?(.*?)\2$/ or next;
        $map{$1} = $3;
    }
    close FH;

    return \%map;
}


sub auto_bundle_deps {
    my $self = shift;

    # Flatten array of arrays into a single array
    my @core = map @$_, map @$_, grep ref, $self->requires;
    while (my ($name, $version) = splice(@core, 0, 2)) {
        next unless $name;
         $self->bundle_deps($name, $version);
         $self->bundle($name, $version);
    }
}

sub bundle_deps {
    my ($self, $pkg, $version) = @_;
    my $deps = $self->admin->scan_dependencies($pkg) or return;

    foreach my $key (sort keys %$deps) {
        $self->bundle($key, ($key eq $pkg) ? $version : 0);
    }
}

1;

__END__

#line 195
