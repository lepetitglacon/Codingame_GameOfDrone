import kotlin.math.absoluteValue

class Drone
    (
    val id: Int,
    var position: Point,
    var lastPosition: Point
    )
{
    var state: DroneState = DroneStateClosestZone(this)
    var closestZones = mutableListOf<Zone>()
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
    fun calculateTarget(filters: MutableList<Zone> = mutableListOf()) {
        val candidates = mutableListOf<ZoneTarget>()
        val zonesToTarget = GE.zones.filterNot { filters.contains(it) || it.willBeUnderControl() }.toMutableList()

        zonesToTarget.forEach { zone ->
            val ratio = (GE.getDistance(zone.center, position) - GE.getDistance(zone.center, lastPosition)).absoluteValue
            candidates.add(ZoneTarget(zone, GE.getDistance(zone.center, position), ratio))
        }

        if (candidates.isNotEmpty()) {
            candidates.sortByDescending { it.ratio }

            if (targetChanges(candidates)) {

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
            zonesToCompare = GE.zones.filterNot { it == filter }.toMutableList()
        } else {
            zonesToCompare = GE.zones
        }

        // calcule distance
        zonesToCompare.forEach { zone ->
            zonesTargets.add(ZoneTarget(zone, GE.getDistance(zone.center, this.position)))
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
        val h = GE.getDistance(zone.center, this.position)
        return h < 100
    }

    fun isPersonnal(): Boolean {
        return GE.getPlayerByDrone(this) == GE.personnalPlayer
    }

    /**
     * Change le state du drone
     */
    fun changeState(state: DroneState) {

        if (!this.state.javaClass.isInstance(state)) {
            this.state = state
            Logger.state("$this changing state to ${state.javaClass.name}")
        } else {
            this.state = state
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
        return "Drone $id from ${GE.getPlayerByDrone(this)} is at position $position"
    }

    override fun toString() : String {
        return "Drone $id"
    }
}


class ZoneTarget(val zone: Zone, val distance: Double, val ratio: Double = 0.0) {
    override fun toString(): String {
        return "Target $zone à distance $distance avec un ratio de $ratio"
    }
}

class DroneTarget(val drone: Drone, val distance: Double) {
    override fun toString(): String {
        return "Target $drone à distance $distance"
    }
}

object DroneFactory {
    fun createDrone(id: Int, position: Point): Drone {
        return Drone(id, position, Point(0, 0))
    }
}

interface DroneState {
    fun move() : String
}
