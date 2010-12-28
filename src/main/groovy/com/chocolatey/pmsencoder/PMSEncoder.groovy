@Typed
package com.chocolatey.pmsencoder

import net.pms.PMS
import net.pms.io.OutputParams

import org.apache.avalon.fortress.util.dag.Vertex
import org.apache.avalon.fortress.util.dag.DirectedAcyclicGraphVerifier as DAG
import org.apache.avalon.fortress.util.dag.CyclicDependencyException

import org.apache.log4j.Level

// if these extend Exception (rather than RuntimeException) Groovy(++?) wraps them in
// InvokerInvocationException, which causes all kinds of tedium.
// For the time being: extend RuntimeException - even though they're both checked

public class MatchFailureException extends RuntimeException { }

public class PMSEncoderException extends RuntimeException {
    // grrr: get "cannot find constructor" errors without this (another GString issue?)
    PMSEncoderException(String msg) {
        super(msg)
    }
}

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

/*
 * this object encapsulates the per-request state passed from/to the PMS transcode launcher (Engine.java).
 */
public class Command extends Logger {
    Stash stash
    List<String> args
    List<String> matches
    OutputParams params
    List<String> hook
    List <String> downloader
    List <String> transcoder
    Level stashAssignmentLogLevel = Level.DEBUG

    private Command(Stash stash, List<String> args, List<String> matches) {
        this.stash = stash
        this.args = args
        this.matches = matches
    }

    public Command() {
        this(new Stash(), [], [])
    }

    public Command(Stash stash) {
        this(stash, [])
    }

    public Command(List<String> args) {
        this(new Stash(), args)
    }

    public Command(Stash stash, List<String> args) {
        this(stash, args, [])
    }

    // convenience constructor: allow the stash to be supplied as a Map<String, String>
    // e.g. new Command([ uri: uri ])
    public Command(Map<String, String> map) {
        this(new Stash(map), [], [])
    }

    public Command(Command other) {
        this(new Stash(other.stash), new ArrayList<String>(other.args), new ArrayList<String>(other.matches))
    }

    public void setParams(OutputParams params) {
        this.params = params
    }

    public boolean equals(Command other) {
        this.stash == other.stash &&
        this.args == other.args &&
        this.matches == other.matches &&
        this.params == other.params &&
        this.downloader == other.downloader &&
        this.transcoder == other.transcoder
    }

    public java.lang.String toString() {
        // can't stringify params until this patch has been applied:
        // https://code.google.com/p/ps3mediaserver/issues/detail?id=863
        "{ stash: $stash, args: $args, matches: $matches, hook: $hook, downloader: $downloader, transcoder: $transcoder }".toString()
    }

    // setter implementation with logged stash assignments
    @Typed(TypePolicy.MIXED) // XXX try to handle GStrings
    public String let(String name, String value) {
        if ((stash[name] == null) || (stash[name] != value.toString())) {
            if (stashAssignmentLogLevel != null) {
                log.log(stashAssignmentLogLevel, "setting $name to $value")
            }
            stash[name] = value
        }

        return value // for chaining: foo = bar = baz i.e. foo = (bar = baz)
    }
}

class Matcher extends Logger {
    // FIXME: this is only public for a (single) test
    // 1) Script has the same scope as Matcher so they could be merged,
    // but we don't want to expose load()
    // 2) the script "globals" (e.g. $DEFAULT_TRANSCODER_ARGS) could be moved here
    public final Script script

    Matcher(PMS pms) {
        this.script = new Script(pms)
    }

    void load(String path, String fileName = path) {
        load(new File(path), fileName)
    }

    void load(URL url, String fileName = url.toString()) {
        load(url.openStream(), fileName)
    }

    void load(File file, String fileName = file.getPath()) {
        load(new FileInputStream(file), fileName)
    }

    void load(InputStream stream, String fileName) {
        load(new InputStreamReader(stream), fileName)
    }

    void load(Reader reader, String fileName) {
        def binding = new Binding(script: script.&script)
        def groovy = new GroovyShell(binding)
        groovy.evaluate(reader, fileName)
    }

    @Typed(TypePolicy.DYNAMIC) // XXX needed to handle GStrings
    boolean match(Command command, boolean useDefault = true) {
        if (useDefault) {
            // watch out: there's a GString about
            script.$DEFAULT_TRANSCODER_ARGS.each { command.args << it.toString() }
        }

        def matched = script.match(command) // we could use the @Delegate annotation, but this is cleaner/clearer

        if (matched) {
            log.trace("command: $command")
        }

        return matched
    }
}

class Script extends Logger {
    private Map<String, Vertex> profiles = [:] // defaults to LinkedHashMap
    private boolean verified = false
    private List<Profile> orderedProfiles

    // DSL fields (mutable)
    public List<String> $DEFAULT_TRANSCODER_ARGS = []
    public List<Integer> $YOUTUBE_ACCEPT = []
    public PMS $PMS

    public Script(PMS pms) {
        $PMS = pms
    }

    private void verifyDependencies() {
        def vertices = profiles.values().toList()

        def verifiedVertices = vertices.every { Vertex vertex ->
            def valid = true

            try {
                DAG.verify(vertex)
            } catch (CyclicDependencyException e) {
                log.error("detected cyclic dependency between profiles: " + e.message)
                valid = false
            }

            return valid
        }

        if (verifiedVertices) {
            DAG.topologicalSort(vertices) // in-place
            orderedProfiles = vertices.collect { (it.node as Reference<Profile>).get() }
        } else {
            orderedProfiles = null
        }

        verified = true
    }

    boolean match(Command command) {
        log.debug("matching URI: ${command.stash['$URI']}")

        if (verified == false) {
            verifyDependencies()
            assert verified == true
        }

        if (orderedProfiles != null) {
            orderedProfiles.each { profile ->
                if (profile.match(command)) {
                    // XXX make sure we take the name from the profile itself
                    // rather than the Map key - the latter may have been usurped
                    // by a profile with a different name
                    command.matches << profile.name
                }
            }
        } else {
            log.error('skipping match due to profile dependency cycle')
        }

        return command.matches.size() > 0
    }

    // DSL method
    @Typed(TypePolicy.MIXED) // Groovy++ doesn't support delegation
    void script(Closure closure) {
        this.with(closure) // run at script compile-time
    }

    // DSL method
    @Typed(TypePolicy.MIXED) // Groovy++ doesn't support delegation
    // a Profile consists of a name, a pattern block and an action block - all
    // determined when the script is loaded/compiled
    // XXX more annoying DDWIM magic: Groovy reorders the arguments
    // http://enfranchisedmind.com/blog/posts/groovy-argument-reordering/
    protected void profile (Map<String, Object> options = [:], String name, Closure closure) throws PMSEncoderException {
        // run the profile block at compile-time to extract its pattern and action blocks,
        // but invoke them at runtime
        def extendz = options['extends'] as String
        def replaces= options['replaces'] as String
        def predecessors, successors

        if (options['before'] != null) {
            predecessors = (options['before'] instanceof List) ?
                options['before'] as List<String> :
                [ options['before'] as String ]
        }

        if (options['after'] != null) {
            successors = (options['after'] instanceof List) ?
                options['after'] as List<String> :
                [ options['after'] as String ]
        }

        if (replaces != null) {
            log.info("replacing profile $replaces with: $name")
        } else if (profiles[name] != null) {
            log.info("replacing profile: $name")
        } else {
            log.info("registering profile: $name")
        }

        Profile profile = new Profile(name, this)

        // TODO need detailed diagnostics
        try {
            profile.extractBlocks(closure)

            if (extendz != null) {
                if (profile[extendz] == null) {
                    log.error("attempt to extend a nonexistent profile: $extendz")
                } else {
                    def base = (profiles[extendz].node as Reference<Profile>).get()
                    profile.assignPatternBlockIfNull(base)
                    profile.assignActionBlockIfNull(base)
                }
            }

            // this is why name is defined both as the key of the map and in the profile
            // itself. the key allows replacement
            def target

            if (replaces != null) {
                target = replaces
            } else {
                target = name
            }

            if (profiles[target] != null) {
                (profiles[target].node as Reference<Profile>).set(profile)
            } else {
                def vertex = new Vertex(new Reference<Profile>(profile))
                profiles[target] = vertex
                verified = false
            }

            if (predecessors != null) {
                predecessors.each { String before ->
                    if (profiles[before] == null) {
                        log.error("attempt to define a predecessor for a nonexistent profile: $name before $before")
                    } else {
                        log.info("adding dependency: $target before $before")
                        profiles[before].addDependency(profiles[target])
                    }
                }
            }

            if (successors != null) {
                successors.each { String after ->
                    if (profiles[after] == null) {
                        log.error("attempt to define a successor for a nonexistent profile: $name after $after")
                    } else {
                        log.info("adding dependency: $target after $after")
                        profiles[target].addDependency(profiles[after])
                    }
                }
            }
        } catch (Throwable e) {
            log.error("invalid profile ($name): " + e.getMessage())
        }
    }
}

// this holds a reference to the pattern and action, and isn't delegated to at runtime
class Profile extends Logger {
    private final Script script
    protected Closure patternBlock
    protected Closure actionBlock

    public final String name

    Profile(String name, Script script) {
        this.name = name
        this.script = script
    }

    @Typed(TypePolicy.MIXED) // Groovy++ doesn't support delegation
    void extractBlocks(Closure closure) {
        def delegate = new ProfileValidationDelegate(script, name)
        // wrapper method: runs the closure then validates the result, raising an exception if anything is amiss
        delegate.runProfileBlock(closure)

        // we made it without triggering an exception, so the two fields are sane: save them
        this.patternBlock = delegate.patternBlock // possibly null
        this.actionBlock = delegate.actionBlock   // possibly null
    }

    // pulled out of the match method below so that type-softening is isolated
    // note: keep it here rather than making it a method in Pattern: trying to keep the delegates
    // clean (cf. Ruby's BlankSlate)
    @Typed(TypePolicy.MIXED) // Groovy++ doesn't support delegation
    boolean runPatternBlock(Pattern delegate) {
        if (patternBlock == null) {
            // unconditionally match
            log.trace('no pattern block supplied: matched OK')
        } else {
            // pattern methods short-circuit matching on failure by throwing a MatchFailureException,
            // so we need to wrap this in a try/catch block

            try {
                delegate.with(patternBlock)
            } catch (MatchFailureException e) {
                log.trace('pattern block: caught match exception')
                // one of the match methods failed, so the whole block failed
                log.debug("match $name: failure")
                return false
            }

            // success simply means "no match failure exception was thrown" - this also handles cases where the
            // pattern block is empty
            log.trace('pattern block: matched OK')
        }

        log.debug("match $name: success")
        return true
    }

    // pulled out of the match method below so that type-softening is isolated
    // note: keep it here rather than making it a method in Action: trying to keep the delegates
    // clean (cf. Ruby's BlankSlate)
    @Typed(TypePolicy.MIXED) // Groovy++ doesn't support delegation
    boolean runActionBlock(Action delegate) {
        if (actionBlock != null) {
            delegate.with(actionBlock)
        } else {
            return true
        }
    }

    boolean match(Command command) {
        def newCommand = new Command(command)

        // avoid clogging up the logfile with pattern-block assignments. If the pattern doesn't match,
        // the assigments are irrelevant; and if it does match, the assignments are logged (below)
        // as the pattern's temporary stash is merged into the command stash. Rather than suppressing these
        // assignments completely, log them at the lowest (TRACE) level
        newCommand.setStashAssignmentLogLevel(Level.TRACE)

        // the pattern block has its own command object (which is initially the same as the action block's).
        // if the match succeeds, then the pattern block's stash is merged into the action block's stash.
        // this ensures that a partial match (i.e. a failed match) with side-effects/bindings doesn't contaminate
        // the action, and, more importantly, it defers logging until the whole pattern block has
        // completed successfully
        def pattern = new Pattern(script, newCommand)

        log.debug("matching profile: $name")

        // returns true if all matches in the block succeed, false otherwise 
        if (runPatternBlock(pattern)) {
            // we can now merge any side-effects (currently only modifications to the stash).
            // first merge (with logging)
            newCommand.stash.each { name, value -> command.let(name, value) }
            // now run the actions
            def action = new Action(script, command)
            runActionBlock(action)
            return true
        } else {
            return false
        }
    }

    public void assignPatternBlockIfNull(Profile profile) {
        // XXX where is ?= ?
        if (this.patternBlock == null) {
            this.patternBlock = profile.patternBlock
        }
    }

    public void assignActionBlockIfNull(Profile profile) {
        // XXX where is ?= ?
        if (this.actionBlock == null) {
            this.actionBlock = profile.actionBlock
        }
    }
}

// i.e. a delegate with access to a Script
public class ScriptDelegate extends Logger {
    private Script script

    public ScriptDelegate(Script script) {
        this.script = script
    }

    // DSL properties

    // $SCRIPT: read-only
    protected final Script get$SCRIPT() {
        script
    }

    // $PMS: read-only
    protected final PMS get$PMS() {
        script.$PMS
    }

    // DSL getter: $DEFAULT_TRANSCODER_ARGS
    protected List<String> get$DEFAULT_TRANSCODER_ARGS() {
        script.$DEFAULT_TRANSCODER_ARGS
    }

    // DSL setter: $DEFAULT_TRANSCODER_ARGS
    protected List<String> get$DEFAULT_TRANSCODER_ARGS(List<String> args) {
        script.$DEFAULT_TRANSCODER_ARGS = args
    }

    // DSL getter: $YOUTUBE_ACCEPT
    protected List<String> get$YOUTUBE_ACCEPT() {
        script.$YOUTUBE_ACCEPT
    }

    // DSL setter: $YOUTUBE_ACCEPT
    protected List<String> get$YOUTUBE_ACCEPT(List<String> args) {
        script.$YOUTUBE_ACCEPT = args
    }
}

// i.e. a delegate with access to a Command
public class CommandDelegate extends ScriptDelegate {
    private Command command
    private final Map<String, String> cache = [:] // only needed/used by scrape()
    @Lazy protected HTTPClient http = new HTTPClient()

    public CommandDelegate(Script script, Command command) {
        super(script)
        this.command = command
    }

    // DSL properties

    // DSL accessor ($ARGS): read-only
    protected List<String> get$ARGS() {
        command.args
    }

    // DSL accessor ($ARGS): read-write
    @Typed(TypePolicy.DYNAMIC) // try to handle GStrings
    // FIXME: test this!
    protected List<String> set$ARGS(List<String> args) {
        command.args = args.collect { it.toString() } // handle GStrings
    }

    // $COMMAND: read-only
    protected Command get$COMMAND() {
        command
    }

    // DSL accessor ($DOWNLOADER): read-only
    protected List<String> get$DOWNLOADER() {
        command.downloader
    }

    /*
        XXX Groovy/Groovy++ fail

        if two setters for $DOWNLOADER are defined (one for String and another for List<String>)
        Groovy/Groovy++ always uses List<String> and complains at runtime that
        it can't cast a GString into List<String>:

        Cannot cast object '/usr/bin/downloader string http://www.downloader-string.com'
        with class 'org.codehaus.groovy.runtime.GStringImpl' to class 'java.util.List'

        workaround: define just one setter and determine the type with instanceof
    */

    // DSL accessor ($DOWNLOADER): read-write
    @Typed(TypePolicy.DYNAMIC) // try to handle GStrings
    protected List<String> set$DOWNLOADER(Object downloader) {
        def list = ((downloader instanceof List) ? downloader : downloader.toString().tokenize()) as List
        command.downloader = list.collect { it.toString() }
    }

    // DSL accessor ($HOOK): read-only
    protected List<String> get$HOOK() {
        command.hook
    }

    // DSL accessor ($HOOK): read-write
    // see $DOWNLOADER above for implementation notes
    @Typed(TypePolicy.DYNAMIC) // try to handle GStrings
    protected List<String> set$HOOK(Object hook) {
        def list = ((hook instanceof List) ? hook : hook.toString().tokenize()) as List
        command.hook = list.collect { it.toString() }
    }

    // $HTTP: read-only
    protected HTTPClient get$HTTP() {
        http
    }

    // $LOGGER: read-only
    protected org.apache.log4j.Logger get$LOGGER() {
        log
    }

    // DSL accessor ($MATCHES): read-only
    protected List<String> get$MATCHES() {
        command.matches
    }

    // $PARAMS: read-only
    public OutputParams get$PARAMS() {
        command.params
    }

    // $STASH: read-only
    protected final Stash get$STASH() {
        command.stash
    }

    // DSL accessor ($TRANSCODER): read-only
    protected List<String> get$TRANSCODER() {
        command.transcoder
    }

    // DSL accessor ($TRANSCODER): read-write
    // see $DOWNLOADER above for implementation notes
    @Typed(TypePolicy.DYNAMIC) // try to handle GStrings
    protected List<String> set$TRANSCODER(Object transcoder) {
        def list = ((transcoder instanceof List) ? transcoder : transcoder.toString().tokenize()) as List
        command.transcoder = list.collect { it.toString() }
    }

    // DSL getter
    protected String propertyMissing(String name) throws PMSEncoderException {
        command.stash[name]
    }

    // DSL setter
    protected String propertyMissing(String name, Object value) {
        command.let(name, value.toString())
    }

    // DSL method - can be called from a pattern or an action.
    // actions inherit this method, whereas patterns add the
    // short-circuiting behaviour and delegate to this via super.scrape(...)
    @Typed(TypePolicy.MIXED) // XXX try to handle GStrings
    boolean scrape(String regex, Map<String, String> options = [:]) {
        def uri = options['uri'] ?: $STASH['$URI']
        def document = cache[uri]
        def newStash = new Stash()
        def scraped = false

        if (document == null) {
            log.debug("getting $uri")
            assert http != null
            document = cache[uri] = http.get(uri)
        }

        if (document == null) {
            log.error('document not found')
            return scraped
        }

        log.debug("matching content of $uri against $regex")

        if (RegexHelper.match(document, regex, newStash)) {
            log.debug('success')
            newStash.each { name, value -> command.let(name, value) }
            scraped = true
        } else {
            log.debug('failure')
        }

        return scraped
    }
}

class ProfileValidationDelegate extends ScriptDelegate {
    public Closure patternBlock
    public Closure actionBlock
    public String name

    ProfileValidationDelegate(Script script, String name) {
        super(script)
        this.name = name
    }

    // DSL method
    private void action(Closure closure) throws PMSEncoderException {
        if (this.actionBlock == null) {
            this.actionBlock = closure
        } else {
            throw new PMSEncoderException("invalid profile ($name): multiple action blocks defined")
        }
    }

    // DSL method
    private void pattern(Closure closure) throws PMSEncoderException {
        if (this.patternBlock == null) {
            this.patternBlock = closure
        } else {
            throw new PMSEncoderException("invalid profile ($name): multiple pattern blocks defined")
        }
    }

    @Typed(TypePolicy.MIXED) // Groovy++ doesn't support delegation
    private runProfileBlock(Closure closure) {
        this.with(closure)
        // the pattern block is optional; if not supplied, the profile always matches
        // the action block is optional; if not supplied no action is performed
    }
}

class Pattern extends CommandDelegate {
    private static final MatchFailureException STOP_MATCHING = new MatchFailureException()

    Pattern(Script script, Command command) {
        super(script, command)
    }

    // DSL setter - overrides the CommandDelegate method to avoid logging,
    // which is handled later (if the match succeeds) by merging the pattern
    // block's temporary stash
    protected String propertyMissing(String name, Object value) {
        $STASH[name] = value
    }

    // DSL method
    @Typed(TypePolicy.DYNAMIC) // XXX try to handle GStrings
    void domain(String name) {
        if (!matchString($STASH['$URI'], domainToRegex(name.toString()))) {
            throw STOP_MATCHING
        }
    }

    // DSL method (alias for domain)
    @Typed(TypePolicy.DYNAMIC) // XXX try to handle GStrings
    void domains(String name) {
        domain(name)
    }

    // DSL method
    @Typed(TypePolicy.DYNAMIC) // XXX try to handle GStrings
    void domain(List<String> domains) {
        if (!(domains.any { name -> matchString($STASH['$URI'], domainToRegex(name.toString())) })) {
            throw STOP_MATCHING
        }
    }

    // DSL method (alias for domain)
    @Typed(TypePolicy.DYNAMIC) // XXX try to handle GStrings
    void domains(List<String> domains) {
        domain(domains)
    }

    // DSL method
    @Typed(TypePolicy.DYNAMIC) // XXX try to handle GStrings
    void protocol(String scheme) {
        if (!matchString($STASH['$URI'], "^${scheme}://")) {
            throw STOP_MATCHING
        }
    }

    // DSL method
    @Typed(TypePolicy.DYNAMIC) // XXX try to handle GStrings
    void protocol(List<String> schemes) {
        if (!(schemes.any { scheme -> matchString($STASH['$URI'], "^${scheme}://") })) {
            throw STOP_MATCHING
        }
    }

    // DSL method
    @Typed(TypePolicy.DYNAMIC) // XXX try to handle GStrings
    void match(Map<String, Object> map) {
        map.each { name, value ->
            def list = (value instanceof List) ? value as List : [ value ]
            match($STASH[name.toString()], list)
        }
    }

    // DSL method
    @Typed(TypePolicy.DYNAMIC) // XXX try to handle GStrings
    void match(String name, List values) {
        if (!(values.any { value -> matchString(name.toString(), value.toString()) })) {
            throw STOP_MATCHING
        }
    }

    // DSL method
    @Typed(TypePolicy.DYNAMIC) // XXX try to handle GStrings
    void match(String name, Object value) {
        def list = (value instanceof List) ? value as List : [ value ]
        match(name.toString(), list)
    }

    // DSL method
    @Typed(TypePolicy.DYNAMIC) // XXX try to handle GStrings
    void match(String name) {
        if (!$MATCHES.contains(name.toString())) {
            throw STOP_MATCHING
        }
    }

    // DSL method
    @Typed(TypePolicy.DYNAMIC) // XXX try to handle GStrings
    void match(List<String> names) {
        if (!$MATCHES.containsAll(names)) {
            throw STOP_MATCHING
        }
    }

    // DSL method
    @Typed(TypePolicy.DYNAMIC) // XXX try to handle GStrings
    void match(Closure closure) {
        log.debug('running match block')

        if (closure()) {
            log.debug('success')
        } else {
            log.debug('failure')
            throw STOP_MATCHING
        }
    }

    @Typed(TypePolicy.DYNAMIC) // XXX try to handle GStrings
    private boolean matchString(String name, String value) {
        if (name == null) {
            log.error('invalid match: name is not defined')
        } else if (value == null) {
            log.error('invalid match: value is not defined')
        } else {
            log.debug("matching $name against $value")

            if (RegexHelper.match(name.toString(), value.toString(), $STASH)) {
                log.debug('success')
                return true // abort default failure below
            } else {
                log.debug("failure")
            }
        }

        return false
    }

    // DSL method
    @Typed(TypePolicy.DYNAMIC) // XXX try to handle GStrings
    protected String domainToRegex(String domain) {
        def escaped = domain.replaceAll('\\.', '\\\\.')
        return "^https?://(\\w+\\.)*${escaped}(/|\$)".toString()
    }

    /*
        We don't have to worry about stash-assignment side-effects, as they're
        only committed if the whole pattern block succeeds. This is handled
        up the callstack (in Profile.match)
    */

    // DSL method
    @Override // for documentation; Groovy doesn't require it
    @Typed(TypePolicy.DYNAMIC) // XXX try to handle GStrings
    boolean scrape(String regex, Map<String, String> options = [:]) {
        if (super.scrape(regex, options)) {
            return true
        } else {
            throw STOP_MATCHING
        }
    }
}

/* XXX: add configurable HTTP proxy support? */
class Action extends CommandDelegate {
    Action(Script script, Command command) {
        super(script, command)
    }

    boolean isOption(String arg) {
        // a rare use case for Groovy's annoyingly lax definition of false.
        // and it's not really a use case because it requires two lines of
        // explanation: null and an empty string evaluate as false

        if (arg) {
           return arg ==~ /^-[^0-9].*$/
        } else {
            return false
        }
    }

    /*
        1) get the URI pointed to by options['uri'] or stash['$URI'] (if it hasn't already been retrieved)
        2) perform a regex match against the document
        3) update the stash with any named captures
    */

    // define a variable in the stash
    // DSL method
    @Typed(TypePolicy.DYNAMIC) // XXX try to handle GStrings
    void let(Map<String, String> map) {
        map.each { key, value ->
            $COMMAND.let(key, value)
        }
    }

    // DSL method
    @Typed(TypePolicy.DYNAMIC) // XXX try to handle GStrings
    void set(Map<String, String> map) {
        // the sort order is predictable (for tests) as long as we (and Groovy) use LinkedHashMap
        map.each { name, value -> setArg(name.toString(), value.toString()) }
    }

    // DSL method
    @Typed(TypePolicy.DYNAMIC) // XXX try to handle GStrings
    void set(String name) {
        setArg(name.toString(), null)
    }

    // set a transcoder option - create it if it doesn't exist
    // DSL method
    @Typed(TypePolicy.MIXED) // XXX try to handle GStrings
    void setArg(String name, String value = null) {
        assert name != null

        def index = $ARGS.findIndexOf { it == name }

        if (index == -1) {
            if (value != null) {
                log.debug("adding $name $value")
                /*
                    XXX squashed bug: careful not to perform operations on $STASH or $COMMAND.args
                    that return and subsequenly operate on a new value
                    (i.e. make sure they're modified in place):

                        def args = $COMMAND.args
                        args += ... // XXX doesn't modify $COMMAND.args
                */
                $ARGS << name << value
            } else {
                log.debug("adding $name")
                $ARGS << name
            }
        } else if (value != null) {
            log.debug("setting $name to $value")
            $ARGS[ index + 1 ] = value
        }
    }

    /*
        perform a string search-and-replace in the value of a transcoder option.
    */

    // DSL method
    @Typed(TypePolicy.MIXED) // XXX try to handle GStrings
    void replace(Map<String, Map<String, String>> replaceMap) {
        // the sort order is predictable (for tests) as long as we (and Groovy) use LinkedHashMap
        replaceMap.each { name, map ->
            // squashed bug (see  above): take care to $ARGS in-place
            def index = $ARGS.findIndexOf { it == name }

            if (index != -1) {
                map.each { search, replace ->
                    log.debug("replacing $search with $replace in $name")
                    // TODO support named captures
                    def value = $ARGS[ index + 1 ]

                    if (value) {
                        // XXX bugfix: strings are immutable!
                        $ARGS[ index + 1 ] = value.replaceAll(search, replace)
                    }
                }
            }
        }
    }

    private Map<String, String> getFormatURLMap(String video_id) {
        def fmt_url_map = [:]

        def found =  [ '&el=embedded', '&el=detailspage', '&el=vevo' , '' ].any { String param ->
            def uri = "http://www.youtube.com/get_video_info?video_id=${video_id}${param}&ps=default&eurl=&gl=US&hl=en"
            def regex = '\\bfmt_url_map=(?<youtube_fmt_url_map>[^&]+)'
            def newStash = new Stash()
            def document = http.get(uri)

            if ((document != null) && RegexHelper.match(document, regex, newStash)) {
                // XXX type-inference fail
                List<String> fmt_url_pairs = URLDecoder.decode(newStash['$youtube_fmt_url_map']).tokenize(',')
                fmt_url_pairs.inject(fmt_url_map) { Map<String, String> map, String pair ->
                    // XXX type-inference fail
                    List<String> kv = pair.tokenize('|')
                    map[kv[0]] = kv[1]
                    return map
                }
                return true
            } else {
                return false
            }
        }

        return found ? fmt_url_map : null
    }

    /*
        given (in the stash) the $youtube_video_id and $youtube_t values of a YouTube stream URI
        (i.e. the direct link to a video), construct the full URI with various $fmt values in
        succession and set the stash $URI value to the first one that's valid (based on a HEAD request)
    */

    // DSL method
    @Typed(TypePolicy.MIXED) // XXX try to handle GStrings
    void youtube(List<Integer> formats = $YOUTUBE_ACCEPT) {
        def uri = $STASH['$URI']
        def video_id = $STASH['$youtube_video_id']
        def t = $STASH['$youtube_t']
        def found = false

        assert video_id != null
        assert t != null

        $COMMAND.let('$youtube_uri', uri)

        if (formats.size() > 0) {
            def fmt_url_map = getFormatURLMap(video_id)
            if (fmt_url_map != null) {
                log.trace('fmt_url_map: ' + fmt_url_map)

                found = formats.any { fmt ->
                    log.debug("checking fmt_url_map for $fmt")
                    def stream_uri = fmt_url_map[fmt.toString()]

                    if (stream_uri != null) {
                        // set the new URI
                        log.debug('success')
                        $COMMAND.let('$youtube_fmt', fmt)
                        $COMMAND.let('$URI', stream_uri)
                        return true
                    } else {
                        log.debug('failure')
                        return false
                    }
                }
            } else {
                log.fatal("can't find fmt -> URI map in video metadata")
            }
        }  else {
            log.fatal("no formats defined for $uri")
        }

        if (!found) {
            log.fatal("can't retrieve stream URI for $uri")
        }
    }

    // DSL method: append a list of options to the command's args list
    void append(List<String> args) {
        $COMMAND.args += args
    }

    // DSL method: prepend a list of options to the command's args list
    void prepend(List<String> args) {
        $COMMAND.args = args + $ARGS
    }

    // DSL method: remove multiple option names and their corresponding values if they have one
    void remove(List<String> optionNames) {
        optionNames.each { remove(it.toString()) }
    }

    // DSL method: remove a single option name and its corresponding value if it has one
    void remove(String optionName) {
        assert optionName != null

        def index = $ARGS.findIndexOf { it == optionName }

        if (index >= 0) {
            def lastIndex = $ARGS.size() - 1
            def nextIndex = index + 1

            if (nextIndex <= lastIndex) {
                def nextArg = $ARGS[ nextIndex ]

                if (isOption(nextArg)) {
                    log.debug("removing: $optionName")
                } else {
                    log.debug("removing: $optionName $nextArg")
                    $ARGS.remove(nextIndex) // remove this first so the index is preserved for the option name
                }
            }

            $ARGS.remove(index)
        }
    }
}
