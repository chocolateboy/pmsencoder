package com.chocolatey.pmsencoder

import com.sun.jna.Platform

@groovy.transform.CompileStatic
class FileUtil {
    private static List<String> paths

    static {
        paths = System.getenv('PATH')?.tokenize(File.pathSeparator) ?: [] as List<String>
        paths.add(0, (new File('plugins').getAbsolutePath()))
    }

    private static List<String> getPath(Object _interpreter, Object _script) {
        List<String> path = null
        String interpreter = _interpreter?.toString()
        String script = _script?.toString()

        if (isExecutable(script)) {
            path = [ script ]
        } else if (interpreter) {
            path = [ interpreter, script ]
        }

        return path
    }

    public static String which(String filename) {
        String found = null

        if (Platform.isWindows()) {
            filename += '.exe'
        }

        for (path in paths) {
            def file = new File(path, filename)

            if (isExecutable(file)) {
                found = file.getPath()
                break
            }
        }

        return found
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
