package fortran.ofp.parser.java;

import java.util.*;
import java.io.FileOutputStream;
import org.antlr.runtime.*;
import fortran.ofp.parser.java.FortranToken;

public class FortranTokenStream extends LegacyCommonTokenStream {

    public FortranLexer lexer;

    public int needIdent;

    public int parserBacktracking;

    public boolean matchFailed;

    private List currLine;

    private int lineLength;

    private Token eofToken = null;

    private ArrayList<Token> packedList;

    private ArrayList<Token> newTokenList;

    public FortranTokenStream(FortranLexer lexer) {
        super(lexer);
        this.lexer = lexer;
        this.needIdent = 0;
        this.parserBacktracking = 0;
        this.matchFailed = false;
        this.currLine = null;
        this.lineLength = 0;
        this.packedList = null;
        this.newTokenList = new ArrayList<Token>();
        this.fillBuffer();
        eofToken = tokenSource.nextToken();
        eofToken.setTokenIndex(size());
        FortranStream fs = ((FortranLexer) lexer).getInput();
        eofToken.setText(fs.getFileName() + ":" + fs.getAbsolutePath());
    }

    /**
    * For some reason antlr v3.3 LA/LT() no longer returns <EOF> token,
    * so save it last token from source (EOF) and return it in LT method.
    */
    public Token LT(int k) {
        if (index() + k - 1 >= this.size()) {
            return eofToken;
        }
        return super.LT(k);
    }

    /**
    * Create a subset list of the non-whitespace tokens in the current line.
    */
    private ArrayList<Token> createPackedList() {
        int i = 0;
        Token tk = null;
        ArrayList<Token> pList = new ArrayList<Token>(this.lineLength + 1);
        for (i = 0; i < currLine.size(); i++) {
            tk = getTokenFromCurrLine(i);
            try {
                if (tk.getChannel() != lexer.getIgnoreChannelNumber()) {
                    pList.add(tk);
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
        if (pList.get(pList.size() - 1).getType() != FortranLexer.T_EOS) {
            FortranToken eos = new FortranToken(lexer.getInput(), FortranLexer.T_EOS, Token.DEFAULT_CHANNEL, lexer.getInput().index(), lexer.getInput().index() + 1);
            eos.setText("\n");
            packedList.add(eos);
        }
        return pList;
    }

    public String lineToString(int lineStart, int lineEnd) {
        int i = 0;
        StringBuffer lineText = new StringBuffer();
        for (i = lineStart; i < packedList.size() - 1; i++) {
            lineText.append(packedList.get(i).getText());
        }
        return lineText.toString();
    }

    public List getTokens(int start, int stop) {
        return super.getTokens(start, stop);
    }

    public int getCurrLineLength() {
        return this.packedList.size();
    }

    public int getRawLineLength() {
        return this.currLine.size();
    }

    public int getLineLength(int start) {
        int lineLength;
        Token token;
        lineLength = 0;
        if (start >= super.tokens.size()) return lineLength;
        do {
            token = super.get(start + lineLength);
            lineLength++;
        } while ((start + lineLength) < super.tokens.size() && (token.getChannel() == lexer.getIgnoreChannelNumber() || token.getType() != FortranLexer.T_EOS && token.getType() != FortranLexer.EOF));
        return lineLength;
    }

    public int findTokenInPackedList(int start, int desiredToken) {
        Token tk;
        if (start >= this.packedList.size()) {
            return -1;
        }
        do {
            tk = (Token) (packedList.get(start));
            start++;
        } while (start < this.packedList.size() && tk.getType() != desiredToken);
        if (tk.getType() == desiredToken) return start - 1;
        return -1;
    }

    public Token getToken(int pos) {
        if (pos >= this.packedList.size() || pos < 0) {
            System.out.println("pos is out of range!");
            System.out.println("pos: " + pos + " packedListSize: " + this.packedList.size());
            return null;
        } else return (Token) (packedList.get(pos));
    }

    public Token getToken(int start, int desiredToken) {
        int index;
        index = findToken(start, desiredToken);
        if (index != -1) return (Token) (packedList.get(index)); else return null;
    }

    public int findToken(int start, int desiredToken) {
        Token tk;
        if (start >= this.packedList.size()) {
            System.out.println("start is out of range!");
            System.out.println("start: " + start + " packedListSize: " + this.packedList.size());
            return -1;
        }
        do {
            tk = (Token) (packedList.get(start));
            start++;
        } while (start < this.packedList.size() && tk.getType() != desiredToken);
        if (tk.getType() == desiredToken) return start - 1;
        return -1;
    }

    /**
    * Search the currLine list for the desired token.
    */
    public int findTokenInCurrLine(int start, int desiredToken) {
        int size;
        Token tk;
        size = currLine.size();
        if (start >= size) return -1;
        do {
            tk = (Token) (currLine.get(start));
            start++;
        } while (start < size && tk.getType() != desiredToken);
        if (tk.getType() == desiredToken) return start;
        return -1;
    }

    /**
    * @param pos Current location in the currLine list; the search 
    * will begin by looking at the next token (pos+1).
    */
    public Token getNextNonWSToken(int pos) {
        Token tk;
        tk = (Token) (packedList.get(pos + 1));
        return tk;
    }

    /**
    * @param pos Current location in the currLine list; the search 
    * will begin by looking at the next token (pos+1).
    */
    public int getNextNonWSTokenPos(int pos) {
        Token tk;
        tk = getNextNonWSToken(pos);
        pos = findTokenInCurrLine(pos, tk.getType());
        return pos;
    }

    public Token getTokenFromCurrLine(int pos) {
        if (pos >= currLine.size() || pos < 0) {
            return null;
        } else {
            return ((Token) (currLine.get(pos)));
        }
    }

    public void setCurrLine(int lineStart) {
        this.lineLength = this.getLineLength(lineStart);
        currLine = this.getTokens(lineStart, (lineStart + this.lineLength) - 1);
        if (currLine == null) {
            System.err.println("currLine is null!!!!");
            System.exit(1);
        }
        this.packedList = createPackedList();
    }

    /**
    * This will use the super classes methods to keep track of the 
    * start and end of the original line, not the line buffered by
    * this class.
    */
    public int findTokenInSuper(int lineStart, int desiredToken) {
        int lookAhead = 0;
        int tk, channel;
        do {
            lookAhead++;
            Token token = LT(lookAhead);
            tk = token.getType();
            channel = token.getChannel();
        } while ((tk != FortranLexer.EOF && tk != FortranLexer.T_EOS && tk != desiredToken) || channel == lexer.getIgnoreChannelNumber());
        if (tk == desiredToken) {
            return lookAhead;
        }
        return -1;
    }

    public void printCurrLine() {
        System.out.println("=================================");
        System.out.println("currLine.size() is: " + currLine.size());
        System.out.println(currLine.toString());
        System.out.println("=================================");
        return;
    }

    public void printPackedList() {
        System.out.println("*********************************");
        System.out.println("packedListSize is: " + this.packedList.size());
        System.out.println(this.packedList.toString());
        System.out.println("*********************************");
        return;
    }

    public void outputTokenList(IFortranParserAction actions) {
        List tmpList = null;
        tmpList = super.getTokens();
        for (int i = 0; i < tmpList.size(); i++) {
            Token tk = (Token) tmpList.get(i);
            actions.next_token(tk);
        }
    }

    public void outputTokenList(String filename) {
        FileOutputStream fos = null;
        List tmpList = null;
        tmpList = super.getTokens();
        try {
            fos = new FileOutputStream(filename);
        } catch (Exception e) {
            System.out.println("ERROR: couldn't open tokenfile " + filename);
            e.printStackTrace();
            System.exit(1);
        }
        for (int i = 0; i < tmpList.size(); i++) {
            Token tk = (Token) tmpList.get(i);
            try {
                fos.write(tk.toString().getBytes());
                fos.write('\n');
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
    }

    public int currLineLA(int lookAhead) {
        Token tk = null;
        try {
            tk = (Token) (packedList.get(lookAhead - 1));
        } catch (Exception e) {
            return -1;
        }
        return tk.getType();
    }

    public boolean lookForToken(int desiredToken) {
        int lookAhead = 1;
        int tk;
        do {
            tk = this.LA(lookAhead);
            lookAhead++;
        } while (tk != FortranLexer.T_EOS && tk != FortranLexer.EOF && tk != desiredToken);
        if (tk == desiredToken) {
            return true;
        } else {
            return false;
        }
    }

    public boolean appendToken(int tokenType, String tokenText) {
        FortranToken newToken = new FortranToken(tokenType);
        newToken.setText(tokenText);
        return this.packedList.add(newToken);
    }

    public void addToken(Token token) {
        this.packedList.add(token);
    }

    public void addToken(int index, int tokenType, String tokenText) {
        try {
            this.packedList.add(index, new FortranToken(tokenType, tokenText));
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        return;
    }

    public void set(int index, Token token) {
        packedList.set(index, token);
    }

    public void add(int index, Token token) {
        packedList.add(index, token);
    }

    public void removeToken(int index) {
        packedList.remove(index);
        return;
    }

    public void clearTokensList() {
        this.packedList.clear();
        return;
    }

    public ArrayList<Token> getTokensList() {
        return this.packedList;
    }

    public void setTokensList(ArrayList<Token> newList) {
        this.packedList = newList;
        return;
    }

    public int getTokensListSize() {
        return this.packedList.size();
    }

    public FortranToken createToken(int type, String text, int line, int col) {
        FortranToken token = new FortranToken(type, text);
        token.setLine(line);
        token.setCharPositionInLine(col);
        return token;
    }

    public void addTokenToNewList(Token token) {
        if (this.newTokenList.add(token) == false) {
            System.err.println("Couldn't add to newTokenList!");
        }
        return;
    }

    public void finalizeLine() {
        if (this.newTokenList.addAll(packedList) == false) {
            System.err.println("Couldn't add to newTokenList!");
        }
    }

    public void finalizeTokenStream() {
        super.tokens = this.newTokenList;
    }
}
