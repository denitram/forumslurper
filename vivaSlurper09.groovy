System.properties.with { p ->
	p['geb.browser']='htmlunit'
	p['org.apache.commons.logging.Log']='org.apache.commons.logging.impl.NoOpLog'
	//p['org.apache.commons.logging.Log']='org.apache.commons.logging.impl.SimpleLog'
	//p['org.apache.commons.logging.simplelog.log.com.gargoylesoftware.htmlunit']='NONE'
}

@Grapes([
	@Grab("org.gebish:geb-core:latest.release"),
	@Grab("org.seleniumhq.selenium:selenium-htmlunit-driver:2.26.0")
])

import geb.Browser
 

SUBFORUM_LANDING_PAGE_LINK = "http://forum.viva.nl/forum/Gezondheid/list_topics/6"
SUBFORUM_LINK_PREFIX = SUBFORUM_LANDING_PAGE_LINK

Browser.drive {
	
	go SUBFORUM_LANDING_PAGE_LINK

	assert title == "Viva - Onderwerpen van forum Gezondheid"
	println "Processing ${title}"
 
	def lastPageLink = $("dl.discussion-navigation.page-navigation.before dd a", rel: "next").previous()
	assert lastPageLink.text() == "526"
	def lastPageNumber = lastPageLink.text().toInteger()
	println "Landing page links to ${lastPageNumber} pages with topics"

	(524..lastPageNumber).each() {

		currentPageNumber ->
		println "\tProcessing page ${currentPageNumber} of ${lastPageNumber}"

		go "${SUBFORUM_LINK_PREFIX}?data[page]=${currentPageNumber}"
		assert title == "Viva - Onderwerpen van forum Gezondheid"

		def topicList = $("table tbody td.topic-name")
		assert topicList.size() > 1
		println "\tPage links to ${topicList.size()} topics"

		topicList.eachWithIndex() {

			topic, i ->

			def topicLink = topic.find("a.topic-link")
			println "\t\t${i}: ${topicLink.text()} -> ${topicLink.@href}" 

		}

	}

}

/*
*/
