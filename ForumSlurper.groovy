///////////////////////////////////////////////////////////////////////////////
FORUM = 'Viva'
FORUM_BASE_URL = 'http://forum.viva.nl/forum'
FORUM_EXPECTED_TITLE = 'Viva - Categorieën'

SUB_FORUM = 'Gezondheid'
SUB_FORUM_BASE_URL = "${FORUM_BASE_URL}/${SUB_FORUM}/list_topics/6"
SUB_FORUM_EXPECTED_TITLE = 'Viva - Onderwerpen van forum Gezondheid'

PAGE_BASE_URL = SUB_FORUM_BASE_URL
PAGE_EXPECTED_TITLE = "${SUB_FORUM_EXPECTED_TITLE}"

FIRST_PAGE_NUMBER = 300
// Use -1 to run until actual last page
//LAST_PAGE_NUMBER = -1
LAST_PAGE_NUMBER = 399

// Database fields limits
MAX_TOPIC_BASE_URL_LENGTH = 400
MAX_TITLE_LENGTH = 400
MAX_CONTENT_LENGTH = 8000

// Drop and create table? (You will lose all data collected in previous runs!)
RE_CREATE_TABLE = false

// Run time output options
MAX_DISPLAY_WIDTH = 40
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

DROP_TABLE_STMT = '''
	DROP TABLE IF EXISTS message;
'''
CREATE_TABLE_STMT = """\
	CREATE TABLE message
	(
		id SERIAL,
		forum character varying(40),
		sub_forum character varying(40),
		topic_base_url character varying(${MAX_TOPIC_BASE_URL_LENGTH}),
		is_topic boolean,
		nr_of_pages integer,
		sub_page integer,
		date character varying(20),
		title character varying(${MAX_TITLE_LENGTH}),
		content character varying(${MAX_CONTENT_LENGTH}),
		CONSTRAINT message_pkey PRIMARY KEY (id)
	)
""".toString()
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
		println "[DEBUG @ confDriver] Driver capabilities:"
		driver.capabilities.asMap().each{ println "[DEBUG @ confDriver] ${it.key}=${it.value}" }
	}
	return driver
}

def initDb() {
	db = Sql.newInstance(
		DB_URL,
		DB_USER,
		DB_PASSWORD,
		DB_DRIVER
	)
	if (RE_CREATE_TABLE) {
		println "Dropping existing db table 'message'"
		db.execute DROP_TABLE_STMT
		println "Creating db table 'message'"
		db.execute CREATE_TABLE_STMT
	}
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
			assert numberOfTopicsOnPage >= 1
			println "Page ${currentPageNumber} links to ${numberOfTopicsOnPage} topics"

			topicList.eachWithIndex() {
				topic, j ->
				def topicLink = topic.find("a.topic-link")
				def topicBaseUrl = topicLink.@href.toString()
				println "Queueing topic base page url #${topicBaseUrls.size()+1} of ${totalNumberOfTopics}: ${topicBaseUrl.padRight(MAX_DISPLAY_WIDTH)}" 
				topicBaseUrls.add(topicBaseUrl)
				try {
					db.execute INSERT_TOPIC_STMT, [FORUM, SUB_FORUM, topicBaseUrl, true]
				} catch(e) {
					assert e in org.postgresql.util.PSQLException
					topicBaseUrl = truncIfNecessary(topicBaseUrl, MAX_TOPIC_BASE_URL_LENGTH)
					db.execute INSERT_TOPIC_STMT, [FORUM, SUB_FORUM, topicBaseUrl, true]
				}
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
			println "Re-queueing topic base url: ${url.padRight(MAX_DISPLAY_WIDTH)}"
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
					println "Queueing topic sub-url #${currentPageNumber} of ${lastPageNumber-1}: ${topicSubUrl.padRight(MAX_DISPLAY_WIDTH)}"
					topicUrls.add(topicSubUrl)
				}
			}
		}
	}
	return topicUrls
}

def collectMessages(topicUrls) {
	def messageCount = 0
	def topicCount = 0
	Browser.drive {
		driver = confDriver(driver)
		println "Processing ${topicUrls.size()} topic pages"
		def replyCount = 0
		topicUrls.eachWithIndex() {
			url, i ->
			go "${url}"
			def messageTitle = $("h1").find("span.topic-name").text().toString().replaceAll('\\ -\\ Pagina\\ (\\d)*','');
			def topicBaseUrl = extractTopicBaseUrl(url)
			def subPage = extractSubPage(url)
			if (subPage == '') {
				def firstMessage = $("ol#firstmessage li.message")
				def date = firstMessage.find("div.author-data address.posted-at").text().toString();
				def content = firstMessage.find("div.message-content div div.message-content-content").text().toString();
				try {
					db.execute UPDATE_TOPIC_CONTENT_STMT, [date, messageTitle, content, url]
					messageCount++
					topicCount++
					println "Stored first message in topic #${topicCount} on base page (page #${i+1} of ${topicUrls.size()}; message #${messageCount}): |${date}|${inLineAndTruncIfNecessary(messageTitle, MAX_DISPLAY_WIDTH).padRight(MAX_DISPLAY_WIDTH+1)}|${inLineAndTruncIfNecessary(content, MAX_DISPLAY_WIDTH).padRight(MAX_DISPLAY_WIDTH+1)}|"
				} catch(e) {
					assert e in org.postgresql.util.PSQLException
					messageTitle = truncIfNecessary(messageTitle, MAX_TITLE_LENGTH)
					content = truncIfNecessary(content, MAX_CONTENT_LENGTH)
					db.execute UPDATE_TOPIC_CONTENT_STMT, [date, messageTitle, content, url]
					messageCount++
					topicCount++
					println "Retried with truncated field(s)"
					println "Stored first message in topic #${topicCount} on base page (page #${i+1} of ${topicUrls.size()}; message #${messageCount}): |${date}|${inLineAndTruncIfNecessary(messageTitle, MAX_DISPLAY_WIDTH).padRight(MAX_DISPLAY_WIDTH+1)}|${inLineAndTruncIfNecessary(content, MAX_DISPLAY_WIDTH).padRight(MAX_DISPLAY_WIDTH+1)}|"
				} finally {
					replyCount = 0
				}
			}
			def replies = $("ol#messages li.message")
			replies.eachWithIndex {
				reply, j ->
				def date = reply.find("div.author-data address.posted-at").text().toString();
				def content = reply.find("div.message-content div div.message-content-content").text().toString();
				try {
					db.execute INSERT_REPLY_STMT, [FORUM, SUB_FORUM, topicBaseUrl, false, (subPage==''?null:subPage.toInteger()), date, messageTitle, content]
					messageCount++
					replyCount++
					println "Stored reply message #${replyCount} in topic #${topicCount} on ${(subPage==''?'base page':'sub-page #'+subPage)} (page #${i+1} of ${topicUrls.size()}; message #${messageCount}): |${date}|${inLineAndTruncIfNecessary(messageTitle, MAX_DISPLAY_WIDTH).padRight(MAX_DISPLAY_WIDTH+1)}|${inLineAndTruncIfNecessary(content, MAX_DISPLAY_WIDTH).padRight(MAX_DISPLAY_WIDTH+1)}|"
				} catch(e) {
					assert e in org.postgresql.util.PSQLException
					messageTitle = truncIfNecessary(messageTitle, MAX_TITLE_LENGTH)
					content = truncIfNecessary(content, MAX_CONTENT_LENGTH)
					db.execute INSERT_REPLY_STMT, [FORUM, SUB_FORUM, topicBaseUrl, false, (subPage==''?null:subPage.toInteger()), date, messageTitle, content]
					messageCount++
					replyCount++
					println "Retried with truncated field(s)"
					println "Stored reply message #${replyCount} in topic #${topicCount} on ${(subPage==''?'base page':'sub-page #'+subPage)} (page #${i+1} of ${topicUrls.size()}; message #${messageCount}): |${date}|${inLineAndTruncIfNecessary(messageTitle, MAX_DISPLAY_WIDTH).padRight(MAX_DISPLAY_WIDTH+1)}|${inLineAndTruncIfNecessary(content, MAX_DISPLAY_WIDTH).padRight(MAX_DISPLAY_WIDTH+1)}|"
				} finally {
				}
			}
		}
	}
	return messageCount
}

def extractTopicBaseUrl(url) {
	def pattern = ~/^(.*\/list_messages\/\d*)(\/?)(\d*)$/
	def matcher = pattern.matcher(url)
	if (DEBUG) {
		println "[DEBUG @ extractTopicBaseUrl] url: ${url}"
		matcher[0].eachWithIndex() {
			elem, i ->
			println "[DEBUG @ extractTopicBaseUrl] matcher ${i}: ${elem}"
		}
		def topicBaseUrl = matcher[0][1]
		println "[DEBUG @ extractTopicBaseUrl] topicBaseUrl: ${topicBaseUrl}"
	}
	return matcher[0][1]
}

def extractSubPage(url) {
	def pattern = ~/^(.*\/list_messages\/\d*)(\/?)(\d*)$/
	def matcher = pattern.matcher(url)
	if (DEBUG) {
		println "[DEBUG @ extractSubPage] url: ${url}"
		matcher[0].eachWithIndex() {
			elem, i ->
			println "[DEBUG @ extractSubPage] matcher ${i}: ${elem}"
		}
		def subPage = matcher[0][3]
		println "[DEBUG @ extractSubPage] subPage: ${subPage}"
	}
	return matcher[0][3]
}

def truncIfNecessary(string, maxLength) {
	if (string != null && string.length() > maxLength) {
		string = string.substring(0, maxLength)
		if (DEBUG) {
			println  "[DEBUG @ truncIfNecessary] Truncated field to ${maxLength} chars: (${string})"
		}
	}
	return string
}

def inLineAndTruncIfNecessary(string, maxLength) {
	if (string != null) {
		if (string.length() <= maxLength) {
			string = string.replaceAll('\\r?\\n','\\\\n')
			if (DEBUG) {
				println  "[DEBUG @ inLineAndTruncIfNecessary] Inlined field: (${string})"
			}		
		} else {
			string = string.replaceAll('\\r?\\n','\\\\n').substring(0, maxLength) 
			if (DEBUG) {
				println  "[DEBUG @ inLineAndTruncIfNecessary] Inlined and truncated field to ${maxLength} chars: (${string})"
			}
		}		
	}
	return string
}

def displayElapsedTime(message, start, stop) {
	long l1 = start.getTime();
	long l2 = stop.getTime();
	long diff = l2 - l1;
	long secondInMillis = 1000;
	long minuteInMillis = secondInMillis * 60;
	long hourInMillis = minuteInMillis * 60;
	long dayInMillis = hourInMillis * 24;
	long elapsedDays = diff / dayInMillis;
	diff = diff % dayInMillis;
	long elapsedHours = diff / hourInMillis;
	diff = diff % hourInMillis;
	long elapsedMinutes = diff / minuteInMillis;
	diff = diff % minuteInMillis;
	long elapsedSeconds = diff / secondInMillis;
	print "${message}: ${(elapsedDays>0?elapsedDays+' days and ':'')}"
	printf("%02d",elapsedHours)
	print ':'
	printf("%02d",elapsedMinutes)
	print ':'
	printf("%02d",elapsedSeconds)
	println ''
}

def start = new Date()
db = initDb()
displayForumAndSubForum()
def actualStart = new Date()
def topicBaseUrls = collectTopicBaseUrls()
def finishedCollectingTopicBaseUrls = new Date()
displayElapsedTime("Time to collect topic base page urls", actualStart, finishedCollectingTopicBaseUrls)
def topicUrls = collectTopicUrls(topicBaseUrls)
def finishedCollectingTopicUrls = new Date()
displayElapsedTime("Time to collect all topic page urls", finishedCollectingTopicBaseUrls, finishedCollectingTopicUrls)
def numberOfMessagesCollected = collectMessages(topicUrls)
def stop = new Date()
displayElapsedTime("Time to collect messages", finishedCollectingTopicUrls, stop)
displayElapsedTime("${numberOfMessagesCollected} Messages stored; total running time was", start, stop)