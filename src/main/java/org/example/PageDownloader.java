package org.example;

import org.jsoup.Jsoup;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PageDownloader implements Runnable {

    private String URI;

    public PageDownloader(String URI) {
        this.URI = URI;
    }

    @Override
    public void run() {
        String html = null;
        try {
            html = Jsoup.connect(URI).get().html();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String fileName = this.URI.replace("https://","/Users/fredriksoderberg/");
        Path pathToFile = Paths.get(fileName);
        System.out.println(pathToFile);

        if(!Files.exists(pathToFile.getParent())){
            try {
                Files.createDirectories(pathToFile.getParent());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(fileName));
            writer.write(html);
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
