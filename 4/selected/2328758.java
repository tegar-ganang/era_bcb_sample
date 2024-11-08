package org.bee.tl.core.compile;

import org.antlr.runtime.CharStream;
import org.antlr.runtime.Token;

/**
 * 出错token以及出错行
 * @author joelli
 * @since 1.1
 */
public class ErrorToken implements Token {

    String tokenName;

    int line;

    public ErrorToken() {
    }

    public ErrorToken set(String tokenName, int line) {
        this.tokenName = tokenName;
        this.line = line;
        return this;
    }

    public int getChannel() {
        throw new UnsupportedOperationException();
    }

    public int getCharPositionInLine() {
        throw new UnsupportedOperationException();
    }

    public CharStream getInputStream() {
        throw new UnsupportedOperationException();
    }

    public int getLine() {
        return line;
    }

    public String getText() {
        return tokenName;
    }

    public int getTokenIndex() {
        throw new UnsupportedOperationException();
    }

    public int getType() {
        throw new UnsupportedOperationException();
    }

    public void setChannel(int channel) {
        throw new UnsupportedOperationException();
    }

    public void setCharPositionInLine(int pos) {
        throw new UnsupportedOperationException();
    }

    public void setInputStream(CharStream input) {
        throw new UnsupportedOperationException();
    }

    public void setLine(int line) {
        this.line = line;
    }

    public void setText(String text) {
        this.tokenName = text;
    }

    public void setTokenIndex(int index) {
        throw new UnsupportedOperationException();
    }

    public void setType(int ttype) {
        throw new UnsupportedOperationException();
    }
}
