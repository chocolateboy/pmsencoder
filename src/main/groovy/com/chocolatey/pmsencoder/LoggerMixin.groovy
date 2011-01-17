@Typed
package com.chocolatey.pmsencoder

import org.apache.log4j.Logger

@Trait class LoggerMixin {
    // this should really be static, but we don't want to faff around with log5j
    // plus Groovy++ traits don't support it
    private Logger log4j = Logger.getLogger(this.getClass().name)

    // expose a "log" property - grr, too much magic
    public Logger getLog() {
        return log4j
    }
}
