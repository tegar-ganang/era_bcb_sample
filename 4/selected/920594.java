package pl.omtt.lang.grammar;

import org.antlr.runtime.IntStream;
import org.antlr.runtime.Parser;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.RecognizerSharedState;
import org.antlr.runtime.Token;
import org.antlr.runtime.TokenStream;
import org.antlr.runtime.tree.TreeAdaptor;
import org.antlr.runtime.tree.TreeIterator;
import pl.omtt.compiler.reporting.IAntlrProblemCollector;
import pl.omtt.lang.model.ast.CommonNode;
import pl.omtt.lang.model.ast.Program;

public abstract class AbstractOmttParser extends Parser {

    private IAntlrProblemCollector fProblems;

    private boolean fErrorsOccured;

    public AbstractOmttParser(TokenStream input) {
        super(input);
    }

    public AbstractOmttParser(TokenStream input, RecognizerSharedState state) {
        super(input, state);
    }

    public void connectProblemContainer(IAntlrProblemCollector problems) {
        fProblems = problems;
    }

    public Program parse() throws RecognitionException {
        setTreeAdaptor(new CommonNodeAdaptor());
        Program tree = (Program) program().getTree();
        tree.setTokenStream(this.getTokenStream());
        TreeIterator itor = new TreeIterator(tree);
        while (itor.hasNext()) {
            Object o = itor.next();
            if (o instanceof CommonNode) {
                CommonNode node = (CommonNode) o;
                int startIndex = removeJunk(node.getTokenStartIndex(), 1);
                int stopIndex = removeJunk(node.getTokenStopIndex(), -1);
                if (startIndex < 0 || startIndex > stopIndex) {
                    node.setTokenStart(node.token);
                    node.setTokenStop(node.token);
                } else {
                    node.setTokenStart(input.get(startIndex));
                    node.setTokenStop(input.get(stopIndex));
                }
            }
        }
        return tree;
    }

    private int removeJunk(int tokenIndex, int direction) {
        while (tokenIndex >= 0 && tokenIndex < input.size()) {
            Token token = input.get(tokenIndex);
            if (token.getChannel() != HIDDEN) switch(token.getType()) {
                case OmttLexer.ACTION_ON_NEWLINE:
                case OmttLexer.EXPRESSION_START:
                case OmttLexer.EXPRESSION_END:
                case OmttLexer.OP_DATA_IS_EXPRESSION:
                case OmttLexer.CONTENT:
                    break;
                default:
                    return tokenIndex;
            }
            tokenIndex += direction;
        }
        return tokenIndex;
    }

    @Override
    public void reportError(RecognitionException e) {
        fErrorsOccured = true;
        if (fProblems != null) fProblems.reportError(input.getSourceName(), e, getErrorMessage(e, getTokenNames())); else super.reportError(e);
    }

    protected void reportNotImplemented(String what) throws RecognitionException {
        throw new NotImplementedException(what, getTokenStream());
    }

    public abstract OmttParser.program_return program() throws RecognitionException;

    public abstract void setTreeAdaptor(TreeAdaptor adaptor);

    public boolean errorsOccured() {
        return fErrorsOccured;
    }

    public class NotImplementedException extends RecognitionException {

        String cause;

        public NotImplementedException(String msg, IntStream stream) {
            super(stream);
            this.cause = msg;
        }

        public String getMessage() {
            return cause + " not implemented yet";
        }

        private static final long serialVersionUID = 1484884878382850930L;
    }
}
