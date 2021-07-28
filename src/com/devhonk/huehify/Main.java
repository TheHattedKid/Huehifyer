package com.devhonk.huehify;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class Main extends Application {

    //GET READY TO SEE MY BAD CODE
    //NO BUT REALLY
    //YOU HATH BEEN WARNED
    //now i shall add this long
    //text to prevent you from watching the code
    //you still very much can tho
    //i use manjaro btw
    //wait a sec imports do the same thing
    //that i want to do.
    //LMAO


    java.util.List<String> input;
    String output;
    public TextField width;
    public TextField height;

    //Source: Stackoverflow
    private static BufferedImage getScaledImage(Image srcImg, int w, int h) {

        //Create a new image with good size that contains or might contain arbitrary alpha values between and including 0.0 and 1.0.
        BufferedImage resizedImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        //Create a device-independant object to draw the resized image
        Graphics2D g2 = resizedImg.createGraphics();

        //This could be changed, Cf. http://stackoverflow.com/documentation/java/5482/creating-images-programmatically/19498/specifying-image-rendering-quality
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        //Finally draw the source image in the Graphics2D with the desired size.
        g2.drawImage(srcImg, 0, 0, w, h, null);

        //Disposes of this graphics context and releases any system resources that it is using
        g2.dispose();

        //Return the image used to create the Graphics2D
        return resizedImg;
    }

    public static void conv(String in, int w, int h, String outF, String paletteFolder) throws IOException {
        BufferedImage bi = ImageIO.read(new File(in));

        BufferedImage scaledImg = getScaledImage(bi, w, h);
        BufferedImage out = new BufferedImage(w*60, h*40, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g = (Graphics2D) out.getGraphics();

        for (int y = 0; y < scaledImg.getHeight(); y++) {
            for (int x = 0; x < scaledImg.getWidth(); x++) {
                Color oc = new Color(scaledImg.getRGB(x, y));
                double avg = ((oc.getRed() + oc.getBlue() + oc.getGreen()) / 3.0) / 255.0;
                int c = (int) Math.floor(avg * (6 - 1));
                g.drawImage(ImageIO.read(new File(paletteFolder + c + ".png")), x*60, y*40, null);
            }
        }
        ImageIO.write(out, "PNG", new File(outF));
    }
    Stage primaryStage;
    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;
        Parent root = FXMLLoader.load(getClass().getResource("hueh.fxml"));
        primaryStage.setTitle("Huehfyer");

        primaryStage.setScene(new Scene(root, 265, 295));
        primaryStage.setResizable(false);
        primaryStage.getIcons().add(new javafx.scene.image.Image(Main.class.getResourceAsStream("hueh.png")));
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);

    }

    @FXML
    public void huehify(ActionEvent actionEvent) {
        new Thread(() -> {
            Platform.runLater(() -> {
                if (!new File(System.getProperty("user.home") + File.separatorChar + ".huehs").isDirectory()) {
                    System.out.println("Hueh folder does not exist! Creating one...");
                    new File(System.getProperty("user.home") + File.separatorChar + ".huehs").mkdirs();
                    try {
                        for (int i = 0; i < 6; i++) {
                            //you may do a custom thing later on
                            BufferedImage bi = ImageIO.read(new URL("https://devhonk.github.io/huehs/" + i + ".png"));
                            ImageIO.write(
                                    bi,
                                    "PNG",
                                    new File(
                                            System.getProperty("user.home") +
                                                    File.separatorChar +
                                                    ".huehs" +
                                                    File.separatorChar +
                                                    i +
                                                    ".png"
                                    )
                            );
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                int w = Integer.parseInt(width.getText());
                int h = Integer.parseInt(height.getText());
                try {
                    if (input.size() == 1) {
                        conv(input.get(0), w, h, output,
                                System.getProperty("user.home") +
                                        File.separatorChar +
                                        ".huehs" +
                                        File.separatorChar);
                    } else {
                        for (int i = 0; i < input.size(); i++) {
                            conv(input.get(i), w, h, new File(output).getParentFile().getPath() + File.separatorChar + (new File(output).getName().split("\\.[^\\.]")[0] + i) + (new File(output).getName().endsWith(".png") || new File(output).getName().endsWith(".PNG") ? "" : ".png"),
                                    System.getProperty("user.home") +
                                            File.separatorChar +
                                            ".huehs" +
                                            File.separatorChar);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }, "Output").start();
    }

    public void outSelect(ActionEvent actionEvent) {
        final FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG", "*.png", "*.PNG"));
        File file = fileChooser.showSaveDialog(primaryStage);
        if (file != null) {
            java.util.List<File> files = Collections.singletonList(file);
            output = files.get(0).getPath();
        }
    }

    public void inSelect(ActionEvent actionEvent) {
        final FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image files",
                "*.png", "*.PNG",
                "*.jpg", "*.JPG", "*.jpeg", "*.JPEG",
                "*.gif", "*.GIF",
                "*.bmp", "*.BMP"
        ));
        java.util.List<File> files = fileChooser.showOpenMultipleDialog(null);
        if (files != null) {
            input = new ArrayList<>();
            for (int i = 0; i < files.size(); i++) {
                input.add(files.get(i).getPath());
            }
        }
    }
}
