package codeformatter;

import io.Reader;
import io.Writer;
import java.io.InputStream;
import java.io.OutputStream;
import exceptions.ReadStreamException;
import exceptions.WriteStreamException;
import exceptions.WrongBracketAmountException;

/**
 * Class for formatting code blocks. Code block should be given as class, realizing {@link InputStream} interface.
 * Parameters for formatting are taken from {@link params.ParamsController} class.
 */
public class Formatter {

    State state;

    SymbolFormatter[] symbols = null;

    /**
     * Constructs a new Formatter object
     */
    public Formatter() {
        IndentStarterFormatter s1 = new IndentStarterFormatter();
        IndentFinisherFormatter s2 = new IndentFinisherFormatter();
        EndOfLineFormatter s3 = new EndOfLineFormatter();
        LineFeedFormatter s4 = new LineFeedFormatter();
        SpaceFormatter s5 = new SpaceFormatter();
        CommentaryPart1Formatter s6 = new CommentaryPart1Formatter();
        CommentaryPart2Formatter s7 = new CommentaryPart2Formatter();
        ArbitraryFormatter s8 = new ArbitraryFormatter();
        symbols = new SymbolFormatter[SymbolFormatter.amount()];
        symbols[0] = s1;
        symbols[1] = s2;
        symbols[2] = s3;
        symbols[3] = s4;
        symbols[4] = s5;
        symbols[5] = s6;
        symbols[6] = s7;
        symbols[7] = s8;
        state = new State();
    }

    /**
     * Formats the given code block
     * 
     * @param is - input data stream which represents the code to be formatted. Function finishes it's work when the stream ends. 
     * @param os - output resource in which the resulting formatted code is printed to.
     * @throws ReadStreamException - indicates an error occurred in the input data stream
     * @throws WriteStreamException - indicates an error occurred in the output data stream
     * @throws WrongBracketAmountException - not all opened brackets were closed or there is too much closing brackets
     */
    public void format(InputStream is, OutputStream os) throws ReadStreamException, WriteStreamException, WrongBracketAmountException {
        int symbol = 0;
        Reader reader = new Reader(is);
        Writer writer = new Writer(os);
        try {
            while ((symbol = reader.readNext()) != -1) {
                for (int i = 0; i < SymbolFormatter.amount(); i++) {
                    if (symbols[i].isThisSymbol(symbol)) {
                        symbols[i].format(reader, writer, state);
                        break;
                    }
                }
            }
            if (state.getIndentAmount() != 0) throw new WrongBracketAmountException(state.getIndentAmount() + " brackets not closed");
        } catch (ReadStreamException e) {
            throw e;
        } catch (WriteStreamException e) {
            throw e;
        }
    }
}
