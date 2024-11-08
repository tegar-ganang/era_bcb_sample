package it.infodea.tapestrydea.components;

import it.infodea.tapestrydea.services.breadcrumb.BreadcrumbService;
import it.infodea.tapestrydea.services.breadcrumb.bean.BreadcrumbBean;
import java.util.Iterator;
import org.apache.tapestry5.BindingConstants;
import org.apache.tapestry5.MarkupWriter;
import org.apache.tapestry5.annotations.IncludeStylesheet;
import org.apache.tapestry5.annotations.Parameter;
import org.apache.tapestry5.corelib.base.AbstractLink;
import org.apache.tapestry5.dom.Element;
import org.apache.tapestry5.ioc.Messages;
import org.apache.tapestry5.ioc.annotations.Inject;

/**
 * Used to display the Breadcrumb
 * 
 * @author rob
 * 
 */
@IncludeStylesheet(value = "../assets/styles/tapestrydea.css")
public class BreadcrumbDisplayDea extends AbstractLink {

    @Parameter(defaultPrefix = BindingConstants.LITERAL, value = ">")
    private String separator;

    @Parameter(defaultPrefix = BindingConstants.LITERAL, value = "true")
    private boolean linkLast;

    @Inject
    private BreadcrumbService breadcrumbService;

    @Inject
    private Messages messages;

    void afterRender(MarkupWriter writer) {
        boolean test = false;
        Iterator<BreadcrumbBean> iterator = breadcrumbService.iterator();
        Element header = writer.element("span");
        header.addClassName("breadcrumb-header");
        writer.end();
        Element breadcrumb = writer.element("span");
        breadcrumb.addClassName("breadcrumb");
        while (iterator.hasNext()) {
            BreadcrumbBean bread = (BreadcrumbBean) iterator.next();
            String label = bread.getLabel();
            if (label != null) {
                test = true;
                boolean hasNext = iterator.hasNext();
                if (linkLast) {
                    writeLink(writer, bread.getLink());
                }
                Element labelSpan = writer.element("span");
                labelSpan.addClassName("breadcrumb-label");
                writer.write(label.toLowerCase());
                writer.end();
                if (linkLast) {
                    writer.end();
                }
                if (hasNext) {
                    writer.write(separator);
                }
            }
        }
        writer.end();
        if (test) {
            header.text(messages.get("breadcrumb-initial-message"));
        }
    }
}
