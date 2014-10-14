﻿Tales Overview
==============

Tales is an extensible service framework built in Java that provides a) contract creation with native support for HTTP, similar to JAX-RS but with strong versioning, b) automatic status/monitoring support, and c) an object mapping storage system with features that take advantage of the column-oriented nature of HBase.

There are three primary objectives for this framework. I want service teams to a) concentrate on building core functionality instead of service scaffolding, b) manage services in a consistent fashion, and c) have the freedom to tailor the framework for their specific needs.

There are a good number of capabilities in Tales and I aspire to document and create more samples over time. There are also updates I would like to make given the feedback and experience of seeing it in production use over the last few of years. 

This framework is one of the underpinnings of the services created at Getjar (http://www.getjar.com/). Getjar is a mobile monetization service supporting over 200 million users.

The current version of Tales is in mid-update:
a) In process of moving from Jetty 8.x/JDK 1.6 to Jetty 9.x/JDK 1.8, and 
b) Adding polymorphic data handling for transport/storage

Note: Right now the storage system is not in a working state and it will be awhile before I update it. 

Related Repositories
--------------------

In total there are three repositories that make up the Tales suite:

* <b>Tales</b>: This is the primary repository that contains only the framework.<br>
https://github.com/Talvish/Tales

* <b>Tales - Samples</b>: This repository contains various samples show the capabilities of the framework. Samples range from simple, to complex, to near real services. <br>
https://github.com/Talvish/Tales-Samples

* <b>Tales - Rigs</b>: This repository contains usable services and their clients built using Tales. The intention is for these components to be used in real environments.<br>
https://github.com/Talvish/Tales-Rigs

A bit of history
----------------

I started building this framework in early 2011 because a) I hadn’t used Java in nearly 10 years and wanted to re-familiarize myself and b) when I looked at other frameworks I found they were lacking in some areas I felt strongly about (e.g. versioned contracts). 
