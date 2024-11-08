package org.sinaxe.context;

import org.jaxen.Function;
import org.jaxen.Context;
import org.jaxen.FunctionCallException;
import org.jaxen.Navigator;
import org.dom4j.Node;
import java.util.List;
import java.io.*;
import org.dom4j.io.XMLWriter;
import org.dom4j.io.OutputFormat;

public class XPathWriteFileFunction extends SinaxeXPathFunction {

    public static final String name = "writefile";

    public XPathWriteFileFunction() {
    }

    public String getName() {
        return name;
    }

    public Boolean writeXML(String filename, Object node) throws FunctionCallException {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(filename);
            OutputFormat formater = OutputFormat.createCompactFormat();
            formater.setTrimText(false);
            XMLWriter writer = new XMLWriter(fileOutputStream, formater);
            writer.write((Node) node);
            return Boolean.TRUE;
        } catch (Exception e) {
            callException("Failed to save file '" + filename + "'!!!", e);
        }
        return Boolean.FALSE;
    }

    public Object call(Context context, List args) throws FunctionCallException {
        checkArgs(args, 2);
        Function stringFunction = getFunction(context, "string");
        String filename = (String) stringFunction.call(context, args.subList(0, 1));
        Navigator nav = context.getNavigator();
        Object node = args.get(1);
        if (node instanceof List && !((List) node).isEmpty()) node = ((List) node).get(0);
        if (nav.isElement(node) || nav.isDocument(node)) return writeXML(filename, node);
        String data = (String) stringFunction.call(context, args.subList(1, 2));
        try {
            ByteArrayInputStream input = new ByteArrayInputStream(data.getBytes());
            OutputStream output = new FileOutputStream(filename);
            int size;
            byte[] buf = new byte[512];
            while ((size = input.read(buf)) >= 0) output.write(buf, 0, size);
            return Boolean.TRUE;
        } catch (Exception e) {
            callException("failed to open file '" + filename + "'", e);
        }
        return Boolean.FALSE;
    }
}
