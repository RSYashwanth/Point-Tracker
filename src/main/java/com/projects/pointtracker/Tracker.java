package com.projects.pointtracker;

import javax.imageio.ImageIO;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Backend tracker class for program
 */
public class Tracker {

    // Deconstructed frames folder
    public static String imageFolder = ".\\target\\classes\\images";

    // Input path of the video file
    public String inputPath;

    // The fps of the input video
    public double fps;

    // The ratio between number of pixels and trackwidth
    public double ratio = 1d;

    // Track width value
    public double trackWidth = 1;

    // Integer representation of rgba color
    public int color;

    /**
     * Constructor to create a tracker object with a specified input video
     * 
     * @param inputPath the path of the input video
     */
    public Tracker(String inputPath) {
        this.inputPath = inputPath;
        this.fps = getFPS(inputPath);
    }

    /**
     * Setter function to set color using rgb
     * 
     * @param r the red channel
     * @param g the green channel
     * @param b the blue channel
     */
    public void setColor(int r, int g, int b) {
        this.color = formatColor(r, g, b);
    }

    /**
     * Utility function to delete all existing intermediate frames from the imageFolder directory
     * 
     * @return true if flushed successfully
     */
    public static boolean flushFrames() {
        File directory = new File(imageFolder);
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
        return true;
    }

    /**
     * Utility function to get input video fps using ffprobe command line tool
     * 
     * @param targetVideo path to target video
     * @return fps of the targetVideo
     */
    public double getFPS(String targetVideo) {
        double toReturn = -1.0;
        try {
            Process process = new ProcessBuilder(
                    "ffprobe", "-v",
                    "error", "-select_streams",
                    "v:0", "-show_entries",
                    "stream=r_frame_rate",
                    "-of", "default=noprint_wrappers=1:nokey=1", targetVideo).redirectErrorStream(true).start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String[] parts = reader.readLine().split("/");
            if (parts.length == 2) {
                int numerator = Integer.parseInt(parts[0]);
                int denominator = Integer.parseInt(parts[1]);
                toReturn = (double) numerator / denominator;
            } else {
                for (String part : parts) {
                    System.out.println(part);
                }
                throw new RuntimeException();
            }
            process.waitFor();
            process.destroy();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return toReturn;
    }

    /**
     * Utility function to convert rgb values into integer rgba representation
     * 
     * @param red   the red channel
     * @param green the green channel
     * @param blue  the blue channel
     * @return integer rgba representation of the color
     */
    public int formatColor(int red, int green, int blue) {
        return (red << 16) | (green << 8) | blue;
    }

    /**
     * Utility function to find the numeric difference between two colors
     * 
     * @param color1
     * @param color2
     * @return numeric difference between the colors
     */
    public double colorDiff(int color1, int color2) {
        int r1 = (color1 & 0x00ff0000) >> 16;
        int g1 = (color1 & 0x0000ff00) >> 8;
        int b1 = color1 & 0x000000ff;

        int r2 = (color2 & 0x00ff0000) >> 16;
        int g2 = (color2 & 0x0000ff00) >> 8;
        int b2 = color2 & 0x000000ff;

        double d = Math.sqrt(
                Math.pow((double) (r1 - r2), 2) + Math.pow((double) (g1 - g2), 2) + Math.pow((double) (b1 - b2), 2));
        return d / Math.sqrt(Math.pow(255, 2) + Math.pow(255, 2) + Math.pow(255, 2)) * 100;
    }

    /**
     * Utility function to draw a circle on an image at a specified coordinate
     * 
     * @param image the image to draw on
     * @param x
     * @param y
     */
    public void drawCircle(BufferedImage image, int x, int y) {
        Graphics g = image.getGraphics();
        g.setColor(new Color(255, 0, 0));
        g.drawOval(x - 5, y - 5, 10, 10);
        g.drawRect(x - 25, y - 25, 50, 50);
        g.dispose();
    }

    /**
     * Utility function to draw the speed on an image at a specified coordinate
     * 
     * @param image   the image to draw on
     * @param coords  the coords of the objects at all frames
     * @param counter the particular frame to calculate and draw speed for
     */
    public void drawSpeed(BufferedImage image, int[][] coords, int counter) {
        Graphics g = image.getGraphics();
        g.setColor(new Color(255, 0, 0));

        double distance = 0;
        if (counter >= 1) {
            distance = Math.sqrt(
                    Math.pow((coords[counter][0] - coords[counter - 1][0]), 2) +
                            Math.pow((coords[counter][1] - coords[counter - 1][1]), 2));
        }
        double time = 1.0 / fps;
        g.drawString("Speed: " + Math.round((distance / time) / ratio / trackWidth) + "tw/s",
                coords[counter][0] - 25, coords[counter][1] - 40);
        g.dispose();
    }

    /**
     * Utility function to deconstruct an input video into frames using ffmpeg
     * 
     * @return true if video deconstructed successfully
     */
    public boolean deconstruct() {
        String framePattern = imageFolder + "\\frame_%04d.png";
        try {
            flushFrames();

            Process process = new ProcessBuilder(
                    "ffmpeg",
                    "-i", inputPath,
                    framePattern).redirectErrorStream(true).start();

            BufferedReader reader =
                new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;

            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("Video deconstruction completed successfully.");
                return true;
            } else {
                System.out.println("Video deconstruction failed with code: " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Utility function to reconstruct intermediary frames into a video at the
     * original fps using ffmpeg
     * 
     * @param outputPath the path to save the output video in
     */
    public void reconstruct(String outputPath) {
        String framePattern = imageFolder + "\\frame_%04d.png";
        try {
            Process process = new ProcessBuilder(
                "ffmpeg",
                "-framerate", "30",
                "-i", framePattern,
                "-c:v", "libx264",
                "-crf", "18",
                "-preset", "slow",
                "-pix_fmt", "yuv420p",
                outputPath + "\\output-video.mp4").redirectErrorStream(true).start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("Video reconstruction completed successfully.");
            } else {
                System.out.println(exitCode);
                System.err.println("Video reconstruction failed.");
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Utility function to search for a point in the image within the bounds
     * specified
     * 
     * @param lowerX lower x bound
     * @param upperX upper x bound
     * @param lowerY lower y bound
     * @param upperY upper y bound
     * @param image  image to search through
     * @param target target color to search for
     * @return and array of all the matching coordinates
     */
    public int[] searchPoint(int lowerX, int upperX, int lowerY, int upperY, BufferedImage image, int target) {
        int sumX = 0;
        int sumY = 0;
        int c = 0;

        for (int x = lowerX; x < upperX; x++) {
            for (int y = lowerY; y < upperY; y++) {
                int color = image.getRGB(x, y);
                if (colorDiff(color, target) <= 15) {
                    sumX += x;
                    sumY += y;
                    c += 1;
                }
            }
        }
        return new int[] { (sumX == 0) ? 0 : sumX / c, (sumY == 0) ? 0 : sumY / c };
    }

    /**
     * Utility function to iteratively look for a point in an image using
     * predicted places the point could be in.
     * 
     * @param target the integer color representation
     * @param image  the image to look through
     * @param predX  the predicted X coordinate
     * @param predY  the predicted Y coordinate
     * @return an array of all the matching coordinates
     */
    public int[] findPoint(int target, BufferedImage image, int predX, int predY) {
        int iter = 1;
        int[] prev_coords = { 0, 0 };
        int[] current_coords = { 0, 0 };
        while (iter <= 20) {
            int rangeX = image.getWidth() / 20;
            int lowerX = predX - (iter * rangeX);
            if (lowerX < 0)
                lowerX = 0;
            else if (lowerX > image.getWidth())
                lowerX = image.getWidth() - 1;

            int upperX = predX + (iter * rangeX);
            if (upperX < 0)
                upperX = 0;
            else if (upperX > image.getWidth())
                upperX = image.getWidth() - 1;

            int rangeY = image.getHeight() / 20;
            int lowerY = predY - (iter * rangeY);
            if (lowerY < 0)
                lowerY = 0;
            else if (lowerY > image.getHeight())
                lowerY = image.getHeight() - 1;

            int upperY = predY + (iter * rangeY);
            if (upperY < 0)
                upperY = 0;
            else if (upperY > image.getHeight())
                upperY = image.getHeight() - 1;

            current_coords = searchPoint(lowerX, upperX, lowerY, upperY, image, target);
            if (current_coords[0] != 0 && current_coords[1] != 0 && current_coords[0] == prev_coords[0]
                    && current_coords[1] == prev_coords[1]) {
                System.out.print(iter + " ");
                return current_coords;
            }
            prev_coords = current_coords;
            iter += 1;
        }
        throw new IllegalStateException("Cannot find color in frame");
    }

    /**
     * Function to go through each frame and track a uniquely colored point visually
     * 
     * @param target the integer representation of the target color
     */
    public void trackPoint(int target) {
        File images = new File(imageFolder);
        int[][] coords;
        try {
            int length = images.listFiles().length;
            coords = new int[length][2];
            int counter = 0;
            for (File file : images.listFiles()) {
                BufferedImage image = ImageIO.read(file);
                int[] pred_coords = { 0, 0 };
                if (counter >= 2) {
                    pred_coords = predictPoint(coords[counter - 1], coords[counter - 2]);
                }
                coords[counter] = findPoint(target, image, pred_coords[0], pred_coords[1]);
                drawCircle(image, coords[counter][0], coords[counter][1]);
                drawSpeed(image, coords, counter);
                ImageIO.write(image, "png", file);
                System.out.println(++counter + " out of " + length);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Overloaded function to track point using this.color
     */
    public void trackPoint() {
        trackPoint(this.color);
    }

    /**
     * Utility function to predict the position of the point in the
     * next frame based on velocity of point.
     * 
     * @param coord1 the coordinates of the point in the last frame
     * @param coord2 the coordinates of the point in the last to last frame
     * @return the predicted coordinates of the point in the current frame
     */
    public int[] predictPoint(int[] coord1, int[] coord2) {
        return new int[] { 2 * coord1[0] - coord2[0], 2 * coord1[1] - coord2[1] };
    }
}
