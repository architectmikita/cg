import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.Locale;

public class ColorLab {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Лабораторная 1");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(new LabPanel());
            frame.setSize(1200, 800);
            frame.setMinimumSize(new Dimension(1200, 800));
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    static Color awt(double[] rgb) {
        return new Color((float)rgb[0], (float)rgb[1], (float)rgb[2]);
    }

    static class LabPanel extends JPanel {
        final ColorState state = new ColorState();
        final Editor[] editors = new Editor[3];
        LabPanel() {
            super(new BorderLayout(12, 12));
            setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
            JLabel title = new JLabel("Цветовые модели");
            title.setFont(title.getFont().deriveFont(Font.BOLD, 25f));
            add(title, BorderLayout.NORTH);
            JPanel columns = new JPanel(new GridLayout(1, 3, 14, 0));
            ColorMath.Model[] models = {
                ColorMath.Model.CMYK, ColorMath.Model.RGB, ColorMath.Model.HLS
            };
            for (int i = 0; i < models.length; i++) {
                editors[i] = new Editor(models[i], state);
                columns.add(editors[i]);
            }
            add(columns, BorderLayout.CENTER);
            JPanel bottom = new JPanel(new BorderLayout(14, 0));
            JPanel swatch = new JPanel();
            swatch.setPreferredSize(new Dimension(140, 65));
            JLabel value = new JLabel();
            Runnable refresh = () -> {
                Color c = awt(state.get(ColorMath.Model.RGB));
                swatch.setBackground(c);
                value.setText(String.format("Цвет: #%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue()));
            };
            state.addListener(refresh);
            refresh.run();
            bottom.add(swatch, BorderLayout.WEST);
            bottom.add(value, BorderLayout.CENTER);
            add(bottom, BorderLayout.SOUTH);
        }
    }

    static class Editor extends JPanel {
        final ColorMath.Model model;
        final ColorState state;
        final JTextField[] fields;
        final JSlider[] sliders;
        final Palette palette;
        boolean updating;
        JTextField activeInput;

        Editor(ColorMath.Model model, ColorState state) {
            super(new BorderLayout(8, 12));
            this.model = model;
            this.state = state;
            setBorder(BorderFactory.createTitledBorder(model.name()));
            JPanel top = new JPanel(new BorderLayout(0, 8));
            JPanel rows = new JPanel(new GridLayout(4, 1, 0, 8));
            fields = new JTextField[model.labels.length];
            sliders = new JSlider[fields.length];
            for (int i = 0; i < fields.length; i++) {
                final int index = i;
                JPanel row = new JPanel(new BorderLayout(6, 0));
                String unit = "%";
                if (model == ColorMath.Model.RGB) {
                    unit = "";
                } else if (model == ColorMath.Model.HLS && i == 0) {
                    unit = "°";
                }
                JLabel label = new JLabel(model.labels[i]+unit);
                label.setPreferredSize(new Dimension(30, 24));
                row.add(label, BorderLayout.WEST);
                sliders[i] = new JSlider(0, 10000);
                row.add(sliders[i], BorderLayout.CENTER);
                fields[i] = new JTextField(7);
                fields[i].setHorizontalAlignment(JTextField.RIGHT);
                fields[i].setToolTipText("0 … "+model.maxima[i]+"; дробные значения допустимы");
                row.add(fields[i], BorderLayout.EAST);
                rows.add(row);
                sliders[i].addChangeListener(e -> {
                    if (updating) return;
                    double[] v = state.get(model);
                    double value = sliders[index].getValue() / 10000.0 * model.maxima[index];
                    v[index] = internal(index, value);
                    state.set(model, v);
                });
                fields[i].getDocument().addDocumentListener(new DocumentListener() {
                    public void insertUpdate(DocumentEvent e) {
                        changed();
                    }
                    public void removeUpdate(DocumentEvent e) {
                        changed();
                    }
                    public void changedUpdate(DocumentEvent e) {
                        changed();
                    }
                    void changed() {
                        if (!updating) readField(index);
                    }
                });
                fields[i].addActionListener(e -> finishField(index));
                fields[i].addFocusListener(new FocusAdapter() {
                    public void focusLost(FocusEvent e) {
                        finishField(index);
                    }
                });
            }
            top.add(rows, BorderLayout.CENTER);
            JLabel help = new JLabel("Введите число или передвиньте ползунок.");
            help.setFont(help.getFont().deriveFont(12f));
            top.add(help, BorderLayout.SOUTH);
            add(top, BorderLayout.NORTH);
            palette = new Palette(this);
            add(palette, BorderLayout.CENTER);
            state.addListener(this::refresh);
            refresh();
        }

        double internal(int i, double display) {
            if (model == ColorMath.Model.HLS && i == 0) return display;
            return display / model.maxima[i];
        }

        double display(int i, double internal) {
            if (model == ColorMath.Model.HLS && i == 0) return internal;
            return internal * model.maxima[i];
        }

        void readField(int i) {
            try {
                double n = Double.parseDouble(fields[i].getText().trim().replace(',', '.'));
                if (!Double.isFinite(n) || n < 0 || n > model.maxima[i]) throw new NumberFormatException();
                fields[i].setBackground(Color.WHITE);
                double[] v = state.get(model);
                v[i] = internal(i, n);
                // это поле сейчас редактируется, его текст не обновляем
                activeInput = fields[i];
                try {
                    state.set(model, v);
                } finally {
                    activeInput = null;
                }
            } catch (NumberFormatException ex) {
                fields[i].setBackground(new Color(255, 222, 222));
            }
        }

        void finishField(int i) {
            readField(i);
            refresh();
        }

        void refresh() {
            // при обновлении интерфейса пропускаем события от полей и ползунков
            updating = true;
            try {
                double[] v = state.get(model);
                for (int i = 0; i < v.length; i++) {
                    double d = display(i, v[i]);
                    if (fields[i] != activeInput) {
                        fields[i].setText(String.format(Locale.ROOT, "%.4f", d).replaceAll("0+$", "").replaceAll("\\.$", ""));
                        fields[i].setBackground(Color.WHITE);
                    }
                    sliders[i].setValue((int)Math.round(d/model.maxima[i]*10000));
                }
                if (palette != null) palette.canvas.repaint();
            } finally {
                updating = false;
            }
        }
    }

    static class Palette extends JPanel {
        final Editor editor;
        final JComboBox<String> xAxis, yAxis;
        final JPanel canvas;

        Palette(Editor editor) {
            super(new BorderLayout(0, 8));
            this.editor = editor;
            JPanel controls = new JPanel(new GridLayout(2, 2, 6, 4));
            xAxis = new JComboBox<>(editor.model.labels);
            yAxis = new JComboBox<>(editor.model.labels);
            xAxis.setSelectedIndex(0);
            yAxis.setSelectedIndex(1);
            controls.add(new JLabel("Горизонталь:"));
            controls.add(xAxis);
            controls.add(new JLabel("Вертикаль:"));
            controls.add(yAxis);
            add(controls, BorderLayout.NORTH);
            canvas = new JPanel() {
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    int w = getWidth(), h = getHeight();
                    if (w < 2 || h < 2) return;
                    int rw = Math.min(w, 160), rh = Math.min(h, 180);
                    BufferedImage image = new BufferedImage(rw, rh, BufferedImage.TYPE_INT_RGB);
                    double[] v = editor.state.get(editor.model);
                    for (int y = 0; y < rh; y++) for (int x = 0; x < rw; x++) {
                        double[] p = point(v, x/(double)(rw-1), 1-y/(double)(rh-1));
                        image.setRGB(x, y, awt(ColorMath.toRgb(editor.model, p)).getRGB());
                    }
                    g.drawImage(image, 0, 0, w, h, null);
                    int xi = xAxis.getSelectedIndex(), yi = yAxis.getSelectedIndex();
                    int px = (int)(editor.display(xi, v[xi])/editor.model.maxima[xi]*(w-1));
                    int py = (int)((1-editor.display(yi, v[yi])/editor.model.maxima[yi])*(h-1));
                    g.setColor(Color.BLACK);
                    g.drawOval(px-6, py-6, 12, 12);
                    g.setColor(Color.WHITE);
                    g.drawOval(px-5, py-5, 10, 10);
                }
            };
            canvas.setPreferredSize(new Dimension(300, 330));
            MouseAdapter mouse = new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    choose(e);
                }
                public void mouseDragged(MouseEvent e) {
                    choose(e);
                }
                void choose(MouseEvent e) {
                    double x = Math.max(0, Math.min(1, e.getX()/(double)Math.max(1, canvas.getWidth()-1)));
                    double y = 1-Math.max(0, Math.min(1, e.getY()/(double)Math.max(1, canvas.getHeight()-1)));
                    editor.state.set(editor.model, point(editor.state.get(editor.model), x, y));
                }
            };
            canvas.addMouseListener(mouse);
            canvas.addMouseMotionListener(mouse);
            add(canvas, BorderLayout.CENTER);
            xAxis.addActionListener(e -> {
                if (xAxis.getSelectedIndex() == yAxis.getSelectedIndex()) yAxis.setSelectedIndex((xAxis.getSelectedIndex()+1)%editor.fields.length);
                canvas.repaint();
            });
            yAxis.addActionListener(e -> {
                if (yAxis.getSelectedIndex() == xAxis.getSelectedIndex()) xAxis.setSelectedIndex((yAxis.getSelectedIndex()+1)%editor.fields.length);
                canvas.repaint();
            });
        }

        double[] point(double[] source, double x, double y) {
            double[] v = source.clone();
            int xi = xAxis.getSelectedIndex(), yi = yAxis.getSelectedIndex();
            v[xi] = editor.internal(xi, x*editor.model.maxima[xi]);
            v[yi] = editor.internal(yi, y*editor.model.maxima[yi]);
            return v;
        }
    }
}
