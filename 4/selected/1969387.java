package com.webreach.mirth.connectors.ihe;

import org.mozilla.javascript.Script;
import org.mule.providers.AbstractServiceEnabledConnector;
import org.mule.umo.MessagingException;
import org.mule.umo.UMOComponent;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.provider.UMOMessageAdapter;
import org.mule.umo.provider.UMOMessageReceiver;
import com.webreach.mirth.server.controllers.ScriptController;
import com.webreach.mirth.server.util.CompiledScriptCache;

public class IheConnector extends AbstractServiceEnabledConnector {

    private CompiledScriptCache compiledScriptCache = CompiledScriptCache.getInstance();

    private ScriptController scriptController = ScriptController.getInstance();

    private String scriptId;

    private String channelId;

    private String configurationFilePath;

    public String getScriptId() {
        return this.scriptId;
    }

    public void setScriptId(String scriptId) {
        this.scriptId = scriptId;
    }

    public String getChannelId() {
        return this.channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getConfigurationFilePath() {
        return configurationFilePath;
    }

    public void setConfigurationFilePath(String configurationFilePath) {
        this.configurationFilePath = configurationFilePath;
    }

    public UMOMessageReceiver createReceiver(UMOComponent component, UMOEndpoint endpoint) throws Exception {
        return null;
    }

    public String getProtocol() {
        return "ihe";
    }

    public UMOMessageAdapter getMessageAdapter(Object message) throws MessagingException {
        return null;
    }

    protected synchronized void initFromServiceDescriptor() throws InitialisationException {
        super.initFromServiceDescriptor();
        org.mozilla.javascript.Context context = org.mozilla.javascript.Context.enter();
        try {
            if (scriptId != null) {
                String script = scriptController.getScript(scriptId);
                if (script != null) {
                    String generatedScript = generateScript(script);
                    logger.debug("compiling script");
                    Script compiledScript = context.compileString(generatedScript, scriptId, 1, null);
                    compiledScriptCache.putCompiledScript(scriptId, compiledScript);
                }
            }
        } catch (Exception e) {
            throw new InitialisationException(e, this);
        } finally {
            org.mozilla.javascript.Context.exit();
        }
    }

    private String generateScript(String scriptString) {
        logger.debug("generating database script");
        StringBuilder script = new StringBuilder();
        script.append("importPackage(Packages.com.webreach.mirth.server.util);\n");
        script.append("function $(string) { ");
        script.append("if (connectorMap.containsKey(string)) { return connectorMap.get(string);} else ");
        script.append("if (channelMap.containsKey(string)) { return channelMap.get(string);} else ");
        script.append("if (globalMap.containsKey(string)) { return globalMap.get(string);} else ");
        script.append("{ return ''; }}");
        script.append("function $g(key, value){");
        script.append("if (arguments.length == 1){return globalMap.get(key); }");
        script.append("else if (arguments.length == 2){globalMap.put(key, value); }}");
        script.append("function $c(key, value){");
        script.append("if (arguments.length == 1){return channelMap.get(key); }");
        script.append("else if (arguments.length == 2){channelMap.put(key, value); }}");
        script.append("function $co(key, value){");
        script.append("if (arguments.length == 1){return connectorMap.get(key); }");
        script.append("else if (arguments.length == 2){connectorMap.put(key, value); }}");
        script.append("function $r(key, value){");
        script.append("if (arguments.length == 1){return responseMap.get(key); }");
        script.append("else if (arguments.length == 2){responseMap.put(key, value); }}");
        script.append("function doScript() {");
        script.append(scriptString + "}\n");
        script.append("doScript()\n");
        return script.toString();
    }
}
