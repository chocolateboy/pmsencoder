package IO_Dumper;
use strict;
use warnings;
use IO::All -base;

our @EXPORT = 'io';

sub io { return IO_Dumper->new(@_) };

package IO::All::Filesys;
use Data::Dumper;
sub dump {
    my $self = shift;
    local $Data::Dumper::Indent = 1;
    local $Data::Dumper::Sortkeys = 1;
    $self->print(Data::Dumper::Dumper(@_));
    return $self;
} 

1;
