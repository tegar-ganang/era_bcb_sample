package com.manydesigns.portofino.methods.scrud;

import com.manydesigns.portofino.base.*;
import com.manydesigns.portofino.base.cache.BlobTokenCache;
import com.manydesigns.portofino.util.Defs;
import com.manydesigns.portofino.util.Escape;
import com.manydesigns.portofino.util.Util;
import com.manydesigns.xmlbuffer.XhtmlBuffer;
import javax.servlet.http.HttpServletRequest;
import java.text.MessageFormat;
import java.util.Locale;

class SearchPrintMDObjectVisitor extends PrintMDObjectVisitor {

    public static final String copyright = "Copyright (c) 2005-2009, ManyDesigns srl";

    private final XhtmlBuffer xb;

    private final String returnurl;

    private final HttpServletRequest req;

    private final MDRelAttribute omitRelAttr;

    private MDObject obj;

    private MDClass cls;

    private String objName;

    private Locale locale;

    private String readLink;

    private String localizedDetailsString;

    private int qid;

    private int position;

    public SearchPrintMDObjectVisitor(XhtmlBuffer xb, String returnurl, HttpServletRequest req, MDRelAttribute omitRelAttr) {
        this.xb = xb;
        this.returnurl = returnurl;
        this.req = req;
        this.omitRelAttr = omitRelAttr;
    }

    SearchPrintMDObjectVisitor(XhtmlBuffer xb, String returnurl, HttpServletRequest req, MDRelAttribute omitRelAttr, int qid, int position) {
        this.xb = xb;
        this.returnurl = returnurl;
        this.req = req;
        this.omitRelAttr = omitRelAttr;
        this.qid = qid;
        this.position = position;
    }

    public void doObjectPre(MDObject obj) throws Exception {
        this.obj = obj;
        objName = obj.getName();
        locale = obj.getConfig().getLocale();
        readLink = obj.getReadLink();
        readLink += "&qid=" + qid;
        localizedDetailsString = MessageFormat.format(Util.getLocalizedString(Defs.MDLIBI18N, locale, "details"), objName);
        cls = obj.getActualClass();
        if (!cls.isRelationship()) {
            String shorterName = Util.shortenString(objName);
            xb.openElement("td");
            String classString = "read_c" + cls.getId();
            xb.writeAnchor(readLink, shorterName, classString, shorterName);
            xb.closeElement("td");
        }
    }

    public void doAttributeListPre() throws Exception {
    }

    public void doAttributeGroupPre(String groupName) throws Exception {
    }

    protected void printAttribute(MDAttribute attribute, String stringValue, boolean canRead, boolean canWrite) throws Exception {
        if (attribute.isInName()) {
            return;
        }
        if (!canRead) {
            stringValue = Util.getLocalizedString(Defs.MDLIBI18N, locale, "not_available");
        }
        print(attribute, stringValue);
    }

    protected void printAttributeWithLink(MDAttribute attribute, String stringValue, String link, boolean canRead, boolean canWrite) throws Exception {
        if (attribute.isInName()) {
            return;
        }
        if (!canRead) {
            stringValue = Util.getLocalizedString(Defs.MDLIBI18N, locale, "not_available");
            print(attribute, stringValue);
        } else {
            xb.openElement("td");
            xb.writeAnchor(link, stringValue, null, stringValue);
            xb.closeElement("td");
        }
    }

    protected void print(MDAttribute attribute, String stringValue) throws Exception {
        xb.openElement("td");
        if (attribute instanceof MDBlobAttribute) printBlobAttribute((MDBlobAttribute) attribute); else xb.write(stringValue);
        xb.closeElement("td");
    }

    protected void printBlobAttribute(MDBlobAttribute attribute) throws Exception {
        MDBlob blob = obj.getBlobAttribute(attribute);
        if (blob == null) return;
        if (attribute.isPreview()) {
            xb.writeImage("ReadBlob?class=" + Escape.urlencode(attribute.getOwnerClass().getName()) + "&id=" + obj.getId() + "&blobid=" + blob.getId() + "&blobname=" + Escape.urlencode(attribute.getName()) + "&thumbnailsearch=", attribute.getPrettyName(), attribute.getPrettyName(), null, null);
        } else {
            xb.write(blob.getName());
        }
        BlobTokenCache blobTokenCache = attribute.getOwnerClass().getConfig().getBlobTokenCache();
        blobTokenCache.put(blob.getId());
    }

    @Override
    public void doTextAttribute(MDTextAttribute attribute, String value, boolean calculated, boolean canRead, boolean canWrite) throws Exception {
        super.doTextAttribute(attribute, Util.shortenString(value), calculated, canRead, canWrite);
    }

    @Override
    public void doRelAttribute(MDRelAttribute attribute, Integer value, boolean calculated, boolean canRead, boolean canWrite) throws Exception {
        if (attribute == omitRelAttr) {
            return;
        }
        super.doRelAttribute(attribute, value, calculated, canRead, canWrite);
    }

    public void doAttributeGroupPost() throws Exception {
    }

    public void doAttributeListPost() throws Exception {
    }

    public void doObjectPost() throws Exception {
        xb.openElement("td");
        xb.addAttribute("class", "actions");
        boolean spacer = true;
        if (!cls.isRelationship() || !cls.isImmutable()) {
            spacer = false;
            xb.openElement("a");
            xb.addAttribute("href", readLink);
            xb.addAttribute("title", MessageFormat.format(localizedDetailsString, objName));
            String detailImgLink = Util.getAbsoluteLink(req, "/images/detail.png");
            xb.writeImage(detailImgLink, localizedDetailsString, null, null, null);
            xb.closeElement("a");
        }
        if (obj.canUpdate()) {
            spacer = false;
            String updateLink = obj.getUpdateLink(returnurl, returnurl);
            String localizedUpdateString = MessageFormat.format(Util.getLocalizedString(Defs.MDLIBI18N, locale, "update"), objName);
            xb.openElement("a");
            xb.addAttribute("href", updateLink);
            xb.addAttribute("title", localizedUpdateString);
            String updateImgLink = Util.getAbsoluteLink(req, "/images/update.png");
            xb.writeImage(updateImgLink, localizedUpdateString, null, null, null);
            xb.closeElement("a");
        }
        if (obj.canDelete()) {
            spacer = false;
            String deleteLink = obj.getDeleteLink(returnurl, returnurl);
            String localizedDeleteString = MessageFormat.format(Util.getLocalizedString(Defs.MDLIBI18N, locale, cls.isRelationship() ? "disconnect" : "delete"), objName);
            xb.openElement("a");
            xb.addAttribute("href", deleteLink);
            xb.addAttribute("title", localizedDeleteString);
            String errorMsg = MessageFormat.format(Util.getLocalizedString(Defs.MDLIBI18N, locale, cls.isRelationship() ? "Are_you_sure_to_disconnect" : "Are_you_sure_to_delete"), objName);
            xb.addAttribute("onclick", "return confirm('" + Escape.javascriptEscape(errorMsg) + "')");
            String deleteImgLink = Util.getAbsoluteLink(req, cls.isRelationship() ? "/images/disconnect.png" : "/images/delete.png");
            xb.writeImage(deleteImgLink, localizedDeleteString, null, null, null);
            xb.closeElement("a");
        }
        if (spacer) {
            xb.openElement("div");
            xb.addAttribute("class", "spacer");
            xb.closeElement("div");
        }
        xb.closeElement("td");
    }
}
