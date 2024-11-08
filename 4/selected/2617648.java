package com.steadystate.css.dom;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import org.w3c.css.sac.CSSException;
import org.w3c.css.sac.InputSource;
import org.w3c.css.sac.SACMediaList;
import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.w3c.dom.css.CSSImportRule;
import org.w3c.dom.css.CSSRule;
import org.w3c.dom.css.CSSRuleList;
import org.w3c.dom.css.CSSStyleSheet;
import org.w3c.dom.stylesheets.MediaList;
import org.w3c.dom.stylesheets.StyleSheet;
import com.steadystate.css.parser.CSSOMParser;
import com.steadystate.css.util.LangUtils;
import com.steadystate.css.util.ThrowCssExceptionErrorHandler;

/**
 * Implementation of {@link CSSStyleSheet}.
 *
 * @author <a href="mailto:davidsch@users.sourceforge.net">David Schweinsberg</a>
 */
public class CSSStyleSheetImpl implements CSSStyleSheet, Serializable {

    private static final long serialVersionUID = -2300541300646796363L;

    private boolean disabled_;

    private Node ownerNode_;

    private StyleSheet parentStyleSheet_;

    private String href_;

    private String title_;

    private MediaList media_;

    private CSSRule ownerRule_;

    private boolean readOnly_;

    private CSSRuleList cssRules_;

    private String baseUri_;

    public void setMedia(final MediaList media) {
        media_ = media;
    }

    private String getBaseUri() {
        return baseUri_;
    }

    public void setBaseUri(final String baseUri) {
        baseUri_ = baseUri;
    }

    public CSSStyleSheetImpl() {
        super();
    }

    public String getType() {
        return "text/css";
    }

    public boolean getDisabled() {
        return disabled_;
    }

    /**
     * We will need to respond more fully if a stylesheet is disabled, probably
     * by generating an event for the main application.
     */
    public void setDisabled(final boolean disabled) {
        disabled_ = disabled;
    }

    public Node getOwnerNode() {
        return ownerNode_;
    }

    public StyleSheet getParentStyleSheet() {
        return parentStyleSheet_;
    }

    public String getHref() {
        return href_;
    }

    public String getTitle() {
        return title_;
    }

    public MediaList getMedia() {
        return media_;
    }

    public CSSRule getOwnerRule() {
        return ownerRule_;
    }

    public CSSRuleList getCssRules() {
        if (cssRules_ == null) {
            cssRules_ = new CSSRuleListImpl();
        }
        return cssRules_;
    }

    public int insertRule(final String rule, final int index) throws DOMException {
        if (readOnly_) {
            throw new DOMExceptionImpl(DOMException.NO_MODIFICATION_ALLOWED_ERR, DOMExceptionImpl.READ_ONLY_STYLE_SHEET);
        }
        try {
            final InputSource is = new InputSource(new StringReader(rule));
            final CSSOMParser parser = new CSSOMParser();
            parser.setParentStyleSheet(this);
            parser.setErrorHandler(ThrowCssExceptionErrorHandler.INSTANCE);
            final CSSRule r = parser.parseRule(is);
            if (r == null) {
                throw new DOMExceptionImpl(DOMException.SYNTAX_ERR, DOMExceptionImpl.SYNTAX_ERROR, "Parsing rule '" + rule + "' failed.");
            }
            if (getCssRules().getLength() > 0) {
                int msg = -1;
                if (r.getType() == CSSRule.CHARSET_RULE) {
                    if (index != 0) {
                        msg = DOMExceptionImpl.CHARSET_NOT_FIRST;
                    } else if (getCssRules().item(0).getType() == CSSRule.CHARSET_RULE) {
                        msg = DOMExceptionImpl.CHARSET_NOT_UNIQUE;
                    }
                } else if (r.getType() == CSSRule.IMPORT_RULE) {
                    if (index <= getCssRules().getLength()) {
                        for (int i = 0; i < index; i++) {
                            final int rt = getCssRules().item(i).getType();
                            if ((rt != CSSRule.CHARSET_RULE) && (rt != CSSRule.IMPORT_RULE)) {
                                msg = DOMExceptionImpl.IMPORT_NOT_FIRST;
                                break;
                            }
                        }
                    }
                } else {
                    if (index <= getCssRules().getLength()) {
                        for (int i = index; i < getCssRules().getLength(); i++) {
                            final int rt = getCssRules().item(i).getType();
                            if ((rt == CSSRule.CHARSET_RULE) || (rt == CSSRule.IMPORT_RULE)) {
                                msg = DOMExceptionImpl.INSERT_BEFORE_IMPORT;
                                break;
                            }
                        }
                    }
                }
                if (msg > -1) {
                    throw new DOMExceptionImpl(DOMException.HIERARCHY_REQUEST_ERR, msg);
                }
            }
            ((CSSRuleListImpl) getCssRules()).insert(r, index);
        } catch (final IndexOutOfBoundsException e) {
            throw new DOMExceptionImpl(DOMException.INDEX_SIZE_ERR, DOMExceptionImpl.INDEX_OUT_OF_BOUNDS, e.getMessage());
        } catch (final CSSException e) {
            throw new DOMExceptionImpl(DOMException.SYNTAX_ERR, DOMExceptionImpl.SYNTAX_ERROR, e.getMessage());
        } catch (final IOException e) {
            throw new DOMExceptionImpl(DOMException.SYNTAX_ERR, DOMExceptionImpl.SYNTAX_ERROR, e.getMessage());
        }
        return index;
    }

    public void deleteRule(final int index) throws DOMException {
        if (readOnly_) {
            throw new DOMExceptionImpl(DOMException.NO_MODIFICATION_ALLOWED_ERR, DOMExceptionImpl.READ_ONLY_STYLE_SHEET);
        }
        try {
            ((CSSRuleListImpl) getCssRules()).delete(index);
        } catch (final IndexOutOfBoundsException e) {
            throw new DOMExceptionImpl(DOMException.INDEX_SIZE_ERR, DOMExceptionImpl.INDEX_OUT_OF_BOUNDS, e.getMessage());
        }
    }

    public boolean isReadOnly() {
        return readOnly_;
    }

    public void setReadOnly(final boolean b) {
        readOnly_ = b;
    }

    public void setOwnerNode(final Node ownerNode) {
        ownerNode_ = ownerNode;
    }

    public void setParentStyleSheet(final StyleSheet parentStyleSheet) {
        parentStyleSheet_ = parentStyleSheet;
    }

    public void setHref(final String href) {
        href_ = href;
    }

    public void setTitle(final String title) {
        title_ = title;
    }

    public void setMediaText(final String mediaText) {
        final InputSource source = new InputSource(new StringReader(mediaText));
        try {
            final CSSOMParser parser = new CSSOMParser();
            final SACMediaList sml = parser.parseMedia(source);
            media_ = new MediaListImpl(sml);
        } catch (final IOException e) {
        }
    }

    public void setOwnerRule(final CSSRule ownerRule) {
        ownerRule_ = ownerRule;
    }

    public void setCssRules(final CSSRuleList rules) {
        cssRules_ = rules;
    }

    public String toString() {
        return getCssRules().toString();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof CSSStyleSheet)) {
            return false;
        }
        final CSSStyleSheet css = (CSSStyleSheet) obj;
        boolean eq = LangUtils.equals(getCssRules(), css.getCssRules());
        eq = eq && (getDisabled() == css.getDisabled());
        eq = eq && LangUtils.equals(getHref(), css.getHref());
        eq = eq && LangUtils.equals(getMedia(), css.getMedia());
        eq = eq && LangUtils.equals(getTitle(), css.getTitle());
        return eq;
    }

    @Override
    public int hashCode() {
        int hash = LangUtils.HASH_SEED;
        hash = LangUtils.hashCode(hash, baseUri_);
        hash = LangUtils.hashCode(hash, cssRules_);
        hash = LangUtils.hashCode(hash, disabled_);
        hash = LangUtils.hashCode(hash, href_);
        hash = LangUtils.hashCode(hash, media_);
        hash = LangUtils.hashCode(hash, ownerNode_);
        hash = LangUtils.hashCode(hash, readOnly_);
        hash = LangUtils.hashCode(hash, title_);
        return hash;
    }

    private void writeObject(final ObjectOutputStream out) throws IOException {
        out.writeObject(baseUri_);
        out.writeObject(cssRules_);
        out.writeBoolean(disabled_);
        out.writeObject(href_);
        out.writeObject(media_);
        out.writeBoolean(readOnly_);
        out.writeObject(title_);
    }

    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        baseUri_ = (String) in.readObject();
        cssRules_ = (CSSRuleList) in.readObject();
        if (cssRules_ != null) {
            for (int i = 0; i < cssRules_.getLength(); i++) {
                final CSSRule cssRule = cssRules_.item(i);
                if (cssRule instanceof AbstractCSSRuleImpl) {
                    ((AbstractCSSRuleImpl) cssRule).setParentStyleSheet(this);
                }
            }
        }
        disabled_ = in.readBoolean();
        href_ = (String) in.readObject();
        media_ = (MediaList) in.readObject();
        readOnly_ = in.readBoolean();
        title_ = (String) in.readObject();
    }

    /**
     * Imports referenced CSSStyleSheets.
     *
     * @param recursive <code>true</code> if the import should be done
     *   recursively, <code>false</code> otherwise
     */
    public void importImports(final boolean recursive) throws DOMException {
        for (int i = 0; i < getCssRules().getLength(); i++) {
            final CSSRule cssRule = getCssRules().item(i);
            if (cssRule.getType() == CSSRule.IMPORT_RULE) {
                final CSSImportRule cssImportRule = (CSSImportRule) cssRule;
                try {
                    final URI importURI = new URI(getBaseUri()).resolve(cssImportRule.getHref());
                    final CSSStyleSheetImpl importedCSS = (CSSStyleSheetImpl) new CSSOMParser().parseStyleSheet(new InputSource(importURI.toString()), null, importURI.toString());
                    if (recursive) {
                        importedCSS.importImports(recursive);
                    }
                    final MediaList mediaList = cssImportRule.getMedia();
                    if (mediaList.getLength() == 0) {
                        mediaList.appendMedium("all");
                    }
                    final CSSMediaRuleImpl cssMediaRule = new CSSMediaRuleImpl(this, null, mediaList);
                    cssMediaRule.setRuleList((CSSRuleListImpl) importedCSS.getCssRules());
                    deleteRule(i);
                    ((CSSRuleListImpl) getCssRules()).insert(cssMediaRule, i);
                } catch (final URISyntaxException e) {
                    throw new DOMException(DOMException.SYNTAX_ERR, e.getLocalizedMessage());
                } catch (final IOException e) {
                }
            }
        }
    }
}
