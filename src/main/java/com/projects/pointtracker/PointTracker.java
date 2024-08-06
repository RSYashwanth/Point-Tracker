package com.projects.pointtracker;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

/**
 * Application UI class for point tracker
 */
public class PointTracker extends Application {

  // Main image viewer for the application
  private ImageView imageView;

  // Status label to warn/update status of program
  private Label warningLabel = new Label("");

  // Backend tracker object
  private Tracker tracker;

  // Boolean to check whether program is in setTarget mode
  private boolean setTarget = false;

  // Boolean to check whether program is in setScale mode
  private boolean setScale = false;

  // Variable to store how many scaling points have been set
  private int scalePoints = 0;

  // Variable to store scaling points
  private int[][] points = new int[2][2];

  /**
   * Runner function for JavaFX
   * 
   * @param primaryStage JavaFX Stage to draw application on
   */
  @Override
  public void start(Stage primaryStage) {
    primaryStage.setTitle("Point Tracker");

    Image defaultImage = new Image(getClass().getResource("/default.png").toExternalForm());

    imageView = new ImageView(defaultImage);
    imageView.setFitWidth(926);
    imageView.setFitHeight(571);
    imageView.addEventHandler(MouseEvent.MOUSE_CLICKED, this::imageViewHandler);

    BorderPane imagePane = new BorderPane(imageView);
    imagePane.getStyleClass().add("image-pane");

    Button loadButton = new Button("Load");
    loadButton.setOnAction(e -> loadImage(primaryStage));
    loadButton.getStyleClass().add("button");

    Button processButton = new Button("Process");
    processButton.setOnAction(e -> processImage(primaryStage));
    processButton.getStyleClass().add("button");

    Button setScaleButton = new Button("Scale");
    setScaleButton.setOnAction(e -> setScale());
    setScaleButton.getStyleClass().add("button");

    Button setTargetButton = new Button("Target");
    setTargetButton.setOnAction(e -> setTarget());
    setTargetButton.getStyleClass().add("button");

    StackPane b1 = new StackPane(loadButton);
    StackPane b2 = new StackPane(processButton);
    StackPane b3 = new StackPane(setScaleButton);
    StackPane b4 = new StackPane(setTargetButton);

    b1.getStyleClass().add("button-container");
    b2.getStyleClass().add("button-container");
    b3.getStyleClass().add("button-container");
    b4.getStyleClass().add("button-container");

    HBox row1 = new HBox(b1, b2);
    HBox row2 = new HBox(b3, b4);

    warningLabel.getStyleClass().add("warning-text");

    HBox bottomLeftBox = new HBox();
    bottomLeftBox.setAlignment(Pos.BOTTOM_LEFT);
    bottomLeftBox.getChildren().add(warningLabel);
    bottomLeftBox.getStyleClass().add("warning-container");
    VBox.setVgrow(bottomLeftBox, Priority.SOMETIMES);

    VBox buttons = new VBox();
    buttons.getChildren().addAll(row1, row2);
    buttons.getStyleClass().add("vbox");

    VBox ui = new VBox();
    ui.getChildren().addAll(imagePane, bottomLeftBox);

    HBox root = new HBox();
    root.getChildren().addAll(ui, buttons);
    root.setPrefSize(1080, 600);
    root.getStyleClass().add("hbox");

    Scene scene = new Scene(root);
    scene.getStylesheets().add(getClass().getResource("/stylesheet.css").toExternalForm());
    primaryStage.setScene(scene);
    primaryStage.setTitle("Image Viewer");
    primaryStage.show();
  }

  /**
   * Handler for imageView to handle the setTarget and setScale modes
   * 
   * @param e mouse event to get clicked coordinates
   */
  private void imageViewHandler(MouseEvent e) {
    if (setTarget) {
      if (tracker == null) {
        warningLabel.setText("Warning: Image not loaded!");
        return;
      }

      try {
        BufferedImage image = ImageIO.read(new File(
            getClass().getResource("/images/frame_0001.png").toExternalForm().substring(6)));

        double Wratio = image.getWidth() / imageView.getFitWidth();
        double Hratio = image.getHeight() / imageView.getFitHeight();

        int color = image.getRGB((int) (e.getX() * Wratio), (int) (e.getY() * Hratio));
        this.tracker.color = color;

        imageView.setCursor(Cursor.DEFAULT);

        int red = (color & 0x00FF0000) >> 16;
        int green = (color & 0x0000FF00) >> 8;
        int blue = (color & 0x000000FF);
        warningLabel.setText("Tracking: rgb(" + red + ", " + green + ", " + blue + ")");
      } catch (Exception ex) {
        warningLabel.setText("Warning: Error loading image!");
        ex.printStackTrace();
        return;
      }

      setTarget = false;
    } else if (setScale) {
      warningLabel.setText("Status: Point " + (scalePoints + 1) + "- (" + e.getX() + ", " + e.getY() + ")");
      points[scalePoints] = new int[] { (int) e.getX(), (int) e.getY() };
      scalePoints += 1;

      if (scalePoints == 2) {
        tracker.ratio = Math
            .sqrt(Math.pow(points[1][0] - points[0][0], 2) + Math.pow(points[1][1] - points[0][1], 2));
        setScale = false;
        scalePoints = 0;
        imageView.setCursor(Cursor.DEFAULT);
      }
    }
  }

  /**
   * Function to load a new video file and preprocess it by splitting it into frames
   * 
   * @param stage the main stage to draw the application in
   */
  private void loadImage(Stage stage) {
    Tracker.flushFrames();

    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Open Video File");
    File file = fileChooser.showOpenDialog(stage);

    if (file != null) {
      tracker = new Tracker(file.toPath().toString());

      if (tracker.deconstruct()) {
        Image image = new Image(
            getClass().getResource("/images/frame_0001.png").toExternalForm());
        imageView.setImage(image);
        warningLabel.setText("Status: Video loaded");
      } else {
        warningLabel.setText("Warning: Error loading video file");
      }
    }
  }

  /**
   * Function to process each frame of the image and reconstruct it into a video and save it
   * 
   * @param stage the main stage to draw the application in
   */
  private void processImage(Stage stage) {
    try {
      DirectoryChooser dc = new DirectoryChooser();
      File f = dc.showDialog(stage);
      if (f != null) {
        warningLabel.setText("Status: Processing image");
        tracker.trackPoint();

        warningLabel.setText("Status: Image Processed. Reconstructing video");
        tracker.reconstruct(f.toPath().toString());

        warningLabel.setText("Status: Video Reconstructed");
      } else {
        warningLabel.setText("Status: Directory does not exist");
      }
    } catch (IllegalStateException e) {
      warningLabel.setText("Status: Error finding color in frame");
    }
  }

  /**
   * Function to activate setScale mode
   */
  private void setScale() {
    setScale = true;
    warningLabel.setText("Status: Setting scale. Click on either end of the track");
    imageView.setCursor(Cursor.CROSSHAIR);
  }

  /**
   * Function to activate setTarget mode
   */
  private void setTarget() {
    imageView.setCursor(Cursor.CROSSHAIR);
    setTarget = true;
  }

  /**
   * Function main
   * 
   * @param args program arguments
   */
  public static void main(String[] args) {
    launch(args);
  }
}
