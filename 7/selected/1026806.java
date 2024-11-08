package com.jaeksoft.searchlib.analysis.synonym;

import com.jaeksoft.searchlib.util.Expression;
import com.jaeksoft.searchlib.util.ExpressionMap;

public class SynonymQueue {

    private Expression expressionKey;

    private ExpressionMap expressionMap;

    private SynonymToken[] tokens;

    private int queueSize;

    protected SynonymQueue(ExpressionMap expressionMap, int size) {
        this.expressionMap = expressionMap;
        this.tokens = new SynonymToken[size];
        this.expressionKey = new Expression(tokens);
        this.queueSize = 0;
    }

    protected final String findSynonym() {
        if (!isFull()) return null;
        return expressionMap.find(expressionKey);
    }

    protected final void clean() {
        for (int i = 0; i < tokens.length; i++) tokens[i] = null;
        queueSize = 0;
    }

    protected final void addToken(SynonymToken token) {
        int l = tokens.length - 1;
        for (int i = 0; i < l; i++) tokens[i] = tokens[i + 1];
        tokens[l] = token;
        if (queueSize < tokens.length) queueSize++;
    }

    protected final SynonymToken popToken() {
        for (int i = 0; i < tokens.length; i++) {
            SynonymToken token = tokens[i];
            if (token != null) {
                tokens[i] = null;
                queueSize--;
                return token;
            }
        }
        return null;
    }

    protected final boolean isFull() {
        return queueSize == tokens.length;
    }

    protected final int getPositionIncrement() {
        int pos = 0;
        for (SynonymToken token : tokens) if (token != null) pos += token.getPositionIncrement();
        return 0;
    }

    protected final int getStartOffset() {
        int startOffset = Integer.MAX_VALUE;
        for (SynonymToken token : tokens) {
            if (token != null) {
                int so = token.getStartOffset();
                if (so < startOffset) startOffset = so;
            }
        }
        return startOffset;
    }

    protected final int getEndOffset() {
        int endOffset = 0;
        for (SynonymToken token : tokens) {
            if (token != null) {
                int so = token.getEndOffset();
                if (so > endOffset) endOffset = so;
            }
        }
        return endOffset;
    }
}
