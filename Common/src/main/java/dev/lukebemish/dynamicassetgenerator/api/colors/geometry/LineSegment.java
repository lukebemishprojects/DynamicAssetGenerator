package dev.lukebemish.dynamicassetgenerator.api.colors.geometry;

/**
 * Represents a line segment between two colors. The start and end colors provided should be in RGB24 (or ARGB32) format.
 */
public record LineSegment(int start, int end) {

    /**
     * @return the minimum distance from the provided color to this segment, in the provided color coordinates
     */
    public double distanceTo(int point, ColorCoordinates coordinates) {
        double startToPoint = coordinates.distance(start, point);
        double endToPoint = coordinates.distance(end, point);
        double startToEnd = coordinates.distance(start, end);

        if (startToPoint * startToPoint >= endToPoint * endToPoint + startToEnd * startToEnd) {
            return endToPoint;
        } else if (endToPoint * endToPoint >= startToPoint * startToPoint + startToEnd * startToEnd) {
            return startToPoint;
        } else {
            double halfPerimeter = (startToPoint + endToPoint + startToEnd) / 2;
            double area = Math.sqrt(halfPerimeter * (halfPerimeter - startToPoint) * (halfPerimeter - endToPoint) * (halfPerimeter - startToEnd));
            return 2 * area / startToEnd;
        }
    }
}
