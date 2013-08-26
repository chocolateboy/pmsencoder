script {
    profile ('Extension Methods') {
        pattern {
            match { "hello, world!".match(/(?<name>\w+)!$/)['name'] == 'world' }
        }

        action {
            def bar = 'bar'
            def match1 = "foo${bar}".match(/^(\w)(\w{2})(\w+)$/) // GString
            def match2 = 'foobar'.match(/no match/) // String

            set '-indexed': match1[1]

            if (match1) {
                set '-bool1': 'true'
            } else {
                set '-bool1': 'false'
            }

            if (match2) {
                set '-bool2': 'true'
            } else {
                set '-bool2': 'false'
            }
        }
    }
}
