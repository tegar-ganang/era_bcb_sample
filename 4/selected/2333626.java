package org.openiaml.iacleaner;

import java.io.IOException;
import java.util.Stack;
import org.openiaml.iacleaner.inline.InlineCleanerException;
import org.openiaml.iacleaner.inline.InlineStringReader;
import org.openiaml.iacleaner.inline.InlineStringWriter;

/**
 * Handles the cleaning of HTML content.
 * 
 * @author Jevon
 *
 */
public class InlineHtmlCleaner {

    private IAInlineCleaner inline;

    public InlineHtmlCleaner(IAInlineCleaner inline) {
        this.inline = inline;
    }

    public IAInlineCleaner getInline() {
        return inline;
    }

    /**
	 * Clean up HTML code.
	 * 
	 * @param reader
	 * @param writer
	 * @throws IOException 
	 * @throws CleanerException 
	 */
    public void cleanHtmlBlock(InlineStringReader reader, InlineStringWriter writer) throws IOException, CleanerException {
        Stack<String> tagStack = new Stack<String>();
        String lastTag = null;
        String stackTag = null;
        while (cleanHtmlTextUntilNextTag(stackTag, lastTag, reader, writer, '<') && reader.readAhead() != -1) {
            String next5 = reader.readAhead(5);
            if (next5.equals("<?xml")) {
                getInline().cleanXmlBlock(reader, writer);
            } else if (next5.equals("<?php")) {
                getInline().cleanPhpBlock(reader, writer);
            } else if (next5.substring(0, 4).equals("<!--")) {
                cleanHtmlComment(reader, writer);
            } else {
                lastTag = cleanHtmlTag(reader, writer).toLowerCase();
                stackTag = getCurrentTagFromStack(lastTag, tagStack);
            }
        }
    }

    /**
	 * The given stack contains a stack of opened elements; when we open new 
	 * ones, they should be added to the stack, and when closed, they should be 
	 * removed.
	 * 
	 * Some elements will never be closed (e.g. <code>&lt;img&gt;</code>;
	 * the tag stack should just jump up until it finds the closing tag. 
	 * 
	 * @param newTagName
	 * @param tagStack
	 * @return the current stack tag, or null if we have run out of stack
	 */
    private String getCurrentTagFromStack(String newTagName, Stack<String> tagStack) {
        if (newTagName == null) return tagStack.empty() ? null : tagStack.peek();
        if (newTagName.startsWith("/")) {
            String r = newTagName.substring(1);
            while (!tagStack.empty() && !tagStack.pop().equals(r)) {
            }
            if (tagStack.empty()) {
                return null;
            } else {
                return tagStack.peek();
            }
        } else {
            tagStack.push(newTagName);
            return newTagName;
        }
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
	 * We need to read in a comment and output it as appropriate.
	 * Reader starts with '&lt;!--'.
	 * 
	 * @param reader
	 * @param writer
	 * @throws IOException 
	 * @throws CleanerException 
	 */
    protected void cleanHtmlComment(InlineStringReader reader, InlineStringWriter writer) throws IOException, CleanerException {
        int cur = -1;
        boolean isNewline = false;
        boolean startedComment = false;
        boolean commentLine = false;
        while (true) {
            if (!startedComment && Character.isWhitespace(reader.getLastChar())) {
                writer.newLineMaybe();
                commentLine = true;
            }
            startedComment = true;
            cur = reader.read();
            if (cur == -1) break;
            if (cur != '\n' && Character.isWhitespace(cur) && isNewline) {
            } else if (cur == '\r') {
            } else if (cur == '\n') {
                writer.newLine();
            } else {
                writer.write(cur);
                isNewline = false;
            }
            if (reader.readAhead(3).equals("-->")) {
                writer.write(reader.read(3));
                if (commentLine) writer.newLine();
                return;
            }
            if (cur == '\n') {
                isNewline = true;
            }
        }
        throw new CleanerException("At end of file before found end of comment");
    }

    /**
	 * Should the given HTML tag be intented?
	 * @param tag
	 * @return
	 */
    protected boolean htmlTagIndented(String tag) {
        if (tag.charAt(0) == '/' && tag.length() > 1) return htmlTagIndented(tag.substring(1));
        return tag.equals("html") || tag.equals("body") || tag.equals("ol") || tag.equals("ul") || tag.equals("head") || tag.equals("p");
    }

    /**
	 * Does the given HTML tag need to be put on a new line?
	 * @param tag
	 * @return
	 */
    protected boolean htmlTagNeedsNewLine(String tag) {
        return tag.equals("h1") || tag.equals("h2") || tag.equals("h3") || tag.equals("h4") || tag.equals("h5") || tag.equals("h6") || tag.equals("li") || tag.equals("title") || tag.equals("link") || tag.equals("head") || tag.equals("body") || tag.equals("ol") || tag.equals("ul");
    }

    /**
	 * Does a new line need to be added at the end of this tag?
	 * @param tag
	 * @return
	 */
    protected boolean htmlTagNeedsTrailingNewLine(String tag) {
        return tag.equals("/h1") || tag.equals("/h2") || tag.equals("/h3") || tag.equals("/h4") || tag.equals("/h5") || tag.equals("/h6") || tag.equals("/li") || tag.equals("/title") || tag.equals("/link") || tag.equals("/head") || tag.equals("/body") || tag.equals("/ol") || tag.equals("/ul") || tag.equals("/script") || tag.startsWith("!");
    }

    /**
	 * Does the given HTML singleton tag (i.e. <tag />) 
	 * need to be appended with a new line?
	 * 
	 * @param tag
	 * @return
	 */
    protected boolean htmlTagNeedsNewLineSingleton(String tag) {
        return htmlTagNeedsTrailingNewLine("/" + tag);
    }

    /**
	 * We have just started an &lt;htmlTag attr...&gt;. Clean the tag up, and return
	 * the 'htmlTag'.
	 * 
	 * @param reader
	 * @param writer
	 * @return the htmlTag found, or null if we unexpectedly fell out
	 * @throws IOException 
	 * @throws CleanerException if we hit EOF unexpectedly
	 */
    protected String cleanHtmlTag(InlineStringReader reader, InlineStringWriter writer) throws IOException, CleanerException {
        int first = reader.read();
        String tagName = findHtmlTagName(reader);
        if (tagName.isEmpty()) {
            throw new InlineCleanerException("Unexpectedly hit an invalid HTML tag while searching for HTML tags [first='" + (char) first + "']", reader);
        }
        if (tagName.charAt(0) == '/' && htmlTagIndented(tagName)) {
            writer.indentDecrease();
            writer.newLineMaybe();
        }
        if (htmlTagNeedsNewLine(tagName)) {
            writer.newLineMaybe();
        }
        writer.write(first);
        int cur;
        boolean doneTag = false;
        while ((cur = reader.read()) != -1) {
            if (cur == '>') {
                int prev = writer.getPrevious();
                writer.write(cur);
                if (tagName.charAt(0) != '/' && htmlTagIndented(tagName)) {
                    writer.indentIncrease();
                    writer.newLine();
                }
                if (htmlTagNeedsTrailingNewLine(tagName)) {
                    writer.newLine();
                } else if (prev == '/' && htmlTagNeedsNewLineSingleton(tagName)) {
                    writer.newLine();
                }
                if (tagName.toLowerCase().equals("script")) {
                    getInline().cleanHtmlJavascript(reader, writer, true);
                } else if (tagName.toLowerCase().equals("style")) {
                    getInline().cleanHtmlCss(reader, writer, true);
                }
                return tagName;
            } else if (Character.isWhitespace(cur)) {
                if (!doneTag) {
                } else {
                    reader.unread(cur);
                    cleanHtmlTagAttributes(reader, writer);
                }
            } else {
                writer.write(cur);
                if (Character.isLetterOrDigit(cur) || cur == '_') {
                    doneTag = true;
                }
            }
        }
        getInline().throwWarning("We never found the end of HTML tag", tagName.toString());
        return null;
    }

    /**
	 * We are in an HTML tag and we have just written
	 * '&lt;a'; we now want to parse and clean any attributes.
	 * 
	 * @param reader
	 * @param writer
	 * @throws IOException 
	 * @throws CleanerException 
	 */
    protected void cleanHtmlTagAttributes(InlineStringReader reader, InlineStringWriter writer) throws IOException, CleanerException {
        int cur = -1;
        int prev = -1;
        boolean needWhitespace = false;
        boolean ignoreWhitespaceAfter = false;
        while ((cur = reader.read()) != -1) {
            if (Character.isWhitespace(cur)) {
                if (prev == -1) {
                    needWhitespace = true;
                } else if (Character.isWhitespace(prev)) {
                } else if (ignoreWhitespaceAfter) {
                } else {
                    needWhitespace = true;
                }
            } else if (cur == '=') {
                writer.write(cur);
                needWhitespace = false;
                ignoreWhitespaceAfter = true;
            } else if (cur == '"' || cur == '\'') {
                if (needWhitespace) {
                    writer.write(' ');
                }
                writer.write(cur);
                jumpOverHtmlAttributeString(reader, writer, cur, true);
                ignoreWhitespaceAfter = false;
                needWhitespace = true;
            } else if (cur == '<' && "?php".equals(reader.readAhead(4))) {
                if (needWhitespace) {
                    writer.write(' ');
                    needWhitespace = false;
                }
                reader.unread(cur);
                getInline().cleanPhpBlock(reader, writer);
            } else {
                if (needWhitespace) {
                    writer.write(' ');
                    needWhitespace = false;
                }
                ignoreWhitespaceAfter = false;
                writer.write(cur);
            }
            prev = cur;
            if (reader.readAhead() == '>') {
                return;
            }
        }
        throw new InlineCleanerException("Expected > to end HTML tag while parsing for attributes", reader);
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

    /**
	 * Read ahead in the stream 'html...>...' and find the current (next) HTML tag.
	 * Also includes formatted attributes. TODO link
	 * 
	 * If this returns an empty string, it may mean EOF, or the stream
	 * begins with [A-Za-z0-9_\-/].
	 * 
	 * @see InlineStringReader#readAheadUntilEndHtmlTag()
	 * @param reader
	 * @return
	 * @throws IOException 
	 * @throws CleanerException 
	 */
    private String findHtmlTagName(InlineStringReader reader) throws IOException, CleanerException {
        return getInline().readAheadUntilEndHtmlTag(reader);
    }

    /**
	 * Remove space around HTML text until we find the character '<'
	 * (don't include this in the output)
	 * 
	 * This also controls the formatting of the output text.
	 * 
	 * @param the current tag (or null if there has not been any tags yet)
	 * @param reader
	 * @param writer
	 * @param c
	 * @return true if there is more text to go, or false at EOF
	 * @throws IOException 
	 * @throws CleanerException 
	 */
    protected boolean cleanHtmlTextUntilNextTag(String currentTag, String lastTag, InlineStringReader reader, InlineStringWriter writer, char c) throws IOException, CleanerException {
        int cur;
        int prev = -1;
        boolean addWhitespace = false;
        boolean hasDoneWhitespace = false;
        while ((cur = reader.readAhead()) != -1) {
            if (cur == '<') {
                if (addWhitespace && !Character.isWhitespace(writer.getPrevious())) {
                    String nextTag = getInline().readAheadUntilEndHtmlTagWithOpenBrace(reader, writer);
                    if (!htmlTagRequiresInlineWhitespace(currentTag) || htmlTagWillIgnoreLeadingWhitespace(lastTag) || htmlTagWillIgnoreTrailingWhitespace(nextTag)) {
                        if ((nextTag.isEmpty() || (nextTag.charAt(0) != '/' && nextTag.charAt(0) != '!')) && !htmlTagNeedsNewLine(nextTag)) {
                            writer.write(' ');
                            addWhitespace = false;
                        }
                    } else {
                        if (!htmlTagWillIgnoreLeadingWhitespace(nextTag) || !htmlTagWillIgnoreTrailingWhitespace(nextTag)) {
                            if (nextTag.isEmpty() || nextTag.charAt(0) != '!') {
                                if (!htmlTagWillIgnoreAllWhitespace(currentTag)) {
                                    writer.write(' ');
                                    addWhitespace = false;
                                }
                            }
                        }
                    }
                }
                return true;
            }
            reader.read();
            if (Character.isWhitespace(cur)) {
                if (!hasDoneWhitespace && htmlTagRequiresInlineWhitespace(currentTag) && !htmlTagWillIgnoreLeadingWhitespace(lastTag) && !Character.isWhitespace(writer.getPrevious())) {
                    addWhitespace = true;
                } else if (Character.isWhitespace(prev)) {
                } else if (prev == -1) {
                } else if (reader.readAhead() == '<' && (reader.readAhead(2).charAt(1) == '/' || reader.readAhead(2).charAt(1) == '!')) {
                } else {
                    addWhitespace = true;
                }
            } else {
                if (addWhitespace) {
                    writer.write(' ');
                    addWhitespace = false;
                }
                writer.write(cur);
            }
            prev = cur;
        }
        return false;
    }

    /**
	 * @param currentTag
	 * @return
	 */
    private boolean htmlTagWillIgnoreAllWhitespace(String currentTag) {
        return currentTag.equals("html") || currentTag.equals("body") || currentTag.equals("head");
    }

    /**
	 * Will the given next tag ignore any leading whitespace placed after it?
	 * 
	 * @param currentTag
	 * @return
	 */
    private boolean htmlTagWillIgnoreLeadingWhitespace(String currentTag) {
        if (currentTag == null) return true;
        return currentTag.equals("h1") || currentTag.equals("h2") || currentTag.equals("h3") || currentTag.equals("h4") || currentTag.equals("h5") || currentTag.equals("h6") || currentTag.equals("p") || currentTag.equals("body") || currentTag.equals("html") || currentTag.equals("title") || currentTag.equals("style") || currentTag.equals("script") || currentTag.equals("head") || currentTag.equals("div") || currentTag.equals("label") || currentTag.equals("li") || currentTag.equals("ol") || currentTag.equals("ul");
    }

    /**
	 * Will the given next tag ignore any trailing whitespace placed before it?
	 * 
	 * @param nextTag
	 * @return
	 */
    private boolean htmlTagWillIgnoreTrailingWhitespace(String nextTag) {
        if (nextTag == null) return true;
        return nextTag.length() > 1 && nextTag.startsWith("/") && htmlTagWillIgnoreLeadingWhitespace(nextTag.substring(1));
    }

    /**
	 * Does the current HTML tag require us to keep whitespace?
	 * 
	 * @return
	 * @throws CleanerException 
	 * @throws IOException 
	 */
    private boolean htmlTagRequiresInlineWhitespace(String currentTag) {
        if (currentTag == null) return false;
        return true;
    }
}
