@Typed
package com.chocolatey.pmsencoder

import static com.chocolatey.pmsencoder.Util.guard

class UtilTest extends PMSEncoderTestCase {
    void testGuard() {
        assert guard(1) { Integer.parseInt("42") } == 42
        assert guard(2) { Integer.parseInt(null) } == 2
        assert guard(3) { Integer.parseInt("") } == 3
        assert guard(4) { Integer.parseInt("Forty Two") } == 4
    }
}
