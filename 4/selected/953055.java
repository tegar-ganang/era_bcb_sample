package org.fao.geonet.services.metadata;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.UUID;
import jeeves.interfaces.Service;
import jeeves.server.ServiceConfig;
import jeeves.server.context.ServiceContext;
import org.fao.geonet.constants.Params;
import org.fao.geonet.util.ParamUtils;
import org.jdom.Element;

/**
 * Allows a user to set the xsl used for displaying metadata
 * 
 * @author jeichar
 */
public class RegisterXsl implements Service {

    static final String USER_XSL_DIR = "user_xsl/";

    private Show showService;

    public Element exec(Element params, ServiceContext context) throws Exception {
        String xslUrlParam = ParamUtils.findParamText(params, "xsl");
        String id = ParamUtils.findParamText(params, Params.ID);
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (!id.matches("[\\w\\d]+")) {
            throw new IllegalParamsException("only letters and characters are permitted in the id");
        }
        URL xslUrl;
        try {
            xslUrl = new URL(xslUrlParam);
        } catch (MalformedURLException e) {
            throw new IllegalParamsException("The 'xsl' parameter must be a valid URL");
        }
        File file = new File(context.getAppPath() + USER_XSL_DIR + id + ".xsl");
        int i = 0;
        while (file.exists()) {
            i++;
            file = new File(context.getAppPath() + USER_XSL_DIR + id + "_" + i + ".xsl");
        }
        file.getParentFile().mkdirs();
        copy(xslUrl, file);
        if (i > 0) {
            id = id + "_" + i;
        }
        Element response = new Element("result");
        Element idElem = new Element("id");
        idElem.setAttribute("id", id);
        response.addContent(idElem);
        return response;
    }

    private void copy(URL xslUrl, File file) throws IOException {
        InputStream in = null;
        ReadableByteChannel inchannel = null;
        FileOutputStream out = null;
        FileChannel outchannel = null;
        try {
            in = xslUrl.openStream();
            inchannel = Channels.newChannel(in);
            out = new FileOutputStream(file);
            outchannel = out.getChannel();
            java.nio.ByteBuffer byteBuffer = java.nio.ByteBuffer.allocate(8092);
            int read;
            do {
                byteBuffer.clear();
                read = inchannel.read(byteBuffer);
                byteBuffer.flip();
                if (byteBuffer.remaining() > 0) {
                    outchannel.write(byteBuffer);
                }
            } while (read != -1);
        } finally {
            if (in != null) in.close();
            if (inchannel != null) inchannel.close();
            if (out != null) out.close();
            if (outchannel != null) outchannel.close();
        }
    }

    public void init(String appPath, ServiceConfig params) throws Exception {
        showService = new Show();
        showService.init(appPath, params);
    }
}
