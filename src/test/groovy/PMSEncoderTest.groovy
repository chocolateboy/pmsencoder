@Typed
package com.chocolatey.pmsencoder

import net.pms.PMS

class PMSEncoderTest extends PMSEncoderTestCase {
    void testCommandClone() {
        def command = new Command([ foo: 'bar' ], [ 'baz', 'quux' ])
        assert command != null
        def newCommand = new Command(command)
        assert newCommand != null

        assert !command.stash.is(newCommand.stash)
        assert !command.args.is(newCommand.args)
        assert !command.is(newCommand)
        assert newCommand.stash == [ $foo: 'bar' ]
        assert newCommand.args == [ 'baz', 'quux' ]
    }

    void testStashClone() {
        def stash = new Stash([ foo: 'bar' ])
        assert stash != null
        def newStash = new Stash(stash)
        assert newStash != null
        assert !stash.is(newStash)
        assert newStash == [ $foo: 'bar' ]
    }

    void testProfileValidationDelegateInitalState() {
        def script = new Script(pms)
        def delegate = new ProfileValidationDelegate(script, 'Test Profile')

        assert delegate != null
        assert delegate.name == 'Test Profile'
        assert delegate.patternBlock == null
        assert delegate.actionBlock == null
    }
}
