script {
    profile ('Scrape String') {
        pattern {
            domain 'scrape.string'
        }

        action {
            // scraping from a string
            scrape(source: 'scrape string')('^(?<first>\\w+)\\s+(?<second>\\w+$)')
            scrape([ source: 'scrape string' ])('^(?<third>\\w+)\\s+(?<fourth>\\w+$)')
            scrape source: 'scrape string', '^(?<fith>\\w+)\\s+(?<sixth>\\w+$)'
        }
    }
}
