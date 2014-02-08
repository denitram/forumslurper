# Simple Groovy forum slurper

Quick and dirty Groovy script to automate the collection of forum posts into a local Postgresql database. Name suggests something generic, but for now only suitable for one specific forum. Fetches topic starters' posts only.

## Usage

    groovy -cp postgresql-9.1-901.jdbc4.jar forumslurper.groovy

## Issues

* Would have included grab annotation for postgresql jar, but that appears to require system classloader, which doesn't sit well with either geb or selenium dependencies. Workaround is to put local jar on explicitly provided classpath.
