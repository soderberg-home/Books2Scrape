package org.example;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;

public class PagerScannerTest {

    PagerScanner pagerScanner;
    @Before
    public void setUp() {
        try {
            pagerScanner = new PagerScanner(URI.create("http://books.toscrape.com"));
        } catch (IllegalArgumentException ex) {
            System.out.println(ex);
        }
    }

    @Test
    public void run() {
        Thread scanThread = new Thread(pagerScanner);
        try {
            //TODO: Fix progress indicator.
            scanThread.start();
            System.out.println("Running Scanner");
            Integer numberOfPagesScanned = 0;
            while (scanThread.isAlive()) {
                if(pagerScanner.numberOfPagesScanned() > numberOfPagesScanned){
                    numberOfPagesScanned = pagerScanner.numberOfPagesScanned();
                    System.out.print(".");
                    System.out.flush();
                }
            }
            System.out.println("Scan Finished");
        } catch (IllegalThreadStateException ex){
            System.out.println(ex);
        }
        Assert.assertEquals("Unexpected number of pages scanned.",50, (long)pagerScanner.numberOfPagesScanned());
    }


}