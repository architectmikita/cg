import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;

public class ImageLab extends JPanel {
    BufferedImage original, result;
    final ImageView before = new ImageView(), after = new ImageView();
    final HistogramPanel beforeHistogram = new HistogramPanel(), afterHistogram = new HistogramPanel();
    final JComboBox<String> methods = new JComboBox<>(ImageMethods.METHODS);
    final JComboBox<String> channels = new JComboBox<>(new String[]{"Яркость (серое)", "R", "G", "B", "V (HSV)"});
    final JComboBox<String> zoom = new JComboBox<>(new String[]{"Вписать", "100%", "200%"});
    final JSpinner strength = new JSpinner(new SpinnerNumberModel(1.0, 0.0, 3.0, 0.1));
    final JButton open = new JButton("Открыть"), save = new JButton("Сохранить результат");
    final JButton apply = new JButton("Применить"), reset = new JButton("Сбросить");
    final JButton example = new JButton("Загрузить пример");
    final JComboBox<String> examples = new JComboBox<>();
    final JLabel status = new JLabel("Откройте изображение или выберите пример.");
    final JLabel info = new JLabel();
    final JLabel beforeTitle = new JLabel("Исходное", SwingConstants.CENTER);
    final File examplesFolder = findExamples();
    boolean busy;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Лабораторная 2. Обработка изображений - вариант 3");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(new ImageLab());
            frame.setSize(1200, 850);
            frame.setMinimumSize(new Dimension(1050, 720));
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    ImageLab() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JPanel controls = new JPanel(new GridLayout(3, 1, 0, 4));
        JPanel files = new JPanel(new FlowLayout(FlowLayout.LEFT));
        files.add(open);
        files.add(examples);
        files.add(example);
        files.add(save);
        File[] sampleFiles = examplesFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));
        if (sampleFiles != null) {
            Arrays.sort(sampleFiles);
            for (File file : sampleFiles) examples.addItem(file.getName());
        }
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actions.add(new JLabel("Метод:"));
        actions.add(methods);
        actions.add(new JLabel("Сила резкости:"));
        actions.add(strength);
        actions.add(apply);
        actions.add(reset);
        JPanel view = new JPanel(new FlowLayout(FlowLayout.LEFT));
        view.add(new JLabel("Гистограмма:"));
        view.add(channels);
        view.add(new JLabel("Масштаб:"));
        view.add(zoom);
        view.add(new JLabel("Обработка всегда от исходника"));
        controls.add(files);
        controls.add(actions);
        controls.add(view);
        add(controls, BorderLayout.NORTH);
        JPanel pictures = new JPanel(new GridLayout(1, 2, 10, 0));
        pictures.add(imageColumn(beforeTitle, before, beforeHistogram));
        pictures.add(imageColumn(new JLabel("Результат", SwingConstants.CENTER), after, afterHistogram));
        add(pictures, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new GridLayout(2, 1, 0, 4));
        bottom.add(info);
        bottom.add(status);
        add(bottom, BorderLayout.SOUTH);

        open.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Изображения PNG, JPEG, BMP, GIF", "png", "jpg", "jpeg", "bmp", "gif"));
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) load(chooser.getSelectedFile());
        });
        example.addActionListener(e -> {
            if (examples.getSelectedItem() != null) load(new File(examplesFolder, examples.getSelectedItem().toString()));
        });
        apply.addActionListener(e -> applyMethod());
        reset.addActionListener(e -> {
            result = original;
            before.setImage(original);
            beforeTitle.setText("Исходное");
            after.setImage(result);
            updateHistograms();
            status.setText("Результат сброшен.");
        });
        methods.addActionListener(e -> { updateInfo(); updateButtons(); });
        channels.addActionListener(e -> updateHistograms());
        zoom.addActionListener(e -> {
            before.setZoom(zoom.getSelectedIndex());
            after.setZoom(zoom.getSelectedIndex());
        });
        save.addActionListener(e -> saveResult());
        methods.setSelectedIndex(7);
        updateInfo();
        updateButtons();
    }

    static File findExamples() {
        File current = new File("images");
        if (current.isDirectory()) return current;
        try {
            current = new File(ImageLab.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            for (int i = 0; i < 5 && current != null; i++, current = current.getParentFile()) {
                File folder = new File(current, "images");
                if (folder.isDirectory()) return folder;
            }
        } catch (Exception ignored) {
        }
        return new File("images");
    }

    JPanel imageColumn(JLabel title, ImageView image, HistogramPanel histogram) {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.add(title, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(image);
        scroll.getViewport().setBackground(new Color(225, 225, 225));
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(histogram, BorderLayout.SOUTH);
        return panel;
    }

    void updateButtons() {
        open.setEnabled(!busy);
        examples.setEnabled(!busy && examples.getItemCount() > 0);
        example.setEnabled(!busy && examples.getItemCount() > 0);
        methods.setEnabled(!busy);
        strength.setEnabled(!busy && (methods.getSelectedIndex() == 5 || methods.getSelectedIndex() == 6));
        apply.setEnabled(!busy && original != null);
        reset.setEnabled(!busy && original != null);
        save.setEnabled(!busy && result != null);
    }

    void updateInfo() {
        String[] descriptions = {
            "Серое: 0.299R + 0.587G + 0.114B.",
            "Растяжение диапазона: 255 × (f − min) / (max − min). Однотонное изображение не меняется.",
            "Серое: новая яркость = 255 × накопленная нормализованная гистограмма.",
            "Три отдельные таблицы для R, G, B. Соотношение каналов и оттенки могут измениться.",
            "RGB → HSV → эквализация V → RGB. Тон H и насыщенность S сохраняются до округления.",
            "При силе 1: [0, −1, 0; −1, 5, −1; 0, −1, 0]. Границы: повтор крайнего пикселя.",
            "При силе 1: [−1, −1, −1; −1, 9, −1; −1, −1, −1]. Фильтр усиливает и детали, и шум.",
            "Растяжение V в HSV на 0–255. H и S не меняются; при V = 0 получается чёрный."
        };
        info.setText(descriptions[methods.getSelectedIndex()]);
    }

    void load(File file) {
        busy = true;
        updateButtons();
        status.setText("Загрузка: " + file.getName());
        new SwingWorker<BufferedImage, Void>() {
            protected BufferedImage doInBackground() throws Exception {
                BufferedImage loaded = ImageIO.read(file);
                if (loaded == null) throw new Exception("Формат изображения не поддерживается");
                BufferedImage rgb = new BufferedImage(loaded.getWidth(), loaded.getHeight(), BufferedImage.TYPE_INT_RGB);
                Graphics2D g = rgb.createGraphics();
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
                g.drawImage(loaded, 0, 0, null);
                g.dispose();
                return rgb;
            }
            protected void done() {
                try {
                    original = get();
                    result = original;
                    before.setImage(original);
                    beforeTitle.setText("Исходное");
                    after.setImage(result);
                    updateHistograms();
                    status.setText(file.getName() + " | " + original.getWidth() + " × " + original.getHeight());
                } catch (Exception ex) {
                    showError(ex);
                } finally {
                    busy = false;
                    updateButtons();
                }
            }
        }.execute();
    }

    void applyMethod() {
        if (original == null || busy) return;
        try { strength.commitEdit(); }
        catch (java.text.ParseException ex) {
            JOptionPane.showMessageDialog(this, "Введите силу от 0 до 3.");
            return;
        }
        int method = methods.getSelectedIndex();
        double amount = ((Number) strength.getValue()).doubleValue();
        busy = true;
        updateButtons();
        status.setText("Обработка...");
        new SwingWorker<BufferedImage[], Void>() {
            protected BufferedImage[] doInBackground() {
                BufferedImage reference = method <= 2 ? ImageMethods.process(original, 0, 1) : original;
                return new BufferedImage[]{reference, ImageMethods.process(original, method, amount)};
            }
            protected void done() {
                try {
                    BufferedImage[] pair = get();
                    before.setImage(pair[0]);
                    beforeTitle.setText(method <= 2 ? "Исходное в оттенках серого" : "Исходное");
                    result = pair[1];
                    after.setImage(result);
                    channels.setSelectedIndex((method == 4 || method == 7) ? 4 : method == 3 ? 1 : 0);
                    updateHistograms();
                    status.setText(ImageMethods.METHODS[method] + ((method == 5 || method == 6) ? " | сила: " + amount : "") + " | " + original.getWidth() + " × " + original.getHeight());
                } catch (Exception ex) {
                    showError(ex);
                } finally {
                    busy = false;
                    updateButtons();
                }
            }
        }.execute();
    }

    void updateHistograms() {
        if (before.image == null || result == null) return;
        int channel = channels.getSelectedIndex();
        long[] first = ImageMethods.histogram(before.image, channel);
        long[] second = ImageMethods.histogram(result, channel);
        long max = 1;
        for (int i = 0; i < 256; i++) max = Math.max(max, Math.max(first[i], second[i]));
        Color[] colors = {Color.DARK_GRAY, new Color(190, 50, 50), new Color(40, 140, 60), new Color(50, 80, 190), Color.DARK_GRAY};
        beforeHistogram.setHistogram(first, max, colors[channel]);
        afterHistogram.setHistogram(second, max, colors[channel]);
    }

    void saveResult() {
        if (result == null) return;
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("result.png"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".png")) file = new File(file.getPath() + ".png");
        if (file.exists() && JOptionPane.showConfirmDialog(this, "Заменить существующий файл?", "Сохранение", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        try {
            if (!ImageIO.write(result, "png", file)) throw new Exception("Не удалось сохранить PNG");
            status.setText("Сохранено: " + file.getAbsolutePath());
        } catch (Exception ex) { showError(ex); }
    }

    void showError(Exception ex) {
        Throwable cause = ex.getCause() == null ? ex : ex.getCause();
        status.setText("Ошибка: " + cause.getMessage());
        JOptionPane.showMessageDialog(this, cause.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
    }

    static class ImageView extends JPanel implements Scrollable {
        BufferedImage image;
        int zoom;
        ImageView() { setBackground(new Color(225, 225, 225)); }
        void setImage(BufferedImage image) { this.image = image; revalidate(); repaint(); }
        void setZoom(int zoom) { this.zoom = zoom; revalidate(); repaint(); }
        public Dimension getPreferredSize() {
            if (image == null || zoom == 0) return new Dimension(450, 380);
            return new Dimension(image.getWidth() * zoom, image.getHeight() * zoom);
        }
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (image == null) return;
            double scale = zoom == 0 ? Math.min(getWidth() / (double) image.getWidth(), getHeight() / (double) image.getHeight()) : zoom;
            int w = Math.max(1, (int) (image.getWidth() * scale));
            int h = Math.max(1, (int) (image.getHeight() * scale));
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, zoom == 0 ? RenderingHints.VALUE_INTERPOLATION_BILINEAR : RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(image, Math.max(0, (getWidth() - w) / 2), Math.max(0, (getHeight() - h) / 2), w, h, null);
            g.dispose();
        }
        public Dimension getPreferredScrollableViewportSize() { return new Dimension(450, 380); }
        public int getScrollableUnitIncrement(Rectangle r, int orientation, int direction) { return 20; }
        public int getScrollableBlockIncrement(Rectangle r, int orientation, int direction) { return orientation == SwingConstants.HORIZONTAL ? r.width : r.height; }
        public boolean getScrollableTracksViewportWidth() { return zoom == 0; }
        public boolean getScrollableTracksViewportHeight() { return zoom == 0; }
    }
}
