config {
    profile ('Domain String') {
        pattern {
            domain 'domain-string.com'
        }
    }

    profile ('Domain List') {
        pattern {
            domain([ 'foo.com', 'domain-list.com', 'bar.com' ])
        }
    }

    profile ('Domains String') {
        pattern {
            domains 'domains-string.com'
        }
    }

    profile ('Domains List') {
        pattern {
            domains([ 'foo.com', 'domains-list.com', 'bar.com' ])
        }
    }

    profile ('Got Dot') {
        pattern {
            domain 'dot.com'
        }
    }
}
