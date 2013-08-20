script {
    profile ('Fail 1', stopOnMatch: false) {
        pattern {
            match { var1 = 'value1' } // OK
            match { false } // not OK: stash assignment should not be committed
        }
    }

    profile ('Fail 2', stopOnMatch: false) {
        pattern {
            scrape(source: "foo value2 baz")('\\w+ (?<var2>\\w+) \\w+')
            match { false } // not OK: stash assignment should not be committed
        }
    }

    profile ('Succeed 1', stopOnMatch: false) {
        pattern {
            match { var3 = 'value3' } // OK
            match { true } // OK: stash assignment should be committed
        }
    }

    profile ('Succeed 2', stopOnMatch: false) {
        pattern {
            scrape(source: 'foo value4 baz')('\\w+ (?<var4>\\w+) \\w+')
            match { true } // OK: stash assignment should be committed
        }
    }


}
