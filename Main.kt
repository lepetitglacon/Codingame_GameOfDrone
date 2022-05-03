import java.util.*
import java.io.*
import java.math.*
import kotlin.math.pow
import kotlin.math.sqrt

/**
CORRECTION
bien respecter les consignes 5/20
pouvoir faire bouger les drones 10/20
travailler sur la logique 10-20/20

TODO

- une fois un point controllé, allé sur l'autre + proche en laissant un drone dessus

 */

/**
 * Auto-generated code below aims at helping you parse
 * the standard input according to the problem statement.
 **/
fun main(args : Array<String>) {

    // CondinGame init
    val input = Scanner(System.`in`)
    val numberOfPlayers = input.nextInt()
    val personnalId = input.nextInt()
    val numberOfDrones = input.nextInt()
    val numberOfZones = input.nextInt()

    val zoneFactory = ZoneFactory()
    val playerFactory = PlayerFactory()
    val droneFactory = DroneFactory()

    // zones init
    // A zone is a circle with a radius of 100 units.
    for (zoneId in 0 until numberOfZones) {
        val X = input.nextInt()
        val Y = input.nextInt()
        GameEngine.addZone(zoneFactory.createZone(zoneId, X, Y, ZoneStateFree()))
    }

    // players init
    for (i in 0 until numberOfPlayers) {
        var player = playerFactory.createPlayer(i)
        GameEngine.addPlayer(player)

        // drones init
        for (i in 0 until numberOfDrones) {
            var drone = droneFactory.createDrone(i,Point(0,0))
            player.drones.add(drone)
        }
    }



    // game loop
    while (GameEngine.turns < 50) {

        // get controlled zones
        for (zoneId in 0 until numberOfZones) {
            val playerId = input.nextInt() // ID of the team controlling the zone (0, 1, 2, or 3) or -1 if it is not controlled. The zones are given in the same order as in the initialization.
            val zone = GameEngine.getZone(zoneId)
            zone.state = if (playerId != -1 && playerId == personnalId) ZoneStateCaptured() else ZoneStateFree()
        }

        for (playerId in 0 until numberOfPlayers) {
            val player = GameEngine.getPlayer(playerId)

            for (droneId in 0 until numberOfDrones) {
                val drone = player.getDrone(droneId)

                drone.setX(input.nextInt())
                drone.setY(input.nextInt())
            }
        }

        for (droneId in 0 until numberOfDrones) {

            var player = GameEngine.getPlayer(personnalId)

            //System.err.println("droneId = ${droneId}")

            var drone = player.getDrone(droneId)
            //drone.calculateClosestZone(GameEngine.zones)
            println(drone.state.move())


            // Write an action using println()
            // To debug: System.err.println("Debug messages...");


            // output a destination point to be reached by one of your drones. The first line corresponds to the first of your drones that you were provided as input, the next to the second, etc.

        }
        GameEngine.addTurn()
    }
}

object GameEngine {
    var turns = 0
    val zones = mutableListOf<Zone>()
    val players = mutableListOf<Player>()

    fun addZone(zone: Zone) {
        zones.add(zone)
    }

    fun getZone(id: Int): Zone {
        return zones.first { it.id == id }
    }

    fun addPlayer(player: Player) {
        players.add(player)
    }

    fun getPlayer(id: Int): Player {
        return players.first { it.id == id }
    }

    fun getDistance(a: Point, b: Point): Double {
        val x = (b.x.toDouble() - a.x.toDouble()).pow(2.0)
        val y = (b.y.toDouble() - a.y.toDouble()).pow(2.0)
        val distance = sqrt(x+y)
        return distance
    }

    fun addTurn() {
        turns++
    }
}

class Zone(val id: Int, val center: Point, var state: ZoneState) {
    var maxEnemyIn: Int = 0

    override fun toString() : String {
        return "Zone $id : captured by TODO with $maxEnemyIn drones"
    }
}

class ZoneFactory {
    fun createZone(id: Int, x: Int = 0, y: Int = 0, state: ZoneState): Zone {
        val pf = PointFactory()
        return Zone(id, pf.createPoint(x,y), state)
    }
}

interface ZoneState {
    fun onEnterState() {}
}

class ZoneStateFree : ZoneState {}
class ZoneStateCaptured : ZoneState {}

class Player(val id: Int, val drones: MutableList<Drone>, val zones: MutableList<Zone>) {
    fun getDrone(id: Int) : Drone {
        return drones.first { it.id == id }
    }
}

class PlayerFactory {
    fun createPlayer(id: Int): Player {
        return Player(id, mutableListOf<Drone>(), mutableListOf<Zone>())
    }
}

class Point(var x: Int, var y: Int) {
    override fun toString() : String {
        return "${x} ${y}"
    }
}

class PointFactory{
    fun createPoint(x: Int = 0, y: Int = 0) : Point {
        return Point(x, y)
    }
}

class Drone(val id: Int, var position: Point) {
    var state: DroneState = DroneStateIdle(this)
    var closestZone: Zone? = null

    fun setX(x: Int) {
        position.x = x
    }

    fun setY(y: Int) {
        position.y = y
        state.move()
    }

    fun calculateClosestZone(zones: List<Zone>) {
        var closestDistance: Double = 0.0
        System.err.println("--")
        System.err.println("drone ${this.id} x: ${position.x}, y x: ${position.y}")
        zones.forEach {
            System.err.println(it)
            System.err.println("distance from zone = ${GameEngine.getDistance(it.center, this.position)}")
            if (GameEngine.getDistance(it.center, this.position) < closestDistance || closestZone == null) {
                closestDistance = GameEngine.getDistance(it.center, this.position)
                this.closestZone = it
            }
            System.err.println("closestDistance = $closestDistance")
        }
        System.err.println("chosen zone = $closestZone")
    }

    fun calculateClosestZoneExeptClosestOne() {
        val zones: MutableList<Zone> = GameEngine.zones
        val filtered = zones.filter { it !== closestZone }
        return calculateClosestZone(filtered)
    }

    fun changeState(state: DroneState) {
        this.state = state
        this.state.onEnterState()
    }
}

class DroneFactory {
    fun createDrone(id: Int, position: Point): Drone {
        return Drone(id,position)
    }
}

interface DroneState {
    fun onEnterState()

    fun move() : String
}
class DroneStateCapturing(val drone: Drone) : DroneState {
    override fun onEnterState() {
        System.err.println("Capturing mode")
    }

    override fun move() : String {

        if (drone.closestZone?.state is ZoneStateCaptured && drone.closestZone?.maxEnemyIn!!.equals(0)) {
            drone.changeState(DroneStateMoving(drone))
        }


        return drone.closestZone?.center.toString()
    }
}
class DroneStateMoving(val drone: Drone) : DroneState {
    override fun onEnterState() {
        System.err.println("Moving mode")

        if (drone.closestZone == null) {
            drone.calculateClosestZone(GameEngine.zones)
        } else {
            drone.calculateClosestZoneExeptClosestOne()
        }

    }

    override fun move() : String {

        if (drone.position.x.equals(drone.closestZone?.center?.x)) {
            drone.changeState(DroneStateCapturing(drone))
        }

        return drone.closestZone?.center.toString()
    }
}

class DroneStateIdle(val drone: Drone) : DroneState {
    override fun onEnterState() {
        System.err.println("Idle mode")
    }

    override fun move() : String {

        if (drone.position.y != 0) {
            drone.changeState(DroneStateMoving(drone))
        }

        return "4000 1800"
    }
}