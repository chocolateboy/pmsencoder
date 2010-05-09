@Typed
package com.chocolatey.pmsencoder

import org.apache.log4j.Logger as Log4JLogger

/*
    This whole class is an elaborate hack around the fact that @Mixin annotations don't
    work (i.e. not compile-time), and around the fact that with-log consistently throws
    NPEs
*/

abstract class Logger { // uninstantiable
    protected static Log4JLogger getLog() { // expose a "log" property - grr, too much magic
        Log4JLogger.getLogger(getClass().name)
    }
}
