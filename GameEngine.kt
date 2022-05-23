import java.util.*
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sqrt

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

    // constantes
    val zones = mutableListOf<Zone>()
    val players = mutableListOf<Player>()
    var personnalPlayer: Player = PlayerFactory.createPlayer(999)

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
        for (zoneId in 0 until numberOfZones) {
            val X = input.nextInt()
            val Y = input.nextInt()
            addZone(ZoneFactory.createZone(zoneId, PointFactory.createPoint(X, Y)))
        }
    }

    private fun initPlayersAndDrones() {
        for (i in 0 until numberOfPlayers) {
            val player: Player

            if (i == personnalId) {
                personnalPlayer.id = i
                player = personnalPlayer
            } else {
                player = PlayerFactory.createPlayer(i)
                addPlayer(player)
            }

            // DRONES INIT
            for (j in 0 until numberOfDrones) {
                val drone = DroneFactory.createDrone(j,PointFactory.createPoint(0, 0))
                player.drones.add(drone)
            }
        }
    }

    /** Update objects with CodeinGame input */
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

    /** Engine */
    fun winning(): Boolean {
        return controlsMoreZones() && hasMorePoints()
    }

    private fun controlsMoreZones(): Boolean {
        return zones.filter { it.controlledBy == personnalPlayer }.count() >= numberOfZones.div(2)
    }

    private fun hasMorePoints(): Boolean {
        return players.filter { it.points >= personnalPlayer!!.points }.first() == personnalPlayer
    }

    /**
     * main
     */
    fun play() {

        while (true) {
            Logger.engine("Tour $turns")

            // input from coding game
            updateZonesControl()
            updateDronesPosition()

            // tour 0
            if (isFirstTurn()) {
                calculateClosestZones()
            } else {

                // differentes fonctions selon la stratégie
                when (gameStrategie) {

                    GameStrategie.COUNTERSTRIKE -> {

                        if (winning()) {
                            Logger.engine("WE ARE WINNING HOLD ON !!!!!!!!\n")
                            calculateDefendingTargets()
                            counterTargets()
                        } else {
                            calculateTargets()
                            counterTargets()
                        }
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

            addPoints()
            showPoints()

            players.forEach { player ->
                addTurn(player)
            }
        }
    }

    private fun isFirstTurn() = turns == 0

    // engine
    private fun addTurns() {
        players.forEach { player ->
            addTurn(player)
        }
    }

    private fun addTurn(player: Player) {

        if (isPersonnalPlayer(player)) {
            player.drones.forEach { drone ->
                Logger.log("make a move")
                println(drone.move())
            }
        }

        turns++
    }

    private fun addPoints() {
        zones.forEach { zone ->
            zone.controlledBy?.points = zone.controlledBy?.points!! + 1
        }
    }

    private fun showPoints() {
        players.forEach { player ->
            Logger.engine("POINTS\n")
            Logger.engine("\t$player has ${player.points} points")
        }
    }

    /** Update objects/state/objectives */

    // calculation
    fun calculateTargets() {
        zones.forEach { zone -> zone.setDronesInRadius() }
        getAllDrones().forEach { it.calculateTarget() }
    }

    fun calculateDefendingTargets() {
        zones.forEach { zone -> zone.setDronesInRadius() }
        getAllDrones().forEach { it.calculateTarget() }
    }

    fun counterTargets() {

        zones.forEach { zone ->

            val inRadiusDelta = zone.getAlliedDronesInRadius().count() - zone.getEnemyDronesInRadius().count()
            val inCommingDroneDelta = zone.getAlliedDronesTargets().count() - zone.getEnemyDronesTargets().count()
            val globalDelta = inRadiusDelta + inCommingDroneDelta

            when (globalDelta.sign) {

                /** On va controller la zone */
                1 -> {
                    when (inRadiusDelta.sign) {
                        // on controlle actuellement la zone
                        1 -> {
                            when (inCommingDroneDelta.sign) {
                                // plus d'allié sont en route
                                // on redistribue le surplus
                                1 -> {
                                    zone.redistributeDrones(zone.getAlliedDronesTargets())
                                }
                                // aucun ou le même nombre
                                // on ne change pas les targets sinon on perd le point
                                0 -> {

                                }
                            }
                        }
                        // la zone est libre ou sous tension
                        0 -> {
                            if (zone.isFree()) {

                            }
                            when (inCommingDroneDelta.sign) {
                                // plus d'allié sont en route
                                // on redistribue le surplus
                                1 -> {
                                    zone.redistributeDrones(zone.getAlliedDronesTargets())
                                }
                                // aucun ou le même nombre
                                // on ne change pas les targets sinon on perd le point
                                0 -> {

                                }
                            }
                        }
                        // on n'a pas la zone
                        -1 -> {

                        }
                    }
                }

                // La zone va etre neutre ou sous tensions
                0 -> {
                    if (zone.dronesInRadius.isEmpty()) {
                        Logger.zoneControl("$zone est libre")
                        zone.callUnusedDrone()
                    } else {
                        when (inRadiusDelta.sign) {
                            // on controlle actuellement la zone

                            1 -> {
                                when (inCommingDroneDelta.sign) {
                                    // plus d'allié sont en route
                                    1 -> {
                                        Logger.zoneControl("$zone est sous tension, controllé par ${zone.controlledBy}")
                                    }
                                    // aucun ou le même nombre
                                    0 -> {
                                        // on test le drone le plus proche
                                        var closestDrone: Drone = zone.getDrones().first()
                                        var distance: Double = GE.getDistance(zone.center, closestDrone.position)
                                        zone.getDrones().forEach {
                                            if (GE.getDistance(zone.center, it.position) < distance) {
                                                closestDrone = it
                                                distance = GE.getDistance(zone.center, it.position)
                                            }
                                        }

                                        if (closestDrone.isPersonnal()) {
                                            Logger.zoneControl("$zone est sous tension, controllé par ${zone.controlledBy}")
                                        }
                                    }
                                }
                            }
                            // la zone est sous tension
                            0 -> {

                            }
                            // on n'a pas la zone
                            -1 -> {

                            }
                        }
                    }

                }

                // L'ennemi va controller la zone
                -1 -> {
                    Logger.zoneControl("$zone va se faire controller par un autre joueur")
                    zone.redistributeDrones(zone.getAlliedDrones())
                }
            }

        }
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

enum class GameStrategie {
    COUNTERSTRIKE,
    DEFENDANDCONQUER,
    CLOSESTZONE
}