# RMB upgrading notes

These are notes to assist upgrading to newer versions.
See the [NEWS](../NEWS.md) summary of changes for each version.

* [Version 30.2](#version-302)
* [Version 30.0](#version-300)
* [Version 29.5](#version-295)
* [Version 29.2](#version-292)
* [Version 29](#version-29)
* [Version 28](#version-28)
* [Version 27.1](#version-271)
* [Version 27](#version-27)
* [Version 26](#version-26)
* [Version 25](#version-25)
* [Version 20](#version-20)

## Version 30.2

* [RMB-652](https://issues.folio.org/browse/RMB-652) error message
  "may not be null" changed to "must not be null" (hibernate-validator)

## Version 30.0

* [RMB-246](https://issues.folio.org/browse/RMB-246) Switch to
    [vertx-pg-client](https://vertx.io/docs/vertx-pg-client/java/).
  * All functions that previously returned `UpdateResult` now return
    `RowSet<Row>`. From that result, the number of rows affected by
    SQL was `getUpdated()`, it is now `rowCount()`.
  * All functions that previously returned `ResultSet` now return
    `RowSet<Row>`. From that result, the number of rows affected by
    SQL is `rowCount()`. The `size()` method returns number of rows
    returned. An iterator to go through rows is obtained by calling
    `iterator()`.
  * `PostgresClient.selectSingle` returns `Row` rather than `JsonArray`.
  * PostgreSQL `JSONB` type was previously represented by Java `String`,
    now by Java `JsonObject` (return type and parameter type).
  * PostgreSQL `UUID` type (used for `id` and foreign keys columns) was
    previously represented by Java `String`, now by Java `UUID`
    (return type and parameter type).
  * In prepared/parameterized queries replace '?' signs by '$1', '$2' and so on
    and the parameters argument type `JsonArray` by `Tuple`.
  * Class `SQLConnection` is now provided by RMB. The same class name
    was used for the SQL client. `io.vertx.ext.sql.SQLConnection` ->
    `org.folio.rest.persist.SQLConnection`.
  * `PostgresClient.getClient()` is no longer public. If you need a
    connection, use `PostgresClient.startTx()`. For modules that wish to use
    vertx-pg-client directly, `PostgresClient.getConnection`  is offered -
    it returns `PgConnection` from the pool that is managed by `PostgresClient`.
  * Replace `exception.getMessage` by `PgExceptionUtil.getMessage(exception)`
    to mimic the old `GenericDatabaseException.getMessage(e)` because the
    new `PgException.getMessage(e)` returns the message field only, no SQL error code,
    no detail.
  * `PgExceptionFacade.getTable` removed.
  * `PgExceptionFacade.getIndex` removed.
  * `PgExceptionFacade.selectStream` without SQLConnection has been
     removed. Streams must be executed within a transaction.
  * `PostgresClient.mutate` removed (deprecated since Oct 2018).
* [RMB-619](https://issues.folio.org/browse/RMB-619)
  [Deprecation due to upgrading to Vert.x 3.9](https://github.com/vert-x3/wiki/wiki/3.9.0-Deprecations-and-breaking-changes):
  * Replace `Verticle#start(Future<Void>)` and `Verticle#stop(Future<Void>)` by
    `Verticle#start(Promise<Void>)` and `Verticle#stop(Promise<Void>)`
  * Replace `Future.setHandler(ar -> …)` by `Future.onComplete(ar -> …)`
* Vert.x 4 will split `Future` into `Future` and `Promise`
  * Vert.x 3 `Future<T>` extends `AsyncResult<T>` _and_ `Handler<AsyncResult<T>>`
  * Vert.x 4 `Future<T>` extends `AsyncResult<T>` _only_
  * Vert.x 4 `Promise<T>` extends `Handler<AsyncResult<T>>` _only_
  * When writing or changing code and a `Handler<AsyncResult<T>>` is needed
    use `promise`, not `promise.future()`, to be prepared for Vert.x 4.
* [RMB-624](https://issues.folio.org/browse/RMB-624) Fix invalid RAML sample
  JSON files, otherwise GenerateRunner/SchemaDereferencer will fail with
  InvocationTargetException/DecodeException "Failed to decode".
  Hint: Use `for i in *; do jq empty $i || echo $i; done` to list invalid JSONs.
* Update Vert.x to 3.9.1, consider using
  [Vert.x Bill of Materials (Maven BOM)](https://github.com/vert-x3/vertx-stack#bills-of-materials-maven-bom)
  (content: [vertx-dependencies pom.xml](https://github.com/vert-x3/vertx-dependencies/blob/3.9.1/pom.xml#L49-L54))
  for io.vertx:\*, com.fasterxml.jackson.core:\* and com.fasterxml.jackson.dataformat:\* dependencies.
  Remove jackson-databind dependency from pom.xml if module doesn't use it directly.

## Version 29.5

* [RMB-587](https://issues.folio.org/browse/RMB-587) Write access to `public` schema has been removed, it is no longer in `search_path` and should be used for Postgres extensions only, prepend `public.` when calling their methods and operators. Example: RMB replaces `gin_trgm_ops` by `public.gin_trgm_ops` in indexes that RMB maintains.
* [RMB-588](https://issues.folio.org/browse/RMB-588) Consider removing
  `<directory>src/main/resources</directory> <filtering>true</filtering>`
  from pom.xml (or use a more specific directory tree). For details see [FOLIO-2548](https://issues.folio.org/browse/FOLIO-2548).
* [RMB-594](https://issues.folio.org/browse/RMB-594) RMB no longer uses a random order, but a fixed order to generate the .java classes from the .raml and .json files. This may consistently fail the build if two classes with the same name are generated. To prevent the second from overwriting the first, set a different class name using "javaType": "org.folio.rest.jaxrs.model.Bar". For example for link-field "folio:isVirtual" annotations for mod-graphql.

## Version 29.2

* [RMB-532](https://issues.folio.org/browse/RMB-532) Use PostgresClient.get without the unused and deprecated setId parameter.

## Version 29

* [RMB-529](https://issues.folio.org/browse/RMB-529) The update to Vert.x 3.8.4 deprecates several elements,
  including Json#mapper, Json#prettyMapper and Json#decodeValue:
  [3.8.2 Deprecations](https://github.com/vert-x3/wiki/wiki/3.8.2-Deprecations-and-breaking-changes),
  [3.8.3 Deprecations](https://github.com/vert-x3/wiki/wiki/3.8.3-Deprecations-and-breaking-changes)
* [RMB-510](https://issues.folio.org/browse/RMB-510) Tenant POST `/_/tenant` must include a body. Client that
  used an empty request must now include a body with at least:
  `{"module_to":"module-id"}`. This means that for modules that are using
  `org.folio.rest.client.TenantClient` that previously passed `null` as for
  `org.folio.rest.jaxrs.model.TenantAttributes` must now create
  `TenantAttributes` with `moduleTo` property.
* [RMB-497](https://issues.folio.org/browse/RMB-497) removed PostgresClient.join methods. It also deprecates all
  PostgresClient.get methods taking where-clause strings. Module users
  should use Criterion or CQLWrapper instead to construct queries:

    * Remove Criterion.selects2String, Criterion.from2String
    * Remove Criteria.setJoinON, Criteria.isJoinON
    * CQLWrapper.toString no longer returns leading blank/space

* [RMB-514](https://issues.folio.org/browse/RMB-514) Update aspectj-maven-plugin to version 11 and
  its aspectjrt and aspectjtools versions to 1.9.4 in pom.xml ([example](https://github.com/folio-org/raml-module-builder/blob/a77786a4b118a523b753a26d2f10aa51e276a4da/sample2/pom.xml#L180-L221))
  to avoid "bad version" warnings.

## Version 28

* [RMB-485](https://issues.folio.org/browse/RMB-485) changed the postgresql driver. Namespace prefix change
   `com.github.mauricio.async.db.postgresql` to
   `com.github.jasync.sql.db.postgresql`

    * `org.folio.rest.persist.PgExceptionFacade.getFields` returns
      `Map<Character.String>` rather than `Map<Object,String>`

    * `org.folio.rest.persist.PgExceptionUtil.getBadRequestFields` returns
      `Map<Character,String>` rather than `Map<Object,String>`

* [RMB-462](https://issues.folio.org/browse/RMB-462) removed support for `fullText defaultDictionary` property (`simple`, `english`, `german`, etc.) in `schema.json`

## Version 27.1

* Remove each foreign key field index and each primary key field `id` index and uniqueIndex, by setting `"tOps": "DELETE"` in schema.json. These btree indexes are created automatically.
* Each fullTextIndex that was created with a dictionary different than 'simple' (for example using `to_tsvector('english', jsonb->>'foo')`)
  needs to be dropped. Then RMB will recreate the index with 'simple'.
  [Example](https://github.com/folio-org/mod-circulation-storage/blob/a8cbed7d32861ec92295a67f93335780e4034e7b/src/main/resources/templates/db_scripts/schema.json):
```
  "scripts": [
    {
      "run": "before",
      "fromModuleVersion": "10.0.1",
      "snippet": "DROP INDEX IF EXISTS loan_userid_idx_ft;"
    }
  ]
```
* Breaking change due to [upgrading to Vert.x 3.8.1](https://github.com/vert-x3/wiki/wiki/3.8.0-Deprecations-and-breaking-changes#blocking-tasks)
    * old: `vertx.executeBlocking(future  -> …, result -> …);`
    * new: `vertx.executeBlocking(promise -> …, result -> …);`
    * old: `vertx.executeBlocking((Future <String> future ) -> …, result -> …);`
    * new: `vertx.executeBlocking((Promise<String> promise) -> …, result -> …);`
* Deprecation changes due to [upgrading to Vert.x 3.8.1](https://github.com/vert-x3/wiki/wiki/3.8.0-Deprecations-and-breaking-changes#future-creation-and-completion)
    * old: `Future <String> fut     = Future.future();   … return fut;`
    * new: `Promise<String> promise = Promise.promise(); … return promise.future();`
    * old: AbstractVerticle `start(Future <Void> fut)`
    * new: AbstractVerticle `start(Promise<Void> fut)`
    * old: `future.compose(res -> …, anotherFuture);`
    * new: `future.compose(res -> { …; return anotherFuture; });`
* Deprecation changes due to [upgrading to Vert.x 3.8.1 > 3.6.2](https://github.com/vert-x3/wiki/wiki/3.6.2-Deprecations)
    * old: `HttpClient` request/response methods with `Handler<HttpClientResponse>`
    * new: `WebClient` request/response methods with `Handler<AsyncResult<HttpClientResponse>>`

## Version 27

* Update the raml-util subdirectory to the latest raml1.0 commit of https://github.com/folio-org/raml to make metadata.createdByUserId optional:
    * Hint: `cd ramls/raml-util; git fetch; git checkout 69f6074; cd ../..; git add ramls/raml-util`
    * Note that pom.xml may revert any submodule update that hasn't been staged or committed!
      (See [notes](https://dev.folio.org/guides/developer-setup/#update-git-submodules).)

## Version 26

* The audit (history) table changed to a new incompatible schema. New audit configuration options are required in schema.json. See [README.md](../README.md#the-post-tenant-api) for details.
* PostgresClient constructor now, by default, starts Embedded Postgres.
  This means that if you have unit tests that do not start Embedded Postgres
  explicitly, you might need to terminate Embedded Postgres by calling
  `PostgresClient.stopEmbeddedPostgres`

## Version 25

* Remove any `"pkColumnName"`, `"generateId"` and `"populateJsonWithId"` entries in `src/main/resources/templates/db_scripts/schema.json`.
    * Hint: `grep -v -e '"pkColumnName"' -e '"generateId"' -e '"populateJsonWithId"' < schema.json > schema.json.new`, review, `mv schema.json.new schema.json`
* In Java files change cql2pgjson import statements:
    * old: `import org.z3950.zing.cql.cql2pgjson.CQL2PgJSON;`
    * old: `import org.z3950.zing.cql.cql2pgjson.FieldException;`
    * new: `import org.folio.cql2pgjson.CQL2PgJSON;`
    * new: `import org.folio.cql2pgjson.exception.FieldException;`
* In Java files replace any `Criteria.setValue`.
    * Consider using some org.folio.rest.persist.PgUtil method or some PostgresClient
      method that takes a CQL query or an id because those methods have
      optimizations that Criteria doesn't have.
    * If you still want to use Criteria please note that setVal does both quoting
      and masking.
    * old: `setValue(s)`  // prone to SQL injection
    * old: `setValue("'" + s + "'")`  // prone to SQL injection
    * old: `setValue("'" + s.replace("'", "''") + "'")`
    * new: `setVal(s)`
* In SQL code replace `_id` (or whatever you used as pkColumnName) by `id`.
* In SQL code remove `gen_random_uuid()` and generate the UUID before sending the query to the database, for example with Java's `UUID.randomUUID()`. This is needed because `gen_random_uuid()` produces different values on different nodes of a replicated database like Pgpool-II. Therefore we no longer support the extension `pgcrypto`.

## Version 20

RMB v20+ is based on RAML 1.0. This is a breaking change from RAML 0.8 and there are multiple changes that must be implemented by modules that upgrade to this version.

```
1. Update the "raml-util" git submodule to use its "raml1.0" branch.
2. MUST change 0.8 to 1.0 in all RAML files (first line)
3. MUST remove the '-' signs from the RAML
   For example:
        CHANGE:  - configs: !include... TO: configs: !include...
4. MUST change the "schemas:" section to "types:"
5. MUST change 'repeat: true' attributes in traits (see our facets) TO type: string[]
6. MUST ensure that documentation field is this format:
   documentation:
     - title: Foo
       content: Bar
7. In resource types change 'schema:' to 'type:'
   This also means that the '- schema:' in the raml is replaced with 'type:'
   For example:
          body:
            application/json:
              type: <<schema>>
8. Remove suffixes from key names in the RAML file.
   Any suffix causes a problem (even `.json`) when it is used to populate
   placeholders in the RAML file.
   Declare only types/schemas in RAML that are used in RAML (no need to declare types
   that are only used in JSON schema references).
   For example:
        CHANGE:
            notify.json: !include notify.json
        TO:
            notify: !include notify.json
        WHEN:
            "notify" is referenced anywhere in the raml
9. JSON schema references may use relative pathname (RMB will dereference them).
   No need to declare them in the RAML file.

10. The resource type examples must not be strict (will result in invalid json content otherwise)
        CHANGE:
            example: <<exampleItem>>
        TO:
            example:
                strict: false
                value: <<exampleItem>>
11. Generated interfaces do not have the 'Resource' suffix
    For example:
        ConfigurationsResource -> Configurations
12. Names of generated pojos (also referenced by the generated interfaces) may change
    For example:
        kv_configuration: !include ../_schemas/kv_configuration.schema
        will produce a pojo called: KvConfiguration

    Referencing the kv_configuration in a schema (example below will produce a pojo called Config)
    which means the same pojo will be created twice with different names.
    Therefore, it is preferable to synchronize names.
            "configs": {
              "id": "configurationData",
              "type": "array",
              "items": {
                "type": "object",
                "$ref": "kv_configuration"
            }
    This may affect which pojo is referenced by the interface - best to use the same name.
13. Generated methods do not throw exceptions anymore.
    This will require removing the 'throws Exception' from the implementing methods.
14. Names of generated methods has changed
15. The response codes have changed:
        withJsonOK -> respond200WithApplicationJson
        withNoContent -> respond204
        withPlainBadRequest -> respond400WithTextPlain
        withPlainNotFound -> respond404WithTextPlain
        withPlainInternalServerError -> respond500WithTextPlain
        withPlainUnauthorized -> respond401WithTextPlain
        withJsonUnprocessableEntity -> respond422WithApplicationJson
        withAnyOK -> respond200WithAnyAny
        withPlainOK -> respond200WithTextPlain
        withJsonCreated -> respond201WithApplicationJson

    Since RMB v23.3.0 PgUtil provides the methods deleteById, getById, post and put that
    automatically create the correct response.

    Note: For 201 / created codes, the location header has changed and is no longer a string
    but an object and should be passed in as:
      PostConfigurationsEntriesResponse.headersFor201().withLocation(LOCATION_PREFIX + ret)
16. Multipart formdata is currently not supported
17. Remove the declaration of trait "secured" auth.raml and its use from RAML files.
    It has been removed from the shared raml-util.
```
