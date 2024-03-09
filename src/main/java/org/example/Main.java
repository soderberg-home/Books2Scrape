package org.example;

import java.net.URI;


public class Main {
    public static void main(String[] args) {

        PagerScanner pagerScanner = new PagerScanner(URI.create("https://books.toscrape.com"));
        pagerScanner.scan();


    }
}