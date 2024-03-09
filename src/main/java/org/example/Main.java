package org.example;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(4);
        List<PageLinkScanner> pageLinkScannerList = new ArrayList<>();
        for(String page : pagerScanner.scannedPages()){
            PageLinkScanner pageLinkScanner = new PageLinkScanner(page);
            pageLinkScannerList.add(pageLinkScanner);
            threadPoolExecutor.execute(pageLinkScanner);
        }

        try {
            threadPoolExecutor.shutdown();
            threadPoolExecutor.awaitTermination(1,TimeUnit.DAYS);
        } catch(InterruptedException ex) {
            System.out.println("jkjk");
        }
        for(PageLinkScanner pageLinkScanner : pageLinkScannerList){
            System.out.println(pageLinkScanner.getPageURI());
            System.out.println(pageLinkScanner.scannedHTMLLinks());
        }



    }
}