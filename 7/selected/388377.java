package compiler.parsing;

import compiler.TokenType;
import compiler.tokens.Token;
import compiler.tokens.TokenException;
import static compiler.ErrorMessage.E_READ_BEYOND_BUFFER;

public class TokenBuffer {

    private LexicalAnalyzer input;

    private int depth;

    private Token[] buffer;

    private int index = 0;

    private TokenBuffer() {
    }

    public TokenBuffer(LexicalAnalyzer input, int depth) throws TokenException {
        this.input = input;
        this.depth = depth;
        buffer = new Token[depth];
        for (int i = 0; i < depth; i++) {
            buffer[i] = input.read();
        }
    }

    public TokenType LA(int i) {
        return LT(i).getType();
    }

    public Token LT(int i) {
        if ((i < 1) || (i > depth)) {
            throw new IllegalArgumentException(E_READ_BEYOND_BUFFER.getMessage() + " (index " + i + ")");
        }
        return buffer[i - 1];
    }

    public void consume() throws TokenException {
        for (int i = 0; i < (depth - 1); i++) {
            buffer[i] = buffer[i + 1];
        }
        buffer[depth - 1] = input.read();
    }
}
