@Typed
package com.chocolatey.pmsencoder

class StashObjectsTest extends PMSEncoderTestCase {
    void testStashObjects() {
        assertMatch([
            script: '/stash_objects.groovy',
            wantMatches: [ 'Stash Objects' ],
            wantStash: [
                patternString:   'Fizz',
                actionString:    'FizzBuzz',
                patternList:   [ 'foo', 'bar', 'baz' ],
                actionList:    [ 'foo', 'bar', 'baz' ],
                patternMap:    [ 'foo': 'bar' ],
                actionMap:     [ 'baz': 'quux' ],
            ]
        ])

        assertMatch([
            script: '/stash_objects.groovy',
            wantMatches: [ 'Stash Objects' ],
            // confirm the shared list doesn't just have the same contents: it's the same object
            wantStash: { Stash stash -> stash['actionList'].is(stash['patternList']) },
        ])
    }
}
