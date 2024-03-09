package org.example;

import org.jsoup.Jsoup;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

public class PageDownloader implements Runnable {

    private final String URI;

    public PageDownloader(String URI) {
        this.URI = URI;
    }

    @Override
    public void run() {
        try {
            String html = Jsoup.connect(URI).get().html(); // Fetch HTML content
            String fileName = URI.replace("https://", "/Users/fredriksoderberg/"); // Construct file name
            Path pathToFile = Paths.get(fileName); // Get the path to the file

            // Ensure parent directories exist
            if (!Files.exists(pathToFile.getParent())) {
                Files.createDirectories(pathToFile.getParent());
            }

            // Write HTML content to file
            Files.write(pathToFile, Collections.singleton(html)); // Use Files.write for simplicity and efficiency
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
