#!/bin/sh

curl --silent https://github.com/rg3/youtube-dl/raw/master/youtube-dl \
    | ack "_VALID_URL\s*=\s*r('[^']+')" --output '$1' \
    | perl -pe "chomp; s/\$/,\\n/; s{^'(?!\\^)}{'^}; s{\\\\}{\\\\\\\\}g" \
    | sort \
    | perl -0777 -pe 's{,$}{}'
