package br.com.mcampos.controller.anoto.util.icr;

import org.jawin.COMException;

public class A2ia extends A2iaCom {

    public static final String paramDir = "C:\\Program Files\\A2iA\\A2iA FieldReader V3.3 R1\\Parms\\SoftField\\Parms";

    private static A2ia objInstance = null;

    private static final String channelParamString = "ScrCreateChannelParam";

    private static final String openChannelParamString = "ScrOpenChannelExt";

    private A2ia() throws COMException {
        super();
        configureA2iaPath(paramDir);
    }

    public static A2ia getInstance() throws COMException {
        if (objInstance == null) {
            objInstance = new A2ia();
            System.out.println("A2ia is finalized!");
        }
        return objInstance;
    }

    public static A2ia createInstance() throws COMException {
        return new A2ia();
    }

    @Override
    protected void finalize() throws Throwable {
        objInstance = null;
        System.out.println("A2ia is finalized!");
        super.finalize();
    }

    private void configureA2iaPath(String path) throws COMException {
        invoke("ScrInit", path);
    }

    public A2iaChannel createChannel() throws COMException {
        Integer id = (Integer) invoke(channelParamString);
        A2iaChannel channel = new A2iaChannel(id, this);
        return channel;
    }

    public void setProperty(Integer objectId, String name, String param) throws COMException {
        invoke("SetProperty", objectId, name, param);
    }

    public Object getProperty(Integer objectId, String name) throws COMException {
        return invoke("GetProperty", objectId, name);
    }

    public Integer openChannel(A2iaChannel channel) throws COMException {
        Integer id = (Integer) invoke(openChannelParamString, channel.getParamId(), 0);
        return id;
    }

    public void close(A2iaChannel channel) throws COMException {
        invoke("ScrCloseChannel", channel.getId());
    }

    public void close(A2iaDocument document) throws COMException {
        invoke("ScrCloseDocument", document.getId());
    }

    public void close(A2iaTemplate template) throws COMException {
        invoke("ScrCloseDocumentTable", template.getId());
    }

    public void close(A2iaRequest request) throws COMException {
        invoke("ScrCloseRequest", request.getId());
    }

    public Integer openTemplate(A2iaTemplate template) throws COMException {
        Integer tableId = (Integer) invoke("ScrOpenDocumentTable", template.getFilename());
        return tableId;
    }

    public Integer createDefaultDocument(A2iaTemplate template) throws COMException {
        return (Integer) invoke("ScrGetDefaultDocument", template.getId());
    }

    public void defineImage(A2iaDocument document) throws COMException {
        invoke("ScrDefineImage", document.getId(), document.getStringType(), "FILE", document.getImagePath());
    }

    public Integer openRequest(A2iaRequest request) throws COMException {
        return (Integer) invoke("ScrOpenRequest", request.getChannel().getId(), request.getDocument().getId());
    }

    public A2iaResult processRequest(A2iaRequest request) throws COMException {
        Integer id;
        id = (Integer) invoke("ScrGetResult", request.getChannel().getId(), request.getDocument().getId(), request.getTimeout());
        A2iaResult result = new A2iaResult(this);
        result.setId(id);
        return result;
    }
}
