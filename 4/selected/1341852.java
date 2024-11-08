package net.stickycode.example;

import java.io.StringWriter;
import java.util.regex.Pattern;
import net.stickycode.resource.ClasspathResource;
import net.stickycode.resource.ResourceReader;
import org.parboiled.Parboiled;
import org.pegdown.PegDownProcessor;

public class Example {

    private Pattern annotation = Pattern.compile("@([a-zA-Z][a-zA-Z0-9_]+)");

    private Pattern type = Pattern.compile("([a-zA-Z][a-zA-Z0-9_]+)\\.class");

    public void process(ClasspathResource resource, StringWriter writer) {
        ResourceReader reader = new ResourceReader(resource);
        ExampleParser parser = Parboiled.createParser(ExampleParser.class);
        PegDownProcessor processor = new PegDownProcessor(parser);
        writer.write(processor.markdownToHtml(reader.toCharArray()));
    }

    String expand(String readLine) {
        int include = readLine.indexOf("<!--#include name=\"");
        if (include == -1) return readLine;
        StringBuilder builder = new StringBuilder();
        builder.append(readLine.substring(0, include));
        int endOfPath = readLine.indexOf("\" -->", include);
        builder.append(load(readLine.substring(include + 21, endOfPath)));
        builder.append(readLine.substring(endOfPath + 5));
        return builder.toString();
    }

    private String load(String file) {
        return null;
    }

    String process(String readLine) {
        String annotations = annotation.matcher(readLine).replaceAll("<a href=\"#\" id=\"see-$1\">@$1</a>");
        String replaceAll = type.matcher(annotations).replaceAll("<a href=\"#\" id=\"see-$1\">$1.class</a>");
        return replaceAll;
    }
}
