@Typed
package com.chocolatey.pmsencoder

class PMSEncoderTest extends PMSEncoderTestCase {
    void testCommandClone() {
        def command = new Command([ foo: "bar" ], [ "baz", "quux" ])
        assert command != null
        def newCommand = new Command(command)
        assert newCommand != null

        assert !newCommand.stash.is(command.stash)
        assert !newCommand.args.is(command.args)
        assert !newCommand.is(command)
        assert newCommand.stash == [ foo: "bar" ]
        assert newCommand.args == [ "baz", "quux" ]
    }

    void testStashClone() {
        def stash = new Stash([ foo: "bar" ])
        assert stash != null
        def newStash = new Stash(stash)
        assert newStash != null

        assert !newStash.is(stash)
        assert newStash == [ foo: "bar" ]
    }

    void testProfileBlockDelegateInitalState() {
        def delegate = new ProfileBlockDelegate("Test Profile")

        assert delegate != null
        assert delegate.name == "Test Profile"
        assert delegate.patternBlock == null
        assert delegate.actionBlock == null
    }
}
