package com.sourcetohtml;

import java.io.File;
import java.io.IOException;

/**
 * This is a page layout manager
 * It concats header, breadcrumb, generated file detail HTML and footer
 * @author jcelak
 */
public class PageGenerator {

    private BreadcrumGenerator breadcrumGenerator = new BreadcrumGenerator();

    public void generate(File input, File output, File rootInput) throws IOException {
        FileDetailGenerator generator = FileDetailGeneratorFactory.getInstance().getGenerator(input);
        String fileContent = generator.getHTML(input, rootInput);
        generatePage(rootInput, input, generator.getOutputFile(output), fileContent);
        if (input.isDirectory()) {
            output.mkdirs();
        } else {
            FileUtils.copyFile(input, output);
        }
    }

    private void generatePage(File root, File aInput, File output, String fileContent) {
        String pathToRoot = "";
        File input = aInput;
        if (!input.isDirectory()) input = input.getParentFile();
        while (!input.equals(root)) {
            pathToRoot += "../";
            input = input.getParentFile();
        }
        String header = FileUtils.readStream(FolderIterator.class.getResourceAsStream(Config.getInstance().getProperty("layout.header", "/header.html")));
        header = header.replace("{0}", pathToRoot);
        String footer = FileUtils.readStream(FolderIterator.class.getResourceAsStream(Config.getInstance().getProperty("layout.footer", "/footer.html")));
        System.out.println("Generating:" + output.getAbsolutePath());
        FileUtils.saveFile(header + (Config.getInstance().getBooleanProperty("layout.generateBreadCrumb", true) ? breadcrumGenerator.generateThumbnail(root, aInput) : "") + fileContent + footer, output);
    }
}
