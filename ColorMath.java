public class ColorMath {
    enum Model {
        RGB(new String[]{"R", "G", "B"}, new double[]{255, 255, 255}),
        CMYK(new String[]{"C", "M", "Y", "K"}, new double[]{100, 100, 100, 100}),
        HLS(new String[]{"H", "L", "S"}, new double[]{360, 100, 100});

        final String[] labels;
        final double[] maxima;

        Model(String[] labels, double[] maxima) {
            this.labels = labels;
            this.maxima = maxima;
        }
    }

    static void validate(double[] values, int count, boolean hasHue) {
        if (values == null || values.length != count) {
            throw new IllegalArgumentException("Неверное число компонент");
        }
        for (int i = 0; i < count; i++) {
            double max = hasHue && i == 0 ? 360 : 1;
            if (!Double.isFinite(values[i]) || values[i] < 0 || values[i] > max) {
                throw new IllegalArgumentException("Компонента вне допустимого диапазона");
            }
        }
    }

    static double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }

    static double[] cmykToRgb(double[] cmyk) {
        validate(cmyk, 4, false);
        double k = cmyk[3];
        return new double[]{
            (1 - cmyk[0]) * (1 - k),
            (1 - cmyk[1]) * (1 - k),
            (1 - cmyk[2]) * (1 - k)
        };
    }

    static double[] rgbToCmyk(double[] rgb) {
        validate(rgb, 3, false);
        double max = Math.max(rgb[0], Math.max(rgb[1], rgb[2]));
        // для чёрного сразу возвращаем K = 1
        if (max == 0) {
            return new double[]{0, 0, 0, 1};
        }
        return new double[]{
            (max - rgb[0]) / max,
            (max - rgb[1]) / max,
            (max - rgb[2]) / max,
            1 - max
        };
    }

    static double[] rgbToHls(double[] rgb) {
        validate(rgb, 3, false);
        double r = rgb[0], g = rgb[1], b = rgb[2];
        double max = Math.max(r, Math.max(g, b));
        double min = Math.min(r, Math.min(g, b));
        double delta = max - min;
        double lightness = (max + min) / 2;

        // у серого насыщенность равна 0
        if (delta == 0) {
            return new double[]{0, lightness, 0};
        }

        double hue;
        if (max == r) {
            hue = (g - b) / delta;
        } else if (max == g) {
            hue = (b - r) / delta + 2;
        } else {
            hue = (r - g) / delta + 4;
        }
        hue = (hue * 60 + 360) % 360;

        double saturation;
        if (lightness <= 0.5) {
            saturation = delta / (max + min);
        } else {
            saturation = delta / ((1 - max) + (1 - min));
        }
        return new double[]{hue, lightness, clamp(saturation)};
    }

    static double[] hlsToRgb(double[] hls) {
        validate(hls, 3, true);
        double hue = (hls[0] % 360) / 60;
        double lightness = hls[1];
        double saturation = hls[2];
        double c = (1 - Math.abs(2 * lightness - 1)) * saturation;
        double x = c * (1 - Math.abs(hue % 2 - 1));
        double m = lightness - c / 2;
        double[] rgb;

        switch ((int) hue) {
            case 0: rgb = new double[]{c, x, 0}; break;
            case 1: rgb = new double[]{x, c, 0}; break;
            case 2: rgb = new double[]{0, c, x}; break;
            case 3: rgb = new double[]{0, x, c}; break;
            case 4: rgb = new double[]{x, 0, c}; break;
            default: rgb = new double[]{c, 0, x};
        }
        for (int i = 0; i < 3; i++) {
            rgb[i] = clamp(rgb[i] + m);
        }
        return rgb;
    }

    static double[] toRgb(Model model, double[] values) {
        switch (model) {
            case CMYK: return cmykToRgb(values);
            case HLS: return hlsToRgb(values);
            default:
                validate(values, 3, false);
                return values.clone();
        }
    }

    static double[] fromRgb(Model model, double[] rgb) {
        switch (model) {
            case CMYK: return rgbToCmyk(rgb);
            case HLS: return rgbToHls(rgb);
            default:
                validate(rgb, 3, false);
                return rgb.clone();
        }
    }
}
