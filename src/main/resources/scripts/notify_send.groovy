import com.chocolatey.pmsencoder.Plugin

script (BEGIN) {
    profile ('Notify Send', stopOnMatch: false, alwaysRun: true) {
        pattern {
            match { NOTIFY_SEND }
        }

        action {
            hook = [ NOTIFY_SEND, Plugin.name, dlna.name ]
        }
    }
}
