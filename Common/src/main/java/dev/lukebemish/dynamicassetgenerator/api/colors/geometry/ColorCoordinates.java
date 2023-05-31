package dev.lukebemish.dynamicassetgenerator.api.colors.geometry;

/**
 * A 3D color space, where each color is represented by an x, y, and z coordinate, that contains methods for
 * transforming from RGB24 (or ARGB32) color values.
 */
public interface ColorCoordinates {
    /**
     * @return the first coordinate of the provided color
     */
    int getX(int color);

    /**
     * @return the second coordinate of the provided color
     */
    int getY(int color);

    /**
     * @return the third coordinate of the provided color
     */
    int getZ(int color);

    /**
     * @return the Euclidean distance between the two provided colors, in this color space
     */
    default double distance(int color1, int color2) {
        int dx = getX(color1) - getX(color2);
        int dy = getY(color1) - getY(color2);
        int dz = getZ(color1) - getZ(color2);

        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
