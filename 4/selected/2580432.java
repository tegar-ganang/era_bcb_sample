package com.servengine.util;

import java.io.*;

public class AttachmentDataSource implements javax.activation.DataSource, java.io.Serializable {

    private static final long serialVersionUID = -1952213624355650431L;

    String name, contenttype;

    private byte[] datos;

    public AttachmentDataSource(String name, String contenttype, InputStream inputstream) throws IOException {
        this.name = name;
        this.contenttype = contenttype;
        BufferedInputStream lector = new BufferedInputStream(inputstream, 25000);
        ByteArrayOutputStream aux = new ByteArrayOutputStream();
        BufferedOutputStream escritor = new BufferedOutputStream(aux);
        byte[] buffer = new byte[10000];
        int leidos = 0;
        while ((leidos = inputstream.read(buffer, 0, 10000)) > 0) escritor.write(buffer, 0, leidos);
        lector.close();
        escritor.close();
        datos = aux.toByteArray();
    }

    public String getContentType() {
        return contenttype;
    }

    public String getName() {
        return name;
    }

    public OutputStream getOutputStream() {
        throw new IllegalStateException("Data cannot be written to this object using getOutputStream()");
    }

    public InputStream getInputStream() {
        return new ByteArrayInputStream(datos);
    }

    public int size() {
        return datos.length;
    }
}
