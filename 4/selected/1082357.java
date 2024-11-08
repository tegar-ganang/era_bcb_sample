package org.zkoss.eclipse.newfile.wizards;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.osgi.framework.Bundle;
import org.zkoss.eclipse.newfile.NewZKFilePlugin;

/**
 * @author Ian Tsai
 *
 */
public abstract class AContentConfig {

    private static final int prefixLen = "%{{".length();

    private static final int suffixLen = "}}".length();

    private static final String FORM = "\\%\\{\\{[\\w]+\\}\\}";

    private static final Pattern PTN = Pattern.compile(FORM);

    public static final String CHARSET = "UTF-8";

    public static final String TITLE = "New ZUL File";

    protected Map<String, String> vars;

    protected Map<String, Boolean> activates;

    public AContentConfig() {
        vars = new LinkedHashMap<String, String>();
        activates = new LinkedHashMap<String, Boolean>();
        vars.put(Const.TITLE, TITLE);
        vars.put(Const.CHARSET, CHARSET);
    }

    /**
	 * 
	 * @return
	 */
    public abstract String getFileExtention();

    /**
	 * 
	 * @return
	 */
    public abstract String getTemplate() throws IOException;

    /**
	 * 
	 * @param key
	 * @return
	 */
    public void setVariable(String key, String value) {
        setVariable(key, value, isActivate(key));
    }

    /**
	 * 
	 * @param key
	 * @return
	 */
    public void setVariable(String key, String value, boolean activate) {
        vars.put(key, value);
        activates.put(key, activate);
    }

    /**
	 * 
	 * @param key
	 * @return
	 */
    public String getVariable(String key) {
        String value = "";
        if (vars.containsKey(key) && isActivate(key)) value = vars.get(key);
        return value;
    }

    /**
	 * 
	 * @return
	 */
    public boolean isActivate(String key) {
        if (activates.containsKey(key)) return activates.get(key);
        return true;
    }

    /**
	 * @param key
	 * @param activate
	 */
    public void setActivate(String key, boolean activate) {
        activates.put(key, activate);
    }

    /**
	 * 
	 * @return
	 * @throws IOException
	 */
    public String getContent() throws IOException {
        return evaluate(getTemplate(), this);
    }

    /**
	 * 
	 * @param relPath
	 * @return
	 * @throws IOException
	 */
    protected static String getDefaultTemplateFromBundle(String relPath) throws IOException {
        if (relPath == null) throw new IllegalArgumentException("The path of template file can't be null!");
        Bundle bundle = NewZKFilePlugin.getDefault().getBundle();
        InputStream in = FileLocator.openStream(bundle, new Path(relPath), false);
        ByteArrayOutputStream templateData = new ByteArrayOutputStream();
        int offset = 0;
        byte[] buff = new byte[1024 * 256];
        while ((offset = in.read(buff)) != -1) templateData.write(buff, 0, offset);
        return templateData.toString("utf-8");
    }

    /**
	 * 
	 * @param t_input
	 * @return
	 */
    private String evaluate(String t_input, AContentConfig settings) {
        if (t_input == null || "".equals(t_input)) return "";
        if (t_input.indexOf("%{{") < 0 || t_input.indexOf("}}") < 0) return t_input;
        Matcher matcher = PTN.matcher(t_input);
        StringBuffer sb = new StringBuffer();
        int start, end, current = 0;
        while (matcher.find()) {
            start = matcher.start();
            end = matcher.end();
            sb.append(t_input.substring(current, start));
            String key = t_input.substring(start + prefixLen, end - suffixLen);
            sb.append(evaluate(settings.getVariable(key), settings));
            current = end;
        }
        sb.append(t_input.substring(current));
        return sb.toString();
    }
}
