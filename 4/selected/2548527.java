package it.freax.fpm.core.solver.antlr;

import it.freax.fpm.util.*;
import it.freax.fpm.util.exceptions.ConfigurationReadException;
import it.freax.fpm.util.exceptions.ExtensionDecodingException;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import javax.tools.*;
import javax.tools.JavaCompiler.CompilationTask;
import org.antlr.Tool;
import org.antlr.runtime.*;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.Tree;
import org.apache.log4j.Logger;

/**
 * @author kLeZ-hAcK
 */
public class AntlrEngine {

    private final Logger log = LogConfigurator.getOne(this.getClass()).configure(true);

    private final String grammarName;

    private final String langName;

    private final Constants consts;

    private final String antlrOutput;

    private final String antlrPlainArgs;

    private final ArrayList<String> antlrArgs;

    private final String lexerSuffix;

    private final String parserSuffix;

    private final URLClassLoader loader;

    private final File grammarsFolder;

    private final File grammar;

    private final Language lang;

    private ParserRuleReturnScope parserRetVal;

    public AntlrEngine(String grammarName, String langName) throws ConfigurationReadException, MalformedURLException, ExtensionDecodingException, FileNotFoundException {
        this.grammarName = grammarName;
        this.langName = langName;
        consts = Constants.getOne();
        antlrOutput = Strings.getOne().safeConcatPaths(consts.getDefaultFpmPath(), consts.getConstant("generated.output.directory"));
        antlrPlainArgs = consts.getConstant("antlr.cmdline.options");
        antlrArgs = new ArrayList<String>(Arrays.asList(antlrPlainArgs.split(" ")));
        lexerSuffix = consts.getConstant("lexer.suffix");
        parserSuffix = consts.getConstant("parser.suffix");
        loader = getAntlrClassLoader();
        grammarsFolder = getGrammarsFolder();
        grammar = getGrammarFile(grammarsFolder);
        lang = Language.create(grammar, langName);
        if (!grammarsFolder.exists()) {
            grammarsFolder.mkdirs();
            fillGrammarsFolder(grammarsFolder);
        }
    }

    public boolean process(String sourceFile) throws ExtensionDecodingException, IOException, ConfigurationReadException {
        boolean ret = false;
        if (grammarsFolder.exists()) {
            if (isAlreadyBuilt()) {
                ret = runParser(sourceFile);
            } else {
                generateParser(grammar);
                if (build()) {
                    ret = runParser(sourceFile);
                }
            }
        }
        return ret;
    }

    public ArrayList<String> getImports() {
        CommonTree t = (CommonTree) parserRetVal.getTree();
        ArrayList<String> ret = new ArrayList<String>();
        if (t != null) {
            for (int i = 0; i < t.getChildCount(); i++) {
                if (t.getChild(i).getType() == lang.getImportStmt()) {
                    int j = i + 1;
                    StringBuilder sb = new StringBuilder();
                    String current;
                    Tree child = t.getChild(j);
                    while ((child != null) && (child.getType() != lang.getEos())) {
                        current = child.toString();
                        sb.append(current);
                        child = t.getChild(++j);
                    }
                    ret.add(sb.toString());
                }
            }
        }
        return ret;
    }

    /**
	 * @param grammarsFolder
	 * @return
	 * @throws FileNotFoundException
	 */
    public File getGrammarFile(File grammarsFolder) throws FileNotFoundException {
        File[] grammars = grammarsFolder.listFiles(new FileNameRegexFilter(langName + ".*\\.g"));
        File grammar = grammars.length > 0 ? grammars[0] : null;
        if ((grammar == null) || !grammar.exists()) {
            String msg = "Grammar file not found. Check grammars folder.";
            throw ErrorHandler.getOne(getClass()).<FileNotFoundException>rethrow(new FileNotFoundException(msg));
        }
        return grammar;
    }

    /**
	 * @param grammar
	 */
    public void generateParser(File grammar) {
        Tool antlr;
        antlrArgs.add(grammar.getAbsolutePath());
        antlr = new Tool(antlrArgs.toArray(new String[] {}));
        antlr.process();
    }

    public boolean isAlreadyBuilt() {
        FpmCollections<String> pakDirs = FpmCollections.getOne(Constants.ENGINE_PACKAGE.split("\\."));
        pakDirs.insert(antlrOutput, 0);
        String regexPattern = "%s(%s|%s).*\\.class";
        String regex = String.format(regexPattern, lang.getLanguageName(), lexerSuffix, parserSuffix);
        String lexerSourceName = lang.getLanguageName().concat(lexerSuffix).concat(".java");
        String parserSourceName = lang.getLanguageName().concat(parserSuffix).concat(".java");
        Strings s = Strings.getOne();
        File lexerSource = new File(s.safeConcatPaths(antlrOutput, lexerSourceName));
        File parserSource = new File(s.safeConcatPaths(antlrOutput, parserSourceName));
        File classesFolder = new File(s.safeConcatPaths(pakDirs.toArray(new String[] {})));
        File[] generatedClasses = classesFolder.listFiles(new FileNameRegexFilter(regex));
        return lexerSource.exists() && parserSource.exists() && (generatedClasses.length > 0);
    }

    /**
	 * @param grammarsFolder
	 */
    private void fillGrammarsFolder(File grammarsFolder) {
        try {
            String[] grammars = Streams.getResourceListing(getClass(), Constants.GRAMMARS_DIR, ".*\\.g");
            InputStream is;
            for (String grammar : grammars) {
                Streams s = Streams.getOne(Strings.getOne().safeConcatPaths(Constants.GRAMMARS_DIR, grammar));
                is = s.getResource();
                s.dump(is, Strings.getOne().safeConcatPaths(grammarsFolder.getAbsolutePath(), grammar));
            }
        } catch (URISyntaxException e) {
            ErrorHandler.getOne(getClass()).handle("Errore in enumerazione risorse di grammatiche.", e);
        } catch (IOException e) {
            ErrorHandler.getOne(getClass()).handle("Errore in dump delle grammatiche.", e);
        }
    }

    /**
	 * @param writer
	 * @param loader
	 * @param lang
	 * @return
	 */
    public boolean runParser(String sourceFile) {
        boolean ret = true;
        try {
            String classPrefix = Constants.ENGINE_PACKAGE + lang.getLanguageName();
            Lexer lexer = (Lexer) Class.forName(classPrefix + lexerSuffix, true, loader).newInstance();
            lexer.setCharStream(new ANTLRStringStream(sourceFile));
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            Class<?> parserClass = Class.forName(classPrefix + parserSuffix, true, loader);
            Constructor<?> parserCTor = parserClass.getConstructor(TokenStream.class);
            Parser parser = (Parser) parserCTor.newInstance(tokens);
            Method entryPointMethod = parserClass.getMethod(lang.getEntryPoint());
            parserRetVal = (ParserRuleReturnScope) entryPointMethod.invoke(parser);
        } catch (SecurityException e) {
            ErrorHandler.getOne(getClass()).handle(e);
            ret = false;
        } catch (IllegalArgumentException e) {
            ErrorHandler.getOne(getClass()).handle(e);
            ret = false;
        } catch (InstantiationException e) {
            ErrorHandler.getOne(getClass()).handle(e);
            ret = false;
        } catch (IllegalAccessException e) {
            ErrorHandler.getOne(getClass()).handle(e);
            ret = false;
        } catch (ClassNotFoundException e) {
            ErrorHandler.getOne(getClass()).handle(e);
            ret = false;
        } catch (NoSuchMethodException e) {
            ErrorHandler.getOne(getClass()).handle(e);
            ret = false;
        } catch (InvocationTargetException e) {
            ErrorHandler.getOne(getClass()).handle(e);
            ret = false;
        }
        return ret;
    }

    /**
	 * @return
	 * @throws MalformedURLException
	 */
    private URLClassLoader getAntlrClassLoader() throws MalformedURLException {
        URL[] classpath = new URL[] { new URL("file://" + antlrOutput) };
        ClassLoader sysCL = ClassLoader.getSystemClassLoader();
        URLClassLoader loader = URLClassLoader.newInstance(classpath, sysCL);
        return loader;
    }

    /**
	 * @return
	 * @throws ExtensionDecodingException
	 */
    public File getGrammarsFolder() throws ExtensionDecodingException {
        String grammarsFolderPath;
        String fpmFolder = consts.getDefaultFpmPath();
        String grammars = consts.getConstant("antlr.grammars.directory");
        grammarsFolderPath = Strings.getOne().concatPaths(fpmFolder, grammars);
        return new File(grammarsFolderPath);
    }

    public boolean build() throws IOException, ConfigurationReadException {
        boolean success;
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
        try {
            JavaFileObject lexer = getFileObject(antlrOutput, grammarName, lexerSuffix);
            JavaFileObject parser = getFileObject(antlrOutput, grammarName, parserSuffix);
            Iterable<? extends JavaFileObject> compilationUnits = Arrays.asList(lexer, parser);
            String[] args = consts.getConstant("java.compiler.cmdline.options").replace(Constants.ANTLR_OUT_P, antlrOutput).split(" ");
            Iterable<String> options = Arrays.asList(args);
            CompilationTask task = compiler.getTask(null, null, diagnostics, options, null, compilationUnits);
            success = task.call();
            StringBuilder sb = new StringBuilder();
            for (Diagnostic<?> diagnostic : diagnostics.getDiagnostics()) {
                sb.delete(0, sb.length());
                sb.append("Code: ").append(diagnostic.getCode()).append("; ");
                sb.append("Kind: ").append(diagnostic.getKind()).append("; ");
                sb.append("Position: ").append(diagnostic.getPosition()).append("; ");
                sb.append("StartPosition: ").append(diagnostic.getStartPosition()).append("; ");
                sb.append("EndPosition: ").append(diagnostic.getEndPosition()).append("; ");
                sb.append("Source: ").append(diagnostic.getSource()).append("; ");
                sb.append(diagnostic.getMessage(null)).append(Constants.LS);
                log.warn(sb);
            }
        } catch (FileNotFoundException fnfe) {
            String msg = "";
            msg = String.format("There was an error in building %s: Source files not found, maybe antlr is not woring", grammarName);
            ErrorHandler.getOne(getClass()).handle(msg, fnfe);
            success = false;
        }
        log.info(String.format("Building grammar %s... %s", grammarName, (success ? "Ok" : "Ko")));
        return success;
    }

    private JavaFileObject getFileObject(String antlrOutput, String grammarName, String type) throws FileNotFoundException {
        StringWriter writer = new StringWriter();
        String className = grammarName + type;
        Scanner scanner = new Scanner(new File(antlrOutput, String.format("%s.java", className)));
        while (scanner.hasNextLine()) {
            writer.append(scanner.nextLine()).append(Constants.LS);
        }
        return new SourceObject(className, writer.toString());
    }

    /**
	 * @return the parserRetVal
	 */
    public ParserRuleReturnScope getParserRetVal() {
        return parserRetVal;
    }
}
