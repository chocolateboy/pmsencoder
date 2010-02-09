#!/bin/sh

# needed for solaris for echo -n to work right
SYSV3=1
export SYSV3

echo Content-type: text/html
echo
cat bigtest.txt
sleep 4
cat bigtest.txt
sleep 2
cat bigtest.txt
sleep 2
echo -n chunk4
sleep 2
echo chunk5
