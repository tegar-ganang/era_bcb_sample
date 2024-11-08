package com.io_software.utils.web;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.io.BufferedOutputStream;
import java.util.Hashtable;
import java.util.Enumeration;
import java.net.URL;
import java.net.MalformedURLException;
import gnu.regexp.REFilterInputStream;
import gnu.regexp.RE;
import gnu.regexp.REMatch;
import gnu.regexp.REException;
import com.abb.util.SimpleTokenizer;

/** A special filtering input stream that reads the output of requesting a
    URL. If the response header reveals that the <tt>Content-type</tt> is
    <tt>text/html</tt>, then this filter will go and replace all words in the
    plain text by links to the passed dictionary proxy URL in such a way that
    pursuing this link will instruct the proxy to lookup that particular
    word. The word itself will appear in between the anchor tags.<p>

    All links discovered in the document (in <tt>href</tt> and <tt>src</tt>
    attributes) will be modified such that following them will be routed
    through the proxy.<p>

    If the contents of the response are not of type <tt>text/html</tt> no
    replacements will be applied.<p>

    This class acts as a bridge, copying the bytes from the input stream to the
    output stream, subjecting them to a filtering process as explained above.

    @see DictionaryProxy

    @author Axel Uhl
    @version $Id: DictionaryPageFilter.java,v 1.4 2001/04/09 12:17:45 aul Exp $
*/
public class DictionaryPageFilter implements Runnable {

    /** creates an instance of this class that reads the HTTP response from the
	passed input stream and, according to the rules explained above,
	performs URL replacements, also using passed proxy's URL.<p>

	@param is the input stream from which to read the HTTP response
	@param outputStream the stream to copy the filtered response to
	@param baseURL the URL to use as context for the creation of absolute
	URLs during substitution
	@param proxyURL when substituting URLs, use this as the proxy URL
	prefix
    */
    public DictionaryPageFilter(InputStream inputStream, OutputStream outputStream, URL baseURL, URL proxyURL) {
        this.inputStream = inputStream;
        this.outputStream = new BufferedOutputStream(outputStream);
        this.baseURL = baseURL;
        this.proxyURL = proxyURL;
    }

    /** copies the bytes from the input stream to the output stream. After the
	header has been read, the content type is known and the filter can be
	put either into substitution or no-substitution mode.<p>

	Once the input has been fully consumed, both the input and the output
	stream are closed.
    */
    public void run() {
        try {
            StringBuffer header = new StringBuffer();
            int read = inputStream.read();
            boolean lastWasNewline = false;
            while (read != -1 && (read != '\n' || !lastWasNewline)) {
                header.append((char) read);
                if (read != '\r') lastWasNewline = (read == '\n');
                read = inputStream.read();
            }
            header.append((char) read);
            if (header.toString().toLowerCase().indexOf("content-type: text/html") >= 0) copyWithSubstitute(header.toString()); else {
                outputStream.write(header.toString().getBytes());
                copyNoSubstitute();
            }
        } catch (Exception ioe) {
            ioe.printStackTrace();
        }
    }

    /** copies the input stream into the output stream without any
	substitutions and closes both streams upon reaching EOF in the input
	stream
    */
    private void copyNoSubstitute() throws IOException {
        int read = inputStream.read();
        while (read != -1) {
            outputStream.write(read);
            read = inputStream.read();
        }
        System.out.println("copyNoSubsitute: close");
        outputStream.close();
        inputStream.close();
    }

    /** copies the input stream into the output stream doing the substitutions
	expliained in the {@link DictionaryPageFilter class comment} and closes
	both streams upon reaching EOF in the input stream

	@param header The header of the request. This will be written out after
	the substituted content has been computed as the substituted content
	length will in general be different from the original content length.
    */
    private void copyWithSubstitute(String header) throws IOException, REException {
        try {
            StringBuffer sb = new StringBuffer();
            int read = inputStream.read();
            while (read != -1) {
                sb.append((char) read);
                read = inputStream.read();
            }
            RE replaceHREF = new RE(" [Hh][Rr][Ee][Ff]=[\"]([^ \"]*)", RE.REG_MULTILINE);
            RE replaceSRC = new RE(" [Ss][Rr][Cc]=[\"]([^ \"]*)", RE.REG_MULTILINE);
            String s = sb.toString();
            s = smartURLReplace(replaceHREF, s, " href=\"");
            s = smartURLReplace(replaceSRC, s, " src=\"");
            s = linkWordsToDictionary(s);
            int contentLength = s.length();
            RE contentLengthPattern = new RE("[Cc]ontent-[Ll]ength: ([0-9]+)");
            String modifiedHeader = contentLengthPattern.substitute(header, "Content-Length: " + contentLength);
            outputStream.write(modifiedHeader.getBytes());
            outputStream.write(s.getBytes());
        } finally {
            outputStream.close();
            inputStream.close();
        }
    }

    /** given a string that represents a valid HTML page parses the page's tag
	structure and replaces all words that are not part of a tag itself and
	not inside an anchor tag by an anchor tag that contains that word in
	its body and links to the dictionary such that traversing the link will
	display the translation results for that word.

	@param s the string in which to substitute
	@return the substituted string
    */
    private String linkWordsToDictionary(String s) throws IOException {
        StringBuffer result = new StringBuffer();
        SimpleTokenizer st = new DictionaryPageFilterTokenizer(new StringReader(s));
        st.setSkipWhitespacesAfterToken(false);
        int tokenType = st.nextToken();
        boolean inAnchor = false;
        boolean inBody = false;
        boolean inScript = false;
        while (tokenType != StreamTokenizer.TT_EOF) {
            if (tokenType == '<') {
                Hashtable attributes = new Hashtable();
                String tagName = Form.parseTag(st, attributes);
                if (tagName.length() > 0) {
                    Form.appendTag(tagName, attributes, result);
                    if (tagName.toLowerCase().equals("/a")) inAnchor = false; else if (tagName.toLowerCase().equals("a")) inAnchor = true; else if (tagName.toLowerCase().equals("body")) inBody = true; else if (tagName.toLowerCase().equals("/body")) inBody = false; else if (tagName.toLowerCase().equals("script")) inScript = true; else if (tagName.toLowerCase().equals("/script")) inScript = false; else if (tagName.toLowerCase().equals("javascript")) inScript = true; else if (tagName.toLowerCase().equals("/javascript")) inScript = false;
                }
            } else if (!inAnchor && inBody && !inScript && (tokenType == StreamTokenizer.TT_WORD || tokenType == '\"')) appendLookupForWord(st.sval, result); else append(tokenType, st, result);
            tokenType = st.nextToken();
        }
        return result.toString();
    }

    /** given a word string outputs a lookup anchor tag that, when traversed,
	instructs the proxy to lookup the passed word.

	@param word the word to look up
	@param buffer the output buffer to which to append the HTML anchor tag
    */
    private void appendLookupForWord(String word, StringBuffer buffer) {
        buffer.append("<a href=");
        buffer.append(proxyURL);
        buffer.append("?lookup=\"");
        buffer.append(word);
        buffer.append("\">");
        buffer.append(word);
        buffer.append("</a>");
    }

    /** given a token type from a {@link SimpleTokenizer} and the tokenizer
	itself, containing any string values for the token, this method appends
	the string representation of the token to the passed {@link
	StringBuffer}.

	@param tokenType type of the token as returned by {@link
	SimpleTokenizer#nextToken}
	@param tokenizer the tokenizer that returned this token type for the
	last requested token
	@param buffer the string buffer to which to append the string
	representation of the last token
    */
    private void append(int tokenType, SimpleTokenizer tokenizer, StringBuffer buffer) {
        if (tokenType == '\"') buffer.append("\"" + tokenizer.sval + "\""); else if (tokenType == StreamTokenizer.TT_WORD) buffer.append(tokenizer.sval); else buffer.append((char) tokenType);
    }

    /** Finds all occurrences of <tt>pattern</tt> in string <tt>s</tt> and
	replaces them "smartly" (by
	<tt>replacePrefix+proxyURL+"?url="+absoluteURL</tt>). The resulting
	string is returned.<p>

	This replacement has two important effects:

	<ul>
	  <li> The referenced URLs are transformed such that accessing them
	       will route the request to the proxy again.
	  <li> The URLs are made absolute using the {@link #baseURL} as context
	       for the possibly relative URLs before rendering them as
	       strings. This allows relative URLs to be accessed through the
	       proxy.
	</ul>

	@param pattern the regular expression 
     */
    private String smartURLReplace(RE pattern, String s, String replacePrefix) throws MalformedURLException {
        StringBuffer replaced = new StringBuffer();
        int index = 0;
        REMatch match = pattern.getMatch(s, index);
        while (match != null) {
            replaced.append(s.substring(index, match.getStartIndex()));
            String urlString = match.toString(1);
            URL absoluteURL = new URL(baseURL, urlString);
            String subst = match.substituteInto(replacePrefix + proxyURL + "?url=" + absoluteURL);
            replaced.append(subst);
            index = match.getEndIndex();
            match = pattern.getMatch(s, index);
        }
        replaced.append(s.substring(index));
        return replaced.toString();
    }

    /** Inner class subclassing {@link SimpleTokenizer}, adding more separator
	characters as they are to be recognized in a regular HTML
	document. As separation characters all characters are considered that
	are outside the rance of [a-zA-Z0-9].

	@author Axel Uhl
	@version $Id: DictionaryPageFilter.java,v 1.4 2001/04/09 12:17:45 aul Exp $
    */
    public static class DictionaryPageFilterTokenizer extends SimpleTokenizer {

        /** constructor passes on the reader to the superclass constructor

	    @param r the reader to read the tokens from
	*/
        public DictionaryPageFilterTokenizer(Reader r) {
            super(r);
        }

        /** as separation characters all characters outside of [a-zA-Z0-9] are
	    considered.

	    @param c the character to check
	    @return <tt>true</tt> if <tt>c</tt> is outside of [a-zA-Z0-9]
	*/
        public boolean isSeparator(char c) {
            return !((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9'));
        }
    }

    /** the URL of the proxy */
    private URL proxyURL;

    /** the base URL for absolute URL construction context */
    private URL baseURL;

    /** the input stream from which to read the HTTP response */
    private InputStream inputStream;

    /** the output stream to copy the filtered HTTP response to */
    private OutputStream outputStream;
}
