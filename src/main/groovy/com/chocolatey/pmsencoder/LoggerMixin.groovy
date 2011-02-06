@Typed
package com.chocolatey.pmsencoder

import org.apache.log4j.Logger

// XXX here be bugs, cargo-culting and other nasties
@Trait class LoggerMixin {
    /*
        every test fails with:

            java.lang.NoSuchMethodError:
                com.chocolatey.pmsencoder.LoggerMixin$TraitImpl.__init_log4j(Lcom/chocolatey/pmsencoder/LoggerMixin;)V

        unless some part of the following statement is executed before the logger
        is returned in getLog()

        note: these don't work:

            private Logger unused = Logger.getRootLogger()
            private Logger unused = Logger.getLogger(this.getClass())
    */

    private Logger unused = Logger.getLogger(this.getClass().name)

    // expose a "log" property - grr, too much magic
    // XXX initializing this as a field (e.g. unused, above)
    // and then returning it doesn't work:
    // it results in every category being java.lang.Class
    public Logger getLog() {
        return Logger.getLogger(this.getClass().name)
    }
}
