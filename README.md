# Books2Scrape

Code for scraping/downloading resources on https://books.toscrape.com.

## How to Compile and Run

Project is set up to use Java 21 using Maven. 

### Compile
`mvn clean compile assembly:single`

### Package and Test

`mvn package`

### Run

`java -jar target/Books2Scrape-1.0-SNAPSHOT-jar-with-dependencies.jar <OUTPUT-PATH>`

example (Mac):

`java -jar target/Books2Scrape-1.0-SNAPSHOT-jar-with-dependencies.jar /Users/username/tests/`

If <OUTPUT-PATH> should end with a `/`. If not present it will be added. 

## Known Issues (Content)

- Star rating icons missing
- Buttons for next/previous pager missing
- 

## Known Issues (Performance)

- 
