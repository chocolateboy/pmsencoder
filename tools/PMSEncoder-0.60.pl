#!/usr/bin/env perl

use strict;
use warnings;

use SelfLoader;

use Cwd qw(getcwd);
use File::Temp qw(tempdir);
use HTTP::Lite;
use IPC::Cmd ();
use Scope::Guard;

sub run (@) {
    my @cmd = @_;

    local $IPC::Cmd::USE_IPC_RUN = 0;

    my ($ok, $err, $full_buffer, $stdout, $stderr) = IPC::Cmd::run(
        verbose => 1,
        command => \@cmd
    );

    if ($ok) {
        return wantarray ? ($stdout, $stderr) : $stdout;
    } else {
        die "can't run @cmd: $err";
    }
}

sub get ($) {
    my $uri  = shift;
    my $http = HTTP::Lite->new();

    $http->method('get');

    my $response = $http->request($uri);

    if (($response >= 200) && ($response <= 300)) {
        return $http->body;
    } else {
        my $status = $http->status_message;
        die "couldn't download $uri: invalid HTTP response: $response $status";
    }
}

sub mirror($$) {
    my ($uri, $filename) = @_;
    my $data = get($uri);
    write_file($filename, $data);
}

sub write_file($$) {
    my ($filename, $data) = @_;
    my $fh;

    open($fh, ">$filename") or die "can't open $filename for writing";
    print $fh $data;
    close $fh or warn("can't close $filename");
}

sub cd($) {
    my $dir = shift;
    chdir($dir) || die "can't chdir to $dir: $!";
}

#####################################################################################

my $install_dir = shift(@ARGV) || File::Spec->catdir($ENV{HOME}, 'pmsencoder');
my $local_lib_build_dir = tempdir("pmsencoder_installer_$$\_XXXX", CLEANUP => 1);
my $local_lib_version   = 'local-lib-1.004009';
my $local_lib_filename  = "$local_lib_version.tar.gz";
my $local_lib_tarball   = get("http://cpan.yimg.com/modules/by-authors/id/A/AP/APEIRON/$local_lib_filename");
my $oldpwd              = getcwd();
my $guard               = Scope::Guard->new(sub { chdir($oldpwd) });

cd($local_lib_build_dir);
write_file($local_lib_filename, $local_lib_tarball);
run('tar', 'xzvf', $local_lib_filename);
cd($local_lib_version);
run($^X, 'Makefile.PL', "--bootstrap=$install_dir");
run('make install');

require lib;
lib->import(File::Spec->catdir($install_dir, 'lib', 'perl5'));

require local::lib;
local::lib->import('--self-contained', $install_dir);    # import @INC and %ENV

my $pmsencoder_build_dir = tempdir("pmsencoder_installer_$$\_XXXX", CLEANUP => 1);
my $pmsencoder_tarball = 'PMSEncoder-0.60.tar.gz';

cd $pmsencoder_build_dir;
mirror("http://chocolatey.com/downloads/$pmsencoder_tarball", $pmsencoder_tarball);
run('tar', 'xzvf', $pmsencoder_tarball);
cd 'App-PMSEncoder-0.60';
run($^X, 'Makefile.PL');
run('make install');

print $/;
print "#####################################################################################", $/;
print "pmsencoder installed to: ", File::Spec->catfile($install_dir, 'bin', 'pmsencoder'), $/;
print "#####################################################################################", $/;
exit 0;

#####################################################################################

__DATA__

package Scope::Guard;

use SelfLoader

use strict;
use warnings;

use vars qw($VERSION);

$VERSION = '0.03';

sub new {
    my $class = shift;
    my $handler = shift() || die "Scope::Guard::new: no handler supplied";
    my $ref = ref $handler || '';

    die "Scope::Guard::new: invalid handler - expected CODE ref, got: '$ref'"
	unless (UNIVERSAL::isa($handler, 'CODE'));

    bless [ 0, $handler ], ref $class || $class;
}

sub dismiss {
    my $self = shift;
    my $dismiss = @_ ? shift : 1;

    $self->[0] = $dismiss;
}

sub DESTROY {
    my $self = shift;
    my ($dismiss, $handler) = @$self;

    $handler->() unless ($dismiss);
}

1;

package HTTP::Lite;

use SelfLoader

use 5.005;
use strict;
use Socket 1.3;
use Fcntl;
use Errno qw(EAGAIN);

use vars qw($VERSION);
BEGIN {
	$VERSION = "2.2";
}

my $BLOCKSIZE = 65536;
my $CRLF = "\r\n";
my $URLENCODE_VALID = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_-.";

# Forward declarations
sub prepare_post;
sub http_write;
sub http_readline;
sub http_read;
sub http_readbytes;

# Prepare the urlencode validchars lookup hash
my @urlencode_valid;
foreach my $char (split('', $URLENCODE_VALID)) {
  $urlencode_valid[ord $char]=$char;
}
for (my $n=0;$n<255;$n++) {
  if (!defined($urlencode_valid[$n])) {
    $urlencode_valid[$n]=sprintf("%%%02X", $n);
  }
}

sub new 
{
  my $self = {};
  bless $self;
  $self->initialize();
  return $self;
}

sub initialize
{
  my $self = shift;
  $self->reset;
  $self->{timeout} = 120;
  $self->{HTTP11} = 0;
  $self->{DEBUG} = 0;
  $self->{header_at_once} = 0; 
  $self->{holdback} = 0;       # needed for http_write 
}

sub header_at_once
{
  my $self=shift;
  $self->{header_at_once} = 1;
}

sub local_addr
{
  my $self = shift;
  my $val = shift;
  my $oldval = $self->{'local_addr'};
  if (defined($val)) {
    $self->{'local_addr'} = $val;
  }
  return $oldval;
}

sub local_port
{
  my $self = shift;
  my $val = shift;
  my $oldval = $self->{'local_port'};
  if (defined($val)) {
    $self->{'local_port'} = $val;
   }
  return $oldval;
}

sub method
{
  my $self = shift;
  my $method = shift;
  $method = uc($method);
  $self->{method} = $method;
}

sub DEBUG
{
  my $self = shift;
  if ($self->{DEBUG}) {
    print STDERR join(" ", @_),"\n";
  }
}

sub reset
{
  my $self = shift;
  foreach my $var ("body", "request", "content", "status", "proxy",
    "proxyport", "resp-protocol", "error-message",  
    "resp-headers", "CBARGS", "callback_function", "callback_params")
  {
    $self->{$var} = undef;
  }
  $self->{HTTPReadBuffer} = "";
  $self->{method} = "GET";
  $self->{headers} = { 'user-agent' => "HTTP::Lite/$VERSION" };
  $self->{headermap} = { 'user-agent'  => 'User-Agent' };
}


# URL-encode data
sub escape {
  my $toencode = shift;
  return join('', 
    map { $urlencode_valid[ord $_] } split('', $toencode));
}

sub set_callback {
  my ($self, $callback, @callbackparams) = @_;
  $self->{'callback_function'} = $callback;
  $self->{'callback_params'} = [ @callbackparams ];
}

sub request
{
  my ($self, $url, $data_callback, $cbargs) = @_;
  
  my $method = $self->{method};
  if (defined($cbargs)) {
    $self->{CBARGS} = $cbargs;
  }

  my $callback_func = $self->{'callback_function'};
  my $callback_params = $self->{'callback_params'};

  # Parse URL 
  my ($protocol,$host,$junk,$port,$object) = 
    $url =~ m{^([^:/]+)://([^/:]*)(:(\d+))?(/.*)$};

  # Only HTTP is supported here
  if ($protocol ne "http")
  {
    warn "Only http is supported by HTTP::Lite";
    return undef;
  }
  
  # Setup the connection
  my $proto = getprotobyname('tcp');
  local *FH;
  socket(FH, PF_INET, SOCK_STREAM, $proto);
  $port = 80 if !$port;

  my $connecthost = $self->{'proxy'} || $host;
  $connecthost = $connecthost ? $connecthost : $host;
  my $connectport = $self->{'proxyport'} || $port;
  $connectport = $connectport ? $connectport : $port;
  my $addr = inet_aton($connecthost);
  if (!$addr) {
    close(FH);
    return undef;
  }
  if ($connecthost ne $host)
  {
    # if proxy active, use full URL as object to request
    $object = "$url";
  }

  # choose local port and address
  my $local_addr = INADDR_ANY; 
  my $local_port = "0";
  if (defined($self->{'local_addr'})) {
    $local_addr = $self->{'local_addr'};
    if ($local_addr eq "0.0.0.0" || $local_addr eq "0") {
      $local_addr = INADDR_ANY;
    } else {
      $local_addr = inet_aton($local_addr);
    }
  }
  if (defined($self->{'local_port'})) {
    $local_port = $self->{'local_port'};
  }
  my $paddr = pack_sockaddr_in($local_port, $local_addr); 
  bind(FH, $paddr) || return undef;  # Failing to bind is fatal.

  my $sin = sockaddr_in($connectport,$addr);
  connect(FH, $sin) || return undef;
  # Set nonblocking IO on the handle to allow timeouts
  if ( $^O ne "MSWin32" ) {
    fcntl(FH, F_SETFL, O_NONBLOCK);
  }

  if (defined($callback_func)) {
    &$callback_func($self, "connect", undef, @$callback_params);
  }  

  if ($self->{header_at_once}) {
    $self->{holdback} = 1;    # http_write should buffer only, no sending yet
  }

  # Start the request (HTTP/1.1 mode)
  if ($self->{HTTP11}) {
    $self->http_write(*FH, "$method $object HTTP/1.1$CRLF");
  } else {
    $self->http_write(*FH, "$method $object HTTP/1.0$CRLF");
  }

  # Add some required headers
  # we only support a single transaction per request in this version.
  $self->add_req_header("Connection", "close");    
  if ($port != 80) {
    $self->add_req_header("Host", "$host:$port");
  } else {
    $self->add_req_header("Host", $host);
  }
  if (!defined($self->get_req_header("Accept"))) {
    $self->add_req_header("Accept", "*/*");
  }

  if ($method eq 'POST') {
    $self->http_write(*FH, "Content-Type: application/x-www-form-urlencoded$CRLF");
  }
  
  # Purge a couple others
  $self->delete_req_header("Content-Type");
  $self->delete_req_header("Content-Length");
  
  # Output headers
  foreach my $header ($self->enum_req_headers())
  {
    my $value = $self->get_req_header($header);
    $self->http_write(*FH, $self->{headermap}{$header}.": ".$value."$CRLF");
  }
  
  my $content_length;
  if (defined($self->{content}))
  {
    $content_length = length($self->{content});
  }
  if (defined($callback_func)) {
    my $ncontent_length = &$callback_func($self, "content-length", undef, @$callback_params);
    if (defined($ncontent_length)) {
      $content_length = $ncontent_length;
    }
  }  

  if ($content_length) {
    $self->http_write(*FH, "Content-Length: $content_length$CRLF");
  }
  
  if (defined($callback_func)) {
    &$callback_func($self, "done-headers", undef, @$callback_params);
  }  
  # End of headers
  $self->http_write(*FH, "$CRLF");
  
  if ($self->{header_at_once}) {
    $self->{holdback} = 0; 
    $self->http_write(*FH, ""); # pseudocall to get http_write going
  }  
  
  my $content_out = 0;
  if (defined($callback_func)) {
    while (my $content = &$callback_func($self, "content", undef, @$callback_params)) {
      $self->http_write(*FH, $content);
      $content_out++;
    }
  } 
  
  # Output content, if any
  if (!$content_out && defined($self->{content}))
  {
    $self->http_write(*FH, $self->{content});
  }
  
  if (defined($callback_func)) {
    &$callback_func($self, "content-done", undef, @$callback_params);
  }  


  # Read response from server
  my $headmode=1;
  my $chunkmode=0;
  my $chunksize=0;
  my $chunklength=0;
  my $chunk;
  my $line = 0;
  my $data;
  while ($data = $self->http_read(*FH,$headmode,$chunkmode,$chunksize))
  {
    $self->{DEBUG} && $self->DEBUG("reading: $chunkmode, $chunksize, $chunklength, $headmode, ".
        length($self->{'body'}));
    if ($self->{DEBUG}) {
      foreach my $var ("body", "request", "content", "status", "proxy",
        "proxyport", "resp-protocol", "error-message", 
        "resp-headers", "CBARGS", "HTTPReadBuffer") 
      {
        $self->DEBUG("state $var ".length($self->{$var}));
      }
    }
    $line++;
    if ($line == 1)
    {
      my ($proto,$status,$message) = split(' ', $$data, 3);
      $self->{DEBUG} && $self->DEBUG("header $$data");
      $self->{status}=$status;
      $self->{'resp-protocol'}=$proto;
      $self->{'error-message'}=$message;
      next;
    } 
    if (($headmode || $chunkmode eq "entity-header") && $$data =~ /^[\r\n]*$/)
    {
      if ($chunkmode)
      {
        $chunkmode = 0;
      }
      $headmode = 0;
      
      # Check for Transfer-Encoding
      my $te = $self->get_header("Transfer-Encoding");
      if (defined($te)) {
        my $header = join(' ',@{$te});
        if ($header =~ /chunked/i)
        {
          $chunkmode = "chunksize";
        }
      }
      next;
    }
    if ($headmode || $chunkmode eq "entity-header")
    {
      my ($var,$datastr) = $$data =~ /^([^:]*):\s*(.*)$/;
      if (defined($var))
      {
        $datastr =~s/[\r\n]$//g;
        $var = lc($var);
        $var =~ s/^(.)/&upper($1)/ge;
        $var =~ s/(-.)/&upper($1)/ge;
        my $hr = ${$self->{'resp-headers'}}{$var};
        if (!ref($hr))
        {
          $hr = [ $datastr ];
        }
        else 
        {
          push @{ $hr }, $datastr;
        }
        ${$self->{'resp-headers'}}{$var} = $hr;
      }
    } elsif ($chunkmode)
    {
      if ($chunkmode eq "chunksize")
      {
        $chunksize = $$data;
        $chunksize =~ s/^\s*|;.*$//g;
        $chunksize =~ s/\s*$//g;
        my $cshx = $chunksize;
        if (length($chunksize) > 0) {
          # read another line
          if ($chunksize !~ /^[a-f0-9]+$/i) {
            $self->{DEBUG} && $self->DEBUG("chunksize not a hex string");
          }
          $chunksize = hex($chunksize);
          $self->{DEBUG} && $self->DEBUG("chunksize was $chunksize (HEX was $cshx)");
          if ($chunksize == 0)
          {
            $chunkmode = "entity-header";
          } else {
            $chunkmode = "chunk";
            $chunklength = 0;
          }
        } else {
          $self->{DEBUG} && $self->DEBUG("chunksize empty string, checking next line!");
        }
      } elsif ($chunkmode eq "chunk")
      {
        $chunk .= $$data;
        $chunklength += length($$data);
        if ($chunklength >= $chunksize)
        {
          $chunkmode = "chunksize";
          if ($chunklength > $chunksize)
          {
            $chunk = substr($chunk,0,$chunksize);
          } 
          elsif ($chunklength == $chunksize && $chunk !~ /$CRLF$/) 
          {
            # chunk data is exactly chunksize -- need CRLF still
            $chunkmode = "ignorecrlf";
          }
          $self->add_to_body(\$chunk, $data_callback);
          $chunk="";
          $chunklength = 0;
          $chunksize = "";
        } 
      } elsif ($chunkmode eq "ignorecrlf")
      {
        $chunkmode = "chunksize";
      }
    } else {
      $self->add_to_body($data, $data_callback);
    }
  }
  if (defined($callback_func)) {
    &$callback_func($self, "done", undef, @$callback_params);
  }
  close(FH);
  return $self->{status};
}

sub add_to_body
{
  my $self = shift;
  my ($dataref, $data_callback) = @_;
  
  my $callback_func = $self->{'callback_function'};
  my $callback_params = $self->{'callback_params'};

  if (!defined($data_callback) && !defined($callback_func)) {
    $self->{DEBUG} && $self->DEBUG("no callback");
    $self->{'body'}.=$$dataref;
  } else {
    my $newdata;
    if (defined($callback_func)) {
      $newdata = &$callback_func($self, "data", $dataref, @$callback_params);
    } else {
      $newdata = &$data_callback($self, $dataref, $self->{CBARGS});
    }
    if ($self->{DEBUG}) {
      $self->DEBUG("callback got back a ".ref($newdata));
      if (ref($newdata) eq "SCALAR") {
        $self->DEBUG("callback got back ".length($$newdata)." bytes");
      }
    }
    if (defined($newdata) && ref($newdata) eq "SCALAR") {
      $self->{'body'} .= $$newdata;
    }
  }
}

sub add_req_header
{
  my $self = shift;
  my ($header, $value) = @_;
  
  my $lcheader = lc($header);
  $self->{DEBUG} && $self->DEBUG("add_req_header $header $value");
  ${$self->{headers}}{$lcheader} = $value;
  ${$self->{headermap}}{$lcheader} = $header;
}

sub get_req_header
{
  my $self = shift;
  my ($header) = @_;
  
  return $self->{headers}{lc($header)};
}

sub delete_req_header
{
  my $self = shift;
  my ($header) = @_;
  
  my $exists;
  if ($exists=defined(${$self->{headers}}{lc($header)}))
  {
    delete ${$self->{headers}}{lc($header)};
    delete ${$self->{headermap}}{lc($header)};
  }
  return $exists;
}

sub enum_req_headers
{
  my $self = shift;
  my ($header) = @_;
  
  my $exists;
  return keys %{$self->{headermap}};
}

sub body
{
  my $self = shift;
  return $self->{'body'};
}

sub status
{
  my $self = shift;
  return $self->{status};
}

sub protocol
{
  my $self = shift;
  return $self->{'resp-protocol'};
}

sub status_message
{
  my $self = shift;
  return $self->{'error-message'};
}

sub proxy
{
  my $self = shift;
  my ($value) = @_;
  
  # Parse URL 
  my ($protocol,$host,$junk,$port,$object) = 
    $value =~ m{^(\S+)://([^/:]*)(:(\d+))?(/.*)$};
  if (!$host)
  {
    ($host,$port) = $value =~ /^([^:]+):(.*)$/;
  }

  $self->{'proxy'} = $host || $value;
  $self->{'proxyport'} = $port || 80;
}

sub headers_array
{
  my $self = shift;
  
  my @array = ();
  
  foreach my $header (keys %{$self->{'resp-headers'}})
  {
    my $aref = ${$self->{'resp-headers'}}{$header};
    foreach my $value (@$aref)
    {
      push @array, "$header: $value";
    }
  }
  return @array;
}

sub headers_string
{
  my $self = shift;
  
  my $string = "";
  
  foreach my $header (keys %{$self->{'resp-headers'}})
  {
    my $aref = ${$self->{'resp-headers'}}{$header};
    foreach my $value (@$aref)
    {
      $string .= "$header: $value\n";
    }
  }
  return $string;
}

sub get_header
{
  my $self = shift;
  my $header = shift;

  return $self->{'resp-headers'}{$header};
}

sub http11_mode
{
  my $self = shift;
  my $mode = shift;

  $self->{HTTP11} = $mode;
}

sub prepare_post
{
  my $self = shift;
  my $varref = shift;
  
  my $body = "";
  while (my ($var,$value) = map { escape($_) } each %$varref)
  {
    if ($body)
    {
      $body .= "&$var=$value";
    } else {
      $body = "$var=$value";
    }
  }
  $self->{content} = $body;
  $self->{headers}{'Content-Type'} = "application/x-www-form-urlencoded"
    unless defined ($self->{headers}{'Content-Type'}) and 
    $self->{headers}{'Content-Type'};
  $self->{method} = "POST";
}

sub http_write
{
  my $self = shift;
  my ($fh,$line) = @_;

  if ($self->{holdback}) {
     $self->{HTTPWriteBuffer} .= $line;
     return;
  } else {
     if (defined $self->{HTTPWriteBuffer}) {   # copy previously buffered, if any
         $line = $self->{HTTPWriteBuffer} . $line;
     }
  }

  my $size = length($line);
  my $bytes = syswrite($fh, $line, length($line) , 0 );  # please double check new length limit
                                                         # is this ok?
  while ( ($size - $bytes) > 0) {
    $bytes += syswrite($fh, $line, length($line)-$bytes, $bytes );  # also here
  }
}
 
sub http_read
{
  my $self = shift;
  my ($fh,$headmode,$chunkmode,$chunksize) = @_;

  $self->{DEBUG} && $self->DEBUG("read handle=$fh, headm=$headmode, chunkm=$chunkmode, chunksize=$chunksize");

  my $res;
  if (($headmode == 0 && $chunkmode eq "0") || ($chunkmode eq "chunk")) {
    my $bytes_to_read = $chunkmode eq "chunk" ?
        ($chunksize < $BLOCKSIZE ? $chunksize : $BLOCKSIZE) :
        $BLOCKSIZE;
    $res = $self->http_readbytes($fh,$self->{timeout},$bytes_to_read);
  } else { 
    $res = $self->http_readline($fh,$self->{timeout});  
  }
  if ($res) {
    if ($self->{DEBUG}) {
      $self->DEBUG("read got ".length($$res)." bytes");
      my $str = $$res;
      $str =~ s{([\x00-\x1F\x7F-\xFF])}{.}g;
      $self->DEBUG("read: ".$str);
    }
  }
  return $res;
}

sub http_readline
{
  my $self = shift;
  my ($fh, $timeout) = @_;
  my $EOL = "\n";

  $self->{DEBUG} && $self->DEBUG("readline handle=$fh, timeout=$timeout");
  
  # is there a line in the buffer yet?
  while ($self->{HTTPReadBuffer} !~ /$EOL/)
  {
    # nope -- wait for incoming data
    my ($inbuf,$bits,$chars) = ("","",0);
    vec($bits,fileno($fh),1)=1;
    my $nfound = select($bits, undef, $bits, $timeout);
    if ($nfound == 0)
    {
      # Timed out
      return undef;
    } else {
      # Get the data
      $chars = sysread($fh, $inbuf, $BLOCKSIZE);
      $self->{DEBUG} && $self->DEBUG("sysread $chars bytes");
    }
    # End of stream?
    if ($chars <= 0 && !$!{EAGAIN})
    {
      last;
    }
    # tag data onto end of buffer
    $self->{HTTPReadBuffer}.=$inbuf;
  }
  # get a single line from the buffer
  my $nlat = index($self->{HTTPReadBuffer}, $EOL);
  my $newline;
  my $oldline;
  if ($nlat > -1)
  {
    $newline = substr($self->{HTTPReadBuffer},0,$nlat+1);
    $oldline = substr($self->{HTTPReadBuffer},$nlat+1);
  } else {
    $newline = substr($self->{HTTPReadBuffer},0);
    $oldline = "";
  }
  # and update the buffer
  $self->{HTTPReadBuffer}=$oldline;
  return length($newline) ? \$newline : 0;
}

sub http_readbytes
{
  my $self = shift;
  my ($fh, $timeout, $bytes) = @_;
  my $EOL = "\n";

  $self->{DEBUG} && $self->DEBUG("readbytes handle=$fh, timeout=$timeout, bytes=$bytes");
  
  # is there enough data in the buffer yet?
  while (length($self->{HTTPReadBuffer}) < $bytes)
  {
    # nope -- wait for incoming data
    my ($inbuf,$bits,$chars) = ("","",0);
    vec($bits,fileno($fh),1)=1;
    my $nfound = select($bits, undef, $bits, $timeout);
    if ($nfound == 0)
    {
      # Timed out
      return undef;
    } else {
      # Get the data
      $chars = sysread($fh, $inbuf, $BLOCKSIZE);
      $self->{DEBUG} && $self->DEBUG("sysread $chars bytes");
    }
    # End of stream?
    if ($chars <= 0 && !$!{EAGAIN})
    {
      last;
    }
    # tag data onto end of buffer
    $self->{HTTPReadBuffer}.=$inbuf;
  }
  my $newline;
  my $buflen;
  if (($buflen=length($self->{HTTPReadBuffer})) >= $bytes)
  {
    $newline = substr($self->{HTTPReadBuffer},0,$bytes+1);
    if ($bytes+1 < $buflen) {
      $self->{HTTPReadBuffer} = substr($self->{HTTPReadBuffer},$bytes+1);
    } else {
      $self->{HTTPReadBuffer} = "";
    }
  } else {
    $newline = substr($self->{HTTPReadBuffer},0);
    $self->{HTTPReadBuffer} = "";
  }
  return length($newline) ? \$newline : 0;
}

sub upper
{
  my ($str) = @_;
  if (defined($str)) {
    return uc($str);
  } else {
    return undef;
  }
}

1;
