package com.chocolatey.pmsencoder

/*
    FIXME

    May 30, 2013 4:13:24 AM org.codehaus.groovy.runtime.m12n.MetaInfExtensionModule newModule
    WARNING: Module [pmsencoder-extensions] - Unable to load extension class [com.chocolatey.pmsencoder.StringExtension]
*/

@groovy.transform.CompileStatic
public final class StringExtension {
    public static String match(GString self, Object regex) {
        return RegexHelper.match(self, regex)
    }

    public static String match(String self, Object regex) {
        return RegexHelper.match(self, regex)
    }
}
