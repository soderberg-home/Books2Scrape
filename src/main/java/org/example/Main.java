package org.example;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class Main {
    public static void main(String[] args)  {

        PagerScanner pagerScanner = new PagerScanner(URI.create("https://books.toscrape.com"));
        Thread scanThread = new Thread(pagerScanner);
        scanThread.start();
        try {
            scanThread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        List<PageLinkScanner> pageLinkScannerList;
        try (ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(4)) {
            pageLinkScannerList = new ArrayList<>();
            for (String page : pagerScanner.scannedPages()) {
                PageLinkScanner pageLinkScanner = new PageLinkScanner(page);
                pageLinkScannerList.add(pageLinkScanner);
                threadPoolExecutor.execute(pageLinkScanner);
            }

            threadPoolExecutor.shutdown();
        }

        try (ThreadPoolExecutor downloadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(4)) {
            for (String page : pagerScanner.scannedPages()) {
                PageDownloader pageDownloader = new PageDownloader(page);
                downloadPoolExecutor.execute(pageDownloader);
            }
            downloadPoolExecutor.shutdown();
            while (!downloadPoolExecutor.isTerminated()) {
                System.out.println((downloadPoolExecutor.getCompletedTaskCount()) / (1.0 * downloadPoolExecutor.getTaskCount()));
            }
        }

        try (ThreadPoolExecutor downloadPoolExecutor2 = (ThreadPoolExecutor) Executors.newFixedThreadPool(4)) {
            for (PageLinkScanner pageLinkScanner : pageLinkScannerList) {
                System.out.println(pageLinkScanner.getPageURI());
                for (String scanned : pageLinkScanner.scannedHTMLLinks()) {
                    PageDownloader pageDownloader = new PageDownloader(scanned);
                    downloadPoolExecutor2.execute(pageDownloader);
                }
            }
            downloadPoolExecutor2.shutdown();
            while (!downloadPoolExecutor2.isTerminated()) {
                System.out.println((downloadPoolExecutor2.getCompletedTaskCount()) / (1.0 * downloadPoolExecutor2.getTaskCount()));
            }
        }

    }
}