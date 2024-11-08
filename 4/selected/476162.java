package org.openiaml.iacleaner;

import java.io.IOException;
import org.openiaml.iacleaner.inline.InlineCleanerException;
import org.openiaml.iacleaner.inline.InlineStringReader;
import org.openiaml.iacleaner.inline.InlineStringWriter;

/**
 * Handles the cleaning of Javascript content.
 * 
 * @author Jevon
 *
 */
public class InlineJavascriptCleaner extends InlineCSyntaxCleaner {

    public InlineJavascriptCleaner(IAInlineCleaner inline) {
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
    private boolean doesntActuallyNeedWhitespaceBeforeJavascript(InlineStringReader reader, InlineStringWriter writer, int cur) throws IOException {
        return cur == '(' || cur == ')' || cur == '}' || cur == ';' || cur == '.' || cur == ',' || cur == '[' || cur == ']' || cur == '+' || cur == '-' || writer.getPrevious() == '-' || writer.getPrevious() == '+' || isTwoCharacterOperator(cur, reader.readAhead());
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
    private boolean needsWhitespaceBetweenJavascript(InlineStringReader reader, InlineStringWriter writer, int a, int b) throws IOException {
        return (a == ')' && b == '{') || (a == ',') || (!isOperator(a) && b == '=') || (a == '=' && (b != '>' && b != '=')) || (b == '?') || (a == '?') || (b == '{') || (a != '(' && a != '!' && b == '!') || (a != '+' && b == '+' && reader.readAhead() != '+') || (a == '+' && b == '+' && reader.readAhead() == '+') || (a != '-' && b == '-' && reader.readAhead() != '-') || (a == '-' && b == '-' && reader.readAhead() == '-') || (a != '|' && b == '|') || (a != '&' && b == '&') || (b == '<' || b == '>') || (isOperator(a) && !isOperator(b) && b != ')' && a != '!' && b != ';' && !writer.getLastWritten(3).equals(", -") && !writer.getLastWritten(3).equals(", +")) || (a == ')' && isOperator(b)) || (b == '*') || (a == ')' && b == '-') || (a == ']' && b == ':') || (a == ')' && b == ':') || (a == ':' && b != ':' && !writer.getLastWritten(2).equals("::")) || (isPreviousWordReserved(writer) && (b == '(' || b == '$')) || (a == ')' && Character.isLetter(b)) || ((isOperator(a) || Character.isLetter(a)) && b == '"') || ((isOperator(a) || Character.isLetter(a)) && b == '\'') || (Character.isLetter(a) && isOperator(b) && !isTwoCharacterOperator(b, reader.readAhead()));
    }

    /**
	 * The last character we read in Javascript mode was "/"; skip through the string
	 * until we find the end of the regexp "/". We also print out any
	 * regexp parameters that are attached to the regexp, e.g.
	 * "/regexp/ig".
	 * 
	 * @param reader
	 * @param writer
	 * @param allowSwitchToPhpMode can we switch to a new PHP block within this string?
	 * @throws IOException 
	 * @throws CleanerException 
	 */
    protected void jumpOverJavascriptRegexp(InlineStringReader reader, InlineStringWriter writer, boolean allowSwitchToPhpMode) throws IOException, CleanerException {
        try {
            writer.enableIndent(false);
            int cur = -1;
            while ((cur = reader.read()) != -1) {
                if (allowSwitchToPhpMode) {
                    if (getInline().didSwitchToPhpMode(reader, writer, cur)) {
                        continue;
                    }
                }
                if (cur == '/') {
                    writer.write(cur);
                    while ((cur = reader.read()) != -1) {
                        if (!Character.isLetter(cur)) break;
                        writer.write(cur);
                    }
                    reader.unread(cur);
                    return;
                }
                writer.write(cur);
                if (cur == '\\' && reader.readAhead() == '/') {
                    writer.write(reader.read());
                }
            }
            throw new InlineCleanerException("PHP single-quoted string did not terminate", reader);
        } finally {
            writer.enableIndent(true);
        }
    }

    /**
	 * We are in HTML and we have just processed a tag
	 * '&lt;script ...&gt;'; or we are in a single Javascript file.
	 * We need to parse the inline Javascript until
	 * we are about to hit '&lt;/script&gt;' (if necessary).
	 * 
	 * @param reader
	 * @param writer
	 * @param withinHtml true if we are in an HTML block; false if we are alone
	 * @throws IOException 
	 * @throws CleanerException 
	 */
    public void cleanJavascriptBlock(InlineStringReader reader, InlineStringWriter writer, boolean withinHtml) throws IOException, CleanerException {
        if (withinHtml && getInline().readAheadUntilEndHtmlTagWithOpenBrace(reader, writer).toLowerCase().equals("/script")) {
            return;
        }
        if (withinHtml) {
            writer.indentIncrease();
        }
        boolean needsNewLine = withinHtml;
        boolean needsLineBefore = false;
        boolean inInlineBrace = false;
        boolean inBracket = false;
        int charBeforeBlockComment = -1;
        int cur = -1;
        int prev = ' ';
        int prevNonWhitespace = -1;
        boolean needsWhitespace = false;
        boolean isOnBlankLine = false;
        boolean hadCdataBlock = false;
        while ((cur = reader.read()) != -1) {
            if (cur == '<' && "![CDATA[".equals(reader.readAheadSkipAllWhitespace(8))) {
                writer.newLineMaybe();
                writer.write("<![CDATA[");
                writer.newLine();
                reader.skipAllWhitespace(8);
                hadCdataBlock = true;
                needsNewLine = false;
                continue;
            }
            if (hadCdataBlock && cur == ']' && "]>".equals(reader.readAheadSkipAllWhitespace(2))) {
                writer.newLineMaybe();
                writer.write("]]>");
                reader.skipAllWhitespace(2);
                needsNewLine = false;
                hadCdataBlock = false;
                continue;
            }
            if (withinHtml && cur == '<') {
                reader.unread(cur);
                if (getInline().readAheadUntilEndHtmlTagWithOpenBrace(reader, writer).toLowerCase().equals("/script")) {
                    writer.indentDecrease();
                    if (!needsNewLine) {
                        writer.newLineMaybe();
                    }
                    return;
                }
                reader.read();
            }
            if (cur == '/' && reader.readAhead() == '/') {
                if (!isOnBlankLine && (prevNonWhitespace == ';')) {
                    writer.write(' ');
                    needsWhitespace = false;
                }
                if (prevNonWhitespace == '{') {
                    writer.newLine();
                    writer.indentIncrease();
                } else if (prevNonWhitespace == -1 && !withinHtml) {
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
                jumpOverInlineComment(reader, writer, true);
                needsLineBefore = true;
                prevNonWhitespace = -3;
                inInlineBrace = false;
                continue;
            }
            if (cur == '/' && reader.readAhead() == '*') {
                if (prevNonWhitespace == '{') {
                    writer.newLine();
                    writer.indentIncrease();
                } else if (!isOnBlankLine && prevNonWhitespace != '(' && prevNonWhitespace != -1 && prevNonWhitespace != -3) {
                    writer.write(' ');
                } else if (prevNonWhitespace == -1 && !withinHtml) {
                } else if (prevNonWhitespace == ';' || prevNonWhitespace == -1 || prevNonWhitespace == -2 || prevNonWhitespace == -1 || prevNonWhitespace == '}') {
                    writer.newLine();
                }
                writer.write(cur);
                writer.write(reader.read());
                jumpOverBlockComment(reader, writer, true);
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
                if (needsNewLine) {
                    writer.newLineMaybe();
                    needsNewLine = false;
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
                    if (cur == ',' || cur == ')' || cur == ';') {
                    } else if (!isNextWordInlineBrace(reader, writer)) {
                        writer.newLine();
                    } else {
                        writer.write(' ');
                        inInlineBrace = true;
                    }
                } else if (prevNonWhitespace == ']' && (cur == ')')) {
                } else if (prevNonWhitespace != -4 && needsWhitespaceBetweenJavascript(reader, writer, prevNonWhitespace, cur)) {
                    writer.write(' ');
                    needsWhitespace = false;
                } else if (needsWhitespace) {
                    if (!doesntActuallyNeedWhitespaceBeforeJavascript(reader, writer, cur) && prevNonWhitespace != -3) {
                        if (writer.getPrevious() != '\n') {
                            writer.write(' ');
                        }
                    }
                    needsWhitespace = false;
                }
                if (cur == '}') {
                    writer.indentDecrease();
                    writer.newLineMaybe();
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
                boolean didRegexpMode = false;
                if (cur == '"') {
                    jumpOverString(reader, writer, true);
                } else if (cur == '\'') {
                    jumpOverSingleString(reader, writer, true);
                } else if (cur == '/' && (prevNonWhitespace == '=' || prevNonWhitespace == '(' || prevNonWhitespace == '.' || prevNonWhitespace == ':')) {
                    jumpOverJavascriptRegexp(reader, writer, true);
                    prevNonWhitespace = -4;
                    needsWhitespace = false;
                    didRegexpMode = true;
                }
                if (!Character.isWhitespace(cur) && !didRegexpMode) {
                    prevNonWhitespace = cur;
                }
            }
            prev = cur;
            if (withinHtml) {
                String nextTag = getInline().readAheadUntilEndHtmlTagWithOpenBrace(reader, writer);
                if (nextTag != null && "/script".equals(nextTag.toLowerCase())) {
                    writer.indentDecrease();
                    if (!needsNewLine) {
                        writer.newLineMaybe();
                    }
                    return;
                }
            }
            if (reader.readAhead(5) != null && reader.readAhead(5).equals("<?php")) {
                if (isOnBlankLine) {
                    writer.newLineMaybe();
                }
                if (prevNonWhitespace == '{') {
                    writer.indentIncrease();
                }
                getInline().cleanPhpBlock(reader, writer);
                if (prevNonWhitespace == '{') {
                    writer.indentDecrease();
                }
            }
        }
        if (withinHtml) {
            throw new InlineCleanerException("Unexpectedly terminated out of Javascript mode", reader);
        } else {
            return;
        }
    }

    /**
	 * We are in an HTML tag, and we have hit a [stringCharacter]. We need to process
	 * until we find the end of the string.
	 * 
	 * @param reader
	 * @param writer
	 * @param stringCharacter either " or '
	 * @param allowJumpToPhp can we jump to PHP mode?
	 */
    protected void jumpOverHtmlAttributeString(InlineStringReader reader, InlineStringWriter writer, int stringCharacter, boolean allowJumpToPhp) throws IOException, CleanerException {
        try {
            writer.enableWordwrap(false);
            writer.enableIndent(false);
            int cur = -1;
            while ((cur = reader.read()) != -1) {
                if (cur == stringCharacter) {
                    writer.write(cur);
                    return;
                }
                if (allowJumpToPhp && cur == '<' && reader.readAhead(4).equals("?php")) {
                    reader.unread(cur);
                    getInline().cleanPhpBlock(reader, writer);
                    continue;
                }
                writer.write(cur);
            }
            throw new InlineCleanerException("HTML Attribute string did not terminate", reader);
        } finally {
            writer.enableWordwrap(true);
            writer.enableIndent(true);
        }
    }
}
