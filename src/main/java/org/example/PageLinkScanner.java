package org.example;

import java.util.concurrent.ConcurrentHashMap;

public class PageLinkScanner implements Runnable {

    private final ConcurrentHashMap<String, String> links;

    public PageLinkScanner() {
        this.links = new ConcurrentHashMap<>();
    }

    @Override
    public void run() {

    }
}
