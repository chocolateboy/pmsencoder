script {
    FFMPEG = [ '-default', '-ffmpeg', '-args' ]

    // default ffmpeg args are only assigned in Matcher.match if
    // a profile consumes the event (in this case: launchTranscode)
    profile ('Dummy') { }
}
