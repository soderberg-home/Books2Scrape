package org.example;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class Books2ScrapeDriver {

    public static void main(String[] args) throws FileAlreadyExistsException {

        if (args.length != 1) {
            // Inform the user about the correct usage
            System.err.println("Usage: java MainClass <OUTPUT-DIRECTORY-PATH>");
            System.exit(1);
        }

        String outputDirName = args[0];
        if(!outputDirName.endsWith("/")){
            outputDirName = outputDirName + "/";
        }

        Path outputPath = Paths.get(outputDirName);

        if (!Files.exists(outputPath)) {
            try {
                Files.createDirectories(outputPath);
                System.out.println("Storing files in: " + outputDirName);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        } else {
            throw new FileAlreadyExistsException("File at: " + outputDirName + " already exists.");
        }

        long startTime, endTime, duration;
        startTime = System.currentTimeMillis();
        System.out.println("Performing paging...");
        PagerScanner pagerScanner = new PagerScanner(URI.create("http://books.toscrape.com"));
        Thread scanThread = new Thread(pagerScanner);
        scanThread.start();
        try {
            scanThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Initial scanning thread was interrupted.");
            return;
        }
        endTime = System.currentTimeMillis();
        duration = endTime - startTime;
        System.out.println("Paging link scanning completed in " + duration + " ms");

        // Page link scanning phase
        List<PageLinkScanner> pageLinkScannerList;
        try (ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())) {
            pageLinkScannerList = new ArrayList<>();
            for (String page : pagerScanner.scannedPages()) {
                PageLinkScanner pageLinkScanner = new PageLinkScanner(page);
                pageLinkScannerList.add(pageLinkScanner);
                threadPoolExecutor.execute(pageLinkScanner);
            }
            threadPoolExecutor.shutdown();
            monitorProgress(threadPoolExecutor);
        }
        endTime = System.currentTimeMillis();
        duration = endTime - startTime;
        System.out.println("Page link scanning completed after " + duration + " ms");

        // Page downloading phase
        System.out.println("Downloading pages...");
        try (ThreadPoolExecutor downloadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())) {
            for (String page : pagerScanner.scannedPages()) {
                PageDownloader pageDownloader = new PageDownloader(page, outputDirName);
                downloadPoolExecutor.execute(pageDownloader);
            }
            downloadPoolExecutor.shutdown();
            monitorProgress(downloadPoolExecutor);
        }
        endTime = System.currentTimeMillis();
        duration = endTime - startTime;
        System.out.println("Page downloading completed after " + duration + " ms");
        // Resource downloading phase (HTML and images)
        System.out.println("Downloading resources...");
        try (ThreadPoolExecutor downloadPoolExecutor2 = (ThreadPoolExecutor) Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())) {
            for (PageLinkScanner pageLinkScanner : pageLinkScannerList) {
                for (String scanned : pageLinkScanner.scannedHTMLLinks()) {
                    PageDownloader pageDownloader = new PageDownloader(scanned, outputDirName);
                    downloadPoolExecutor2.execute(pageDownloader);
                }
                for (String cssResources : pageLinkScanner.scannedCSSLinks()) {
                    PageDownloader pageDownloader = new PageDownloader(cssResources, outputDirName);
                    downloadPoolExecutor2.execute(pageDownloader);
                }
                for (String image : pageLinkScanner.scannedImageLinks()) {
                    ImageDownloader imageDownloader = new ImageDownloader(image, outputDirName);
                    downloadPoolExecutor2.execute(imageDownloader);
                }
            }
            downloadPoolExecutor2.shutdown();
            monitorProgress(downloadPoolExecutor2);
        }

        endTime = System.currentTimeMillis();
        duration = endTime - startTime;
        System.out.println("Process completed after " + duration + " ms");
    }

    private static void monitorProgress(ThreadPoolExecutor executor) {
        double numberOfTasks = 1.0 * executor.getTaskCount();
        long lastReportedPercent = -1;

        while (!executor.isTerminated()) {
            long tasksFinished = executor.getCompletedTaskCount();
            long percent = (long) Math.floor(100 * tasksFinished / numberOfTasks);

            if (percent != lastReportedPercent) {
                System.out.println("Progress: " + percent + "%");
                lastReportedPercent = percent;
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Monitoring thread was interrupted.");
                return; // Exit the function
            }
        }
    }
}

