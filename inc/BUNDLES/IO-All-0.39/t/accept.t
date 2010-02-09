use lib 't', 'lib';
use strict;
use warnings;
use Test::More tests => 20;
use IO::All;
use IO_All_Test;
use IO::Socket::INET;

# This test tests for the ability of a non-forking socket to handle more 
# than one connection.

my $pid = fork();
if (! $pid)
{
    # Let the child process listen on a port
    my $port = 5555;
    my $accepted = 0;
    while (1)
    {
        # Log the port to a file.
        open my $out, ">t/output/server-port.t";
        print {$out} $port;
        close($out);
        
        my $server = io("localhost:$port");

        eval {
            for my $count (1 .. 10)
            {
                my $connection = $server->accept();
                $accepted = 1;
                $connection->print(sprintf("Ingy-%.2d", $count)); 
                $connection->close();
            }
        };
        if ($accepted)
        {
            # We have a listening socket on a port, so we can continue
            last;
        }
    }
    continue
    {
        # Try a different port.
        $port++;
    }
    exit(0);
}
# Let the parent process handle the testing.

# Wait a little for the client to find a port.
sleep(1);

open my $in, "<t/output/server-port.t";
my $port = <$in>;
close($in);

# TEST*2*10
for my $c (1 .. 10)
{
    my $sock = IO::Socket::INET->new(
        PeerAddr => "localhost",
        PeerPort => $port,
        Proto => "tcp"
    );

    ok(defined($sock), "Checking for validity of sock No. $c");

    if (!defined($sock))
    {
        last;
    }

    my $data;
    $sock->recv($data, 7);

    $sock->close();

    is ($data, sprintf("Ingy-%.2d", $c), "Checking for connection No. $c.");
}

waitpid($pid, 0);

