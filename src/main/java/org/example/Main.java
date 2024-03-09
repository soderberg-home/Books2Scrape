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
        /*try (ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(8)) {
       // try (ExecutorService threadPoolExecutor = Executors.newVirtualThreadPerTaskExecutor()) {

                pageLinkScannerList = new ArrayList<>();
            for (String page : pagerScanner.scannedPages()) {
                PageLinkScanner pageLinkScanner = new PageLinkScanner(page);
                pageLinkScannerList.add(pageLinkScanner);
                threadPoolExecutor.execute(pageLinkScanner);
            }
            threadPoolExecutor.shutdown();
            double numberOfTasks = 1.0 * threadPoolExecutor.getTaskCount();
            while (!threadPoolExecutor.isTerminated()) {
                long tasksFinished = threadPoolExecutor.getCompletedTaskCount();
                long percent = (long)Math.floor(100 * tasksFinished / numberOfTasks);
                System.out.print(percent);

                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }*/
        //ExecutorService threadPoolExecutor = null;
        pageLinkScannerList = new ArrayList<>();
        try (ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(8)) {
            // Initialize the list to store PageLinkScanners
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
            // try (ExecutorService downloadPoolExecutor = Executors.newVirtualThreadPerTaskExecutor()) {

            for (String page : pagerScanner.scannedPages()) {
                PageDownloader pageDownloader = new PageDownloader(page);
                downloadPoolExecutor.execute(pageDownloader);
            }
            downloadPoolExecutor.shutdown();
            double numberOfTasks = 1.0 * downloadPoolExecutor.getTaskCount();
            while (!downloadPoolExecutor.isTerminated()) {
                long tasksFinished = downloadPoolExecutor.getCompletedTaskCount();
                long percent = (long) Math.floor(100 * tasksFinished / numberOfTasks);
                System.out.print("+");
                try {
                    sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        System.out.println("");
        System.out.println("Downloading resources...");

        try (ThreadPoolExecutor downloadPoolExecutor2 = (ThreadPoolExecutor) Executors.newFixedThreadPool(8)) {
            // try (ExecutorService downloadPoolExecutor2 = Executors.newVirtualThreadPerTaskExecutor()) {

            for (PageLinkScanner pageLinkScanner : pageLinkScannerList) {
                //System.out.println(pageLinkScanner.getPageURI());
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
            double numberOfTasks2 = 1.0 * downloadPoolExecutor2.getTaskCount();
            while (!downloadPoolExecutor2.isTerminated()) {
                long tasksFinished = downloadPoolExecutor2.getCompletedTaskCount();
                long percent = (long) Math.floor(100 * tasksFinished / numberOfTasks2);
                System.out.print("+");
                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }


    }
}