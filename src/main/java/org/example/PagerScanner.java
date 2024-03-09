package org.example;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class PagerScanner {

    private final URI root;

    public PagerScanner(URI root) {
        this.root = root;
    }

    void scan() {
        List<String> pages = new ArrayList<>();
        String pageURL = root.toString();
        boolean morePages = true;
        try {
            System.out.println("Finding number of pages to scan.");
            while (morePages) {
                System.out.print(".");
                Document rootDocument = Jsoup.connect(pageURL).get();
                List<Element> pagers = rootDocument.select("li.next");
                if (!pagers.isEmpty()) {
                    pageURL = pagers.get(0).select("a[href]").get(0).attr("abs:href");
                    pages.add(pageURL);
                } else {
                    morePages = false;
                }
            }
            System.out.println("Found: " + pages.size() + " pages to scan.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }


}
