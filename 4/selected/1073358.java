package v2k.parser;

import static v2k.util.Utils.abnormalExit;
import static v2k.util.Utils.getPropertyAsBool;
import v2k.message.MessageMgr;
import antlr.*;
import java.io.*;

/**
 *
 * @author karl
 */
public class Lexer implements TokenStream {

    private static final int stBufSz = Parser.stBufSz;

    public Lexer(String fname) {
        m_lexer = new VLexer(fname);
    }

    private static final boolean stDBG1 = getPropertyAsBool("DBG1");

    private static final boolean stDBG2 = getPropertyAsBool("DBG2");

    public Token nextToken() throws TokenStreamException {
        Token tok = m_lexer.nextToken();
        Message.message(stDBG2, 'I', "DBG-2", tok.getText());
        return tok;
    }

    public String getFilename() {
        return m_lexer.getFilename();
    }

    public int getLine() {
        return m_lexer.getLine();
    }

    /**The post processed lexer for use by parser.*/
    private VLexer m_lexer;

    private PipedReader m_reader = new PipedReader();

    private class VLexer extends VlogLexer {

        VLexer(String fname) {
            super(m_reader);
            try {
                m_writer = new PipedWriter();
                m_reader.connect(m_writer);
                m_lexerpp = new Lexerpp(fname, m_writer);
                setFilename(fname);
                m_lexerpp.start();
            } catch (Exception ex) {
                abnormalExit(ex);
            }
        }

        private Lexerpp m_lexerpp;

        private PipedWriter m_writer;
    }

    /**
     * Preprocessor acting as writer side of Pipe.
     */
    private static class Lexerpp extends Thread {

        Lexerpp(String fname, Writer writer) throws IOException {
            InputStream fis = new BufferedInputStream(new FileInputStream(fname), stBufSz);
            m_lexer = new Helper(fis, fname);
            m_writer = writer;
            if (Parser.stDumpPP) {
                String fnm = new File(fname).getPath() + ".E";
                try {
                    m_outf = new FileWriter(fnm);
                    MessageMgr.message('I', "VPP-1", fnm);
                } catch (Exception ex) {
                    m_outf = null;
                    MessageMgr.message('W', "VPP-2", fnm);
                }
            }
        }

        @Override
        public void run() {
            BufferedWriter bw = new BufferedWriter(m_writer);
            try {
                Token tok;
                int type;
                while (true) {
                    tok = m_lexer.nextToken();
                    type = tok.getType();
                    String s;
                    if (VlogppLexer.EOF == type) {
                        break;
                    }
                    s = tok.getText();
                    m_writer.append(s);
                    if (null != m_outf) {
                        m_outf.write(s);
                    }
                }
            } catch (TokenStreamException ex) {
            } catch (Exception ex) {
                abnormalExit(ex);
            } finally {
                try {
                    if (null != m_outf) {
                        m_outf.close();
                    }
                    bw.close();
                } catch (Exception ex) {
                    abnormalExit(ex);
                }
            }
        }

        /**Output preprocessed file.*/
        private Writer m_outf;

        private Writer m_writer;

        private Helper m_lexer;

        private class Helper implements TokenStream, Preproc.IPreproc {

            private Helper(InputStream fis) {
                m_lexer = new VlogppLexer(fis);
                m_pp = Preproc.getTheOne();
                m_pp.setPreproc(this);
            }

            Helper(InputStream fis, String fname) {
                this(fis);
                m_lexer.setFilename(fname);
            }

            private static final int eIncl = 1;

            private static final int ePostIncl = 2;

            private void addTicLine(String fname, int lnum, int type) {
                StringBuffer s = new StringBuffer("`line ").append(lnum).append(" ").append('"').append(fname).append("\" ").append(type).append("\n");
                try {
                    m_writer.append(s);
                    if (null != m_outf) {
                        m_outf.append(s);
                    }
                } catch (Exception ex) {
                    abnormalExit(ex);
                }
            }

            private void addTicLine(LexerSharedInputState is, int type) {
                addTicLine(is.getFilename(), is.getLine(), type);
            }

            public void pop() throws TokenStreamRetryException {
                if (m_pp.getStack().empty()) return;
                Preproc.LexState lis = m_pp.getStack().pop();
                m_lexer.setInputState(lis.m_state);
                if (lis.m_doLine) {
                    addTicLine(lis.m_state, ePostIncl);
                }
                throw new TokenStreamRetryException();
            }

            private void retry(VlogppLexer newLexer, boolean addLine) throws TokenStreamRetryException {
                LexerSharedInputState sst = m_lexer.getInputState();
                Preproc.LexState lis = new Preproc.LexState(sst, addLine);
                m_pp.getStack().push(lis);
                sst = newLexer.getInputState();
                m_lexer.setInputState(sst);
                if (addLine) {
                    addTicLine(sst, eIncl);
                }
                throw new TokenStreamRetryException();
            }

            public void push(File f) throws TokenStreamRetryException {
                BufferedInputStream bis = null;
                VlogppLexer lxr = null;
                try {
                    bis = new BufferedInputStream(new FileInputStream(f), stBufSz);
                    String fn = Parser.stUseAbsPaths ? f.getAbsolutePath() : f.getPath();
                    lxr = new VlogppLexer(bis);
                    lxr.setFilename(fn);
                    Message.message(getLocation(), 'I', "INCL-1", fn);
                } catch (Exception ex) {
                }
                if (null != lxr) retry(lxr, true);
            }

            public void push(String txt) throws TokenStreamRetryException {
                StringReader rdr = new StringReader(txt);
                VlogppLexer lxr = new VlogppLexer(rdr);
                LexerSharedInputState ls = m_lexer.getInputState();
                lxr.setFilename(ls.getFilename());
                lxr.setLine(ls.getTokenStartLine());
                lxr.setColumn(ls.getTokenStartColumn());
                retry(lxr, false);
            }

            public Token nextToken() throws TokenStreamException {
                Token tok;
                String text;
                boolean passToken;
                while (true) {
                    try {
                        tok = m_lexer.nextToken();
                        text = tok.getText();
                        passToken = passToken();
                        if (passToken) {
                            Message.message(stDBG1, getLocation(), 'I', "DBG-1", text);
                            return tok;
                        } else {
                            boolean emit = false;
                            switch(tok.getType()) {
                                case VlogppLexerTokenTypes.COMMENT:
                                case VlogppLexerTokenTypes.ML_COMMENT:
                                case VlogppLexerTokenTypes.SL_COMMENT:
                                    emit = true;
                                    break;
                                default:
                                    emit = text.equals("\n");
                            }
                            if (emit) {
                                return tok;
                            }
                        }
                    } catch (TokenStreamRetryException r) {
                    }
                }
            }

            public Location getLocation() {
                String fname = m_lexer.getFilename();
                int lnum = m_lexer.getLine();
                Location loc = new Location(fname, lnum);
                return loc;
            }

            /**Return true to parser if can pass token.
             * This way we can block tokens while processing ifdef blocks.
             */
            private boolean passToken() {
                return m_pp.passToken();
            }

            private VlogppLexer m_lexer;

            private Preproc m_pp;
        }
    }

    public static void main(String argv[]) {
        for (String s : argv) {
            Lexer lexer = new Lexer(s);
            Token tok;
            int type;
            try {
                while (true) {
                    tok = lexer.nextToken();
                    type = tok.getType();
                    if (VlogLexer.EOF == type) {
                        break;
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
