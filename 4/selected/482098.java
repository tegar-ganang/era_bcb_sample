package com.ibm.wala.cast.js.util;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Stack;
import javax.swing.text.BadLocationException;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import com.ibm.wala.util.collections.HashMapFactory;

public class HTMLCallback extends HTMLEditorKit.ParserCallback {

    private final URL input;

    private final FileWriter out, out2;

    private int counter = 0;

    private boolean script = false;

    private final HashMap<String, String> constructors = HashMapFactory.make();

    protected final Stack<String> stack;

    public HTMLCallback(URL input, FileWriter out, FileWriter out2) {
        this.input = input;
        this.out = out;
        this.out2 = out2;
        stack = new Stack<String>();
        constructors.put("FORM", "DOMHTMLFormElement");
        constructors.put("TABLE", "DOMHTMLTableElement");
    }

    protected FileWriter getWriter() {
        return out;
    }

    protected FileWriter getWriter2() {
        return out2;
    }

    public void flush() throws BadLocationException {
    }

    public void handleText(char[] data, int pos) {
        System.out.println("text pos: " + pos);
        getScript(data);
    }

    private void getScript(char[] data) {
        if (script) {
            System.out.println(new String(data));
            try {
                out2.write(data);
                out2.write("\n");
            } catch (IOException e) {
                System.out.println("Error writing to second file");
            }
        }
    }

    public void handleComment(char[] data, int pos) {
        System.out.println("comment pos: " + pos);
        getScript(data);
    }

    public void handleEndOfLineString(String eol) {
        if (script) {
            try {
                out2.write("\n");
            } catch (IOException e) {
                System.out.println("Error writing to second file");
            }
        }
    }

    protected String createElement(HTML.Tag t, MutableAttributeSet a) {
        String tag = t.toString().toUpperCase();
        String varName = "node" + (counter++);
        String cons = constructors.get(tag);
        if (tag.equals("SCRIPT")) {
            Object value = a.getAttribute(HTML.Attribute.SRC);
            if (value != null) {
                try {
                    URL scriptSrc = new URL(input, value.toString());
                    InputStreamReader scriptReader = new InputStreamReader(scriptSrc.openConnection().getInputStream());
                    int read;
                    char[] buffer = new char[1024];
                    while ((read = scriptReader.read(buffer)) != -1) {
                        out2.write(buffer, 0, read);
                    }
                    scriptReader.close();
                } catch (IOException e) {
                    System.out.println("bad input script " + value);
                }
            } else {
                System.out.println("Entering Script");
                script = true;
            }
        }
        if (cons == null) cons = "DOMHTMLElement";
        try {
            writeElement(t, a, tag, cons, varName);
            out.write("\n");
        } catch (IOException e) {
            System.out.println("Error writing to file");
            System.exit(1);
        }
        return varName;
    }

    protected void writeElement(HTML.Tag t, MutableAttributeSet a, String tag, String cons, String varName) throws IOException {
        Enumeration enu = a.getAttributeNames();
        if (!enu.hasMoreElements()) {
            out.write("var " + varName + " = new " + cons + "(" + tag + ");\n");
        } else {
            out.write("function make_" + varName + "() {\n");
            out.write("  this.temp = " + cons + ";\n");
            out.write("  this.temp(" + tag + ");\n");
            while (enu.hasMoreElements()) {
                Object attrObj = enu.nextElement();
                String attr = attrObj.toString();
                String value = a.getAttribute(attrObj).toString();
                System.out.println(attr);
                writeAttribute(t, a, attr, value, "this", varName);
            }
            out.write("}\n");
            out.write("var " + varName + " = new make_" + varName + "();\n");
        }
        if (!stack.empty()) {
            out.write(stack.peek() + ".appendChild(" + varName + ");\n");
        } else {
            out.write("document.appendChild(" + varName + ");\n");
        }
    }

    protected void writeAttribute(HTML.Tag t, MutableAttributeSet a, String attr, String value, String varName, String varName2) throws IOException {
        writePortletAttribute(t, a, attr, value, varName);
        writeEventAttribute(t, a, attr, value, varName, varName2);
    }

    protected void writeEventAttribute(HTML.Tag t, MutableAttributeSet a, String attr, String value, String varName, String varName2) throws IOException {
        if (attr.substring(0, 2).equals("on")) {
            out.write(varName + "." + attr + " = function " + attr + "_" + varName + "(event) {" + value + "};\n");
            out2.write("\n\n" + varName2 + "." + attr + "(null);\n\n");
        } else {
            out.write(varName + ".setAttribute('" + attr + "', '" + value + "');\n");
        }
    }

    protected void writePortletAttribute(HTML.Tag t, MutableAttributeSet a, String attr, String value, String varName) throws IOException {
        if (attr.equals("portletid")) {
            if (value.substring(value.length() - 4).equals("vice")) {
                out.write("\n\nfunction cVice() { var contextVice = " + varName + "; }\ncVice();\n\n");
            } else if (value.substring(value.length() - 4).equals("root")) {
                out.write("\n\nfunction cRoot() { var contextRoot = " + varName + "; }\ncRoot();\n\n");
            }
        }
    }

    public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
        System.out.println("Start" + t);
        String varName = createElement(t, a);
        stack.push(varName);
    }

    public void handleEndTag(HTML.Tag t, int pos) {
        if (t.toString().toUpperCase().equals("SCRIPT")) {
            System.out.println("Exiting Script");
            script = false;
        }
        System.out.println("End" + t);
        stack.pop();
    }

    public void handleSimpleTag(HTML.Tag t, MutableAttributeSet a, int pos) {
        System.out.println("Simple" + t);
        createElement(t, a);
    }

    public void handleError(String errorMsg, int pos) {
        System.out.println("Error" + errorMsg);
    }
}
