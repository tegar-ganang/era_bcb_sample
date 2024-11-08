package mcujavasource.transformer.result;

import java.io.*;
import java.lang.reflect.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.*;
import org.w3c.dom.*;
import mcujavasource.transformer.*;

/**
 *
 */
public class CSourceFileGenerator extends ConfigurableTransformer {

    private static final int BUFFER_SIZE = 131072;

    private File resultDir;

    private CSourceWriter writer;

    private boolean mainWritten;

    /** Creates a new instance of CSourceFileGenerator
   *
   */
    public CSourceFileGenerator(File resultDir, Map<String, Object> settings) {
        this.resultDir = resultDir;
        addSettings(settings);
    }

    public void generate(Node source) throws IOException {
        mainWritten = false;
        writer = new CSourceWriter();
        writeHeader();
        writer.writeFragment(callPlatformMethod("getHeader").toString());
        writer.newLine();
        process(source, true);
        if (!mainWritten) writeMain(null);
        writer.newLine();
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(resultDir.getPath() + File.separator + "main.c"), (String) settings.get("resultEncoding")));
            bw.write(writer.getContent());
        } finally {
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException eIgnored) {
                }
            }
        }
        writer = null;
    }

    private void process(Node n, boolean operatorEnds) throws IOException {
        NodeList nList = n.getChildNodes();
        for (int i = 0; i < nList.getLength(); i++) {
            Node n1 = nList.item(i);
            if (n1.getNodeType() == Node.ELEMENT_NODE) {
                Element e1 = (Element) n1;
                processDirect(e1, operatorEnds);
            }
        }
    }

    private void processDirect(Element e1, boolean operatorEnds) throws IOException {
        String name = e1.getNodeName();
        if (name.equals("block")) {
            writer.enterBlock();
            process(e1, true);
            writer.exitBlock();
        } else if (name.equals("define")) {
            writer.newLine();
            writer.writeFragment("#define " + e1.getAttribute("name") + "\t" + e1.getAttribute("value"));
            writer.newLine();
        } else if (name.equals("typedef")) {
            writer.newLine();
            writer.writeFragment("typedef " + e1.getAttribute("name") + "\t" + e1.getAttribute("value") + ";");
            writer.newLine();
        } else if (name.equals("include")) {
            writer.newLine();
            String value = e1.getAttribute("value");
            if (value.startsWith("/")) {
                writeFile(value);
                value = value.substring(1);
            }
            if (!value.startsWith("<")) value = "\"" + value + "\"";
            writer.writeFragment("#include " + value);
            writer.newLine();
        } else if (name.equals("function-decl")) {
            writer.writeFragment((String) callPlatformMethod("processFunctionDeclaration", e1));
            writer.writeFragment(";");
            writer.newLine();
        } else if (name.equals("register")) {
            String s = (String) callPlatformMethod("processRegister", e1);
            if (s.trim().length() > 0) {
                writer.writeFragment(s);
                writer.newLine();
            }
        } else if (name.equals("continue")) {
            writer.writeFragment("continue;");
            writer.newLine();
        } else if (name.equals("break")) {
            writer.writeFragment("break;");
            writer.newLine();
        } else if (name.equals("goto")) {
            writer.writeFragment("goto " + e1.getAttribute("targetname") + ";");
            writer.newLine();
        } else if (name.equals("field") || name.equals("local-variable")) {
            writer.writeFragment((String) callPlatformMethod("processVariableDeclaration", e1));
            processVariableDeclaration(e1);
            if (operatorEnds) {
                writer.writeFragment(";");
                writer.newLine();
            }
        } else if (name.equals("constructor")) {
            writeMain(e1);
        } else if (name.equals("method")) {
            writer.newLine();
            writer.newLine();
            writer.writeFragment((String) callPlatformMethod("processFunctionDeclaration", e1));
            process(e1, true);
        } else if (name.equals("send")) {
            processSend(e1);
            if (operatorEnds) {
                writer.writeFragment(";");
                writer.newLine();
            }
        } else if (name.equals("literal-boolean")) {
            writer.writeFragment(e1.getAttribute("value"));
            if (operatorEnds) {
                writer.writeFragment(";");
                writer.newLine();
            }
        } else if (name.equals("literal-char")) {
            writer.writeFragment("'" + e1.getAttribute("value") + "'");
            if (operatorEnds) {
                writer.writeFragment(";");
                writer.newLine();
            }
        } else if (name.equals("literal-number")) {
            writer.writeFragment(e1.getAttribute("value"));
            String kind = "integer";
            if (e1.hasAttribute("kind")) kind = e1.getAttribute("kind");
            if (kind.equals("long")) writer.writeFragment("L");
            if (operatorEnds) {
                writer.writeFragment(";");
                writer.newLine();
            }
        } else if (name.equals("literal-string")) {
            writer.writeFragment("\"" + e1.getAttribute("value") + "\"");
            if (operatorEnds) {
                writer.writeFragment(";");
                writer.newLine();
            }
        } else if (name.equals("paren")) {
            writer.writeFragment("(");
            process(e1, false);
            writer.writeFragment(")");
            if (operatorEnds) {
                writer.writeFragment(";");
                writer.newLine();
            }
        } else if (name.equals("var-ref")) {
            writer.writeFragment(e1.getAttribute("name"));
            if (operatorEnds) {
                writer.writeFragment(";");
                writer.newLine();
            }
        } else if (name.equals("field-access")) {
            processTarget(e1);
            writer.writeFragment(e1.getAttribute("field"));
            if (operatorEnds) {
                writer.writeFragment(";");
                writer.newLine();
            }
        } else if (name.equals("assignment-expr")) {
            processAssignmentExpr(e1);
            if (operatorEnds) {
                writer.writeFragment(";");
                writer.newLine();
            }
        } else if (name.equals("binary-expr")) {
            processBinaryExpr(e1);
            if (operatorEnds) {
                writer.writeFragment(";");
                writer.newLine();
            }
        } else if (name.equals("unary-expr")) {
            processUnaryExpr(e1);
            if (operatorEnds) {
                writer.writeFragment(";");
                writer.newLine();
            }
        } else if (name.equals("return")) {
            writer.writeFragment("return ");
            process(e1, false);
            writer.writeFragment(";");
        } else if (name.equals("cast-expr")) {
            writer.writeFragment("((" + SourceGeneratorHelper.getType(e1) + ") ");
            process(e1, false);
            writer.writeFragment(")");
            if (operatorEnds) {
                writer.writeFragment(";");
                writer.newLine();
            }
        } else if (name.equals("if")) {
            processIf(e1);
        } else if (name.equals("array-ref")) {
            processArrayRef(e1);
            if (operatorEnds) {
                writer.writeFragment(";");
                writer.newLine();
            }
        } else if (name.equals("loop")) {
            processLoop(e1);
        } else if (name.equals("conditional-expr")) {
            processConditionalExpr(e1);
        } else if (name.equals("switch")) {
            processSwitch(e1);
        } else if (name.equals("array-initializer")) {
            processArrayInitializer(e1);
            if (operatorEnds) {
                writer.writeFragment(";");
                writer.newLine();
            }
        } else if (name.equals("label")) {
            process(e1, true);
            writer.writeFragment(e1.getAttribute("name") + ": ");
            writer.newLine();
        } else if (name.equals("empty")) {
            if (operatorEnds) {
                writer.writeFragment(";");
                writer.newLine();
            }
        } else if (name.equals("c-source-program") || name.equals("c-source-file")) {
            process(e1, true);
        } else if (name.equals("type") || name.equals("formal-arguments") || name.equals("target") || name.equals("lvalue") || name.equals("dim-expr")) {
        } else if (name.equals("type-parameters") || name.equals("type-argument")) {
        }
    }

    private void writeMain(Element e1) throws IOException {
        writer.newLine();
        writer.newLine();
        writer.writeFragment((String) callPlatformMethod("getMainFuctionDeclaration"));
        writer.enterBlock();
        if (e1 != null) process(e1, true);
        writer.writeFragment("init();");
        writer.newLine();
        writer.writeFragment("start();");
        writer.newLine();
        writer.writeFragment((String) callPlatformMethod("getMainFunctionEnd"));
        writer.exitBlock();
    }

    private void processVariableDeclaration(Element n) throws IOException {
        NodeList nList = n.getChildNodes();
        for (int i = 0; i < nList.getLength(); i++) {
            Node n1 = nList.item(i);
            if (n1.getNodeType() == Node.ELEMENT_NODE) {
                Element e1 = (Element) n1;
                String name = n1.getNodeName();
                if (name.equals("type")) ; else if (name.equals("new-array")) {
                    checkArrayInitializer(e1);
                } else {
                    writer.writeFragment(" = ");
                    processDirect(e1, false);
                }
            }
        }
    }

    private void checkArrayInitializer(Element n) throws IOException {
        NodeList nList = n.getChildNodes();
        for (int i = 0; i < nList.getLength(); i++) {
            Node n1 = nList.item(i);
            if (n1.getNodeType() == Node.ELEMENT_NODE) {
                Element e1 = (Element) n1;
                String name = n1.getNodeName();
                if (name.equals("array-initializer")) {
                    writer.writeFragment(" = ");
                    processDirect(e1, false);
                }
            }
        }
    }

    private void processSend(Element n) throws IOException {
        NodeList nList = n.getChildNodes();
        for (int i = 0; i < nList.getLength(); i++) {
            Node n1 = nList.item(i);
            if (n1.getNodeType() == Node.ELEMENT_NODE) {
                Element e1 = (Element) n1;
                String name = n1.getNodeName();
                if (name.equals("target")) {
                    processTarget(e1);
                } else if (name.equals("arguments")) {
                    writer.writeFragment(n.getAttribute("message"));
                    processSendArguments(e1);
                    break;
                }
            }
        }
    }

    private void processTarget(Element n) throws IOException {
        boolean present = false;
        NodeList nList = n.getChildNodes();
        for (int i = 0; i < nList.getLength(); i++) {
            Node n1 = nList.item(i);
            if (n1.getNodeType() == Node.ELEMENT_NODE) {
                Element e1 = (Element) n1;
                String name = n1.getNodeName();
                processDirect(e1, false);
                present = true;
                break;
            }
        }
        if (present) writer.writeFragment(".");
    }

    private void processSendArguments(Element n) throws IOException {
        writer.writeFragment("(");
        boolean first = true;
        NodeList nList = n.getChildNodes();
        for (int i = 0; i < nList.getLength(); i++) {
            Node n1 = nList.item(i);
            if (n1.getNodeType() == Node.ELEMENT_NODE) {
                Element e1 = (Element) n1;
                if (first) first = false; else writer.writeFragment(", ");
                processDirect(e1, false);
            }
        }
        writer.writeFragment(")");
    }

    private void processAssignmentExpr(Element n) throws IOException {
        boolean wasLValue = false;
        NodeList nList = n.getChildNodes();
        for (int i = 0; i < nList.getLength(); i++) {
            Node n1 = nList.item(i);
            if (n1.getNodeType() == Node.ELEMENT_NODE) {
                Element e1 = (Element) n1;
                String name = n1.getNodeName();
                if (name.equals("lvalue")) {
                    process(e1, false);
                    wasLValue = true;
                } else if (wasLValue) {
                    writer.writeFragment(" " + n.getAttribute("op") + " ");
                    processDirect(e1, false);
                    break;
                }
            }
        }
    }

    private void processBinaryExpr(Element n) throws IOException {
        writer.writeFragment("(");
        boolean wasFirst = false;
        NodeList nList = n.getChildNodes();
        for (int i = 0; i < nList.getLength(); i++) {
            Node n1 = nList.item(i);
            if (n1.getNodeType() == Node.ELEMENT_NODE) {
                Element e1 = (Element) n1;
                if (wasFirst) {
                    writer.writeFragment(" " + n.getAttribute("op") + " ");
                    processDirect(e1, false);
                    break;
                } else {
                    processDirect(e1, false);
                    wasFirst = true;
                }
            }
        }
        writer.writeFragment(")");
    }

    private void processUnaryExpr(Element n) throws IOException {
        writer.writeFragment("(");
        String op = n.getAttribute("op");
        boolean post = false;
        if (n.hasAttribute("post") && n.getAttribute("post").equals("true")) post = true;
        NodeList nList = n.getChildNodes();
        for (int i = 0; i < nList.getLength(); i++) {
            Node n1 = nList.item(i);
            if (n1.getNodeType() == Node.ELEMENT_NODE) {
                Element e1 = (Element) n1;
                if (!post) writer.writeFragment(op);
                processDirect(e1, false);
                if (post) writer.writeFragment(op);
                break;
            }
        }
        writer.writeFragment(")");
    }

    private void processIf(Element n) throws IOException {
        int step = 0;
        writer.writeFragment("if(");
        NodeList nList = n.getChildNodes();
        for (int i = 0; i < nList.getLength(); i++) {
            Node n1 = nList.item(i);
            if (n1.getNodeType() == Node.ELEMENT_NODE) {
                Element e1 = (Element) n1;
                String name = n1.getNodeName();
                if (step == 0 && name.equals("test")) {
                    process(e1, false);
                    writer.writeFragment(") ");
                    step++;
                } else if (step == 1 && name.equals("true-case")) {
                    process(e1, true);
                    step++;
                } else if (step == 2 && name.equals("false-case")) {
                    writer.writeFragment("else ");
                    process(e1, true);
                    break;
                }
            }
        }
    }

    private void processArrayRef(Element n) throws IOException {
        NodeList nList = n.getChildNodes();
        for (int i = 0; i < nList.getLength(); i++) {
            Node n1 = nList.item(i);
            if (n1.getNodeType() == Node.ELEMENT_NODE) {
                Element e1 = (Element) n1;
                String name = n1.getNodeName();
                if (name.equals("base")) {
                    process(e1, false);
                } else if (name.equals("offset")) {
                    writer.writeFragment("[");
                    process(e1, false);
                    writer.writeFragment("]");
                    break;
                }
            }
        }
    }

    private void processLoop(Element n) throws IOException {
        String kind = n.getAttribute("kind");
        Node init = null;
        Node test = null;
        Node update = null;
        Element body = null;
        NodeList nList = n.getChildNodes();
        for (int i = 0; i < nList.getLength(); i++) {
            Node n1 = nList.item(i);
            if (n1.getNodeType() == Node.ELEMENT_NODE) {
                Element e1 = (Element) n1;
                String name = n1.getNodeName();
                if (name.equals("init")) {
                    init = e1;
                } else if (name.equals("test")) {
                    test = e1;
                } else if (name.equals("update")) {
                    update = e1;
                } else {
                    body = e1;
                    break;
                }
            }
        }
        if (kind.equals("for")) {
            writer.writeFragment("for(");
            if (init != null) process(init, false);
            writer.writeFragment("; ");
            if (test != null) process(test, false);
            writer.writeFragment("; ");
            if (update != null) process(update, false);
            writer.writeFragment(") ");
            if (body != null) processDirect(body, true); else writer.writeFragment("; ");
        } else if (kind.equals("while")) {
            writer.writeFragment("while(");
            if (test != null) process(test, false);
            writer.writeFragment(") ");
            if (body != null) processDirect(body, true); else writer.writeFragment("; ");
        } else if (kind.equals("do")) {
            writer.writeFragment("do ");
            if (body != null) processDirect(body, true); else writer.writeFragment(" {} ");
            writer.writeFragment("while(");
            if (test != null) process(test, false);
            writer.writeFragment(");");
            writer.newLine();
        }
    }

    private void processConditionalExpr(Element n) throws IOException {
        int step = 0;
        NodeList nList = n.getChildNodes();
        for (int i = 0; i < nList.getLength(); i++) {
            Node n1 = nList.item(i);
            if (n1.getNodeType() == Node.ELEMENT_NODE) {
                Element e1 = (Element) n1;
                if (step == 0) {
                    processDirect(e1, false);
                    writer.writeFragment(" ? ");
                    step++;
                } else if (step == 1) {
                    processDirect(e1, false);
                    writer.writeFragment(" : ");
                    step++;
                } else if (step == 2) {
                    processDirect(e1, false);
                    break;
                }
            }
        }
    }

    private void processSwitch(Element n) throws IOException {
        writer.writeFragment("switch(");
        boolean wasExpr = false;
        NodeList nList = n.getChildNodes();
        for (int i = 0; i < nList.getLength(); i++) {
            Node n1 = nList.item(i);
            if (n1.getNodeType() == Node.ELEMENT_NODE) {
                Element e1 = (Element) n1;
                String name = n1.getNodeName();
                if (name.equals("switch-block")) {
                    processSwitchBlock(e1);
                } else if (!wasExpr) {
                    processDirect(e1, false);
                    writer.writeFragment(")");
                    writer.enterBlock();
                    wasExpr = true;
                }
            }
        }
        writer.exitBlock();
    }

    private void processSwitchBlock(Element n) throws IOException {
        writer.enterSwitchCase();
        NodeList nList = n.getChildNodes();
        for (int i = 0; i < nList.getLength(); i++) {
            Node n1 = nList.item(i);
            if (n1.getNodeType() == Node.ELEMENT_NODE) {
                Element e1 = (Element) n1;
                String name = n1.getNodeName();
                if (name.equals("case")) {
                    writer.writeFragment("case ");
                    process(e1, false);
                    writer.writeFragment(":");
                    writer.newLine();
                } else if (name.equals("default-case")) {
                    writer.writeFragment("default: ");
                    writer.newLine();
                } else {
                    processDirect(e1, true);
                }
            }
        }
        writer.exitSwitchCase();
    }

    private void processArrayInitializer(Element n) throws IOException {
        writer.writeFragment("{ ");
        boolean wasFirst = false;
        NodeList nList = n.getChildNodes();
        for (int i = 0; i < nList.getLength(); i++) {
            Node n1 = nList.item(i);
            if (n1.getNodeType() == Node.ELEMENT_NODE) {
                Element e1 = (Element) n1;
                if (wasFirst) {
                    writer.writeFragment(", ");
                } else {
                    wasFirst = true;
                }
                processDirect(e1, false);
            }
        }
        writer.writeFragment("}");
    }

    private void writeHeader() {
        writer.writeFragment("/* This file is automatically generated ");
        writer.writeFragment("by McuJavaSource application ");
        writer.writeFragment("from java source file.");
        writer.newLine();
        writer.writeFragment("Do not edit this file manually.");
        writer.writeFragment(" Edit java source file instead. ");
        writer.newLine();
        writer.newLine();
        writer.writeFragment("Target platform: " + settings.get("mcuarch"));
        writer.newLine();
        writer.writeFragment("Target device: " + settings.get("mcu"));
        writer.newLine();
        writer.writeFragment("*/");
        writer.newLine();
    }

    private void writeFile(String res) throws IOException {
        InputStream is = getClass().getResourceAsStream(res);
        if (is == null) {
            is = Thread.currentThread().getContextClassLoader().getResourceAsStream(res);
        }
        if (is == null) throw new InvalidInputDataException("Can't find resource " + res, "resource " + res);
        ReadableByteChannel input = Channels.newChannel(is);
        String fPathEnd = res;
        if (!fPathEnd.startsWith(File.separator)) fPathEnd = File.separator + fPathEnd;
        File f = new File(resultDir.getPath() + fPathEnd);
        f.getParentFile().mkdirs();
        FileChannel output = new FileOutputStream(f).getChannel();
        ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
        Charset outCharset = Charset.forName((String) settings.get("resultEncoding"));
        int bytesRead;
        while ((bytesRead = input.read(buffer)) >= 0) {
            buffer.flip();
            CharBuffer c = Charset.forName("UTF-8").decode(buffer);
            ByteBuffer buffer1 = outCharset.encode(c);
            output.write(buffer1);
            buffer.clear();
        }
        input.close();
        output.close();
    }

    public Object callPlatformMethod(String methodName, Object... args) {
        return PlatformCaller.callPlatformMethod(settings, "CSourceGenerator", methodName, "CSourceGenerator", args);
    }
}
