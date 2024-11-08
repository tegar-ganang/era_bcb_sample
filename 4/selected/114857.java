package org.qtitools.playr.rendering.xhtml;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.qtitools.playr.rendering.core.ItemData;
import org.qtitools.playr.rendering.core.Renderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XHTMLRenderer extends Renderer {

    public static String RENDER_FOOTER = "render.footer";

    public static String RENDER_HEADER = "render.header";

    public static String RENDER_BODY = "render.body";

    public static String HEADER_TEMPLATE = "render.header.template";

    public static String BODY_TEMPLATE = "render.body.template";

    public static String FOOTER_TEMPLATE = "render.footer.template";

    public static String UNBOUNDED_PV_REPLACEMENT = "render.unbounded.variable.replacement";

    public static String RENDER_ASSESSMENT_FEEDBACK_TITLE = "render.assessmentFeedback.title";

    public static String RENDER_TESTPART_FEEDBACK_TITLE = "render.testPartFeedback.title";

    boolean render_header = false;

    boolean render_footer = false;

    boolean render_body = false;

    String header_template;

    String footer_template;

    String body_template;

    Properties renderProperties = new Properties();

    protected Logger logger = LoggerFactory.getLogger(XHTMLRenderer.class);

    public XHTMLRenderer(String settingsFile) {
        super(settingsFile);
        String settings_file = getRendererSettingsDirectory() + File.separator + settingsFile;
        try {
            renderProperties.load(new FileInputStream(settings_file));
        } catch (FileNotFoundException e) {
            logger.error("Unable to locate renderer settings:" + settings_file + ". Will default to rendering nothing!");
        } catch (IOException e) {
            logger.error("Error reading renderer settings:" + settings_file + ". Will default to rendering nothing!");
        }
        render_footer = Boolean.parseBoolean(renderProperties.getProperty(RENDER_FOOTER, "false"));
        render_header = Boolean.parseBoolean(renderProperties.getProperty(RENDER_HEADER, "false"));
        render_body = Boolean.parseBoolean(renderProperties.getProperty(RENDER_BODY, "false"));
        header_template = renderProperties.getProperty(HEADER_TEMPLATE);
        footer_template = renderProperties.getProperty(FOOTER_TEMPLATE);
        body_template = renderProperties.getProperty(BODY_TEMPLATE);
        logger.debug("xhtml-renderer init complete");
        logger.debug("Header: " + render_header + " " + header_template);
        logger.debug("Body: " + render_body + " " + body_template);
        logger.debug("Footer: " + render_footer + " " + footer_template);
    }

    @Override
    public byte[] render(ItemData id, String view) {
        String xhtml;
        if (id.r2q2_data == null) {
            xhtml = renderHeader(id, view) + renderTestComplete(id, view) + renderFooter(id, view);
        } else {
            xhtml = renderHeader(id, view) + renderBody(id, view) + renderFooter(id, view);
        }
        try {
            return xhtml.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    String renderHeader(ItemData id, String view) {
        if (render_header) {
            return renderTemplate(id, readTemplate(header_template), view);
        }
        return "";
    }

    String renderBody(ItemData id, String view) {
        if (render_body) {
            return renderTemplate(id, readTemplate(body_template), view);
        }
        return "";
    }

    String renderFooter(ItemData id, String view) {
        if (render_footer) {
            return renderTemplate(id, readTemplate(footer_template), view);
        }
        return "";
    }

    String renderTemplate(ItemData id, String template, String view) {
        if (id == null) return template;
        String rubric = renderRubric(id, view);
        if (rubric == null || rubric.length() == 0) {
            template = deleteBlock(template, "RUBRIC");
        } else {
            template = replaceBlock(template, "RUBRIC", rubric);
        }
        if (id.r2q2_data != null && id.r2q2_data.length != 0) {
            template = replaceBlock(template, "QUESTION", new String(id.r2q2_data));
        } else {
            template = deleteBlock(template, "QUESTION");
        }
        if (id.qId != null) {
            template = replaceBlock(template, "QUESTION_ID", id.qId);
        } else {
            template = deleteBlock(template, "QUESTION_ID");
        }
        if (id.test_title != null) {
            template = replaceBlock(template, "TEST_TITLE", id.test_title);
        } else {
            template = deleteBlock(template, "TEST_TITLE");
        }
        if (id.section_titles != null) {
            String titles = "";
            for (String t : id.section_titles) titles += t + "<br />";
            template = replaceBlock(template, "SECTION_TITLES", titles);
        } else {
            template = deleteBlock(template, "SECTION_TITLES");
        }
        String feedback = renderFeedback(id);
        if (feedback == null || feedback.length() == 0) {
            template = deleteBlock(template, "FEEDBACK");
        } else {
            template = replaceBlock(template, "FEEDBACK", feedback);
        }
        if (id.numItemsSelected > 0) {
            template = replaceBlock(template, "NUM_ITEMS_SELECTED", Integer.toString(id.numItemsSelected));
        } else {
            template = deleteBlock(template, "NUM_ITEMS_SELECTED");
        }
        if (id.numItemsRemaining >= 0) {
            template = replaceBlock(template, "NUM_ITEMS_REMAINING", Integer.toString(id.numItemsRemaining));
        } else {
            template = deleteBlock(template, "NUM_ITEMS_REMAINING");
        }
        if (id.millisSelected >= 0) {
            template = replaceBlock(template, "MILLIS_SELECTED", Long.toString(id.millisSelected));
        } else {
            template = deleteBlock(template, "MILLIS_SELECTED");
        }
        if (id.millisRemaining >= 0) {
            template = replaceBlock(template, "MILLIS_REMAINING", Long.toString(id.millisRemaining));
        } else {
            template = deleteBlock(template, "MILLIS_REMAINING");
        }
        if (id.millisSelected > 0) {
            template = replaceBlock(template, "MILLIS_SELECTED_FORMATTED", formatTime(id.millisSelected));
        } else {
            template = deleteBlock(template, "MILLIS_SELECTED_FORMATTED");
        }
        if (id.millisRemaining > 0) {
            template = replaceBlock(template, "MILLIS_REMAINING_FORMATTED", formatTime(id.millisRemaining));
        } else {
            template = deleteBlock(template, "MILLIS_REMAINING_FORMATTED");
        }
        template = deleteBlock(template, "TEST_COMPLETE");
        if (id.previousEnabled) {
            template = replaceBlock(template, "PREVIOUS_ENABLED", "");
            template = deleteBlock(template, "PREVIOUS_DISABLED");
        } else {
            template = replaceBlock(template, "PREVIOUS_DISABLED", "");
            template = deleteBlock(template, "PREVIOUS_ENABLED");
        }
        if (id.backwardsEnabled) {
            template = replaceBlock(template, "BACKWARDS_ENABLED", "");
            template = deleteBlock(template, "BACKWARDS_DISABLED");
        } else {
            template = replaceBlock(template, "BACKWARDS_DISABLED", "");
            template = deleteBlock(template, "BACKWARDS_ENABLED");
        }
        if (id.nextEnabled) {
            template = replaceBlock(template, "NEXT_ENABLED", "");
            template = deleteBlock(template, "NEXT_DISABLED");
        } else {
            template = replaceBlock(template, "NEXT_DISABLED", "");
            template = deleteBlock(template, "NEXT_ENABLED");
        }
        if (id.forwardsEnabled) {
            template = replaceBlock(template, "FORWARDS_ENABLED", "");
            template = deleteBlock(template, "FORWARDS_DISABLED");
        } else {
            template = replaceBlock(template, "FORWARDS_DISABLED", "");
            template = deleteBlock(template, "FORWARDS_ENABLED");
        }
        if (id.submitEnabled) {
            template = replaceBlock(template, "SUBMIT_ENABLED", "");
            template = deleteBlock(template, "SUBMIT_DISABLED");
        } else {
            template = replaceBlock(template, "SUBMIT_DISABLED", "");
            template = deleteBlock(template, "SUBMIT_ENABLED");
        }
        if (id.skipEnabled) {
            template = replaceBlock(template, "SKIP_ENABLED", "");
            template = deleteBlock(template, "SKIP_DISABLED");
        } else {
            template = replaceBlock(template, "SKIP_DISABLED", "");
            template = deleteBlock(template, "SKIP_ENABLED");
        }
        return template;
    }

    public enum View {

        AUTHOR("author"), CANDIDATE("candidate"), PROTOR("proctor"), SCORER("scorer"), TESTCONSTRUCTOR("testConstructor"), TUTOR("tutor");

        private final String name;

        private static Map<String, View> nameLookup = new HashMap<String, View>();

        static {
            for (View ty : View.values()) {
                nameLookup.put(ty.view_xml(), ty);
            }
        }

        View(String name) {
            this.name = name;
        }

        public String view_name() {
            return name;
        }

        public String view_xml() {
            return "<rubricBlock view=\"" + view_name() + "\">";
        }

        public static View parseString(String str) {
            return nameLookup.get(str);
        }
    }

    String renderRubric(ItemData id, String view) {
        if (id.rubricBlocks == null) return null;
        Map<String, String> rubricdata = new HashMap<String, String>();
        for (String[] sa : id.rubricBlocks) {
            for (String s : sa) {
                s = s.replace("</rubricBlock>", "");
                s = s.trim();
                View v = View.parseString(s.substring(0, s.indexOf(">") + 1));
                s = s.replace(v.view_xml(), "").trim();
                String rub = "";
                if (rubricdata.containsKey(v.view_name())) rub = rubricdata.get(v.view_name());
                rubricdata.put(v.view_name(), rub + s);
            }
        }
        String rubric = "";
        if (view == null || view.compareTo("*") == 0) {
            for (String key : rubricdata.keySet()) {
                rubric += "<div class=\"" + key + "\">" + rubricdata.get(key) + "</div>";
            }
        } else {
            if (rubricdata.get(view) == null) return null;
            rubric = "<div class=\"" + view + "\">" + rubricdata.get(view) + "</div>";
        }
        rubric = renderPrintedVariables(rubric);
        rubric = processImages(rubric, id.ImageBasePath);
        return rubric;
    }

    /**
	 * Render feedback, with optional titles
	 * @param id
	 * @return
	 */
    String renderFeedback(ItemData id) {
        if (id.assessmentFeedback == null && id.testPartFeedback == null) return null;
        String feedback = "";
        if (id.assessmentFeedback != null) {
            String title = "<div class=\"feedback_title\">$1</div>";
            if (renderProperties.getProperty(RENDER_ASSESSMENT_FEEDBACK_TITLE, "true").equals("false")) title = "";
            for (String fb : id.assessmentFeedback) {
                fb = fb.replace("</testFeedback>", "</div>");
                fb = fb.replaceAll("(?s)<testFeedback.*?title=\"(.*?)\".*?>", "<div class=\"feedback_assessmentTest\">" + title);
                feedback += fb;
            }
        }
        if (id.testPartFeedback != null) {
            String title = "<div class=\"feedback_title\">$1</div>";
            if (renderProperties.getProperty(RENDER_TESTPART_FEEDBACK_TITLE, "true").equals("false")) title = "";
            for (String fb : id.testPartFeedback) {
                fb = fb.replace("</testFeedback>", "</div>");
                fb = fb.replaceAll("(?s)<testFeedback.*?title=\"(.*?)\".*?>", "<div class=\"feedback_testPart\">" + title);
                feedback += fb;
            }
        }
        feedback = feedback.replaceAll("(?s)<testFeedback.*?>", "<div class=\"feedback_testPart\">");
        feedback = renderPrintedVariables(feedback);
        feedback = processImages(feedback, id.ImageBasePath);
        return feedback;
    }

    /**
	 * Render a page to show the test is finished (with optional feedback according to template)
	 * @param id
	 * @param view
	 * @return
	 */
    String renderTestComplete(ItemData id, String view) {
        if (render_body) {
            String body = readTemplate(body_template);
            body = deleteBlock(body, "RUBRIC");
            body = deleteBlock(body, "QUESTION");
            body = replaceBlock(body, "TEST_COMPLETE", "");
            return renderTemplate(id, body, view);
        }
        return "";
    }

    /**
	 * Replace a template block
	 * @param body
	 * @param tag
	 * @param replace
	 * @return
	 */
    String replaceBlock(String body, String tag, String replace) {
        body = body.replace("[[[BEGIN_" + tag + "]]]", "");
        body = body.replace("[[[" + tag + "]]]", replace);
        body = body.replace("[[[END_" + tag + "]]]", "");
        return body;
    }

    /**
	 * Delete a template block
	 * @param body
	 * @param tag
	 * @return
	 */
    String deleteBlock(String body, String tag) {
        int startidx = 0;
        int stopidx = 0;
        while ((startidx = body.indexOf("[[[BEGIN_" + tag + "]]]", startidx)) != -1) {
            stopidx = body.indexOf("[[[END_" + tag + "]]]", startidx);
            if (stopidx == -1) {
                logger.warn("unterminated " + tag);
                body = body.replace(body.substring(startidx), "");
            } else {
                stopidx += ("[[[END_" + tag + "]]]").length();
                body = body.replace(body.substring(startidx, stopidx), "");
            }
            startidx = stopidx;
        }
        return body;
    }

    String readTemplate(String file) {
        File f = new File(getRendererSettingsDirectory() + File.separator + file);
        if (!f.canRead()) {
            f = new File(file);
            if (!f.canRead()) {
                return "<div>Unable to load template file " + file + "</div>";
            }
        }
        try {
            byte[] bytes = loadBytesFromStream(new FileInputStream(f));
            return new String(bytes);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    byte[] loadBytesFromStream(InputStream in) throws IOException {
        int chunkSize = 4096;
        int count;
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] b = new byte[chunkSize];
        try {
            while ((count = in.read(b, 0, chunkSize)) > 0) bo.write(b, 0, count);
            return bo.toByteArray();
        } finally {
            bo.close();
            bo = null;
        }
    }

    String processImages(String in, String img_base) {
        String out = in;
        Pattern p = Pattern.compile("(?s)<img(.*)src=([\"\'])(.*?)([\"\'])(.*?)>");
        Matcher m = p.matcher(in);
        while (m.find()) {
            String href = m.group(3);
            if (!href.contains("://") || !href.startsWith("/")) {
                out = out.replaceAll("(?s)<img(.*)src=([\"\'])(.*?)([\"\'])(.*?)>", "<img$1src=$2" + img_base + "$3$4$5>");
            }
        }
        return out;
    }

    String renderPrintedVariables(String in) {
        String out = in.replaceAll("<printedVariable (.*?)/>", renderProperties.getProperty(UNBOUNDED_PV_REPLACEMENT, "<span class=\"error unbound\">&lt;unbound printed variable&gt;</span>"));
        Pattern p = Pattern.compile("(?s)<printedVariable (.*?)>(.*?)<baseValue baseType=\"(.*?)\">(.*?)</baseValue>(.*?)</printedVariable>");
        Matcher m = p.matcher(out);
        while (m.find()) {
            String attrs = m.group(1);
            String type = m.group(3);
            String value = m.group(4);
            Pattern p2 = Pattern.compile("format=\"(.*?)\"");
            Matcher m2 = p2.matcher(attrs);
            String format = (m2.find()) ? m2.group(1) : null;
            p2 = Pattern.compile("base=\"(.*?)\"");
            m2 = p2.matcher(attrs);
            String base = (m2.find()) ? m2.group(1) : null;
            p2 = Pattern.compile("identifier=\"(.*?)\"");
            m2 = p2.matcher(attrs);
            String identifier = (m2.find()) ? m2.group(1) : null;
            String formattedValue = formatString(value, format, base, type);
            formattedValue = "<span class=\"printedVariable " + type + "\" id=\"" + identifier + "\">" + formattedValue + "</span>";
            out = out.replace("<printedVariable " + attrs + ">" + m.group(2) + "<baseValue baseType=\"" + type + "\">" + value + "</baseValue>" + m.group(5) + "</printedVariable>", formattedValue);
            m = p.matcher(out);
        }
        return out;
    }

    String formatString(String value, String format, String base, String type) {
        if (type.equals("float") && format != null && !format.equals("")) {
            Object[] args = new Object[1];
            args[0] = Float.parseFloat(value);
            return String.format(format, args);
        } else if (type.equals("integer") && format != null && !format.equals("")) {
            Object[] args = new Object[1];
            args[0] = Integer.parseInt(value);
            if (base != null && !base.equals("") && format != null && format.contains("i")) {
                format = format.replace("i", "s");
                args[0] = Integer.toString((Integer) args[0], Integer.parseInt(base));
            }
            return String.format(format, args);
        } else {
            return value;
        }
    }

    /**
	  * Format a time as hrs:mins:secs
	  * @param milliseconds Time to format
	  * @return formatted string
	  */
    String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        String s = String.format("%1$02d:%2$02d:%3$02d", seconds / (60 * 60), (seconds / 60) % 60, seconds % 60);
        return s;
    }

    public static void main(String[] args) {
        System.setProperty("catalina.home", "/usr/local/apache-tomcat-5.5.23/");
        XHTMLRenderer r = new XHTMLRenderer(null);
        String s = "<p>Space probe New Horizons was launched in 2006 to investigate Pluto. It will pass close to Jupiter and is due to arrive near Pluto in 2015.<br/><img alt=\"New Horizons blast off\" height=\"282\" src=\"images/rocket.png\" width=\"300\"/></p>";
        System.out.println(r.processImages(s, null));
    }
}
