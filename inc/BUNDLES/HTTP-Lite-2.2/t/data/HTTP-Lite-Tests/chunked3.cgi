#!/bin/sh

# needed for solaris for echo -n to work right
SYSV3=1
export SYSV3

echo Content-type: text/html
echo
echo chunk1
sleep 4
echo chunk2
sleep 2
echo -n chunk3
sleep 2
echo -n chunk4
sleep 2
echo chunk5
echo
