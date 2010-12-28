script {
    profile ('Default') {
        // note: the pattern block is (now) optional
        action {
            replace '-lavcopts': [ 'abitrate=\\d+': 'abitrate=384' ]
        }
    }
}
