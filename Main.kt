/**
CORRECTION
bien respecter les consignes 5/20
pouvoir faire bouger les drones 10/20
travailler sur la logique 10-20/20
 */

fun main(args : Array<String>) {

    // INITIALIZATION
    GE.init(GameStrategie.COUNTERSTRIKE)

    // GAME LOOP
    GE.play()
}

abstract class ZoneObserver() {
    abstract fun onZoneCapture()
    abstract fun onZoneEnter()
    abstract fun onZoneLeave()
}
