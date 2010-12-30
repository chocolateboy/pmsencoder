// workaround MEncoder's recently-useless cache

script {
    profile ('MEncoder Cache Fix', after: 'END') { // or 'replaces', or overwrite END
        pattern {
            match { $TRANSCODER == null }
            match { $DOWNLOADER == null }
            match { !$URI.startsWith('ffmpeg://') }
        }

        action {
            // XXX this requires a recent (late 2010) MEncoder
            $URI = "ffmpeg://${$URI}" // use FFmpeg's non-broken networking code
        }
    }
}
