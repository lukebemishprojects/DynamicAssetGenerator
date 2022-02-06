package palette_extractor.palette;

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
}
