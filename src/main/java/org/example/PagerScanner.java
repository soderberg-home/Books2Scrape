package org.example;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class PagerScanner implements Runnable {

    private final URI root;
    ConcurrentHashMap<String, Boolean> pages = new ConcurrentHashMap<>();

    public PagerScanner(URI root) {
        this.root = root;
    }

    @Override
    public void run() {
        String pageURL = root.toString();
        boolean morePages = true;
        try {
            while (morePages) {
                Document rootDocument = Jsoup.connect(pageURL).get();
                List<Element> pagers = rootDocument.select("li.next");
                if (!pagers.isEmpty()) {
                    pageURL = pagers.get(0).select("a[href]").get(0).attr("abs:href");
                    pages.put(pageURL, Boolean.TRUE);
                } else {
                    morePages = false;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    Integer numberOfPagesScanned(){
        return pages.size();
    }

}
