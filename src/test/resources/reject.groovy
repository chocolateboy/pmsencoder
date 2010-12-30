script {
    profile('Match') { }

    profile('Reject Profile') {
        pattern {
            reject 'Unmatched Profile'
        }
    }

    profile("Don't Reject Profile") {
        pattern {
            reject 'Match'
        }
    }

    profile('Reject Profiles') {
        pattern {
            reject([ 'Unmatched Profile 1', 'Unmatched Profile 2' ])
        }
    }

    profile("Don't Reject Profiles 1") {
        pattern {
            reject([ 'Match', 'Reject Profile' ])
        }
    }

    profile("Don't Reject Profiles 2") {
        pattern {
            reject([ 'Reject Profile', 'Match' ])
        }
    }

    profile('Reject Key Unquoted') {
        pattern {
            reject $URI: 'http://nosuchdomain'
        }
    }

    profile("Don't Reject Key Unquoted") {
        pattern {
            reject $URI: 'http://www.example.com'
        }
    }

    profile('Reject Key Quoted') {
        pattern {
            reject '$URI': 'http://nosuchdomain'
        }
    }

    profile("Don't Reject Key Quoted") {
        pattern {
            reject '$URI': 'http://www.example.com'
        }
    }

    profile('Reject Block') {
        pattern {
            reject { $URI == 'http://nosuchdomain' }
        }
    }

    profile("Don't Reject Block") {
        pattern {
            reject { $URI == 'http://www.example.com' }
        }
    }

    profile ('Reject (String, String)') {
        pattern {
            def $uri = $URI
            reject $uri, 'http://nosuchdomain'
        }
    }

    profile ("Don't Reject (String, String)") {
        pattern {
            def $uri = $URI
            reject $uri, 'http://www.example.com'
        }
    }

    profile ('Reject (String, List)') {
        pattern {
            def $uri = $URI
            reject $uri, [ 'http://nosuchdomain' ]
        }
    }

    profile ("Don't Reject (String, List)") {
        pattern {
            def $uri = $URI
            reject $uri, [ 'http://www.example.com' ]
        }
    }
}
