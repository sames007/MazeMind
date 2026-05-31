package com.mazemind;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

public final class DocumentationMediaGenerator {
    private static final int WIDTH = 1120;
    private static final int HEIGHT = 680;
    private static final int HEADER = 68;
    private static final int SIDEBAR = 312;
    private static final Color INK = new Color(234, 240, 248);
    private static final Color MUTED = new Color(157, 168, 184);
    private static final Color APP_BG = new Color(10, 14, 22);
    private static final Color PANEL = new Color(17, 24, 35);
    private static final Color SURFACE = new Color(24, 33, 47);
    private static final Color SURFACE_ALT = new Color(31, 42, 59);
    private static final Color LINE = new Color(61, 74, 93);
    private static final Color ACCENT = new Color(45, 212, 191);
    private static final Color GOAL = new Color(168, 116, 255);

    public static void main(String[] args) throws Exception {
        File screenshots = new File("docs/screenshots");
        if (!screenshots.exists() && !screenshots.mkdirs()) {
            throw new IllegalStateException("Could not create docs/screenshots");
        }

        BufferedImage maze = read("/assets/maze.png");
        BufferedImage car = read("/assets/car.png");
        Color wall = MazeMindApp.MazeTools.guessWallColor(maze);
        MazeMindApp.CollisionMask mask = new MazeMindApp.CollisionMask(maze, wall, 76);
        Point2D.Double start = MazeMindApp.MazeTools.findOpenPoint(mask, 30, 271, 28);
        Point2D.Double goal = MazeMindApp.MazeTools.findOpenPoint(mask, 586, 255, 28);
        List<Point2D.Double> route = MazeMindApp.MazeTools.solvePath(mask, start, goal, 10.5);

        ImageIO.write(renderFrame(maze, car, route, 0.0, false, "Manual mode ready"), "png", new File(screenshots, "main.png"));
        ImageIO.write(renderFrame(maze, car, route, 0.45, true, "Auto solver running"), "png", new File(screenshots, "auto-solve.png"));

        try (ImageOutputStream output = new FileImageOutputStream(new File("docs/walkthrough.gif"));
             GifSequenceWriter writer = new GifSequenceWriter(output, BufferedImage.TYPE_INT_RGB, 55, true)) {
            int frames = 72;
            for (int i = 0; i < frames; i++) {
                double t = i / (double) (frames - 1);
                writer.writeToSequence(renderFrame(maze, car, route, t, true, t < 0.98 ? "Auto solver running" : "Solved"));
            }
        }

        System.out.println("Documentation media generated.");
    }

    private static BufferedImage renderFrame(BufferedImage maze, BufferedImage car, List<Point2D.Double> route,
                                             double progress, boolean showPath, String status) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));

        paintBackground(g);
        paintHeader(g);
        paintSidebar(g, status, showPath);

        int areaW = WIDTH - SIDEBAR;
        int areaH = HEIGHT - HEADER;
        int maxW = areaW - 58;
        int maxH = areaH - 58;
        double scale = Math.min(maxW / (double) maze.getWidth(), maxH / (double) maze.getHeight());
        int drawW = (int) Math.round(maze.getWidth() * scale);
        int drawH = (int) Math.round(maze.getHeight() * scale);
        int x = (areaW - drawW) / 2;
        int y = HEADER + (areaH - drawH) / 2;

        g.setColor(new Color(0, 0, 0, 95));
        g.fillRoundRect(x + 10, y + 12, drawW + 18, drawH + 18, 16, 16);
        g.setColor(SURFACE);
        g.fillRoundRect(x - 10, y - 10, drawW + 20, drawH + 20, 16, 16);
        g.setColor(LINE);
        g.drawRoundRect(x - 10, y - 10, drawW + 20, drawH + 20, 16, 16);
        g.drawImage(maze, x, y, drawW, drawH, null);

        if (showPath) {
            paintRoute(g, route, x, y, scale);
        }

        paintMarker(g, x, y, scale, route.get(0), ACCENT, "S");
        paintMarker(g, x, y, scale, route.get(route.size() - 1), GOAL, "F");
        paintCharacter(g, car, route, progress, x, y, scale);
        g.dispose();
        return image;
    }

    private static void paintBackground(Graphics2D g) {
        g.setPaint(new GradientPaint(0, 0, APP_BG, WIDTH, HEIGHT, new Color(17, 28, 43)));
        g.fillRect(0, 0, WIDTH, HEIGHT);
    }

    private static void paintHeader(Graphics2D g) {
        g.setPaint(new GradientPaint(0, 0, new Color(11, 18, 32), WIDTH, HEADER, new Color(15, 86, 91)));
        g.fillRect(0, 0, WIDTH, HEADER);
        g.setColor(INK);
        g.setFont(g.getFont().deriveFont(Font.BOLD, 28f));
        g.drawString("MazeMind", 26, 42);
        g.setColor(new Color(191, 224, 221));
        g.setFont(g.getFont().deriveFont(Font.PLAIN, 15f));
        g.drawString("Manual play and shortest-path auto solving", 182, 42);
    }

    private static void paintSidebar(Graphics2D g, String status, boolean autoRunning) {
        int x = WIDTH - SIDEBAR;
        g.setColor(PANEL);
        g.fillRect(x, HEADER, SIDEBAR, HEIGHT - HEADER);
        g.setColor(LINE);
        g.drawLine(x, HEADER, x, HEIGHT);

        int y = HEADER + 34;
        y = paintControl(g, x + 22, y, "Maze", "Classic maze");
        y = paintControl(g, x + 22, y + 20, "Character", "Car");
        y = paintButton(g, x + 22, y + 26, autoRunning ? "Stop Auto" : "Auto Solve", true);
        y = paintButton(g, x + 22, y + 12, "Set Start", false);
        y = paintButton(g, x + 22, y + 12, "Set Finish", false);
        y = paintButton(g, x + 22, y + 12, "Reset", false);

        g.setColor(MUTED);
        g.setFont(g.getFont().deriveFont(Font.BOLD, 13f));
        g.drawString("Move", x + 22, y + 28);
        paintButton(g, x + 82, y + 42, "Up", false, 96);
        paintButton(g, x + 22, y + 86, "Left", false, 82);
        paintButton(g, x + 116, y + 86, "Down", false, 82);
        paintButton(g, x + 210, y + 86, "Right", false, 82);

        g.setColor(SURFACE);
        g.fillRoundRect(x + 22, HEIGHT - 58, SIDEBAR - 44, 42, 10, 10);
        g.setColor(LINE);
        g.drawRoundRect(x + 22, HEIGHT - 58, SIDEBAR - 44, 42, 10, 10);
        g.setColor(MUTED);
        g.setFont(g.getFont().deriveFont(Font.PLAIN, 14f));
        g.drawString(status, x + 36, HEIGHT - 31);
    }

    private static int paintControl(Graphics2D g, int x, int y, String label, String value) {
        g.setColor(MUTED);
        g.setFont(g.getFont().deriveFont(Font.BOLD, 13f));
        g.drawString(label, x, y);
        g.setColor(SURFACE);
        g.fillRoundRect(x, y + 10, 268, 38, 9, 9);
        g.setColor(LINE);
        g.drawRoundRect(x, y + 10, 268, 38, 9, 9);
        g.setColor(INK);
        g.setFont(g.getFont().deriveFont(Font.PLAIN, 14f));
        g.drawString(value, x + 12, y + 35);
        return y + 58;
    }

    private static int paintButton(Graphics2D g, int x, int y, String label, boolean primary) {
        return paintButton(g, x, y, label, primary, 268);
    }

    private static int paintButton(Graphics2D g, int x, int y, String label, boolean primary, int width) {
        g.setColor(primary ? ACCENT : SURFACE_ALT);
        g.fillRoundRect(x, y, width, 36, 9, 9);
        g.setColor(primary ? new Color(35, 172, 157) : LINE);
        g.drawRoundRect(x, y, width, 36, 9, 9);
        g.setColor(primary ? new Color(6, 24, 30) : INK);
        g.setFont(g.getFont().deriveFont(Font.BOLD, 13f));
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(label, x + (width - metrics.stringWidth(label)) / 2, y + 23);
        return y + 36;
    }

    private static void paintRoute(Graphics2D g, List<Point2D.Double> route, int x, int y, double scale) {
        Path2D.Double path = new Path2D.Double();
        path.moveTo(x + route.get(0).x * scale, y + route.get(0).y * scale);
        for (int i = 1; i < route.size(); i++) {
            Point2D.Double point = route.get(i);
            path.lineTo(x + point.x * scale, y + point.y * scale);
        }
        g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(45, 212, 191, 155));
        g.draw(path);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(255, 255, 255, 190));
        g.draw(path);
    }

    private static void paintMarker(Graphics2D g, int x, int y, double scale, Point2D.Double point, Color color, String label) {
        int px = (int) Math.round(x + point.x * scale);
        int py = (int) Math.round(y + point.y * scale);
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 64));
        g.fillOval(px - 18, py - 18, 36, 36);
        g.setColor(color);
        g.setStroke(new BasicStroke(2.4f));
        g.drawOval(px - 14, py - 14, 28, 28);
        g.setFont(g.getFont().deriveFont(Font.BOLD, 13f));
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(label, px - metrics.stringWidth(label) / 2, py + 5);
    }

    private static void paintCharacter(Graphics2D g, BufferedImage car, List<Point2D.Double> route, double progress,
                                       int x, int y, double scale) {
        RoutePosition routePosition = pointAlongRoute(route, progress);
        int drawW = 48;
        int drawH = Math.max(20, (int) Math.round(drawW * car.getHeight() / (double) car.getWidth()));
        int cx = (int) Math.round(x + routePosition.point().x * scale);
        int cy = (int) Math.round(y + routePosition.point().y * scale);
        g.setColor(new Color(0, 0, 0, 58));
        g.fillOval(cx - 22, cy + 12, 44, 8);
        Graphics2D rotated = (Graphics2D) g.create();
        rotated.rotate(routePosition.angle(), cx, cy);
        rotated.drawImage(car, cx - drawW / 2, cy - drawH / 2, drawW, drawH, null);
        rotated.dispose();
    }

    private static RoutePosition pointAlongRoute(List<Point2D.Double> route, double progress) {
        double total = 0;
        for (int i = 1; i < route.size(); i++) {
            total += route.get(i - 1).distance(route.get(i));
        }
        double target = total * Math.max(0, Math.min(1, progress));
        double covered = 0;
        for (int i = 1; i < route.size(); i++) {
            Point2D.Double from = route.get(i - 1);
            Point2D.Double to = route.get(i);
            double segment = from.distance(to);
            if (covered + segment >= target || i == route.size() - 1) {
                double t = segment == 0 ? 0 : (target - covered) / segment;
                double px = from.x + (to.x - from.x) * t;
                double py = from.y + (to.y - from.y) * t;
                return new RoutePosition(new Point2D.Double(px, py), Math.atan2(to.y - from.y, to.x - from.x));
            }
            covered += segment;
        }
        Point2D.Double last = route.get(route.size() - 1);
        return new RoutePosition(last, 0);
    }

    private static BufferedImage read(String path) throws Exception {
        try (InputStream stream = DocumentationMediaGenerator.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("Missing resource: " + path);
            }
            BufferedImage image = ImageIO.read(stream);
            if (image == null) {
                throw new IllegalStateException("Unsupported resource: " + path);
            }
            return image;
        }
    }

    private record RoutePosition(Point2D.Double point, double angle) {
    }

    private static final class GifSequenceWriter implements AutoCloseable {
        private final ImageWriter writer;
        private final ImageWriteParam params;
        private final IIOMetadata metadata;

        GifSequenceWriter(ImageOutputStream output, int imageType, int delayMillis, boolean loop) throws Exception {
            Iterator<ImageWriter> writers = ImageIO.getImageWritersBySuffix("gif");
            if (!writers.hasNext()) {
                throw new IllegalStateException("No GIF writer available.");
            }

            writer = writers.next();
            params = writer.getDefaultWriteParam();
            ImageTypeSpecifier type = ImageTypeSpecifier.createFromBufferedImageType(imageType);
            metadata = writer.getDefaultImageMetadata(type, params);
            configureMetadata(metadata, delayMillis, loop);
            writer.setOutput(output);
            writer.prepareWriteSequence(null);
        }

        void writeToSequence(BufferedImage image) throws Exception {
            writer.writeToSequence(new IIOImage(image, null, metadata), params);
        }

        @Override
        public void close() throws Exception {
            writer.endWriteSequence();
            writer.dispose();
        }

        private static void configureMetadata(IIOMetadata metadata, int delayMillis, boolean loop) throws Exception {
            String format = metadata.getNativeMetadataFormatName();
            IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(format);
            IIOMetadataNode graphics = getNode(root, "GraphicControlExtension");
            graphics.setAttribute("disposalMethod", "none");
            graphics.setAttribute("userInputFlag", "FALSE");
            graphics.setAttribute("transparentColorFlag", "FALSE");
            graphics.setAttribute("delayTime", Integer.toString(Math.max(1, delayMillis / 10)));
            graphics.setAttribute("transparentColorIndex", "0");

            IIOMetadataNode appExtensions = getNode(root, "ApplicationExtensions");
            IIOMetadataNode appNode = new IIOMetadataNode("ApplicationExtension");
            appNode.setAttribute("applicationID", "NETSCAPE");
            appNode.setAttribute("authenticationCode", "2.0");
            int loopCount = loop ? 0 : 1;
            appNode.setUserObject(new byte[]{0x1, (byte) (loopCount & 0xff), (byte) ((loopCount >> 8) & 0xff)});
            appExtensions.appendChild(appNode);
            metadata.setFromTree(format, root);
        }

        private static IIOMetadataNode getNode(IIOMetadataNode root, String name) {
            for (int i = 0; i < root.getLength(); i++) {
                if (root.item(i).getNodeName().equalsIgnoreCase(name)) {
                    return (IIOMetadataNode) root.item(i);
                }
            }
            IIOMetadataNode node = new IIOMetadataNode(name);
            root.appendChild(node);
            return node;
        }
    }
}
