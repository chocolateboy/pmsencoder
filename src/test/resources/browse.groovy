script {
    profile ('Browse') {
        pattern {
            domain 'eurogamer.net'

            // confirm that it works in the pattern block
            match {
                browse { $('title').text() } == 'Uncharted 3 chateau gameplay part 2 - Eurogamer Videos | Eurogamer.net'
            }
        }

        action {
            // confirm that it works in the action block
            $URI = 'http://www.eurogamer.net/' + browse { $('a.download').@href }
        }
    }
}
