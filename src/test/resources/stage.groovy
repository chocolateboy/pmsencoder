// confirm that profiles are sorted by stage (by adding them in reverse order)
// also confirm that document ordering is preserved

end {
    profile('End 1', stopOnMatch: false) { }
}

end {
    profile('End 2', stopOnMatch: false) { }
}

check {
    profile('Check 1', stopOnMatch: false) { }
}

check {
    profile('Check 2', stopOnMatch: false) { }
}

script {
    profile('Script 1', stopOnMatch: false) { }
}

script {
    profile('Script 2', stopOnMatch: false) { }
}

init {
    profile('Init 1', stopOnMatch: false) { }
}

init {
    profile('Init 2', stopOnMatch: false) { }
}

begin {
    profile('Begin 1', stopOnMatch: false) { }
}

begin {
    profile('Begin 2', stopOnMatch: false) { }
}
