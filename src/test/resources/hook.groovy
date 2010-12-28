script {
    profile ('Hook List') {
        pattern {
            domain 'hook.list'
        }

        action {
            $HOOK = [ 'list', $URI ]
        }
    }

    profile ('Hook String') {
        pattern {
            domain 'hook.string'
        }

        action {
            $HOOK = "string ${$URI}"
        }
    }
}
