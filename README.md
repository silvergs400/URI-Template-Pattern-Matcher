# URI-Template-Pattern-Matcher

OVERVIEW
Compare URL against many RFC 6570 URI Templates Level 1 to find a match. Code is written in Java.


What problem does this solve? Given an URL, the HTTP server has to determine if that URL matches one of URI Templates.
One use-case is server-side authorization, the caller of URL has "paid for" the URI Template, so the caller will be
granted access.


For example, incoming URL is:
  http://prodigi.com/image/123.jpg

Does that URL match the following URI Template and be granted access? Yes. This is a very basic and simple URI Template.
There are more complicated ones. See PatternMatchingTrieTest.java for details.

  http://prodigi.com/image/{imageId}


Let's say there are 10,000 URI Templates defined, if URI template is organized as a linked list, there will be up to
10,000 comparison, and on average, 5,000 comparisons. If there is a match. If there is no match, then all 10,000.
Linked List is inefficient and does not scale well.

I think the better choice is to use tree data structure instead of a linked list. Which is what this module is about.
Using tree reduces run time from O(n) to *roughly* O(log n) which is the height of the tree.

I ran some simple benchmark on my Mac OSX 10.9.5, 2.7 GHz Intel i7, 16GB 1600MHz DDR3

Matching URL against Tree with 1,000 templates takes under 1 ms.
Matching URL against Tree with 10,000 templates takes under 2 ms.

...with debug log turned off.

For use-cases, check out PatternMatchingTrieTest.java


NOTES

* Character encoding is UTF-8

* The validator does not check host and port.

* The tree is intended to be cached. Building the tree is fairly expensive and slow.
But matching URL against tree is fast. Therefore, it is logical to cache the tree in RAM.

* One idea is to use some kind of LRU (Least Recently Used) data structure to cache tree to limit RAM use. Java
LinkedHashSet or Google Guava should do the job.

* Caching tree in RAM is better than caching in Redis/Memcached. Because the latter requires de-serialization before use
Based on testing, de-serializing Tree with 1,000 templates takes about 30 ms.

* One strategy is to use RAM cache as primary cache, fall back to memcached/Redis as secondary cache.

* The code is deployed to run in Redhat 6 / Tomcat 7 / Spring Boot.


PREREQUISITES

Oracle Java 7 JDK
Apache Maven


INSTALLATION
Download the source code from GitHub. In root directory, type:

  mvn install

