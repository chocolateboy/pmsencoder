script {
    profile ('jsoup') {
        pattern {
            domain 'ps3mediaserver.org'

            // confirm that it works in the pattern block
            match { $('title').text() == 'PS3 Media Server' }
            match { $(uri: $URI)('title').text() == 'PS3 Media Server' }
            match { $([ uri: $URI ])('title').text() == 'PS3 Media Server' }
            match { $(uri: $URI, 'title').text() == 'PS3 Media Server' }
        }

        action {
            // confirm that it works in the action block
            title1 = $('title').text()
            title2 = $(uri: $URI)('title').text()
            title3 = $([ uri: $URI ])('title').text()
            title4 = $(uri: $URI, 'title').text()
        }
    }
}
