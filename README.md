# Simple Groovy forum slurper

Quick and dirty Groovy script to automate the collection of forum posts into a local Postgresql database. Name suggests something generic, but for now only suitable for one specific forum. Fetches topic starters' posts only.

## Usage
	
1. Create db on local postgresql server

    createdb -Upostgres -hlocalhost forumslurper

2. Run script

    groovy -cp postgresql-9.1-901.jdbc4.jar forumslurper.groovy

3. Watch db while script is running (2nd shell)

    watch -n 2 'psql -t -Upostgres -hlocalhost -dforumslurper -c "select id,forum,subforum,substring(url from 1 for 40),date,substring(title from 1 for 40),substring(content from 1 for 40) from message order by date desc;"'

## Issues

* Would have included grab annotation for postgresql jar, but that appears to require system classloader, which doesn't sit well with either geb or selenium dependencies. Workaround is to put local jar on explicitly provided classpath.
