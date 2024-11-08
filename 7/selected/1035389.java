package com.rapidminer.operator.web.html;

import java.io.UnsupportedEncodingException;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringEscapeUtils;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.io.web.GetWebpageOperator;
import com.rapidminer.operator.io.web.URLConnectionProvider;
import com.rapidminer.operator.text.Document;
import com.rapidminer.operator.text.Token;
import com.rapidminer.operator.text.io.AbstractTokenProcessor;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.io.Encoding;

public class HTMLTextExtractionOperator extends AbstractTokenProcessor {

    public static final String PARAMETER_EXTRACT_CONTENT = "extract_content";

    public static final String PARAMETER_MIN_LENGTH = "minimum_text_block_length";

    public static final String PARAMETER_OVERRIDE_CONTENT_TYPE_INFORMATION = "override_content_type_information";

    public static final String PARAMETER_NEGLEGT_SPAN_TAGS = "neglegt_span_tags";

    public static final String PARAMETER_NEGLECT_P_TAGS = "neglect_p_tags";

    public static final String PARAMETER_NEGLECT_B_TAGS = "neglect_b_tags";

    public static final String PARAMETER_NEGLECT_I_TAGS = "neglect_i_tags";

    public static final String PARAMETER_NEGLECT_BR_TAGS = "neglect_br_tags";

    public static final String PARAMETER_IGNORE_NON_HTML_TAGS = "ignore_non_html_tags";

    private static final String META_DATA_HTML_TITLE = "Title";

    private static final String META_DATA_HTML_LANGUAGE = "Language";

    private static final String META_DATA_HTML_DESCRIPTION = "Description";

    private static final String META_DATA_HTML_KEYWORDS = "Keywords";

    private static final String META_DATA_HTML_ROBOTS = "Robots";

    private static final Set<String> knownTags = new HashSet<String>();

    static {
        knownTags.add("abbr");
        knownTags.add("acronym");
        knownTags.add("address");
        knownTags.add("applet");
        knownTags.add("area");
        knownTags.add("b");
        knownTags.add("base");
        knownTags.add("basefont");
        knownTags.add("bdo");
        knownTags.add("big");
        knownTags.add("blockquote");
        knownTags.add("body");
        knownTags.add("br");
        knownTags.add("button");
        knownTags.add("caption");
        knownTags.add("center");
        knownTags.add("cite");
        knownTags.add("code");
        knownTags.add("col");
        knownTags.add("colgroup");
        knownTags.add("dd");
        knownTags.add("del");
        knownTags.add("dfn");
        knownTags.add("dir");
        knownTags.add("div");
        knownTags.add("dl");
        knownTags.add("dt");
        knownTags.add("em");
        knownTags.add("fieldset");
        knownTags.add("font");
        knownTags.add("form");
        knownTags.add("frame");
        knownTags.add("frameset");
        knownTags.add("h1");
        knownTags.add("h2");
        knownTags.add("h3");
        knownTags.add("h4");
        knownTags.add("h5");
        knownTags.add("h6");
        knownTags.add("head");
        knownTags.add("hr");
        knownTags.add("html");
        knownTags.add("i");
        knownTags.add("iframe");
        knownTags.add("img");
        knownTags.add("input");
        knownTags.add("ins");
        knownTags.add("isindex");
        knownTags.add("kbd");
        knownTags.add("label");
        knownTags.add("legend");
        knownTags.add("li");
        knownTags.add("link");
        knownTags.add("map");
        knownTags.add("menu");
        knownTags.add("meta");
        knownTags.add("noframes");
        knownTags.add("noscript");
        knownTags.add("object");
        knownTags.add("ol");
        knownTags.add("optgroup");
        knownTags.add("option");
        knownTags.add("p");
        knownTags.add("param");
        knownTags.add("pre");
        knownTags.add("q");
        knownTags.add("s");
        knownTags.add("samp");
        knownTags.add("script");
        knownTags.add("select");
        knownTags.add("small");
        knownTags.add("span");
        knownTags.add("strike");
        knownTags.add("strong");
        knownTags.add("style");
        knownTags.add("sub");
        knownTags.add("sup");
        knownTags.add("table");
        knownTags.add("tbody");
        knownTags.add("td");
        knownTags.add("textarea");
        knownTags.add("tfoot");
        knownTags.add("th");
        knownTags.add("thead");
        knownTags.add("title");
        knownTags.add("tr");
        knownTags.add("tt");
        knownTags.add("u");
        knownTags.add("ul");
        knownTags.add("var");
    }

    /**
	 * indicates whether the encoding changed by extracting new content type information
	 */
    private boolean encodingChanged = false;

    public HTMLTextExtractionOperator(OperatorDescription description) {
        super(description);
    }

    @Override
    protected Document doWork(Document document) throws OperatorException {
        String text = document.getText();
        Matcher matcher = null;
        if (getParameterAsBoolean(PARAMETER_OVERRIDE_CONTENT_TYPE_INFORMATION)) {
            String previousEncoding = URLConnectionProvider.DEFAULT_ENCODING;
            if (document.getMetaDataKeys().contains(GetWebpageOperator.META_DATA_CONTENT_TYPE)) {
                previousEncoding = URLConnectionProvider.parseEncoding((String) document.getMetaDataValue(GetWebpageOperator.META_DATA_CONTENT_TYPE));
            }
            matcher = Pattern.compile("<meta[\\s]*http-equiv=[\"]*([^>]*?)[\"]*[\\s]*content=[\"]*([^>]*?)[\"]*[\\s]*?[/]*?>").matcher(text.toLowerCase());
            while (matcher.find()) {
                if (matcher.groupCount() >= 2) {
                    String key = matcher.group(1).trim();
                    String value = matcher.group(2).trim();
                    if (key.toLowerCase().equals("content-type")) {
                        String encoding = URLConnectionProvider.parseEncoding(value.replace("\"", ""));
                        try {
                            text = new String(text.getBytes(previousEncoding), Encoding.getEncoding(encoding));
                            encodingChanged = true;
                        } catch (UnsupportedEncodingException e) {
                            text = new String(text.getBytes(), Encoding.getEncoding(encoding));
                        } catch (IllegalCharsetNameException e) {
                        } catch (UnsupportedCharsetException e) {
                        }
                        document.addMetaData(GetWebpageOperator.META_DATA_CONTENT_TYPE, value, Ontology.NOMINAL);
                        break;
                    }
                }
            }
        }
        int titleStart = text.toLowerCase().indexOf("<title>") + "<title>".length();
        int titleEnd = text.toLowerCase().indexOf("</title>");
        if (titleStart >= 0 && titleEnd >= 0 && titleStart < titleEnd) {
            String title = text.substring(titleStart, titleEnd).trim();
            if (title != null && !title.isEmpty()) {
                document.addMetaData(META_DATA_HTML_TITLE, StringEscapeUtils.unescapeHtml(title), Ontology.NOMINAL);
            } else {
                document.addMetaData(META_DATA_HTML_TITLE, (String) null, Ontology.NOMINAL);
            }
        } else {
            document.addMetaData(META_DATA_HTML_TITLE, (String) null, Ontology.NOMINAL);
        }
        document.addMetaData(META_DATA_HTML_LANGUAGE, (String) null, Ontology.NOMINAL);
        document.addMetaData(META_DATA_HTML_DESCRIPTION, (String) null, Ontology.NOMINAL);
        document.addMetaData(META_DATA_HTML_KEYWORDS, (String) null, Ontology.NOMINAL);
        document.addMetaData(META_DATA_HTML_ROBOTS, (String) null, Ontology.NOMINAL);
        matcher = Pattern.compile("<(meta|META)[\\s]*(HTTP-EQUIV|http-equiv|NAME|name)=[\"]*([^>]*?)[\"]*[\\s]*(content|CONTENT)=[\"]*([^>]*?)[\"]*[\\s]*?[/]*?>").matcher(text);
        while (matcher.find()) {
            if (matcher.groupCount() >= 5) {
                String key = matcher.group(3).trim();
                String value = matcher.group(5).trim();
                if (key != null && !key.isEmpty()) {
                    if (key.toLowerCase().equals("language")) {
                        document.addMetaData(META_DATA_HTML_LANGUAGE, value, Ontology.NOMINAL);
                        continue;
                    }
                    if (key.toLowerCase().equals("description")) {
                        document.addMetaData(META_DATA_HTML_DESCRIPTION, StringEscapeUtils.unescapeHtml(value), Ontology.NOMINAL);
                        continue;
                    }
                    if (key.toLowerCase().equals("keywords")) {
                        document.addMetaData(META_DATA_HTML_KEYWORDS, StringEscapeUtils.unescapeHtml(value), Ontology.NOMINAL);
                        continue;
                    }
                    if (key.toLowerCase().equals("robots")) {
                        document.addMetaData(META_DATA_HTML_ROBOTS, value, Ontology.NOMINAL);
                        continue;
                    }
                }
            }
        }
        if (getParameterAsBoolean(PARAMETER_EXTRACT_CONTENT)) {
            text = text.replaceAll("<!--[\\s\\S]?-->", "");
            text = text.replaceAll("<style.*?>[\\s\\S]*?</style>", "");
            text = text.replaceAll("<script.*?>[\\s\\S]*?</script>", "");
            text = text.replaceAll("<img[^>]*?>", "");
            text = text.replaceAll("<a[^>]*?>(.*?)<[\\s]*/a>", " $1 ");
            if (getParameterAsBoolean(PARAMETER_NEGLEGT_SPAN_TAGS)) {
                text = text.replaceAll("<[/]*[span|SPAN][^>]*?>(.*?)<[\\s]*/span>", " $1 ");
            }
            if (getParameterAsBoolean(PARAMETER_NEGLECT_P_TAGS)) {
                text = text.replaceAll("<[/]*[p|P][^a-zA-Z>]*?>", " ");
            }
            if (getParameterAsBoolean(PARAMETER_NEGLECT_B_TAGS)) {
                text = text.replaceAll("<[/]*[b|B][^a-zA-Z>]*?>", " ");
            }
            if (getParameterAsBoolean(PARAMETER_NEGLECT_I_TAGS)) {
                text = text.replaceAll("<[/]*[i|I][^a-zA-Z>]*?>", " ");
            }
            if (getParameterAsBoolean(PARAMETER_NEGLECT_BR_TAGS)) {
                text = text.replaceAll("<(br|BR)[^>]*?>", " ");
            }
            if (getParameterAsBoolean(PARAMETER_IGNORE_NON_HTML_TAGS)) {
                matcher = Pattern.compile("</?([A-Za-z]*)?([^>]*?)>").matcher(text);
                while (matcher.find()) {
                    if (matcher.groupCount() > 0) {
                        if (!knownTags.contains(matcher.group(1).toLowerCase())) {
                            text = text.replace(matcher.group(0), "");
                        }
                    }
                }
            }
            text = text.replaceAll("(<[^>]*?>)", " <tag> ");
            text = text.replaceAll("[\r]*[\n]+", " ");
            text = text.replaceAll("[\\s]+", " ");
            String[] words = text.split("\\s");
            int[] lengths = new int[words.length];
            for (int i = 0; i < words.length; i++) {
                if (words[i].equals("<tag>")) {
                    lengths[i] = 0;
                } else {
                    lengths[i] = (i > 0 ? lengths[i - 1] : 0) + 1;
                }
            }
            for (int i = words.length - 2; i >= 0; i--) {
                if (lengths[i] != 0 && lengths[i] < lengths[i + 1]) {
                    lengths[i] = lengths[i + 1];
                }
            }
            int minLength = getParameterAsInt(PARAMETER_MIN_LENGTH);
            List<Token> tokens = new LinkedList<Token>();
            for (int i = 0; i < words.length; i++) {
                if (lengths[i] >= minLength) {
                    StringBuffer buf = new StringBuffer();
                    for (int j = 0; j < lengths[i]; j++) {
                        buf.append(words[i++] + " ");
                        if (i >= lengths.length) {
                            break;
                        }
                    }
                    tokens.add(new Token(StringEscapeUtils.unescapeHtml(buf.toString().trim()), 1));
                }
            }
            return new Document(tokens, new Document(Collections.singletonList(new Token(text, 1f)), document));
        } else {
            if (encodingChanged) {
                Document newDocument = new Document(Collections.singletonList(new Token(text, 1f)), new Document(Collections.singletonList(new Token(text, 1f)), document));
                newDocument.getText();
                return newDocument;
            } else {
                return document;
            }
        }
    }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = new LinkedList<ParameterType>();
        types.add(new ParameterTypeBoolean(PARAMETER_EXTRACT_CONTENT, "Specifies whether content is extracted or not", true, false));
        ParameterType type = new ParameterTypeInt(PARAMETER_MIN_LENGTH, "The minimum length (in words/tokens) of text blocks.", 1, Integer.MAX_VALUE, 5, false);
        type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_EXTRACT_CONTENT, false, true));
        types.add(type);
        types.add(new ParameterTypeBoolean(PARAMETER_OVERRIDE_CONTENT_TYPE_INFORMATION, "Specifies whether potentially existing content type information and used encoding information should be overriden using the HTML meta http-equiv tag.", true, false));
        types.add(new ParameterTypeBoolean(PARAMETER_NEGLEGT_SPAN_TAGS, "Specifies whether <span> tags should be neglected or used as text block divider.", true, false));
        types.add(new ParameterTypeBoolean(PARAMETER_NEGLECT_P_TAGS, "Specifies whether <p> tags should be neglected or used as text block divider.", true, false));
        types.add(new ParameterTypeBoolean(PARAMETER_NEGLECT_B_TAGS, "Specifies whether <b> tags should be neglected or used as text block divider.", true, false));
        types.add(new ParameterTypeBoolean(PARAMETER_NEGLECT_I_TAGS, "Specifies whether <i> tags should be neglected or used as text block divider.", true, false));
        types.add(new ParameterTypeBoolean(PARAMETER_NEGLECT_BR_TAGS, "Specifies whether <br> tags should be neglected or used as text block divider.", true, false));
        types.add(new ParameterTypeBoolean(PARAMETER_IGNORE_NON_HTML_TAGS, "Specifies whether tags that are not common HTML should be ignored.", true, false));
        return types;
    }
}
