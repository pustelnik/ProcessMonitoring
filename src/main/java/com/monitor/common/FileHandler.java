package com.monitor.common;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * @author jakub on 19.08.16.
 */
public class FileHandler {
    private final String templateName;

    public FileHandler(String templateName) {
        this.templateName = templateName;
    }

    private static final Logger LOGGER = LogManager.getLogger(FileHandler.class);
    /**
     * Implementation:
     * http://docs.oracle.com/javase/8/docs/technotes/guides/io/fsp/zipfilesystemprovider.html
     *
     * @param file1target Target file to copy first resource
     * @param file2target Target file to copy second resource
     * @throws IOException If file does not exists
     */
    private void copyTemplateResourcesFromJar(File file1target, File file2target) throws IOException {
        try {
            Files.createDirectory(new File("target/classes/processCharts").toPath());
        } catch (FileAlreadyExistsException e) {
            LOGGER.debug(e);
        }
        String pathToResource = getClass().getResource("/processCharts/"+templateName).getPath();
        URI uri1 = URI.create("jar:" + pathToResource);
        Map<String, String> env = new HashMap<>();
        env.put("create", "true");
        final FileSystem zipFs = FileSystems.newFileSystem(uri1,env);
        try {
            Path pathInZipfile1 = zipFs.getPath("/processCharts/"+templateName);
            Path pathInZipfile2 = zipFs.getPath("/processCharts/Chart.bundle.js");
            Files.copy(pathInZipfile1,file1target.toPath(),REPLACE_EXISTING);
            Files.copy(pathInZipfile2,file2target.toPath(),REPLACE_EXISTING);
        } catch (FileNotFoundException e) {
            LOGGER.debug(e);
        }
    }

    public void copyMonitoringResults(String targetPath) throws IOException {
        if(StringUtils.isBlank(targetPath)) {
            throw new IOException("Invalid path");
        }
        if (!targetPath.endsWith("/")) {
            targetPath += "/";
        }
        try {
            Files.createDirectories(new File(targetPath+"charts").toPath());
        } catch (FileAlreadyExistsException e) {
            LOGGER.debug(e);
        }
        File file1target = new File(targetPath+"charts/"+templateName);
        File file2target = new File(targetPath+"charts/Chart.bundle.js");
        File file3target = new File(targetPath+"charts/data.js");

        File file1source = new File("target/classes/processCharts/"+templateName);
        File file2source = new File("target/classes/processCharts/Chart.bundle.js");

        if(Files.notExists(file1source.toPath()) || Files.notExists(file2source.toPath())) {
            copyTemplateResourcesFromJar(file1source, file2source);
        }
        Files.copy(file1source.toPath(), file1target.toPath(), REPLACE_EXISTING);
        Files.copy(file2source.toPath(), file2target.toPath(), REPLACE_EXISTING);
        Files.copy(new File("target/classes/processCharts/data.js").toPath(), file3target.toPath(), REPLACE_EXISTING);
    }
}
