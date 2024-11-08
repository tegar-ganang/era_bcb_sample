package com.sourcetohtml;

import java.io.File;
import java.io.IOException;

/**
 * Class that will manage generation of whole folder
 * It will recursively iterate through the folder 
 * and use PageGenerator to generate HTML detail of each file
 * @author jcelak
 */
public class FolderIterator {

    private File input;

    private File rootInput;

    private File rootOutput;

    private PageGenerator pageGenerator = new PageGenerator();

    public FolderIterator(File input, File output) {
        this.input = input;
        if (input.isDirectory()) this.rootInput = input; else this.rootInput = input.getParentFile();
        this.rootOutput = output;
    }

    public void generate() throws IOException {
        generate(input, rootOutput);
        FileUtils.copyFile(getClass().getResourceAsStream(Config.getInstance().getProperty("cssStyles", "/styles.css")), new File(rootOutput, "sourcetohtml-styles.css"));
    }

    private void generate(File input, File output) throws IOException {
        pageGenerator.generate(input, output, rootInput);
        if (input.isDirectory()) {
            output.mkdirs();
            for (File f : input.listFiles()) {
                generate(f, new File(output, f.getName()));
            }
        }
    }
}
