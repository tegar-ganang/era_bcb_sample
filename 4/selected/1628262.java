package annone.server.web;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import annone.engine.Channel;
import annone.engine.ComponentId;
import annone.engine.ids.MethodId;
import annone.html.A;
import annone.html.BODY;
import annone.html.DIV;
import annone.html.H1;
import annone.html.HEAD;
import annone.html.HTML;
import annone.html.LINK;
import annone.html.Nbsp;
import annone.html.Plain;
import annone.html.SCRIPT;
import annone.html.TITLE;
import annone.http.HttpRequest;
import annone.http.HttpResponse;
import annone.http.HttpSeeOtherException;
import annone.util.Const;
import annone.util.Text;

public abstract class PageBind implements PathBind {

    protected DIV container;

    protected DIV header;

    protected DIV content;

    protected DIV extra;

    protected DIV footer;

    protected A logo;

    protected DIV options;

    protected DIV user;

    protected HTML html;

    protected HEAD head;

    protected BODY body;

    protected TITLE title;

    protected void checkChannelOrRedirect(WebContext context) {
        Channel channel = context.getChannel();
        if (channel == null) throw new HttpSeeOtherException("Channel not open.", "/");
    }

    protected void createPage(WebContext context) {
        logo = new A(new H1(new Plain("Annone")));
        logo.setHRef("/?channelId=" + context.getChannel().getId());
        logo.setId("logo");
        A optionsLink = new A(new Plain(Text.get(context.getLocale(), "Options")));
        optionsLink.setHRef("/o");
        A helpLink = new A(new Plain(Text.get(context.getLocale(), "Help")));
        helpLink.setHRef("/h");
        options = new DIV(optionsLink, new Nbsp(), new Plain("|"), new Nbsp(), helpLink);
        options.setId("options");
        user = new DIV(new Plain(Text.get(context.getLocale(), "User")));
        user.setId("user");
        header = new DIV();
        content = new DIV();
        extra = new DIV();
        footer = new DIV();
        container = new DIV();
        header.setId("header");
        content.setId("content");
        extra.setId("extra");
        footer.setId("footer");
        container.setId("container");
        header.add(logo, options, user);
        footer.add(new Plain(Text.get(context.getLocale(), "Copyright © 2010 theCar")));
        container.add(header, content, extra, footer);
        title = new TITLE();
        head = new HEAD(title, new LINK("stylesheet", "/c"));
        addScript("/scripts/prototype.js");
        addScript("/scripts/controls/Control.js");
        addScript("/scripts/controls/Value.js");
        addScript("/scripts/controls/Box.js");
        addScript("/scripts/controls/Edit.js");
        body = new BODY(container);
        html = new HTML(head, body);
        html.setDoctype(HTML.HTML_4_01_STRICT);
        html.setLang(context.getLocale().getLanguage());
    }

    protected void addScript(String src) {
        SCRIPT script = new SCRIPT();
        script.setSrc(src);
        script.setType("text/javascript");
        head.add(script);
    }

    protected void setTitle(String text) {
        title.removeAll();
        title.add(new Plain(String.format("%s | Annone", text)));
    }

    protected void updateResponse(WebContext context, int statusCode) {
        HttpResponse response = context.getResponse();
        HttpRequest request = context.getRequest();
        response.setStatusCode(statusCode);
        response.setContent(new HttpHtmlContent(html, request.getPreferredCharset()));
    }

    protected String parseScript(WebContext context, Reader r) throws IOException {
        BufferedReader r1 = new BufferedReader(r);
        String line;
        StringBuilder script = new StringBuilder();
        while ((line = r1.readLine()) != null) {
            int i0 = 0;
            int i1 = line.indexOf("~[");
            while (i1 >= 0) {
                script.append(line.substring(i0, i1));
                int i2 = line.indexOf(']', i1 + 2);
                String reference = line.substring(i1 + 2, i2);
                script.append(getReferenceId(context, reference));
                i0 = i2 + 1;
                i1 = line.indexOf("~[", i0);
            }
            script.append(line.substring(i0)).append(Const.LINE_SEPARATOR);
        }
        return script.toString();
    }

    private String getReferenceId(WebContext context, String reference) {
        Channel channel = context.getChannel();
        WebServer server = context.getServer();
        int i = reference.indexOf('.');
        if (i >= 0) {
            String componentName = reference.substring(0, i);
            String methodName = reference.substring(i + 1);
            ComponentId componentId = channel.getComponentId(componentName);
            if (componentId != null) {
                MethodId methodId = componentId.getMethodId(methodName);
                if (methodId != null) return server.elementIdToId(methodId);
            }
        } else {
            ComponentId componentId = channel.getComponentId(reference);
            if (componentId != null) return server.elementIdToId(componentId);
        }
        return null;
    }
}
