/**
 * START ZONE
 */

class Zone(val id: Int, val center: Point) {
    var controlledBy: Player? = null
    var dronesInRadius: MutableList<Drone> = mutableListOf()
    var targets: MutableList<Drone> = mutableListOf()
    var closestDrones: MutableList<Drone> = mutableListOf()

    fun setDronesInRadius() {
        Logger.radius("Drones in radius of $this")

        GE.getAllDrones().forEach { drone ->
            if (isDroneInRadius(drone)) {
                if (!dronesInRadius.contains(drone)) {
                    dronesInRadius.add(drone)
                }
            } else {
                dronesInRadius.remove(drone)
            }
        }
    }

    fun getDrones(): MutableList<Drone> {
        var drones = getAlliedDrones()
        drones = (drones + getEnemyDrones()).toMutableList()
        return drones.toSet().toMutableList()
    }

    fun getAlliedDrones(): MutableList<Drone> {
        var drones = getAlliedDronesTargets().toMutableList()
        drones = (drones + getAlliedDronesInRadius()).toMutableList()
        return drones.toSet().toMutableList()
    }

    fun getEnemyDrones(): MutableList<Drone> {
        val drones = getEnemyDronesTargets()
            .filter { drone -> drone.isPersonnal() }
            .toMutableList()
        if (drones.addAll(dronesInRadius.filter { drone -> drone.isPersonnal() }.toMutableList())) else Logger.log("alldrones")
        return drones.toSet().toMutableList()
    }

    fun getAlliedDronesInRadius(): MutableList<Drone> = dronesInRadius.filter { drone -> drone.isPersonnal() }.toMutableList()
    fun getEnemyDronesInRadius(): MutableList<Drone> = dronesInRadius.filterNot { drone -> drone.isPersonnal() }.toMutableList()
    fun getAlliedDronesTargets(): MutableList<Drone> = targets.filter { drone -> drone.isPersonnal() }.toMutableList()
    fun getEnemyDronesTargets(): MutableList<Drone> = targets.filterNot { drone -> drone.isPersonnal() }.toMutableList()

    fun redistributeDrones(dronesToRedistribute: MutableList<Drone>) {
        dronesToRedistribute.forEach { drone ->
            Logger.log("Redistribution de $drone")
            drone.calculateTarget(mutableListOf(this))
        }
    }

    fun callUnusedDrone() {
        calculateClosestDrones()
        val handlers = closestDrones.filter { it.canHandleCall() }

        Logger.log("handlers $handlers")

        if (handlers.isNotEmpty()) {
            Logger.log("${handlers.first()} answering the call")
            handlers.first().goToZone(this)
        }

    }

    fun isDroneInRadius(drone: Drone) : Boolean = GE.getDistance(this.center, drone.position) < 100

    fun closestDrone(): Drone {
        var closestDrones = mutableListOf<DroneTarget>()
        GE.getAlliedDrones().forEach { drone ->
            closestDrones.add(DroneTarget(drone, GE.getDistance(this.center, drone.position)))
        }
        closestDrones.sortBy { it.distance }
        return closestDrones.first().drone
    }

    fun calculateClosestDrones() {
        val droneTargets = mutableListOf<DroneTarget>()
        closestDrones.clear()

        GE.getAlliedDrones().forEach {
            droneTargets.add(DroneTarget(it, GE.getDistance(this.center, it.position)))
        }

        droneTargets.sortBy { it.distance }
        droneTargets.forEach { closestDrones.add(it.drone) }
    }

    fun calculateDistanceWithOtherZones(): Double {
        var distance = 0.0
        GE.ZONES.forEach { zone ->
            distance += GE.getDistance(this.center, zone.center)
        }
        return distance
    }

    fun calculateDistanceWithOtherZonesOrderedByZones(): List<ZoneTarget> {
        var zoneTargets = mutableListOf<ZoneTarget>()
        GE.ZONES.forEach { zone ->
            zoneTargets.add(ZoneTarget(zone, GE.getDistance(this.center, zone.center)))
        }
        zoneTargets.sortBy { it.distance }
        return zoneTargets
    }

    fun isUnderControl(): Boolean = controlledBy == GE.PERSONNALPLAYER && getEnemyDronesTargets().isEmpty()
    /** Vrai si le nombre de drone des autres joueurs sont supérieur à 1 */
    fun isLockedByOtherPlayer() = getEnemyDronesTargets().groupBy { GE.getPlayerByDrone(it) }.filter { entry -> entry.value.count() > 1 }.isNotEmpty()
    /** Vrai si le nombre de drones dans son radius est null */
    fun isFree(): Boolean = dronesInRadius.isEmpty()
    fun isNotFocused() = getEnemyDronesTargets().isEmpty()
    fun willBeUnderControl(): Boolean = controlledBy == GE.PERSONNALPLAYER && getEnemyDronesTargets().isEmpty()

    fun toStringAll() : String = "Zone $id [controlled by $controlledBy, center's at $center]"
    override fun toString() : String = "Zone $id"
}

object ZoneFactory {
    fun createZone(id: Int, point: Point): Zone {
        return Zone(id, point)
    }
}

/**
 * END OF ZONE
 */