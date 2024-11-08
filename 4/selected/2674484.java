package edu.ufpa.ppgcc.visualpseudo.semantic.evaluate;

import java.util.Stack;
import edu.ufpa.ppgcc.visualpseudo.exceptions.BreakException;
import edu.ufpa.ppgcc.visualpseudo.exceptions.GrammarException;
import edu.ufpa.ppgcc.visualpseudo.exceptions.ReturnException;
import edu.ufpa.ppgcc.visualpseudo.exceptions.SemanticException;
import edu.ufpa.ppgcc.visualpseudo.exceptions.SyntaticException;
import edu.ufpa.ppgcc.visualpseudo.types.TokenType;

public class CommandEval extends BaseEval {

    private BaseEval read = null;

    private BaseEval write = null;

    private BaseEval assigment = null;

    private BaseEval ifEval = null;

    private BaseEval forEval = null;

    private BaseEval whileEval = null;

    private BaseEval repeatEval = null;

    private BaseEval switchEval = null;

    public CommandEval(IEvaluable execute) {
        super(execute);
        this.read = new ReadEval(this);
        this.write = new WriteEval(this);
        this.assigment = new AssigmentEval(this);
        this.ifEval = new IfEval(this);
        this.forEval = new ForEval(this);
        this.whileEval = new WhileEval(this);
        this.repeatEval = new RepeatEval(this);
        this.switchEval = new SwitchEval(this);
    }

    public boolean S(int[] i, int[] o) throws GrammarException, SemanticException, BreakException, ReturnException {
        return commands(i, o);
    }

    protected boolean commands(int i[], int o[]) throws GrammarException, SemanticException, BreakException, ReturnException {
        int i1[] = { i[0] }, i2[] = { 0 };
        return segment(i, i1) && x(",", i1, i2) && commands(i2, o) || attr(o, i1[0]);
    }

    private boolean segment(int i[], int o[]) throws GrammarException, SemanticException, BreakException, ReturnException {
        int i1[] = { 0 }, i2[] = { 0 }, i3[] = { 0 };
        Object v1[] = { 0 };
        Stack params = new Stack();
        try {
            return x("bk()", i, o) && raiseBreak(i) || x("rt()", i, o) && raiseReturn(i, new Object[] { null }) || x("rt(", i, i1) && eval(v1, i1, i2) && x(")", i2, o) && raiseReturn(i, v1) || read.S(i, o) || write.S(i, o) || assigment.S(i, o) || ifEval.S(i, o) || forEval.S(i, o) || whileEval.S(i, o) || repeatEval.S(i, o) || switchEval.S(i, o) || x(TokenType.IDENTIFIER, i, i1) && x("(", i1, i2) && interv(params, i2, i3) & x(")", i3, o) && run(v1, peekToken(i), params) || x(TokenType.IDENTIFIER, i, o) && run(v1, peekToken(i), params) || !attr(o, i[0]);
        } catch (SyntaticException e) {
            throw new SemanticException(e, peekToken(i));
        }
    }

    private boolean interv(Stack v, int i[], int o[]) throws GrammarException, SemanticException {
        int i1[] = { 0 }, i2[] = { 0 };
        Object v1[] = { 0 };
        return eval(v1, i, i1) && x(",", i1, i2) && interv(v, i2, o) && v.push(v1[0]) != null || eval(v1, i, o) && v.push(v1[0]) != null;
    }

    private boolean raiseBreak(int i[]) throws GrammarException, BreakException {
        if (getLevel() == EXECUTION || getProcessReturnOrBreak()) throw new BreakException(peekToken(i)); else return true;
    }

    private boolean raiseReturn(int i[], Object v[]) throws GrammarException, ReturnException {
        if (getLevel() == EXECUTION || getProcessReturnOrBreak()) throw new ReturnException(v, peekToken(i)); else return true;
    }
}
