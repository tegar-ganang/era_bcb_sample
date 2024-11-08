package net.innig.imre.domain.markup;

import net.innig.collect.InnigCollections;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static org.apache.commons.lang.StringUtils.*;
import org.grlea.logBridge.LogBridge;
import org.grlea.logBridge.LogBridgeManager;

public class MarkupUtils {

    private static final LogBridge log = LogBridgeManager.createLogBridge(MarkupUtils.class);

    private static final Pattern attributePattern = Pattern.compile("\\s+([\\w\\-]+)\\s*=\\s*(\"[^\n\r\"]*\"|[\\w]+)"), tagPattern = Pattern.compile("<\\s*(/?)\\s*([\\w\\-]+)((" + attributePattern + ")*)\\s*>"), paragraphPattern = Pattern.compile(tagPattern + "|\\s*\n\\s*|$");

    public static CharSequence cleanXml(CharSequence markup) {
        if (markup == null) return "";
        StringBuffer out = new StringBuffer(markup.length());
        Matcher tagMatch = tagPattern.matcher(markup);
        while (tagMatch.find()) {
            boolean endTag = tagMatch.group(1).length() > 0;
            String tagName = tagMatch.group(2);
            String attributes = tagMatch.group(3);
            tagMatch.appendReplacement(out, "<");
            if (endTag) out.append('/');
            out.append(tagName.toLowerCase());
            Matcher attrMatch = attributePattern.matcher(attributes);
            while (attrMatch.find()) {
                String attrName = attrMatch.group(1);
                String attrValue = attrMatch.group(2);
                attrMatch.appendReplacement(out, " ");
                out.append(attrName.toLowerCase());
                out.append('=');
                if (!attrValue.startsWith("\"")) out.append('"');
                out.append(attrValue);
                if (!attrValue.startsWith("\"")) out.append('"');
            }
            out.append('>');
        }
        tagMatch.appendTail(out);
        return out;
    }

    public static CharSequence addParagraphTags(CharSequence markup, Set<String> textTags, Set<String> inlineTags) {
        StringBuilder out = new StringBuilder(markup.length());
        LinkedList<String> tagStack = new LinkedList<String>();
        tagStack.add("");
        int cursor = 0;
        Matcher match = paragraphPattern.matcher(markup);
        while (match.find()) {
            String curTag = tagStack.getLast();
            CharSequence text = markup.subSequence(cursor, match.start());
            cursor = match.end();
            if (!isBlank(text.toString()) && !isAddedPara(curTag)) if (!inlineTags.contains(curTag) && !textTags.contains(curTag)) curTag = openPara(out, tagStack);
            out.append(text);
            if (match.group(1) != null) {
                boolean endTag = match.group(1).length() > 0;
                String tagName = match.group(2);
                if (!endTag) {
                    if (isAddedPara(curTag) && !inlineTags.contains(tagName)) closePara(out, tagStack);
                    tagStack.addLast(tagName);
                } else {
                    if (isAddedPara(curTag)) closePara(out, tagStack);
                    popTag(tagStack, tagName);
                }
            } else {
                if (isAddedPara(curTag)) curTag = closePara(out, tagStack);
            }
            out.append(match.group(0));
        }
        assert cursor == markup.length() : "Paragraph pattern should have matched end of string";
        return out;
    }

    private static boolean isAddedPara(String curTag) {
        return curTag.equals("p+");
    }

    private static String openPara(StringBuilder out, LinkedList<String> tagStack) {
        tagStack.addLast("p+");
        out.append("<p>");
        return "p+";
    }

    private static String closePara(StringBuilder out, LinkedList<String> tagStack) {
        popTag(tagStack, "p+");
        out.append("</p>");
        return tagStack.getLast();
    }

    private static void popTag(LinkedList<String> tagStack, String expectedTagName) {
        String popped = tagStack.removeLast();
        if (!popped.equals(expectedTagName)) log.warn("mismatched end tag: expected " + popped + " but got " + expectedTagName);
    }

    public static void main(String[] args) throws Exception {
        RandomAccessFile file = new RandomAccessFile("work/tokentest.xml", "r");
        CharSequence markup = Charset.forName("utf-8").decode(file.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, file.length()));
        System.out.println(markup = cleanXml(markup));
        System.out.println("------------------------------------------------------------------------------");
        System.out.println(addParagraphTags(markup, InnigCollections.toSet("p", "li", "h1", "h2", "h3", "h4", "ih"), InnigCollections.toSet("a", "b", "i", "br")));
    }
}
