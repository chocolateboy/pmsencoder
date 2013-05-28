package com.chocolatey.pmsencoder

/*
    a Map<String, Object> that works around GString annoyances:
    http://www.nearinfinity.com/blogs/scott_leberknight/wackiness_with_string_gstring_and.html

    keys and values can be any type, but keys and GString values
    are converted to String before being set.
    note: the Java String type is spelled out to avoid GString surprises

    LinkedHashMap rather than HashMap to preserve insertion order.
*/
@groovy.transform.CompileStatic
public class Stash extends LinkedHashMap<String, Object> {
    public Stash() {
        super()
    }

    public Stash(Map map) {
        super()
        putAll(map)
    }

    public Object put(Object key, Object value) {
        /*
            more Groovy fail: string != gstring
            ... which breaks the stash == wantStash equality test
            in PMSEncoderTestCase for the gstrings.groovy test in
            ProfileTest.groovy.

            it's either this or implement an equals and hashCode that
            treat values as equal if gstring.toString() == string.
            this is easier
        */
        if (value instanceof GString) {
            super.put(key.toString(), value.toString())
        } else {
            super.put(key.toString(), value)
        }
    }

    public Object get(Object key) {
        super.get(key.toString())
    }

    public boolean containsKey(Object key) {
        super.containsKey(key.toString())
    }
}
