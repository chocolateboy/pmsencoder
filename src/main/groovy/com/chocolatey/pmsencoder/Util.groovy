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

    // there is no sane documentation for cmd.exe and there are no[1] useful examples online for
    // our use case (cmd.exe + Java (ProcessBuilder) string array + shell redirection (pipe)) e.g.:
    //
    //    [
    //        'cmd.exe', '/C', 'downloader.exe', '--input', 'URI', '--output', 'DOWNLOADER_OUT',
    //        '|',
    //        'transcoder.exe', '--input', 'DOWNLOADER_OUT', '--output', 'TRANSCODER_OUT'
    //    ]
    //
    // since we already have a working implementation that only escapes URIs, we're going to do the
    // bare minimum to avoid errors rather than trying to achieve "correctness", which is unattainable
    // because: Microsoft. we only escape values that a) contain spaces or b) cmd.exe special characters
    // e.g. |, > &c. things like nested single or double quotes can be handled (by trial and error)
    // if they become an issue, but, with the exception of the rtmpdump[2] --jtv option (which takes a
    // JSON argument(!)), they don't crop up in any of the builtin downloader/transcoder arguments
    // and are atypical characters for streaming video/feed URIs.
    //
    // [1] although it's the opposite of a tutorial, the release notes for JDK 7u25
    // contain partial, tantalising hints for working with^H^H^H^H around cmd.exe without tears:
    // http://www.oracle.com/technetwork/java/javase/7u25-relnotes-1955741.html#jruntime
    //
    // [2] which can be phased out (as a downloader) in favour of the builtin ffmpeg support
    public static String shellQuoteString(Object obj) {
        String str = obj?.toString()

        if ((str != null) && Platform.isWindows() && (str =~ /\s|[&<>()@^|]/)) {
            str = '"' + str + '"'
        }

        return str
    }

    public static List<String> shellQuoteList(List<?> list) {
        return list.collect { shellQuoteString(it) }
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
