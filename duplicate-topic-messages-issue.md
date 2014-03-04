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

## Scratchpad

<pre>

### Create tmp table with multi page topics

forumslurper=# create table multi_page_topic as (select * from message where is_topic = true and nr_of_pages > 1);                                                                    
SELECT 19093
Time: 2072.718 ms

### Verify all topic base urls in multi page topic table are unique

forumslurper=# select count(distinct topic_base_url) from multi_page_topic;
 count 
-------
 19093
(1 row)

Time: 871.689 ms

forumslurper=# select count(*) from message;                                                                                                                                          
  count  
---------
 3690948
(1 row)

Time: 1365.730 ms

forumslurper=# select count(topic_base_url) from message;                                                                                                                             
  count  
---------
 3690948
(1 row)

Time: 1691.000 ms

forumslurper=# select count(distinct topic_base_url) from message;                                                                                                                    
 count 
-------
 43098
(1 row)

Time: 520758.520 ms

### Create tmp table with replies

forumslurper=# create table reply as (select * from message where is_topic = false);                                                                                                  
SELECT 3647850
Time: 71319.512 ms

### Create tmp table with replies in multi page topics

forumslurper=# create table multi_page_topic_reply as (select r.id,r.forum,r.sub_forum,r.topic_base_url,r.is_topic,r.nr_of_pages,r.sub_page,r.date,r.title,r.content from reply r join
 multi_page_topic mpt on r.topic_base_url = mpt.topic_base_url where r.is_topic = false);                                                                                             
SELECT 3358077
Time: 141970.560 ms

### Create tmp table with multi page topic ids

forumslurper=# create table multi_page_topic_reply_id as (select id from multi_page_topic_reply);
SELECT 3358077
Time: 4585.779 ms

### Verify mpt and mptr are disjunct

forumslurper=# select count(id) from multi_page_topic where id in (select id from multi_page_topic_reply);
 count 
-------
     0
(1 row)

Time: 3410.895 ms

### Create tmp table with multi page topic topic base urls 

forumslurper=# create table multi_page_topic_reply_topic_base_url as (select topic_base_url from multi_page_topic_reply);                                                             
SELECT 3358077
Time: 10271.150 ms

### Verify number of unique topic base urls in multi page topic replies equals number of multi page topics 

forumslurper=# select count(distinct topic_base_url) from multi_page_topic_reply_topic_base_url;                                                                                     
 count 
-------
 19093
(1 row)

Time: 469060.463 ms

### Zoom in on issue

forumslurper=# \d
                          List of relations
 Schema |                 Name                  |   Type   |  Owner   
--------+---------------------------------------+----------+----------
 public | message                               | table    | postgres
 public | message_id_seq                        | sequence | postgres
 public | multi_page_topic                      | table    | postgres
 public | multi_page_topic_reply                | table    | postgres
 public | multi_page_topic_reply_id             | table    | postgres
 public | multi_page_topic_reply_topic_base_url | table    | postgres
 public | reply                                 | table    | postgres
(7 rows)

forumslurper=# select id,topic_base_url,date from multi_page_topic_reply limit 5;                                                                                                     
   id    |                                    topic_base_url                                     |       date        
---------+---------------------------------------------------------------------------------------+-------------------
 2474098 | http://forum.viva.nl/forum/Kinderen/Gestopt_met_de_pil_januari_09/list_messages/72694 | 11-03-2010, 18:06
 2474099 | http://forum.viva.nl/forum/Kinderen/Gestopt_met_de_pil_januari_09/list_messages/72694 | 11-03-2010, 18:08
 2474100 | http://forum.viva.nl/forum/Kinderen/Gestopt_met_de_pil_januari_09/list_messages/72694 | 11-03-2010, 18:10
 2474101 | http://forum.viva.nl/forum/Kinderen/Gestopt_met_de_pil_januari_09/list_messages/72694 | 11-03-2010, 18:23
 2474102 | http://forum.viva.nl/forum/Kinderen/Gestopt_met_de_pil_januari_09/list_messages/72694 | 11-03-2010, 18:24
(5 rows)

Time: 0.649 ms

forumslurper=# select mptr.id r_id,mpt.id t_id,mpt.topic_base_url,mptr.date r_date,mpt.date t_date from multi_page_topic_reply mptr join multi_page_topic mpt on mptr.topic_base_url =
 mpt.topic_base_url limit 5;                                                                                                                                                          
 r_id | t_id |                               topic_base_url                               |      r_date       |      t_date       
------+------+----------------------------------------------------------------------------+-------------------+-------------------
 3501 |   16 | http://forum.viva.nl/forum/Gezondheid/ouderdomswratjes/list_messages/53595 | 06-08-2009, 15:16 | 06-08-2009, 15:13
 3502 |   16 | http://forum.viva.nl/forum/Gezondheid/ouderdomswratjes/list_messages/53595 | 06-08-2009, 15:20 | 06-08-2009, 15:13
 3503 |   16 | http://forum.viva.nl/forum/Gezondheid/ouderdomswratjes/list_messages/53595 | 06-08-2009, 15:22 | 06-08-2009, 15:13
 3504 |   16 | http://forum.viva.nl/forum/Gezondheid/ouderdomswratjes/list_messages/53595 | 06-08-2009, 15:24 | 06-08-2009, 15:13
 3505 |   16 | http://forum.viva.nl/forum/Gezondheid/ouderdomswratjes/list_messages/53595 | 06-08-2009, 15:26 | 06-08-2009, 15:13
(5 rows)

Time: 4.782 ms

forumslurper=# select mptr.id r_id,mpt.id t_id,mpt.topic_base_url,mptr.date r_date,mpt.date t_date from multi_page_topic_reply mptr join multi_page_topic mpt on mptr.topic_base_url =
 mpt.topic_base_url limit 25;                                                                                                                                                         
Time: 46.123 ms
forumslurper=# select mptr.id r_id,mpt.id t_id,mpt.topic_base_url,mptr.date r_date,mpt.date t_date from multi_page_topic_reply mptr join multi_page_topic mpt on mptr.topic_base_url =
 mpt.topic_base_url where mpt.topic_base_url = 'http://forum.viva.nl/forum/Gezondheid/ouderdomswratjes/list_messages/53595';
Time: 1600.386 ms
forumslurper=# select mptr.id r_id,mpt.id t_id,mpt.topic_base_url,mptr.date r_date,mpt.date t_date,mptr.sub_page from multi_page_topic_reply mptr join multi_page_topic mpt on mptr.to
pic_base_url = mpt.topic_base_url where mpt.topic_base_url = 'http://forum.viva.nl/forum/Gezondheid/ouderdomswratjes/list_messages/53595';                                            
Time: 1462.950 ms

### We don't have a duplicates issue after all!

forumslurper=# select count(*) from (select mptr.id r_id,mpt.id t_id,mpt.topic_base_url,mptr.date r_date,mpt.date t_date,mptr.sub_page from multi_page_topic_reply mptr join multi_page_topic mpt on mptr.topic_base_url = mpt.topic_base_url where mpt.topic_base_url = 'http://forum.viva.nl/forum/Gezondheid/ouderdomswratjes/list_messages/53595') subquery;
 count 
-------
    30
(1 row)

Time: 1463.420 ms
forumslurper=# select count(*) from (select mptr.id r_id,mpt.id t_id,mpt.topic_base_url,mptr.date r_date,mpt.date t_date,mptr.sub_page from multi_page_topic_reply mptr join multi_page_topic mpt on mptr.topic_base_url = mpt.topic_base_url where mpt.topic_base_url = 'http://forum.viva.nl/forum/Gezondheid/ouderdomswratjes/list_messages/53595' and mptr.date = mpt.date) subquery;
 count 
-------
     0
(1 row)

Time: 1433.635 ms

</pre>
