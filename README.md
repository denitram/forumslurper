# Simple Groovy forum slurper

Quick and dirty Groovy script to automate the collection of forum posts into a local Postgresql database. Name suggests something generic, but for now only suitable for one specific forum. Fetches topic starters' posts only.

## Usage

### Linux

These instructions assume a GNU/Linux environment with Java, Groovy and a local Postgresql server. Just install the required dependencies with your favourite package manager.

Postgres user's password is set in the script.
	
1. Create db on local postgresql server

    `createdb -Upostgres -hlocalhost forumslurper`

2. Run script

    `groovy -cp postgresql-9.1-901.jdbc4.jar forumslurper.groovy`

3. Watch db while script is running (2nd shell)

    `watch -n 2 'psql -t -Upostgres -hlocalhost -dforumslurper -c "select id,forum,subforum,substring(url from 1 for 40),date,substring(title from 1 for 40),substring(content from 1 for 40) from message order by date desc;"'`

### Windows

Although it's possible to use Cygwin (http://cygwin.com) and GVM (http://gvmtool.net) to create a similar environment on Windows, it may be a bit of a hassle to get things running, and then turn out to be quite slow - well, it was on my virtualized XP in any case. To set up the prerequisites with native Windows packages instead:

1. Download and install a Java SE Development Kit

    * Open http://www.oracle.com/technetwork/java/javase/downloads/index.html
    * Download the appropriate package for your Windows version and install with defaults
    * Add a system environment variable `JAVA_HOME=C:\Program Files\Java\jdk1.7.0_51` (right click My Computer, Properties | Advanced | Environment variables | System variables | New)
    * Change the system environment variable PATH by appending `;%JAVA_HOME%\bin`

2. Download and install Groovy

    * Open http://groovy.codehaus.org/Download?nc
    * Download zip package and unzip in `C:\Program Files\Java`
    * Add a system environment variable `GROOVY_HOME=C:\Program Files\Java\groovy-2.2.1`
    * Change the system environment variable PATH by appending `;%GROOVY_HOME%\bin`

3. Download and install Postgresql

    * Open http://www.postgresql.org/download/windows
    * Download and install with defaults, and enter `password` for the administrative user postgres's password
    * Create a database with pgadmin3 (Menu Start | Programs | PostgreSQL 9.3 | pgAdmin III; double click server on localhost:5432, supply password, right click Databases | 
New Database, enter db name `forumslurper` and accept defaults

Now run the script from a command prompt with `groovy -cp postgresql-9.1-901.jdbc4.jar -Dfile.encoding=UTF-8 forumslurper.groovy`

## Issues

* Would have included grab annotation for postgresql jar, but that appears to require system classloader, which doesn't sit well with either geb or selenium dependencies. Workaround is to put local jar on explicitly provided classpath.
