script {
    profile('Prepend Object Zero') { // prepend a single object when the list is empty
        pattern {
            domain 'prepend.object.zero'
        }

        action {
            hook { prepend('one') }
            downloader { prepend('one') }
            transcoder { prepend('one') }
        }
    }

    profile('Prepend Object One') { // prepend a single object when the list contains one element
        pattern {
            domain 'prepend.object.one'
        }

        action {
            hook = [ 'one' ]
            downloader = [ 'one' ]
            transcoder = [ 'one' ]
            hook { prepend('two') }
            downloader { prepend('two') }
            transcoder { prepend('two') }
        }
    }

    profile('Prepend Object Two') { // prepend a single object when the list contains one element
        pattern {
            domain 'prepend.object.two'
        }

        action {
            hook = [ 'one', 'two' ]
            downloader = [ 'one', 'two' ]
            transcoder = [ 'one', 'two' ]
            hook { prepend('three') }
            downloader { prepend('three') }
            transcoder { prepend('three') }
        }
    }

    profile('Prepend List Zero') { // prepend a list when the list contains one element
        pattern {
            domain 'prepend.list.zero'
        }
        action {
            hook = []
            downloader = []
            transcoder = []
            hook { prepend([ 'one', 'two' ]) }
            downloader { prepend([ 'one', 'two' ]) }
            transcoder { prepend([ 'one', 'two' ]) }
        }
    }

    profile('Prepend List One') { // prepend a list when the list contains one element
        pattern {
            domain 'prepend.list.one'
        }
        action {
            hook = [ 'one' ]
            downloader = [ 'one' ]
            transcoder = [ 'one' ]
            hook { prepend([ 'two', 'three' ]) }
            downloader { prepend([ 'two', 'three' ]) }
            transcoder { prepend([ 'two', 'three' ]) }
        }
    }

    profile('Prepend List Two') { // prepend a list when the list contains two elements
        pattern {
            domain 'prepend.list.two'
        }
        action {
            hook = [ 'one', 'two' ]
            downloader = [ 'one', 'two' ]
            transcoder = [ 'one', 'two' ]
            hook { prepend([ 'three', 'four' ]) }
            downloader { prepend([ 'three', 'four' ]) }
            transcoder { prepend([ 'three', 'four' ]) }
        }
    }
}
