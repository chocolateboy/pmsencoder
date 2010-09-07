config {
    profile ("GStrings") {
        def closed_over = 'Hello, world!'

        pattern {
            domain = 'example'
            pattern = closed_over
            match $URI: "http://www.${domain}.com"
        }

        action {
            action = closed_over
            key = 'key'
            value = 'value'
            n = 41
            set "-$key": 'key'                                        // GString key
            set '-value': "$value"                                    // GString value
            $URI = "${$URI}/$domain/$key/$value/${n.toInteger() + 1}" // set GString property value
        }
    }
}
