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