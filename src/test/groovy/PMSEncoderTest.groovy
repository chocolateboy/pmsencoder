@Typed
package com.chocolatey.pmsencoder

import net.pms.PMS

class PMSEncoderTest extends PMSEncoderTestCase {
    void testStashCopy() {
        def stash = new Stash([ foo: 'bar' ])
        assert stash != null
        def newStash = new Stash(stash)
        assert newStash != null
        assert !stash.is(newStash)
        assert newStash == [ foo: 'bar' ]
    }

    void testProfileValidationDelegateInitalState() {
        def profileValidationDelegate = new ProfileValidationDelegate('Test Profile')
        assert profileValidationDelegate != null
        assert profileValidationDelegate.name == 'Test Profile'
        assert profileValidationDelegate.patternBlock == null
        assert profileValidationDelegate.actionBlock == null
    }
}
