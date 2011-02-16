// videostream.Web,Screen=Screen,screen://

script {
    profile ('Screen') {
        pattern {
            match { !$PMS.get().isWindows() && CVLC }
            protocol 'screen'
        }

        action {
            // $PARAMS.waitbeforestart = 0
            $TRANSCODER = [
                CVLC,
                'screen://',
                '--screen-fps',     '5',
                '--screen-caching', '100',
                '--sout',
                    '#transcode{vcodec=mp2v,fps=25,vb=512,scale=1,width=1024,height=768,acodec=none}:' +
                    "duplicate{dst=std{access=file,mux=ts,dst=\"${$TRANSCODER_OUT}\"}}"
            ]
        }
    }
}
