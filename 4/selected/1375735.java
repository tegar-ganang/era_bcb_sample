package org.fnal.mcas.mule.transformers;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringBufferInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.Exception;
import java.lang.Process;
import java.lang.Runtime;
import java.lang.Thread;
import java.lang.System;
import org.mule.transformer.AbstractTransformer;
import org.mule.api.transformer.TransformerException;
import org.mule.util.IOUtils;

class CopyThread extends Thread {

    private InputStream _src;

    private OutputStream _dest;

    CopyThread(InputStream src, OutputStream dest) {
        _src = src;
        _dest = dest;
    }

    public void run() {
        try {
            try {
                IOUtils.copy(_src, _dest);
            } finally {
                _dest.close();
            }
        } catch (Exception e) {
        }
    }
}

public class HtmlToXml extends AbstractTransformer {

    public HtmlToXml() {
        setReturnClass(String.class);
        registerSourceType(InputStream.class);
        registerSourceType(String.class);
    }

    public Object doTransform(Object src, String encoding) throws TransformerException {
        try {
            if (src instanceof InputStream) return convert((InputStream) src, encoding); else return convert(new StringBufferInputStream((String) src), encoding);
        } catch (Exception ex) {
            throw new TransformerException(this, ex);
        }
    }

    private String emptyTags = "unknown,emph";

    public void setEmptyTags(String tags) {
        emptyTags = tags;
    }

    public String getEmptyTags() {
        return emptyTags;
    }

    private String convert(InputStream input, String encoding) throws Exception {
        Process p = Runtime.getRuntime().exec("tidy -q -f /dev/null -wrap 0 --output-xml yes --doctype omit --force-output true --new-empty-tags  " + emptyTags + " --quote-nbsp no -utf8");
        Thread t = new CopyThread(input, p.getOutputStream());
        t.start();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        IOUtils.copy(p.getInputStream(), output);
        p.waitFor();
        t.join();
        return output.toString();
    }
}
