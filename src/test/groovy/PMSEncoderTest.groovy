@Typed
package com.chocolatey.pmsencoder

class PMSEncoderTest extends PMSEncoderTestCase {
    void testCommandClone() {
        def command = new Command([ foo: "bar" ], [ "baz", "quux" ])
        assertNotNull(command)
        def newCommand = new Command(command)
        assertNotNull(newCommand)

        assertNotSame(command.stash, newCommand.stash)
        assertNotSame(command.args, newCommand.args)
        assertNotSame(command, newCommand)
        assertEquals([ foo: "bar" ], newCommand.stash)
        assertEquals([ "baz", "quux" ], newCommand.args)
    }

    void testStashClone() {
        def stash = new Stash([ foo: "bar" ])
        assertNotNull(stash)
        def newStash = new Stash(stash)
        assertNotNull(newStash)

        assertNotSame(stash, newStash)
        assertEquals([ foo: "bar" ], newStash)
    }

    void testProfileBlockDelegateInitalState() {
        def config = new Config()
        def delegate = new ProfileBlockDelegate(config, "Test Profile")

        assertNotNull(delegate)
        assertEquals("Test Profile", delegate.name)
        assertNull(delegate.patternBlock)
        assertNull(delegate.actionBlock)
    }
}
