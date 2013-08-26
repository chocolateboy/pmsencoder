script {
    profile('Default',      stopOnMatch: false)                   { }
    profile('Finalize',     stopOnMatch: false, on: FINALIZE)     { }
    profile('Incompatible', stopOnMatch: false, on: INCOMPATIBLE) { }
    profile('Transcode',    stopOnMatch: false, on: TRANSCODE)    { }
}
