script {
    profile ('rtsp://') {
        pattern {
            protocol ([ 'rtsph', 'rtspm', 'rtspt', 'rtspu' ])
        }

        action {
            switch (protocol) {
                case 'rtsph':
                    set '-rtsp_protocol': 'http'
                    break
                case 'rtspm':
                    set '-rtsp_protocol': 'udp_multicast'
                    break
                case 'rtspt':
                    set '-rtsp_protocol': 'tcp'
                    break
                case 'rtspu':
                    set '-rtsp_protocol': 'udp'
            }

            protocol = 'rtsp'
        }
    }
}

