package com.jaeksoft.searchlib.analysis.shingle;

public class ShingleQueue {

    private ShingleToken[] tokens;

    private int currentSize;

    private String tokenSeparator;

    public ShingleQueue(String tokenSeparator, int size) {
        this.tokenSeparator = tokenSeparator;
        tokens = new ShingleToken[size];
        currentSize = 0;
    }

    public final boolean isFull() {
        return currentSize == tokens.length;
    }

    public final void addToken(ShingleToken shingleToken) {
        tokens[currentSize++] = shingleToken;
    }

    public final String getTerm() {
        StringBuffer sb = new StringBuffer(tokens[0].getTerm());
        for (int i = 1; i < currentSize; i++) {
            sb.append(tokenSeparator);
            sb.append(tokens[i].getTerm());
        }
        return sb.toString();
    }

    protected final int getPositionIncrement() {
        int pos = 0;
        for (int i = 0; i < currentSize; i++) pos += tokens[i].getPositionIncrement();
        return pos;
    }

    protected final int getStartOffset() {
        int startOffset = Integer.MAX_VALUE;
        for (int i = 0; i < currentSize; i++) {
            int so = tokens[i].getStartOffset();
            if (so < startOffset) startOffset = so;
        }
        return startOffset;
    }

    protected final int getEndOffset() {
        int endOffset = 0;
        for (int i = 0; i < currentSize; i++) {
            int so = tokens[i].getEndOffset();
            if (so > endOffset) endOffset = so;
        }
        return endOffset;
    }

    protected final void pop() {
        currentSize--;
        for (int i = 0; i < currentSize; i++) tokens[i] = tokens[i + 1];
        tokens[currentSize] = null;
    }
}
