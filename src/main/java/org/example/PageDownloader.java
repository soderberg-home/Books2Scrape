package org.example;

import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PageDownloader implements Runnable {

    private final String URI;
    private final String parentFolder;

    public PageDownloader(String URI, String parentFolderForOutput) {
        this.URI = URI;
        this.parentFolder = parentFolderForOutput;
    }

    @Override
    public void run() {
        ExecutorService executor = Executors.newCachedThreadPool(); // For asynchronous tasks

        try {
            CompletableFuture<String> htmlFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    // Set timeouts to avoid hanging indefinitely
                    Connection connection = Jsoup.connect(URI).timeout(10_000); // 10 seconds timeout
                    return connection.get().html(); // Fetch HTML content asynchronously
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, executor);

            String html = htmlFuture.get(); // Wait for the HTML content to be fetched
            String fileName = URI.replace("http://", parentFolder); // Construct file name
            Path pathToFile = Paths.get(fileName); // Get the path to the file

            // Ensure parent directories exist
            if (!Files.exists(pathToFile.getParent())) {
                Files.createDirectories(pathToFile.getParent());
            }

            // Write HTML content to file
            try (BufferedWriter writer = Files.newBufferedWriter(pathToFile)) {
                writer.write(html); // Use BufferedWriter for large files
            }
        } catch (IOException | ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            executor.shutdown(); // Always remember to shut down the executor
        }
    }
}
