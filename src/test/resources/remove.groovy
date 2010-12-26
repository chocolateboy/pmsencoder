script {
    profile('Smart Remove Name') {
        pattern {
            domain 'smart.remove.name'
        }

        action {
            // -foo -bar -baz -quux -> -foo -baz -quux
            remove '-bar'
        }
    }

    profile('Smart Remove Value') {
        pattern {
            domain 'smart.remove.value'
        }

        action {
            // -foo -bar baz -quux -> -foo -quux
            remove '-bar'
        }
    }

    profile('Remove N') {
        pattern {
            domain 'remove.n'
        }

        action {
            // -foo -bar baz -quux -> -quux
            remove('-foo', 2)
        }
    }
}
