config {
    def var1 = "config$nbcores"

    profile ('GString') {
        def var2 = "profile$nbcores"
        def var3

        pattern {
            var3 = "pattern$nbcores"
            match { 1 == 1 }
        }

        action {
            def var4 = "action$nbcores"
            args = [ var1, var2, var3, var4 ]
        }
    }
}
