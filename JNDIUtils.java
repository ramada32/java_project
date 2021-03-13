package br.com.foursys.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

public final class JNDIUtils {
  public static final int GLASSFISH_SERVER_TYPE = 0;
  public static final int WILDFLY_SERVER_TYPE = 1;
  public static final int DEFAULT_SERVER_TYPE = GLASSFISH_SERVER_TYPE;
  
  private JNDIUtils() {
  }

  public static final Properties getServerProperties(int serverType) {
    Properties jndiProperties = new Properties();
    switch (serverType) {
    case GLASSFISH_SERVER_TYPE:
      jndiProperties.setProperty("java.naming.factory.initial", "com.sun.enterprise.naming.SerialInitContextFactory");
      jndiProperties.setProperty("java.naming.factory.url.pkgs", "com.sun.enterprise.naming");
      break;
    case WILDFLY_SERVER_TYPE:
      jndiProperties.put(Context.INITIAL_CONTEXT_FACTORY, "org.jboss.naming.remote.client.InitialContextFactory");
      jndiProperties.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
      jndiProperties.put(Context.PROVIDER_URL, "http-remoting://localhost:8080"); // Check this!!!!
      jndiProperties.put("jboss.naming.client.ejb.context", true);
      break;
    }
    return jndiProperties;
  }

  public static final InitialContext createInitialContext() {
    return createInitialContext(DEFAULT_SERVER_TYPE);
  }

  public static final InitialContext createInitialContext(int serverType) {
    Properties props = getServerProperties(serverType);
    try {
      return new InitialContext(props);
    } catch (NamingException ex) {
      ex.printStackTrace();
    }
    return null;
  }

  public static <T> T lookup(InitialContext ctx, String appName, String moduleName, String beanName) {
    return lookup(ctx, appName, moduleName, beanName, null);
  }
  
  /*
    -- For Wildfly:
    java:global/ejb-sample-1/RegiaoEJB!br.com.foursys.ejb.remote.RegiaoEJBRemote
    java:app/ejb-sample-1/RegiaoEJB!br.com.foursys.ejb.remote.RegiaoEJBRemote
    java:module/RegiaoEJB!br.com.foursys.ejb.remote.RegiaoEJBRemote
    java:jboss/exported/ejb-sample-1/RegiaoEJB!br.com.foursys.ejb.remote.RegiaoEJBRemote
    java:global/ejb-sample-1/RegiaoEJB
    java:app/ejb-sample-1/RegiaoEJB
    java:module/RegiaoEJB
   */
  @SuppressWarnings("unchecked")
  public static <T> T lookup(InitialContext ctx, String appName, String moduleName, String beanName, String viewName) {
    // String name = "java:global/" + moduleName + "/" + beanName; // For Glassfish
    // String name = "ejb:[appName]/moduleName/[distinctName]/beanName!viewClassName"; // For Wildfly (Check this!!!)
    
    /*
    final String appName = "foursys-system-ear-7.1"; // If haven't .ear
    final String moduleName = "br.com.foursys-foursys-system-business-7.1";
    final String distinctName = ""; // Optional
    final String beanName = "RegiaoEJB";
    final String viewClassName = "br.com.foursys.ejb.RegiaoEJBRemote";
    String name = "java:global/foursys-system-ear-7.1/br.com.foursys-foursys-system-web-7.1/RegiaoEJB";
    */
    
    // For Glassfish!!!!
    // Use: 'java:global/foursys-system-ear-7.1/br.com.foursys-foursys-system-web-7.1/RegiaoEJB'
    StringBuilder sb = new StringBuilder();
    sb.append("java:global");
    if(appName != null && !appName.isEmpty()) {
      sb.append("/").append(appName);
    }
    sb.append("/").append(moduleName);
    sb.append("/").append(beanName);
    if(viewName != null && !viewName.isEmpty()) {
      sb.append("!").append(viewName);
    }
    
    try {
      return (T) ctx.lookup(sb.toString());
    } catch (NamingException ex) {
      ex.printStackTrace();
    }
    return null;
  }

  @SuppressWarnings("rawtypes")
  public static Map toMap(Context ctx) {
    try {
      String namespace = ctx instanceof InitialContext ? ctx.getNameInNamespace() : "";
      HashMap<String, Object> map = new HashMap<String, Object>();
      System.out.println("> Listing namespace: " + namespace);
      NamingEnumeration<NameClassPair> list = ctx.list(namespace);
      while (list.hasMoreElements()) {
        NameClassPair next = list.next();
        String name = next.getName();
        String jndiPath = namespace + name;
        Object lookup;
        try {
          System.out.println("> Looking up name: " + jndiPath);
          Object tmp = ctx.lookup(jndiPath);
          if (tmp instanceof Context) {
            lookup = toMap((Context) tmp);
          } else {
            lookup = tmp.toString();
          }
        } catch (Throwable t) {
          lookup = "[ERROR] " + t.getMessage();
        }
        map.put(name, lookup);
      }
      return map;
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return null;
  }

}
