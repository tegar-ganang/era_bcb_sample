package org.owasp.orizon.core;

import java.io.IOException;
import java.io.PrintWriter;
import jline.ConsoleReader;
import jline.Terminal;

/**
 * @author thesp0nge
 *
 */
public class CommonUI {

    private Terminal terminal;

    private ConsoleReader reader;

    public CommonUI() {
        initJline();
    }

    public CommonUI(ConsoleReader reader) {
        this.reader = reader;
        this.terminal = this.reader.getTerminal();
    }

    private void initJline() {
        try {
            terminal = Terminal.setupTerminal();
            reader = new ConsoleReader(System.in, new PrintWriter(System.out));
            terminal.beforeReadLine(reader, "", (char) 0);
            terminal.enableEcho();
        } catch (Exception e) {
            e.printStackTrace();
            terminal = null;
        }
    }

    public ConsoleReader getReader() {
        return reader;
    }

    public void cleanUp() {
        if (terminal == null) return;
        Terminal.resetTerminal();
        try {
            reader.flushConsole();
            reader.getCursorBuffer().clearBuffer();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return;
    }

    private String repeat(String s, int times) {
        String ret = "";
        for (int i = 0; i < times; i++) ret = ret + s;
        return ret;
    }

    public void println(String s) {
        print(s + "\n");
        return;
    }

    public void print(String s) {
        try {
            reader.printString(s);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return;
    }

    private void __setProgress(String s, int completed, int total) {
        if (terminal == null) return;
        int w = reader.getTermwidth();
        int progress = (completed * 20) / total;
        String totalStr = String.valueOf(total);
        String percent = String.format("%0" + totalStr.length() + "d/%s [", completed, totalStr);
        String result;
        if (s == null) result = percent + repeat("=", progress) + repeat(" ", 20 - progress) + "]"; else result = s + percent + repeat("=", progress) + repeat(" ", 20 - progress) + "]";
        try {
            reader.getCursorBuffer().clearBuffer();
            reader.getCursorBuffer().write(result);
            reader.setCursorPosition(w);
            reader.redrawLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setProgress(int completed, int total) {
        __setProgress(null, completed, total);
    }

    public void setProgress(String s, int completed, int total) {
        __setProgress(s, completed, total);
    }
}
