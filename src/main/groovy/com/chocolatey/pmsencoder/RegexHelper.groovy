@Typed
package com.chocolatey.pmsencoder

import info.codesaway.util.regex.Pattern
import info.codesaway.util.regex.Matcher

// XXX could be a singleton if we implement caching
// @Singleton(lazy=true)
class RegexHelper {
    static boolean match(String string, String regex, Map<String, String> stash) {
        // Compile and use regular expression
        Pattern pattern = Pattern.compile(regex)
        Matcher matcher = pattern.matcher(string)
        boolean matchFound = matcher.find()

        if (matchFound) {
            /*
		store named groups as name => match pairs in stash
		XXX 0 is the index of the entire matched string, so group indeces start at 1 
		XXX groupCount is the number of explicit groups
	    */
	    /*
	        we have to kick this old-school because groovy ranges
	        are bidirectional (i.e. (1 .. 0) works)
            */

	    int groupCount = matcher.groupCount()

            for (int i = 1; i <= groupCount; ++i) {
                String name = matcher.getGroupName(i)
                if (name) {
                    String value = matcher.group(i, "") /* default to an empty string */
                    stash[name] = value
                }
            }
        }

        return matchFound
    }
}
