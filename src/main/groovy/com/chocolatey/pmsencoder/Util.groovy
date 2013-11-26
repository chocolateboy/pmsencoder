package com.chocolatey.pmsencoder

import com.sun.jna.Platform

@groovy.transform.CompileStatic
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

    public static <T> T guard(T defaultValue, Closure<T> closure) {
        T result
        try {
            result = closure()
        } catch (Exception e) {
            result = defaultValue
        }
        return result
    }

    public static String[] cmdListToArray(List<String> list) {
        String[] array = new String[ list.size() ]
        list.toArray(array)
        return array
    }
}
