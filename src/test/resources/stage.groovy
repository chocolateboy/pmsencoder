// confirm that profiles are sorted by stage (by adding them in reverse order)
// also confirm that document ordering is preserved

script (END) {
    profile('End 1', stopOnMatch: false) { }
}

script (END) {
    profile('End 2', stopOnMatch: false) { }
}

script (CHECK) {
    profile('Check 1', stopOnMatch: false) { }
}

script (CHECK) {
    profile('Check 2', stopOnMatch: false) { }
}

// make sure an explicit stage doesn't break document ordering (i.e. isn't sorted before an implicit stage)
script {
    profile('Default 1', stopOnMatch: false) { }
}

// explicit stage
script (DEFAULT) {
    profile('Default 2', stopOnMatch: false) { }
}

// make sure an implicit stage doesn't break document ordering (i.e. isn't sorted before an explicit stage)
script (DEFAULT) {
    profile('Default 3', stopOnMatch: false) { }
}

// implicit stage
script {
    profile('Default 4', stopOnMatch: false) { }
}

script (INIT) {
    profile('Init 1', stopOnMatch: false) { }
}

script (INIT) {
    profile('Init 2', stopOnMatch: false) { }
}

script (BEGIN) {
    profile('Begin 1', stopOnMatch: false) { }
}

script (BEGIN) {
    profile('Begin 2', stopOnMatch: false) { }
}
