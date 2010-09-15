config {
    profile ('Default') {
        pattern { match { true } }

        action {
            tr '-lavcopts': [ 'abitrate=\\d+': 'abitrate=384' ]
        }
    }
}
