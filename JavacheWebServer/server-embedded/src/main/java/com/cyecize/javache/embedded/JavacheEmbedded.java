package com.cyecize.javache.embedded;

import com.cyecize.StartUp;
import com.cyecize.javache.ConfigConstants;
import com.cyecize.javache.core.Server;
import com.cyecize.javache.core.ServerImpl;
import com.cyecize.javache.embedded.services.EmbeddedJavacheConfigService;
import com.cyecize.javache.embedded.services.EmbeddedRequestHandlerLoadingService;
import com.cyecize.javache.services.JavacheConfigService;
import com.cyecize.javache.services.LoggingService;
import com.cyecize.javache.services.LoggingServiceImpl;

import java.util.HashMap;
import java.util.Map;

public class JavacheEmbedded {

    public static void startServer(int port, Class<?> mainClass) {
        startServer(port, new HashMap<>(), mainClass);
    }

    /**
     * Replaces system classloader with an instance of URLClassLoader
     * Extracts working directory from the given startup class.
     * Sets runtime config for Embedded server. NOTE that this config is tested on intelliJ.
     * If a problem occurs, you can pass your own properties for file location.
     * <p>
     * The working directory will be printed as the program starts. Check and see if
     * you compile output matches the working directory.
     * If if doesn't consider changing the properties.
     * <p>
     * Creates server instance and runs the server.
     */
    public static void startServer(int port, Map<String, Object> config, Class<?> mainClass) {
        try {
            StartUp.replaceSystemClassLoader();

            String workingDir = mainClass.getProtectionDomain().getCodeSource().getLocation().getFile().substring(1);
            System.out.println(String.format("Working Directory: %s", workingDir));

            final LoggingService loggingService = new LoggingServiceImpl();

            //Since classes is the default output directory.
            config.putIfAbsent(ConfigConstants.MAIN_APP_JAR_NAME, "classes");

            //There is not "classes" folder inside the jar file so we set it to empty.
            config.put(ConfigConstants.APP_COMPILE_OUTPUT_DIR_NAME, "");

            //Because if how Broccolina and Toyote read their request handlers, we want to go one step back.
            config.put(ConfigConstants.WEB_APPS_DIR_NAME, "../");

            JavacheConfigService configService = new EmbeddedJavacheConfigService(config);

            Server server = new ServerImpl(port, loggingService, new EmbeddedRequestHandlerLoadingService(workingDir, configService), configService);
            server.run();

        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }
}
