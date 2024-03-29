package org.example;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PageLinkScanner implements Runnable {

    private final ConcurrentHashMap<String, String> links;
    private final String pageURI;
    private  Integer numberOfScans;

    public PageLinkScanner(String uri) {
        this.links = new ConcurrentHashMap<>();
        this.pageURI = uri;
        this.numberOfScans = 0;
    }

    //ico, jpg, html,
    private void processLinks(Document document, String selector, String linkEnding, String fileType) {
        List<Element> links = document.select(selector).stream()
                .filter(element -> element.attr("href").endsWith(linkEnding))
                .toList();
        numberOfScans += links.size();
        links.forEach(link -> this.links.put(link.attr("abs:href"), fileType));
    }

    private void processHtmlImages() {
        scannedHTMLLinks().forEach(link -> {
            try {
                Document doc = Jsoup.connect(link).get();
                Elements activeItems = doc.getElementsByClass("item active");
                if (!activeItems.isEmpty()) {
                    String imgUrl = activeItems.first().getElementsByTag("img").attr("abs:src");
                    links.put(imgUrl, "jpg");
                    numberOfScans++;
                }
            } catch (IOException e) {
                System.err.println("Failed to load or process HTML image from: " + link);
            }
        });
    }

    private void processProductImages(Document document) {
        Elements productElements = document.getElementsByClass("product_pod");
        numberOfScans += productElements.size();
        productElements.forEach(product -> {
            String imgUrl = product.getElementsByClass("thumbnail").first().attr("abs:src");
            links.put(imgUrl, "jpg");
        });
    }
    @Override
    public void run() {

            try {
                Document rootDocument = Jsoup.connect(pageURI).get();

                // Extract and process both HTML and CSS links in one step
                processLinks(rootDocument, "a[href]", ".html", "html");
                processLinks(rootDocument, "link", "css", "css");

                // Process HTML images
                processHtmlImages();

                // Process product images
                processProductImages(rootDocument);

            } catch (IOException e) {
                throw new RuntimeException("Failed to connect or process the document", e);
            }
        }

    public Integer numberOfLinks(){
        return links.size();
    }
    public Integer numberOfScans(){
        return numberOfScans;
    }
    public Integer numberOfHTMLLinks(){
        return links.values().stream().filter(s->s.contains("html")).toList().size();
    }
    public Integer numberOfImageLinks(){
        return links.values().stream().filter(s->s.contains("jpg")).toList().size();
    }
    public Integer numberOfCSSLinks(){
        return links.values().stream().filter(s->s.contains("css")).toList().size();
    }

    public Set<String> scannedHTMLLinks(){ //TODO: repeated code
        HashSet<String> retVal = new HashSet<>();
        links.forEach((k,val) -> {
            if (val.contains("html")){
                retVal.add(k);
            }
        });
        return retVal;
    }

    public Set<String> scannedImageLinks(){ //TODO: repeated code
        HashSet<String> retVal = new HashSet<>();
        links.forEach((k,val) -> {
            if (val.contains("jpg")){
                retVal.add(k);
            }
        });
        return retVal;
    }

    public Set<String> scannedCSSLinks(){ //TODO: repeated code
        HashSet<String> retVal = new HashSet<>();
        links.forEach((k,val) -> {
            if (val.contains("css")){
                retVal.add(k);
            }
        });
        return retVal;
    }

    public String getPageURI(){
        return this.pageURI;
    }

}
