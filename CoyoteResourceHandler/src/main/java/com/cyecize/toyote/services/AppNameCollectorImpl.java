package com.cyecize.toyote.services;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AppNameCollectorImpl implements AppNameCollector {

    private static final String APPLICATIONS_FOLDER_NON_EXISTENT = "Invalid applications folder \"%s\"";

    private final String applicationsFolderName;

    private List<String> applicationNames;

    public AppNameCollectorImpl(String applicationsFolderName) {

        this.applicationsFolderName = applicationsFolderName;
    }

    @Override
    public List<String> getApplicationNames(String workingDir) {
        if (this.applicationNames != null) {
            return this.applicationNames;
        }

        this.loadApplicationNames(workingDir + this.applicationsFolderName);
        return this.applicationNames;
    }

    /**
     * Scans folder for jar files and adds jar names to a list.
     */
    private void loadApplicationNames(String applicationsFolder) {
        this.applicationNames = new ArrayList<>();

        File file = new File(applicationsFolder);
        if (!file.exists() || !file.isDirectory()) {
            throw new RuntimeException(String.format(APPLICATIONS_FOLDER_NON_EXISTENT, applicationsFolder));
        }

        Arrays.stream(file.listFiles()).filter(this::isJarFile).forEach(f -> {
            String s = "/" + f.getName().replace(".jar", "");
            this.applicationNames.add(s);
        });
    }

    /**
     * Checks if a file name ends with .jar
     */
    private boolean isJarFile(File file) {
        return file.isFile() && file.getName().endsWith(".jar");
    }
}
