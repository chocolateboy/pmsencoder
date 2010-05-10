@Typed
package com.chocolatey.pmsencoder

/*
    This whole class is an elaborate hack around the fact that @Mixin annotations don't
    work (i.e. not compile-time), and around the fact that with-log consistently throws
    NPEs
*/

// avoid stub generation funkiness with import ... as by fully-qualifying the log4j class's name
abstract class Logger { // uninstantiable
    // this should really be static, but we don't want to faff around with log5j
    protected org.apache.log4j.Logger getLog() { // expose a "log" property - grr, too much magic
        org.apache.log4j.Logger.getLogger(getClass().name)
    }
}
