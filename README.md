# Raml-Module-Builder

Copyright (C) 2016 The Open Library Foundation

This software is distributed under the terms of the Apache License, Version 2.0. See the file ["LICENSE"](https://github.com/folio-org/raml-module-builder/blob/master/LICENSE) for more information.


##### This documentation includes information about the Raml-Module-Builder framework
and examples of how to use it.

The goal of the project is to abstract away as much boilerplate functionality as
possible and allow a developer to focus on implementing business functions. In
other words: **simplify the process of developing a micro service module**.
The framework is RAML driven, meaning a developer / analyst declares APIs that the
'to be developed' module is to expose (via RAML files) and declares the objects
to be used and exposed by the APIs (via JSON schemas). Once the schemas and RAML
files are in place, the framework generates code and offers a number of tools
to help implement the module.

The framework consists of a number of tools:

1. domain-models-api-interfaces - project exposes tools that receive as input
   these RAML files and these JSON schemas, and generates java POJOs and java
   interfaces.

2. domain-models-api-aspects - project exposes tools that enforce strict
   adherence to the RAML declaration to any API call by exposing validation
   functionality.

   - for example: a RAML file may indicate that a specific parameter is
     mandatory or that a query parameter value must be a specific regex pattern.
     The aspects project handles this type of validation for developers so that it
     does not need to be re-developed over and over. More on validation
     [below](https://github.com/folio-org/raml-module-builder#a-little-more-on-validation).

3. domain-models-runtime - project exposes a runtime library which should be
   used to run a module. It is vertx based. When a developer implements the
   interfaces generated by the interfaces project, the runtime library should be
   included in the developed project and run. The runtime library will
   automatically map URLs to the correct implemented function so that developers
   only need to implement APIs, and so all the wiring, validation,
   parameter / header / body parsing, logging (every request is logged in an
   apache like format) is handled by the framework. Its goal is to abstract
   away all boilerplate functionality and allow a module implementation to focus
   on implementing business functions.

   - The runtime framework also exposes hooks that allow developers to
     implement one-time jobs, scheduled tasks, etc.

   - Provides tooling (postgres client, mongodb client, etc.) for developers
     to use while developing their module.

   - Runtime library runs a vertx verticle.

4. rules - Basic Drools functionality allowing module developers to create
   validation rules via .drl files for objects (JSON schemas).

# The basics

![](images/build.png)
![](images/generate.png)
![](images/what.png)

# Implement the interfaces

For example – note the validation annotations generated based on the constraints in the RAML.

![](images/interface_example.png)

- When implementing the interfaces, you must add the @Validate
  annotation to enforce the annotated constraints declared by the interface.

- Note that a Bib entity was passed as a parameter – the runtime framework
  transforms the JSON passed in the body to the correct POJO.

# Set up your pom.xml

After including the maven plugin to generate our sources, we need to add a few
more maven plugins:

- Add the `aspectj-maven-plugin` to your pom. This is required if you
  would like the runtime framework to validate all URLs.

- Add the `maven-resources-plugin` to your pom. This plugin will copy
  your RAML files to the /apidocs directory where they will be made visible
  online (html view) by the runtime framework.

- Add the `maven-shade-plugin` to your pom, indicating the main class to
  run as `RestLauncher` and main verticle as `RestVerticle`. This will create a
  runnable jar with the runtime’s `RestVerticle` serving as the main class.

# Build and run

Do `mvn clean install` ... and run :)

The runtime framework will route URLs in your RAML to the correct method
implementation. It will validate (if `@Validate` was used), log, and expose
various tools.

Notice that no web server was configured or even referenced in the implementing
module - this is all handled by the runtime framework.

Sample projects:

- https://github.com/folio-org/mod-circulation
- https://github.com/folio-org/mod-configuration
- https://github.com/folio-org/mod-acquisitions
- https://github.com/folio-org/mod-acquisitions-postgres


# Get started with a sample working module

Clone / download the framework:

- raml-module-builder - this is the core framework that can be used to help
  developers quickly get a vertx based module up and running. Build via
  `mvn clean install` which will create all the needed jars for the framework.

Clone / download the Circulation sample module -
https://github.com/folio-org/mod-circulation - Build via `mvn clean install`

- This module implements basic circulation APIs.

- RAMLs and JSON schemas can be found in the `ramls` directory.

- Open the pom.xml - notice the jars in the `dependencies` section as
  well as the `plugins` section. The `ramls` directory is passed in the pom.xml via
  a maven exec plugin to the interfaces framework tool, to generate source files
  within the circulation project. The generated interfaces are implemented within
  the project.

- Open the `org.folio.rest.impl` package and notice that the appropriate
  parameters (as described in the RAML) are passed as parameters to these
  functions so that no parameter parsing is needed by the developer.

- **IMPORTANT NOTE:** Every interface implementation - by any module -
  must reside in package `org.folio.rest.impl`. This is the package that is
  scanned at runtime by the runtime framework, to find the needed runtime
  implementations of the generated interfaces.

To run the circulation module, navigate to the `/target/` directory and do
`java -jar circulation-fat.jar`

# Command-line options

- `java.util.logging.config.file=C:\Git\circulation\target\classes\vertx-default-jul-logging.properties`
  (Optional - defaults to /target/classes/vertx-default-jul-logging.properties)

- `embed_mongo=false` (Optional - defaults to false)

- `-Dhttp.port=8080` (Optional - defaults to 8081)

- `embed_postgres=true` (Optional - defaults to false)

- `db_connection=[path]` (Optional - path to an external JSON config file with
  connection parameters to a postgreSQL DB)

  - for example Postgres: `{"host":"localhost", "port":5432, "maxPoolSize":50,
    "username":"postgres","password":"mysecretpassword", "database":"postgres",
    "charset":"windows-1252", "queryTimeout" : 10000}`

- `drools_dir=[path]` (Optional - path to an external drools file. By default,
  `*.drl` files in the /resources/rules directory are loaded)

- `mongo_connection=[path]` (Optional - path to an external JSON config file
  with connection parameters to a MongoDB)

  - for example MongoDB: `{
  "db_name": "indexd_test",
  "host" : "ec2-52-41-57-165.us-west-2.compute.amazonaws.com",
  "port" : 27017,
  "maxPoolSize" : 3,
  "minPoolSize" : 1,
  "maxIdleTimeMS" : 300000,
  "maxLifeTimeMS" : 3600000,
  "waitQueueMultiple"  : 100,
  "waitQueueTimeoutMS" : 10000,
  "maintenanceFrequencyMS" : 2000,
  "maintenanceInitialDelayMS" : 500,
  "connectTimeoutMS" : 300000,
  "socketTimeoutMS"  : 100000,
  "sendBufferSize"    : 8192,
  "receiveBufferSize" : 8192,
  "keepAlive" : true
}`

- `-XX:+HeapDumpOnOutOfMemoryError -XX:+PrintGCDetails -XX:+PrintGCTimeStamps
  -Xloggc:C:\Git\circulation\gc.log` (Optional)

# Creating a new module

Pre step 1: Clone / Download the raml-module-builder project and `mvn clean install`

### Step 1: Describe the APIs to be exposed by the new module

Create a new project - Create a RAML file/s and define the API endpoints to
be exposed by the module. Place this in the project - for example `/ramls`
directory within the root of the project.

`ebook.raml`

```sh
#%RAML 0.8

title: e-BookMobile API
baseUri: http://api.e-bookmobile.com/{version}
version: v1

schemas:
 - book: !include ebook.schema


/ebooks:
  /{bookTitle}:
  get:
    queryParameters:
       author:
         displayName: Author
         type: string
         description: An author's full name
         example: Mary Roach
         required: true
       publicationYear:
         displayName: Pub Year
         type: number
         description: The year released for the first time in the US
         example: 1984
         required: false
       rating:
         displayName: Rating
         type: number
         description: Average rating (1-5) submitted by users
         example: 3.14
         required: false
       isbn:
         displayName: ISBN
         type: string
         minLength: 10
         example: 0321736079?
    responses:
      200:
       body:
         application/json:
          schema: book
          example: |
             {
               "bookdata": {
                 "id": "SbBGk",
                 "title": "Stiff: The Curious Lives of Human Cadavers",
                 "description": null,
                 "datetime": 1341533193,
                 "genre": "science",
                 "author": "Mary Roach",
                 "link": "http://e-bookmobile.com/books/Stiff",
               },
               "success": true,
               "status": 200
             }
```

Create JSON schemas indicating the objects exposed by the module:

`ebook.schema`

```sh

{
  "$schema": "http://json-schema.org/draft-04/schema#",
  "type": "object",
  "properties": {
    "bookdata": {
      "type": "object",
      "properties": {
        "id": {
          "type": "string"
        },
        "title": {
          "type": "string"
        },
        "description": {
          "type": "null"
        },
        "datetime": {
          "type": "integer"
        },
        "genre": {
          "type": "string"
        },
        "author": {
          "type": "string"
        },
        "link": {
          "type": "string"
        }
      },
      "required": [
        "id",
        "title",
        "description",
        "datetime",
        "genre",
        "author",
        "link"
      ]
    },
    "success": {
      "type": "boolean"
    },
    "status": {
      "type": "integer"
    }
  },
  "required": [
    "bookdata",
    "success",
    "status"
  ]
}
```

### Step 2: Include the jars in your project pom.xml

```sh
        <dependency>
			<groupId>org.folio</groupId>
			<artifactId>domain-models-api-interfaces</artifactId>
			<version>0.0.1-SNAPSHOT</version>
		</dependency>
		<dependency>
			<groupId>org.folio</groupId>
			<artifactId>domain-models-runtime</artifactId>
			<version>0.0.1-SNAPSHOT</version>
		</dependency>
```

### Step 3: Add the plugins to your pom.xml

Four plugins should be declared in the pom.xml file:

- The aspect plugin, which will pre-compile your code with validation aspects
  provided by the framework - remember the **@Validate** annotation. The
  validation supplied by the framework verifies that headers are passed
  correctly, parameters are of the correct type and contain the correct content
  as indicated by the RAML file.

- The shade plugin, which will generate a fat-jar runnable jar. While the
  shade plugin is not mandatory, it does makes things easier. The important thing to
  notice is the main class that will be run when running your module. Notice the
  `Main-class` and `Main-Verticle` in the shade plugin configuration.

- The maven exec plugin, which will generate the POJOs and interfaces based on
  the RAML files.

- The maven resource plugin, which will copy the RAML files into a directory
  under `/apidocs` so that the runtime framework can pick it up and display html
  documentation based on the RAML files.

Add `ramlfiles_path` property indicating the location of the RAML directory:

```sh
	<properties>
		<ramlfiles_path>${basedir}/ramls</ramlfiles_path>
	</properties>
```

Add the plugins:

```sh

			<plugin>
				<artifactId>maven-compiler-plugin</artifactId>
				<version>3.1</version>
				<configuration>
					<source>1.8</source>
					<target>1.8</target>
					<encoding>UTF-8</encoding>
				</configuration>
			</plugin>

			<plugin>
				<groupId>org.codehaus.mojo</groupId>
				<artifactId>exec-maven-plugin</artifactId>
				<version>1.5.0</version>
				<executions>
					<execution>
						<id>generate_interfaces</id>
						<phase>generate-sources</phase>
						<goals>
							<goal>java</goal>
						</goals>
						<configuration>
							<mainClass>org.folio.rest.tools.GenerateRunner</mainClass>
							<!-- <executable>java</executable> -->
							<cleanupDaemonThreads>false</cleanupDaemonThreads>
							<systemProperties>
								<systemProperty>
									<key>project.basedir</key>
									<value>${basedir}</value>
								</systemProperty>
								<systemProperty>
									<key>raml_files</key>
									<value>${ramlfiles_path}/circulation</value>
								</systemProperty>
							</systemProperties>
						</configuration>
					</execution>
				</executions>
			</plugin>

			<plugin>
				<groupId>org.codehaus.mojo</groupId>
				<artifactId>aspectj-maven-plugin</artifactId>
				<version>1.8</version>
				<configuration>
					<verbose>true</verbose>
					<showWeaveInfo>false</showWeaveInfo>
					<complianceLevel>1.8</complianceLevel>
					<includes>
						<include>**/impl/*.java</include>
						<include>**/*.aj</include>
					</includes>
					<aspectDirectory>src/main/java/org/folio/rest/annotations</aspectDirectory>
					<XaddSerialVersionUID>true</XaddSerialVersionUID>
					<showWeaveInfo>true</showWeaveInfo>
					<aspectLibraries>
						<aspectLibrary>
							<groupId>org.folio</groupId>
							<artifactId>domain-models-api-aspects</artifactId>
						</aspectLibrary>
					</aspectLibraries>
				</configuration>
				<executions>
					<execution>
						<goals>
							<goal>compile</goal>
						</goals>
					</execution>
				</executions>
				<dependencies>
					<dependency>
						<groupId>org.aspectj</groupId>
						<artifactId>aspectjrt</artifactId>
						<version>1.8.9</version>
					</dependency>
					<dependency>
						<groupId>org.aspectj</groupId>
						<artifactId>aspectjtools</artifactId>
						<version>1.8.9</version>
					</dependency>
				</dependencies>
			</plugin>
			<plugin>
				<artifactId>maven-resources-plugin</artifactId>
				<version>3.0.1</version>
				<executions>
					<execution>
						<id>copy-resources</id>
						<phase>prepare-package</phase>
						<goals>
							<goal>copy-resources</goal>
						</goals>
						<configuration>
							<outputDirectory>${basedir}/target/classes/apidocs/raml</outputDirectory>
							<resources>
								<resource>
									<directory>${ramlfiles_path}</directory>
									<filtering>true</filtering>
								</resource>
							</resources>
						</configuration>
					</execution>
				</executions>
			</plugin>

			<plugin>
				<groupId>org.apache.maven.plugins</groupId>
				<artifactId>maven-shade-plugin</artifactId>
				<version>2.3</version>
				<executions>
					<execution>
						<phase>package</phase>
						<goals>
							<goal>shade</goal>
						</goals>
						<configuration>
							<transformers>
								<transformer
									implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
									<manifestEntries>
										<Main-Class>org.folio.rest.RestLauncher</Main-Class>
										<Main-Verticle>org.folio.rest.RestVerticle</Main-Verticle>
									</manifestEntries>
								</transformer>
							</transformers>
							<artifactSet />
							<outputFile>${project.build.directory}/${project.artifactId}-fat.jar</outputFile>
						</configuration>
					</execution>
				</executions>
			</plugin>
```

### Step 4: Build your project

Do `mvn clean install`

This should:

- Create java interfaces for each added RAML file.

- Each interface will contain functions to implement (each function represents
  an API endpoint declared in the RAML).

- The parameters within each function interface will be annotated with
  validation annotations that were declared in the RAML. So, if a trait was
  marked as mandatory, it will be marked as @NOT_NULL. This is not something that
  needs to be handled by the implementer. This is handled by the framework,
  which handles validation.

- POJOs - The JSON schemas will be generated into java objects.

### Step 5: Implement the generated interfaces

Implement the interfaces associated with the RAML files you created. An
interface is generated for every root endpoint in the RAML file you added to
the `raml` project. So, for the ebook RAML an
`org.folio.rest.jaxrs.resource.EbooksResource` interface will be generated.
Note that the `org.folio.rest.jaxrs.resource` will be the package for every
generated interface.

See an [example](#function-example) of an implemented function.

## Adding an init() implementation

It is possible to add custom code that will run once before the application is deployed
(e.g. to init a DB, create a cache, create static variables, etc.) by implementing 
the `InitAPIs` interface. You must implement the
`init(Vertx vertx, Context context, Handler<AsyncResult<Boolean>> resultHandler)`.
Currently the implementation should sit in the
`org.folio.rest.impl` package in the implementing project. The implementation
will run during verticle deployment. The verticle will not complete deployment
until the init() completes. The init() function can do anything basically. but
it must call back the Handler. For example:

```sh
public class InitAPIs implements InitAPI {

  public void init(Vertx vertx, Context context, Handler<AsyncResult<Boolean>> resultHandler){
    try {
      sayHello();
      resultHandler.handle(io.vertx.core.Future.succeededFuture(true));
    } catch (Exception e) {
      e.printStackTrace();
      resultHandler.handle(io.vertx.core.Future.failedFuture(e.getMessage()));
    }
  }
}
```
 

## Adding code to run periodically

It is possible to add custom code that will run periodically. For example,
to ongoingly check status of something in the system and act upon that.
Need to implement the PeriodicAPI interface:

```sh
public interface PeriodicAPI {
  /** this implementation should return the delay in which to run the function */
  public long runEvery();
  /** this is the implementation that will be run every runEvery() milliseconds*/
  public void run(Vertx vertx, Context context);

}
```

For example:

```sh

public class PeriodicAPIImpl implements PeriodicAPI {


  @Override
  public long runEvery() {
    return 45000;
  }

  @Override
  public void run(Vertx vertx, Context context) {
    try {
      InitAPIs.amIMaster(vertx, context, v-> {
        if(v.failed()){
          //TODO - what should be done here?
        }
      });
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
```

## Adding a shutdown hook

It is possible to add custom code that will run just before the verticle is
undeployed and the JVM stopped. This will occur on graceful shutdowns - but can
not be guaranteed to run if the JVM is forcefully shutdown.

The interface to implement:

```sh
public interface ShutdownAPI {

  public void shutdown(Vertx vertx, Context context, Handler<AsyncResult<Void>> handler);

}
```

An implementation example:

```sh
public class ShutdownImpl implements ShutdownAPI {

  @Override
  public void shutdown(Vertx vertx, Context context, Handler<AsyncResult<Void>> handler) {
    try {
      AuditLogger.getInstance().publish(new LogRecord(Level.INFO, "closing audit logger"));
      AuditLogger.getInstance().close();
      handler.handle(io.vertx.core.Future.succeededFuture());
    }
    catch (Exception e) {
      e.printStackTrace();
      handler.handle(io.vertx.core.Future.failedFuture(e.getMessage()));
    }
  }
}
```



Note that when implementing the generated interfaces it is possible to add a constructor to the implementing class. This constructor will be called for every API call. This is another way you can implement custom code that will run per request.


## Implementing file uploads

To create an api that allows file uploads, do one of the following:

1. Use the `/apis/admin/upload` url which every module using the platform inherits. This will stream a file to the `java.io.tmpdir` directory and can optionally send a notification when the upload completes via the vertx event bus.
 - The RAML file `/resources/raml/admin.raml` describes the API parameters.
 - Example of a listening service waiting for notifications:
 https://github.com/folio-org/mod-circulation/blob/master/src/main/java/org/folio/rest/impl/ProcessUploads.java 

2. The second option is to declare a file upload API in your RAML:

```sh
post:
      description: |
         Enters the file content for an existing entity.
         Use the "multipart-form/data" content type to upload a file which content will become the file-content
      body:
        multipart/form-data:
          formParameters:
            file:
              description: The file to be uploaded
              required: true
              type: file
              example: <<exampleItem>>
```

Please see the https://github.com/folio-org/mod-configuration/blob/master/ramls/configuration/config.raml file for an example.
Notice the `/configurations/rules` entry in the RAML



(see a client example call in Java here:)

https://github.com/folio-org/raml-module-builder/blob/master/domain-models-runtime/src/test/java/org/folio/DemoRamlRestTest.java


The body content should look something like this: 



```sh
------WebKitFormBoundaryNKJKWHABrxY1AdmG
Content-Disposition: form-data; name="config.json"; filename="kv_configuration.sample"
Content-Type: application/octet-stream

<file content 1>

------WebKitFormBoundaryNKJKWHABrxY1AdmG
Content-Disposition: form-data; name="sample.drl"; filename="Sample.drl"
Content-Type: application/octet-stream

<file content 2>

------WebKitFormBoundaryNKJKWHABrxY1AdmG
```


The generated API interface will have a function signiture of:

```sh
public void postConfigurationsRules(String authorization, String lang, MimeMultipart entity, 
Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) throws Exception
```

The `MimeMultipart` parameter can be used to retrieve the contents in the
following manner:

```sh
int parts = entity.getCount();
for (int i = 0; i < parts; i++) {
        BodyPart part = entity.getBodyPart(i);
        Object o = part.getContent();
}
```

where each section in the body (separated by the boundary) is a "part"

see an example here:
https://github.com/folio-org/mod-configuration/blob/master/src/main/java/org/folio/rest/impl/ConfigAPI.java (see `postConfigurationsRules` function)

## MongoDB integration

By default an embedded mongoDB is included in the runtime but is not run by
default. To change that add `embed_mongo=true` to the command line
(`java -jar circulation-fat.jar embed_mongo=true`). Connection parameters to a 
non-embedded mongoDB can be placed in `resources/mongo-conf.json` or passed 
via the command line.

The runtime framework exposes a mongoDB async client which offers CRUD
operations in an ORM type fashion. Please see `/domain-models-runtime/src/main/java/org/folio/rest/persist/MongoCRUD.java` for the available APIs exposed. 

Extensive usage examples can be found in the following classes:
https://github.com/folio-org/mod-circulation/blob/master/src/main/java/org/folio/rest/impl/PatronAPI.java
https://github.com/folio-org/mod-configuration/blob/master/src/main/java/org/folio/rest/impl/ConfigAPI.java



## PostgreSQL integration

By default an embedded PostgreSQL is included in the runtime, but is not run by
default. To change that add `embed_postgres=true` to the command line
(`java -jar circulation-fat.jar embed_postgres=true`).
Connection parameters to a
non-embedded mongoDB can be found in `resources/postgres-conf.json` or passed
via the command line.

The runtime framework exposes a postgreSQL async client which offers CRUD
operations in an ORM type fashion.

**Important Note:** The PostgreSQL client currently implemented assumes
JSONB tables in PostgreSQL. This is not mandatory and developers can work with
regular postgreSQL tables but will need to implement their own data access
layer.

Currently the expected format is:

```sh
create table <schema>.<table_name> (
	_id SERIAL PRIMARY KEY,
	jsonb JSONB NOT NULL
);
```

**Schemas / Tables should be created manually.**

Dates (creation / updated) maybe added later on.

credentials when running in embedded mode:
port: 6000
host: 127.0.0.1
user: username
password: password


Examples:

Saving a POJO:

```sh
PoLine poline = new PoLine();

...

postgresClient.save(beginTx, TABLE_NAME_POLINE, poline , reply -> {...
```

Querying for similar POJOs in the DB (with or without additional criteria):

```sh
Criterion criterion = Criterion.json2Criterion(query);

criterion.setLimit(new Limit(limit)).setOffset(new Offset(offset));

postgresClient.get(TABLE_NAME_POLINE, PoLine.class, criterion,
              reply -> {...
```

Usage examples:
https://github.com/folio-org/mod-acquisitions-postgres/blob/master/src/main/java/org/folio/rest/impl/POLine.java


## Query Syntax

Note for modules using the built-in mongoDB client / Postgres client support:
Query syntax varies depending on whether the module is a mongoDB or a
postgreSQL backed module.

***For mongoDB*** backed modules, the native mongoDB syntax should be passed in
the query string as a parameter.

An example from the Circulation sample module:
`http://localhost:8081/apis/patrons?query={"$and":[{"total_loans": { "$lt": 60 } }, { "contact_info.patron_address_local.city": "London" } ]}`

For usage examples, see:
https://github.com/folio-org/mod-circulation/blob/master/src/main/java/com/folio/rest/impl/PatronAPI.java

***For postgreSQL*** backed modules, the following JSON format can be sent:
`[{"field":"''","value":"","op":""}]`

Some examples:
```sh
http://localhost:8081/apis/po_lines?query=[{"field":"'po_line_status'->>'value'","value":"fa(l|t)se","op":"SIMILAR TO"}]

[{"field":"'fund_distributions'->'amount'->>'sum'","value":120,"op":">"}]
[{"field":"'po_line_status'->>'value'","value":"SENT","op":"like"},{"field":"'owner'->>'value'","value":"MITLIBMATH","op":"="}, {"op":"AND"}]
[[{"field":"'po_line_status'->>'value'","value":"SENT","op":"like"},{"field":"'owner'->>'value'","value":"MITLIBMATH","op":"="}, {"op":"AND"}],[{"field":"'po_line_status'->>'value'","value":"SENT","op":"like"}],[{"field":"'rush'","value":"false","op":"="}], [{"field":"'po_line_status'->>'value'","value":"SENT","op":"like"},{"field":"'type'->>'value'","value":"PRINT_ONETIME","op":"="}, {"op":"OR"}]]
[{"field":"'ebook_url'","value":null,"op":"IS NOT NULL"}]
[{"field":"'ebook_url'","value":null,"op":"IS NULL"}]
[{"field":"'price'","value":{"sum": "150.0"},"op":"@>"}]
[{"field":"'po_line_status'->>'value'","value":"fa(l|t)se","op":"SIMILAR TO"}, {"op":"NOT"}]
[{"field":"'fund_distributions'->[]->'amount'->>'sum'","value":120,"op":">"}]
[{"field":"'notes'","value":null,"op":"="}]
```

See usage here:
https://github.com/folio-org/mod-acquisitions-postgres/blob/master/src/main/java/com/folio/rest/impl/POLine.java

## Drools integration

The framework scans the `/resources/rules` path in an implemented project for
`*.drl` files. A directory can also be passed via the command line. Those files are
loaded and are applied automatically to all objects passed in the body (post,
put) by the runtime framework. This allows for more complex validation of
passed objects.

- For example, if two specific fields can logically be null, but not at the
  same time - that can easily be implemented with a Drool, as those types of
  validations are harder to create in a RAML file.

- The `rules` project also exposes the drools session and allows validation
  within the implemented APIs. See the `tests` in the `rules` project.

For example: (Sample.drl)

```
package com.sample

import org.folio.rest.jaxrs.model.Patron;

rule "Patron needs one ID at the least"

	no-loop

    when
        p : Patron( patronBarcode  == null, patronLocalId == null )
    then
    	throw new java.lang.Exception("Patron needs one ID field populated at the least");
end
```

## Messages

The runtime framework comes with a set of messages it prints out to the logs /
sends back as error responses to incorrect API calls. These messages are
language specific. In order to add your own message files, place the files in
your project under the `/resources/messages` directory.

Note that the format of the file names should be:
`[lang_2_letters]_messages.yyy - for example: en_messages.prop`

For example: 
In the circulation project, the messages file can be found at `/circulation/src/main/resources/en_messages.prop` with the following content:
```sh
20002=Operation can not be calculated on a Null Amount
20003=Unable to pay fine, amount is larger then owed
20004=The item {0} is not renewable
20005=Loan period must be greater than 1, period entered: {0}
```
The circulation project exposes these messages as enums for easier usage in the code:

```sh
package org.folio.utils;

import org.folio.rest.tools.messages.MessageEnum;

public enum CircMessageConsts implements MessageEnum {

  OperationOnNullAmount("20002"),
  FinePaidTooMuch("20003"),
  NonRenewable("20004"),
  LoanPeriodError("20005");
  
  private String code;
  private CircMessageConsts(String code){
    this.code = code;
  }
  public String getCode(){
    return code;
  }
}
```

Usage:

`private final Messages messages = Messages.getInstance();`

`messages.getMessage(lang, CircMessageConsts.OperationOnNullAmount);`

Note: parameters can also be passed when relevant. The raml-module-builder runtime also exposes generic error message enums which can be found at `/domain-models-runtime/src/main/java/org/folio/rest/tools/messages/MessageConsts.java` 

## Documentation

The runtime framework includes a web application which exposes RAMLs in a view
friendly HTML format. The `maven-resources-plugin` plugin described earlier
copies the RAML files into the correct directory in your project, so that the
runtime framework can access it and expose it.

```
http://[host]:[port]/apidocs/index.html?raml=raml/circulation/patrons.raml
```

## Logging

As stated earlier (command line options), you can pass a configuration file with logging configurations. However, you may also change log levels via the `/admin` API provided by the framework.

For example: 

Change log level of all classes to FINE

(PUT) `http://localhost:8081/apis/admin/loglevel?level=FINE`

Get log level of all classes

(GET) `http://localhost:8081/apis/admin/loglevel`

A `java_package` parameter can also be passed to change the log level of a specific package. For Example:

 `http://localhost:8081/apis/admin/loglevel?level=INFO&java_package=org.folio.rest.persist.MongoCRUD`
 
 `http://localhost:8081/apis/admin/loglevel?level=INFO&java_package=org.folio.rest.persist`
 

## A Little More on Validation

Query parameters and header validation
![](images/validation.png)

### Object validations

![](images/object_validation.png)

### function example
```sh

  @Validate
  @Override
  public void putPatronsByPatronId(String patronId, String authorization, String lang, Patron entity,
      Handler<AsyncResult<Response>> asyncResultHandler, Context context) throws Exception {

    try {
      JsonObject q = new JsonObject();
      q.put("_id", patronId);
      System.out.println("sending... putPatronsByPatronId");
      context.runOnContext(v -> {
        MongoCRUD.getInstance(context.owner()).update(Consts.PATRONS_COLLECTION,
            entity, q,
            reply -> {
              try {
                asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(PutPatronsByPatronIdResponse.withNoContent()));
              } catch (Exception e) {
                e.printStackTrace();
                asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(PutPatronsByPatronIdResponse
                    .withPlainInternalServerError(messages.getMessage(lang, "10001"))));
              }
            });
      });
    } catch (Exception e) {
      e.printStackTrace();
      asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(PutPatronsByPatronIdResponse.withPlainInternalServerError(messages
          .getMessage(lang, "10001"))));
    }
  }
```

## Some REST examples

Have these in the headers - currently not validated hence not mandatory:
- Authorization: Bearer a2VybWl0Omtlcm1pdA==
- Accept: application/json,text/plain
- Content-Type: application/json;

Example 1. Add a fine to a patron (post)

```
http://localhost:8080/apis/patrons/56dbe25ea12958478cec42ba/fines
{
  "fine_amount": 10,
  "fine_outstanding": 0,
  "fine_date": 1413879432,
  "fine_pay_in_full": true,
  "fine_pay_in_partial": false,
  "fine_note": "aaaaaa",
  "item_id": "56dbe160a129584dc8de7973",
  "fine_forgiven": {
 "user": "the cool librarian",
 "amount": "none"
  },
  "patron_id": "56dbe25ea12958478cec42ba"
}
```

Example 2. get fines for patron with id 56dbe25ea12958478cec42ba

```
http://localhost:8080/apis/patrons/56dbe25ea12958478cec42ba/fines
```

Example 3. get a specific patron

```
http://localhost:8080/apis/patrons/56dbe25ea12958478cec42ba
```

Example 4. get all patrons

```
http://localhost:8080/apis/patrons
```

Example 5. delete a patron (delete)

```
http://localhost:8080/apis/patrons/56dbe791a129584a506fb41a
```

Example 6. add a patron (post)

```
http://localhost:8080/apis/patrons
{
 "status": "ACTIVE",
 "patron_name": "Smith,John",
 "patron_barcode": "00007888",
 "patron_local_id": "abcdefd",
 "contact_info": {
  "patron_address_local": {
   "line1": "Main Street 1",
   "line2": "Nice building near the corner",
   "city": "London",
   "state_province": "",
   "postal_code": "",
   "address_note": "",
   "start_date": "2013-12-26Z"
  },
  "patron_address_home": {
   "line1": "Main Street 1",
   "line2": "Nice building near the corner",
   "city": "London",
   "state_province": "",
   "postal_code": "",
   "address_note": "",
   "start_date": "2013-12-26Z"
  },
  "patron_address_work": {
   "line1": "Main Street 1",
   "line2": "Nice building near the corner",
   "city": "London",
   "state_province": "",
   "postal_code": "",
   "address_note": "",
   "start_date": "2013-12-26Z"
  },
  "patron_email": "johns@mylib.org",
  "patron_email_alternative": "johns@mylib.org",
  "patron_phone_cell": "123456789",
  "patron_phone_home": "123456789",
  "patron_phone_work": "123456789",
  "patron_primary_contact_info": "patron_email"
 },
 "total_loans": 50,
 "total_fines": "100$",
 "total_fines_paid": "0$",
 "patron_code": {
  "value": "CH",
  "description": "Child"
 }
}
```






