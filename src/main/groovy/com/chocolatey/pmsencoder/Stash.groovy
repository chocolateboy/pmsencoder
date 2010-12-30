@Typed
package com.chocolatey.pmsencoder

// a long-winded way of getting Java Strings and Groovy GStrings to play nice
public class Stash extends LinkedHashMap<java.lang.String, java.lang.String> {
    public Stash() {
        super()
    }

    public Stash(Stash old) {
        super()
        old.each { key, value -> this.put(key.toString(), value.toString()) }
    }

    private java.lang.String canonicalize(Object key) {
        java.lang.String name = key.toString()
        name.startsWith('$') ? name : '$' + name
    }

    public Stash(Map<String, String> map) {
        map.each { key, value -> this.put(key.toString(), value.toString()) }
    }

    public java.lang.String put(java.lang.String key, java.lang.String value) {
        super.put(canonicalize(key), value)
    }

    public java.lang.String put(Object key, Object value) {
        super.put(canonicalize(key), value.toString())
    }

    public java.lang.String get(java.lang.String key) {
        super.get(canonicalize(key))
    }

    public java.lang.String get(Object key) {
        super.get(canonicalize(key))
    }
}

