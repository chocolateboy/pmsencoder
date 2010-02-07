#!/bin/sh

PMSENCODER_STANDALONE=1 cpan2dist \
    --ignorelist tools/ignorelist
    --defaults \
    --buildprereq \
    --format CPANPLUS::Dist::PAR \
    --archive App-PMSEncoder-*.tar.gz
