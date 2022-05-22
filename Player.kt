class Player(
    var id: Int,
    val drones: MutableList<Drone>,
    var points: Int = 0
) {

    fun getDrone(id: Int) : Drone {
        return drones.first { it.id == id }
    }

    fun toStringALl(): String {
        var string = "Player $id\n\t"
        string += "Points : $points\n\t"
        drones.forEach { string += "$it\n" }

        return string
    }

    override fun toString(): String {
        return "Player $id"
    }
}

object PlayerFactory {
    fun createPlayer(id: Int): Player {
        return Player(id, mutableListOf<Drone>())
    }
}