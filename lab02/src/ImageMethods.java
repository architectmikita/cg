import java.awt.Color;
import java.awt.image.BufferedImage;

public class ImageMethods {
    static final String[] METHODS = {
        "Оттенки серого", "Линейное контрастирование (серое)",
        "Эквализация (серое)", "Эквализация RGB", "Эквализация V в HSV",
        "Резкость: 4 соседа", "Резкость: 8 соседей",
        "Линейное контрастирование (цветное, HSV)"
    };

    static int gray(int rgb) {
        int r = (rgb >> 16) & 255;
        int g = (rgb >> 8) & 255;
        int b = rgb & 255;
        return (int) Math.round(0.299 * r + 0.587 * g + 0.114 * b);
    }

    static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    static int channel(int rgb, int channel) {
        if (channel == 0) return gray(rgb);
        if (channel <= 3) return (rgb >> (24 - channel * 8)) & 255;
        return Math.max((rgb >> 16) & 255, Math.max((rgb >> 8) & 255, rgb & 255));
    }

    static long[] histogram(BufferedImage image, int channel) {
        long[] counts = new long[256];
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                counts[channel(image.getRGB(x, y), channel)]++;
            }
        }
        return counts;
    }

    static int[] equalizationTable(long[] counts) {
        long total = 0;
        int occupied = 0;
        for (long count : counts) {
            total += count;
            if (count > 0) occupied++;
        }
        int[] table = new int[256];
        long sum = 0;
        for (int i = 0; i < 256; i++) {
            sum += counts[i];
            // Однотонное изображение оставляем без изменений.
            if (occupied <= 1) table[i] = i;
            else table[i] = (int) Math.round(255.0 * sum / total);
        }
        return table;
    }

    static int[] contrastTable(long[] counts) {
        int min = 0, max = 255;
        while (min < 255 && counts[min] == 0) min++;
        while (max > 0 && counts[max] == 0) max--;
        int[] table = new int[256];
        for (int i = 0; i < 256; i++) {

            table[i] = max <= min ? i : clamp((int) Math.round(255.0 * (i - min) / (max - min)));
        }
        return table;
    }

    static BufferedImage process(BufferedImage source, int method, double strength) {
        if (method < 0 || method >= METHODS.length) throw new IllegalArgumentException("Неизвестный метод");
        if (method == 5 || method == 6) return sharpen(source, method == 6, strength);
        int width = source.getWidth(), height = source.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int[][] tables = new int[3][];
        if (method == 1) tables[0] = contrastTable(histogram(source, 0));
        if (method == 2) tables[0] = equalizationTable(histogram(source, 0));
        if (method == 3) {
            for (int i = 0; i < 3; i++) tables[i] = equalizationTable(histogram(source, i + 1));
        }
        if (method == 4) tables[0] = equalizationTable(histogram(source, 4));
        if (method == 7) tables[0] = contrastTable(histogram(source, 4));
        float[] hsv = new float[3];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = source.getRGB(x, y);
                int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
                if (method <= 2) {
                    int value = gray(rgb);
                    if (method != 0) value = tables[0][value];
                    rgb = (value << 16) | (value << 8) | value;
                } else if (method == 3) {
                    rgb = (tables[0][r] << 16) | (tables[1][g] << 8) | tables[2][b];
                } else {
                    Color.RGBtoHSB(r, g, b, hsv);
                    int value = Math.max(r, Math.max(g, b));
                    rgb = Color.HSBtoRGB(hsv[0], hsv[1], tables[0][value] / 255f);
                }
                result.setRGB(x, y, rgb);
            }
        }
        return result;
    }

    static BufferedImage sharpen(BufferedImage source, boolean diagonals, double strength) {
        if (!Double.isFinite(strength) || strength < 0) throw new IllegalArgumentException("Неверная сила фильтра");
        int width = source.getWidth(), height = source.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int neighbors = diagonals ? 8 : 4;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int center = source.getRGB(x, y);
                int red = 0, green = 0, blue = 0;
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        if (dx == 0 && dy == 0) continue;
                        if (!diagonals && dx != 0 && dy != 0) continue;

                        int sx = Math.max(0, Math.min(width - 1, x + dx));
                        int sy = Math.max(0, Math.min(height - 1, y + dy));
                        int pixel = source.getRGB(sx, sy);
                        red += (pixel >> 16) & 255;
                        green += (pixel >> 8) & 255;
                        blue += pixel & 255;
                    }
                }
                int r = (center >> 16) & 255, g = (center >> 8) & 255, b = center & 255;

                r = clamp((int) Math.round(r + strength * (neighbors * r - red)));
                g = clamp((int) Math.round(g + strength * (neighbors * g - green)));
                b = clamp((int) Math.round(b + strength * (neighbors * b - blue)));
                result.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }
        return result;
    }
}
