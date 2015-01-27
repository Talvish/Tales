Tales Overview
==============

Tales is an extensible service framework built in Java that provides a) service contract creation with an HTTP implementation (similar to JAX-RS but with strong versioning), b) automatic status/monitoring support, c) the starting of security support including JWT / capabilities, and, previously, d) an object mapping storage system with features that take advantage of the column-oriented nature of HBase.

There are three primary objectives for this framework. I want service teams to a) concentrate on building core functionality instead of service scaffolding, b) manage services in a consistent and simple fashion, and c) have the freedom to tailor the framework for their specific needs.

There are a good number of capabilities in Tales and I aspire to document and create more samples over time. There are also updates I would like to make given the feedback and experience of seeing it in production use over the last few of years. 

This framework is one of the underpinnings of the services created at Getjar (http://www.getjar.com/). Getjar is a mobile monetization service supporting over 300 million users.

Tales started its life on Jetty 7.x/JDK 1.6 and now requires Jetty 9.x/JDK 1.8. While Jetty is used it isn't directly exposed in the framework and developers, when creating services, generally do not deal with servlets or other J2EE-like classes and configuration.

For more details please view the [wiki pages](https://github.com/Talvish/Tales/wiki).

Latest Changes
--------------------
January 26, 2015 - Primary updates in Tales 1.6.6 include:
* Annotated Java class configuration support
* ResourceClient updated to use new class-based configuration pattern
* Misc items (OPTIONS responses, deserialization of maps bug fix)

January 7, 2015 - Primary updates in Tales 1.6.5 include:
* New json configuration source for the configuration management system (supports include files, profiles, inheritence, overrides, deferred definitions, etc)
* Preliminary auth-related features include json web tokens and capabilities

Project/Jar Overview
--------------------
There are four active projects and one inactive project in this repository:

* <b>common</b>: This project contains a wide variety of helper classes used by the other Tales projects and meant to be used by developers using Tales. It contains classes covering the basics for contracts, helpers for reflection, data translation and serialization, in addition to classes for doing configuration and execution status tracking/monitoring.

* <b>security</b>: This is a new and somewhat experimental project around security. Currently it has a way to create and verify json web tokens (JWT) and define capabilities.

* <b>service</b>: This project is used to create services. It contains classes for service hosting, classes for creating HTTP resources/contracts, HTTP contracts for service administration, and base classes for developers to create their own network interfaces and contract mechanisms beyond HTTP.

* <b>client</b>: This project makes it easier to create service clients for HTTP resources/contracts. Ideally a tool would exist to automatically create clients, and that may happen at some point, but this project is a stepping stone. 

* <b>storage</b>: This project doesn't work and currently exists as a reference only. In older versions of the framework it allowed an easy way to map Java objects into NoSQL stores (particularly HBase) in a manner similar to object-relational mappers. The difference was, this project allowed you to do interesting mappings of aggregated objects and collections into tables, columns and column families.

Related Repositories
--------------------

In total there are three repositories that make up the Tales suite:

* <b>Tales</b>: This is the primary repository that contains only the framework.<br>
https://github.com/Talvish/Tales

* <b>Tales - Samples</b>: This repository contains samples that show various, though not all, capabilities of the framework. Samples range from simple, to complex, to near real services. <br>
https://github.com/Talvish/Tales-Samples

* <b>Tales - Rigs</b>: This repository contains usable services and their clients built using Tales. The intention is for these components to be used in real environments.<br>
https://github.com/Talvish/Tales-Rigs

A bit of history
----------------

I started building this framework in early 2011 because a) I hadn’t used Java in nearly 10 years and wanted to re-familiarize myself and b) when I looked at other frameworks I found they were lacking in some areas I felt strongly about (e.g. versioned contracts). 
