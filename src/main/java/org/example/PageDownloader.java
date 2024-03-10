package org.example;

import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        ExecutorService executor = Executors.newCachedThreadPool();
        try {
            CompletableFuture<String> htmlFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    // Set timeouts
                    Connection connection = Jsoup.connect(URI).timeout(10_000);
                    return connection.get().html();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, executor);

            String html = htmlFuture.get();
            String fileName = URI.replace("http://", parentFolder); // Construct file name
            Path pathToFile = Paths.get(fileName); // Get the path to the file

            // Ensure parent directories exist
            if (!Files.exists(pathToFile.getParent())) {
                Files.createDirectories(pathToFile.getParent());
            }

            // Write HTML content to file
            try (BufferedWriter writer = Files.newBufferedWriter(pathToFile)) {
                writer.write(html);
            }
        } catch (IOException | ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            executor.shutdown();
        }
    }
}
