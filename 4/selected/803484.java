package org.maestroframework.maestro.widgets;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import org.maestroframework.markup.Component;
import org.maestroframework.utils.StreamUtils;

/**
 * The FileReaderComponent reads and prints out a file within
 * the Component tree context;
 * 
 * @author jgarlick
 *
 */
public class FileReaderComponent extends Component {

    private File file;

    public FileReaderComponent(File file) {
        super("");
        this.file = file;
    }

    @Override
    public void write(Writer writer, int depth) throws IOException {
        FileInputStream in = new FileInputStream(file);
        InputStreamReader reader = new InputStreamReader(in);
        StreamUtils.copyStream(reader, writer);
        reader.close();
        in.close();
    }
}
