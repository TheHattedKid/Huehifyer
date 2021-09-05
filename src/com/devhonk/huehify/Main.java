package com.devhonk.huehify;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;


import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JFrame;

class Dither {

	static class C3 {
		int r, g, b;

		public C3(int c) {
			Color color = new Color(c);
			r = color.getRed();
			g = color.getGreen();
			b = color.getBlue();
		}

		public C3(int r, int g, int b) {
			this.r = r;
			this.g = g;
			this.b = b;
		}

		public C3 add(C3 o) {
			return new C3(r + o.r, g + o.g, b + o.b);
		}

		public int clamp(int c) {
			return Math.max(0, Math.min(255, c));
		}

		public int diff(C3 o) {
			int Rdiff = o.r - r;
			int Gdiff = o.g - g;
			int Bdiff = o.b - b;
			int distanceSquared = Rdiff * Rdiff + Gdiff * Gdiff + Bdiff * Bdiff;
			return distanceSquared;
		}

		public C3 mul(double d) {
			return new C3((int) (d * r), (int) (d * g), (int) (d * b));
		}

		public C3 sub(C3 o) {
			return new C3(r - o.r, g - o.g, b - o.b);
		}

		public Color toColor() {
			return new Color(clamp(r), clamp(g), clamp(b));
		}

		public int toRGB() {
			return toColor().getRGB();
		}
	}

	private static C3 findClosestPaletteColor(C3 c, C3[] palette) {
		C3 closest = palette[0];

		for (C3 n : palette) {
			if (n.diff(c) < closest.diff(c)) {
				closest = n;
			}
		}

		return closest;
	}

	static BufferedImage floydSteinbergDithering(BufferedImage img) {

		C3[] palette = new C3[]{
				new C3(0, 0, 0), // black
				new C3(0, 0, 255), // green
				new C3(0, 255, 0), // blue
				new C3(0, 255, 255), // cyan
				new C3(255, 0, 0), // red
				new C3(255, 0, 255), // purple
				new C3(255, 255, 0), // yellow
				new C3(255, 255, 255)  // white
		};

		int w = img.getWidth();
		int h = img.getHeight();

		C3[][] d = new C3[h][w];

		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				d[y][x] = new C3(img.getRGB(x, y));
			}
		}

		for (int y = 0; y < img.getHeight(); y++) {
			for (int x = 0; x < img.getWidth(); x++) {

				C3 oldColor = d[y][x];
				C3 newColor = findClosestPaletteColor(oldColor, palette);
				img.setRGB(x, y, newColor.toColor().getRGB());

				C3 err = oldColor.sub(newColor);

				if (x + 1 < w) {
					d[y][x + 1] = d[y][x + 1].add(err.mul(7. / 16));
				}

				if (x - 1 >= 0 && y + 1 < h) {
					d[y + 1][x - 1] = d[y + 1][x - 1].add(err.mul(3. / 16));
				}

				if (y + 1 < h) {
					d[y + 1][x] = d[y + 1][x].add(err.mul(5. / 16));
				}

				if (x + 1 < w && y + 1 < h) {
					d[y + 1][x + 1] = d[y + 1][x + 1].add(err.mul(1. / 16));
				}
			}
		}

		return img;
	}
}
public class Main extends Application {
	static boolean video = false;
	static boolean color = false;
	public ChoiceBox mode;
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

    static String ffmpeg = "";
    static String ffprobe = "";

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
	public static void convMono(String in, int w, int h, String outF) throws IOException {
		BufferedImage bi = ImageIO.read(new File(in));

		BufferedImage scaledImg = getScaledImage(bi, w, h);
		BufferedImage out = new BufferedImage(w*60, h*40, BufferedImage.TYPE_INT_ARGB);

		Graphics2D g = (Graphics2D) out.getGraphics();

		for (int y = 0; y < scaledImg.getHeight(); y++) {
			for (int x = 0; x < scaledImg.getWidth(); x++) {
				Color oc = new Color(scaledImg.getRGB(x, y));
				double avg = ((oc.getRed() + oc.getBlue() + oc.getGreen()) / 3.0) / 255.0;
				int c = (int) Math.floor(avg * (6 - 1));
				g.drawImage(ImageIO.read(new File(System.getProperty("user.home") +
						File.separatorChar +
						".huehs" +
						File.separatorChar + c + ".png")), x*60, y*40, null);
			}
		}
		ImageIO.write(out, "PNG", new File(outF));
	}
    public static void conv(String in, int w, int h, String outF, boolean color) throws IOException {
    	if(!color) { convMono(in, w, h, outF); return; }
        System.out.println("In: " + in);
        BufferedImage bi = ImageIO.read(new File(in));

        BufferedImage scaledImg = Dither.floydSteinbergDithering(getScaledImage(bi, w, h));
        BufferedImage out = new BufferedImage(w*60, h*40, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g = (Graphics2D) out.getGraphics();

        for (int y = 0; y < scaledImg.getHeight(); y++) {
            for (int x = 0; x < scaledImg.getWidth(); x++) {
                Color oc = new Color(scaledImg.getRGB(x, y));
                boolean re = oc.getRed() > 127;
                boolean gr = oc.getGreen() > 127;
                boolean bl = oc.getBlue() > 127;
                String c = String.format("%d%d%d", re ? 1 : 0, gr ? 1 : 0, bl ? 1 : 0);
                g.drawImage(ImageIO.read(new File(System.getProperty("user.home") +
		                File.separatorChar +
		                ".huehs" +
		                File.separatorChar +
		                "colors" +
		                File.separatorChar + c + ".png")), x*60, y*40, null);
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
	public static String findExecutableOnPath(String name) {
		for (String dirname : System.getenv("PATH").split(File.pathSeparator)) {
			File file = new File(dirname, name);
			if (file.isFile() && file.canExecute()) {
				return file.getAbsolutePath();
			}
		}
		System.err.println("Oops, FFMPEG isnt here????");
		throw new IllegalArgumentException();
	}
    public static void main(String[] args) throws IOException {
    	//black magic code that is totally supposed to run both on windows or linux
	    ffmpeg = findExecutableOnPath("ffmpeg" + (System.getProperty("os.name").contains("win") ? ".exe" : ""));
        /*for (int i = 0; i < 8; i++) {

            String path = "/home/hatkid/Bureau/grayhueh/color/" + String.format("%3s", Integer.toBinaryString(i)).replace(" ", "0") + ".png";
            System.out.println(path);
            ImageIO.write(getScaledImage(ImageIO.read(new File(path)), 60, 40), "PNG", new File(path));
        }*/
        launch(args);

    }

    @FXML
    public void huehify(ActionEvent actionEvent) {
        new Thread(() -> {
            Platform.runLater(() -> {
                if (!new File(System.getProperty("user.home") + File.separatorChar + ".huehs" + File.separatorChar + "colors").isDirectory()) {
                    System.out.println("Hueh folder does not exist! Creating one...");
                    new File(System.getProperty("user.home") + File.separatorChar + ".huehs" + File.separatorChar + "colors").mkdirs();
                    try {
                        for (int i = 0; i < 8; i++) {
                            //you may do a custom thing later on
                            BufferedImage bi = ImageIO.read(new URL("https://devhonk.github.io/huehs/colors/" + String.format("%3s", Integer.toBinaryString(i)).replace(" ", "0") + ".png"));
                            ImageIO.write(
                                    bi,
                                    "PNG",
                                    new File(
                                            System.getProperty("user.home") +
                                                    File.separatorChar +
                                                    ".huehs" +
                                                    File.separatorChar +
                                                    "colors" +
                                                    File.separatorChar +
                                                    String.format("%3s", Integer.toBinaryString(i)).replace(" ", "0") +
                                                    ".png"
                                    )
                            );
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
	            if(!video) conv();
                else {
                	//Step 0: Making temporary file
		            File folder = null;
		            try {
			            folder = Files.createTempDirectory("huehifyer").toFile();
		            } catch (IOException e) {
			            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
			            errorAlert.setHeaderText("Cannot create temporary folder");
			            errorAlert.setContentText("Cannot make a temporary file for video mode. Exiting");
			            errorAlert.showAndWait();
			            System.exit(1);
		            }
		            //Step 1: extracting frames
		            Process p = null;
		            try {
			            System.out.printf("%s/%%d.png\n", folder.getPath());
			            p = Runtime.getRuntime().exec(String.format("%s -r 30 -i %s %s/%%d.png -hide_banner", ffmpeg, input.get(0), folder.getPath()));
			            String result = new BufferedReader(new InputStreamReader(p.getErrorStream()))
					            .lines().collect(Collectors.joining("\n"));
			            System.out.println(result);
		            } catch (IOException e) {
			            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
			            errorAlert.setHeaderText("Cannot create temporary file");
			            errorAlert.setContentText("Cannot make a temporary file for video mode. Exiting");
			            errorAlert.showAndWait();
			            System.exit(1);
		            }
		            try {
			            int status = p.waitFor();
			            if(status != 0) {
				            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
				            errorAlert.setHeaderText("FFMPEG exit code is non-null");
				            errorAlert.setContentText("Oops. Check out the exit error to know the FFMPEG exit error. Exiting");
				            errorAlert.showAndWait();
				            System.exit(status);
			            }
		            } catch (InterruptedException e) {
			            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
			            errorAlert.setHeaderText("Cannot retrieve FFMPEG exit code");
			            errorAlert.setContentText("Cannot retrieve FFMPEG exit code. Exiting");
			            errorAlert.showAndWait();
			            System.exit(1);
		            }
		            int frames = folder.list().length;
		            //step 2: convert all frames into hueh
		            for (int i = 0; i < frames; i++) {
			            try {
				            conv(String.format("%s/%d.png", folder.getPath(), i + 1), Integer.parseInt(width.getText()), Integer.parseInt(height.getText()), String.format("%s/hueh-%d.png", folder.getPath(), i + 1), color);
				            new File(String.format("%s/%d.png", folder.getPath(), i + 1)).delete();
			            } catch (IOException e) {
				            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
				            errorAlert.setHeaderText("Cannot huehify a frame");
				            errorAlert.setContentText("You may have not enough disk space for that. Exiting...");
				            errorAlert.showAndWait();
				            System.exit(i + 1);
			            }
		            }
		            //step 3: convert all huehs into video
		            try {
			            p = Runtime.getRuntime().exec(String.format("%s -i %s/hueh-%%d.png -vcodec libx264 -crf 25  -pix_fmt yuv420p %s", ffmpeg, folder.getPath(), output));
			            String result = new BufferedReader(new InputStreamReader(p.getErrorStream()))
					            .lines().collect(Collectors.joining("\n"));
			            System.out.println(result);
		            } catch (IOException e) {
			            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
			            errorAlert.setHeaderText("Cannot create temporary file");
			            errorAlert.setContentText("Cannot make a temporary file for video mode. Exiting");
			            errorAlert.showAndWait();
			            System.exit(1);
		            }
		            folder.delete();
	            }
            });
        }, "Output").start();
        
    }
    public void conv() {
	    int w = Integer.parseInt(width.getText());
	    int h = Integer.parseInt(height.getText());
	    try {
		    if (input.size() == 1) {
			    conv(input.get(0), w, h, output, color);
		    } else {
			    for (int i = 0; i < input.size(); i++) {
				    conv(input.get(i), w, h, new File(output).getParentFile().getPath() + File.separatorChar + (new File(output).getName().split("\\.[^\\.]")[0] + i) + (new File(output).getName().endsWith(".png") || new File(output).getName().endsWith(".PNG") ? "" : ".png"), color);
			    }
		    }
	    } catch (IOException e) {
		    e.printStackTrace();
	    }
    }

    public void outSelect(ActionEvent actionEvent) {
        final FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(!video ? new FileChooser.ExtensionFilter("PNG", "*.png", "*.PNG") : new FileChooser.ExtensionFilter("Video", "*.mp4", "*.MP4"));
        File file = fileChooser.showSaveDialog(primaryStage);
        if (file != null) {
            java.util.List<File> files = Collections.singletonList(file);
            output = files.get(0).getPath();
        }
    }

    public void inSelect(ActionEvent actionEvent) {
    	video = false;
        final FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image files",
                "*.png", "*.PNG",
                "*.jpg", "*.JPG", "*.jpeg", "*.JPEG",
                "*.gif", "*.GIF",
                "*.bmp", "*.BMP"
        ));
	    fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Videos",
			    "*.mp4", "*.MP4",
			            "*.mov", "*.MOV",
			            "*.mkv", "*.MKV"
	    ));
        java.util.List<File> files = fileChooser.showOpenMultipleDialog(null);
        if (files != null) {
            input = new ArrayList<>();
	        if(files.get(0).getPath().toLowerCase().endsWith(".mp4") || files.get(0).getPath().toLowerCase().endsWith(".mov") || files.get(0).getPath().toLowerCase().endsWith(".mkv"))
		        video = true;

	        for (int i = 0; i < files.size(); i++) {
	        	input.add(files.get(i).getPath());
	        }
        }
    }
	int c = 0;
	@FXML
	public void initialize() throws Exception {
		mode.getItems().addAll("Monochrome", "Color");
		mode.setOnAction((a) -> {
			color = mode.getValue().equals("Color");
		});
	}
}
