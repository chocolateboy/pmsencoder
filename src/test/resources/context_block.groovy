script {
    profile ('Context Block') {
        action {
            $HOOK = 'hook -foo -bar -baz'
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

            output {
                set '-bar'
            }

            // default to $TRANSCODER
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

            output {
                remove '-bar'
            }

            // default to $TRANSCODER
            remove '-baz'
        }
    }
}
