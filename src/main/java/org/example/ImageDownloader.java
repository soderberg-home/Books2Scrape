package org.example;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
public class ImageDownloader implements Runnable {

    private final String URI;
    private final String parentFolder;

    public ImageDownloader(String URI,String parentFolderForOutput) {
        this.URI = URI;
        this.parentFolder = parentFolderForOutput;
    }
    @Override
    public void run() {
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        executor.execute(() -> {
            try {
                URL imageUrl = new URL(URI);
                String pathString = this.URI.replace("http://", parentFolder);
                Path pathToFile = Paths.get(pathString);

                // Ensure parent directories exist
                Files.createDirectories(pathToFile.getParent());

                // Open a stream to download the image
                URLConnection connection = imageUrl.openConnection();
                connection.setConnectTimeout(5000); // 5 seconds timeout
                connection.setReadTimeout(5000);

                try (InputStream in = new BufferedInputStream(connection.getInputStream())) {
                    // Read the image from the stream
                    BufferedImage image = ImageIO.read(in);
                    if (image == null) {
                        throw new IOException("The image cannot be decoded");
                    }

                    // Write the image to a file
                    File outputFile = pathToFile.toFile();
                    ImageIO.write(image, "jpg", outputFile);
                }
            } catch (IOException e) {
                e.printStackTrace(); //TODO: improve with something more robust
            }
        });

        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Preserve interrupt status
        }
    }
}
