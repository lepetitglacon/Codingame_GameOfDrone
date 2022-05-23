class Zone(val id: Int, val center: Point) {
    var controlledBy: Player? = null

    var dronesInRadius: MutableList<Drone> = mutableListOf()
    var targets: MutableList<Drone> = mutableListOf()

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
        Logger.log("drones\n\t${drones.toSet().toMutableList()}")
        return drones.toSet().toMutableList()
    }

    fun getAlliedDrones(): MutableList<Drone> {
        var drones = getAlliedDronesTargets().toMutableList()
        drones = (drones + getAlliedDronesInRadius()).toMutableList()
        Logger.log("drones\n\t${drones.toSet().toMutableList()}")
        return drones.toSet().toMutableList()
    }

    fun getEnemyDrones(): MutableList<Drone> {
        val drones = getEnemyDronesTargets()
            .filter { drone -> drone.isPersonnal() }
            .toMutableList()
        if (drones.addAll(dronesInRadius.filter { drone -> drone.isPersonnal() }.toMutableList())) else Logger.log("alldrones")
        return drones.toSet().toMutableList()
    }

    fun getAlliedDronesInRadius(): MutableList<Drone> {
        return dronesInRadius.filter { drone -> drone.isPersonnal() }.toMutableList()
    }

    fun getEnemyDronesInRadius(): MutableList<Drone> {
        return dronesInRadius.filter { drone -> !drone.isPersonnal() }.toMutableList()
    }

    fun getAlliedDronesTargets(): MutableList<Drone> {
        return targets.filter { drone -> drone.isPersonnal() }.toMutableList()
    }

    fun getEnemyDronesTargets(): MutableList<Drone> {
        return targets.filter { drone -> !drone.isPersonnal() }.toMutableList()
    }

    fun redistributeDrones(dronesToRedistribute: MutableList<Drone>) {
        dronesToRedistribute.forEach { drone ->
            Logger.log("Redistribution de $drone")
            drone.calculateTarget(mutableListOf(this))
        }
    }

    fun redistributeAlliedDronesByDelta(numberOfDronesToRedistribute: Int? = null) {

        if (numberOfDronesToRedistribute == null) {
            Logger.log("Redistribution de tous les drones ${getAlliedDronesInRadius()}")
            getAlliedDrones().forEach { drone -> drone.calculateTarget(mutableListOf(this)) }
        } else {
            Logger.log("Redistribution de $numberOfDronesToRedistribute drones")

            getAlliedDrones().forEachIndexed() { index, drone ->
                if (index < numberOfDronesToRedistribute) {
                    drone.calculateTarget(mutableListOf(this))
                    Logger.log("$drone, index $index, target : ${drone.target}")

                }
            }
        }
    }

    fun callUnusedDrone() {
        Logger.log("Unused drone ${closestDrone()} has been called")
        if (closestDrone().target!!.getAlliedDrones().count() > 1 && !closestDrone().target!!.isFree()) {
            Logger.log("${closestDrone()} answering the call")
            closestDrone().goToZone(this)
        }

    }

    /**
     * @param drone
     * @return true si le drone est dans la zone
     */
    fun isDroneInRadius(drone: Drone) : Boolean {
        val h = GE.getDistance(this.center, drone.position)
        return h < 100
    }

    fun closestDrone(): Drone {
        var closestDrones = mutableListOf<DroneTarget>()

        GE.getDrones().forEach { drone ->
            closestDrones.add(DroneTarget(drone, GE.getDistance(this.center, drone.position)))
        }

        closestDrones.sortBy { it.distance }

        return closestDrones.first().drone
    }

    fun calculateDistanceWithOtherZones(): Double {
        var distance = 0.0
        GE.zones.forEach { zone ->
            distance += GE.getDistance(this.center, zone.center)
        }
        return distance
    }

    fun isUnderControl(): Boolean {
        return controlledBy == GE.personnalPlayer && getEnemyDronesTargets().isEmpty()
    }

    fun isFree(): Boolean {
        return dronesInRadius.isEmpty()
    }

    fun willBeUnderControl(): Boolean {
        return controlledBy == GE.personnalPlayer && getEnemyDronesTargets().isEmpty()
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