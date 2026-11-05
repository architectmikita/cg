import javax.swing.*;
import java.awt.*;

public class HistogramPanel extends JPanel {
    private long[] counts = new long[256];
    private long scale = 1;
    private Color barColor = Color.DARK_GRAY;

    HistogramPanel() {
        setPreferredSize(new Dimension(400, 145));
        setBackground(Color.WHITE);
    }

    void setHistogram(long[] counts, long scale, Color color) {
        this.counts = counts;
        this.scale = Math.max(1, scale);
        this.barColor = color;
        repaint();
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int left = 48, top = 18;
        int width = getWidth() - left - 12, height = getHeight() - top - 24;
        if (width <= 0 || height <= 0) return;
        g.setColor(Color.GRAY);
        g.drawLine(left, top + height, left + width, top + height);
        g.drawLine(left, top, left, top + height);
        g.drawString(Long.toString(scale), 3, top + 4);
        g.drawString("0", left - 3, top + height + 17);
        g.drawString("255", left + width - 20, top + height + 17);
        g.setColor(barColor);
        for (int i = 0; i < 256; i++) {
            int x = left + i * width / 256;
            int next = left + (i + 1) * width / 256;
            int h = (int) Math.round(counts[i] * (double) height / scale);
            g.fillRect(x, top + height - h, Math.max(1, next - x), h);
        }
    }
}
