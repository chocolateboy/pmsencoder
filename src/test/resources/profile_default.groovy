config {
    profile ('Default') {
        // note: the pattern block is (now) optional
        action {
            tr '-lavcopts': [ 'abitrate=\\d+': 'abitrate=384' ]
        }
    }
}
