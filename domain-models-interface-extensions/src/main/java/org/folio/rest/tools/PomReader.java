package org.folio.rest.tools;

import java.io.File;
import java.io.FileReader;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.folio.rest.tools.utils.ClassPath;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;


/**
 * @author shale
 *
 */
public enum PomReader {

  INSTANCE;

  private String moduleName = null;
  private String version = null;
  private Properties props = null;
  private List<Dependency> dependencies = null;
  private String rmbVersion = null;

  private final Logger log = LoggerFactory.getLogger(PomReader.class);

  @SuppressWarnings("checkstyle:methodlength")
  private PomReader() {
    try {
      System.out.print("Attempting to read in the module name from....");
      MavenXpp3Reader mavenreader = new MavenXpp3Reader();
      Model model = null;
      String currentRunningJar =
          PomReader.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
      System.out.println(currentRunningJar);
      if(currentRunningJar != null && (currentRunningJar.contains("domain-models-runtime")
          || currentRunningJar.contains("domain-models-interface-extensions"))){
        //the runtime is the jar run when deploying during unit tests
        //the interface-extensions is the jar run when running build time tools,
        //like MDGenerator, ClientGenerator, etc..
        //this is build time - not runtime, so just use the pom
        File pomFile = new File("pom.xml");
        model = mavenreader.read(new FileReader(pomFile));
      }
      else{
        //this is runtime, the jar called via java -jar is the module's jar
        ClassPath classPath = ClassPath.from(Thread.currentThread().getContextClassLoader());
        Set<ClassPath.ResourceInfo> resources = classPath.getResources();
        for (ClassPath.ResourceInfo info : resources) {
          if(info.getResourceName().endsWith("pom.xml")){
            //maven sets the classpath order according to the pom, so the poms project will be the first entry
            model = mavenreader.read(PomReader.class.getResourceAsStream("/"+info.getResourceName()));
            break;
          }
        }
      }
      if(model.getParent() != null){
        moduleName = model.getParent().getArtifactId();
        version = model.getParent().getVersion();
      }
      else{
        moduleName = model.getArtifactId();
      }

      if(version == null){
        version = model.getVersion();
        if(version == null){
          version = "1.0.0";
        }
      }
      version = version.replaceAll("-.*", "");

      moduleName = moduleName.replaceAll("-", "_");
      props = model.getProperties();
      dependencies = model.getDependencies();

      //the version is a placeholder to a value in the props section
      version = replacePlaceHolderWithValue(version);

      for (int i = 0; i < dependencies.size(); i++) {
        if("domain-models-runtime".equals(dependencies.get(i).getArtifactId())){
          rmbVersion = dependencies.get(i).getVersion();
          rmbVersion = replacePlaceHolderWithValue(rmbVersion);
          rmbVersion = rmbVersion.replaceAll("-.*", "");
        }
      }

      if(rmbVersion == null){
        //if we are in the rmb jar - build time
        rmbVersion = version;
      }

      //props.list(System.out);
      log.info("module name: " + moduleName + ", version: " + version);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  private String replacePlaceHolderWithValue(String placeholder){
    String ret[] = new String[]{placeholder};
    if(placeholder != null && placeholder.startsWith("${")){
      props.forEach( (k,v) -> {
        if(("${"+k+"}").equals(placeholder)){
          ret[0] = (String)v;
        }
      });
    }
    return ret[0];
  }

  public String getVersion() {
    return version;
  }

  public String getModuleName() {
    return moduleName;
  }

  public Properties getProps() {
    return props;
  }

  public List<Dependency> getDependencies() {
    return dependencies;
  }

  public String getRmbVersion() {
    return rmbVersion;
  }

}
