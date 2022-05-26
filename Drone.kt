

/**
 * START DRONE
 */

class Drone (val id: Int, var position: Point) {
    var lastPosition: Point = position
    var state: DroneState = DroneStateClosestZone(this)
    var closestZones = mutableListOf<Zone>()
    var target: Zone? = null

    fun calculateTarget(filters: MutableList<Zone> = mutableListOf()) {
        val candidates = mutableListOf<ZoneTarget>()
        var zonesToTarget = GE.ZONES
            .filterNot { filters.contains(it) || it.willBeUnderControl() }
            .toMutableList()

        zonesToTarget = zonesToTarget.filterNot { filters.contains(it) }.toMutableList()

        zonesToTarget.forEach { zone ->
            val ratio = (GE.getDistance(zone.center, position) - GE.getDistance(zone.center, lastPosition)).absoluteValue
            candidates.add(ZoneTarget(zone, GE.getDistance(zone.center, position), ratio))
        }

        if (candidates.isNotEmpty()) {
            candidates.sortBy { it.distance }
            candidates.sortByDescending { it.ratio }

            if (targetChanges(candidates) && GE.isAWinningTrade()) {

                // enleve l'ancienne target de la zone
                if (target?.targets?.contains(this) == true) {
                    target?.targets?.remove(this)
                }

                // change la target
                target = candidates.first().zone

                // ajout la nouvelle target a la zone
                if (!candidates.first().zone.targets.contains(this)) {
                    candidates.first().zone.targets.add(this)
                }
                goToZone(target!!)
            }
        } else {
            target = closestZones.first()
            // reset candidates
            lastPosition = position
        }
    }

    private fun targetChanges(candidates: MutableList<ZoneTarget>) = target != candidates.first().zone
    fun goToZone(zone: Zone) = changeState(DroneStateGoTo(this, zone))

    /** Calcule les zones les plus proches */
    fun calculateClosestZones(filter: Zone? = null) {
        val zonesToCompare: MutableList<Zone>
        val zonesTargets = mutableListOf<ZoneTarget>()
        closestZones.clear()

        // filtrer une zone
        if (filter !== null) {
            zonesToCompare = GE.ZONES.filterNot { it == filter }.toMutableList()
        } else {
            zonesToCompare = GE.ZONES
        }
        // calcule distance
        zonesToCompare.forEach { zone -> zonesTargets.add(ZoneTarget(zone, GE.getDistance(zone.center, this.position))) }
        zonesTargets.sortBy { it.distance }

        Logger.log("zones du $this")
        zonesTargets.forEach {
            Logger.log("\t$it")
            closestZones.add(it.zone)
        }
    }

    /** */
    fun canHandleCall() = target?.isUnderControl() ?: false
    /** Donne si un drone est dans une Zone ou non */
    fun isInRadius(zone: Zone) : Boolean = GE.getDistance(zone.center, this.position) < 100
    /** Donne si un drone est le nôtre ou non */
    fun isPersonnal(): Boolean = GE.getPlayerByDrone(this) == GE.PERSONNALPLAYER
    /** Change le state du drone */
    fun changeState(state: DroneState) { this.state = state }

    /** Donne le point vers lequel le drone doit se diriger */
    fun move() : Point = this.state.move()
    override fun toString() : String = "Drone $id"
}

class ZoneTarget(val zone: Zone, val distance: Double, val ratio: Double = 0.0) {
    override fun toString(): String = "Target $zone à distance $distance avec un ratio de $ratio"
}

class DroneTarget(val drone: Drone, val distance: Double) {
    override fun toString(): String = "Target $drone à distance $distance"
}

object DroneFactory { fun createDrone(id: Int, position: Point): Drone = Drone(id, position) }
interface DroneState { fun move() : Point }

class DroneStateIdle(val drone: Drone) : DroneState {
    override fun move() : Point {
        Logger.state("$drone is going standby (center of zones)")
        return drone.position
    }
}

class DroneStateDefendZone(val drone: Drone) : DroneState {
    override fun move() : Point {
        Logger.state("$drone stays at position")
        return drone.position
    }
}

class DroneStateClosestZone(val drone: Drone) : DroneState {
    override fun move() : Point {
        Logger.state("$drone is going at closest zone ${drone.closestZones.first()}")
        return drone.closestZones.first().center
    }
}

class DroneStateGoTo(val drone: Drone, val zone: Zone) : DroneState {
    override fun move() : Point {
        Logger.state("$drone is going strait to $zone")
        return zone.center
    }
}

/**
 * END OF DRONE
 */


