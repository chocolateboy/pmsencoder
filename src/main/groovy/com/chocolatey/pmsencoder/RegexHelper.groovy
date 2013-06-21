package com.chocolatey.pmsencoder

import com.google.code.regexp.Matcher
import com.google.code.regexp.MatchResult as NativeMatchResult
import com.google.code.regexp.Pattern

import static java.util.regex.Pattern.DOTALL

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
        matched
    }

    public String toString() {
        return "{ matched: $matched, named: $named, indexed: $indexed }"
    }
}

@groovy.transform.CompileStatic
class RegexHelper {
    static MatchResult match(Object string, Object regex) {
        Map<String, String> named
        List<String> indexed

        Pattern pattern = Pattern.compile(regex.toString(), DOTALL)
        Matcher matcher = pattern.matcher(string.toString())
        boolean matched = matcher.find()

        if (matched) {
            NativeMatchResult matchResult = matcher.toMatchResult()
            named = matchResult.namedGroups()
            indexed = matchResult.orderedGroups()
        } else {
            named = [:] // new LinkedHashMap<String, String>()
            indexed = [] // new ArrayList<String>()
        }

        return new MatchResult(matched, named, indexed)
    }
}
