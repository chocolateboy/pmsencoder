@Typed
package com.chocolatey.pmsencoder

import net.pms.PMS

class PMSEncoderTest extends PMSEncoderTestCase {
    void testStashClone() {
        def stash = new Stash([ foo: 'bar' ])
        assert stash != null
        def newStash = new Stash(stash)
        assert newStash != null
        assert !stash.is(newStash)
        assert newStash == [ foo: 'bar' ]
    }

    void testProfileValidationDelegateInitalState() {
        def delegate = new ProfileValidationDelegate('Test Profile')
        assert delegate != null
        assert delegate.name == 'Test Profile'
        assert delegate.patternBlock == null
        assert delegate.actionBlock == null
    }
}
