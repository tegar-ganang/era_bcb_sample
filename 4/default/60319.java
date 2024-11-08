import com.jclark.xsl.sax.*;
import java.io.*;
import java.util.Hashtable;
import org.xml.sax.*;

public class XSLTransformer {

    class ErrorHandlerImpl implements ErrorHandler {

        public void warning(SAXParseException saxparseexception) {
            printSAXParseException(saxparseexception);
        }

        public void error(SAXParseException saxparseexception) {
            printSAXParseException(saxparseexception);
        }

        public void fatalError(SAXParseException saxparseexception) throws SAXException {
            throw saxparseexception;
        }

        ErrorHandlerImpl() {
        }
    }

    public void setParser() throws IOException {
        setParser("com.jclark.xml.sax.CommentDriver");
    }

    public void setParser(String s) throws IOException {
        System.getProperties().put("com.jclark.xsl.sax.parser", s);
        xsl = new XSLProcessorImpl();
        setParser(xsl);
        xsl.setErrorHandler(new ErrorHandlerImpl());
        outputMethodHandler = new OutputMethodHandlerImpl(xsl);
        xsl.setOutputMethodHandler(outputMethodHandler);
    }

    public void setXSLParameter(String s, String s1) {
        if (xsl == null) {
            throw new IllegalArgumentException("Have not set XSL parser yet.");
        } else {
            xsl.setParameter(s, s1);
            return;
        }
    }

    public void setXMLInput(File file) {
        xmlInput = Driver.fileInputSource(file);
    }

    public void setXSLInput(File file) {
        xslInput = Driver.fileInputSource(file);
    }

    public void setXMLInput(Reader reader) {
        xmlInput = new InputSource(reader);
    }

    public void setXSLInput(Reader reader) {
        xslInput = new InputSource(reader);
    }

    public void setOutput(File file) {
        outputDest = new FileDestination(file);
    }

    public void setOutput(Writer writer) {
        outputDest = new WriterDestination(writer);
    }

    public void setFiles(File file, File file1, File file2) {
        setXMLInput(file);
        setXSLInput(file1);
        setOutput(file2);
    }

    public void setData(Reader reader, Reader reader1, Writer writer) {
        setXMLInput(reader);
        setXSLInput(reader1);
        setOutput(writer);
    }

    public boolean transformFile(XSLProcessor xslprocessor, OutputMethodHandlerImpl outputmethodhandlerimpl, File file, File file1, File file2) {
        Object obj;
        if (file2 == null) {
            obj = new FileDescriptorDestination(FileDescriptor.out);
        } else {
            obj = new FileDestination(file2);
        }
        outputmethodhandlerimpl.setDestination(((Destination) (obj)));
        return transform(xslprocessor, Driver.fileInputSource(file1), Driver.fileInputSource(file));
    }

    public boolean transform(XSLProcessor xslprocessor, OutputMethodHandlerImpl outputmethodhandlerimpl, Reader reader, Reader reader1, Writer writer) {
        Object obj;
        if (writer == null) {
            obj = new FileDescriptorDestination(FileDescriptor.out);
        } else {
            obj = new WriterDestination(writer);
        }
        outputmethodhandlerimpl.setDestination(((Destination) (obj)));
        return transform(xslprocessor, new InputSource(reader1), new InputSource(reader));
    }

    public boolean transform(XSLProcessor xslprocessor, InputSource inputsource, InputSource inputsource1) {
        try {
            xslprocessor.loadStylesheet(inputsource);
            xslprocessor.parse(inputsource1);
            return true;
        } catch (SAXParseException saxparseexception) {
            printSAXParseException(saxparseexception);
        } catch (SAXException saxexception) {
            System.err.println(saxexception.getMessage());
        } catch (IOException ioexception) {
            System.err.println(ioexception.toString());
        }
        return false;
    }

    public boolean transform() {
        if (xsl == null) {
            try {
                setParser();
            } catch (IOException ioexception) {
                throw new IllegalArgumentException(ioexception.getMessage());
            }
        }
        if (xslInput == null || xmlInput == null) {
            throw new IllegalArgumentException("Must set xml and xsl input for transform.XSLTransformer");
        }
        if (outputDest == null) {
            outputDest = new FileDescriptorDestination(FileDescriptor.out);
        }
        outputMethodHandler.setDestination(outputDest);
        return transform(((XSLProcessor) (xsl)), xslInput, xmlInput);
    }

    void printSAXParseException(SAXParseException saxparseexception) {
        String s = saxparseexception.getSystemId();
        int i = saxparseexception.getLineNumber();
        if (s != null) {
            System.err.print(s + ":");
        }
        if (i >= 0) {
            System.err.print(i + ":");
        }
        if (s != null || i >= 0) {
            System.err.print(" ");
        }
        System.err.println(saxparseexception.getMessage());
    }

    public void setParser(XSLProcessorImpl xslprocessorimpl) throws IOException {
        String s = System.getProperty("com.jclark.xsl.sax.parser");
        if (s == null) {
            s = System.getProperty("org.xml.sax.parser");
        }
        if (s == null) {
            s = "com.jclark.xml.sax.CommentDriver";
        }
        try {
            Object obj = Class.forName(s).newInstance();
            if (obj instanceof XMLProcessorEx) {
                xslprocessorimpl.setParser((XMLProcessorEx) obj);
                return;
            } else {
                xslprocessorimpl.setParser((Parser) obj);
                return;
            }
        } catch (ClassNotFoundException classnotfoundexception) {
            System.err.println(classnotfoundexception.toString());
        } catch (InstantiationException instantiationexception) {
            System.err.println(instantiationexception.toString());
        } catch (IllegalAccessException illegalaccessexception) {
            System.err.println(illegalaccessexception.toString());
        } catch (ClassCastException _ex) {
            System.err.println(s + " is not a SAX driver");
        }
        throw new IOException("Error in XSLTransformer.setParser()");
    }

    public XSLTransformer() {
    }

    XSLProcessorImpl xsl;

    OutputMethodHandlerImpl outputMethodHandler;

    public InputSource xmlInput;

    public InputSource xslInput;

    public Destination outputDest;
}
