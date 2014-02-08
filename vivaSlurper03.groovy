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

	//FAILS
	//def firstLink = $("div.result_links").find("a.url")
	//assert firstLink.text() == "Wikipedia"

	//FAILS
	//def firstLink = $("a.url")
	//assert firstLink.text() == "Wikipedia"

	//OK
	//def element = $("title")
	//assert element.text() == "wikipedia at DuckDuckGo"

	//APPEARS TO BE OK BUT FAILS
	//def element = $("a", 0)
	//assert element.toString() == '<a name="top" id="top" />'

	//FAILS
	//def element = $("noscript")
	//assert element.text() == "wikipedia at DuckDuckGo"

	//OK
	//def elements = $("a")
	//elements.each { println it.text() }
 
	//OK, BUT NOT AS INTENDED
	//def elements = $("a")
	//elements.each { try{println it.href}catch(Exception e){println "OEPS!"} }
 
	//def element = $("a", class: "url")
	//assert element.text() == "wikipedia at DuckDuckGo"

	//FAILS, BUT INFORMATIVELY
	def links = $("a")
	assert link.size() == 4

	// click the link
//	firstLink.click()
 
	// wait for DuckDuckGo's javascript to redirect to Wikipedia
//	waitFor { title == "Wikipedia" }
}
