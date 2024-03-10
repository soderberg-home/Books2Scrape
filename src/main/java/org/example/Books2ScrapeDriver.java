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
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class Books2ScrapeDriver {

    private static final Logger LOGGER = Logger.getLogger(Books2ScrapeDriver.class.getName());

    public static void main(String[] args) throws FileAlreadyExistsException {

        if (args.length != 1) {
            LOGGER.severe("Usage: java MainClass <OUTPUT-DIRECTORY-PATH>");
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
                LOGGER.info("Storing files in: " + outputDirName);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        } else {
            throw new FileAlreadyExistsException("File at: " + outputDirName + " already exists.");
        }

        long startTime, endTime, duration;
        startTime = System.currentTimeMillis();
        LOGGER.info("Performing paging...");
        PagerScanner pagerScanner = new PagerScanner(URI.create("http://books.toscrape.com"));
        Thread scanThread = new Thread(pagerScanner);
        scanThread.start();
        try {
            scanThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.severe("Initial scanning thread was interrupted.");
            return;
        }
        endTime = System.currentTimeMillis();
        duration = endTime - startTime;
        LOGGER.info("Paging link scanning completed in " + duration + " ms");

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
        LOGGER.info("Page link scanning completed after " + duration + " ms");

        // Page downloading phase
        LOGGER.info("Downloading pages...");
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
        LOGGER.info("Page downloading completed after " + duration + " ms");

        Semaphore semaphore = new Semaphore(32);
        LOGGER.info("Downloading resources...");
        List<Thread> threads = new ArrayList<>();
        AtomicInteger completedTasks = new AtomicInteger(0);
        AtomicInteger printCounter = new AtomicInteger(0);

        int totalTasks = pageLinkScannerList.stream().mapToInt(pls -> pls.scannedHTMLLinks().size() + pls.scannedCSSLinks().size() + pls.scannedImageLinks().size()).sum();
        int printFrequency = totalTasks / 100; // Adjust this value to control the frequency of progress output

        for (PageLinkScanner pageLinkScanner : pageLinkScannerList) {
            for (String scanned : pageLinkScanner.scannedHTMLLinks()) {
                PageDownloader pageDownloader = new PageDownloader(scanned, outputDirName);
                Thread thread = Thread.ofVirtual().start(() -> {
                    try {
                        semaphore.acquire();
                        pageDownloader.run();
                        completedTasks.incrementAndGet();
                        if (printCounter.incrementAndGet() % printFrequency == 0) {
                            System.out.println("Progress: " + (100 * completedTasks.get() / totalTasks) + "%");
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        LOGGER.severe("Resource downloading thread was interrupted.");
                    } finally {
                        semaphore.release();
                    }
                });
                threads.add(thread);
            }
            for (String cssResources : pageLinkScanner.scannedCSSLinks()) {
                PageDownloader pageDownloader = new PageDownloader(cssResources, outputDirName);
                Thread thread = Thread.ofVirtual().start(() -> {
                    try {
                        semaphore.acquire();
                        pageDownloader.run();
                        completedTasks.incrementAndGet();
                        if (printCounter.incrementAndGet() % printFrequency == 0) {
                            System.out.println("Progress: " + (100 * completedTasks.get() / totalTasks) + "%");
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        LOGGER.severe("Resource downloading thread was interrupted.");
                    } finally {
                        semaphore.release();
                    }
                });
                threads.add(thread);
            }
            for (String image : pageLinkScanner.scannedImageLinks()) {
                ImageDownloader imageDownloader = new ImageDownloader(image, outputDirName);
                Thread thread = Thread.ofVirtual().start(() -> {
                    try {
                        semaphore.acquire();
                        imageDownloader.run();
                        completedTasks.incrementAndGet();
                        if (printCounter.incrementAndGet() % printFrequency == 0) {
                            System.out.println("Progress: " + (100 * completedTasks.get() / totalTasks) + "%");
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        LOGGER.severe("Resource downloading thread was interrupted.");
                    } finally {
                        semaphore.release();
                    }
                });
                threads.add(thread);
            }
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.severe("Resource downloading thread was interrupted.");
            }
        }

        endTime = System.currentTimeMillis();
        duration = endTime - startTime;
        LOGGER.info("Process completed after " + duration + " ms");
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
                LOGGER.severe("Monitoring thread was interrupted.");
                return; // Exit the function
            }
        }
    }
}
