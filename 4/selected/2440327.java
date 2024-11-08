package codeformatter;

import exceptions.WriteStreamException;
import io.Reader;
import io.Writer;
import params.ParamsController;

/**
 * Describes common methods and fields involved in parsing one concrete symbol in the given code block
 */
public abstract class SymbolFormatter {

    private static int amount = 0;

    /**
     * constructs a new {@link SymbolFormatter} class object
     */
    public SymbolFormatter() {
        amount++;
    }

    /**
     * check, if variable symb belongs to a current realization of this abstract class
     * @param symb - symbol to be checked
     * @return true, if symb matches current {@link SymbolFormatter} realization class, false otherwise 
     */
    public abstract boolean isThisSymbol(int symb);

    /**
     * Formats the encapsulated symbol, i.e. writes this symbol and, may be, some other symbols with a help of specified {@link Writer}
     * @param reader - input code source
     * @param writer - output destination
     * @param state - current state of a {@link Formatter} class object, that calls this method
     * @throws WriteStreamException - this exception is thrown when some errors happened during the write process of {@link Writer} class object
     */
    public abstract void format(Reader reader, Writer writer, State state) throws WriteStreamException;

    /**
     * @return the amount of all existing objects that are descendants of {@link SymbolFormatter}
     */
    public static int amount() {
        return amount;
    }
}

/**
 * Encapsulates the symbol, that represents the beginning of a new code block
 */
class IndentStarterFormatter extends SymbolFormatter {

    @Override
    public boolean isThisSymbol(int symb) {
        return symb == ParamsController.getIndentStarter();
    }

    @Override
    public void format(Reader reader, Writer writer, State state) throws WriteStreamException {
        if (state.getCommentary() != ParamsController.getNoCommentary()) return;
        try {
            if (state.isIndentNeeded()) for (int i = 0; i < state.getIndentAmount() * ParamsController.getIndent(); i++) {
                writer.write(ParamsController.getIndentSymbol());
            }
            writer.write(ParamsController.getIndentStarter());
            writer.write(ParamsController.getLineFeed());
            state.incIndentAmount();
            state.setIndentNeeded(true);
        } catch (WriteStreamException e) {
            throw e;
        } finally {
        }
    }
}

/**
 * Encapsulates the symbol, that represents the end of a code block
 */
class IndentFinisherFormatter extends SymbolFormatter {

    @Override
    public boolean isThisSymbol(int symb) {
        return symb == ParamsController.getIndentFinisher();
    }

    @Override
    public void format(Reader reader, Writer writer, State state) throws WriteStreamException {
        if (state.getCommentary() != ParamsController.getNoCommentary()) return;
        try {
            for (int i = 0; i < ParamsController.getIndent() * (state.getIndentAmount() - 1); i++) writer.write(ParamsController.getIndentSymbol());
            writer.write(ParamsController.getIndentFinisher());
            writer.write(ParamsController.getLineFeed());
            state.decIndentAmount();
            state.setIndentNeeded(true);
        } catch (WriteStreamException e) {
            throw e;
        } finally {
        }
    }
}

/**
 * Encapsulates the symbol, that represents the end of a line
 */
class EndOfLineFormatter extends SymbolFormatter {

    @Override
    public boolean isThisSymbol(int symb) {
        return symb == ParamsController.getEndOfLine();
    }

    @Override
    public void format(Reader reader, Writer writer, State state) throws WriteStreamException {
        if (state.getCommentary() != ParamsController.getNoCommentary()) return;
        try {
            writer.write(ParamsController.getEndOfLine());
            writer.write(ParamsController.getLineFeed());
            state.setIndentNeeded(true);
        } catch (WriteStreamException e) {
            throw e;
        } finally {
        }
    }
}

/**
 * Encapsulates the symbol or couple of symbols, that represent line feed
 */
class LineFeedFormatter extends SymbolFormatter {

    private int symbol = 0;

    @Override
    public boolean isThisSymbol(int symb) {
        for (int i = 0; i < ParamsController.getLineFeed().length; i++) {
            if (symb == ParamsController.getLineFeed()[i]) {
                symbol = symb;
                return true;
            }
        }
        return false;
    }

    @Override
    public void format(Reader reader, Writer writer, State state) throws WriteStreamException {
        if (state.getCommentary() == ParamsController.getNoCommentary() || symbol != ParamsController.getLineFeed()[ParamsController.getLineFeed().length - 1]) return;
        try {
            writer.write(ParamsController.getLineFeed());
        } catch (WriteStreamException e) {
            throw e;
        } finally {
        }
        if (state.getCommentary() == ParamsController.getCommentary1stType()) state.setCommentary(ParamsController.getNoCommentary());
        state.setIndentNeeded(true);
    }
}

/**
 * Encapsulates the symbol, that represents space
 */
class SpaceFormatter extends SymbolFormatter {

    @Override
    public boolean isThisSymbol(int symb) {
        return symb == ParamsController.getSpace();
    }

    @Override
    public void format(Reader reader, Writer writer, State state) throws WriteStreamException {
        if (state.getCommentary() != ParamsController.getNoCommentary()) {
            writer.write(ParamsController.getSpace());
            return;
        }
        try {
            if (reader.getPrev() != ParamsController.getLineFeed()[1] && reader.getPrev() != ParamsController.getSpace()) writer.write(ParamsController.getSpace());
        } catch (WriteStreamException e) {
            throw e;
        } finally {
        }
    }
}

/**
 * Encapsulates the symbol, that represents first symbol in a commentary syntax structure
 */
class CommentaryPart1Formatter extends SymbolFormatter {

    @Override
    public boolean isThisSymbol(int symb) {
        return symb == ParamsController.getCommentaryPart1();
    }

    @Override
    public void format(Reader reader, Writer writer, State state) throws WriteStreamException {
        try {
            writer.write(ParamsController.getCommentaryPart1());
        } catch (WriteStreamException e) {
            throw e;
        } finally {
        }
        if (reader.getPrev() == ParamsController.getCommentaryPart1()) {
            if (state.getCommentary() != ParamsController.getCommentary2ndType()) {
                state.setCommentary(ParamsController.getCommentary1stType());
            }
        } else if (reader.getPrev() == ParamsController.getCommentaryPart2()) {
            if (state.getCommentary() != ParamsController.getCommentary1stType()) {
                state.setCommentary(ParamsController.getNoCommentary());
            }
        }
    }
}

/**
 * Encapsulates the symbol, that represents second part in a commentary syntax structure
 */
class CommentaryPart2Formatter extends SymbolFormatter {

    @Override
    public boolean isThisSymbol(int symb) {
        return symb == ParamsController.getCommentaryPart2();
    }

    @Override
    public void format(Reader reader, Writer writer, State state) throws WriteStreamException {
        try {
            writer.write(ParamsController.getCommentaryPart2());
        } catch (WriteStreamException e) {
            throw e;
        } finally {
        }
        if (reader.getPrev() == ParamsController.getCommentaryPart1()) {
            if (state.getCommentary() != ParamsController.getCommentary1stType()) {
                state.setCommentary(ParamsController.getCommentary2ndType());
            }
        }
    }
}

/**
 * Encapsulates the symbol, that represents any symbol.
 */
class ArbitraryFormatter extends SymbolFormatter {

    private int symbol;

    @Override
    public boolean isThisSymbol(int symb) {
        symbol = symb;
        return true;
    }

    @Override
    public void format(Reader reader, Writer writer, State state) throws WriteStreamException {
        try {
            if (state.getCommentary() != ParamsController.getNoCommentary()) {
                writer.write(symbol);
                return;
            }
            if (state.isIndentNeeded()) for (int i = 0; i < ParamsController.getIndent() * state.getIndentAmount(); i++) {
                writer.write(ParamsController.getIndentSymbol());
            }
            writer.write(symbol);
            state.setIndentNeeded(false);
        } catch (WriteStreamException e) {
            throw e;
        } finally {
        }
    }
}
