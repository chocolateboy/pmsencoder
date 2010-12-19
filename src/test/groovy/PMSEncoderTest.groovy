@Typed
package com.chocolatey.pmsencoder

import net.pms.PMS

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

    void testProfileValidationDelegateInitalState() {
        def script = new Script(pms)
        def delegate = new ProfileValidationDelegate(script, "Test Profile")

        assertNotNull(delegate)
        assertEquals("Test Profile", delegate.name)
        assertNull(delegate.patternBlock)
        assertNull(delegate.actionBlock)
    }
}
