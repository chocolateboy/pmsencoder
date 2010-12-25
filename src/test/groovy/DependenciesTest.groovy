@Typed
package com.chocolatey.pmsencoder

class DependenciesTest extends PMSEncoderTestCase {
    void testViddlerDependency() {
        assertMatch([
            script:    '/profile_dependencies.groovy',
            uri:       'http://icanhascheezburger.com/2010/12/14/funny-pictures-video-cat-laptop-touchscreen/',
            matches:   [ 'I Can Has Cheezburger', 'I Can Has Viddler' ],
            wantStash: [ $URI: 'http://www.viddler.com/file/d658b9e8/html5mobile/' ],
            wantArgs:  { List<String> args -> args[1] =~ '\\bAppleWebKit\\b' }
        ])
    }

    void testYouTubeDependency() {
        assertMatch([
            script: '/profile_dependencies.groovy',
            uri: 'http://icanhascheezburger.com/2010/12/13/funny-pictures-videos-film-by-cats/',
            matches: [
               'I Can Has Cheezburger',
               'I Can Has YouTube',
               'YouTube Metadata',
               'YouTube-DL Compatible',
               'YouTube'
            ],
            wantStash: { Stash stash -> stash['$URI'] =~ '\\.youtube\\.com/videoplayback\\?' }
        ])
    }
}
