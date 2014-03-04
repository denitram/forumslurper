# Duplicate topic opening message issue

The forumslurper script fails to take into account that every topic opening message is repeated (first message) on every sub-page of a multi-page topic. These duplicates are taken as replies and stored in the database.

## Number of messages (including duplicates)

	forumslurper=# select count(*) from message;
	count  
	---------
	3690948

## Number of topics (topic opening messages)

No duplicates, as the script correctly set is_topic = true only on the topic base page.

	forumslurper=# select count(*) from message where is_topic = true;
 	count 
	-------
 	43098

## Number of multi-page topics

	forumslurper=# select count(*) from message where is_topic = true and nr_of_pages > 1;
 	count 
	-------
 	19093

## Number of single-page topics

	forumslurper=# select count(*) from message where is_topic = true and nr_of_pages = 1;
 	count 
	-------
 	24005

## Check

	forumslurper=# select (select count(*) from message where is_topic = true and nr_of_pages > 1) + (select count(*) from message where is_topic = true and nr_of_pages = 1);
 	?column? 
	----------
 	43098

## Number of pages in multi-page topics

	forumslurper=# select sum(nr_of_pages) from message where is_topic = true and nr_of_pages > 1;
  	sum   
	--------
 	145857

## Number of duplicates

Equals number of pages in multi-page topics excluding base pages.

	forumslurper=# select sum(nr_of_pages - 1) from message where is_topic = true and nr_of_pages > 1;
  	sum   
	--------
 	126764

## Check

	select (select count(*) from message where is_topic = true and nr_of_pages > 1) + (select sum(nr_of_pages - 1) from message where is_topic = true and nr_of_pages > 1);
 	?column? 
	----------
   	145857

