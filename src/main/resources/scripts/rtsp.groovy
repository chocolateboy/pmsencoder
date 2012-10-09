script {
    profile ('RTSP') {
        pattern {
            protocol ([ 'rtsph', 'rtspm', 'rtspt', 'rtspu' ])
        }

        action {
            switch (protocol) {
                case 'rtsph':
                    set '-rtsp_transport': 'http'
                    break
                case 'rtspm':
                    set '-rtsp_transport': 'udp_multicast'
                    break
                case 'rtspt':
                    set '-rtsp_transport': 'tcp'
                    break
                case 'rtspu':
                    set '-rtsp_transport': 'udp'
            }

            protocol = 'rtsp'
        }
    }
}

