package org.folio.rest.tools;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.io.FileUtils;

import com.sun.codemodel.JBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JConditional;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JDocComment;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JForLoop;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JPackage;
import com.sun.codemodel.JVar;
import com.sun.codemodel.JWhileLoop;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 *
 */
public class ClientGenerator {

  public static final String  PATH_ANNOTATION        = "javax.ws.rs.Path";
  public static final String  CLIENT_CLASS_SUFFIX    = "Client";
  public static final String  PATH_TO_GENERATE_TO    = "/target/generated-sources/raml-jaxrs/";
  public static final String  OKAPI_HEADER_TENANT = "x-okapi-tenant";

  private static final Logger log = LoggerFactory.getLogger(ClientGenerator.class);

  /* Creating java code model classes */
  JCodeModel jCodeModel = new JCodeModel();

  /* for creating the class per interface */
  JDefinedClass jc = null;

  private String globalPath = null;

  private List<String> functionSpecificHeaderParams = new ArrayList<>();

  private String className = null;

  private String mappingType = "postgres";

  private JFieldVar tenantId;
  private JFieldVar token;

  private JFieldVar httpClient;

  public static void main(String[] args) throws Exception {

    String dir = System.getProperties().getProperty("project.basedir")
      + ClientGenerator.PATH_TO_GENERATE_TO
      + RTFConsts.CLIENT_GEN_PACKAGE.replace('.', '/');

    makeCleanDir(dir);

    AnnotationGrabber.generateMappings();

  }

  /**
   * Add a comment to the body of the method saying that it is auto-generated and how.
   * @param method  where to add the comment
   */
  private void addCommentAutogenerated(JMethod method) {
    method.body().directStatement("// Auto-generated code");
    method.body().directStatement("// - generated by       " + getClass().getCanonicalName());
    method.body().directStatement("// - generated based on " + RTFConsts.INTERFACE_PACKAGE + "." + className + "Resource");
  }

  /**
   * Create a constructor and add a comment to the body of the constructor saying that it
   * is auto-generated and how.
   * @param mods  Modifiers for this constructor
   * @return the new constructor
   */
  private JMethod constructor(int mods) {
    JMethod constructor = jc.constructor(mods);
    addCommentAutogenerated(constructor);
    return constructor;
  }

  /**
   * Create a method and add a comment to the body of the method saying that it
   * is auto-generated and how.
   * @param mods  Modifiers for this method
   * @param type  Return type for this method
   * @param name  name for this method
   * @return the new method
   */
  private JMethod method(int mods, Class <?> type, String name) {
    JMethod method = jc.method(mods, type, name);
    addCommentAutogenerated(method);
    return method;
  }

  public void generateClassMeta(String className, Object globalPath){

    String mapType = System.getProperty("json.type");
    if(mapType != null){
      if(mapType.equals("mongo")){
        mappingType = "mongo";
      }
    }
    this.globalPath = "GLOBAL_PATH";

    /* Adding packages here */
    JPackage jp = jCodeModel._package(RTFConsts.CLIENT_GEN_PACKAGE);

    try {
      /* Giving Class Name to Generate */
      this.className = className.substring(RTFConsts.INTERFACE_PACKAGE.length()+1);
      jc = jp._class(this.className+CLIENT_CLASS_SUFFIX);
      JDocComment com = jc.javadoc();
      com.add("Auto-generated code - based on class " + className);

      /* class variable to root url path to this interface */
      JFieldVar globalPathVar = jc.field(JMod.PRIVATE | JMod.STATIC | JMod.FINAL, String.class, "GLOBAL_PATH");
      globalPathVar.init(JExpr.lit((String)globalPath));

      /* class variable tenant id */
      tenantId = jc.field(JMod.PRIVATE, String.class, "tenantId");

      token = jc.field(JMod.PRIVATE, String.class, "token");

      /* class variable to http options */
      JFieldVar options = jc.field(JMod.PRIVATE, HttpClientOptions.class, "options");

      /* class variable to http client */
      httpClient = jc.field(JMod.PRIVATE, HttpClient.class, "httpClient");

      /* constructor, init the httpClient - allow to pass keep alive option */
      JMethod consructor = constructor(JMod.PUBLIC);
      JVar host = consructor.param(String.class, "host");
      JVar port = consructor.param(int.class, "port");
      JVar param = consructor.param(String.class, "tenantId");
      JVar token = consructor.param(String.class, "token");
      JVar keepAlive = consructor.param(boolean.class, "keepAlive");
      JVar connTO = consructor.param(int.class, "connTO");
      JVar idleTO = consructor.param(int.class, "idleTO");

     /* populate constructor */
      JBlock conBody=  consructor.body();
      conBody.assign(JExpr._this().ref(tenantId), param);
      conBody.assign(JExpr._this().ref(token), token);
      conBody.assign(options, JExpr._new(jCodeModel.ref(HttpClientOptions.class)));
      conBody.invoke(options, "setLogActivity").arg(JExpr.TRUE);
      conBody.invoke(options, "setKeepAlive").arg(keepAlive);
      conBody.invoke(options, "setDefaultHost").arg(host);
      conBody.invoke(options, "setDefaultPort").arg(port);
      conBody.invoke(options, "setConnectTimeout").arg(connTO);
      conBody.invoke(options, "setIdleTimeout").arg(idleTO);

      JExpression vertx = jCodeModel.ref("org.folio.rest.tools.utils.VertxUtils").staticInvoke("getVertxFromContextOrNew");
      conBody.assign(httpClient, vertx.invoke("createHttpClient").arg(options));

      /* constructor, init the httpClient */
      JMethod consructor2 = constructor(JMod.PUBLIC);
      JVar hostVar = consructor2.param(String.class, "host");
      JVar portVar = consructor2.param(int.class, "port");
      JVar tenantIdVar = consructor2.param(String.class, "tenantId");
      JVar tokenVar = consructor2.param(String.class, "token");
      JBlock conBody2 = consructor2.body();
      conBody2.invoke("this").arg(hostVar).arg(portVar).arg(tenantIdVar).arg(tokenVar).arg(JExpr.TRUE)
        .arg(JExpr.lit(2000)).arg(JExpr.lit(5000));

      JMethod consructor1 = constructor(JMod.PUBLIC);
      JVar hostVar1 = consructor1.param(String.class, "host");
      JVar portVar1 = consructor1.param(int.class, "port");
      JVar tenantIdVar1 = consructor1.param(String.class, "tenantId");
      JVar tokenVar1 = consructor1.param(String.class, "token");
      JVar keepAlive1 = consructor1.param(boolean.class, "keepAlive");
      JBlock conBody1 = consructor1.body();
      conBody1.invoke("this").arg(hostVar1).arg(portVar1).arg(tenantIdVar1).arg(tokenVar1).arg(keepAlive1)
      .arg(JExpr.lit(2000)).arg(JExpr.lit(5000));

      /* constructor, init the httpClient */
      JMethod consructor3 = constructor(JMod.PUBLIC);
      JBlock conBody3 = consructor3.body();

      conBody3.invoke("this").arg("localhost").arg(JExpr.lit(8081)).arg("folio_demo").arg("folio_demo").arg(JExpr.FALSE)
        .arg(JExpr.lit(2000)).arg(JExpr.lit(5000));
      consructor3.javadoc().add("Convenience constructor for tests ONLY!<br>Connect to localhost on 8081 as folio_demo tenant.");

    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }

  }

  public void generateCloseClient(){
    JMethod jmCreate = method(JMod.PUBLIC, void.class, "close");
    jmCreate.javadoc().add("Close the client. Closing will close down any "
        + "pooled connections. Clients should always be closed after use.");
    JBlock body = jmCreate.body();

    body.directStatement("httpClient.close();");
  }

  public static void makeCleanDir(String dirPath) throws IOException {
    File dir = new File(dirPath);
    if (dir.exists()) {
      FileUtils.cleanDirectory(dir);
    } else {
      dir.mkdirs();
    }
  }

  public void generateMethodMeta(String methodName, JsonObject params, String url,
      String httpVerb, JsonArray contentType, JsonArray accepts){

    /* Adding method to the Class which is public and returns void */

    JMethod jmCreate = method(JMod.PUBLIC, void.class, methodName);
    JBlock body = jmCreate.body();

    /* create the query parameter string builder */
    JVar queryParams = body.decl(jCodeModel._ref(StringBuilder.class), "queryParams",
            JExpr._new(jCodeModel.ref(StringBuilder.class)).arg("?"));


    ////////////////////////---- Handle place holders in the url  ----//////////////////
    /* create request */
    if(url == null){
      //if there is no path associated with a function
      //use the @path from the class
      url = globalPath;
    }
    else{
      /* Handle place holders in the URL
       * replace {varName} with "+varName+" so that it will be replaced
       * in the url at runtime with the correct values */
      Matcher m = Pattern.compile("\\{.*?\\}").matcher(url);
      while(m.find()){
        String varName = m.group().replace("{","").replace("}", "");
        url = url.replace("{"+varName+"}", "\"+"+varName+"+\"");
      }

      url = "\""+url.substring(1)+"\"+queryParams.toString()";
    }

    /* Adding java doc for method */
    jmCreate.javadoc().add("Service endpoint " + url);


    /* iterate on function params and add the relevant ones
     * --> functionSpecificQueryParamsPrimitives is populated by query parameters that are primitives
     * --> functionSpecificHeaderParams (used later on) is populated by header params
     * --> functionSpecificQueryParamsEnums is populated by query parameters that are enums */
    Iterator<Entry<String, Object>> paramList = params.iterator();

    boolean bodyContentExists[] = new boolean[]{false};
    paramList.forEachRemaining(entry -> {
      String valueName = ((JsonObject) entry.getValue()).getString("value");
      String valueType = ((JsonObject) entry.getValue()).getString("type");
      String paramType = ((JsonObject) entry.getValue()).getString("param_type");
      if(handleParams(jmCreate, queryParams, paramType, valueType, valueName)){
        bodyContentExists[0] = true;
      }
    });

    //////////////////////////////////////////////////////////////////////////////////////

    /* create the http client request object */
    body.directStatement("io.vertx.core.http.HttpClientRequest request = httpClient."+
        httpVerb.substring(httpVerb.lastIndexOf(".")+1).toLowerCase()+"("+url+");");
    body.directStatement("request.handler(responseHandler);");

    /* add headers to request */
    functionSpecificHeaderParams.forEach( val -> {
      body.directStatement(val);
    });
    //reset for next method usage
    functionSpecificHeaderParams = new ArrayList<>();

    /* add content and accept headers if relevant */
    if(contentType != null){
      String cType = contentType.toString().replace("\"", "").replace("[", "").replace("]", "");
      if(contentType.contains("multipart/form-data")){
        body.directStatement("request.putHeader(\"Content-type\", \""+cType+"; boundary=--BOUNDARY\");");
      }
      else{
        body.directStatement("request.putHeader(\"Content-type\", \""+cType+"\");");
      }
    }
    if(accepts != null){
      String aType = accepts.toString().replace("\"", "").replace("[", "").replace("]", "");
      //replace any/any with */* to allow declaring accpet */* which causes compilation issues
      //when declared in raml. so declare any/any in raml instead and replaced here
      aType = aType.replaceAll("any/any", "");
      body.directStatement("request.putHeader(\"Accept\", \""+aType+"\");");
    }

    /* push tenant id into x-okapi-tenant and authorization headers for now */
    JConditional _if = body._if(tenantId.ne(JExpr._null()));
    _if._then().directStatement("request.putHeader(\"X-Okapi-Token\", token);");
    _if._then().directStatement("request.putHeader(\""+OKAPI_HEADER_TENANT+"\", tenantId);");
    /* add response handler to each function */
    JClass handler = jCodeModel.ref(Handler.class).narrow(HttpClientResponse.class);
    jmCreate.param(handler, "responseHandler");

    /* if we need to pass data in the body */
    if(bodyContentExists[0]){
      body.directStatement("request.putHeader(\"Content-Length\", buffer.length()+\"\");");
      body.directStatement("request.setChunked(true);");
      body.directStatement("request.write(buffer);");
    }

    body.directStatement("request.end();");

  }

  private void addParameter(JBlock methodBody, JVar queryParams, String valueName, Boolean encode, Boolean simple, boolean isList) {
    JBlock b = methodBody;
    if (!simple) {
      JConditional _if = methodBody._if(JExpr.ref(valueName).ne(JExpr._null()));
      b = _if._then();
    }
    b.invoke(queryParams, "append").arg(JExpr.lit(valueName + "="));
    if (encode) {
        JExpression expr = jCodeModel.ref(java.net.URLEncoder.class).staticInvoke("encode").arg(JExpr.ref(valueName)).arg("UTF-8");
        b.invoke(queryParams, "append").arg(expr);
    } else {
      if(isList){
        b.directStatement("if("+valueName+".getClass().isArray())"
            +"{queryParams.append(String.join(\"&"+valueName+"=\"," +valueName+"));}");
      } else{
        b.invoke(queryParams, "append").arg(JExpr.ref(valueName));
      }
    }
    b.invoke(queryParams, "append").arg(JExpr.lit("&"));
  }

  /**
   * @param paramType
   * @param valueType
   */
  private boolean handleParams(JMethod method, JVar queryParams, String paramType, String valueType, String valueName) {

    JBlock methodBody = method.body();

    if (AnnotationGrabber.NON_ANNOTATED_PARAM.equals(paramType) /*&& !FILE_UPLOAD_PARAM.equals(valueType)*/) {
      try {
        // this will also validate the json against the pojo created from the schema
        Class<?> entityClazz = Class.forName(valueType);

        if (!valueType.equals("io.vertx.core.Handler") && !valueType.equals("io.vertx.core.Context") &&
            !valueType.equals("java.util.Map") && !valueType.equals("io.vertx.ext.web.RoutingContext")) {

          /* this is a post or put since our only options here are receiving a reader (data in body) or
           * entity - which is also data in body - but we can only have one since a multi part body
           * should be indicated by a multipart objector input stream in the body */
          JExpression jexpr = jCodeModel.ref(io.vertx.core.buffer.Buffer.class).staticInvoke("buffer");
          JVar buffer = methodBody.decl(jCodeModel.ref(io.vertx.core.buffer.Buffer.class), "buffer", jexpr);
          // methodBody.directStatement( "io.vertx.core.buffer.Buffer buffer = io.vertx.core.buffer.Buffer.buffer();" );

          if ("java.io.Reader".equals(valueType)){
            JVar reader = method.param(Reader.class, "reader");
            method._throws(Exception.class);
            JConditional _if = methodBody._if(reader.ne(JExpr._null()));
           _if._then().directStatement( "buffer.appendString(org.apache.commons.io.IOUtils.toString(reader));" );
          }
          else if("java.io.InputStream".equals(valueType)){
            JVar inputStream = method.param(InputStream.class, "inputStream");
            JVar result = methodBody.decl(jCodeModel.ref(ByteArrayOutputStream.class), "result", JExpr._new(jCodeModel.ref(ByteArrayOutputStream.class)));
            JVar byteA = methodBody.decl(jCodeModel.BYTE.array(), "buffer1", JExpr.newArray(jCodeModel.BYTE, 1024));
            JVar length = methodBody.decl(jCodeModel.INT, "length");
            // http://stackoverflow.com/questions/26037015/how-do-i-force-enclose-a-codemodel-expression-in-brackets
            JWhileLoop _while = methodBody._while(JExpr.TRUE);
            _while.body().assign(length, inputStream.invoke("read").arg(byteA));
            _while.body()._if(length.eq(JExpr.lit(-1)))._then()._break();
            _while.body().add(result.invoke("write").arg(byteA).arg(JExpr.lit(0)).arg(length));
            methodBody.add(buffer.invoke("appendBytes").arg(result.invoke("toByteArray")));
            method._throws(IOException.class);
          }
          else if("javax.mail.internet.MimeMultipart".equals(valueType)){
            JVar mimeMultiPart = method.param(MimeMultipart.class, "mimeMultipart");
            method._throws(MessagingException.class);
            method._throws(IOException.class);
            JBlock b1 = methodBody._if(mimeMultiPart.ne(JExpr._null()))._then();
            JVar parts = b1.decl(jCodeModel.INT, "parts", mimeMultiPart.invoke("getCount"));
            JVar sb = b1.decl(jCodeModel._ref(StringBuilder.class), "sb",
                    JExpr._new(jCodeModel.ref(StringBuilder.class)));
            JForLoop _for = b1._for();
            JVar i_var = _for.init(jCodeModel._ref(int.class), "i", JExpr.lit(0));
            _for.test(i_var.lt(parts));
            _for.update(i_var.incr());
            JBlock fBody = _for.body();
            JVar bp = fBody.decl(jCodeModel.ref(javax.mail.BodyPart.class), "bp", mimeMultiPart.invoke("getBodyPart").arg(i_var));
            fBody.add(sb.invoke("append").arg("----BOUNDARY\r\n"));
            fBody.add(sb.invoke("append").arg("Content-Disposition: \""));
            fBody.add(sb.invoke("append").arg(bp.invoke("getDisposition")));
            fBody.add(sb.invoke("append").arg("\"; name=\""));
            fBody.add(sb.invoke("append").arg(bp.invoke("getFileName")));
            fBody.add(sb.invoke("append").arg("\"; filename=\")"));
            fBody.add(sb.invoke("append").arg(bp.invoke("getFileName")));
            fBody.add(sb.invoke("append").arg("\"\r\n"));
            fBody.add(sb.invoke("append").arg("Content-Type: application/octet-stream\r\n"));
            fBody.add(sb.invoke("append").arg("Content-Transfer-Encoding: binary\r\n"));
            b1.add(sb.invoke("append").arg("----BOUNDARY\r\n"));
            b1.add(buffer.invoke("appendString").arg(sb.invoke("toString")));
          }
          else{
            String objParamName = entityClazz.getSimpleName();
            JConditional _if = methodBody._if(JExpr.ref(objParamName).ne(JExpr._null()));
            JBlock b = methodBody;
            b = _if._then();
            if(mappingType.equals("postgres")){
              method._throws(Exception.class);
              b.directStatement("buffer.appendString("
                  + "org.folio.rest.tools.ClientHelpers.pojo2json("+objParamName+"));");
            }else{
              b.directStatement( "buffer.appendString("
                  + "org.folio.rest.tools.utils.JsonUtils.entity2Json("+objParamName+").encode());");
            }
            method.param(entityClazz, entityClazz.getSimpleName());
          }
          return true;
        }
      } catch (Exception e) {
        log.error(e.getMessage(), e);
      }
    }
    else if (AnnotationGrabber.PATH_PARAM.equals(paramType)) {
      method.param(String.class, valueName);

    }
    else if (AnnotationGrabber.HEADER_PARAM.equals(paramType)) {
      method.param(String.class, valueName);
      functionSpecificHeaderParams.add("request.putHeader(\""+valueName+"\", "+valueName+");");
    }
    else if (AnnotationGrabber.QUERY_PARAM.equals(paramType)) {
      // support enum, numbers or strings as query parameters
      boolean encode = false;
      try {
        if (valueType.contains("String")) {
          method.param(String.class, valueName);
          encode = true;
          addParameter(methodBody, queryParams, valueName, encode, false, false);
        } else if (valueType.contains("int")) {
          method.param(int.class, valueName);
          addParameter(methodBody, queryParams, valueName, encode, true, false);
        } else if (valueType.contains("boolean")) {
          method.param(boolean.class, valueName);
          addParameter(methodBody, queryParams, valueName, encode, true, false);
        } else if (valueType.contains("BigDecimal")) {
          method.param(BigDecimal.class, valueName);
          addParameter(methodBody, queryParams, valueName, encode, false, false);
        } else if (valueType.contains("Number")) {
          method.param(Number.class, valueName);
          addParameter(methodBody, queryParams, valueName, encode, false, false);
        }
        else if (valueType.contains("Integer")) {
            method.param(Integer.class, valueName);
            addParameter(methodBody, queryParams, valueName, encode, false, false);
        } else if (valueType.contains("Boolean")) {
            method.param(Boolean.class, valueName);
            addParameter(methodBody, queryParams, valueName, encode, false, false);
        }else if (valueType.contains("List")) {
          method.param(String[].class, valueName);
          addParameter(methodBody, queryParams, valueName, encode, false, true);
        }else { // enum object type
          try {
            Class<?> enumClazz1 = Class.forName(valueType);
            if (enumClazz1.isEnum()) {
              method.param(enumClazz1, valueName);
              addParameter(methodBody, queryParams, valueName, encode, false, false);
            }
          } catch (Exception ee) {
            log.error(ee.getMessage(), ee);

          }
        }
        if(encode){
          method._throws(UnsupportedEncodingException.class);
        }
      } catch (Exception e) {
        log.error(e.getMessage(), e);
      }
    }
    return false;
  }

  public void generateClass(JsonObject classSpecificMapping) throws IOException{
    String genPath = System.getProperty("project.basedir") + PATH_TO_GENERATE_TO;
    jCodeModel.build(new File(genPath));
  }

}
