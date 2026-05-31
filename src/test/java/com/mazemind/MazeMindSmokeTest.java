package com.mazemind;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;

public final class MazeMindSmokeTest {
    public static void main(String[] args) throws Exception {
        checkMaze("/assets/maze2.png", 34, 34, 446, 329);
        checkMaze("/assets/maze.png", 30, 271, 586, 255);
        checkImageValidation();
        System.out.println("Collision, solver, and upload validation smoke test passed.");
    }

    private static void checkMaze(String path, double startX, double startY, double goalX, double goalY) throws Exception {
        BufferedImage image = read(path);
        Color wallColor = MazeMindApp.MazeTools.guessWallColor(image);
        MazeMindApp.CollisionMask mask = new MazeMindApp.CollisionMask(image, wallColor, 76);

        Point2D.Double start = MazeMindApp.MazeTools.findOpenPoint(mask, startX, startY, 28);
        Point2D.Double goal = MazeMindApp.MazeTools.findOpenPoint(mask, goalX, goalY, 28);
        require(MazeMindApp.MazeTools.canStandAt(mask, start.x, start.y, 10.5), path + " start should be open");
        require(MazeMindApp.MazeTools.canStandAt(mask, goal.x, goal.y, 10.5), path + " finish should be open");

        List<Point2D.Double> route = MazeMindApp.MazeTools.solvePath(mask, start, goal, 10.5);
        require(route.size() > 1, path + " should have an auto-solve route");
        for (Point2D.Double point : route) {
            require(MazeMindApp.MazeTools.canStandAt(mask, point.x, point.y, 10.5), path + " route should stay open");
        }
        for (int i = 1; i < route.size(); i++) {
            requireSegmentOpen(mask, route.get(i - 1), route.get(i), path + " route segment should stay open");
        }

        boolean foundWall = false;
        for (int y = 0; y < image.getHeight() && !foundWall; y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (mask.isWall(x, y)) {
                    require(!MazeMindApp.MazeTools.canStandAt(mask, x, y, 5.0), path + " wall should block movement");
                    foundWall = true;
                    break;
                }
            }
        }
        require(foundWall, path + " should contain walls");
    }

    private static BufferedImage read(String path) throws Exception {
        try (InputStream stream = MazeMindSmokeTest.class.getResourceAsStream(path)) {
            require(stream != null, "Missing resource: " + path);
            BufferedImage image = ImageIO.read(stream);
            require(image != null, "Unsupported resource: " + path);
            return image;
        }
    }

    private static void checkImageValidation() throws Exception {
        File directory = new File("out/test-artifacts");
        require(directory.exists() || directory.mkdirs(), "Could not create test artifact directory");

        File validImage = new File(directory, "valid.png");
        BufferedImage image = new BufferedImage(12, 12, BufferedImage.TYPE_INT_ARGB);
        ImageIO.write(image, "png", validImage);
        require(MazeMindApp.readUserImage(validImage).getWidth() == 12, "Valid upload image should load");

        File invalidImage = new File(directory, "not-an-image.txt");
        Files.writeString(invalidImage.toPath(), "not an image");
        boolean rejected = false;
        try {
            MazeMindApp.readUserImage(invalidImage);
        } catch (MazeMindApp.UserImageException expected) {
            rejected = true;
        }
        require(rejected, "Unsupported upload files should be rejected");
    }

    private static void requireSegmentOpen(MazeMindApp.CollisionMask mask, Point2D.Double from, Point2D.Double to, String message) {
        double distance = from.distance(to);
        int steps = Math.max(1, (int) Math.ceil(distance / 2.0));
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            double x = from.x + (to.x - from.x) * t;
            double y = from.y + (to.y - from.y) * t;
            require(MazeMindApp.MazeTools.canStandAt(mask, x, y, 10.5), message);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
