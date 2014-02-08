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
 
FORUM = "Viva"
FORUM_BASE_URL = "http://forum.viva.nl/forum"
SUBFORUM = "Gezondheid"
SUBFORUM_BASE_URL = "${FORUM_BASE_URL}/${SUBFORUM}/list_topics/6"
PAGE_BASE_URL = SUBFORUM_BASE_URL
FIRST_PAGE_NUMBER = 525

println "Dropping and creating db table message"
///////////////////////////////////////////////////////////////////////////////

db = Sql.newInstance(
	'jdbc:postgresql://localhost/forumslurper',
	'postgres',
	'password',
	'org.postgresql.Driver'
)

db.execute '''
	DROP TABLE IF EXISTS message;
	CREATE TABLE message
	(
		id SERIAL,
		forum character varying(40),
		subforum character varying(40),
		url character varying(400),
		date character varying(20),
		title character varying(400),
		content character varying(8000),
		CONSTRAINT message_pkey PRIMARY KEY (id)
	)
'''

String messageInsert = '''
	INSERT INTO message (forum, subforum, url) VALUES (?, ?, ?);
'''

String messageUpdate = '''
	UPDATE message SET date = ?, title = ?, content = ? WHERE url = ?;
'''

println "Scraping topic URLs"
///////////////////////////////////////////////////////////////////////////////

Browser.drive {
	
	println "Processing forum ${FORUM}"

	go FORUM_BASE_URL
	assert title == "Viva - Categorieën"
 
	println "Processing subforum ${SUBFORUM}"

	go SUBFORUM_BASE_URL
	assert title == "Viva - Onderwerpen van forum Gezondheid"
 
	def lastPageLink = $("dl.discussion-navigation.page-navigation.before dd a", rel: "next").previous()
	def lastPageNumber = lastPageLink.text().toInteger()
	assert lastPageNumber > 525
	println "Subforum base page links to ${lastPageNumber} pages with multiple topics"

	def urlList = []

	(FIRST_PAGE_NUMBER..lastPageNumber).each() {

		currentPageNumber ->
		println "Processing page ${currentPageNumber} of ${lastPageNumber}"

		go "${PAGE_BASE_URL}?data[page]=${currentPageNumber}"
		assert title == "Viva - Onderwerpen van forum Gezondheid"

		def topicList = $("table tbody td.topic-name")
		def numberOfTopicsOnPage = topicList.size()
		assert numberOfTopicsOnPage > 1
		println "Page ${currentPageNumber} links to ${numberOfTopicsOnPage} topics"

		if (currentPageNumber == FIRST_PAGE_NUMBER) {
			def estimatedNumberOfTopics = (lastPageNumber - FIRST_PAGE_NUMBER) * numberOfTopicsOnPage + numberOfTopicsOnPage/2
			println "Estimated number of topics:  ${estimatedNumberOfTopics}"
		}

		topicList.eachWithIndex() {

			topic, i ->

			def topicLink = topic.find("a.topic-link")
			def topicLinkText = topicLink.text()
			def topicLinkHref = topicLink.@href
			println "${i+1}/${numberOfTopicsOnPage}: ${topicLinkText.padRight(40)}|${topicLinkHref.padRight(40)}" 

			urlList.add(topicLinkHref)

			db.execute messageInsert, [FORUM, SUBFORUM, topicLinkHref]

		}

	}
	
	println "Scraping messages"
	///////////////////////////////////////////////////////////////////////////////

	def numberOfTopics = urlList.size()
	println "Processing ${numberOfTopics} topic pages"

	urlList.eachWithIndex() {

		url, i ->
	
		go "${url}"

		def messageTitle = $("h1").find("span.topic-name").text().replaceAll('\\ -\\ Pagina\\ 1','');
		def shortMessageTitle = messageTitle.length() > 40?"${messageTitle.replaceAll('\\r?\\n','\\\\n').substring(0,40)}+":messageTitle
		assert title == "Viva - ${messageTitle} - ${SUBFORUM}"
		def message = $("ol#firstmessage li.message")
		def date = message.find("div.author-data address.posted-at").text();
		def content = message.find("div.message-content div div.message-content-content").text();
		def shortContent = content.length() > 40?"${content.replaceAll('\\r?\\n','\\\\n').substring(0,40)}+":content
		println "${i+1}/${numberOfTopics}: ${date}|${shortMessageTitle.padRight(41)}|${shortContent.padRight(41)}" 

		db.execute messageUpdate, [date.toString(), messageTitle.toString(), content.toString(), url.toString()]

	}

}

/*
*/

