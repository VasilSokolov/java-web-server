package com.cyecize.broccolina.services;

import com.cyecize.solet.BaseHttpSolet;
import com.cyecize.solet.HttpSolet;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.stream.Collectors;

public class ApplicationScanningServiceImpl implements ApplicationScanningService {

    private static final String APPLICATION_LIB_FOLDER_NAME = "lib";

    private static final String APPLICATION_CLASSES_FOLDER_NAME = "classes";

    private final String applicationsFolderPath;

    private final JarFileUnzipService jarFileUnzipService;

    private List<String> applicationNames;

    private Map<String, List<Class<HttpSolet>>> soletClasses;

    public ApplicationScanningServiceImpl(String applicationsFolderPath, JarFileUnzipService jarFileUnzipService) {
        this.applicationsFolderPath = applicationsFolderPath;
        this.jarFileUnzipService = jarFileUnzipService;
        this.applicationNames = new ArrayList<>();
        this.soletClasses = new HashMap<>();
    }

    @Override
    public List<String> getApplicationNames() {
        return this.applicationNames;
    }

    /**
     * Starts scanning apps from javache's webapps folder.
     * Iterates over every jar file in the folder and for each jar file,
     * calls the jarUnzipService to extract the file.
     * Extracts the app name, and starts an application scan.
     * <p>
     * Returns map of application name and a list of solet classes.
     */
    @Override
    public Map<String, List<Class<HttpSolet>>> findSoletClasses() throws IOException, ClassNotFoundException {
        File applicationsFolder = new File(this.applicationsFolderPath);

        if (applicationsFolder.exists() && applicationsFolder.isDirectory()) {
            List<File> allJarFiles = Arrays.stream(applicationsFolder.listFiles()).filter(this::isJarFile).collect(Collectors.toList());
            for (File applicationJarFile : allJarFiles) {
                this.jarFileUnzipService.unzipJar(applicationJarFile);

                String appName = applicationJarFile.getName().replace(".jar", "");
                this.loadApplicationFromFolder(applicationJarFile.getCanonicalPath().replace(".jar", File.separator), appName);
            }
        }

        return this.soletClasses;
    }

    /**
     * Loads application libraries.
     * Loads application classes.
     * Adds the application name to the applicationNames list.
     */
    private void loadApplicationFromFolder(String applicationRootFolderPath, String applicationName) throws IOException, ClassNotFoundException {
        String classesRootFolderPath = applicationRootFolderPath + APPLICATION_CLASSES_FOLDER_NAME + File.separator;
        String librariesRootFolderPath = applicationRootFolderPath + APPLICATION_LIB_FOLDER_NAME + File.separator;

        this.loadApplicationLibraries(librariesRootFolderPath);
        this.loadApplicationClasses(classesRootFolderPath, applicationName);
        this.applicationNames.add("/" + applicationName);
    }

    /**
     * If the directory does not exist, return.
     * Adds the directory to the classpath.
     * Starts a recursion for loading classes and finding solets.
     */
    private void loadApplicationClasses(String classesRootFolderPath, String currentApplicationName) throws IOException, ClassNotFoundException {
        File classesRootDirectory = new File(classesRootFolderPath);
        if (!classesRootDirectory.exists() || !classesRootDirectory.isDirectory()) {
            return;
        }
        this.addDirectoryToClassPath(classesRootDirectory.getCanonicalPath() + File.separator);
        this.loadClass(classesRootDirectory, "", currentApplicationName);
    }

    /**
     * Recursive method for loading classes, starts with empty packageName.
     * If the file is directory, iterate all files inside and call loadClass with the current file name
     * appended to the packageName.
     * <p>
     * If the file is file and the file name ends with .class, load it and check if the class
     * is assignable from BaseHttpSolet. If it is, add it to the map of solet classes.
     */
    private void loadClass(File currentFile, String packageName, String applicationName) throws ClassNotFoundException {
        if (currentFile.isDirectory()) {
            for (File childFile : currentFile.listFiles()) {
                this.loadClass(childFile, (packageName + currentFile.getName() + "."), applicationName);
            }
        } else {
            if (!currentFile.getName().endsWith(".class")) {
                return;
            }

            String className = (packageName.replace(APPLICATION_CLASSES_FOLDER_NAME + ".", "")) + currentFile
                    .getName()
                    .replace(".class", "")
                    .replace("/", ".");

            Class currentClassFile = Class.forName(className, true, Thread.currentThread().getContextClassLoader());

            if (BaseHttpSolet.class.isAssignableFrom(currentClassFile)) {
                if (!this.soletClasses.containsKey(applicationName)) {
                    this.soletClasses.put(applicationName, new ArrayList<>());
                }

                this.soletClasses.get(applicationName).add(currentClassFile);
            }
        }
    }

    /**
     * Iterates the given directory's files and filters jar files
     * then adds them to the system classpath.
     */
    private void loadApplicationLibraries(String librariesRootFolderPath) {

        File libraryFolder = new File(librariesRootFolderPath);
        if (!libraryFolder.exists() || !libraryFolder.isDirectory()) {
            return;
        }

        Arrays.stream(libraryFolder.listFiles()).filter(this::isJarFile)
                .forEach(jf -> {
                    try {
                        this.addJarFileToClassPath(jf.getCanonicalPath());
                    } catch (IOException ignored) {
                    }
                });
    }

    /**
     * Creates a proper URL for directory and adds it to the system classloader.
     */
    private void addDirectoryToClassPath(String canonicalPath) {
        try {
            this.addUrlToClassPath(new URL("file:/" + canonicalPath));
        } catch (MalformedURLException ignored) {
        }
    }

    /**
     * Creates a proper URL format for .jar files and adds it to the system classloader.
     */
    private void addJarFileToClassPath(String canonicalPath) {
        try {
            this.addUrlToClassPath(new URL("jar:file:" + canonicalPath + "!/"));
        } catch (MalformedURLException ignored) {
        }
    }

    /**
     * Adds a URL to the current system classloader.
     * This method works by default on Java 8.
     * On newer versions it is required to first replace the system classloader with an instance of
     * URLClassLoader. This is done at the start of Javache.
     */
    private void addUrlToClassPath(URL url) {
        try {
            URLClassLoader sysClassLoaderInstance = (URLClassLoader) ClassLoader.getSystemClassLoader();
            Class<URLClassLoader> sysClassLoaderType = URLClassLoader.class;

            Method method = sysClassLoaderType.getDeclaredMethod("addURL", URL.class);
            method.setAccessible(true);
            method.invoke(sysClassLoaderInstance, url);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * Checks if a file's name ends with .jar
     */
    private boolean isJarFile(File file) {
        return file.isFile() && file.getName().endsWith(".jar");
    }
}
