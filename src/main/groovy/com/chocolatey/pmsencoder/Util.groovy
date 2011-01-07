@Typed
package com.chocolatey.pmsencoder

class Util {
    static public List<String> scalarList(Object scalarOrList) {
        return (scalarOrList instanceof List) ? scalarOrList.collect { it.toString() } : [ scalarOrList.toString() ]
    }

    // handle values that can be a String or a List.
    // split the former along whitespace and return the latter as-is
    static public List<String> stringList(Object stringOrList) {
        return (stringOrList instanceof List) ?
            stringOrList.collect { it.toString() } :
            stringOrList.toString().tokenize()
    }
}
