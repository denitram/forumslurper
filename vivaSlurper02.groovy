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
 
//try {
	Browser.drive {
		go "https://duckduckgo.com"
 
		// make sure we actually got to the page
		assert title == "DuckDuckGo"
 
		// enter wikipedia into the search field
		$("input", name: "q").value("wikipedia")
 
		// click submit button
		$("input", name: "submit").click()
 
		// wait for DuckDuckGo's result page
		waitFor { title == "Wikipedia" }
		assert title == "wikipedia at DuckDuckGo"

		// is the first link to wikipedia?
		def firstLink = $("div#links").find("a.url")
		assert firstLink.text() == "Wikipedia"
 
		// click the link
		firstLink.click()
 
		// wait for DuckDuckGo's javascript to redirect to Wikipedia
		waitFor { title == "Wikipedia" }
	}
//} catch (Exception e) {
//	def cause = e.getCause()
//	cause = org.codehaus.groovy.runtime.StackTraceUtils.sanitize(cause)
//	cause.printStackTrace()
//	System.exit(1)
//}
