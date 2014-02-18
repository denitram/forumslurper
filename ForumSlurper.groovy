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

FIRST_PAGE_NUMBER = 528
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
		date character varying(20),
		title character varying(400),
		content character varying(8000),
		CONSTRAINT message_pkey PRIMARY KEY (id)
	)
'''
INSERT_MESSAGE_STMT = '''
	INSERT INTO message (forum, subforum, url) VALUES (?, ?, ?);
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

def scrapeTopics() {
	println "Scraping topic URLs"
	def topicUrlList = []
	Browser.drive {
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
		println "Driver capabilities:"
		println driver.capabilities.asMap().each{ println "${it.key}=${it.value}" }
	
		println "Processing forum ${FORUM}"
		go FORUM_BASE_URL
		assert title == FORUM_EXPECTED_TITLE
	
		println "Processing subforum ${SUBFORUM}"
		go SUBFORUM_BASE_URL
		assert title == SUBFORUM_EXPECTED_TITLE
	 
		def lastPageLink = $("dl.discussion-navigation.page-navigation.before dd a", rel: "next").previous()
		def lastPageNumber = lastPageLink.text().toInteger()
		assert lastPageNumber >= SUBFORUM_EXPECTED_MINIMAL_LAST_PAGE_NUMBER
		println "Subforum base page links to ${lastPageNumber} pages with multiple topics"
	
		(FIRST_PAGE_NUMBER..lastPageNumber).each() {
			currentPageNumber ->
			println "Processing page ${currentPageNumber} of ${lastPageNumber}"
	
			go "${PAGE_BASE_URL}?data[page]=${currentPageNumber}"
			assert title == PAGE_EXPECTED_TITLE
	
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
				topicUrlList.add(topicLinkHref)
				db.execute INSERT_MESSAGE_STMT, [FORUM, SUBFORUM, topicLinkHref]
			}
		}
	}
	return topicUrlList
}

def scrapeMessages(topicUrlList) {
	println "Scraping messages"
	Browser.drive {
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
		println "Driver capabilities:"
		println driver.capabilities.asMap().each{ println "${it.key}=${it.value}" }

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
def topicUrlList = scrapeTopics()
scrapeMessages(topicUrlList)
