// override the GameTrailers profile
script {
    profile ('Base') {
        pattern {
            domain 'inherit.pattern'
        }

        action {
            set '-base'
        }
    }

    profile ('Inherit Pattern', extends: 'Base') {
        action {
            set '-inherit': 'pattern'
        }
    }

    profile ('Inherit Action', extends: 'Base') {
        pattern {
            domain 'inherit.action'
        }
    }
}
