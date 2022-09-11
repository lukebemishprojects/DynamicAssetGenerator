package io.github.lukebemish.dynamic_asset_generator.impl.client.palette;

import io.github.lukebemish.dynamic_asset_generator.impl.client.ColorConversionUtils;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class ColorHolder implements Comparable<ColorHolder> {
    private final float r;
    private final float g;
    private final float b;
    private final float a;

    public static int toColorInt(ColorHolder color) {
        int ret = 0;
        ret |= (Math.round(Mth.clamp(color.a*255, 0, 255))&0xFF)<<24;
        ret |= (Math.round(Mth.clamp(color.b*255, 0, 255))&0xFF)<<16;
        ret |= (Math.round(Mth.clamp(color.g*255, 0, 255))&0xFF)<< 8;
        ret |= (Math.round(Mth.clamp(color.r*255, 0, 255))&0xFF);
        return ret;
    }

    public static ColorHolder fromColorInt(int color) {
        return new ColorHolder(
                (color    &0xFF)/255f,
                (color>> 8&0xFF)/255f,
                (color>>16&0xFF)/255f,
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
        ColorHolder c1 = this.toCIELAB();
        ColorHolder c2 = o.toCIELAB();
        float mySum = c1.r;
        float otherSum = c2.r;
        if (mySum > otherSum) {
            return 1;
        } else if (mySum < otherSum) {
            return -1;
        }
        return 0;
    }

    public static ColorHolder alphaBlend(ColorHolder over, ColorHolder under) {
        float a0 = over.a + under.a * (1-over.a);
        float r0 = (over.r* over.a + under.r* under.a*(1-over.a))/a0;
        float g0 = (over.g* over.a + under.g* under.a*(1-over.a))/a0;
        float b0 = (over.b* over.a + under.b* under.a*(1-over.a))/a0;
        return new ColorHolder(r0,g0,b0,a0);
    }

    public ColorHolder withA(float a) {
        return new ColorHolder(this.r,this.g,this.b,a);
    }

    public double distanceToLS(ColorHolder c) {
        ColorHolder c1 = c.toHLS();
        ColorHolder c2 = this.toHLS();
        return Math.sqrt((c2.g-c1.g)*(c2.g-c1.g)+
                        (c2.b-c1.b)*(c2.b-c1.b));
    }

    private static final double HYBRID_W_LS = 2;
    private static final double HYBRID_W_LAB = 1;
    public double distanceToHybrid(ColorHolder c) {
        return (distanceToLS(c)*HYBRID_W_LS+distanceToLab(c)*HYBRID_W_LAB)/(HYBRID_W_LS+HYBRID_W_LAB);
    }

    public double distanceToHLS(ColorHolder c) {
        ColorHolder c1 = c.toHLS();
        ColorHolder c2 = this.toHLS();
        return Math.sqrt((c2.r-c1.r)*(c2.r-c1.r)+
                (c2.g-c1.g)*(c2.g-c1.g)+
                (c2.b-c1.b)*(c2.b-c1.b));
    }

    public double distanceToRGB(ColorHolder c) {
        ColorHolder c2 = this;
        return Math.sqrt((c2.r- c.r)*(c2.r- c.r)+
                (c2.g- c.g)*(c2.g- c.g)+
                (c2.b- c.b)*(c2.b- c.b));
    }

    public double distanceToLab(ColorHolder c) {
        return distanceToLab(c,1);
    }
    public double euDistanceToLab(ColorHolder c) {
        ColorHolder c1 = c.toCIELAB();
        ColorHolder c2 = this.toCIELAB();
        return Math.sqrt((c2.g-c1.g)*(c2.g-c1.g)+
                (c2.b-c1.b)*(c2.b-c1.b)+(c2.r-c1.r)*(c2.r-c1.r));
    }
    public double distanceToLab(ColorHolder c, float weightL) {
        ColorHolder c1 = c.toCIELAB();
        ColorHolder c2 = this.toCIELAB();
        return Math.sqrt((c2.g-c1.g)*(c2.g-c1.g)+
                (c2.b-c1.b)*(c2.b-c1.b)) + Math.abs(c2.r-c1.r)/2*weightL;
    }

    public ColorHolder toHLS() {
        float max = max(r,g,b);
        float min = min(r,g,b);
        float h,s,l;
        h = s = l = (max+min)/2;
        if (max==min) {
            h=s=0;
        } else {
            float d = max-min;
            s = l > 0.5 ? d / (2 - max - min) : d / (max + min);
            int m = max==r ? 0 : max==g ? 1 : 2;
            h = switch (m) {
                case 0 -> (g - b) / d + (g < b ? 6 : 0);
                case 1 -> (b - r) / d + 2;
                default -> (r - g) / d + 4;
            };
            h/=6;
        }
        return new ColorHolder(h,l,s);
    }

    public ColorHolder fromHLS() {
        float h = r;
        float l = g;
        float s = b;
        float _a, _b, _c, _d, _e;

        if (s == 0) {
            _c = _d = _e = l;
        } else {
            _a = l < 0.5 ? (l * (1 + s)) : (l + s - l * s);
            _b = 2 * l - _a;
            _c = hlsRgbHelper(_b, _a, h + 1.0f / 3);
            _d = hlsRgbHelper(_b, _a, h);
            _e = hlsRgbHelper(_b, _a, h - 1.0f / 3);
        }
        return new ColorHolder(_c,_d,_e);
    }

    private static float hlsRgbHelper(float p, float q, float h) {
        if (h < 0) {
            h += 1;
        }
        if (h > 1) {
            h -= 1;
        }
        if (6 * h < 1) {
            return p + ((q - p) * 6 * h);
        }
        if (2 * h < 1) {
            return q;
        }
        if (3 * h < 2) {
            return p + ((q - p) * 6 * ((2.0f / 3.0f) - h));
        }
        return p;
    }

    public ColorHolder toCIELAB() {
        return ColorConversionUtils.rgb2lab(this);
    }

    public ColorHolder fromCIELAB() {
        return ColorConversionUtils.lab2rgb(this);
    }

    private static float max(float a, float b, float c) {
        return Math.max(Math.max(a,b),c);
    }

    private static float min(float a, float b, float c) {
        return Math.min(Math.min(a,b),c);
    }

    public float get_L() {
        return this.r;
    }

    public float get_a() {
        return this.g;
    }

    public float get_b() {
        return this.b;
    }

    public float getH() {
        return this.r;
    }

    public float getL() {
        return this.g;
    }

    public float getS() {
        return this.b;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ColorHolder that = (ColorHolder) o;
        return that.r == r && that.g == g && that.b == b && that.a == a;
    }

    @Override
    public int hashCode() {
        return Objects.hash(r, g, b, a);
    }

    public float getX() {
        return r;
    }
    public float getY() {
        return g;
    }
    public float getZ() {
        return b;
    }

    public int toInt() {
        return ColorHolder.toColorInt(this);
    }
}
