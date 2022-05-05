import java.util.*
import kotlin.math.pow
import kotlin.math.sqrt

/**
CORRECTION
bien respecter les consignes 5/20
pouvoir faire bouger les drones 10/20
travailler sur la logique 10-20/20

TODO
- [FEATURE] ]une fois un point controllé, aller sur l'autre + proche en laissant un drone dessus

- [FEATURE] rechercher les drones les plus proche d'un de nos drones, puis calculer sa zone la plus proche, puis comparer cette distance par rapport au notre
- si distance < autre drone -> on y va
- sinon on va aider un pote

- [REFACTOR] calculer les distances en tours
 */


/**********************
 *       MAIN
 *********************/
fun main(args : Array<String>) {

    // Logger (oui j'en ai eu marre des lignes de debug dans tous les sens)
    Logger.debug = true
    Logger.engine = true
    Logger.event = false
    Logger.distance = false
    Logger.radius = false
    Logger.state = false

    // INITIALIZATION
    GameEngine.init()
    GameEngine.initZones()
    GameEngine.initPlayers()

    // GAME LOOP
    GameEngine.play()
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

    // engine
    var turns = 0

    // constantes
    val zones = mutableListOf<Zone>()
    val players = mutableListOf<Player>()
    var personnalPlayer: Player? = null

    // variables reset à chaque turn
    var zonesControlled = mutableListOf<Zone>()
    var freeZones = mutableListOf<Zone>()

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

    fun getAllDrones(): MutableList<Drone> {
        val drones = mutableListOf<Drone>()
        players.forEach { player ->
            player.drones.forEach { drone ->
                drones.add(drone)
            }
        }
        return drones
    }

    fun getDrones(): MutableList<Drone> {
        return personnalPlayer?.drones ?: mutableListOf<Drone>()
    }

    /** Initialization */
    // init
    fun init() {
        numberOfPlayers = input.nextInt()
        personnalId = input.nextInt()
        numberOfDrones = input.nextInt()
        numberOfZones = input.nextInt()
    }

    fun initZones() {
        for (zoneId in 0 until numberOfZones) {
            val X = input.nextInt()
            val Y = input.nextInt()
            addZone(ZoneFactory.createZone(zoneId, PointFactory.createPoint(X, Y)))
        }
    }

    fun initPlayers() {
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
    fun updateZonesControl() {
        zones.forEach { zone ->
            val playerId = input.nextInt()
            zone.controlledBy = getPlayerById(playerId)
        }
    }

    fun updateDronesPosition() {
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
     * PLAY()
     */
    fun play() {
        while (true) {
            Logger.engine("Tour $turns")

            updateZonesControl()
            updateDronesPosition()

            if (turns > 1) {
                calculateTargets()
            }


            players.forEach { player ->
                addTurn(player)
            }

        }
    }

    // engine
    fun addTurn(player: Player) {

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
        getAllDrones().forEach { it.calculateTarget() }
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

    fun getCenterOfZones() : Point {
        var x = 0
        var y = 0
        zones.forEach { zone ->
            x += zone.center.x
            y += zone.center.y
        }
        val center = PointFactory.createPoint(x.div(zones.count()), y.div(zones.count()))
        return center
    }

    fun filterClosestZoneIfIsUnderControl(closestZones: MutableMap<Double, Zone>): Map<Double, Zone> {
        return closestZones.filter { (distance, zone) -> !zone.isUnderControl() }
    }

    // general utils
    fun setFreeZone(zone: Zone) {
        if (zone.isFree() || zone.hasNoEnemy()) this.freeZones.add(zone)
    }

    fun isPersonnalPlayer(player: Player): Boolean {
        return player == personnalPlayer
    }




}


/**********************
 *       ZONE
 *********************/
class Zone(
    val id: Int,
    val center: Point,
    var enemyDrones: MutableList<Drone> = mutableListOf<Drone>(),
    var personnalDrones: MutableList<Drone> = mutableListOf<Drone>()) {
    var controlledBy : Player? = null

    /**
     * Calcule le nombre de drone du joueur qui a la zone
     *
     */
    fun setDronesInRadius() : Int {
        Logger.radius("Drones in radius of $this")

        val dronesByPlayer = getDronesInRadius()

        /**
         * compte le nombre de drones ennemies dans la zone
         * TODO gestion multijoueurs
         */
        dronesByPlayer.forEach { player ->
            if (player.value.isNotEmpty()) {

                player.value.forEach { drone ->
                    Logger.radius("\t${player.key} $drone")

                    if (player.key == GameEngine.personnalPlayer) {
                        personnalDrones.add(drone)
                        drone.zone = this
                    } else {
                        if (player.key == controlledBy) {
                            enemyDrones.add(drone)
                        }
                    }

                }
            }
        }
        return enemyDrones.count()
    }

    fun getDronesInRadius() : HashMap<Player, MutableList<Drone>> {

        val dronesByPlayer = hashMapOf<Player, MutableList<Drone>>()
        GameEngine.players.forEach { player ->
            dronesByPlayer[player] = mutableListOf<Drone>()

            player.drones.forEach { drone ->
                if (isDroneInRadius(drone)) {
                    dronesByPlayer[player]?.add(drone)
                }
            }
        }
        return dronesByPlayer
    }

    fun isFree() : Boolean {
        return controlledBy == null
    }

    fun hasNoEnemy() : Boolean {
        return enemyDrones.isEmpty()
    }

    fun isUnderControl() : Boolean {
        return controlledBy == GameEngine.getPlayerById(GameEngine.personnalId)
    }

    /**
     * @param drone
     * @return true si le drone est dans la zone
     */
    fun isDroneInRadius(drone: Drone) : Boolean {
        val h = GameEngine.getDistance(this.center, drone.position)
        return h < 100
    }

    override fun toString() : String {
        return "Zone $id [captured by TODO with $enemyDrones drones, $center]"
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
    val id: Int,
    var position: Point,
    var lastPosition: Point
    ) : Listener() {
    var state: DroneState = DroneStateIdle(this)
    var closestZones = mutableMapOf<Double, Zone>()
    var closestFreeZone: Zone? = null
    var zone: Zone? = null
    var target: Zone? = null

    fun calculateTarget() {
        val candidates = mutableListOf<Target>()

        GameEngine.zones.forEach { zone ->
            if (GameEngine.getDistance(zone.center, position) < GameEngine.getDistance(zone.center, lastPosition)) {
                candidates.add(Target(zone, GameEngine.getDistance(lastPosition, position)))
            }
        }

        candidates.sortByDescending { it.distance }

        if (candidates.isEmpty()) {
            target = null
        } else {

            target = candidates.first().zone
            Logger.log("1st target for Drone ${this.id} is Zone ${target!!.id}")

//            if (candidates.count() == 1) {
//                target = candidates.first().zone
//                Logger.log("only 1 target of $this = $target")
//            } else {
//                Logger.log(candidates)
//            }
        }

    }

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

    fun move() : String {
        return GameEngine.getCenterOfZones().toString()
//        return this.state.move()
    }

    /**
     * TODO Refactor cette daube et réflechir à un mecanisme plus pratique
     * Calcule les zones les plus proches
     */
    fun calculateClosestZones() {
        val zonesToSort  = mutableMapOf<Double, Zone>()
        val sortedZones = TreeMap<Double, Zone>()

        GameEngine.zones.forEach { zone ->
            val distance = GameEngine.getDistance(zone.center, this.position)
            zonesToSort[distance] = zone
        }

        // auto sort by distance
        sortedZones.putAll(zonesToSort)
        closestZones = sortedZones
        Logger.distance("Distances for $this")

        closestZones.forEach {
            Logger.distance("\tDistance ${it.key}")
        }

        closestFreeZone = closestZones.values.first()
    }

//    /**
//     * Calcule la zone la plus proche d'un drone
//     * @Deprecated
//     */
//    fun calculateClosestZone(zones: List<Zone>) {
//        var closestDistance: Double = 0.0
//        zones.forEach {
//            if (GameEngine.getDistance(it.center, this.position) < closestDistance || closestZone == null) {
//                closestDistance = GameEngine.getDistance(it.center, this.position)
//                this.closestZones[0] = it
//            }
//        }
//    }

    /**
     * Calcule le radius d'une zone
     * @return true - Si le drone est dans le zone
     * @return false - Sinon
     */
    fun isInRadius(zone: Zone) : Boolean {
        val h = GameEngine.getDistance(zone.center, this.position)
        return h < 100
    }

    fun isRedoundant(): Boolean {
        GameEngine.players.forEach { player ->
            if (player == GameEngine.personnalPlayer) {
                player.drones.forEach { drone ->
                    return drone.zone == this.zone
                }
            }
        }
        return false
    }

    fun changeState(state: DroneState) {
        this.state = state
    }

    override fun onZoneCapture() {
        handleChanges()
    }

    fun handleChanges() {


        // Si on a plus de la moitié des zones, DEFENSE
        if (GameEngine.zonesControlled.count() > GameEngine.numberOfZones / 2) {
            changeState(DroneStateDefending(this))
            return
        }

        // on ne garde qu'un drone par point
        // les drones redondant vont vers les autres zones
        if (isRedoundant()) {
            changeState(DroneStateRunThrewZones(this))
            return
        }

        // Si il y a des zones libres
        if (GameEngine.freeZones.isNotEmpty()) {

            GameEngine.freeZones.forEach { zone ->
                // si on a 1 drone de + que les adversaires
                // OU si on n'a pas la zone
                if (zone.personnalDrones.count() > zone.enemyDrones.count() ||
                    !zone.isUnderControl()) {
                    Logger.state("CHANGING STATE TO GO TO FREE ZONE")
                    changeState(DroneStateScoutFreeZone(this))
                    return
                }
            }

        }

        changeState(DroneStateMoving(this))

        // si plusieurs drone sur 1 zone, déplacer 1

        // si pas de drone ennemie pres de la zone, aller à la 2eme zone plus proche

        // aller sur la zone qui vient de se faire capturer

    }

    override fun toString() : String {
        return "Drone $id $position"
    }
}

class Target(val zone: Zone, val distance: Double) {
    override fun toString(): String {
        return "Target $zone à distance $distance"
    }
}

abstract class Listener() {
    abstract fun onZoneCapture()
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
        Logger.state("CHANGING STATE TO IDLE")

        return GameEngine.getCenterOfZones().toString()
//        return drone.position.toString()
    }
}

class DroneStateMoving(val drone: Drone) : DroneState {
    override fun move() : String {
        Logger.state("CHANGING STATE TO MOVING")

        if (drone.closestZones.values.first().isUnderControl()) {
            return drone.closestZones.remove(drone.closestZones.keys.first())?.center.toString()
        }

        return drone.closestZones.values.first().center.toString()
    }
}

class DroneStateScoutFreeZone(val drone: Drone) : DroneState {
    override fun move() : String {
        Logger.state("CHANGING STATE TO SCOUT FREE ZONE")

        if (drone.closestFreeZone !== null) {
            return drone.closestFreeZone!!.center.toString()
        } else {
            return GameEngine.getCenterOfZones().toString()
        }

    }
}

class DroneStateRunThrewZones(val drone: Drone) : DroneState {
    override fun move() : String {
        Logger.state("CHANGING STATE TO RUN THREW ZONE")

        val filteredZones = GameEngine.filterClosestZoneIfIsUnderControl(drone.closestZones)

        if (filteredZones.isEmpty()) {
            return GameEngine.getCenterOfZones().toString()
        }

        return filteredZones.values.first().center.toString()

    }
}

class DroneStateDefending(val drone: Drone) : DroneState {
    override fun move() : String {
        Logger.state("CHANGING STATE TO DEFENSE")

        if (drone.zone !== null) {
            if (drone.isInRadius(drone.zone!!)) {
                return GameEngine.filterClosestZoneIfIsUnderControl(drone.closestZones)
                    .values
                    .first()
                    .center
                    .toString()
            }
        }


        return drone.position.toString()
    }
}

/**
 * si debug = true, affiche les log(). Et tous les autres log qui sont à true.
 */
object Logger {
    var debug: Boolean = false
    var engine: Boolean = false
    var event: Boolean = false
    var distance: Boolean = false
    var radius: Boolean = false
    var state: Boolean = false

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
}