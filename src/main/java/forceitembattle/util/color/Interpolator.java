package forceitembattle.util.color;

@FunctionalInterface
public interface Interpolator {

    double[] interpolate(double from, double to, int max);

}
