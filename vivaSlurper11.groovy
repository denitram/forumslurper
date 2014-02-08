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
 

FORUM_BASE_URL = "http://forum.viva.nl/forum"
SUBFORUM_SUBJECT = "Gezondheid"
SUBFORUM_LANDING_PAGE_URL = "${FORUM_BASE_URL}/${SUBFORUM_SUBJECT}/list_topics/6"
SUBFORUM_PAGE_URL_PREFIX = SUBFORUM_LANDING_PAGE_URL

Browser.drive {
	
	go SUBFORUM_LANDING_PAGE_URL

	assert title == "Viva - Onderwerpen van forum Gezondheid"
	println "Processing ${title}"
 
	def lastPageLink = $("dl.discussion-navigation.page-navigation.before dd a", rel: "next").previous()
	assert lastPageLink.text() == "526"
	def lastPageNumber = lastPageLink.text().toInteger()
	println "Landing page links to ${lastPageNumber} pages with topics"

	def hrefList = []

	(526..lastPageNumber).each() {

		currentPageNumber ->
		println "\tProcessing page ${currentPageNumber} of ${lastPageNumber}"

		go "${SUBFORUM_PAGE_URL_PREFIX}?data[page]=${currentPageNumber}"
		assert title == "Viva - Onderwerpen van forum Gezondheid"

		def topicList = $("table tbody td.topic-name")
		assert topicList.size() > 1
		println "\tPage links to ${topicList.size()} topics"


		topicList.eachWithIndex() {

			topic, i ->

			def topicLink = topic.find("a.topic-link")
			def topicLinkText = topicLink.text()
			def topicLinkHref = topicLink.@href
			println "\t\t${i}: ${topicLinkText} -> ${topicLinkHref}" 

			hrefList.add(topicLinkHref)
		}

	}
	
	println "Processing ${hrefList.size()} topic pages"

	hrefList.eachWithIndex() {

		href, i ->
	
		go "${href}"
		//assert title == "Viva - ${topicLinkText} - ${SUBFORUM_SUBJECT}"

		def originalPost = $("ol#firstmessage li.message")
		assert originalPost.size() == 1

		def messageContent = originalPost.find("div.message-content div div.message-content-content");
		println "\t${i}. Original post content: \"${messageContent.text().substring(0,40)}...\"" 

	}

}

/*
*/
