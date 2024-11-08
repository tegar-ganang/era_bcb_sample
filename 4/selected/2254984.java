package org.openiaml.iacleaner;

import java.io.IOException;
import org.openiaml.iacleaner.inline.InlineStringReader;
import org.openiaml.iacleaner.inline.InlineStringWriter;

/**
 * Handles the cleaning of PHP content.
 * 
 * @author Jevon
 *
 */
public class InlinePhpCleaner extends InlineCSyntaxCleaner {

    public InlinePhpCleaner(IAInlineCleaner inline) {
        super(inline);
    }

    /**
	 * Even though we've been told we need whitespace before this character,
	 * do we actually need it?
	 * @param writer 
	 * @param reader 
	 * 
	 * @param cur current character
	 * @return
	 * @throws IOException 
	 */
    private boolean doesntActuallyNeedWhitespaceBeforePhp(InlineStringReader reader, InlineStringWriter writer, int cur) throws IOException {
        return cur == '(' || cur == ')' || cur == '}' || cur == ';' || cur == '[' || cur == ']' || cur == ',' || cur == '+' || cur == '-' || writer.getPrevious() == '-' || writer.getPrevious() == '+' || isTwoCharacterOperator(cur, reader.readAhead()) || (writer.getLastWritten(2).equals("::")) || (writer.getLastWritten(2).equals("->"));
    }

    /**
	 * Do we need to add one piece of whitespace (' ') between characters
	 * 'a' and 'b' in PHP mode?
	 * 
	 * @param a previous character
	 * @param b current character
	 * @return
	 * @throws IOException 
	 */
    public boolean needsWhitespaceBetweenPhp(InlineStringReader reader, InlineStringWriter writer, int a, int b) throws IOException {
        return (a == ')' && b == '{') || (a == ',') || (!isOperator(a) && b == '=') || (a == '=' && (b != '>' && b != '=')) || (a == '.' && b != '=') || (b == '.') || (b == '?') || (a == '?') || (b == '{') || (a != '(' && a != '!' && b == '!') || (a != '+' && b == '+' && reader.readAhead() != '+') || (a == '+' && b == '+' && reader.readAhead() == '+') || (a != '-' && b == '-' && reader.readAhead() != '-' && reader.readAhead() != '>') || (a == '-' && b == '-' && reader.readAhead() == '-') || (a != '|' && b == '|') || (a != '&' && b == '&') || (b == '<' || (a != '-' && a != '=' && b == '>')) || (isOperator(a) && !isOperator(b) && b != ')' && a != '!' && b != ';' && b != '$' && !writer.getLastWritten(2).equals("->") && !writer.getLastWritten(3).equals(", -") && !writer.getLastWritten(3).equals(", +")) || (a == ')' && isOperator(b)) || (isOperator(a) && a != '!' && b == '$') || (b == '*') || (a == ')' && b == '-') || (a == ']' && b == ':') || (a == ')' && b == ':') || (a == ':' && b != ':' && !writer.getLastWritten(2).equals("::")) || (isPreviousWordReserved(writer) && (b == '(' || b == '$')) || (a == ')' && Character.isLetter(b)) || (Character.isLetter(a) && b == '"') || (Character.isLetter(a) && b == '\'') || (Character.isLetter(a) && isOperator(b) && !isTwoCharacterOperator(b, reader.readAhead()));
    }

    /**
	 * Are these two characters a two-character PHP operator?
	 * 
	 * isPhpOperator(a) == true
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
    @Override
    public boolean isTwoCharacterOperator(int a, int b) {
        return (a == '-' && b == '>') || super.isTwoCharacterOperator(a, b);
    }

    /**
	 * Is the given character a single-character operator?
	 * 
	 * @param a
	 * @return
	 */
    @Override
    public boolean isOperator(int a) {
        return a == '.' || super.isOperator(a);
    }

    /**
	 * We need to read in a PHP script and output it as appropriate.
	 * Reader starts with "&lt;?php'. 
	 * 
	 * @param reader
	 * @param writer
	 * @throws IOException 
	 * @throws CleanerException 
	 */
    public void cleanPhpBlock(InlineStringReader reader, InlineStringWriter writer) throws IOException, CleanerException {
        writer.write(reader.read(5));
        String next2 = reader.readAheadSkipWhitespace(2);
        boolean needsWhitespace = false;
        if (!next2.equals("//") && !next2.equals("/*")) {
            needsWhitespace = true;
        }
        writer.indentIncrease();
        boolean needsLineBefore = false;
        boolean inInlineBrace = false;
        boolean inBracket = false;
        int charBeforeBlockComment = -1;
        int cur = -1;
        int prev = ' ';
        int prevNonWhitespace = -1;
        boolean isOnBlankLine = false;
        while ((cur = reader.read()) != -1) {
            if (cur == '?' && reader.readAhead() == '>') {
                writer.write(' ');
                writer.write(cur);
                writer.write(reader.read());
                if (prevNonWhitespace == '{') {
                    writer.newLineMaybe();
                    writer.indentIncrease();
                    needsWhitespace = false;
                    inInlineBrace = false;
                    prevNonWhitespace = -5;
                }
                writer.indentDecrease();
                return;
            }
            if (cur == '/' && reader.readAhead() == '/') {
                if (!isOnBlankLine && (prevNonWhitespace == ';')) {
                    writer.write(' ');
                    needsWhitespace = false;
                }
                if (prevNonWhitespace == '{') {
                    writer.newLine();
                    writer.indentIncrease();
                } else if (prevNonWhitespace == -1) {
                    writer.newLine();
                } else if (prevNonWhitespace == '}') {
                    writer.newLine();
                } else if (isOnBlankLine) {
                    writer.newLineMaybe();
                    isOnBlankLine = false;
                }
                writer.write(cur);
                writer.write(reader.read());
                jumpOverInlineComment(reader, writer, false);
                needsLineBefore = true;
                prevNonWhitespace = -3;
                inInlineBrace = false;
                continue;
            }
            if (cur == '/' && reader.readAhead() == '*') {
                if (prevNonWhitespace == '{') {
                    writer.newLine();
                    writer.indentIncrease();
                } else if (isOnBlankLine && prevNonWhitespace == '/') {
                    writer.write(' ');
                } else if (!isOnBlankLine && prevNonWhitespace != '(' && prevNonWhitespace != -1 && prevNonWhitespace != -3) {
                    writer.write(' ');
                } else if (prevNonWhitespace == ';' || prevNonWhitespace == -1 || prevNonWhitespace == -2 || prevNonWhitespace == -1 || prevNonWhitespace == '}') {
                    writer.newLine();
                }
                writer.write(cur);
                writer.write(reader.read());
                jumpOverBlockComment(reader, writer, false);
                needsWhitespace = true;
                charBeforeBlockComment = prevNonWhitespace;
                prevNonWhitespace = -2;
                inInlineBrace = false;
                continue;
            }
            if (cur == '\n' || cur == '\r') {
                isOnBlankLine = true;
            }
            if (Character.isWhitespace(cur) && shouldIgnoreWhitespaceAfter(prev)) {
                if (needsWhitespaceCharacterAfter(prev)) {
                    needsWhitespace = true;
                }
            } else if (Character.isWhitespace(cur) && Character.isWhitespace(prev)) {
            } else if (Character.isWhitespace(cur) && !Character.isWhitespace(prev)) {
                if (prev != '[' && prev != '!') {
                    needsWhitespace = true;
                }
            } else if (Character.isWhitespace(cur)) {
            } else {
                if (needsLineBefore) {
                    writer.newLineMaybe();
                    needsLineBefore = false;
                }
                isOnBlankLine = false;
                if (prevNonWhitespace == ';') {
                    if (inBracket) {
                        writer.write(' ');
                    } else {
                        writer.newLine();
                    }
                } else if (prevNonWhitespace == -2 && cur != ';' && cur != ',' && cur != ')' && cur != '{' && !Character.isWhitespace(cur) && charBeforeBlockComment != '(') {
                    writer.newLineMaybe();
                    needsWhitespace = false;
                } else if (prevNonWhitespace == '{') {
                    writer.newLineMaybe();
                    writer.indentIncrease();
                    needsWhitespace = false;
                    inInlineBrace = false;
                } else if (prevNonWhitespace == '}') {
                    if (cur == ',' || cur == ')') {
                    } else if (!isNextWordInlineBrace(reader, writer)) {
                        writer.newLine();
                    } else {
                        writer.write(' ');
                        inInlineBrace = true;
                    }
                } else if (prevNonWhitespace == ']' && (cur == ')')) {
                } else if (needsWhitespaceBetweenPhp(reader, writer, prevNonWhitespace, cur)) {
                    writer.write(' ');
                    needsWhitespace = false;
                } else if (needsWhitespace) {
                    if (!doesntActuallyNeedWhitespaceBeforePhp(reader, writer, cur) && prevNonWhitespace != -3) {
                        if (writer.getPrevious() != '\n') {
                            writer.write(' ');
                        }
                    }
                    needsWhitespace = false;
                }
                if (cur == '}') {
                    writer.indentDecrease();
                    if (prevNonWhitespace != -1) {
                        writer.newLineMaybe();
                    } else {
                        writer.write(' ');
                    }
                }
                if (cur == '(' && inInlineBrace) {
                    writer.write(' ');
                }
                writer.write(cur);
                if (cur == ';') {
                    inInlineBrace = false;
                }
                if (cur == '(') {
                    inBracket = true;
                } else if (cur == ')') {
                    inBracket = false;
                }
                if (cur == '"') {
                    jumpOverString(reader, writer, false);
                } else if (cur == '\'') {
                    jumpOverSingleString(reader, writer, false);
                }
                if (!Character.isWhitespace(cur)) {
                    prevNonWhitespace = cur;
                }
            }
            prev = cur;
        }
        if (prevNonWhitespace == '{') {
            writer.newLineMaybe();
            writer.indentIncrease();
        }
        writer.indentDecrease();
    }
}
