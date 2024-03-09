package org.example;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;

import static org.junit.Assert.*;

public class PageLinkScannerTest {

    PageLinkScanner pageLinkScanner;
    @Before
    public void setUp() throws Exception {
        try {
            pageLinkScanner = new PageLinkScanner("https://books.toscrape.com");
        } catch (IllegalArgumentException ex) {
            System.out.println(ex);
        }
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void run() {
        Thread scanThread = new Thread(pageLinkScanner);
        try {
            //TODO: Fix progress indicator.
            scanThread.start();
            System.out.println("Running Scanner");
            scanThread.join();
        } catch (IllegalThreadStateException ex){
            System.out.println(ex);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        Assert.assertEquals("Unexpected number of css-links scanned",3,(long)pageLinkScanner.numberOfCSSLinks());
        Assert.assertEquals("Unexpected number of image-links scanned",20,(long)pageLinkScanner.numberOfImageLinks());
        Assert.assertEquals("Unexpected number of html-links scanned",73,(long)pageLinkScanner.numberOfHTMLLinks());

        System.out.println("Number of links:" + pageLinkScanner.numberOfLinks());
        System.out.println("Number of html-links:" + pageLinkScanner.numberOfHTMLLinks());
        System.out.println("Number of jpg-links:" + pageLinkScanner.numberOfImageLinks());
        System.out.println("Number of css-links:" + pageLinkScanner.numberOfCSSLinks());

        System.out.println("Number of scans:" + pageLinkScanner.numberOfScans());

    }
}