/*
    this is loaded last (if there's a script directory) and executed last (by default)
    as a convenience so that scripts can perform any fixups after other profiles
    have run. see misc/scripts/end.groovy
*/

script {
    profile ('END') {
        pattern { match { false } }
    }
}
