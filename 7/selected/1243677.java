package de.iritgo.aktera.struts.tags.html;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.servlet.jsp.JspException;
import lombok.Getter;
import lombok.Setter;
import org.apache.struts.Globals;
import org.apache.struts.taglib.TagUtils;

/**
 * OptionsCollection tag with localization support.
 */
public class OptionsCollectionTag extends org.apache.struts.taglib.html.OptionsCollectionTag {

    /** An option. */
    protected class Option {

        public String label;

        public String value;

        public boolean selected;

        public Option(String label, String value, boolean selected) {
            this.label = label;
            this.value = value;
            this.selected = selected;
        }
    }

    /** */
    private static final long serialVersionUID = 1L;

    /**
	 * The name of the servlet context attribute containing our message
	 * resources.
	 */
    protected String bundle = Globals.MESSAGES_KEY;

    /**
	 * The name of the attribute containing the Locale to be used for
	 * looking up internationalized messages.
	 */
    protected String locale = Globals.LOCALE_KEY;

    /** Collected options. */
    protected List options;

    public String getBundle() {
        return this.bundle;
    }

    public void setBundle(String bundle) {
        this.bundle = bundle;
    }

    public String getLocale() {
        return this.locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    @Setter
    @Getter
    protected boolean sort = true;

    /**
	 * Release any acquired resources.
	 */
    @Override
    public void release() {
        super.release();
        sort = true;
    }

    /**
	 * Overridden method.
	 *
	 * @param sb StringBuffer accumulating our results
	 * @param value Value to be returned to the server for this option
	 * @param label Value to be shown to the user for this option
	 * @param matched Should this value be marked as selected?
	 */
    @Override
    protected void addOption(StringBuffer sb, String label, String value, boolean matched) {
        try {
            if (label.startsWith("$")) {
                String optionBundle = bundle;
                int colonIndex = label.indexOf(":");
                if (colonIndex != -1) {
                    optionBundle = label.substring(1, colonIndex);
                    label = label.substring(colonIndex);
                }
                String[] args = label.split("\\|");
                if (args.length > 1) {
                    String[] args2 = new String[args.length - 1];
                    for (int i = 0; i < args2.length; ++i) {
                        args2[i] = args[i + 1].replaceAll("\\\\\\|", "\\|");
                    }
                    label = TagUtils.getInstance().message(pageContext, optionBundle, locale, args[0].substring(1), args2);
                } else {
                    label = TagUtils.getInstance().message(pageContext, optionBundle, locale, label.substring(1));
                }
            }
        } catch (JspException x) {
        }
        if (getParent() == null || !(getParent() instanceof de.iritgo.aktera.struts.tags.html.SelectTag) || !((SelectTag) getParent()).getReadOnly()) {
            options.add(new Option(label, value, matched));
        } else if (matched) {
            try {
                TagUtils.getInstance().write(pageContext, label);
            } catch (JspException x) {
            }
        }
    }

    /**
	 * Process the start of this tag.
	 *
	 * @exception JspException if a JSP exception has occurred
	 */
    @Override
    public int doStartTag() throws JspException {
        options = new LinkedList();
        super.doStartTag();
        if (sort) {
            Collections.sort(options, new Comparator() {

                public int compare(Object o1, Object o2) {
                    Option opt1 = (Option) o1;
                    Option opt2 = (Option) o2;
                    if (opt1 == null || opt1.label == null || opt2 == null || opt2.label == null) {
                        return 0;
                    }
                    return opt1.label.compareTo(opt2.label);
                }
            });
        }
        StringBuffer sb = new StringBuffer();
        for (Iterator i = options.iterator(); i.hasNext(); ) {
            Option o = (Option) i.next();
            super.addOption(sb, o.label, o.value, o.selected);
        }
        TagUtils.getInstance().write(pageContext, sb.toString());
        return SKIP_BODY;
    }
}
