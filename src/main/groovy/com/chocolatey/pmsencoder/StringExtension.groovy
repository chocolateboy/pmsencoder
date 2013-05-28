package com.chocolatey.pmsencoder

@groovy.transform.CompileStatic
public final class StringExtension {
    public static String match(GString self, Object regex) {
        return RegexHelper.match(self, regex)
    }

    public static String match(String self, Object regex) {
        return RegexHelper.match(self, regex)
    }
}
