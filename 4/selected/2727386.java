package no.ntnu.xqft.parse;

import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.Token;
import org.antlr.runtime.TokenSource;

public class UnbufferedCommonTokenStream extends CommonTokenStream {

    private int tokenIndex;

    public UnbufferedCommonTokenStream() {
        super();
        tokenIndex = 0;
        p = 0;
    }

    public UnbufferedCommonTokenStream(TokenSource tokenSource) {
        super(tokenSource);
        tokenIndex = 0;
        p = 0;
    }

    public UnbufferedCommonTokenStream(TokenSource tokenSource, int channel) {
        super(tokenSource, channel);
        tokenIndex = 0;
        p = 0;
    }

    /** Reset this token stream by setting its token source. */
    public void setTokenSource(TokenSource tokenSource) {
        this.tokenSource = tokenSource;
        tokens.clear();
        p = 0;
        channel = Token.DEFAULT_CHANNEL;
    }

    /**Method to check if there is enough valid (i.e. in the right channel) 
     * tokens in the buffer to look k tokens ahead
     * @param k the number of tokens to look ahead
     * @return true if there is enough tokens in the buffer, false if not.
     */
    protected boolean enoughValidLH(int k) {
        int i = p;
        int n = tokens.size();
        int no = 0;
        while (i < n) {
            if (((Token) tokens.get(i)).getChannel() == channel) if (++no == k) return true;
            i++;
        }
        return false;
    }

    /** Get the ith token from the current position 1..n where k=1 is the
     *  first symbol of lookahead.
     */
    public Token LT(int k) {
        if (k == 0) {
            return null;
        }
        if (k < 0) {
            return LB(-k);
        }
        if (!enoughValidLH(k)) {
            fillBuffer(k);
        }
        if ((p + k - 1) >= tokens.size()) {
            return Token.EOF_TOKEN;
        }
        int i = p;
        int n = 1;
        while (n < k) {
            i = skipOffTokenChannels(i + 1);
            n++;
        }
        if (i >= tokens.size()) {
            return Token.EOF_TOKEN;
        }
        return (Token) tokens.get(i);
    }

    /**Cannot be used 
     * Load all tokens from the token source and put in tokens.
     *  This is done upon first LT request because you might want to
     *  set some token type / channel overrides before filling buffer.
     */
    protected void fillBuffer() {
        System.err.println("SOMETHING IS WRONG -> SHOULDN'T USE THIS METHOD IN UnbufferedCommonTokenStream");
    }

    /** Load the number of tokens needed for the look-ahead.
     *  This is done instead of buffering all tokens, so that the parser
     *  may be able to tell the lexer that it is about to parse a ElementContent 
     *  or AttributeContent etc.
     * @param k the amount of look-ahead needed
     */
    protected void fillBuffer(int k) {
        int no = 0;
        Token t = tokenSource.nextToken();
        while (t != null && t.getType() != CharStream.EOF) {
            boolean discard = false;
            if (channelOverrideMap != null) {
                Integer channelI = (Integer) channelOverrideMap.get(new Integer(t.getType()));
                if (channelI != null) {
                    t.setChannel(channelI.intValue());
                }
            }
            if (discardSet != null && discardSet.contains(new Integer(t.getType()))) {
                discard = true;
            } else if (discardOffChannelTokens && t.getChannel() != this.channel) {
                discard = true;
            }
            if (!discard) {
                t.setTokenIndex(tokenIndex);
                tokens.add(t);
                tokenIndex++;
                if (t.getChannel() == channel) {
                    if (++no == k) {
                        p = skipOffTokenChannels(p);
                        break;
                    }
                }
            }
            t = tokenSource.nextToken();
        }
    }
}
