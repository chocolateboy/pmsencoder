script {
    profile('Default',   stopOnMatch: false)                { }
    profile('Finalize',  stopOnMatch: false, on: FINALIZE)  { }
    profile('Transcode', stopOnMatch: false, on: TRANSCODE) { }
}
