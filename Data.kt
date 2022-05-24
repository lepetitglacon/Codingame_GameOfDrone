/** Point Data */
data class Point(var x: Int = 0, var y: Int = 0) { override fun toString(): String = "${x} ${y}" }
object PointFactory {
    fun createPointWithCoordinates(x: Int = 0, y: Int = 0) : Point = Point(x, y)
    fun createPoint() : Point = Point()
}

/** Player Data */
data class Player(var id: Int, val drones: MutableList<Drone>, var points: Int = 0) {
    override fun toString(): String = "Player $id"
}
object PlayerFactory { fun createPlayer(id: Int): Player = Player(id, mutableListOf<Drone>()) }

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