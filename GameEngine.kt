import java.util.*
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sqrt
import kotlin.math.absoluteValue

/**
CORRECTION
bien respecter les consignes 5/20
pouvoir faire bouger les drones 10/20
travailler sur la logique 10-20/20
 */

fun main(args : Array<String>) {

    // INITIALIZATION
    GE.init(GameStrategie.FOCUSEDZONES)

    // GAME LOOP
    GE.play()
}

typealias GE = GameEngine

object GameEngine {
    // init
    var input = Scanner(System.`in`)
    var numberOfPlayers = 0
    var personnalId = 0
    var numberOfDrones = 0
    var numberOfZones = 0
    var gameStrategie = GameStrategie.COUNTERSTRIKE

    // engine
    var turns = 0
    val zonesToFocus = mutableListOf<Zone>()

    // constantes
    val ZONES = mutableListOf<Zone>()
    val PLAYERS = mutableListOf<Player>()
    var PERSONNALPLAYER: Player = PlayerFactory.createPlayer(999)

    /** init codingame variables and creates objects */
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
        for (zoneId in 0 until numberOfZones) {
            val X = input.nextInt()
            val Y = input.nextInt()
            ZONES.add(ZoneFactory.createZone(zoneId, PointFactory.createPointWithCoordinates(X, Y)))
        }
    }

    private fun initPlayersAndDrones() {
        for (playerId in 0 until numberOfPlayers) {
            val player: Player

            if (playerId == personnalId) {
                PERSONNALPLAYER.id = playerId
                player = PERSONNALPLAYER
            } else {
                player = PlayerFactory.createPlayer(playerId)
            }

            PLAYERS.add(player)

            // initialisation des drones
            for (droneId in 0 until numberOfDrones) {
                player.drones.add(DroneFactory.createDrone(droneId,PointFactory.createPoint()))
            }
        }
    }

    /** Update objects with CodeinGame input */
    private fun updateZonesControl() {
        ZONES.forEach { zone ->
            val playerId = input.nextInt()
            zone.controlledBy = getPlayerById(playerId)
        }
    }

    private fun updateDronesPosition() {
        PLAYERS.forEach { player ->
            player.drones.forEach { drone ->
                drone.lastPosition = drone.position
                drone.position.x = input.nextInt()
                drone.position.y = input.nextInt()
            }
        }
    }

    /** Engine */
    private fun isWinning(): Boolean = controlsMoreZones() && hasMorePoints()
    private fun controlsMoreZones(): Boolean = ZONES.filter { it.controlledBy == PERSONNALPLAYER }.count() >= numberOfZones.div(2)
    private fun hasMorePoints(): Boolean = PLAYERS.filter { it.points >= PERSONNALPLAYER!!.points }.first() == PERSONNALPLAYER
    fun isAWinningTrade(): Boolean = ZONES.filter { it.controlledBy == PERSONNALPLAYER }.count() + 1 >= (numberOfZones/2)

    /** Main loop */
    fun play() {

        while (true) {
            Logger.engine("Tour $turns")

            // input from coding game
            updateZonesControl()
            updateDronesPosition()

            if (turns == numberOfPlayers) {
                calculateZonesToFocus()
            }

            // tour 0
            if (isFirstTurn()) {
                calculateClosestZones()

            } else {
                when (gameStrategie) {
                    GameStrategie.COUNTERSTRIKE -> {
                        if (isWinning()) {
                            Logger.engine("WINNING\n")
                            calculateDefendingTargets()
                            counterTargets()
                        } else {
                            Logger.engine("LOOSING\n")
                            calculateTargets()
                            counterTargets()
                        }
                    }

                    GameStrategie.DEFENDANDCONQUER -> {
                    }

                    GameStrategie.CLOSESTZONE -> {
                    }

                    GameStrategie.FOCUSEDZONES -> {
                        goToFocusedZones()
                    }

                    else -> {
                        Logger.error("Wrong strategy")
                    }
                }
            }

            addPoints()
            showPoints()
            addTurns()
        }
    }

    private fun isFirstTurn() = turns == 0

    // engine
    private fun addTurns() = PLAYERS.forEach { makeMoves(it); addTurn() }
    private fun addTurn() = turns++
    private fun makeMoves(player: Player) {

        if (isPersonnalPlayer(player)) {
            player.drones.forEach { drone -> println(drone.move()) }
        }
    }
    private fun addPoints() {
        ZONES.forEach { zone ->
            zone.controlledBy?.points = zone.controlledBy?.points!! + 1
        }
    }
    private fun showPoints() {
        Logger.engine("POINTS\n")
        PLAYERS.forEach { player ->
            Logger.engine("\t$player has ${player.points} points")
        }
    }

    /** Update objects/state/objectives */

    /**
     * Calculate targets (zones) for every drones
     */
    fun calculateTargets() {
        ZONES.forEach { it.setDronesInRadius() }
        getAllDrones().forEach { it.calculateTarget() }
    }
    fun calculateDefendingTargets() = calculateTargets()

    /**
     * Counter enemy targets
     */
    fun counterTargets() {

        zonesToFocus.forEach { zone ->

            val inRadiusDelta = zone.getAlliedDronesInRadius().count() - zone.getEnemyDronesInRadius().count()
            val inCommingDroneDelta = zone.getAlliedDronesTargets().count() - zone.getEnemyDronesTargets().count()
            val globalDelta = inRadiusDelta + inCommingDroneDelta

            when (globalDelta.sign) {

                // On va controller la zone
                1 -> {
                    Logger.log("$zone va être gagnée")
                    if (zone.isUnderControl()) {
                        zone.redistributeDrones(zone.getAlliedDrones())
                    }
                }

                // La zone va etre neutre ou sous tensions
                0 -> {
                    if (zone.isNotFocused() && zone.isFree()) {
                        Logger.log("$zone est libre")
                        zone.callUnusedDrone()
                    } else {
                        Logger.log("$zone c'est le zbeul")
                        if (zone.closestDrone().isPersonnal()) {
                            Logger.log("$zone plus proche drone est ${zone.closestDrone()}")
                        } else {
                            Logger.log("$zone plus proche drone est ${zone.closestDrone()}")
                            Logger.log("$zone on redistribue")
                            zone.redistributeDrones(zone.getAlliedDrones())
                        }
                    }
                }

                // L'ennemi va controller la zone
                -1 -> {
                    Logger.log("$zone est perdue")
                    Logger.log("$zone on redistribue")
                    zone.redistributeDrones(zone.getAlliedDrones())
                }
            }

        }
    }

    fun goToFocusedZones() {
        calculateClosestZones()

        zonesToFocus.forEach {
            it.closestDrones.first().goToZone(it)
        }
    }

    /** Calcule les zones les plus proches de chaques drones */
    private fun calculateClosestZones() = getAllDrones().forEach { drone -> drone.calculateClosestZones() }

    /** Calcule les zones les + proches entre elles */
    private fun calculateZonesToFocus() {
        val zones = mutableListOf<ZoneTarget>()
        // pour x zones, on veut calculer la distance la plus proche pour ses x/2 plus proches zones
        ZONES.forEach { zone ->
            zones.add(ZoneTarget(zone, zone.calculateDistanceWithOtherZones()))
        }
        zones.sortBy { it.distance }

        for (i in 0 until numberOfZones.div(2)) {
            zonesToFocus.add(zones[i].zone)
        }

        zonesToFocus.forEach { Logger.log(it) }
    }

    /** Donne la distance entre 2 Points */
    fun getDistance(a: Point, b: Point): Double {
        val x = (b.x.toDouble() - a.x.toDouble()).pow(2.0)
        val y = (b.y.toDouble() - a.y.toDouble()).pow(2.0)
        val distance = sqrt(x+y)
        Logger.distance(distance)
        return distance
    }

    /** Donne le point médian entre toute les zones */
    fun getCenterOfPoints(points: MutableList<Point>): Point {
        var x = 0
        var y = 0
        points.forEach { point ->
            x += point.x
            y += point.y
        }
        return PointFactory.createPointWithCoordinates(x.div(points.count()), y.div(points.count()))
    }

    // general utils
    private fun isPersonnalPlayer(player: Player): Boolean = player == PERSONNALPLAYER
    fun getPlayerById(id: Int): Player? = PLAYERS.firstOrNull { it.id == id }

    fun getPlayerByDrone(drone: Drone): Player {
        PLAYERS.forEach { player ->
            if (player.drones.contains(drone)) {
                return player
            }
        }
        return PERSONNALPLAYER
    }

    /** Donne les drones alliés */
    fun getAlliedDrones(): MutableList<Drone> = PERSONNALPLAYER.drones

    /** Donne tous les drones*/
    fun getAllDrones(): MutableList<Drone> {
        val drones = mutableListOf<Drone>()
        PLAYERS.forEach { player ->
            player.drones.forEach { drone ->
                drones.add(drone)
            }
        }
        return drones
    }

    /** Donne tous les drones enemies */
    fun getEnemyDrones(): MutableList<Drone> {
        val drones = mutableListOf<Drone>()
        PLAYERS
            .filter { it !== PERSONNALPLAYER }
            .forEach { player ->
                player.drones.forEach { drone ->
                    drones.add(drone)
                }
            }
        return drones
    }
}

enum class GameStrategie {
    COUNTERSTRIKE,
    DEFENDANDCONQUER,
    CLOSESTZONE,
    FOCUSEDZONES
}

/**
 * END OF GAME ENGINE
 */