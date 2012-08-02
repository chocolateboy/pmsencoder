script {
    profile ('jsoup') {
        pattern {
            domain 'ps3mediaserver.org'

            // confirm that it works in the pattern block
            match { $('title').text() == 'PS3 Media Server' }
            match { $(uri: uri)('title').text() == 'PS3 Media Server' }
            match { $([ uri: uri ])('title').text() == 'PS3 Media Server' }
            match { $(uri: uri, 'title').text() == 'PS3 Media Server' }
        }

        action {
            // confirm that it works in the action block
            title1 = $('title').text()
            title2 = $(uri: uri)('title').text()
            title3 = $([ uri: uri ])('title').text()
            title4 = $(uri: uri, 'title').text()
        }
    }
}
