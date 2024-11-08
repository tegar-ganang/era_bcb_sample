package org.mime4j.test;

import org.mime4j.*;
import java.util.Map;
import java.io.*;

public class TestCallbacks implements MIMEEventCallbacks {

    private int indent = 0;

    public void onNonMimeBody(Headers headers, InputStream stream) {
    }

    public void onDiscreteBodyPart(Headers headers, InputStream stream) throws IOException {
        WriteIndent();
        System.out.println("Discrete: " + headers.getHeaderValue("Content-Type"));
        Map ctParams = headers.getHeaderParams("Content-Type", true);
        if (ctParams == null || ((String) ctParams.get("")).toLowerCase().startsWith("text/")) {
            String charset = "US-ASCII";
            if (ctParams != null && ctParams.containsKey("charset")) {
                charset = (String) ctParams.get("charset");
            }
            InputStreamReader reader = null;
            StringWriter writer = null;
            try {
                try {
                    reader = new InputStreamReader(stream, charset);
                } catch (UnsupportedEncodingException e) {
                    Debug.fail("Encoding " + charset + " not available, reverting to US-ASCII");
                    reader = new InputStreamReader(stream);
                }
                writer = new StringWriter();
                int c;
                while (-1 != (c = reader.read())) writer.write(c);
                System.out.println(writer.toString());
            } finally {
                if (reader != null) reader.close();
                if (writer != null) writer.close();
            }
        } else if (ctParams != null && ((String) ctParams.get("")).toLowerCase().startsWith("image/")) {
            if (stream instanceof QuotedPrintableInputStream || (stream instanceof CloseShieldInputStream && ((CloseShieldInputStream) stream).getUnderlyingStream() instanceof QuotedPrintableInputStream)) System.out.println("Using QP"); else if (stream instanceof Base64InputStream || (stream instanceof CloseShieldInputStream && ((CloseShieldInputStream) stream).getUnderlyingStream() instanceof Base64InputStream)) System.out.println("Using B64"); else System.out.println("Using ASCII");
            OutputStream s = null;
            try {
                String filename = "c:\\temp\\mimetest2\\" + Math.random() + ".gif";
                s = new FileOutputStream(filename);
                int b;
                while (-1 != (b = stream.read())) s.write(b);
                System.out.println("Wrote " + filename);
            } finally {
                try {
                    s.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void onHeaders(Headers headers) {
        if (headers.containsHeader("Content-Transfer-Encoding")) {
            WriteIndent();
            System.out.println("Encoding: " + headers.getHeaderValue("Content-Transfer-Encoding"));
        }
    }

    public void onPreamble(InputStream stream) throws IOException {
        WriteIndent();
        System.out.println("Preamble");
    }

    public void onEpilogue(InputStream stream) throws IOException {
        WriteIndent();
        System.out.println("Epilogue");
    }

    public void onMultipartEnd() {
        indent--;
        WriteIndent();
        System.out.println("/Multipart");
    }

    public void onMultipartStart(Headers headers) {
        WriteIndent();
        System.out.println("Multipart: " + headers.getHeaderValue("Content-Type"));
        indent++;
    }

    public void onMessageStart(Headers headers) {
        WriteIndent();
        System.out.println("Message: " + headers.getHeaderValue("Content-Type"));
        indent++;
    }

    public void onMessageEnd() {
        indent--;
        WriteIndent();
        System.out.println("/Message");
    }

    private void WriteIndent() {
        for (int i = 0; i < indent; i++) System.out.print("  ");
    }
}
