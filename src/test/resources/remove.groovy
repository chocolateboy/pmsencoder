script {
    profile ('Remove Name') {
        pattern {
            domain 'remove.name'
        }

        action {
            // -foo -bar -baz -quux -> -foo -baz -quux
            remove '-bar'
        }
    }

    profile ('Remove Value') {
        pattern {
            domain 'remove.value'
        }

        action {
            // -foo -bar baz -quux -> -foo -quux
            remove '-bar'
        }
    }

    profile ('Digit Value') {
        pattern {
            domain 'digit.value'
        }

        action {
            // -foo -bar -42 -quux -> -foo -quux
            remove '-bar'
        }
    }

    profile ('Hyphen Value') {
        pattern {
            domain 'hyphen.value'
        }

        action {
            // -foo -ouput - -quux -> -foo -quux
            remove '-output'
        }
    }
}
