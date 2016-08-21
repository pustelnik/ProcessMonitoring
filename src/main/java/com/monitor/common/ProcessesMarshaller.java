package com.monitor.common;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

/**
 * @author jakub on 19.08.16.
 */
public abstract class ProcessesMarshaller {

    private static Marshaller marshaller(Class[] classes) throws JAXBException {
        Map<String, Object> properties = new HashMap<>(2);
        properties.put("eclipselink.media-type", "application/json");
        properties.put("eclipselink.json.include-root", false);
        JAXBContext jaxbContext = JAXBContext.newInstance(classes,properties);
        return jaxbContext.createMarshaller();
    }

    public static <T> void SaveProcessesAsJson(Class[] classes, T processes) throws IOException, JAXBException {
        Files.createDirectories(new File("target/classes/processCharts/").toPath());
        File file = new File(String.format("target/processes_%d.json", System.currentTimeMillis() / 1000));
        marshaller(classes).marshal(processes,new FileWriter(file));
        FileWriter fileWriter = new FileWriter(new File("target/classes/processCharts/data.js"));
        StringBuilder json = new StringBuilder();
        Files.readAllLines(file.toPath(), Charset.defaultCharset()).forEach(json::append);
        fileWriter.append("var processStat = ");
        fileWriter.append(json.toString());
        fileWriter.append(";");
        fileWriter.close();
    }
}
