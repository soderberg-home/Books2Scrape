package org.example;


import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ImageDownloader implements Runnable {

    private final String URI;

    public ImageDownloader(String URI) {
        this.URI = URI;
    }
    @Override
    public void run() {
        try {
            URL imageUrl = new URL(URI);

            String fileName = this.URI.replace("https://","/Users/fredriksoderberg/");
            Path pathToFile = Paths.get(fileName);
            if(!Files.exists(pathToFile.getParent())){
                try {
                    Files.createDirectories(pathToFile.getParent());
                } catch (IOException e) {
                    System.out.println(e);
                }
            }
            // Open a stream to download the image
            try (InputStream in = imageUrl.openStream()) {
                // Read the image from the stream
                BufferedImage image = ImageIO.read(in);

                // Output file path
                File outputFile = new File(fileName);

                // Write the image to a file in JPEG format
                ImageIO.write(image, "jpg", outputFile);

                //System.out.println("Image downloaded successfully: " + outputFile.getPath());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
