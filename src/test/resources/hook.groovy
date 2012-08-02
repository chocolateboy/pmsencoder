script {
    profile ('Hook String') {
        pattern {
            domain 'hook.string'
        }

        action {
            hook = "string ${uri}"
        }
    }

    profile ('Hook List') {
        pattern {
            domain 'hook.list'
        }

        action {
            hook = [ 'list', uri ]
        }
    }
}
