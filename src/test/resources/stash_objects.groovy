// confirm stash values can be arbitrary objects (not just strings)
// and that they are shared from pattern to action blocks
script {
    profile ('Stash Objects') {
        pattern {
            patternString = 'Fizz'
            patternList   = [ 'foo', 'bar', 'baz' ]
            patternMap    = [ 'foo': 'bar' ]
        }

        action {
            actionString = patternString + 'Buzz'
            actionList   = patternList
            actionMap    = [ 'baz': 'quux' ]
        }
    }
}
