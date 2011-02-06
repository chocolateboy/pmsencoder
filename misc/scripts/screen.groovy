// videostream.Web,Screen=Screen,screen://

script {
    profile ('Screen') {
        pattern {
            protocol 'screen'
        }

        action {
            $TRANSCODER = [
                'cvlc',
                'screen://',
                '--screen-fps',     '25', // limited to MPEG-2 framerates
                '--screen-caching', '100',
                '--sout',
                    '#transcode{vcodec=mp2v,vb=512,scale=1,width=1024,height=768,acodec=none}:' +
                    "duplicate{dst=std{access=file,mux=ts,dst=\"${$TRANSCODER_OUT}\"}}"
            ]
        }
    }
}
