script {
    profile ('Scrape String') {
        pattern {
            domain 'scrape.string'
        }

        action {
            // scraping from a string
            scrape '^(?<first>\\w+)\\s+(?<second>\\w+$)', [ source: 'scrape string' ]
        }
    }
}
