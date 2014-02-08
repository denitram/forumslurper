System.properties.with { p ->
	p['geb.browser']='htmlunit'
	p['org.apache.commons.logging.Log']='org.apache.commons.logging.impl.NoOpLog'
	//p['org.apache.commons.logging.Log']='org.apache.commons.logging.impl.SimpleLog'
	//p['org.apache.commons.logging.simplelog.log.com.gargoylesoftware.htmlunit']='NONE'
}

//@GrabConfig(systemClassLoader=true) 
@Grapes([
	@Grab("org.gebish:geb-core:latest.release"),
	@Grab("org.seleniumhq.selenium:selenium-htmlunit-driver:2.26.0"),
	@Grab(group='postgresql', module='postgresql', version='9.1-901.jdbc4')
])

import geb.Browser
import groovy.sql.Sql
import org.postgresql.Driver
 
FORUM_BASE_URL = "http://forum.viva.nl/forum"
SUBFORUM_SUBJECT = "Gezondheid"
SUBFORUM_LANDING_PAGE_URL = "${FORUM_BASE_URL}/${SUBFORUM_SUBJECT}/list_topics/6"
SUBFORUM_PAGE_URL_PREFIX = SUBFORUM_LANDING_PAGE_URL

/*
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
		id bigint NOT NULL,
		content character varying(2000),
		url character varying(2000),
		CONSTRAINT message_pkey PRIMARY KEY (id)
	)
'''
*/
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

		//db.execute "INSERT INTO message (content, url) VALUES ('${messageContent.text()}', '${href}');"

	}

}

/*
def sql = Sql.newInstance("jdbc:mysql://localhost:3306/mydb",
    "user", "pswd", "com.mysql.jdbc.Driver")

// delete table if previously created
try {
   sql.execute("drop table PERSON")
} catch(Exception e){}

// create table
sql.execute('''create table PERSON (
    id integer not null primary key,
    firstname varchar(20),
    lastname varchar(20),
    location_id integer,
    location_name varchar(30)
)''')

// now let's populate the table
def people = sql.dataSet("PERSON")
people.add( firstname:"James", lastname:"Strachan", id:1, location_id:10, location_name:'London' )
people.add( firstname:"Bob", lastname:"Mcwhirter", id:2, location_id:20, location_name:'Atlanta' )
people.add( firstname:"Sam", lastname:"Pullara", id:3, location_id:30, location_name:'California' )

// do a query to check it all worked ok
def results = sql.firstRow("select firstname, lastname from PERSON where id=1").firstname
def expected = "James"
assert results == expected
*/
