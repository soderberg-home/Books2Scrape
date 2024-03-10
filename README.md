# Books2Scrape

Code for scraping/downloading resources on https://books.toscrape.com.

https://books.toscrape.com includes 1000 books organised on 50 pages with 20 books per page. 
There is a convenient paging mechanism with next/previous buttons that are utilised in the code 
to first download all the pages referenced, i.e. the `../page-x.html` files.

Then each separate page is scanned for links to:
- other html pages (individual books)
- images
- css files

As a final step all resources are downloaded to a local directory that is created by the program.
This local directory can be configured by the user as an argument to the program (see below).


## How to Compile and Run

The project is set up to use Java 21 and Maven.

### Compile
`mvn clean compile assembly:single`

### Package and Test

`mvn package`

### Run

`java -jar target/Books2Scrape-1.0-SNAPSHOT-jar-with-dependencies.jar <OUTPUT-PATH>`

example (Mac):

`java -jar target/Books2Scrape-1.0-SNAPSHOT-jar-with-dependencies.jar /Users/username/tests/`

`<OUTPUT-PATH>` should end with a slash character (/). If not present it will be added. 

## Known Issues (Content)

- Star rating icons missing (Fonts not fully downloaded from css).
- Buttons for next/previous pager missing.

## Known Issues (Performance)

- Virtual threads can be used to improve performance (together with a small delay between requests).
- This is a first version of the program. It has been developed according to the instructions within a few hours (3-6) of coding.
- The program is not fully optimised for performance. It is a first version that can be improved in many ways.
- Next step would be to verify the program with a profiler and then optimise the code accordingly.
- Another check that I would like to perform is to verify that Jsoup is used in the most efficient way and that the scan is efficient.
- 
