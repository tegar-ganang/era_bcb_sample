package org.maverickdbms.tools;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;
import org.maverickdbms.basic.Array;
import org.maverickdbms.basic.ConstantString;
import org.maverickdbms.basic.MaverickException;
import org.maverickdbms.basic.Factory;
import org.maverickdbms.basic.Program;
import org.maverickdbms.basic.Session;
import org.maverickdbms.basic.MaverickString;
import antlr.RecognitionException;
import antlr.TokenStreamException;

public class BASIC extends ClassLoader implements BasicConstants, Program {

    static final String[] BASIC_FILE_SUFFIX = { ".B", ".BAS" };

    static final String COMPILER_USAGE = "MaVerick BASIC Compiler Version {0} (build {1})\n" + "\n" + "Usage: BASIC <-d dir> <-i> <-p pckge> <-s class> <-v> sourcefile(s)\n" + "    -d write output files to the specified directory\n" + "    -i causes the compiler to only parse the input\n" + "    -p specifies the package name for the compiled code\n" + "    -s specify the superclass for the basic program\n" + "    -v outputs the compiler version\n" + "\n" + "Special thanks to the creators of the ANTLR tool.";

    private Session session;

    private int exitCode = 0;

    private void getIdentifiers(BasicAST ast, Hashtable identifiers) {
        while (ast != null) {
            if (ast.getType() == BASICTokenTypes.IDENT) {
                identifiers.put(ast.getText(), ast);
            }
            if (ast.getFirstChild() != null) {
                getIdentifiers((BasicAST) ast.getFirstChild(), identifiers);
            }
            ast = (BasicAST) ast.getNextSibling();
        }
    }

    public ConstantString run(Session session, MaverickString[] args) throws MaverickException {
        this.session = session;
        Vector files = new Vector();
        boolean parseOnly = false;
        Factory factory = session.getFactory();
        MaverickString status = session.getStatus();
        Properties orig = session.getProperties();
        Properties properties = new Properties(orig);
        int index = 0;
        while (index < args.length) {
            MaverickString arg = args[index++];
            switch(arg.charAt(0)) {
                case '-':
                    switch(arg.charAt(1)) {
                        case 'd':
                            properties.setProperty(PROP_OUTPUT_DIRECTORY, args[index++].toString());
                            break;
                        case 'i':
                            parseOnly = true;
                            break;
                        case 'p':
                            properties.setProperty(PROP_PACKAGE, args[index++].toString());
                            break;
                        case 's':
                            properties.setProperty(PROP_SUPERCLASS, args[index++].toString());
                            break;
                        case 'v':
                            String spec = getClass().getPackage().getSpecificationVersion();
                            String build = getClass().getPackage().getImplementationVersion();
                            String[] params = { spec, build };
                            session.getChannel(Session.SCREEN_CHANNEL).PRINT(factory.getConstant(MessageFormat.format(COMPILER_USAGE, params)), true, status);
                            return null;
                        default:
                            session.getChannel(Session.SCREEN_CHANNEL).PRINT(factory.getConstant("Unknown option: " + arg), true, status);
                            return null;
                    }
                    break;
                default:
                    files.addElement(arg);
            }
        }
        session.setProperties(properties);
        try {
            if (files.size() > 0) {
                Class walker = Class.forName(session.getProperty(PROP_TREE_WALKER, DEFAULT_TREE_WALKER));
                for (int i = 0; i < files.size(); i++) {
                    MaverickString arg = (MaverickString) files.elementAt(i);
                    Properties properties2 = new Properties(session.getProperties());
                    session.setProperties(properties2);
                    String directory = session.getProperty(PROP_SOURCE_DIRECTORY, DEFAULT_SOURCE_DIRECTORY);
                    java.io.File file = new java.io.File(arg.toString());
                    String parent = file.getParent();
                    if (parent != null) {
                        directory = parent;
                    }
                    SourceReader input = new SourceReader(session, this, properties2, directory);
                    input.push(arg.toString());
                    String source = input.getFilename();
                    properties2.setProperty(PROP_SOURCE_NAME, source);
                    if (!session.getProperty(PROP_CASE_FUNCTIONS, DEFAULT_CASE_FUNCTIONS)) {
                        source = source.toUpperCase();
                    }
                    for (int j = 0; j < BASIC_FILE_SUFFIX.length; j++) {
                        String suffix = BASIC_FILE_SUFFIX[j];
                        if (source.toUpperCase().endsWith(suffix)) {
                            source = source.substring(0, source.length() - suffix.length());
                            break;
                        }
                    }
                    properties2.setProperty(PROP_PROGRAM_NAME, Session.convertName(source));
                    PreprocessorReader reader = new PreprocessorReader(input);
                    reader.setProperties(session.getProperties());
                    BASICLexer lexer = new BASICLexer(reader);
                    lexer.setProperties(session.getProperties());
                    EquateFilterStream efilter = new EquateFilterStream(input, lexer);
                    BASICParser parser = new BASICParser(efilter);
                    parser.setProperties(session.getProperties());
                    parser.program();
                    BasicAST ast = (BasicAST) parser.getAST();
                    if (ast == null) {
                        throw new InternalCompilerException("Error - no output.");
                    }
                    if (parseOnly) {
                        session.getChannel(Session.SCREEN_CHANNEL).PRINT(factory.getConstant(ast.toStringList()), true, status);
                    } else {
                        if (session.getProperty(PROP_DEBUG, DEFAULT_DEBUG)) {
                            AbstractTreeWalker atw = new AbstractTreeWalker();
                            atw.setProperties(session.getProperties());
                            try {
                                atw.program(ast);
                                BasicAST old = ast;
                                ast = (BasicAST) atw.getAST();
                                if (old != null && ast != null) {
                                    compareTree(old, ast);
                                }
                            } catch (Exception e) {
                                e.printStackTrace(System.err);
                            }
                        }
                        ExpressionTreeWalker etw = new ExpressionTreeWalker();
                        etw.setProperties(session.getProperties());
                        etw.program(ast);
                        ast = (BasicAST) etw.getAST();
                        BasicTreeWalker treewalker = (BasicTreeWalker) walker.newInstance();
                        treewalker.setProperties(session.getProperties());
                        ProgramOutputStream pos = new ProgramOutputStream(this, factory, session.getProperties());
                        treewalker.setOutputStream(pos);
                        String programName = session.getProperty(PROP_PROGRAM_NAME, DEFAULT_PROGRAM_NAME);
                        boolean itypemode = session.getProperty(PROP_ITYPE_MODE, DEFAULT_ITYPE_MODE);
                        if (itypemode) {
                            Hashtable identifiers = new Hashtable();
                            getIdentifiers(ast, identifiers);
                            String[] arguments = new String[identifiers.size()];
                            Array array = arg.getArray();
                            ConstantString[] dim = { factory.getConstant(identifiers.size() + 1) };
                            array.DIM(dim);
                            int index2 = 0;
                            for (Enumeration e = identifiers.keys(); e.hasMoreElements(); ) {
                                String arg2 = (String) e.nextElement();
                                arguments[index2++] = arg2;
                                array.get(factory.getConstant(index2 + 1)).set(arg2);
                            }
                            treewalker.setArguments(arguments);
                            treewalker.compile(ast);
                            byte[] classfile = pos.toByteArray();
                            Class c = defineClass(programName, classfile, 0, classfile.length);
                            array.get(factory.getConstant(1)).setProgram((Program) c.newInstance());
                        } else {
                            treewalker.compile(ast);
                        }
                    }
                }
            } else {
                String spec = getClass().getPackage().getSpecificationVersion();
                String build = getClass().getPackage().getImplementationVersion();
                String[] params = { spec, build };
                session.getChannel(Session.SCREEN_CHANNEL).PRINT(factory.getConstant(MessageFormat.format(COMPILER_USAGE, params)), true, status);
                return null;
            }
        } catch (ClassNotFoundException cnfe) {
            session.handleError(cnfe, status);
            exitCode = -1;
        } catch (FileNotFoundException fnfe) {
            session.handleError(fnfe, status);
            exitCode = -1;
        } catch (IllegalAccessException iae) {
            session.handleError(iae, status);
            exitCode = -1;
        } catch (InstantiationException ie) {
            session.handleError(ie, status);
            exitCode = -1;
        } catch (IOException ioe) {
            session.handleError(ioe, status);
            exitCode = -1;
        } catch (InternalCompilerException ice) {
            session.handleError(ice, status);
            exitCode = -1;
        } catch (RecognitionException re) {
            session.handleError(re, status);
            exitCode = -1;
        } catch (TokenStreamException tse) {
            session.handleError(tse, status);
            exitCode = -1;
        } catch (Exception e) {
            session.handleError(e, status);
            exitCode = -1;
        } finally {
            session.setProperties(orig);
        }
        return null;
    }

    public static void main(String[] args) {
        Session session = new Session();
        BASIC compiler = new BASIC();
        MaverickString[] args2 = new MaverickString[args.length];
        for (int i = 0; i < args.length; i++) {
            args2[i] = session.getFactory().getString();
            args2[i].set(args[i]);
        }
        session.EXECUTE(null, compiler, session.getFactory().getConstant("BASIC"), args2);
        System.exit(compiler.exitCode);
    }

    private static void compareTree(BasicAST ast1, BasicAST ast2) throws MaverickException {
        if (ast1 != null) {
            if (ast2 == null) {
                throw new MaverickException(0, "ast1[" + ast1.getFile() + ", " + ast1.getLine() + ", " + ast1.getColumn() + "] = " + ast1.getType() + ", " + ast1.getText() + " ast2 = null");
            }
            if (ast1.getType() != ast2.getType()) {
                throw new MaverickException(0, "ast1[" + ast1.getType() + "] != ast2[" + ast2.getType() + "]");
            }
            if (!ast1.getText().equals(ast2.getText())) {
                throw new MaverickException(0, "text " + ast1.getText() + " != " + ast2.getText());
            }
            if (ast1.getFirstChild() != null) {
                try {
                    compareTree((BasicAST) ast1.getFirstChild(), (BasicAST) ast2.getFirstChild());
                } catch (MaverickException mve) {
                    throw new MaverickException(0, ast1.getText() + " " + mve.getMessage());
                }
            }
            if (ast1.getNextSibling() != null) {
                compareTree((BasicAST) ast1.getNextSibling(), (BasicAST) ast2.getNextSibling());
            }
        }
    }
}
