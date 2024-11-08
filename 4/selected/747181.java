package com.peterhi.classroom;

public class PropertyAccessException extends Exception {

    private static final long serialVersionUID = -2097779881005560748L;

    public PropertyAccessException(String property, boolean readOrWrite, Exception cause) {
        super("Cannot " + (readOrWrite ? "read" : "write") + " " + property + " " + cause);
    }
}
