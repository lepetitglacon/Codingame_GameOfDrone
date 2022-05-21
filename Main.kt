import java.util.*
import kotlin.math.pow
import kotlin.math.sqrt

/**
CORRECTION
bien respecter les consignes 5/20
pouvoir faire bouger les drones 10/20
travailler sur la logique 10-20/20
 */


/**********************
 *       MAIN
 *********************/
fun main(args : Array<String>) {

    // INITIALIZATION
    GameEngine.init(GameStrategie.COUNTERSTRIKE)

    // GAME LOOP
    GameEngine.play()
}

enum class GameStrategie {
    COUNTERSTRIKE,
    DEFENDANDCONQUER,
    CLOSESTZONE
}

/**********************
 *       GAME ENGINE
 *********************/
object GameEngine {

    /** VARS */
    // init
    var input = Scanner(System.`in`)
    var numberOfPlayers = 0
    var personnalId = 0
    var numberOfDrones = 0
    var numberOfZones = 0
    var gameStrategie = GameStrategie.COUNTERSTRIKE

    // engine
    var turns = 0

    // constantes
    val zones = mutableListOf<Zone>()
    val zonesToFocus = mutableListOf<Zone>()
    val players = mutableListOf<Player>()
    var personnalPlayer: Player? = null

    /** Initialization */
    // init
    fun init(gameStrategie: GameStrategie) {
        numberOfPlayers = input.nextInt()
        personnalId = input.nextInt()
        numberOfDrones = input.nextInt()
        numberOfZones = input.nextInt()
        this.gameStrategie = gameStrategie

        initZones()
        initPlayersAndDrones()
    }

    private fun initZones() {
        // zones init
        for (zoneId in 0 until numberOfZones) {
            val X = input.nextInt()
            val Y = input.nextInt()
            addZone(ZoneFactory.createZone(zoneId, PointFactory.createPoint(X, Y)))
        }

        // zones to focus init

        val zonesToFocus = initZonesToFocus()
    }

    /**
     * stock dans zonesToFocus les zones les plus proches entre elles
     */
    private fun initZonesToFocus() {
        val distances = mutableListOf<ZoneTarget>()
        zones.forEach { zone -> distances.add(ZoneTarget(zone, zone.calculateDistanceWithOtherZones())) }
        distances.sortBy { it.distance }
        distances.forEach { zoneTarget -> zonesToFocus.add(zoneTarget.zone) }
    }

    private fun initPlayersAndDrones() {
        for (i in 0 until numberOfPlayers) {
            val player = PlayerFactory.createPlayer(i)
            addPlayer(player)

            // DRONES INIT
            for (j in 0 until numberOfDrones) {
                val drone = DroneFactory.createDrone(j,PointFactory.createPoint(0, 0))
                player.drones.add(drone)
            }
        }

        // set personnal player
        personnalPlayer = players.first { it.id == personnalId }
    }

    /** Update objects with CodeinGame input */
    // update
    private fun updateZonesControl() {
        zones.forEach { zone ->
            val playerId = input.nextInt()
            zone.controlledBy = getPlayerById(playerId)
        }
    }

    private fun updateDronesPosition() {
        players.forEach { player ->
            player.drones.forEach { drone ->
                // set last position
                drone.setLastPositionX(drone.position.x)
                drone.setLastPositionY(drone.position.y)
                // set new position
                drone.setX(input.nextInt())
                drone.setY(input.nextInt())
            }
        }
    }

    /**
     * main GameEngine function
     */
    fun play() {

        while (true) {
            Logger.engine("Tour $turns")

            // input from coding game
            updateZonesControl()
            updateDronesPosition()

            // init
            if (turns == 0) { // tour 0
                calculateClosestZones()
//                closestDroneFromZoneGoesToClosestZone()

            } else {

                // differentes fonction selon la strategies
                when (gameStrategie) {

                    GameStrategie.COUNTERSTRIKE -> {
                        calculateTargets()
                        counterTargets()
                    }

                    GameStrategie.DEFENDANDCONQUER -> {

                    }

                    GameStrategie.CLOSESTZONE -> {
                        goToClosestZones()
                    }

                    else -> {
                        Logger.error("Wrong strategy")
                    }
                }
            }

            players.forEach { player ->
                addTurn(player)
            }

        }
    }

    // engine
    private fun addTurn(player: Player) {

        if (isPersonnalPlayer(player)) {
            player.drones.forEach { drone ->
                println(drone.move())
            }
        }

        turns++
    }

    /** Update objects/state/objectives */

    // calculation
    fun calculateTargets() {
        zones.forEach { zone -> zone.setDronesInRadius() }
        getAllDrones().forEach { it.calculateTarget() }
    }

    fun counterTargets() {

        zones.forEach { zone ->
            Logger.log(zone)
            Logger.log("\tdrones ${zone.dronesInRadius}")
            Logger.log("\tdrones alliés : ${zone.getAlliedDronesInRadius()}")
            Logger.log("\tdrones enemies : ${zone.getEnemyDronesInRadius()}")
        }

        getDrones().forEach { drone -> drone.goToZone(zones.first()) }

//        zones.forEach { zone ->
//
//            if (zone.isUnderControl()) {
//
//                // si on a plus de drone que les enemies
//                if (zone.personnalDrones.count() > zone.enemyDrones.count()) {
//                    zone.closestDrone().changeState(DroneStateClosestZone(zone.closestDrone()))
//                    return
//                }
//                // sinon on defend
//                else {
//                    if (zone.personnalDrones.count() == zone.enemyDrones.count()) {
//                        zone.closestDrone().changeState(DroneStateDefendZone(zone.closestDrone()))
//                        return
//                    } else {
//                        zone.personnalDrones.forEach { drone ->
//                            drone.calculateClosestZones(zone)
//                            drone.changeState(DroneStateClosestZone(drone))
//                            return
//                        }
//                    }
//                }
//
//            } else {
//                if (zone.isFree() && !zone.isTargeted) {
//
//                    zone.closestDrone().goToZone(zone)
//                    return
//                }
//            }
//        }

    }

    fun calculateClosestZones() {
        getAllDrones().forEach { drone ->
            drone.calculateClosestZones()
        }
    }

    /**
     *
     */
    fun goToClosestZones() {

        getDrones().forEach { drone ->
            Logger.log("$drone\r")
            Logger.log("\tzone p proche = ${drone.closestZones}\r")

            if (drone.state !is DroneStateClosestZone) {
                drone.changeState(DroneStateClosestZone(drone))
            }

            if (drone.isInRadius(drone.closestZones.first())) {
                drone.calculateClosestZones(drone.closestZones.first())
            }
        }
    }

    fun closestDroneFromZoneGoesToClosestZone() {
        zones.forEach { zone ->
            zone.closestDrone().goToZone(zone)
        }
    }

    // geometry utils
    /**
     * @return Double : distance entre 2 points
     */
    fun getDistance(a: Point, b: Point): Double {
        val x = (b.x.toDouble() - a.x.toDouble()).pow(2.0)
        val y = (b.y.toDouble() - a.y.toDouble()).pow(2.0)
        val distance = sqrt(x+y)
        Logger.distance(distance)
        return distance
    }

    /**
     * @return Point : point médian entre toutes les zones
     */
    fun getCenterOfZones(): Point {
        var x = 0
        var y = 0
        zones.forEach { zone ->
            x += zone.center.x
            y += zone.center.y
        }
        return PointFactory.createPoint(x.div(zones.count()), y.div(zones.count()))
    }

    // general utils
    /**
     * @return Boolean : est-ce que c'est notre joueur ?
     */
    fun isPersonnalPlayer(player: Player): Boolean {
        return player == personnalPlayer
    }

    /** Getters/Setters */
    // customs setters/getters
    fun addZone(zone: Zone) {
        zones.add(zone)
    }
    fun getZoneById(id: Int): Zone {
        return zones.first { it.id == id }
    }

    fun addPlayer(player: Player) {
        players.add(player)
    }
    fun getPlayerById(id: Int): Player? {
        return players.firstOrNull { it.id == id }
    }
    fun getPlayerByDrone(drone: Drone): Player {
        players.forEach { player ->
            if (player.drones.contains(drone)) {
                return player
            }
        }
        return personnalPlayer!!
    }

    /**
     * @return tous les drones
     */
    fun getAllDrones(): MutableList<Drone> {
        val drones = mutableListOf<Drone>()
        players.forEach { player ->
            player.drones.forEach { drone ->
                drones.add(drone)
            }
        }
        return drones
    }

    /**
     * @return tous les drones enemies
     */
    fun getEnemyDrones(): MutableList<Drone> {
        val drones = mutableListOf<Drone>()
        players
            .filter { it !== personnalPlayer }
            .forEach { player ->
                player.drones.forEach { drone ->
                    drones.add(drone)
                }
            }
        return drones
    }

    /**
     * @return les drones de notre joueur
     */
    fun getDrones(): MutableList<Drone> {
        return personnalPlayer?.drones ?: mutableListOf<Drone>()
    }

}


/**********************
 *       ZONE
 *********************/
class Zone(val id: Int, val center: Point) {
    var controlledBy: Player? = null

    var dronesInRadius: MutableList<Drone> = mutableListOf<Drone>()
    var targets: MutableList<Drone> = mutableListOf<Drone>()

    fun setDronesInRadius() {
        Logger.radius("Drones in radius of $this")

        GameEngine.getAllDrones().forEach { drone ->
            if (isDroneInRadius(drone)) {
                if (!dronesInRadius.contains(drone)) {
                    dronesInRadius.add(drone)
                }
            } else {
                dronesInRadius.remove(drone)
            }
        }
    }

    fun getAlliedDronesInRadius(): MutableList<Drone> {
        return dronesInRadius.filter { drone -> drone.isPersonnal() }.toMutableList()
    }

    fun getEnemyDronesInRadius(): MutableList<Drone> {
        return dronesInRadius.filter { drone -> !drone.isPersonnal() }.toMutableList()
    }

    /**
     * @param drone
     * @return true si le drone est dans la zone
     */
    fun isDroneInRadius(drone: Drone) : Boolean {
        val h = GameEngine.getDistance(this.center, drone.position)
        return h < 100
    }

    fun closestDrone(): Drone {
        var closestDrones = mutableListOf<DroneTarget>()

        GameEngine.getDrones().forEach { drone ->
            closestDrones.add(DroneTarget(drone, GameEngine.getDistance(this.center, drone.position)))
        }

        closestDrones.sortBy { it.distance }

        return closestDrones.first().drone
    }

    fun calculateDistanceWithOtherZones(): Double {
        var distance = 0.0
        GameEngine.zones.forEach { zone ->
            distance += GameEngine.getDistance(this.center, zone.center)
        }
        return distance
    }

    fun toStringAll() : String {
        return "Zone $id [controlled by $controlledBy, center's at $center]"
    }

    override fun toString() : String {
        return "Zone $id"
    }
}

object ZoneFactory {
    fun createZone(id: Int, point: Point): Zone {
        return Zone(id, point)
    }
}

/**********************
 *       PLAYER
 *********************/
class Player(
    val id: Int,
    val drones: MutableList<Drone>,
    val points: Int = 0
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

/**********************
 *       POINT
 *********************/
class Point(var x: Int, var y: Int) {
    override fun toString() : String {
        return "${x} ${y}"
    }
}

object PointFactory{
    fun createPoint(x: Int = 0, y: Int = 0) : Point {
        return Point(x, y)
    }
}

/**********************
 *       DRONE
 *********************/
class Drone(

    /** Vars */
    val id: Int,
    var position: Point,
    var lastPosition: Point
    ) : ZoneObserver() {
    var state: DroneState = DroneStateIdle(this)
    var closestZones = mutableListOf<Zone>()
    var closestFreeZone: Zone? = null
    var zone: Zone? = null
    var target: Zone? = null

    /** Getter/Setters */
    fun setX(x: Int) {
        position.x = x
    }
    fun setY(y: Int) {
        position.y = y
    }
    fun setLastPositionX(x: Int) {
        lastPosition.x = x
    }
    fun setLastPositionY(y: Int) {
        lastPosition.y = y
    }

    /** Utils */
    fun calculateTarget(filter: Zone? = null) {
        val candidates = mutableListOf<ZoneTarget>()
        val zonesToTarget: MutableList<Zone>

        if (filter !== null) {
            zonesToTarget = GameEngine.zones.filterNot { it == filter }.toMutableList()
        } else {
            zonesToTarget = GameEngine.zones
        }

        zonesToTarget.forEach { zone ->
            if (GameEngine.getDistance(zone.center, position) < GameEngine.getDistance(zone.center, lastPosition)) {
                candidates.add(ZoneTarget(zone, GameEngine.getDistance(zone.center, position)))
            }
        }

        if (candidates.isNotEmpty()) {

            candidates.sortBy { it.distance }

            target = candidates.first().zone
            candidates.first().zone.targets.add(this)
        } else {
            target = null
        }
    }

    fun goToZone(zone: Zone) {
        changeState(DroneStateGoTo(this, zone))
    }

    /**
     * Calcule les zones les plus proches
     */
    fun calculateClosestZones(filter: Zone? = null) {
        val zonesToCompare: MutableList<Zone>
        val zonesTargets = mutableListOf<ZoneTarget>()

        // vider closestZones
        closestZones.removeAll { true }

        // filtrer une zone
        if (filter !== null) {
            zonesToCompare = GameEngine.zones.filterNot { it == filter }.toMutableList()
        } else {
            zonesToCompare = GameEngine.zones
        }

        // calcule distance
        zonesToCompare.forEach { zone ->
            zonesTargets.add(ZoneTarget(zone, GameEngine.getDistance(zone.center, this.position)))
        }

        // trier par distance
        zonesTargets.sortBy { it.distance }

        Logger.log("zones du $this")
        zonesTargets.forEach { Logger.log("\t$it") }

        // renvoyer les zones
        zonesTargets.forEach {
            closestZones.add(it.zone)
        }
    }

    /**
     * Calcule le radius d'une zone
     * @return true - Si le drone est dans le zone
     * @return false - Si non
     */
    fun isInRadius(zone: Zone) : Boolean {
        val h = GameEngine.getDistance(zone.center, this.position)
        return h < 100
    }

    fun isPersonnal(): Boolean {
        return GameEngine.getPlayerByDrone(this) == GameEngine.personnalPlayer
    }

    /**
     * Change le state du drone
     */
    fun changeState(state: DroneState) {

        if (!this.state.javaClass.isInstance(state)) {
            this.state = state
            Logger.state("$this changing state to ${state.javaClass.name}")
        } else {
            Logger.state("$this allready in state ${state.javaClass.name}")
        }

    }

    /**
     * @return le point vers lequel le drone doit se diriger
     */
    fun move() : String {
        return this.state.move()
    }

    /**
     * Return toutes les infos du drones
     */
    fun toStringAll() : String {
        return "Drone $id from ${GameEngine.getPlayerByDrone(this)} is at position $position"
    }

    override fun toString() : String {
        return "Drone $id"
    }

    // Override ZoneObserver
    override fun onZoneCapture() {

    }

    override fun onZoneEnter() {
        TODO("Not yet implemented")
    }

    override fun onZoneLeave() {
        TODO("Not yet implemented")
    }
}


class ZoneTarget(val zone: Zone, val distance: Double) {
    override fun toString(): String {
        return "Target $zone à distance $distance"
    }
}

class DroneTarget(val drone: Drone, val distance: Double) {
    override fun toString(): String {
        return "Target $drone à distance $distance"
    }
}

abstract class ZoneObserver() {
    abstract fun onZoneCapture()
    abstract fun onZoneEnter()
    abstract fun onZoneLeave()
}

object DroneFactory {
    fun createDrone(id: Int, position: Point): Drone {
        return Drone(id, position, Point(0, 0))
    }
}

interface DroneState {
    fun move() : String
}

class DroneStateIdle(val drone: Drone) : DroneState {
    override fun move() : String {
        Logger.state("$drone is going standby (center of zones)")

        return GameEngine.getCenterOfZones().toString()
    }
}

class DroneStateDefendZone(val drone: Drone) : DroneState {
    override fun move() : String {
        Logger.state("$drone stays at position")

        return drone.position.toString()
    }
}

class DroneStateClosestZone(val drone: Drone) : DroneState {
    override fun move() : String {
        Logger.state("$drone is going at closest zone ${drone.closestZones.first()}")

        return drone.closestZones.first().center.toString()
    }
}

class DroneStateClosestFreeZone(val drone: Drone) : DroneState {
    override fun move() : String {
        Logger.state("CHANGING STATE TO CLOSEST FREE ZONE")

        return GameEngine.getCenterOfZones().toString()
    }
}

class DroneStateGoTo(val drone: Drone, val zone: Zone) : DroneState {
    override fun move() : String {
        Logger.state("$drone is going strait to $zone")
        return zone.center.toString()
    }
}

/**
 * si debug = true, affiche les log(). Et tous les autres log qui sont à true.
 */
object Logger {
    var debug: Boolean = true
    var error: Boolean = true
    var engine: Boolean = true

    var event: Boolean = false
    var distance: Boolean = false
    var radius: Boolean = false
    var state: Boolean = true
    var target: Boolean = false

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
        if (error) logging("$string")
    }
}