package com.mazemind;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicComboBoxUI;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public final class MazeMindApp {
    private static final Color INK = new Color(234, 240, 248);
    private static final Color MUTED = new Color(157, 168, 184);
    private static final Color APP_BG = new Color(10, 14, 22);
    private static final Color PANEL = new Color(17, 24, 35);
    private static final Color SURFACE = new Color(24, 33, 47);
    private static final Color SURFACE_ALT = new Color(31, 42, 59);
    private static final Color LINE = new Color(61, 74, 93);
    private static final Color ACCENT = new Color(45, 212, 191);
    private static final Color GOAL = new Color(168, 116, 255);
    private static final long MAX_UPLOAD_BYTES = 15L * 1024L * 1024L;
    private static final long MAX_UPLOAD_PIXELS = 8_000_000L;

    private final DefaultComboBoxModel<MazeAsset> mazeModel = new DefaultComboBoxModel<>();
    private final DefaultComboBoxModel<CharacterAsset> characterModel = new DefaultComboBoxModel<>();

    private MazePanel mazePanel;
    private JComboBox<MazeAsset> mazeBox;
    private JComboBox<CharacterAsset> characterBox;
    private JSlider sizeSlider;
    private JTextArea statusArea;
    private JToggleButton setStartButton;
    private JToggleButton setGoalButton;
    private JButton autoSolveButton;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            setLookAndFeel();
            new MazeMindApp().show();
        });
    }

    private static void setLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ignored) {
            // The default Swing look and feel is fine if the system theme is unavailable.
        }
        UIManager.put("ToolTip.background", SURFACE_ALT);
        UIManager.put("ToolTip.foreground", INK);
        UIManager.put("ToolTip.border", BorderFactory.createLineBorder(LINE));
    }

    private void show() {
        loadDefaults();

        JFrame frame = new JFrame("MazeMind");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(960, 640));

        mazePanel = new MazePanel(this::setStatus, this::finishToolAction, this::updateAutoSolveButton);
        mazePanel.setMaze((MazeAsset) mazeModel.getSelectedItem());
        mazePanel.setCharacter((CharacterAsset) characterModel.getSelectedItem());

        frame.add(createTopBar(), BorderLayout.NORTH);
        frame.add(mazePanel, BorderLayout.CENTER);
        frame.add(createSidePanel(frame), BorderLayout.EAST);
        bindMovementKeys(frame.getRootPane());

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        mazePanel.requestFocusInWindow();
        setStatus("Ready.");
    }

    private JPanel createTopBar() {
        JPanel topBar = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setPaint(new GradientPaint(0, 0, new Color(11, 18, 32), getWidth(), getHeight(), new Color(15, 86, 91)));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.dispose();
                super.paintComponent(graphics);
            }
        };
        topBar.setOpaque(false);
        topBar.setBorder(new EmptyBorder(16, 20, 14, 20));

        JLabel title = new JLabel("MazeMind");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        title.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel("Manual play and auto solving");
        subtitle.setFont(subtitle.getFont().deriveFont(Font.PLAIN, 13f));
        subtitle.setForeground(new Color(191, 224, 221));
        subtitle.setHorizontalAlignment(SwingConstants.RIGHT);

        topBar.add(title, BorderLayout.WEST);
        topBar.add(subtitle, BorderLayout.EAST);
        return topBar;
    }

    private JPanel createSidePanel(JFrame frame) {
        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setPreferredSize(new Dimension(340, 640));
        side.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 1, 0, 0, LINE),
                new EmptyBorder(16, 16, 16, 16)
        ));
        side.setBackground(PANEL);

        mazeBox = new JComboBox<>(mazeModel);
        mazeBox.addActionListener(event -> {
            MazeAsset selected = (MazeAsset) mazeBox.getSelectedItem();
            if (selected != null) {
                mazePanel.setMaze(selected);
                setToolMode(ToolMode.NONE);
                setStatus("Maze loaded: " + selected.name + ".");
            }
        });
        styleComboBox(mazeBox);
        stretch(mazeBox);

        JButton uploadMaze = new JButton("Add Maze");
        styleButton(uploadMaze, false);
        uploadMaze.addActionListener(event -> chooseMaze(frame));

        characterBox = new JComboBox<>(characterModel);
        characterBox.addActionListener(event -> {
            CharacterAsset selected = (CharacterAsset) characterBox.getSelectedItem();
            if (selected != null) {
                mazePanel.setCharacter(selected);
                setStatus("Character loaded: " + selected.name + ".");
            }
        });
        styleComboBox(characterBox);
        stretch(characterBox);

        JButton uploadCharacter = new JButton("Add Character");
        styleButton(uploadCharacter, false);
        uploadCharacter.addActionListener(event -> chooseCharacter(frame));

        sizeSlider = new JSlider(16, 54, 28);
        sizeSlider.setMajorTickSpacing(19);
        sizeSlider.setPaintTicks(true);
        styleSlider(sizeSlider);
        sizeSlider.addChangeListener(event -> {
            mazePanel.setAvatarSize(sizeSlider.getValue());
            if (!sizeSlider.getValueIsAdjusting()) {
                setStatus("Character size set to " + sizeSlider.getValue() + ".");
            }
        });

        setStartButton = new JToggleButton("Set Start");
        styleButton(setStartButton, false);
        setStartButton.setToolTipText("Click a walkable tile to move the starting point.");
        setStartButton.addActionListener(event -> setToolMode(setStartButton.isSelected() ? ToolMode.SET_START : ToolMode.NONE));

        setGoalButton = new JToggleButton("Set Finish");
        styleButton(setGoalButton, false);
        setGoalButton.setToolTipText("Click a walkable tile to move the finish point.");
        setGoalButton.addActionListener(event -> setToolMode(setGoalButton.isSelected() ? ToolMode.SET_GOAL : ToolMode.NONE));

        autoSolveButton = new JButton("Auto Solve");
        styleButton(autoSolveButton, true);
        autoSolveButton.addActionListener(event -> {
            if (mazePanel.isAutoSolving()) {
                mazePanel.stopAutoSolve("Auto solver stopped.");
            } else {
                setToolMode(ToolMode.NONE);
                mazePanel.startAutoSolve();
            }
            updateAutoSolveButton();
        });

        JButton resetButton = new JButton("Reset");
        styleButton(resetButton, false);
        resetButton.addActionListener(event -> {
            mazePanel.resetPlayer();
            setToolMode(ToolMode.NONE);
            setStatus("Reset to start.");
        });

        statusArea = new JTextArea(3, 20);
        statusArea.setEditable(false);
        statusArea.setLineWrap(true);
        statusArea.setWrapStyleWord(true);
        statusArea.setOpaque(false);
        statusArea.setForeground(MUTED);
        statusArea.setFont(statusArea.getFont().deriveFont(13f));

        side.add(sectionLabel("Maze"));
        side.add(mazeBox);
        side.add(Box.createVerticalStrut(8));
        side.add(stretch(uploadMaze));
        side.add(Box.createVerticalStrut(18));

        side.add(sectionLabel("Character"));
        side.add(characterBox);
        side.add(Box.createVerticalStrut(8));
        side.add(stretch(uploadCharacter));
        side.add(Box.createVerticalStrut(18));

        side.add(sectionLabel("Tools"));
        side.add(stretch(autoSolveButton));
        side.add(Box.createVerticalStrut(8));
        side.add(createToolGrid(setStartButton, setGoalButton, resetButton));
        side.add(Box.createVerticalStrut(16));

        side.add(sectionLabel("Character Size"));
        side.add(sizeSlider);
        side.add(Box.createVerticalStrut(18));

        side.add(sectionLabel("Move"));
        side.add(createMovePad());
        side.add(Box.createVerticalGlue());
        side.add(createStatusPanel());

        updateAutoSolveButton();
        return side;
    }

    private JLabel sectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 12f));
        label.setForeground(new Color(196, 205, 218));
        label.setBorder(new EmptyBorder(0, 0, 6, 0));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JComponent stretch(JComponent component) {
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, Math.max(32, component.getPreferredSize().height)));
        return component;
    }

    private void styleButton(AbstractButton button, boolean primary) {
        Color background = primary ? ACCENT : Color.WHITE;
        Color foreground = primary ? new Color(6, 24, 30) : INK;
        Color border = primary ? new Color(35, 172, 157) : LINE;
        if (!primary) {
            background = SURFACE_ALT;
        }

        button.setFont(button.getFont().deriveFont(Font.BOLD, 12f));
        button.setUI(new BasicButtonUI());
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(true);
        button.setBackground(background);
        button.setForeground(foreground);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border),
                new EmptyBorder(8, 10, 8, 10)
        ));
    }

    private void styleComboBox(JComboBox<?> comboBox) {
        comboBox.setUI(new BasicComboBoxUI());
        comboBox.setBackground(SURFACE);
        comboBox.setForeground(INK);
        comboBox.setOpaque(true);
        comboBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE),
                new EmptyBorder(4, 6, 4, 6)
        ));
        comboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focused) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focused);
                label.setBackground(selected ? SURFACE_ALT : SURFACE);
                label.setForeground(INK);
                label.setBorder(new EmptyBorder(4, 8, 4, 8));
                return label;
            }
        });
    }

    private void styleSlider(JSlider slider) {
        slider.setOpaque(false);
        slider.setForeground(MUTED);
        slider.setBackground(PANEL);
    }

    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 78));
        panel.setBackground(SURFACE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE),
                new EmptyBorder(10, 10, 10, 10)
        ));
        panel.add(statusArea, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createToolGrid(JComponent one, JComponent two, JComponent three) {
        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 84));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.insets = new Insets(0, 0, 8, 8);
        gbc.gridx = 0;
        gbc.gridy = 0;
        grid.add(one, gbc);
        gbc.gridx = 1;
        gbc.insets = new Insets(0, 0, 8, 0);
        grid.add(two, gbc);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(0, 0, 0, 0);
        grid.add(three, gbc);
        return grid;
    }

    private JPanel createMovePad() {
        JPanel pad = new JPanel(new GridBagLayout());
        pad.setOpaque(false);
        pad.setAlignmentX(Component.LEFT_ALIGNMENT);
        pad.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
        JButton up = holdButton("Up", Direction.UP);
        JButton down = holdButton("Down", Direction.DOWN);
        JButton left = holdButton("Left", Direction.LEFT);
        JButton right = holdButton("Right", Direction.RIGHT);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.insets = new Insets(0, 48, 6, 48);
        gbc.gridx = 1;
        gbc.gridy = 0;
        pad.add(up, gbc);

        gbc.insets = new Insets(0, 0, 0, 6);
        gbc.gridx = 0;
        gbc.gridy = 1;
        pad.add(left, gbc);

        gbc.insets = new Insets(0, 0, 0, 6);
        gbc.gridx = 1;
        gbc.gridy = 1;
        pad.add(down, gbc);

        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.gridx = 2;
        gbc.gridy = 1;
        pad.add(right, gbc);
        return pad;
    }

    private JButton holdButton(String label, Direction direction) {
        JButton button = new JButton(label);
        styleButton(button, false);
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                mazePanel.setVirtualDirection(direction, true);
                mazePanel.requestFocusInWindow();
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                mazePanel.setVirtualDirection(direction, false);
            }

            @Override
            public void mouseExited(MouseEvent event) {
                mazePanel.setVirtualDirection(direction, false);
            }
        });
        return button;
    }

    private void bindMovementKeys(JComponent root) {
        bindKey(root, KeyEvent.VK_W, Direction.UP);
        bindKey(root, KeyEvent.VK_UP, Direction.UP);
        bindKey(root, KeyEvent.VK_S, Direction.DOWN);
        bindKey(root, KeyEvent.VK_DOWN, Direction.DOWN);
        bindKey(root, KeyEvent.VK_A, Direction.LEFT);
        bindKey(root, KeyEvent.VK_LEFT, Direction.LEFT);
        bindKey(root, KeyEvent.VK_D, Direction.RIGHT);
        bindKey(root, KeyEvent.VK_RIGHT, Direction.RIGHT);
    }

    private void bindKey(JComponent root, int keyCode, Direction direction) {
        String pressName = "pressed-" + keyCode;
        String releaseName = "released-" + keyCode;
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(keyCode, 0, false), pressName);
        root.getActionMap().put(pressName, new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                mazePanel.setVirtualDirection(direction, true);
            }
        });
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(keyCode, 0, true), releaseName);
        root.getActionMap().put(releaseName, new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                mazePanel.setVirtualDirection(direction, false);
            }
        });
    }

    private void chooseMaze(JFrame frame) {
        File file = chooseImage(frame, "Add Maze Image");
        if (file == null) {
            return;
        }

        try {
            BufferedImage image = readUserImage(file);

            Color wallColor = MazeTools.guessWallColor(image);
            int tolerance = MazeTools.DEFAULT_WALL_TOLERANCE;
            CollisionMask mask = new CollisionMask(image, wallColor, tolerance);
            double avatar = sizeSlider.getValue();
            Point2D.Double start = MazeTools.findOpenPoint(mask, image.getWidth() * 0.12, image.getHeight() * 0.12, avatar);
            Point2D.Double goal = MazeTools.findOpenPoint(mask, image.getWidth() * 0.88, image.getHeight() * 0.88, avatar);
            MazeAsset asset = new MazeAsset(cleanName(file), image, start, goal, wallColor, tolerance);
            mazeModel.addElement(asset);
            mazeBox.setSelectedItem(asset);
            setStatus("Added maze: " + asset.name + ".");
        } catch (UserImageException ex) {
            showError(frame, ex.getMessage());
        } catch (IOException ex) {
            showError(frame, "Could not load the maze image.");
        }
    }

    private void chooseCharacter(JFrame frame) {
        File file = chooseImage(frame, "Add Character Image");
        if (file == null) {
            return;
        }

        try {
            BufferedImage image = readUserImage(file);
            CharacterAsset asset = new CharacterAsset(cleanName(file), image);
            characterModel.addElement(asset);
            characterBox.setSelectedItem(asset);
            setStatus("Added character: " + asset.name + ".");
        } catch (UserImageException ex) {
            showError(frame, ex.getMessage());
        } catch (IOException ex) {
            showError(frame, "Could not load the character image.");
        }
    }

    private File chooseImage(JFrame frame, String title) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(title);
        chooser.setFileFilter(new FileNameExtensionFilter("Image files", "png", "jpg", "jpeg", "gif", "bmp"));
        int result = chooser.showOpenDialog(frame);
        return result == JFileChooser.APPROVE_OPTION ? chooser.getSelectedFile() : null;
    }

    private String cleanName(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        String baseName = dot > 0 ? name.substring(0, dot) : name;
        baseName = baseName.replaceAll("[\\p{Cntrl}<>]", "_").trim();
        if (baseName.isEmpty()) {
            return "Untitled";
        }
        return baseName.length() > 48 ? baseName.substring(0, 48) : baseName;
    }

    static BufferedImage readUserImage(File file) throws IOException, UserImageException {
        if (!file.isFile()) {
            throw new UserImageException("Choose a valid image file.");
        }
        if (file.length() > MAX_UPLOAD_BYTES) {
            throw new UserImageException("Image is too large. Use a file under 15 MB.");
        }

        try (ImageInputStream stream = ImageIO.createImageInputStream(file)) {
            if (stream == null) {
                throw new UserImageException("That file is not a supported image.");
            }

            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) {
                throw new UserImageException("That file is not a supported image.");
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(stream, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || (long) width * height > MAX_UPLOAD_PIXELS) {
                    throw new UserImageException("Image dimensions are too large. Use an image under 8 megapixels.");
                }

                BufferedImage image = reader.read(0);
                if (image == null) {
                    throw new UserImageException("That file is not a supported image.");
                }
                return image;
            } finally {
                reader.dispose();
            }
        }
    }

    private void showError(JFrame frame, String message) {
        JOptionPane.showMessageDialog(frame, message, "MazeMind", JOptionPane.ERROR_MESSAGE);
    }

    private void setToolMode(ToolMode mode) {
        setStartButton.setSelected(mode == ToolMode.SET_START);
        setGoalButton.setSelected(mode == ToolMode.SET_GOAL);
        refreshToolButtonStyles();
        mazePanel.setToolMode(mode);

        if (mode == ToolMode.SET_START) {
            setStatus("Click a start tile.");
        } else if (mode == ToolMode.SET_GOAL) {
            setStatus("Click a finish tile.");
        }
    }

    private void finishToolAction() {
        setToolMode(ToolMode.NONE);
    }

    private void refreshToolButtonStyles() {
        styleToggleState(setStartButton);
        styleToggleState(setGoalButton);
    }

    private void styleToggleState(JToggleButton button) {
        if (button == null) {
            return;
        }
        boolean selected = button.isSelected();
        button.setBackground(selected ? new Color(28, 130, 121) : SURFACE_ALT);
        button.setForeground(selected ? Color.WHITE : INK);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(selected ? ACCENT : LINE),
                new EmptyBorder(8, 10, 8, 10)
        ));
    }

    private void updateAutoSolveButton() {
        if (autoSolveButton == null || mazePanel == null) {
            return;
        }
        boolean running = mazePanel.isAutoSolving();
        autoSolveButton.setText(running ? "Stop Auto" : "Auto Solve");
        autoSolveButton.setBackground(running ? new Color(177, 68, 68) : ACCENT);
        autoSolveButton.setForeground(running ? Color.WHITE : new Color(6, 24, 30));
        autoSolveButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(running ? new Color(137, 42, 42) : new Color(22, 107, 89)),
                new EmptyBorder(8, 10, 8, 10)
        ));
    }

    private void setStatus(String text) {
        if (statusArea != null) {
            statusArea.setText(text);
        }
    }

    private void loadDefaults() {
        BufferedImage mazeTwo = readResource("/assets/maze2.png");
        BufferedImage mazeOne = readResource("/assets/maze.png");
        BufferedImage robot = readResource("/assets/robot.png");
        BufferedImage car = readResource("/assets/car.png");

        mazeModel.addElement(createMazeAsset("Starter maze", mazeTwo, 34, 34, 446, 329));
        mazeModel.addElement(createMazeAsset("Classic maze", mazeOne, 30, 271, 586, 255));

        characterModel.addElement(new CharacterAsset("Robot", robot));
        characterModel.addElement(new CharacterAsset("Car", car));
    }

    private MazeAsset createMazeAsset(String name, BufferedImage image, double startX, double startY, double goalX, double goalY) {
        Color wallColor = MazeTools.guessWallColor(image);
        int tolerance = MazeTools.DEFAULT_WALL_TOLERANCE;
        CollisionMask mask = new CollisionMask(image, wallColor, tolerance);
        Point2D.Double start = MazeTools.findOpenPoint(mask, startX, startY, 28);
        Point2D.Double goal = MazeTools.findOpenPoint(mask, goalX, goalY, 28);
        return new MazeAsset(name, image, start, goal, wallColor, tolerance);
    }

    private BufferedImage readResource(String path) {
        try (InputStream stream = MazeMindApp.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("Missing resource: " + path);
            }
            BufferedImage image = ImageIO.read(stream);
            if (image == null) {
                throw new IllegalStateException("Unsupported image: " + path);
            }
            return image;
        } catch (IOException ex) {
            throw new IllegalStateException("Could not read resource: " + path, ex);
        }
    }

    private enum Direction {
        UP, DOWN, LEFT, RIGHT
    }

    private enum ToolMode {
        NONE, SET_START, SET_GOAL
    }

    private static final class MazeAsset {
        final String name;
        final BufferedImage image;
        Point2D.Double start;
        Point2D.Double goal;
        Color wallColor;
        int tolerance;

        MazeAsset(String name, BufferedImage image, Point2D.Double start, Point2D.Double goal, Color wallColor, int tolerance) {
            this.name = name;
            this.image = image;
            this.start = start;
            this.goal = goal;
            this.wallColor = wallColor;
            this.tolerance = tolerance;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static final class CharacterAsset {
        final String name;
        final BufferedImage image;

        CharacterAsset(String name, BufferedImage image) {
            this.name = name;
            this.image = image;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static final class MazePanel extends JPanel {
        private static final double BASE_SPEED = 155.0;
        private static final double AUTO_SPEED = 190.0;
        private static final double MIN_STEP = 2.2;

        private final StatusSink statusSink;
        private final Runnable toolCompleted;
        private final Runnable autoSolveChanged;
        private final EnumSet<Direction> pressed = EnumSet.noneOf(Direction.class);
        private final Timer timer;

        private MazeAsset maze;
        private CharacterAsset character;
        private CollisionMask mask;
        private ToolMode toolMode = ToolMode.NONE;
        private List<Point2D.Double> autoPath = List.of();
        private int autoTargetIndex;
        private boolean autoSolving;
        private Point2D.Double player = new Point2D.Double();
        private double avatarSize = 28;
        private double facingAngle;
        private boolean solved;
        private long lastTick = System.nanoTime();
        private DrawLayout layout = new DrawLayout(0, 0, 1);

        MazePanel(StatusSink statusSink, Runnable toolCompleted, Runnable autoSolveChanged) {
            this.statusSink = statusSink;
            this.toolCompleted = toolCompleted;
            this.autoSolveChanged = autoSolveChanged;
            setBackground(APP_BG);
            setFocusable(true);
            setPreferredSize(new Dimension(880, 680));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent event) {
                    requestFocusInWindow();
                    handleMazeClick(event.getPoint());
                }
            });

            timer = new Timer(16, event -> tick());
            timer.start();
        }

        MazeAsset getMaze() {
            return maze;
        }

        void setMaze(MazeAsset maze) {
            clearAutoSolve();
            this.maze = maze;
            this.player = new Point2D.Double(maze.start.x, maze.start.y);
            this.solved = false;
            this.facingAngle = 0;
            rebuildCollisionMask();
            repaint();
        }

        void setCharacter(CharacterAsset character) {
            this.character = character;
            repaint();
        }

        void setAvatarSize(double avatarSize) {
            clearAutoSolve();
            this.avatarSize = avatarSize;
            if (maze != null && !canStandAt(player.x, player.y)) {
                player = new Point2D.Double(maze.start.x, maze.start.y);
            }
            repaint();
        }

        void setToolMode(ToolMode mode) {
            if (mode != ToolMode.NONE) {
                clearAutoSolve();
            }
            this.toolMode = mode;
            repaint();
        }

        void setVirtualDirection(Direction direction, boolean down) {
            if (down) {
                if (autoSolving) {
                    stopAutoSolve("Auto solver stopped.");
                }
                pressed.add(direction);
            } else {
                pressed.remove(direction);
            }
        }

        void resetPlayer() {
            if (maze == null) {
                return;
            }
            player = new Point2D.Double(maze.start.x, maze.start.y);
            solved = false;
            pressed.clear();
            clearAutoSolve();
            repaint();
        }

        void rebuildCollisionMask() {
            clearAutoSolve();
            if (maze == null) {
                mask = null;
            } else {
                mask = new CollisionMask(maze.image, maze.wallColor, maze.tolerance);
            }
            repaint();
        }

        boolean isAutoSolving() {
            return autoSolving;
        }

        boolean startAutoSolve() {
            if (maze == null || mask == null) {
                statusSink.setStatus("Load a maze first.");
                return false;
            }

            List<Point2D.Double> path = MazeTools.solvePath(mask, player, maze.goal, collisionRadius());
            if (path.size() < 2) {
                statusSink.setStatus("No route found. Try moving start or finish.");
                clearAutoSolve();
                return false;
            }

            autoPath = new ArrayList<>();
            autoPath.add(new Point2D.Double(player.x, player.y));
            autoPath.addAll(path);
            autoTargetIndex = 1;
            autoSolving = true;
            solved = false;
            pressed.clear();
            statusSink.setStatus("Auto solver running.");
            autoSolveChanged.run();
            repaint();
            return true;
        }

        void stopAutoSolve(String status) {
            boolean changed = autoSolving || !autoPath.isEmpty();
            autoSolving = false;
            autoPath = List.of();
            autoTargetIndex = 0;
            if (status != null) {
                statusSink.setStatus(status);
            }
            if (changed) {
                autoSolveChanged.run();
                repaint();
            }
        }

        private void clearAutoSolve() {
            boolean changed = autoSolving || !autoPath.isEmpty();
            autoSolving = false;
            autoPath = List.of();
            autoTargetIndex = 0;
            if (changed) {
                autoSolveChanged.run();
            }
        }

        private void tick() {
            long now = System.nanoTime();
            double dt = Math.min(0.04, (now - lastTick) / 1_000_000_000.0);
            lastTick = now;

            if (maze == null) {
                repaint();
                return;
            }

            if (autoSolving) {
                tickAutoSolver(dt);
                repaint();
                return;
            }

            if (pressed.isEmpty()) {
                repaint();
                return;
            }

            double dx = 0;
            double dy = 0;
            if (pressed.contains(Direction.LEFT)) {
                dx -= 1;
            }
            if (pressed.contains(Direction.RIGHT)) {
                dx += 1;
            }
            if (pressed.contains(Direction.UP)) {
                dy -= 1;
            }
            if (pressed.contains(Direction.DOWN)) {
                dy += 1;
            }

            if (dx != 0 || dy != 0) {
                double length = Math.sqrt(dx * dx + dy * dy);
                dx = dx / length * BASE_SPEED * dt;
                dy = dy / length * BASE_SPEED * dt;
                Point2D.Double moved = moveBy(dx, dy);
                updateFacing(moved.x, moved.y);
                checkGoal();
                repaint();
            }
        }

        private void tickAutoSolver(double dt) {
            if (autoTargetIndex >= autoPath.size()) {
                stopAutoSolve(null);
                checkGoal();
                return;
            }

            Point2D.Double target = autoPath.get(autoTargetIndex);
            double dx = target.x - player.x;
            double dy = target.y - player.y;
            double distance = Math.sqrt(dx * dx + dy * dy);
            while (distance < 1.2 && autoTargetIndex < autoPath.size() - 1) {
                player = new Point2D.Double(target.x, target.y);
                autoTargetIndex++;
                target = autoPath.get(autoTargetIndex);
                dx = target.x - player.x;
                dy = target.y - player.y;
                distance = Math.sqrt(dx * dx + dy * dy);
            }

            if (distance < 1.2) {
                player = new Point2D.Double(target.x, target.y);
                checkGoal();
                if (!solved) {
                    statusSink.setStatus("Auto solver finished.");
                }
                stopAutoSolve(null);
                return;
            }

            double step = Math.min(AUTO_SPEED * dt, distance);
            double moveX = dx / distance * step;
            double moveY = dy / distance * step;
            double beforeX = player.x;
            double beforeY = player.y;
            Point2D.Double moved = moveBy(moveX, moveY);
            updateFacing(moved.x, moved.y);
            if (Point2D.distance(beforeX, beforeY, player.x, player.y) < 0.02) {
                stopAutoSolve("Auto solver was blocked. Try moving start or finish.");
                return;
            }

            checkGoal();
            if (solved) {
                stopAutoSolve(null);
            }
        }

        private Point2D.Double moveBy(double dx, double dy) {
            double beforeX = player.x;
            double beforeY = player.y;
            moveAxis(dx, true);
            moveAxis(dy, false);
            return new Point2D.Double(player.x - beforeX, player.y - beforeY);
        }

        private void updateFacing(double movedX, double movedY) {
            if (Math.hypot(movedX, movedY) > 0.03) {
                facingAngle = Math.atan2(movedY, movedX);
            }
        }

        private void moveAxis(double delta, boolean horizontal) {
            int steps = Math.max(1, (int) Math.ceil(Math.abs(delta) / MIN_STEP));
            double part = delta / steps;
            for (int i = 0; i < steps; i++) {
                double nextX = horizontal ? player.x + part : player.x;
                double nextY = horizontal ? player.y : player.y + part;
                if (canStandAt(nextX, nextY)) {
                    player.x = nextX;
                    player.y = nextY;
                } else {
                    return;
                }
            }
        }

        private boolean canStandAt(double x, double y) {
            return MazeTools.canStandAt(mask, x, y, collisionRadius());
        }

        private double collisionRadius() {
            return Math.max(5.0, avatarSize * 0.38);
        }

        private void checkGoal() {
            if (maze == null || solved) {
                return;
            }
            double distance = player.distance(maze.goal);
            if (distance <= Math.max(16, avatarSize * 0.7)) {
                solved = true;
                pressed.clear();
                statusSink.setStatus("Solved.");
            }
        }

        private void handleMazeClick(Point point) {
            if (maze == null || toolMode == ToolMode.NONE) {
                return;
            }

            Point2D.Double mazePoint = screenToMaze(point);
            if (mazePoint == null) {
                return;
            }

            if (toolMode == ToolMode.SET_START || toolMode == ToolMode.SET_GOAL) {
                if (!canStandAt(mazePoint.x, mazePoint.y)) {
                    statusSink.setStatus("Pick an open tile.");
                    return;
                }
                if (toolMode == ToolMode.SET_START) {
                    maze.start = mazePoint;
                    player = new Point2D.Double(mazePoint.x, mazePoint.y);
                    solved = false;
                    statusSink.setStatus("Start updated.");
                } else {
                    maze.goal = mazePoint;
                    solved = false;
                    statusSink.setStatus("Finish updated.");
                }
                toolCompleted.run();
                repaint();
            }
        }

        private Point2D.Double screenToMaze(Point point) {
            if (maze == null || layout.scale <= 0) {
                return null;
            }
            double x = (point.x - layout.x) / layout.scale;
            double y = (point.y - layout.y) / layout.scale;
            if (x < 0 || y < 0 || x >= maze.image.getWidth() || y >= maze.image.getHeight()) {
                return null;
            }
            return new Point2D.Double(x, y);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            paintBackdrop(g);
            if (maze == null) {
                g.dispose();
                return;
            }

            layout = computeLayout();
            int drawW = (int) Math.round(maze.image.getWidth() * layout.scale);
            int drawH = (int) Math.round(maze.image.getHeight() * layout.scale);
            g.setColor(new Color(0, 0, 0, 96));
            g.fillRoundRect((int) layout.x + 8, (int) layout.y + 10, drawW + 10, drawH + 10, 12, 12);
            g.setColor(SURFACE);
            g.fillRoundRect((int) layout.x - 8, (int) layout.y - 8, drawW + 16, drawH + 16, 12, 12);
            g.setColor(LINE);
            g.drawRoundRect((int) layout.x - 8, (int) layout.y - 8, drawW + 16, drawH + 16, 12, 12);
            g.drawImage(maze.image, (int) layout.x, (int) layout.y, drawW, drawH, null);

            paintAutoPath(g);
            paintMarker(g, maze.start, ACCENT, "S");
            paintMarker(g, maze.goal, GOAL, "F");
            paintPlayer(g);
            if (toolMode != ToolMode.NONE) {
                paintToolHint(g);
            }
            if (solved) {
                paintSolved(g);
            }
            g.dispose();
        }

        private void paintBackdrop(Graphics2D g) {
            GradientPaint paint = new GradientPaint(0, 0, APP_BG, getWidth(), getHeight(), new Color(17, 28, 43));
            g.setPaint(paint);
            g.fillRect(0, 0, getWidth(), getHeight());
        }

        private DrawLayout computeLayout() {
            int padding = 28;
            double scaleX = (getWidth() - padding * 2.0) / maze.image.getWidth();
            double scaleY = (getHeight() - padding * 2.0) / maze.image.getHeight();
            double scale = Math.max(0.1, Math.min(scaleX, scaleY));
            double drawW = maze.image.getWidth() * scale;
            double drawH = maze.image.getHeight() * scale;
            double x = (getWidth() - drawW) / 2.0;
            double y = (getHeight() - drawH) / 2.0;
            return new DrawLayout(x, y, scale);
        }

        private void paintMarker(Graphics2D g, Point2D.Double point, Color color, String label) {
            double radius = Math.max(10, avatarSize * 0.42) * layout.scale;
            double x = layout.x + point.x * layout.scale;
            double y = layout.y + point.y * layout.scale;
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 44));
            g.fill(new Ellipse2D.Double(x - radius * 1.2, y - radius * 1.2, radius * 2.4, radius * 2.4));
            g.setColor(color);
            g.setStroke(new BasicStroke(2.4f));
            g.draw(new Ellipse2D.Double(x - radius, y - radius, radius * 2, radius * 2));
            g.setFont(g.getFont().deriveFont(Font.BOLD, Math.max(10f, (float) (11 * layout.scale))));
            FontMetrics metrics = g.getFontMetrics();
            g.drawString(label, (float) (x - metrics.stringWidth(label) / 2.0), (float) (y + metrics.getAscent() / 2.5));
        }

        private void paintAutoPath(Graphics2D g) {
            if (!autoSolving || autoPath.size() < 2 || autoTargetIndex >= autoPath.size()) {
                return;
            }

            Path2D.Double path = new Path2D.Double();
            path.moveTo(layout.x + player.x * layout.scale, layout.y + player.y * layout.scale);
            for (int i = autoTargetIndex; i < autoPath.size(); i++) {
                Point2D.Double point = autoPath.get(i);
                path.lineTo(layout.x + point.x * layout.scale, layout.y + point.y * layout.scale);
            }

            g.setStroke(new BasicStroke(5.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(33, 134, 111, 150));
            g.draw(path);
            g.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(255, 255, 255, 190));
            g.draw(path);
        }

        private void paintPlayer(Graphics2D g) {
            if (character == null) {
                return;
            }
            double maxDim = avatarSize * layout.scale;
            double imageW = character.image.getWidth();
            double imageH = character.image.getHeight();
            double scale = maxDim / Math.max(imageW, imageH);
            int drawW = Math.max(8, (int) Math.round(imageW * scale));
            int drawH = Math.max(8, (int) Math.round(imageH * scale));
            int x = (int) Math.round(layout.x + player.x * layout.scale - drawW / 2.0);
            int y = (int) Math.round(layout.y + player.y * layout.scale - drawH / 2.0);
            double centerX = x + drawW / 2.0;
            double centerY = y + drawH / 2.0;

            g.setColor(new Color(0, 0, 0, 42));
            g.fillOval(x + 2, y + drawH - 5, drawW - 4, 7);

            Graphics2D rotated = (Graphics2D) g.create();
            rotated.rotate(facingAngle, centerX, centerY);
            rotated.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            rotated.drawImage(character.image, x, y, drawW, drawH, null);
            rotated.dispose();
            paintFacingPointer(g, centerX, centerY, Math.max(drawW, drawH));
        }

        private void paintFacingPointer(Graphics2D g, double centerX, double centerY, int playerSize) {
            double tipDistance = Math.max(12, playerSize * 0.58);
            double baseDistance = Math.max(6, playerSize * 0.25);
            double halfWidth = Math.max(4, playerSize * 0.13);
            double cos = Math.cos(facingAngle);
            double sin = Math.sin(facingAngle);
            double tipX = centerX + cos * tipDistance;
            double tipY = centerY + sin * tipDistance;
            double baseX = centerX + cos * baseDistance;
            double baseY = centerY + sin * baseDistance;
            double normalX = -sin;
            double normalY = cos;

            Path2D.Double pointer = new Path2D.Double();
            pointer.moveTo(tipX, tipY);
            pointer.lineTo(baseX + normalX * halfWidth, baseY + normalY * halfWidth);
            pointer.lineTo(baseX - normalX * halfWidth, baseY - normalY * halfWidth);
            pointer.closePath();

            g.setColor(new Color(8, 18, 25, 150));
            g.fill(pointer);
            g.setColor(ACCENT);
            g.draw(pointer);
        }

        private void paintToolHint(Graphics2D g) {
            String text;
            if (toolMode == ToolMode.SET_START) {
                text = "Set start";
            } else {
                text = "Set finish";
            }
            paintBadge(g, text, ACCENT);
        }

        private void paintSolved(Graphics2D g) {
            paintBadge(g, "Solved", GOAL);
        }

        private void paintBadge(Graphics2D g, String text, Color color) {
            g.setFont(g.getFont().deriveFont(Font.BOLD, 18f));
            FontMetrics metrics = g.getFontMetrics();
            int width = metrics.stringWidth(text) + 32;
            int height = 38;
            int x = (getWidth() - width) / 2;
            int y = 20;
            g.setColor(new Color(17, 24, 35, 232));
            g.fillRoundRect(x, y, width, height, 8, 8);
            g.setColor(color);
            g.drawRoundRect(x, y, width, height, 8, 8);
            g.setColor(INK);
            g.drawString(text, x + 16, y + 25);
        }
    }

    private interface StatusSink {
        void setStatus(String text);
    }

    static final class UserImageException extends Exception {
        UserImageException(String message) {
            super(message);
        }
    }

    private static final class DrawLayout {
        final double x;
        final double y;
        final double scale;

        DrawLayout(double x, double y, double scale) {
            this.x = x;
            this.y = y;
            this.scale = scale;
        }
    }

    static final class CollisionMask {
        private final int width;
        private final int height;
        private final boolean[] wall;

        CollisionMask(BufferedImage image, Color wallColor, int tolerance) {
            this.width = image.getWidth();
            this.height = image.getHeight();
            this.wall = new boolean[width * height];
            int toleranceSq = tolerance * tolerance;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int argb = image.getRGB(x, y);
                    int alpha = (argb >>> 24) & 0xff;
                    if (alpha < 24) {
                        continue;
                    }
                    int red = (argb >>> 16) & 0xff;
                    int green = (argb >>> 8) & 0xff;
                    int blue = argb & 0xff;
                    wall[y * width + x] = colorDistanceSq(red, green, blue, wallColor) <= toleranceSq;
                }
            }
        }

        boolean isWall(double x, double y) {
            int px = (int) Math.round(x);
            int py = (int) Math.round(y);
            if (px < 0 || py < 0 || px >= width || py >= height) {
                return true;
            }
            return wall[py * width + px];
        }
    }

    static final class MazeTools {
        static final int DEFAULT_WALL_TOLERANCE = 76;

        private MazeTools() {
        }

        static Color guessWallColor(BufferedImage image) {
            Map<Integer, Integer> counts = new HashMap<>();
            int step = Math.max(1, Math.min(image.getWidth(), image.getHeight()) / 220);
            for (int y = 0; y < image.getHeight(); y += step) {
                for (int x = 0; x < image.getWidth(); x += step) {
                    int argb = image.getRGB(x, y);
                    int alpha = (argb >>> 24) & 0xff;
                    if (alpha < 32) {
                        continue;
                    }
                    int red = (argb >>> 16) & 0xff;
                    int green = (argb >>> 8) & 0xff;
                    int blue = argb & 0xff;
                    if (isNearWhite(red, green, blue)) {
                        continue;
                    }
                    int key = (red / 16 << 8) | (green / 16 << 4) | (blue / 16);
                    counts.put(key, counts.getOrDefault(key, 0) + 1);
                }
            }

            if (counts.isEmpty()) {
                return Color.BLACK;
            }

            int bestKey = 0;
            int bestCount = -1;
            for (Map.Entry<Integer, Integer> entry : counts.entrySet()) {
                if (entry.getValue() > bestCount) {
                    bestKey = entry.getKey();
                    bestCount = entry.getValue();
                }
            }

            int red = ((bestKey >> 8) & 0xf) * 16 + 8;
            int green = ((bestKey >> 4) & 0xf) * 16 + 8;
            int blue = (bestKey & 0xf) * 16 + 8;
            return new Color(red, green, blue);
        }

        static Point2D.Double findOpenPoint(CollisionMask mask, double preferredX, double preferredY, double avatarSize) {
            double radius = Math.max(5.0, avatarSize * 0.38);
            double maxRadius = Math.max(mask.width, mask.height);
            for (double search = 0; search < maxRadius; search += 6) {
                for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 10) {
                    double x = preferredX + Math.cos(angle) * search;
                    double y = preferredY + Math.sin(angle) * search;
                    if (canStandAt(mask, x, y, radius)) {
                        return new Point2D.Double(x, y);
                    }
                }
            }
            return new Point2D.Double(mask.width / 2.0, mask.height / 2.0);
        }

        static List<Point2D.Double> solvePath(CollisionMask mask, Point2D.Double start, Point2D.Double goal, double radius) {
            if (mask == null || !canStandAt(mask, start.x, start.y, radius) || !canStandAt(mask, goal.x, goal.y, radius)) {
                return List.of();
            }

            // A* searches walkable center points only. The octile heuristic is admissible for
            // 8-direction movement, and the final smoothing pass keeps segments wall-safe.
            int step = Math.max(4, Math.min(8, (int) Math.round(radius * 0.5)));
            int cols = (int) Math.ceil((mask.width - 1) / (double) step) + 1;
            int rows = (int) Math.ceil((mask.height - 1) / (double) step) + 1;
            boolean[] open = new boolean[cols * rows];
            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < cols; col++) {
                    Point2D.Double point = gridPoint(col, row, step, mask);
                    open[row * cols + col] = canStandAt(mask, point.x, point.y, radius);
                }
            }

            int startIndex = nearestOpenIndex(open, cols, rows, start, step);
            int goalIndex = nearestOpenIndex(open, cols, rows, goal, step);
            if (startIndex < 0 || goalIndex < 0) {
                return List.of();
            }

            double[] costs = new double[cols * rows];
            int[] previous = new int[cols * rows];
            Arrays.fill(costs, Double.POSITIVE_INFINITY);
            Arrays.fill(previous, -1);

            PriorityQueue<SearchNode> queue = new PriorityQueue<>((left, right) -> Double.compare(left.priority, right.priority));
            costs[startIndex] = 0;
            queue.add(new SearchNode(startIndex, 0, octileHeuristic(startIndex, goalIndex, cols, step)));

            int[] dCol = {1, -1, 0, 0, 1, 1, -1, -1};
            int[] dRow = {0, 0, 1, -1, 1, -1, 1, -1};

            while (!queue.isEmpty()) {
                SearchNode node = queue.poll();
                if (node.cost > costs[node.index]) {
                    continue;
                }
                if (node.index == goalIndex) {
                    break;
                }

                int col = node.index % cols;
                int row = node.index / cols;
                for (int i = 0; i < dCol.length; i++) {
                    int nextCol = col + dCol[i];
                    int nextRow = row + dRow[i];
                    if (nextCol < 0 || nextRow < 0 || nextCol >= cols || nextRow >= rows) {
                        continue;
                    }

                    int nextIndex = nextRow * cols + nextCol;
                    if (!open[nextIndex]) {
                        continue;
                    }
                    if (dCol[i] != 0 && dRow[i] != 0) {
                        int horizontal = row * cols + nextCol;
                        int vertical = nextRow * cols + col;
                        if (!open[horizontal] || !open[vertical]) {
                            continue;
                        }
                    }
                    if (!hasClearSegment(mask, gridPoint(col, row, step, mask), gridPoint(nextCol, nextRow, step, mask), radius)) {
                        continue;
                    }

                    double nextCost = node.cost + (dCol[i] == 0 || dRow[i] == 0 ? step : step * Math.sqrt(2.0));
                    if (nextCost < costs[nextIndex]) {
                        costs[nextIndex] = nextCost;
                        previous[nextIndex] = node.index;
                        queue.add(new SearchNode(nextIndex, nextCost, nextCost + octileHeuristic(nextIndex, goalIndex, cols, step)));
                    }
                }
            }

            if (startIndex != goalIndex && previous[goalIndex] == -1) {
                return List.of();
            }

            List<Point2D.Double> path = new ArrayList<>();
            for (int index = goalIndex; index != -1; index = previous[index]) {
                path.add(gridPoint(index % cols, index / cols, step, mask));
            }
            Collections.reverse(path);
            if (path.isEmpty()) {
                return List.of();
            }
            path.set(0, new Point2D.Double(start.x, start.y));
            path.set(path.size() - 1, new Point2D.Double(goal.x, goal.y));
            return smoothPath(mask, simplifyPath(path), radius);
        }

        private static List<Point2D.Double> simplifyPath(List<Point2D.Double> path) {
            if (path.size() <= 2) {
                return path;
            }

            List<Point2D.Double> simplified = new ArrayList<>();
            simplified.add(path.get(0));
            for (int i = 1; i < path.size() - 1; i++) {
                Point2D.Double previous = path.get(i - 1);
                Point2D.Double current = path.get(i);
                Point2D.Double next = path.get(i + 1);
                double cross = (current.x - previous.x) * (next.y - current.y)
                        - (current.y - previous.y) * (next.x - current.x);
                if (Math.abs(cross) > 0.01) {
                    simplified.add(current);
                }
            }
            simplified.add(path.get(path.size() - 1));
            return simplified;
        }

        private static List<Point2D.Double> smoothPath(CollisionMask mask, List<Point2D.Double> path, double radius) {
            if (path.size() <= 2) {
                return path;
            }

            List<Point2D.Double> smoothed = new ArrayList<>();
            int index = 0;
            smoothed.add(path.get(0));
            while (index < path.size() - 1) {
                int next = path.size() - 1;
                while (next > index + 1 && !hasClearSegment(mask, path.get(index), path.get(next), radius)) {
                    next--;
                }
                smoothed.add(path.get(next));
                index = next;
            }
            return smoothed;
        }

        static boolean hasClearSegment(CollisionMask mask, Point2D.Double from, Point2D.Double to, double radius) {
            double distance = from.distance(to);
            int steps = Math.max(1, (int) Math.ceil(distance / 1.25));
            for (int i = 0; i <= steps; i++) {
                double t = i / (double) steps;
                double x = from.x + (to.x - from.x) * t;
                double y = from.y + (to.y - from.y) * t;
                if (!canStandAt(mask, x, y, radius)) {
                    return false;
                }
            }
            return true;
        }

        private static int nearestOpenIndex(boolean[] open, int cols, int rows, Point2D.Double point, int step) {
            int centerCol = clamp((int) Math.round(point.x / step), 0, cols - 1);
            int centerRow = clamp((int) Math.round(point.y / step), 0, rows - 1);
            int maxRing = Math.max(cols, rows);
            for (int ring = 0; ring <= maxRing; ring++) {
                for (int row = centerRow - ring; row <= centerRow + ring; row++) {
                    for (int col = centerCol - ring; col <= centerCol + ring; col++) {
                        if (col < 0 || row < 0 || col >= cols || row >= rows) {
                            continue;
                        }
                        if (Math.abs(col - centerCol) != ring && Math.abs(row - centerRow) != ring) {
                            continue;
                        }
                        int index = row * cols + col;
                        if (open[index]) {
                            return index;
                        }
                    }
                }
            }
            return -1;
        }

        private static Point2D.Double gridPoint(int col, int row, int step, CollisionMask mask) {
            double x = Math.min(mask.width - 1, col * step);
            double y = Math.min(mask.height - 1, row * step);
            return new Point2D.Double(x, y);
        }

        private static double octileHeuristic(int fromIndex, int toIndex, int cols, int step) {
            int fromCol = fromIndex % cols;
            int fromRow = fromIndex / cols;
            int toCol = toIndex % cols;
            int toRow = toIndex / cols;
            int dx = Math.abs(fromCol - toCol);
            int dy = Math.abs(fromRow - toRow);
            return step * (Math.max(dx, dy) + (Math.sqrt(2.0) - 1.0) * Math.min(dx, dy));
        }

        static boolean canStandAt(CollisionMask mask, double x, double y, double radius) {
            if (mask == null || mask.isWall(x, y)) {
                return false;
            }

            List<Point2D.Double> samples = new ArrayList<>();
            samples.add(new Point2D.Double(0, 0));
            for (int i = 0; i < 16; i++) {
                double angle = i * Math.PI * 2 / 16.0;
                samples.add(new Point2D.Double(Math.cos(angle) * radius, Math.sin(angle) * radius));
            }
            for (int i = 0; i < 8; i++) {
                double angle = (i + 0.5) * Math.PI * 2 / 8.0;
                samples.add(new Point2D.Double(Math.cos(angle) * radius * 0.58, Math.sin(angle) * radius * 0.58));
            }

            for (Point2D.Double sample : samples) {
                if (mask.isWall(x + sample.x, y + sample.y)) {
                    return false;
                }
            }
            return true;
        }

        private static boolean isNearWhite(int red, int green, int blue) {
            return red > 235 && green > 235 && blue > 235;
        }

        private static final class SearchNode {
            final int index;
            final double cost;
            final double priority;

            SearchNode(int index, double cost, double priority) {
                this.index = index;
                this.cost = cost;
                this.priority = priority;
            }
        }
    }

    private static int colorDistanceSq(int red, int green, int blue, Color color) {
        int dr = red - color.getRed();
        int dg = green - color.getGreen();
        int db = blue - color.getBlue();
        return dr * dr + dg * dg + db * db;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
