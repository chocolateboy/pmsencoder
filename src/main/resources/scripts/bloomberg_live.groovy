/*
    videostream.Web,TV=Bloomberg Live,mms://a627.l2479952251.c24799.g.lm.akamaistream.net/D/627/24799/v0001/reflector:52251,http://www.definedbymedia.com/images/logos/logos-bloombergtelevision.jpg
    videostream.Web,TV=Bloomberg Live stream 2,mms://a536.l2479952400.c24799.g.lm.akamaistream.net/D/536/24799/v0001/reflector:52400,http://www.definedbymedia.com/images/logos/logos-bloombergtelevision.jpg
    videostream.Web,TV=Bloomberg Live stream 3,mms://a1598.l2489858165.c24898.n.lm.akamaistream.net/D/1598/24898/v0001/reflector:58165,http://www.definedbymedia.com/images/logos/logos-bloombergtelevision.jpg
    videostream.Web,TV=Bloomberg Live stream 4,mms://a1332.l2489859148.c24898.n.lm.akamaistream.net/D/1332/24898/v0001/reflector:59148,http://www.definedbymedia.com/images/logos/logos-bloombergtelevision.jpg
*/

script {
    profile('Bloomberg Live') {
        pattern {
            match uri: '\\blm\\.akamaistream\\.net/D/'
        }

        action {
            // fix sync issues (these are in the stream itself)
            set '-delay': 0.2
        }
    }
}
