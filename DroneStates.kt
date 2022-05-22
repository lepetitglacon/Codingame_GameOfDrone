class DroneStateIdle(val drone: Drone) : DroneState {
    override fun move() : String {
        Logger.state("$drone is going standby (center of zones)")

        return GE.getCenterOfZones().toString()
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

        return GE.getCenterOfZones().toString()
    }
}

class DroneStateGoTo(val drone: Drone, val zone: Zone) : DroneState {
    override fun move() : String {
        Logger.state("$drone is going strait to $zone")
        return zone.center.toString()
    }
}