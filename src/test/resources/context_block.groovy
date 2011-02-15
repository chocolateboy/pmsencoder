script {
    profile ('Context Block') {
        action {
            $DOWNLOADER = $MPLAYER
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
                set '-user-agent': 'PS3 Media Server'
            }

            transcoder {
                set '-threads': '42'
            }

            output {
                set '-target': 'pal-dvd'
            }

            // default to $TRANSCODER
            set '-foo': 'bar'
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
                remove([ '-msglevel', '-quiet' ])
            }

            transcoder {
                remove '-y'
            }

            output {
                remove '-target'
            }

            // default to $TRANSCODER
            remove '-v'
        }
    }
}
