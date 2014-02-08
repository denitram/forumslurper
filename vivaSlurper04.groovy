System.properties.with { p ->
	p['geb.browser']='htmlunit'
	p['org.apache.commons.logging.Log']='org.apache.commons.logging.impl.SimpleLog'
	p['org.apache.commons.logging.simplelog.log.com.gargoylesoftware.htmlunit']='NONE'
}

@Grapes([
	@Grab("org.gebish:geb-core:latest.release"),
	@Grab("org.seleniumhq.selenium:selenium-htmlunit-driver:2.26.0")
])

import geb.Browser
 
Browser.drive {
	go "https://duckduckgo.com"
 
	// make sure we actually got to the page
	assert title == "DuckDuckGo"
 
	// enter wikipedia into the search field
	$("input", name: "q").value("wikipedia")
 
	// click submit button
	$("input#search_button_homepage").click()
 
	// test title for DuckDuckGo's result page
	assert title == "wikipedia at DuckDuckGo"

	// is the first link to wikipedia?
	def links = $("div.results_links")
	assert links.size() == 15
	links = $("div.results_links").not("div.web-result-sponsored")
	assert links.size() == 14
	def firstLink = links.find("div.url", 0)
	assert firstLink.text() == "wikipedia.org"

	// click the link
	//firstLink.click()
 
	// wait for DuckDuckGo's javascript to redirect to Wikipedia
	//assert title == "wikipedia at DuckDuckGo"
}
