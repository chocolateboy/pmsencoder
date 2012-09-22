@Typed
package com.chocolatey.pmsencoder

// a long-winded way of getting Java Strings and Groovy GStrings to play nice
public class Stash extends LinkedHashMap<java.lang.String, java.lang.String> {
    public Stash() {
        super()
    }

    public Stash(Stash old) {
        super()
        old.each { key, value -> this.put(key, value) }
    }

    public Stash(Map map) {
        map.each { key, value -> this.put(key.toString(), value?.toString()) }
    }

    public Object put(Object key, Object value) {
        super.put(key.toString(), value?.toString())
    }

    public Object get(Object key) {
        super.get(key.toString())
    }

    public boolean containsKey(Object key) {
        super.containsKey(key.toString())
    }
}
