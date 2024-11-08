package edu.usc.epigenome.uecgatk.YapingWriter;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;

public class bedObjectWriterImp extends FormatWriterBase {

    public bedObjectWriterImp(File location) {
        super(location);
    }

    public bedObjectWriterImp(OutputStream output) {
        super(output);
    }

    public bedObjectWriterImp(File location, OutputStream output) {
        super(location, output);
    }

    @Override
    public void add(genomeObject obj) {
        String readsLine = String.format("%s\t%d\t%d", obj.getChr(), obj.getStart(), ((bedObject) obj).getEnd());
        try {
            mWriter.write(readsLine);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Iterator<Object> it = ((bedObject) obj).getValueObject().iterator();
        while (it.hasNext()) {
            try {
                mWriter.write("\t" + it.next());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            mWriter.write("\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addHeader(Object o) {
        try {
            mWriter.write(o.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
