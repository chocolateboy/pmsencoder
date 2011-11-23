script {
    profile ('Browse') {
        pattern {
            domain 'example.org'

            // confirm that it works in the pattern block
            match {
                // XXX: why are these entities - this is &mdash; - being translated?
                browse { $('title').text() } == 'IANA — Example domains'
            }
        }

        action {
            // confirm that it works in the action block
            title = browse { $('title').text() }
        }
    }
}
