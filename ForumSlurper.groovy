///////////////////////////////////////////////////////////////////////////////
FORUM = 'Viva'
FORUM_BASE_URL = 'http://forum.viva.nl/forum'
FORUM_EXPECTED_TITLE = 'Viva - Categorieën'

SUB_FORUM = 'Gezondheid'
SUB_FORUM_BASE_URL = "${FORUM_BASE_URL}/${SUB_FORUM}/list_topics/6"
SUB_FORUM_EXPECTED_TITLE = 'Viva - Onderwerpen van forum Gezondheid'
SUB_FORUM_EXPECTED_MINIMAL_LAST_PAGE_NUMBER = 528

PAGE_BASE_URL = SUB_FORUM_BASE_URL
PAGE_EXPECTED_TITLE = 'Viva - Onderwerpen van forum Gezondheid'

FIRST_PAGE_NUMBER = 526
// Use -1 to run until actual last page
//LAST_PAGE_NUMBER = -1
LAST_PAGE_NUMBER = 527
MAX_LABEL_WIDTH = 40

DEBUG = false
///////////////////////////////////////////////////////////////////////////////

System.properties.with { p ->
	//p['geb.browser']='htmlunit'
	p['org.apache.commons.logging.Log']='org.apache.commons.logging.impl.NoOpLog'
	//p['org.apache.commons.logging.Log']='org.apache.commons.logging.impl.SimpleLog'
	//p['org.apache.commons.logging.simplelog.log.com.gargoylesoftware.htmlunit']='NONE'
}

//@GrabConfig(systemClassLoader=true)
//@Grab(group='postgresql', module='postgresql', version='9.1-901.jdbc4')
@Grapes([
	@Grab("org.gebish:geb-core:latest.release"),
	@Grab("org.seleniumhq.selenium:selenium-htmlunit-driver:2.37.1"),
	@Grab("org.seleniumhq.selenium:selenium-chrome-driver:2.37.1"),
	@Grab("org.seleniumhq.selenium:selenium-firefox-driver:2.37.1"),
])

import geb.Browser
import groovy.sql.Sql
import org.postgresql.Driver

import org.openqa.selenium.Proxy
import org.openqa.selenium.Proxy.ProxyType
import org.openqa.selenium.Capabilities
import org.openqa.selenium.remote.DesiredCapabilities

import org.openqa.selenium.htmlunit.HtmlUnitDriver
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.chrome.ChromeDriver

DB_URL = 'jdbc:postgresql://localhost/forumslurper'
DB_USER = 'postgres'
DB_PASSWORD = 'password'
DB_DRIVER = 'org.postgresql.Driver'
CREATE_TABLE_STMT = '''
	DROP TABLE IF EXISTS message;
	CREATE TABLE message
	(
		id SERIAL,
		forum character varying(40),
		sub_forum character varying(40),
		topic_base_url character varying(400),
		is_topic boolean,
		nr_of_pages integer,
		sub_page integer,
		date character varying(20),
		title character varying(400),
		content character varying(8000),
		CONSTRAINT message_pkey PRIMARY KEY (id)
	)
'''
INSERT_TOPIC_STMT = '''
	INSERT INTO message (forum, sub_forum, topic_base_url, is_topic) VALUES (?, ?, ?, ?);
'''
UPDATE_TOPIC_PAGES_STMT = '''
	UPDATE message SET nr_of_pages = ? WHERE topic_base_url = ?;
'''
UPDATE_TOPIC_CONTENT_STMT = '''
	UPDATE message SET date = ?, title = ?, content = ? WHERE topic_base_url = ?;
'''
INSERT_REPLY_STMT = '''
	INSERT INTO message (forum, sub_forum, topic_base_url, is_topic, sub_page, date, title, content) VALUES (?, ?, ?, ?, ?, ?, ?, ?);
'''

def isProxied() {
	return (System.getProperty('http.proxyHost') != null && System.getProperty('http.proxyPort') != null)   
}

def buildProxy() {
	proxyHost = System.getProperty('http.proxyHost')
	proxyPort = System.getProperty('http.proxyPort')
	Proxy proxy = new Proxy()
	proxy.setProxyType(ProxyType.MANUAL) 
	proxy.setHttpProxy(proxyHost+":"+proxyPort)
	proxy.setSslProxy(proxyHost+":"+proxyPort)
	proxy.setFtpProxy(proxyHost+":"+proxyPort)
	return proxy
}

def confDriver(driver) {
	if (isProxied()) {
		if (driver instanceof HtmlUnitDriver) {
			println "Adding proxy to driver (htmlunit)"
			proxyHost = System.getProperty('http.proxyHost')
			proxyPort = System.getProperty('http.proxyPort')
			driver.setProxy(proxyHost, proxyPort.toInteger())
		} else {
			println "Adding proxy to driver capabilities (firefox, chrome)"
			Proxy proxy = buildProxy()
			driver.capabilities.setCapability('PROXY', buildProxy())
		}
	}
	if (DEBUG) {
		println "Driver capabilities:"
		println driver.capabilities.asMap().each{ println "${it.key}=${it.value}" }
	}
	return driver
}

def initDb() {
	println "Dropping and creating db table message"
	db = Sql.newInstance(
		DB_URL,
		DB_USER,
		DB_PASSWORD,
		DB_DRIVER
	)
	db.execute CREATE_TABLE_STMT
	return db
}

def displayForumAndSubForum() {
	Browser.drive {
		driver = confDriver(driver)
		println "Processing forum ${FORUM}"
		go FORUM_BASE_URL
		assert title == FORUM_EXPECTED_TITLE
		println "Processing sub-forum ${SUB_FORUM}"
		go SUB_FORUM_BASE_URL
		assert title == SUB_FORUM_EXPECTED_TITLE	 
	}
}
	
def collectTopicBaseUrls() {
	def topicBaseUrls = []
	Browser.drive {
		driver = confDriver(driver)
		
		def lastPageLink = $("dl.discussion-navigation.page-navigation.before dd a", rel: "next").previous()
		def lastPageNumber = lastPageLink.text().toInteger()
		assert lastPageNumber >= SUB_FORUM_EXPECTED_MINIMAL_LAST_PAGE_NUMBER
		println "Sub-forum base page links to ${lastPageNumber} pages with multiple topics"

		if (LAST_PAGE_NUMBER != -1) {
			lastPageNumber = LAST_PAGE_NUMBER
		}

		assert FIRST_PAGE_NUMBER <= lastPageNumber
		def numberOfPages = lastPageNumber - FIRST_PAGE_NUMBER + 1

		go "${PAGE_BASE_URL}?data[page]=${FIRST_PAGE_NUMBER}"
		def firstPageTopicList = $("table tbody td.topic-name")
		def numberOfTopicsOnFirstPage = firstPageTopicList.size()

		def totalNumberOfTopics
		if (numberOfPages < 2) {
			println "This run will try to collect messages from a single page (${FIRST_PAGE_NUMBER})"
			totalNumberOfTopics = numberOfTopicsOnFirstPage
		} else {
			println "This run will try to collect messages from ${numberOfPages} pages (${FIRST_PAGE_NUMBER} - ${lastPageNumber})"
			go "${PAGE_BASE_URL}?data[page]=${lastPageNumber}"
			def lastPageTopicList = $("table tbody td.topic-name")
			def numberOfTopicsOnLastPage = lastPageTopicList.size()
			totalNumberOfTopics = (numberOfPages - 1) * numberOfTopicsOnFirstPage + numberOfTopicsOnLastPage
		}
		println "Total number of topics to queue: ${totalNumberOfTopics}"

		(FIRST_PAGE_NUMBER..lastPageNumber).eachWithIndex() {
			currentPageNumber, i ->
			println "Processing page ${currentPageNumber} (#${i+1} of ${numberOfPages})"
	
			go "${PAGE_BASE_URL}?data[page]=${currentPageNumber}"
			assert title == PAGE_EXPECTED_TITLE
	
			def topicList = $("table tbody td.topic-name")
			def numberOfTopicsOnPage = topicList.size()
			assert numberOfTopicsOnPage > 1
			println "Page ${currentPageNumber} links to ${numberOfTopicsOnPage} topics"

			topicList.eachWithIndex() {
				topic, j ->
				def topicLink = topic.find("a.topic-link")
				def topicBaseUrl = topicLink.@href
				println "Queueing topic base page url #${topicBaseUrls.size()+1} of ${totalNumberOfTopics}: ${topicBaseUrl.padRight(MAX_LABEL_WIDTH)}" 
				topicBaseUrls.add(topicBaseUrl)
				db.execute INSERT_TOPIC_STMT, [FORUM, SUB_FORUM, topicBaseUrl, true]
			}
		}
	}
	return topicBaseUrls
}

def collectTopicUrls(topicBaseUrls) {
	def topicUrls = []
	Browser.drive {
		driver = confDriver(driver)
		println "Processing ${SUB_FORUM} topics"
		topicBaseUrls.eachWithIndex() {
			url, i ->
			println "Processing topic #${i+1} of ${topicBaseUrls.size()}" 
			go "${url}"
			println "Re-queueing topic base url: ${url.padRight(MAX_LABEL_WIDTH)}"
			topicUrls.add(url)
			def lastPageLink = $("dl.topic-navigation.page-navigation.before dd a.rel", rel: "last").previous()
			def lastPageNumber
			if (lastPageLink.size() == 0) {
				println "Topic consists of a single page"
				db.execute UPDATE_TOPIC_PAGES_STMT, [1, url]
			} else {
				lastPageNumber = lastPageLink.text().toInteger()
				println "Topic base page links to ${lastPageNumber-1} more pages"
				db.execute UPDATE_TOPIC_PAGES_STMT, [lastPageNumber, url]
				(1..lastPageNumber-1).each() {
					currentPageNumber ->
					def topicSubUrl = "${url}/${currentPageNumber}"
					println "Queueing topic sub-url #${currentPageNumber} of ${lastPageNumber-1}: ${topicSubUrl.padRight(MAX_LABEL_WIDTH)}"
					topicUrls.add(topicSubUrl)
				}
			}
		}
	}
	return topicUrls
}

def extractTopicBaseUrl(url) {
	def pattern = ~/^(.*\/list_messages\/\d*)(\/?)(\d*)$/
	def matcher = pattern.matcher(url)
	if (DEBUG) {
		println "[extractTopicBaseUrl] url: ${url}"
		matcher[0].eachWithIndex() {
			elem, i ->
			println "[extractTopicBaseUrl] matcher ${i}: ${elem}"
		}
		def topicBaseUrl = matcher[0][1]
		println "[extractTopicBaseUrl] topicBaseUrl: ${topicBaseUrl}"
	}
	return matcher[0][1]
}

def extractSubPage(url) {
	def pattern = ~/^(.*\/list_messages\/\d*)(\/?)(\d*)$/
	def matcher = pattern.matcher(url)
	if (DEBUG) {
		println "[extractSubPage] url: ${url}"
		matcher[0].eachWithIndex() {
			elem, i ->
			println "[extractSubPage] matcher ${i}: ${elem}"
		}
		def subPage = matcher[0][3] 
		println "[extractSubPage] subPage: ${subPage}"
	}
	return matcher[0][3]
}

def collectMessages(topicUrls) {
	Browser.drive {
		driver = confDriver(driver)
		println "Processing ${topicUrls.size()} topic pages"
		def messageCount = 0
		def replyCount = 0
		topicUrls.eachWithIndex() {
			url, i ->
			go "${url}"
			def messageTitle = $("h1").find("span.topic-name").text().replaceAll('\\ -\\ Pagina\\ (\\d)*','');
			def shortMessageTitle = messageTitle.length() > MAX_LABEL_WIDTH?"${messageTitle.replaceAll('\\r?\\n','\\\\n').substring(0,MAX_LABEL_WIDTH)}+":messageTitle
			assert title == "${FORUM} - ${messageTitle} - ${SUB_FORUM}"
			def topicBaseUrl = extractTopicBaseUrl(url)
			def subPage = extractSubPage(url)
			if (subPage == '') {
				def firstMessage = $("ol#firstmessage li.message")
				def date = firstMessage.find("div.author-data address.posted-at").text();
				def content = firstMessage.find("div.message-content div div.message-content-content").text();
				def shortContent = content.length() > MAX_LABEL_WIDTH?"${content.replaceAll('\\r?\\n','\\\\n').substring(0,MAX_LABEL_WIDTH)}+":content
				messageCount++
				replyCount = 0
				println "Storing first message (topic #${i+1} of ${topicUrls.size()}; message #${messageCount}): |${date}|${shortMessageTitle.padRight(MAX_LABEL_WIDTH+1)}|${shortContent.padRight(MAX_LABEL_WIDTH+1)}|"
				db.execute UPDATE_TOPIC_CONTENT_STMT, [date.toString(), messageTitle.toString(), content.toString(), url.toString()]
			}
			def replies = $("ol#messages li.message")
			replies.eachWithIndex {
				reply, j ->
				def date = reply.find("div.author-data address.posted-at").text();
				def content = reply.find("div.message-content div div.message-content-content").text();
				def shortContent = content.length() > MAX_LABEL_WIDTH?"${content.replaceAll('\\r?\\n','\\\\n').substring(0,MAX_LABEL_WIDTH)}+":content
				messageCount++
				replyCount++
				println "Storing reply message #${replyCount} on topic ${(subPage==''?'base page':'sub-page #'+subPage)} (topic #${i+1} of ${topicUrls.size()}; message #${messageCount}): |${date}|${shortMessageTitle.padRight(MAX_LABEL_WIDTH+1)}|${shortContent.padRight(MAX_LABEL_WIDTH+1)}|"
				db.execute INSERT_REPLY_STMT, [FORUM, SUB_FORUM, topicBaseUrl, false, (subPage==''?null:subPage.toInteger()), date.toString(), messageTitle.toString(), content.toString()]
			}
		}
	}
}

db = initDb()
displayForumAndSubForum()
def topicBaseUrls = collectTopicBaseUrls()
def topicUrls = collectTopicUrls(topicBaseUrls)
collectMessages(topicUrls)
