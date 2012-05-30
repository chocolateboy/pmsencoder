#!/bin/sh

curl --silent https://raw.github.com/rg3/youtube-dl/master/youtube_dl/InfoExtractors.py \
    | ack "_VALID_URL\s*=\s*r('[^']+')" --output '$1' \
    | perl -pe "chomp; s/\$/,\\n/; s{^'(?!\\^)}{'^}; s{\\\\}{\\\\\\\\}g; s{\?P<\w+>}{}g" \
    | sort \
    | perl -0777 -pe 's{,$}{}'
