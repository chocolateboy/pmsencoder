script {
    profile ('uri_method') {
        pattern {
            match { uri().toString() == 'mms://www.example.com' }
            match { uri().toString() != 'http://www.example.com' }
            match { uri().scheme == 'mms' }
            match { uri().scheme != 'http' }
        }

        action {
            set '-uri': uri()
            set '-scheme': uri().scheme
        }
    }
}
