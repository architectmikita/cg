import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

public class ColorState {
    private final EnumMap<ColorMath.Model, double[]> values = new EnumMap<>(ColorMath.Model.class);
    private final List<Runnable> listeners = new ArrayList<>();

    public ColorState() {
        set(ColorMath.Model.RGB, new double[]{0.25, 0.5, 0.75});
    }

    public double[] get(ColorMath.Model model) {
        return values.get(model).clone();
    }

    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    public void set(ColorMath.Model source, double[] input) {
        double[] rgb = ColorMath.toRgb(source, input);
        for (ColorMath.Model model : ColorMath.Model.values()) {
            double[] converted;
            // оставляем введённые значения без обратного пересчёта
            if (model == source) {
                converted = input.clone();
            } else {
                converted = ColorMath.fromRgb(model, rgb);
            }

            // у серого сохраняем прежнее значение H
            if (model == ColorMath.Model.HLS && model != source
                    && converted[2] == 0 && values.containsKey(model)) {
                converted[0] = values.get(model)[0];
            }
            values.put(model, converted);
        }
        for (Runnable listener : List.copyOf(listeners)) {
            listener.run();
        }
    }
}
