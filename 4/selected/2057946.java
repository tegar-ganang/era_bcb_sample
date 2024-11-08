package pl.omtt;

import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.Token;
import pl.omtt.compiler.OmttCompilationTask;
import pl.omtt.compiler.OmttCompiler;
import pl.omtt.lang.grammar.OmttLexer;
import pl.omtt.lang.grammar.OmttParser;
import pl.omtt.lang.model.PrintTreeVisitor;
import pl.omtt.util.stream.FileEnrichedStream;

public class Test {

    enum ParseComponent {

        LEXER, PARSER, CODE
    }

    ;

    static final ParseComponent testComponent = ParseComponent.CODE;

    static final String DIR = "/home/endrju/runtime-EclipseApplication/OMTT%20Example%20Project";

    static final String DIR2 = "/home/endrju/runtime-EclipseApplication/Simple%20OMTT";

    static final String FILE = DIR + "/templates/sample.omtt";

    public static void main(String[] args) {
        URI target, corejar;
        List<URI> classpath = new ArrayList<URI>();
        List<URI> sources = new ArrayList<URI>();
        try {
            sources.add(new URI("file:" + DIR + "/templates/sample.omtt"));
            target = new URI("file:" + DIR + "/bin/");
            corejar = new URI("file:../pl.omtt.core/lib/omtt-core.jar");
            classpath.add(corejar);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }
        if (testComponent == ParseComponent.LEXER) {
            printTokens(sources.get(0));
            return;
        }
        OmttCompiler compiler = new OmttCompiler();
        compiler.setEnvorionmentDirectory(DIR + "/bin/");
        OmttCompilationTask task = compiler.getTask(sources, target, classpath);
        try {
            if (testComponent == ParseComponent.PARSER) task.buildTree(); else task.compile();
        } catch (Exception e) {
            System.out.println("---\nCompilation failed.");
            e.printStackTrace();
        }
        System.out.println("---");
        new PrintTreeVisitor().run(task.getTree(sources.get(0)));
        if (testComponent == ParseComponent.CODE) {
            System.out.println("---");
            System.out.println(task.getJavaCode(sources.get(0)).getCode());
        }
    }

    private static void printTokens(URI uri) {
        Token token;
        OmttLexer lexer = getLexer(uri);
        while ((token = lexer.nextToken()) != Token.EOF_TOKEN) {
            String txt = "Token" + "(" + (lexer.brackets.empty() ? "-" : lexer.brackets.peek()) + "): " + OmttParser.tokenNames[token.getType()] + ": " + token.getText();
            if (token.getChannel() != OmttParser.HIDDEN) System.out.println(txt);
        }
    }

    private static OmttLexer getLexer(URI uri) {
        try {
            CharStream input = new FileEnrichedStream(uri);
            return new OmttLexer(input);
        } catch (FileNotFoundException e) {
            System.err.println(e);
            return null;
        }
    }
}
