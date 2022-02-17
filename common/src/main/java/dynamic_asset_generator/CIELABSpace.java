package dynamic_asset_generator;

import java.awt.color.ColorSpace;

public class CIELABSpace extends ColorSpace {

    // D65/2
    private static final double refX = 95.047;
    private static final double refY = 100.0;
    private static final double refZ = 108.883;

    private static final ColorSpace CIEXYZ =
            ColorSpace.getInstance(ColorSpace.CS_CIEXYZ);

    private CIELABSpace() {
        super(ColorSpace.TYPE_Lab, 3);
    }

    public static CIELABSpace getInstance() {
        return Holder.CIELAB;
    }

    private static class Holder {
        public static CIELABSpace CIELAB = new CIELABSpace();
    }


    @Override
    public float[] toRGB(float[] colorvalue) {
        return CIEXYZ.toRGB(toCIEXYZ(colorvalue));
    }

    @Override
    public float[] fromRGB(float[] rgbvalue) {
        return fromCIEXYZ(CIEXYZ.toCIEXYZ(rgbvalue));
    }

    @Override
    public float[] toCIEXYZ(float[] colorvalue) {
        float _L = colorvalue[0];
        float _a = colorvalue[1];
        float _b = colorvalue[2];
        double _y = (_L+16f)/116f;
        double _x = _a/500f + _y;
        double _z = _y - _b/200f;

        if (_y*_y*_y > 0.008856) {_y = _y*_y*_y;}
        else {_y = (_y - 16f/116f) / 7.787;}
        if (_x*_x*_x > 0.008856) {_x = _x*_x*_x;}
        else {_x = (_x - 16f/116f) / 7.787;}
        if (_z*_z*_z > 0.008856) {_z = _z*_z*_z;}
        else {_z = (_z - 16f/116f) / 7.787;}

        return new float[] {(float)(_x*refX),(float)(_y*refY),(float)(_z*refZ)};
    }

    @Override
    public float[] fromCIEXYZ(float[] colorvalue) {
        float x = colorvalue[0];
        float y = colorvalue[1];
        float z = colorvalue[2];
        double _x = x/refX;
        double _y = y/refY;
        double _z = z/refZ;

        if (_x > 0.008856) {_x = Math.cbrt(_x);}
        else {_x = (7.787 * _x) + (16f/116f);}
        if (_y > 0.008856) {_y = Math.cbrt(_y);}
        else {_y = (7.787 * _y) + (16f/116f);}
        if (_z > 0.008856) {_z = Math.cbrt(_z);}
        else {_z = (7.787 * _z) + (16f/116f);}

        double _L = (116 * _y) - 16;
        double _a = 500 * (_x - _y);
        double _b = 200 * (_y - _z);
        return new float[] {(float)_L/100, (float)_a/255,(float)_b/255};
    }
}