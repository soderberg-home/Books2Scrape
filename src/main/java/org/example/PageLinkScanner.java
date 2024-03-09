package org.example;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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
    @Override
    public void run() {
        try {
            //TODO:Favico?
            Document rootDocument = Jsoup.connect(pageURI).get();
            //HTML
            List<Element> rootLinks = rootDocument.select("a[href]").stream().filter(element -> element.attr("href").endsWith(".html")).toList();
            numberOfScans = rootLinks.size();
            for(Element element:rootLinks){
                links.put(element.attr("abs:href"),"html");
            }
            //CSS
            List<Element> cssLinks = rootDocument.select("link").stream().filter(element -> element.attr("href").endsWith("css")).toList();
            numberOfScans += cssLinks.size();
            for(Element cssLink : cssLinks){
                links.put(cssLink.attr("abs:href"),"css");
            }
            //Products images
            ArrayList<Element> products = rootDocument.getElementsByClass("product_pod");
            numberOfScans += products.size();
            for(Element product : products){
                String imgUrl = product.getElementsByClass("thumbnail").first().attr("abs:src");
                links.put(imgUrl,"jpg");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
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

    public Set<String> scannedHTMLLinks(){
        HashSet<String> retVal = new HashSet<>();
        links.forEach((k,val) -> {
            if (val.contains("html")){
                retVal.add(k);
            }
        });
        return retVal;
    }

    public String getPageURI(){
        return this.pageURI;
    }

}
