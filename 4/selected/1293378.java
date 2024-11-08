package com.volantis.mcs.protocols.widgets.renderers;

import java.io.StringWriter;
import java.util.Set;
import java.util.HashSet;
import com.volantis.mcs.dom.Element;
import com.volantis.mcs.localization.LocalizationFactory;
import com.volantis.mcs.protocols.DOMOutputBuffer;
import com.volantis.mcs.protocols.DOMProtocol;
import com.volantis.mcs.protocols.MCSAttributes;
import com.volantis.mcs.protocols.OutputBuffer;
import com.volantis.mcs.protocols.ProtocolException;
import com.volantis.mcs.protocols.VolantisProtocol;
import com.volantis.mcs.protocols.ticker.attributes.FeedAttributes;
import com.volantis.mcs.protocols.ticker.renderers.ElementDefaultRenderer;
import com.volantis.mcs.protocols.widgets.attributes.RefreshAttributes;
import com.volantis.mcs.protocols.widgets.attributes.TickerTapeAttributes;
import com.volantis.mcs.themes.StylePropertyDetails;
import com.volantis.mcs.themes.StyleValue;
import com.volantis.mcs.themes.properties.DisplayKeywords;
import com.volantis.mcs.runtime.scriptlibrarymanager.ScriptModule;
import com.volantis.mcs.runtime.scriptlibrarymanager.WidgetScriptModules;
import com.volantis.mcs.runtime.scriptlibrarymanager.ScriptModulesDefinitionRegistry;
import com.volantis.styling.PseudoElements;
import com.volantis.styling.Styles;
import com.volantis.styling.StylingFactory;
import com.volantis.synergetics.log.LogDispatcher;

/**
 * Widget renderer for TickerTape widget suitable for HTML protocols.
 */
public class TickerTapeDefaultRenderer extends WidgetDefaultRenderer {

    public static final ScriptModule MODULE = createAndRegisterModule();

    private static ScriptModule createAndRegisterModule() {
        Set dependencies = new HashSet();
        dependencies.add(WidgetScriptModules.BASE_BB);
        ScriptModule module = new ScriptModule("/vfc-tickertape.mscr", dependencies, 30100, true);
        ScriptModulesDefinitionRegistry.register(module);
        return module;
    }

    /**
     * Used for logging.
     */
    private static final LogDispatcher logger = LocalizationFactory.createLogger(TickerTapeDefaultRenderer.class);

    private String itemTemplateId;

    private OutputBuffer itemTemplateOutputBuffer;

    public void doRenderOpen(VolantisProtocol protocol, MCSAttributes attributes) throws ProtocolException {
        if (!isWidgetSupported(protocol)) {
            return;
        }
        require(MODULE, protocol, attributes);
        if (attributes.getId() == null) {
            attributes.setId(protocol.getMarinerPageContext().generateUniqueFCID());
        }
        openDivElement(protocol, attributes);
        itemTemplateId = null;
    }

    public void doRenderClose(VolantisProtocol protocol, MCSAttributes attributes) throws ProtocolException {
        if (!isWidgetSupported(protocol)) {
            return;
        }
        DOMOutputBuffer currentBuffer = getCurrentBuffer(protocol);
        closeDivElement(protocol);
        TickerTapeAttributes tickerTapeAttributes = (TickerTapeAttributes) attributes;
        if (tickerTapeAttributes.getRefreshAttributes() != null) {
            require(WidgetScriptModules.BASE_AJAX, protocol, attributes);
        }
        FeedAttributes feedAttributes = tickerTapeAttributes.getFeedAttributes();
        String separatorId = null;
        if (feedAttributes != null) {
            Styles separatorStyles = feedAttributes.getStyles();
            if (separatorStyles != null) {
                separatorStyles = separatorStyles.getNestedStyles(PseudoElements.MCS_ITEM);
                if (separatorStyles != null) {
                    separatorStyles = separatorStyles.getNestedStyles(PseudoElements.MCS_BETWEEN);
                    if (separatorStyles != null) {
                        Element placeholderSpan = getCurrentBuffer(protocol).openStyledElement("span", StylingFactory.getDefaultInstance().createInheritedStyles(protocol.getMarinerPageContext().getStylingEngine().getStyles(), DisplayKeywords.NONE));
                        separatorId = protocol.getMarinerPageContext().generateUniqueFCID();
                        placeholderSpan.setAttribute("id", separatorId);
                        Element contentSpan = getCurrentBuffer(protocol).openStyledElement("span", separatorStyles);
                        StyleValue separatorContent = separatorStyles.getPropertyValues().getSpecifiedValue(StylePropertyDetails.CONTENT);
                        if (separatorContent != null) {
                            ((DOMProtocol) protocol).getInserter().insert(contentSpan, separatorContent);
                        }
                        getCurrentBuffer(protocol).closeElement("span");
                        getCurrentBuffer(protocol).closeElement("span");
                    }
                }
            }
        }
        RefreshAttributes refreshAttributes = ((TickerTapeAttributes) attributes).getRefreshAttributes();
        StylesExtractor styles = createStylesExtractor(protocol, attributes.getStyles());
        StringWriter scriptWriter = new StringWriter();
        scriptWriter.write(createJavaScriptWidgetRegistrationOpening(attributes.getId()));
        addCreatedWidgetId(attributes.getId());
        scriptWriter.write("new Widget.TickerTape(" + createJavaScriptString(attributes.getId()) + ", {");
        scriptWriter.write("style:" + createJavaScriptString(styles.getMarqueeStyle()) + ",");
        scriptWriter.write("focusable:" + createJavaScriptString(styles.getFocusStyle()) + ",");
        scriptWriter.write("scroll:{");
        scriptWriter.write("direction:" + createJavaScriptString(styles.getMarqueeDirection()) + ",");
        scriptWriter.write("framesPerSecond:" + styles.getFrameRate() + ",");
        scriptWriter.write("charsPerSecond:" + styles.getMarqueeSpeed());
        scriptWriter.write("}");
        if (refreshAttributes != null) {
            scriptWriter.write(",refresh:{");
            scriptWriter.write("url:" + createJavaScriptString(refreshAttributes.getSrc()) + ",");
            scriptWriter.write("interval:" + createJavaScriptString(refreshAttributes.getInterval()));
            scriptWriter.write("}");
        }
        int repetitions = styles.getMarqueeRepetitions();
        scriptWriter.write(",repetitions:" + ((repetitions != Integer.MAX_VALUE) ? Integer.toString(repetitions) : createJavaScriptString("infinite")));
        scriptWriter.write("})");
        scriptWriter.write(createJavaScriptWidgetRegistrationClosure());
        scriptWriter.write(";");
        if (itemTemplateOutputBuffer != null) {
            getCurrentBuffer(protocol).transferContentsFrom(itemTemplateOutputBuffer);
            itemTemplateOutputBuffer = null;
        }
        if (feedAttributes != null) {
            require(ElementDefaultRenderer.WIDGET_TICKER, protocol, attributes);
            scriptWriter.write("Ticker.createTickerTapeController({tickerTape:Widget.getInstance(" + createJavaScriptString(attributes.getId()) + ")");
            if (feedAttributes.getChannel() != null) {
                scriptWriter.write(", channel:" + createJavaScriptString(feedAttributes.getChannel()));
            }
            if (feedAttributes.getItemDisplay() != null) {
                scriptWriter.write(", itemDisplayId:" + createJavaScriptString(feedAttributes.getItemDisplay()));
            }
            if (separatorId != null) {
                scriptWriter.write(", separatorId:" + createJavaScriptString(separatorId));
            }
            if (itemTemplateId != null) {
                scriptWriter.write(", itemTemplate:" + createJavaScriptWidgetReference(itemTemplateId));
                addUsedWidgetId(itemTemplateId);
            }
            scriptWriter.write("});");
            addUsedWidgetId(protocol.getMarinerPageContext().generateFCID(ElementDefaultRenderer.FEED_POLLER_ID_SUFFIX));
        }
        writeJavaScript(scriptWriter.toString());
    }

    public void setItemTemplateId(String id) {
        itemTemplateId = id;
    }

    public void setItemTemplateOutputBuffer(OutputBuffer buffer) {
        itemTemplateOutputBuffer = buffer;
    }
}
