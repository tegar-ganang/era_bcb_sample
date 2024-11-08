package wyq.tool.gui;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import javax.swing.text.JTextComponent;

public class PrintStreamWrapper extends PrintStream {

    public PrintStreamWrapper(JTextComponent txtField) {
        super(getOutputStream(txtField));
    }

    static WrapperOutputStream getOutputStream(JTextComponent txtField) {
        return new WrapperOutputStream(txtField);
    }

    static class WrapperOutputStream extends OutputStream {

        public WrapperOutputStream(JTextComponent textPane) {
            this.textPane = textPane;
        }

        private StringBuilder sb = new StringBuilder();

        private String endLineMark = System.getProperty("line.separator");

        private int endLineMarkLength = endLineMark.length();

        private char[] buff = new char[endLineMarkLength];

        private JTextComponent textPane;

        @Override
        public void write(int b) throws IOException {
            sb.append((char) b);
            for (int i = 0; i < buff.length - 1; i++) {
                buff[i] = buff[i + 1];
            }
            buff[buff.length - 1] = (char) b;
            String tmp = String.valueOf(buff);
            if (tmp.equals(endLineMark)) {
                textPane.setText(sb.toString());
            }
        }
    }
}
