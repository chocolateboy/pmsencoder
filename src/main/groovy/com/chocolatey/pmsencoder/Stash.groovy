@Typed
package com.chocolatey.pmsencoder

// a long-winded way of getting Java Strings and Groovy GStrings to play nice
public class Stash extends LinkedHashMap<java.lang.String, Object> {
    public Stash() {
        super()
    }

    public Stash(Stash old) {
        super()
        old.each { key, value -> this.put(key.toString(), value) }
    }

    public Stash(Map map) {
        map.each { key, value -> this.put(key.toString(), value) }
    }

    public Object put(java.lang.String key, Object value) {
        super.put(key, value)
    }

    public Object put(Object key, Object value) {
        super.put(key.toString(), value)
    }

    public Object get(java.lang.String key) {
        super.get(key)
    }

    public Object get(Object key) {
        super.get(key.toString())
    }

    public boolean containsKey(Object key) {
        super.containsKey(key.toString())
    }
}
