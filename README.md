# Simple Groovy forum slurper

Groovy script to automate collection of forum posts into a database. Name suggests something generic, but for now only suitable for one specific forum. Fetches both topic starters' posts and replies. The script uses [Geb](http://www.gebish.org) and [WebDriver](http://docs.seleniumhq.org/projects/webdriver/) to crawl forum pages.

## Usage

### Linux

These instructions assume a GNU/Linux environment with Java and Groovy installed, plus a database accessible from wherever you're running the script. Just install the required dependencies with your favourite package manager.

Customize the string constants in the script to change database and forum properties. By default the slurper will collect Gezondheid topics from forum.viva.nl into a local Postgresql database.
	
1. Create db on local Postgresql server

    `createdb -Upostgres -hlocalhost forumslurper`

2. Run script with db driver on classpath [[1]](#1)

    `groovy -cp postgresql-9.1-901.jdbc4.jar ForumSlurper.groovy`

Depending on the number of messages you're downloading, the script may take considerable time to finish. If you want to follow what's going into the db while it's running, run something like

    watch -n 2 'psql -t -Upostgres -hlocalhost -dforumslurper -c "select * from message order by date desc;"'

#### Headless and browser modes
 
By default the script employs a headless HTTP client, but it's possible to use Firefox or Chrome to collect posts. To do this, specify either `firefox` or `chrome` for the run-time option `geb.env` (the third value, `htmlunit`, is the default driver). 

    groovy -cp postgresql-9.1-901.jdbc4.jar -Dgeb.env=firefox ForumSlurper.groovy

#### Proxy support

Limited proxy support is available. Auto configuration is not supported, and Firefox/Chrome don't seem to pick up explicitly provided proxy settings. They will use system proxy settings by default, and may therefore work anyway.

    groovy -cp postgresql-9.1-901.jdbc4.jar -Dhttp.proxyHost=wwwproxy.rivm.nl -Dhttp.proxyPort=8080 ForumSlurper.groovy

<a name="1">[1]</a> Would have included Grab annotation for JDBC jar, but that appears to require system classloader, which doesn't sit well with either Geb or Selenium dependencies. Workaround is to put the provided Postgresql JDBC jar on the classpath explicitly.

### Windows

Although it's possible to use [Cygwin](http://cygwin.com) and [GVM](http://gvmtool.net) to create a similar environment on Windows, it may be a hassle to get things running, and then turn out to be quite slow - well, it was on my virtualized Windows XP. To set up the prerequisites with native Windows packages:

1. Download and install a Java SE Development Kit
    1. Open http://www.oracle.com/technetwork/java/javase/downloads/index.html
    2. Download the appropriate package for your Windows version and install with defaults
    3. Add a system environment variable `JAVA_HOME=C:\Program Files\Java\jdk1.7.0_51` (right click My Computer, Properties | Advanced | Environment variables | System variables | New)
    4. Change the system environment variable PATH by appending `;%JAVA_HOME%\bin`
2. Download and install Groovy
    1. Open http://groovy.codehaus.org/Download
    2. Download zip package and unzip in `C:\Program Files\Java`
    3. Add a system environment variable `GROOVY_HOME=C:\Program Files\Java\groovy-2.2.1`
    4. Change the system environment variable PATH by appending `;%GROOVY_HOME%\bin`
3. Download and install Postgresql
    1. Open http://www.postgresql.org/download/windows
    2. Download and install with defaults, and enter `password` for the administrative user `postgres`'s password
    3. Create a database with pgadmin3 (Menu Start | Programs | PostgreSQL 9.3 | pgAdmin III; double click server on `localhost:5432`, supply password, right click Databases | New Database, enter db name `forumslurper` and accept defaults

Now run the script from a command prompt with

    groovy -cp postgresql-9.1-901.jdbc4.jar -Dfile.encoding=UTF-8 ForumSlurper.groovy
