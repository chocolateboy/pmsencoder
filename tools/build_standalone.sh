#!/bin/sh

export PMSENCODER_STANDALONE=1

make clean
perl Makefile.PL
make tardist

# --defaults \

cpan2dist \
    --buildprereq \
    --ignorelist tools/ignorelist \
    --format CPANPLUS::Dist::PAR \
    --archive App-PMSEncoder-*.tar.gz \
