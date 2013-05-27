@Typed
package com.chocolatey.pmsencoder

import net.pms.PMS

class Util {
    public static List<String> toStringList(Object maybeList, boolean split = false) {
        if (maybeList == null) {
            // empty list
            return []
        } else {
            if (maybeList instanceof List) {
                // stringify each element
                return maybeList.collect { it.toString() }
            } else if (split) {
                // split along whitespace
                return maybeList.toString().tokenize()
            } else {
                // 1-element list
                return [ maybeList.toString() ]
            }
        }
    }

    public static <T> T guard(T defaultValue, Function0<T> closure) {
        T result
        try {
            result = closure()
        } catch (Exception e) {
            result = defaultValue
        }
        return result
    }

    public static String quoteURI(String uri) {
        // double quote a URI to make it safe for cmd.exe
        // XXX need to test this
        return PMS.get().isWindows() ? '"' + uri.replaceAll('"', '%22') + '"' : uri
    }
}
