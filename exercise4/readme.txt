Tipsdb.

TipsDB is a three tier J2EE application that allows the user to search for performance tips. The tiers of the application are; a web browser client, Jetty Servlet engine, and HSQL database.

TipsDB supports two types of queries, a wildcard and a keyword search. The wildcard search is an in-memory search while as the keyword search uses the database to complete its task. The URLs for wildcard and keyword search are http://localhost:8080/tips/wildcard/ and http://localhost:8080/tips/keyword respectfully.

The following steps must be performed in order for the complete application to function.

1) Compile using compile.bat or compile.sh
2) Start HSQLDB by running the dbstart script in the db directory. You do not need to create or load the db
3) Start the web tier by running appserverStart.bar or appserverStart.sh.
4) Start a browser and enter one of the URLS. A search box should appear in which you can type in a word and have a result returned and rendered in the browser.
