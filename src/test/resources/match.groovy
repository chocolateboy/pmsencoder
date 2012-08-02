script {
    profile('Match') { }

    profile('Match Profile') {
        pattern {
            match 'Match'
        }
    }

    profile("Don't Match Profile") {
        pattern {
            match 'Unmatched Profile 1'
        }
    }

    profile('Match Profiles') {
        pattern {
            match([ 'Match', 'Match Profile' ])
        }
    }

    profile("Don't Match Profiles 1") {
        pattern {
            match([ 'Match', 'Unmatched Profile 1' ])
        }
    }

    profile("Don't Match Profiles 2") {
        pattern {
            match([ 'Unmatched Profile 1', 'Match' ])
        }
    }

    profile("Don't Match Profiles 3") {
        pattern {
            match([ 'Unmatched Profile 1', 'Unmatched Profile 3' ])
        }
    }

    profile('Match Key Unquoted') {
        pattern {
            match uri: 'http://www.example.com'
        }
    }

    profile("Don't Match Key Unquoted") {
        pattern {
            match uri: 'http://nosuchdomain'
        }
    }

    profile('Match Key Quoted') {
        pattern {
            match 'uri': 'http://www.example.com'
        }
    }

    profile("Don't Match Key Quoted") {
        pattern {
            match 'uri': 'http://nosuchdomain'
        }
    }

    profile('Match Block') {
        pattern {
            match { uri == 'http://www.example.com' }
        }
    }

    profile("Don't Match Block") {
        pattern {
            match { uri != 'http://www.example.com' }
        }
    }

    profile ('Match (String, String)') {
        pattern {
            def $uri = uri
            match $uri, 'http://www.example.com'
        }
    }

    profile ("Don't Match (String, String)") {
        pattern {
            def $uri = uri
            match $uri, 'http://nosuchdomain'
        }
    }

    profile ('Match (String, List)') {
        pattern {
            def $uri = uri
            match $uri, [ 'http://www.example.com' ]
        }
    }

    profile ("Don't Match (String, List)") {
        pattern {
            def $uri = uri
            match $uri, [ 'http://nosuchdomain' ]
        }
    }
}
