script {
    profile ('Context Block') {
        action {
            hook = 'hook -foo -bar -baz'
        }
    }

    profile ('Context Block Set') {
        pattern {
            domain 'context-block-set.com'
        }

        action {
            hook {
                set '-quux'
            }

            downloader {
                set '-bar'
            }

            transcoder {
                set '-bar'
            }

            // default to transcoder
            set '-baz'
        }
    }

    profile ('Context Block Remove') {
        pattern {
            domain 'context-block-remove.com'
        }

        action {
            hook {
                remove '-bar'
            }

            downloader {
                remove '-bar'
            }

            transcoder {
                remove '-bar'
            }

            // default to transcoder
            remove '-baz'
        }
    }
}
