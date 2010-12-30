/*
    this is loaded first (if there's a script directory) and executed first (by default)
    as a convenience so that scripts can create/override settings common to all other profiles
    without modifying $DEFAULT_TRANSCODER_ARGS &c. see misc/scripts/begin.groovy
*/

script {
    profile ('BEGIN') {
        pattern { match { false } }
    }
}
