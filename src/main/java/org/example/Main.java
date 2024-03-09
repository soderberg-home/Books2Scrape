package org.example;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import static java.lang.Thread.sleep;

public class Main {

    public static void main(String[] args) {

        System.out.println("Performing paging...");
        PagerScanner pagerScanner = new PagerScanner(URI.create("https://books.toscrape.com"));
        Thread scanThread = new Thread(pagerScanner);
        scanThread.start();
        try {
            scanThread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        List<PageLinkScanner> pageLinkScannerList;

        try (ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(8)) {
            pageLinkScannerList = new ArrayList<>();
            // Submit tasks to the thread pool
            for (String page : pagerScanner.scannedPages()) {
                PageLinkScanner pageLinkScanner = new PageLinkScanner(page);
                pageLinkScannerList.add(pageLinkScanner);
                threadPoolExecutor.execute(pageLinkScanner);
            }
            threadPoolExecutor.shutdown(); // Initiate shutdown
            double numberOfTasks = threadPoolExecutor.getTaskCount();
            long lastReportedPercent = -1; // Track the last reported progress

            // Monitor and report progress
            while (!threadPoolExecutor.isTerminated()) {
                long tasksFinished = threadPoolExecutor.getCompletedTaskCount();
                long percent = (long) Math.floor(100 * tasksFinished / numberOfTasks);

                // Report progress only if it has changed
                if (percent != lastReportedPercent) {
                    System.out.println("Progress: " + percent + "%");
                    lastReportedPercent = percent;
                }

                // Pause to reduce frequent checking
                try {
                    Thread.sleep(1000); // Sleep for 1 second
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // Preserve interrupt status
                    System.err.println("Monitoring thread was interrupted.");
                    break; // Exit the monitoring loop
                }
            }
        }
        System.out.println("Finding links...");
        try (ThreadPoolExecutor downloadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(8)) {
            // Submit download tasks for each page
            for (String page : pagerScanner.scannedPages()) {
                PageDownloader pageDownloader = new PageDownloader(page);
                downloadPoolExecutor.execute(pageDownloader);
            }
            downloadPoolExecutor.shutdown(); // Initiate shutdown of the pool

            double numberOfTasks = downloadPoolExecutor.getTaskCount();
            long lastReportedPercent = -1; // To track the last reported percentage

            // Monitor the progress and report percentage
            while (!downloadPoolExecutor.isTerminated()) {
                long tasksFinished = downloadPoolExecutor.getCompletedTaskCount();
                long percent = (long) Math.floor(100 * tasksFinished / numberOfTasks);

                // Only print the percentage if it has changed since the last print
                if (percent != lastReportedPercent) {
                    System.out.println(percent + "% completed");
                    lastReportedPercent = percent; // Update the last reported percentage
                }

                // Sleep briefly to reduce the frequency of updates
                try {
                    Thread.sleep(100); // Sleep for 100 milliseconds
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // Restore the interrupted status
                    throw new RuntimeException("Thread was interrupted", e);
                }
            }
        }
        System.out.println("");
        System.out.println("Downloading resources...");

        try (ThreadPoolExecutor downloadPoolExecutor2 = (ThreadPoolExecutor) Executors.newFixedThreadPool(8)) {
            // Submitting tasks for downloading pages and images
            for (PageLinkScanner pageLinkScanner : pageLinkScannerList) {
                for (String scanned : pageLinkScanner.scannedHTMLLinks()) {
                    PageDownloader pageDownloader = new PageDownloader(scanned);
                    downloadPoolExecutor2.execute(pageDownloader);
                }
                for (String image : pageLinkScanner.scannedImageLinks()) {
                    ImageDownloader imageDownloader = new ImageDownloader(image);
                    downloadPoolExecutor2.execute(imageDownloader);
                }
            }
            downloadPoolExecutor2.shutdown();

            // Tracking and displaying progress
            double numberOfTasks2 = 1.0 * downloadPoolExecutor2.getTaskCount();
            long lastPrintedPercent = -1;  // Initialize with an impossible value to ensure the first update is printed

            while (!downloadPoolExecutor2.isTerminated()) {
                long tasksFinished = downloadPoolExecutor2.getCompletedTaskCount();
                long percent = (long) Math.floor(100 * tasksFinished / numberOfTasks2);

                if (percent != lastPrintedPercent) {  // Only print if the percent has changed
                    System.out.println("Progress: " + percent + "%");
                    lastPrintedPercent = percent;  // Update last printed percent
                }

                // Throttle the loop to reduce CPU usage
                try {
                    Thread.sleep(1000);  // Sleep for 1 second
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();  // Set the interrupt flag
                    throw new RuntimeException("Thread was interrupted", e);
                }
            }
        }


    }
}