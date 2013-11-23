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

    public static String shellQuote(Object obj) {
        if (obj == null) {
            return null
        } else {
            String uri = obj.toString()
            // double quote a URI to make it safe for cmd.exe
            // XXX need to test this
            return Platform.isWindows() ? '"' + uri.replaceAll('"', '""') + '"' : uri
        }
    }

    public static String[] cmdListToArray(List<String> list) {
        String[] array = new String[ list.size() ]
        list.toArray(array)
        return array
    }

    public static boolean fileExists(File file) {
        (file != null) && file.exists() && file.isFile()
    }

    public static boolean fileExists(String path) {
        def file = new File(path)
        fileExists(file)
    }

    public static boolean directoryExists(File file) {
        (file != null) && file.exists() && file.isDirectory()
    }

    public static boolean directoryExists(String path) {
        def file = new File(path)
        directoryExists(file)
    }

    public static boolean isExecutable(File file) {
        fileExists(file) && file.canExecute()
    }

    public static boolean isExecutable(String path) {
        def file = new File(path)
        isExecutable(file)
    }
}
