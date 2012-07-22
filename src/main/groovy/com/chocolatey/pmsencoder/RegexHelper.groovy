@Typed
package com.chocolatey.pmsencoder

import info.codesaway.util.regex.Pattern
import info.codesaway.util.regex.Matcher

class MatchResult {
    boolean matched
    List<String> positional
    Map<String, String> named

    public MatchResult(boolean matched, Map<String, String> named, List<String> positional) {
        this.matched = matched
        this.named = named
        this.positional = positional
    }

    public String getAt(int index) {
        positional[index]
    }

    public String getAt(String string) {
        named[string]
    }

    public asBoolean() {
        matched
    }

    public String toString() {
        return "{ matched: $matched, named: $named, positional: $positional }"
    }
}

// XXX could be a singleton if we implement caching
// @Singleton(lazy=true)
class RegexHelper {
    static MatchResult match(Object string, Object regex) {
        List<String> positional = []
        Map<String, String> named = [:]
        def matched = match(string, regex, named, positional)
        return new MatchResult(matched, named, positional)
    }

    static boolean match(Object string, Object regex, Map<String, String> named) {
        match(string, regex, named, null)
    }

    static boolean match(Object string, Object regex, List<String> positional) {
        match(string, regex, null, positional)
    }

    static boolean match(Object string, Object regex, Map<String, String> named, List<String> positional) {
        // Compile and use regular expression
        def pattern = Pattern.compile(regex.toString(), Pattern.DOTALL)
        def matcher = pattern.matcher(string.toString())
        def matched = matcher.find()

        if (matched) {
            /*
                store named groups as name => match pairs in named
                XXX 0 is the index of the entire matched string, so group indices start at 1
                XXX groupCount is the number of explicit groups
            */

            int groupCount = matcher.groupCount()

            if (positional != null) {
                positional << matcher.group(0)
            }

            /*
                we have to use a traditional for-loop here because groovy ranges
                are bidirectional (i.e. (1 .. 0) works)
            */
            for (int i = 1; i <= groupCount; ++i) {
                String name = matcher.getGroupName(i)
                if (name) {
                    if (named != null) {
                        String value = matcher.group(i, "") /* default to an empty string */
                        named.put(name, value)
                    }
                } else if (positional != null) {
                    String value = matcher.group(i, "") /* default to an empty string */
                    positional << value
                }
            }
        }

        return matched
    }
}
