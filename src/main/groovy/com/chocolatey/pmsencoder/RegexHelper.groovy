package com.chocolatey.pmsencoder

import info.codesaway.util.regex.Pattern
import info.codesaway.util.regex.Matcher

@groovy.transform.CompileStatic
class MatchResult {
    boolean matched
    List<String> indexed
    Map<String, String> named

    public MatchResult(boolean matched, Map<String, String> named, List<String> indexed) {
        this.matched = matched
        this.named = named
        this.indexed = indexed
    }

    public String getAt(int index) {
        indexed[index]
    }

    public String getAt(String name) {
        named[name]
    }

    public boolean asBoolean() {
        !!matched
    }

    public String toString() {
        return "{ matched: $matched, named: $named, indexed: $indexed }"
    }
}

// XXX could be a singleton if we implement caching
// @Singleton(lazy=true)
@groovy.transform.CompileStatic
class RegexHelper {
    static MatchResult match(Object string, Object regex) {
        Map<String, String> named = new LinkedHashMap<String, String>()
        List<String> indexed = new ArrayList<String>()
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

            indexed << matcher.group(0)

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
                } else if (indexed != null) {
                    String value = matcher.group(i, "") /* default to an empty string */
                    indexed << value
                }
            }
        }

        return new MatchResult(matched, named, indexed)
    }
}
