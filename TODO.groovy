/*
    links:

        - *real* (i.e. type-checked/validated) named parameters: http://jira.codehaus.org/browse/GROOVY-3520
        - transitive delegation (via @Delegate): http://jira.codehaus.org/browse/GRECLIPSE-1627
        - non-mavenized dependencies in gradle:
            - https://github.com/Ullink/gradle-repositories-plugin
            - http://issues.gradle.org/browse/GRADLE-2179
            - http://forums.gradle.org/gradle/topics/how_to_use_a_jar_from_a_sourceforge_project_as_a_dependency_in_a_project
            - https://github.com/GradleFx/GradleFx-Examples/blob/master/sdk-autoinstall/build.gradle
*/

// move all of this to GitHub issues

/*
    infinite loop/stack overflow in maven assembly plugin (in Plexus Archiver) with
    Groovy++ 0.2.26: https://groups.google.com/group/groovyplusplus/msg/a765fe77975650db
*/

// script management: disable/enable scripts/settings through the Swing UI (cf. GreaseMonkey)

/*
  investigate adding seek support for YouTube videos:

      http://stackoverflow.com/questions/3302384/youtubes-hd-video-streaming-server-technology
*/

// WEB.groovy (path is relative to the renderer's root folder):

videostream ('Web/TV') {
    uri  = 'mms://example.com/stream'
    name = 'Example'
    thumbnail = 'http://example.com/rss.jpg' // optional
}

// use the user-specified folder, rather than appending the feed name, so feeds can be merged:

videofeed ('Web/YouTube/Favourites') {
    uri = 'http://youtube.com/api/whatever?1-50'
}

videofeed ('Web/YouTube/Favourites') {
    uri = 'http://youtube.com/api/whatever?50-100'
}

// tests for append

// use a web interface because a) Swing sucks and b) headless servers. Only use swing to enable/disable the web server
// and set the port.

// investigate using busybox-w32/ash instead of cmd.exe on Windows

// Pattern: add extension matcher (use the stash URI):

    extension 'm3u8'
    extension ([ 'mp4', 'm4v' ])

// profile: add 'extension' variable

/*
Groovy++ bytecode compilation error (both at compile-time and runtime): see Plugin.groovy

[ERROR] Failed to execute goal org.codehaus.gmaven:gmaven-plugin:1.3:compile (default) on project pmsencoder: startup failed:
[ERROR] /home/chocolateboy/dev/public/pmsencoder/src/main/groovy/com/chocolatey/pmsencoder/Plugin.groovy: 50: Internal Error: java.lang.VerifyError: (class: com/chocolatey/pmsencoder/Plugin, method: <init> signature: ()V) Register 3 contains wrong type
[ERROR] @ line 50, column 5.
[ERROR] public Plugin() {
[ERROR] ^
[ERROR] org.codehaus.groovy.syntax.SyntaxException: Internal Error: java.lang.VerifyError: (class: com/chocolatey/pmsencoder/Plugin, method: <init> signature: ()V) Register 3 contains wrong type
[ERROR] @ line 50, column 5.
[ERROR] at org.mbte.groovypp.compiler.CompilerTransformer.addError(CompilerTransformer.java:92)
[ERROR] at org.mbte.groovypp.compiler.StaticMethodBytecode.<init>(StaticMethodBytecode.java:84)
[ERROR] at org.mbte.groovypp.compiler.StaticMethodBytecode.replaceMethodCode(StaticMethodBytecode.java:98)
[ERROR] at org.mbte.groovypp.compiler.CompileASTTransform.visit(CompileASTTransform.java:108)
[ERROR] at org.codehaus.groovy.transform.ASTTransformationVisitor.visitClass(ASTTransformationVisitor.java:129)
[ERROR] at org.codehaus.groovy.transform.ASTTransformationVisitor$2.call(ASTTransformationVisitor.java:172)
[ERROR] at org.codehaus.groovy.control.CompilationUnit.applyToPrimaryClassNodes(CompilationUnit.java:936)
[ERROR] at org.codehaus.groovy.control.CompilationUnit.doPhaseOperation(CompilationUnit.java:513)
[ERROR] at org.codehaus.groovy.control.CompilationUnit.processPhaseOperations(CompilationUnit.java:491)
[ERROR] at org.codehaus.groovy.control.CompilationUnit.compile(CompilationUnit.java:468)
[ERROR] at org.codehaus.groovy.control.CompilationUnit.compile(CompilationUnit.java:447)
*/

/*
    script loading order:

        builtin scripts
        user scripts

    script stages:

        BEGIN
        INIT
        DEFAULT
        CHECK
        END

document this:

    replacing a profile with a profile with a different name
    does not change its canonical name. this ensures other replacement profiles
    have a predictable, consistent name to refer to

TODO: determine behaviour (if any) if a replacement has a different stage

TODO: re-stage all the profiles in a script block, preserving the natural order

keep a list of Script objects rather than (just) a hash of profiles?

*/

// remove rtmpdump protocol and manage everything through pmsencoder://

// No need to expose pms. Just use PMS.get() as normal

// documentation/example: dlna.getSystemName() can be used to access the original URI e.g.:

    if (dlna.systemName.toURI().protocol == 'concat') { ... }

// bring back reject: e.g.:

    reject uri: '^concat:'
    reject { domain == 'members.example.com' }

/*

    image to video:

        ffmpeg -r 24 -i http://ip:port/plugin/name/imdb_plot?id=42&fmt=png \
            -vcodec mpeg2video -qscale 2 -vframes 1 transcoder.out

*/

// use MPlayer -dumpstream as a downloader/null transcoder
// FIXME: MPlayer can't dump to stdout: http://lists.mplayerhq.hu/pipermail/mplayer-users/2006-April/059898.html

// test Pattern.scrape

// make the rtmp2pms functionality available via a web page (e.g. GitHub page) using JavaScript:
// i.e. enter 1) name/path 2) the command line 3) optional thumbnail URI and click to generate the WEB.conf
// line

// unused example/pattern
// TODO: expose guard in scripts

    import static example.getJson

    profile('Example') {
        pattern {
            // don't match if the query fails
            match {
                json = guard(false) { getJson(uri) }
            }
        }

        action {
            uri = json['uri']
        }
    }

// unused example/pattern: access the renderer (RendererConfiguration):

    def renderer = params.mediaRenderer

// weird Groovy(++) bug:

    println("patternMap class: ${patternMap.class.name}") // fails in stash_objects.groovy
    println("actionMap class: ${patternMap.class.name}") // fails in stash_objects.groovy

// these work:

    println("patternString class: ${patternString.class.name}")
    println("patternList class: ${patternList.class.name}")
    println("patternMap class: ${patternList.getClass().getName()}")

// investigate using gmock: https://code.google.com/p/gmock/

// extension methods (e.g. String.match): figure out why the META-INF method doesn't work

// test nested context blocks

// add XML support to the jSoup methods:

    $(xml: true, 'test')

// http.head(): implement a HeaderMap that implements String getValue() and List<String> getValues()

    def headers = http.head()
    headers.getValue('Content-type') // getValues('Content-type').empty() ? null : getValues('Content-type').get(0)
    headers.getValues('Warning')

// Alternatively/also, return (comma-joined) strings: http://greenbytes.de/tech/webdav/rfc2616.html#message.headers

    headers.get('Warnings')

// log the version of youtube-dl and get-flash-videos if available

// add web audio engine

// implement isCompatible hook; this would allow PMSEncoder to fall through
// to another engine (e.g. VLC). invert the hook name (e.g. SKIP,
// INCOMPATIBLE, REJECT, UNSUPPORTED) so that isCompatible returns false if a
// profile matches:

    profile(on: INCOMPATIBLE) {
        match { domain == 'example.com' }
    }

// change pattern/action to when/then? what about profiles with no 'when' block? allow 'then'
// to be called as e.g. 'always'?

    profile('No When Block 1') {
        then { ... }
    }

    profile('No When Block 2') {
        always { ... }
    }

// replace hook with:

        exec  stringOrList // blocking
        async stringOrList

// e.g.

        async ([ NOTIFY_SEND, 'PMSEncoder', "Playing ${dlna.getName()}" ]) { rv ->
            // optional callback
        }

        def rv = exec '/usr/bin/foo --bar --baz'
        // rv.stdout, rv.stderr, rv.status

// allow the command contexts to be initialised with an executable?

    downloader ('/usr/bin/mydownloader') {
        set '-foo': 'bar'
    }

// XXX can be done already/better with list/string assignment:

    downloader = '/usr/bin/downloader -foo bar'

// allow script to set common profile settings (removes a common gotcha when writing tests).
// profile gets default values from script:

    script (stopOnMatch: true) {
        profile ('Foo')                     { } // inherit stopOnMatch: true
        profile ('Bar', stopOnMatch: false) { } // override
    }

// test: add testResolve method to test the builtin resolvers:

    testResolve(
        'http://www.example.com/foo.html',
        'http://cdn.example.com/video/foo.mp4'
    )
