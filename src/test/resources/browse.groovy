script {
    profile ('Browse') {
        pattern {
            domain 'ps3mediaserver.org'

            // confirm that it works in the pattern block
            match {
                // XXX: previously the title contained an &mdash; that was being translated: why?
                browse { $('title').text() } == 'PS3 Media Server'
            }
        }

        action {
            // confirm that it works in the action block
            title = browse { $('title').text() }
        }
    }
}
