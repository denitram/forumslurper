///////////////////////////////////////////////////////////////////////////////
FORUM = 'Viva'
FORUM_BASE_URL = 'http://forum.viva.nl/forum'
FORUM_EXPECTED_TITLE = 'Viva - Categorieën'

SUBFORUM = 'Gezondheid'
SUBFORUM_BASE_URL = "${FORUM_BASE_URL}/${SUBFORUM}/list_topics/6"
SUBFORUM_EXPECTED_TITLE = 'Viva - Onderwerpen van forum Gezondheid'
SUBFORUM_EXPECTED_MINIMAL_LAST_PAGE_NUMBER = 528

PAGE_BASE_URL = SUBFORUM_BASE_URL
PAGE_EXPECTED_TITLE = 'Viva - Onderwerpen van forum Gezondheid'

FIRST_PAGE_NUMBER = 526
MAX_LABEL_WIDTH = 40
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

DEBUG = false
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
		subforum character varying(40),
		url character varying(400),
		topic boolean,
		pages integer,
		page integer,
		date character varying(20),
		title character varying(400),
		content character varying(8000),
		CONSTRAINT message_pkey PRIMARY KEY (id)
	)
'''
INSERT_MESSAGE_STMT = '''
	INSERT INTO message (forum, subforum, url, topic) VALUES (?, ?, ?, ?);
'''
UPDATE_MESSAGE_STMT = '''
	UPDATE message SET date = ?, title = ?, content = ? WHERE url = ?;
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

def scrapeForum() {
	Browser.drive {
		driver = confDriver(driver)
		println "Processing forum ${FORUM}"
		go FORUM_BASE_URL
		assert title == FORUM_EXPECTED_TITLE
	}
}
	
def scrapeSubforum() {
	def subforumUrlList = []
	Browser.drive {
		driver = confDriver(driver)
		
		println "Processing subforum ${SUBFORUM}"
		go SUBFORUM_BASE_URL
		assert title == SUBFORUM_EXPECTED_TITLE
	 
		def lastPageLink = $("dl.discussion-navigation.page-navigation.before dd a", rel: "next").previous()
		def lastPageNumber = lastPageLink.text().toInteger()
		assert lastPageNumber >= SUBFORUM_EXPECTED_MINIMAL_LAST_PAGE_NUMBER
		println "Subforum base page links to ${lastPageNumber} pages with multiple topics"
	
		go "${PAGE_BASE_URL}?data[page]=${FIRST_PAGE_NUMBER}"
		def firstPageTopicList = $("table tbody td.topic-name")
		def numberOfTopicsOnFirstPage = firstPageTopicList.size()

		go "${PAGE_BASE_URL}?data[page]=${lastPageNumber}"
		def lastPageTopicList = $("table tbody td.topic-name")
		def numberOfTopicsOnLastPage = lastPageTopicList.size()
		
		def totalNumberOfTopics = (lastPageNumber - FIRST_PAGE_NUMBER) * numberOfTopicsOnFirstPage + numberOfTopicsOnLastPage
		println "Total number of topics to queue:  ${totalNumberOfTopics}"

		(FIRST_PAGE_NUMBER..lastPageNumber).each() {
			currentPageNumber ->
			println "Processing page ${currentPageNumber} of ${lastPageNumber}"
	
			go "${PAGE_BASE_URL}?data[page]=${currentPageNumber}"
			assert title == PAGE_EXPECTED_TITLE
	
			def topicList = $("table tbody td.topic-name")
			def numberOfTopicsOnPage = topicList.size()
			assert numberOfTopicsOnPage > 1
			println "Page ${currentPageNumber} links to ${numberOfTopicsOnPage} topics"

			topicList.eachWithIndex() {
				topic, i ->
				def topicLink = topic.find("a.topic-link")
				def topicLinkText = topicLink.text()
				def topicLinkHref = topicLink.@href
				println "Queueing topic base page url ${subforumUrlList.size()} of ${totalNumberOfTopics}: ${topicLinkHref.padRight(MAX_LABEL_WIDTH)}" 
				subforumUrlList.add(topicLinkHref)
				db.execute INSERT_MESSAGE_STMT, [FORUM, SUBFORUM, topicLinkHref, true]
			}
		}
	}
	return subforumUrlList
}

def scrapeTopics(subforumUrlList) {
	def topicUrlList = []
	Browser.drive {
		driver = confDriver(driver)
		println "Processing ${SUBFORUM} topics"
		subforumUrlList.eachWithIndex() {
			url, i ->
			println "${i}".center(MAX_LABEL_WIDTH, '-')
			go "${url}"
			//def ttl = $("h1").find("span.topic-name").text()
			//println ttl
			def topicUrl = url
			println "Adding topic base page url: ${topicUrl.padRight(MAX_LABEL_WIDTH)}"
			topicUrlList.add(topicUrl)
			def lastPageLink = $("dl.topic-navigation.page-navigation.before dd a.rel", rel: "last").previous()
			//def lastPageLink = $("dl", class: contains(~/topic-navigation/)).find("a", rel: "last").previous()
			def lastPageNumber
			if (lastPageLink.size() == 0) {
				println "Topic base page links to 1 page with messages"
			} else {
				//println lastPageLink
				//println lastPageLink.text()
				//println lastPageLink.@href
				lastPageNumber = lastPageLink.text().toInteger()
				println "Topic base page links to ${lastPageNumber} pages with messages"
				(1..lastPageNumber-1).each() {
					currentPageNumber ->
					topicUrl = "${url}/${currentPageNumber}"
					topicUrlList.add(topicUrl)
					println "Adding topic subpage url: ${currentPageNumber}/${lastPageNumber-1}: ${topicUrl.padRight(MAX_LABEL_WIDTH)}"
					topicUrlList.add(topicUrl)
				}
			}
		}
	}
	return topicUrlList
}

def scrapeMessages(topicUrlList) {
	Browser.drive {
		driver = confDriver(driver)

		def numberOfTopics = topicUrlList.size()
		println "Processing ${numberOfTopics} topic pages"
	
		topicUrlList.eachWithIndex() {
			url, i ->
			go "${url}"
			def messageTitle = $("h1").find("span.topic-name").text().replaceAll('\\ -\\ Pagina\\ 1','');
			def shortMessageTitle = messageTitle.length() > MAX_LABEL_WIDTH?"${messageTitle.replaceAll('\\r?\\n','\\\\n').substring(0,MAX_LABEL_WIDTH)}+":messageTitle
			assert title == "${FORUM} - ${messageTitle} - ${SUBFORUM}"
			def message = $("ol#firstmessage li.message")
			def date = message.find("div.author-data address.posted-at").text();
			def content = message.find("div.message-content div div.message-content-content").text();
			def shortContent = content.length() > MAX_LABEL_WIDTH?"${content.replaceAll('\\r?\\n','\\\\n').substring(0,MAX_LABEL_WIDTH)}+":content
			println "${i+1}/${numberOfTopics}: ${date}|${shortMessageTitle.padRight(MAX_LABEL_WIDTH+1)}|${shortContent.padRight(MAX_LABEL_WIDTH+1)}" 
			db.execute UPDATE_MESSAGE_STMT, [date.toString(), messageTitle.toString(), content.toString(), url.toString()]
		}
	}
}

db = initDb()
scrapeForum()
def subforumUrlList = scrapeSubforum()
def topicUrlList = scrapeTopics(subforumUrlList)
//scrapeMessages(topicUrlList)
