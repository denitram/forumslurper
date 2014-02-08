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
FIRST_PAGE_NUMBER = 524

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
		content character varying(8000),
		url character varying(400),
		CONSTRAINT message_pkey PRIMARY KEY (id)
	)
'''

String messageUrlInsert = '''
	INSERT INTO message (url) VALUES (?);
'''

String messageContentUpdate = '''
	UPDATE message SET content = ? WHERE url = ?;
'''

println "Stage 2: scrape message urls"
///////////////////////////////////////////////////////////////////////////////

Browser.drive {
	
	go SUBFORUM_LANDING_PAGE_URL

	assert title == "Viva - Onderwerpen van forum Gezondheid"
	println "\tProcessing ${title}"
 
	def lastPageLink = $("dl.discussion-navigation.page-navigation.before dd a", rel: "next").previous()
	assert lastPageLink.text() == "526"
	def lastPageNumber = lastPageLink.text().toInteger()
	println "\tLanding page links to ${lastPageNumber} pages with topics"

	def hrefList = []

	(FIRST_PAGE_NUMBER..lastPageNumber).each() {

		currentPageNumber ->
		println "\t\tProcessing page ${currentPageNumber} of ${lastPageNumber}"

		go "${SUBFORUM_PAGE_URL_PREFIX}?data[page]=${currentPageNumber}"
		assert title == "Viva - Onderwerpen van forum Gezondheid"

		def topicList = $("table tbody td.topic-name")
		assert topicList.size() > 1
		println "\t\tPage links to ${topicList.size()} topics"

		topicList.eachWithIndex() {

			topic, i ->

			def topicLink = topic.find("a.topic-link")
			def topicLinkText = topicLink.text()
			def topicLinkHref = topicLink.@href
			println "\t\t\t${i}: ${topicLinkText} -> ${topicLinkHref}" 

			hrefList.add(topicLinkHref)

			db.execute messageUrlInsert, [topicLinkHref]

		}

	}
	
	println "Stage 3: scrape messages"
	///////////////////////////////////////////////////////////////////////////////

	println "\tProcessing ${hrefList.size()} topic pages"

	hrefList.eachWithIndex() {

		href, i ->
	
		go "${href}"
		//assert title == "Viva - ${topicLinkText} - ${SUBFORUM_SUBJECT}"

		def originalPost = $("ol#firstmessage li.message")
		assert originalPost.size() == 1

		def messageContentNode = originalPost.find("div.message-content div div.message-content-content");
		def messageContentFull = messageContentNode.text()
		def messageContentShort = "${messageContentFull.replaceAll('\\r?\\n','\\\\n').substring(0,20)}...\"" 
		println "\t\t${i}. Original post content: \"${messageContentShort}\"" 

		db.execute messageContentUpdate, [messageContentFull.toString(), href.toString()]

	}

}

/*
*/

