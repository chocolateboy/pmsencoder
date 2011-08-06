@Typed
package com.chocolatey.pmsencoder

import net.pms.PMS

class Util {
    public static List<String> scalarList(Object scalarOrList) {
        if (scalarOrList == null) {
            return []
        } else {
            return (scalarOrList instanceof List) ? scalarOrList.collect { it.toString() } : [ scalarOrList.toString() ]
        }
    }

    // handle values that can be a String or a List.
    // split the former along whitespace and return the latter as-is
    public static List<String> stringList(Object stringOrList) {
        if (stringOrList == null) {
            return []
        } else {
            return (stringOrList instanceof List) ?
                stringOrList.collect { it.toString() } :
                stringOrList.toString().tokenize()
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
