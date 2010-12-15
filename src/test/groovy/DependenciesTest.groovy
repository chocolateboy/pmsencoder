@Typed
package com.chocolatey.pmsencoder

class DependenciesTest extends PMSEncoderTestCase {
    void testViddlerDependency() {
        def customConfig = this.getClass().getResource('/profile_dependencies.groovy')
        def uri = 'http://icanhascheezburger.com/2010/12/14/funny-pictures-video-cat-laptop-touchscreen/'
        def command = new Command([ $URI: uri ])

        matcher.load(customConfig)
        // bypass Groovy's annoyingly loose definition of true
        assertSame(true, matcher.match(command, false)) // false: don't use default transcoder args

        assert command.matches == [ 'I Can Has Cheezburger', 'I Can Has Viddler' ]
        assert command.stash == [ $URI: 'http://www.viddler.com/file/d658b9e8/html5mobile/' ]
        assert command.args[1] =~ 'AppleWebKit'
    }

    void testYouTubeDependency() {
        def customConfig = this.getClass().getResource('/profile_dependencies.groovy')
        def uri = 'http://icanhascheezburger.com/2010/12/13/funny-pictures-videos-film-by-cats/'
        def command = new Command([ $URI: uri ])

        matcher.load(customConfig)
        // bypass Groovy's annoyingly loose definition of true
        assertSame(true, matcher.match(command, false)) // false: don't use default transcoder args

        assert command.matches == [
           'I Can Has Cheezburger',
           'I Can Has YouTube',
           'YouTube Metadata',
           'YouTube-DL Compatible',
           'YouTube'
        ]

        assert command.stash['$URI'] =~ '\\.youtube\\.com/videoplayback\\?'
    }
}
