package org.akrogen.core.codegen.code;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;

public abstract class AbstractCodeUpdater implements ICodeUpdater {

    public Reader getUpdatedContents() throws Exception {
        StringWriter writer = new StringWriter();
        save(writer);
        writer.flush();
        writer.close();
        StringReader reader = new StringReader(writer.getBuffer().toString());
        return reader;
    }
}
