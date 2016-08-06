package com.flatironschool.javacs;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import redis.clients.jedis.Jedis;


public class WikiCrawler {
	// keeps track of where we started
	private final String source;
	
	// the index where the results go
	private JedisIndex index;
	
	// queue of URLs to be indexed
	private Queue<String> queue = new LinkedList<String>();
	
	// fetcher used to get pages from Wikipedia
	final static WikiFetcher wf = new WikiFetcher();

	/**
	 * Constructor.
	 * 
	 * @param source
	 * @param index
	 */
	public WikiCrawler(String source, JedisIndex index) {
		this.source = source;
		this.index = index;
		queue.offer(source);
	}

	/**
	 * Returns the number of URLs in the queue.
	 * 
	 * @return
	 */
	public int queueSize() {
		return queue.size();	
	}

	/**
	 * Gets a URL from the queue and indexes it.
	 * @param b 
	 * 
	 * @return Number of pages indexed.
	 * @throws IOException
	 */
	public String crawl(boolean testing) throws IOException {
		String url = queue.remove();
        if(testing){
        	// choose and remove url from queue (FIFO);
        	// read contents of page using wikifetcher.readwikipedia (cached)
        	// (should index pages regardless of whether they are already indexed)
        	// find ALL internal links and add to queue
        	//finally return the page that was indexed (the first removed url)
        	
        	Elements paragraphs = wf.readWikipedia(url);

        	index.indexPage(url, paragraphs);
        	queueInternalLinks(paragraphs);

        	return url; 
        } else{

        	// choose and remove a url from queue (FIFO)
        	// if url is indexed, should not indexit again and return NULL
        	// otherwise, should read contents of the page using wikifetcher.fetchwikipedia
        	// then it should index the page, add links to the queue and return the URL of the page it indexed 

        	if(index.isIndexed(url)){
        		return null;
        	} 

        	Elements paragraphs = wf.fetchWikipedia(url);
        	index.indexPage(url,paragraphs);
        	queueInternalLinks(paragraphs);
        	return url;
        }
	}
	
	/**
	 * Parses paragraphs and adds internal links to the queue.
	 * 
	 * @param paragraphs
	 */
	// NOTE: absence of access level modifier means package-level
	void queueInternalLinks(Elements paragraphs) {

		// parse through all the paragraphs 
		for (Element paragraph : paragraphs){
			Iterable<Node> iter = new WikiNodeIterable(paragraph);
			for (Node node: iter) {

			if (node instanceof Element){
				Element accesibleNode = (Element)node;
				String tag = accesibleNode.tagName();
					if(tag.equals("a")){
						String url = accesibleNode.attr("abs:href");
						// make sure the link is an internal link (links to another wikipedia page)
						boolean internal = url.contains("wikipedia");

						if (internal) {
							queue.offer(accesibleNode.attr("abs:href"));
						}
					
					
					}
				}
			}

		}
        
	}


	public static void main(String[] args) throws IOException {
		//System.out.println("I'm in the main");
		
		// make a WikiCrawler
		 Jedis jedis = JedisMaker.make();
		 JedisIndex index = new JedisIndex(jedis); 
		String source = "https://en.wikipedia.org/wiki/Java_(programming_language)";
		WikiCrawler wc = new WikiCrawler(source, index);
		
		// for testing purposes, load up the queue
		Elements paragraphs = wf.fetchWikipedia(source);
		
		wc.queueInternalLinks(paragraphs);

		System.out.println(wc.queue.peek());
		

		String[] urls = wc.queue.toArray(new String[0]);

		for (String url: urls){
			System.out.println(url);
		}



		System.out.println(wc.queueSize());

		loop until we index a new page
		String res;
		do {
			res = wc.crawl(false);

            
		} while (res == null);
		
		Map<String, Integer> map = index.getCounts("the");
		for (Entry<String, Integer> entry: map.entrySet()) {
			System.out.println(entry);
		}
	}
}
