System.properties.with { p ->
	p['geb.browser']='htmlunit'
	p['org.apache.commons.logging.Log']='org.apache.commons.logging.impl.NoOpLog'
	//p['org.apache.commons.logging.Log']='org.apache.commons.logging.impl.SimpleLog'
	//p['org.apache.commons.logging.simplelog.log.com.gargoylesoftware.htmlunit']='NONE'
}

//@GrabConfig(systemClassLoader=true)
//@Grab(group='postgresql', module='postgresql', version='9.1-901.jdbc4')
@Grapes([
	@Grab("org.gebish:geb-core:latest.release"),
	@Grab("org.seleniumhq.selenium:selenium-htmlunit-driver:2.26.0"),
])

import geb.Browser
import groovy.sql.Sql
import org.postgresql.Driver
 
FORUM_BASE_URL = "http://forum.viva.nl/forum"
SUBFORUM_SUBJECT = "Gezondheid"
SUBFORUM_LANDING_PAGE_URL = "${FORUM_BASE_URL}/${SUBFORUM_SUBJECT}/list_topics/6"
SUBFORUM_PAGE_URL_PREFIX = SUBFORUM_LANDING_PAGE_URL
FIRST_PAGE_NUMBER = 514

println "Stage 1: (re-)create db table"
///////////////////////////////////////////////////////////////////////////////

db = Sql.newInstance(
	'jdbc:postgresql://localhost/viva-slurper',
	'postgres',
	'password',
	'org.postgresql.Driver'
)

db.execute '''
	DROP TABLE IF EXISTS message;
	CREATE TABLE message
	(
		id SERIAL,
		url character varying(400),
		date character varying(20),
		title character varying(400),
		content character varying(8000),
		CONSTRAINT message_pkey PRIMARY KEY (id)
	)
'''

String messageInsert = '''
	INSERT INTO message (url) VALUES (?);
'''

String messageUpdate = '''
	UPDATE message SET date = ?, title = ?, content = ? WHERE url = ?;
'''

println "Stage 2: scrape message urls"
///////////////////////////////////////////////////////////////////////////////

Browser.drive {
	
	println "Processing ${SUBFORUM_SUBJECT}"

	go SUBFORUM_LANDING_PAGE_URL
	assert title == "Viva - Onderwerpen van forum Gezondheid"
 
	def lastPageLink = $("dl.discussion-navigation.page-navigation.before dd a", rel: "next").previous()
	def lastPageNumber = lastPageLink.text().toInteger()
	assert lastPageNumber > 525
	println "Subforum landing page links to ${lastPageNumber} pages with multiple topics"

	def hrefList = []

	(FIRST_PAGE_NUMBER..lastPageNumber).each() {

		currentPageNumber ->
		println "Processing page ${currentPageNumber} of ${lastPageNumber}"

		go "${SUBFORUM_PAGE_URL_PREFIX}?data[page]=${currentPageNumber}"
		assert title == "Viva - Onderwerpen van forum Gezondheid"

		def topicList = $("table tbody td.topic-name")
		assert topicList.size() > 1
		println "Page ${currentPageNumber} links to ${topicList.size()} topics"

		topicList.eachWithIndex() {

			topic, i ->

			def topicLink = topic.find("a.topic-link")
			def topicLinkText = topicLink.text()
			def topicLinkHref = topicLink.@href
			println "${i}: ${topicLinkText} -> ${topicLinkHref}" 

			hrefList.add(topicLinkHref)

			db.execute messageInsert, [topicLinkHref]

		}

	}
	
	println "Stage 3: scrape messages"
	///////////////////////////////////////////////////////////////////////////////

	println "Processing ${hrefList.size()} topic pages"

	hrefList.eachWithIndex() {

		href, i ->
	
		go "${href}"

		def messageTitle = $("h1").find("span.topic-name").text();
		//assert title == "Viva - ${topicLinkText} - ${SUBFORUM_SUBJECT}"
		def message = $("ol#firstmessage li.message")
		def date = message.find("div.author-data address.posted-at").text();
		def content = message.find("div.message-content div div.message-content-content").text();
		def shortContent = content.length() > 80?"${content.replaceAll('\\r?\\n','\\\\n').substring(0,80)}...\"":content
		println "${i}. Short content: \"${shortContent}\"" 

		db.execute messageUpdate, [date.toString(), messageTitle.toString(), content.toString(), href.toString()]

	}

}

/*
*/

