script {
    profile ('Match', stopOnMatch: false) { }

    profile ('Match Profile', stopOnMatch: false) {
        pattern {
            match 'Match'
        }
    }

    profile ("Don't Match Profile", stopOnMatch: false) {
        pattern {
            match 'Unmatched Profile 1'
        }
    }

    profile ('Match Profiles', stopOnMatch: false) {
        pattern {
            match([ 'Match', 'Match Profile' ])
        }
    }

    profile ("Don't Match Profiles 1", stopOnMatch: false) {
        pattern {
            match([ 'Match', 'Unmatched Profile 1' ])
        }
    }

    profile ("Don't Match Profiles 2", stopOnMatch: false) {
        pattern {
            match([ 'Unmatched Profile 1', 'Match' ])
        }
    }

    profile ("Don't Match Profiles 3", stopOnMatch: false) {
        pattern {
            match([ 'Unmatched Profile 1', 'Unmatched Profile 3' ])
        }
    }

    profile ('Match Key Unquoted', stopOnMatch: false) {
        pattern {
            match uri: 'http://www.example.com'
        }
    }

    profile ("Don't Match Key Unquoted", stopOnMatch: false) {
        pattern {
            match uri: 'http://nosuchdomain'
        }
    }

    profile ('Match Key Quoted', stopOnMatch: false) {
        pattern {
            match 'uri': 'http://www.example.com'
        }
    }

    profile ("Don't Match Key Quoted", stopOnMatch: false) {
        pattern {
            match 'uri': 'http://nosuchdomain'
        }
    }

    profile ('Match Block', stopOnMatch: false) {
        pattern {
            match { uri == 'http://www.example.com' }
        }
    }

    profile ("Don't Match Block", stopOnMatch: false) {
        pattern {
            match { uri != 'http://www.example.com' }
        }
    }

    profile ('Match (String, String)', stopOnMatch: false) {
        pattern {
            def $uri = uri
            match $uri, 'http://www.example.com'
        }
    }

    profile ("Don't Match (String, String)", stopOnMatch: false) {
        pattern {
            def $uri = uri
            match $uri, 'http://nosuchdomain'
        }
    }

    profile ('Match (String, List)', stopOnMatch: false) {
        pattern {
            def $uri = uri
            match $uri, [ 'http://www.example.com' ]
        }
    }

    profile ("Don't Match (String, List)", stopOnMatch: false) {
        pattern {
            def $uri = uri
            match $uri, [ 'http://nosuchdomain' ]
        }
    }
}
