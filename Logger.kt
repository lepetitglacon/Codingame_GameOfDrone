object Logger {
    var debug: Boolean = true
    var error: Boolean = true
    var engine: Boolean = true

    var event: Boolean = false
    var distance: Boolean = false
    var radius: Boolean = false
    var state: Boolean = true
    var target: Boolean = false
    var zoneControl: Boolean = false

    fun logging(string: Any) {
        if (debug) {
            System.err.println(string)
        }
    }

    fun log(string: Any) {
        logging(string)
    }

    fun engine(string: Any) {
        if (engine) logging(string)
    }

    fun event(string: Any) {
        if (event) logging(string)
    }

    fun distance(string: Any) {
        if (distance) logging(string)
    }

    fun radius(string: Any) {
        if (radius) logging(string)
    }

    fun state(string: Any) {
        if (state) logging(string)
    }

    fun error(string: Any) {
        if (error) logging("[ERROR] $string")
    }

    fun target(string: Any) {
        if (target) logging("$string")
    }

    fun zoneControl(string: Any) {
        if (zoneControl) logging("$string")
    }
}