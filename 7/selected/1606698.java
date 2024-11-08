package jmathlib.toolbox.string;

import jmathlib.core.tokens.Token;
import jmathlib.core.tokens.OperandToken;
import jmathlib.core.functions.ExternalFunction;
import jmathlib.core.tokens.CharToken;
import jmathlib.core.interpreter.*;

/**An external function for changing strings into numbers */
public class sprintf extends ExternalFunction {

    String formatS = "";

    String retString = "";

    int pos = -1;

    boolean EOL = false;

    Token[] tok;

    int nTok = -1;

    /**returns a matrix of numbers 
    * @param operands[0] = string (e.g. ["hello"]) 
    * @return a matrix of numbers                                */
    public OperandToken evaluate(Token[] operands, GlobalValues globals) {
        if (getNArgIn(operands) < 2) throwMathLibException("sprintf: number of input arguments <2");
        if (!(operands[0] instanceof CharToken)) throwMathLibException("sprintf: format must be a string");
        formatS = ((CharToken) operands[0]).getValue();
        tok = new Token[operands.length - 1];
        for (int i = 0; i < (operands.length - 1); i++) tok[i] = operands[i + 1];
        while (EOL == false) {
            char c = getNextChar();
            switch(c) {
                case '%':
                    {
                        parseFormat();
                        break;
                    }
                default:
                    {
                        retString = retString + c;
                        ErrorLogger.debugLine("sprintf: " + retString);
                    }
            }
        }
        return new CharToken(retString);
    }

    private void parseFormat() {
        while (!EOL) {
            char c = getNextChar();
            switch(c) {
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    {
                        ErrorLogger.debugLine("sprintf: Feature not implemented yet");
                        break;
                    }
                case '.':
                    {
                        ErrorLogger.debugLine("sprintf: Feature not implemented yet");
                        break;
                    }
                case '+':
                    {
                        ErrorLogger.debugLine("sprintf: Feature not implemented yet");
                        break;
                    }
                case '-':
                    {
                        ErrorLogger.debugLine("sprintf: Feature not implemented yet");
                        break;
                    }
                case '#':
                    {
                        ErrorLogger.debugLine("sprintf: Feature not implemented yet");
                        break;
                    }
                case '%':
                    {
                        retString = retString + c;
                        return;
                    }
                case 'd':
                case 'i':
                case 'u':
                case 'f':
                case 'e':
                case 'E':
                case 'g':
                case 'G':
                    {
                        nTok++;
                        retString = retString + tok[nTok].toString();
                        return;
                    }
                case 's':
                    {
                        nTok++;
                        retString = retString + tok[nTok].toString();
                        return;
                    }
                default:
                    {
                        ErrorLogger.debugLine("sprintf: Feature not implemented yet");
                    }
            }
        }
    }

    private char getNextChar() {
        if (pos < (formatS.length() - 1)) {
            pos++;
            if (pos == (formatS.length() - 1)) EOL = true;
            return formatS.charAt(pos);
        }
        return ' ';
    }

    private char inspectNextChar() {
        if (pos < (formatS.length() - 2)) return formatS.charAt(pos + 1); else return ' ';
    }
}
