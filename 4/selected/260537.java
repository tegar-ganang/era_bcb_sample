package org.restfaces.util;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;
import org.restfaces.Constants;
import org.restfaces.Filter;
import org.restfaces.RestFacesException;
import org.restfaces.core.RestApplication;
import org.restfaces.model.Action;
import org.restfaces.model.ActionParameter;
import org.restfaces.model.BaseElement;
import org.restfaces.model.View;
import org.restfaces.model.ViewListener;

public class InfoTool {

    protected String readResource(InputStream in) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buff = new byte[512];
        int read;
        while ((read = in.read(buff)) != -1) {
            baos.write(buff, 0, read);
        }
        String str = baos.toString();
        return str;
    }

    public String renderInfo() throws IOException {
        InputStream in = getClass().getResourceAsStream(Constants.INFO_RESOURCE);
        if (null == in) {
            throw new FileNotFoundException(Constants.INFO_RESOURCE);
        }
        String result = "";
        try {
            RestApplication app = RestApplication.getCurrentInstance();
            result = readResource(in);
            result = result.replaceAll("#view#", renderViews(app.getViewList()));
            result = result.replaceAll("#action#", renderActions(app.getActionCollection()));
            result = result.replaceAll("#filter#", renderFilter(app.isFilterOn()));
        } catch (Exception ex) {
            throw new RestFacesException("error parsing " + Constants.INFO_RESOURCE + " :" + ex.getMessage(), ex);
        } finally {
            in.close();
        }
        return result;
    }

    private String renderFilter(boolean filterOn) {
        StringBuilder sb = new StringBuilder();
        sb.append(format("{0} is {1}.<br/>", Filter.class.getName(), filterOn ? " active" : "<b>not </b> active"));
        return sb.toString();
    }

    private String format(String template, Object... params) {
        for (int i = 0; i < params.length; ++i) {
            if (null == params[i]) {
                params[i] = " - ";
            }
        }
        return MessageFormat.format(template, params);
    }

    private String printMethod(Action action) {
        if (null == action.getMethod()) {
            return action.getExpression();
        } else {
            String s = "";
            if (action.getExpression() != null) {
                s += "<i>instance</i>: " + action.getExpression() + "<br/><i>method</i>: ";
            }
            s += action.getMethod().getMethod().toString();
            return s;
        }
    }

    private String tableParameters(Action action) {
        String[] params = action.getMethod().getSortedParameterNames();
        StringBuilder sb = new StringBuilder();
        sb.append("<table class=\"parameters\">").append("<thead><tr>").append("<th>Name</th><th>Converter</th><th>Encode</th><th>Expression</th><th>Type</th>").append("</tr></thead><tbody>");
        for (String param : params) {
            ActionParameter actParam = action.getPropertyMap().get(param);
            sb.append(format("<tr><td>{0}</td><td>{1}</td><td>{2}</td><td>{3}</td><td>{4}</td></tr>", param, actParam.getConverterId(), actParam.getParam().isEncode(), actParam.getValue(), actParam.getParam().getType()));
        }
        sb.append("</tbody></table>");
        return sb.toString();
    }

    private boolean containsProperties(Action action) {
        for (ActionParameter p : action.getPropertyMap().values()) {
            if (null == p.getReadonly()) {
                return true;
            }
        }
        return false;
    }

    private String tableProperties(BaseElement element) {
        StringBuilder sb = new StringBuilder();
        sb.append("<table class=\"parameters\">").append("<thead><tr>").append("<th>Name</th><th>Converter</th><th>Encode</th><th>Expression</th><th>Type</th><th>Nullable</th>").append("</tr></thead><tbody>");
        for (ActionParameter param : element.getPropertyMap().values()) {
            if (null == param.getReadonly()) {
                sb.append(format("<tr><td>{0}</td><td>{1}</td><td>{2}</td><td>{3}</td><td>{4}</td><td>{5}</td></tr>", param.getParam().getName(), param.getConverterId(), param.getParam().isEncode(), param.getValue(), param.getParam().getType(), param.isNullable()));
            }
        }
        sb.append("</tbody></table>");
        return sb.toString();
    }

    private String printListeners(View view) {
        StringBuilder sb = new StringBuilder();
        sb.append("<table class=\"parameters\">").append("<thead><tr>").append("<th>Method</th><th>When</th>").append("</tr></thead><tbody>");
        for (ViewListener listener : view.getListenerList()) {
            sb.append(format("<tr><td><code>{0}</code></td><td><code>{1}</code></td></tr>", listener.getValue(), listener.getWhenExpression()));
        }
        sb.append("</tbody></table>");
        return sb.toString();
    }

    private String renderActions(Collection<Action> actionCollection) {
        StringBuilder sb = new StringBuilder();
        for (Action action : actionCollection) {
            sb.append("<li>").append(format("<a name=\"{0}\"></a>", action.getName())).append(format("<a href=\"#\" class=\"info\" rel=\"action_{0}\"><span>+</span>{1}</a>", action.getName(), action.getName())).append("<div>").append(format("<table id=\"action_{0}\" style=\"display:none;\">", action.getName()));
            if (null != action.getParent()) {
                sb.append("<tr>").append("<td class=\"header\">Parent</td>").append(format("<td><a href=\"#{0}\">{0}</a></td>", action.getParent().getName())).append("</tr>");
            }
            sb.append("<tr>").append("<td class=\"header\">Method</td>").append(format("<td><code>{0}</code></td>", printMethod(action))).append("</tr>").append("<tr>").append("<td class=\"header\">URL</td>").append(format("<td>{0}{1}</td>", action.getUri(), action.isParametric() ? (" --&gt; " + action.getPattern().getPattern().pattern()) : "")).append("</tr>");
            if (action.getMethod() != null && action.getMethod().getSortedParameterNames().length > 0) {
                sb.append("<tr>").append("<td class=\"header\">Parameters</td>").append(format("<td>{0}</td>", tableParameters(action))).append("</tr>");
            }
            if (containsProperties(action)) {
                sb.append("<tr>").append("<td class=\"header\">Properties</td>").append(format("<td>{0}</td>", tableProperties(action))).append("</tr>");
            }
            sb.append("</table></div></li>");
        }
        return sb.toString();
    }

    private String renderViews(List<View> viewList) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (View view : viewList) {
            sb.append("<li>").append(format("<a href=\"#\" class=\"info\" rel=\"view_{0}\"><span>+</span>{1}</a>", i, view.getId())).append("<div>").append(format("<table id=\"view_{0}\" style=\"display:none;\">", i));
            if (!view.getListenerList().isEmpty()) {
                sb.append("<tr>").append("<td class=\"header\">Listeners</td>").append(format("<td><code>{0}</code></td>", printListeners(view))).append("</tr>");
            }
            if (!view.getPropertyMap().isEmpty()) {
                sb.append("<tr>").append("<td class=\"header\">Properties</td>").append(format("<td>{0}</td>", tableProperties(view))).append("</tr>");
            }
            sb.append("</table></div></li>");
            i++;
        }
        return sb.toString();
    }
}
