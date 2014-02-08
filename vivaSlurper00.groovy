System.properties.with { p ->
	p['geb.browser']='htmlunit'
	p['org.apache.commons.logging.Log']='org.apache.commons.logging.impl.SimpleLog'
	p['org.apache.commons.logging.simplelog.log.com.gargoylesoftware.htmlunit']='NONE'
}

@Grapes([
  @Grab("org.gebish:geb-core:0.9.0-RC-1"),
  @Grab("org.seleniumhq.selenium:selenium-htmlunit-driver:2.26.0")
])

import geb.*

Browser.drive {
  go "file:scrapeUsingGeb.html"
 
  $('div.item').each { println it.firstElement() }
} 
