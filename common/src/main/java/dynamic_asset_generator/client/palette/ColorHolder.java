package dynamic_asset_generator.client.palette;

import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

public class ColorHolder implements Comparable<ColorHolder> {
    private final float r;
    private final float g;
    private final float b;
    private final float a;

    public static int toColorInt(ColorHolder color) {
        int ret = 0;
        ret |= (Math.round(Mth.clamp(color.getA()*255, 0, 255))&0xFF)<<24;
        ret |= (Math.round(Mth.clamp(color.getR()*255, 0, 255))&0xFF)<<16;
        ret |= (Math.round(Mth.clamp(color.getG()*255, 0, 255))&0xFF)<< 8;
        ret |= (Math.round(Mth.clamp(color.getB()*255, 0, 255))&0xFF);
        return ret;
    }

    public static ColorHolder fromColorInt(int color) {
        return new ColorHolder(
                (color>>16&0xFF)/255f,
                (color>> 8&0xFF)/255f,
                (color    &0xFF)/255f,
                (color>>24&0xFF)/255f
        );
    }

    public ColorHolder(float v) {
        this.r = v;
        this.g = v;
        this.b = v;
        this.a = 1.0f;
    }

    public ColorHolder(float r, float g, float b) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = 1.0f;
    }

    public ColorHolder(float r, float g, float b, float a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    public float getR() {
        return r;
    }

    public float getG() {
        return g;
    }

    public float getB() {
        return b;
    }

    public float getA() {
        return a;
    }

    @Override
    public int compareTo(@NotNull ColorHolder o) {
        float mySum = this.r+this.g+this.b;
        float otherSum = o.r+o.g+o.b;
        if (mySum > otherSum) {
            return 1;
        } else if (mySum < otherSum) {
            return -1;
        }
        return 0;
    }

    public static ColorHolder alphaBlend(ColorHolder over, ColorHolder under) {
        float a0 = over.getA() + under.getA() * (1-over.getA());
        float r0 = (over.getR()* over.getA() + under.getR()* under.getA()*(1-over.getA()))/a0;
        float g0 = (over.getG()* over.getA() + under.getG()* under.getA()*(1-over.getA()))/a0;
        float b0 = (over.getB()* over.getA() + under.getB()* under.getA()*(1-over.getA()))/a0;
        return new ColorHolder(r0,g0,b0,a0);
    }

    public ColorHolder withA(float a) {
        return new ColorHolder(this.getR(),this.getG(),this.getB(),a);
    }

    public double distanceTo(ColorHolder c) {
        ColorHolder c1 = c.toHSL();
        ColorHolder c2 = this.toHSL();
        return Math.sqrt(//(c2.r-c1.getR())*(c2.r-c1.getR())+
                (c2.g-c1.getG())*(c2.g-c1.getG())+
                (c2.b-c1.getB())*(c2.b-c1.getB()));
    }

    public ColorHolder toHSL() {
        float cMax = max(this.r,this.g,this.b);
        float cMin = min(this.r,this.g,this.b);
        float delta = cMax - cMin;
        float l = (cMax + cMin)/2;
        float s = 0;
        float h = 0;
        if (delta == 0) {
            h = 0;
            s = 0;
        } else {
            s = delta/(1-Math.abs(2*l-1));
            if (cMax == this.r) {
                h = (this.g-this.b)/delta;
            } else if (cMax == this.g) {
                h = 2 + (this.b-this.r)/delta;
            } else if (cMax == this.b) {
                h = 4 + (this.r-this.g)/delta;
            }
            h = h / 6;
            if (h<0) {
                h += 1;
            } else if (h>1) {
                h -= 1;
            }
        }
        return new ColorHolder(h,s,l);
    }

    private static float max(float a, float b, float c) {
        return Math.max(Math.max(a,b),c);
    }

    private static float min(float a, float b, float c) {
        return Math.min(Math.min(a,b),c);
    }
}
