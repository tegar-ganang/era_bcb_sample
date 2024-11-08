package com.test.jsonwebservice.rpc.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.jws.soap.SOAPBinding.Style;
import javax.xml.ws.Holder;
import com.test.jsonwebservice.rpc.AttachmentTest;

@SOAPBinding(style = Style.RPC)
@WebService(name = "AttachmentTest", serviceName = "RPCTestService", portName = "AttachmentPort", targetNamespace = "http://jsonwebservice.test.com/rpc", endpointInterface = "com.test.jsonwebservice.rpc.AttachmentTest", wsdlLocation = "WEB-INF/wsdl/rpc.wsdl")
public class AttachmentTestImpl implements AttachmentTest {

    private static final String IMAGE = "testImage.gif";

    public byte[] test1EmptyInImageOut() {
        InputStream ins = AttachmentTestImpl.class.getResourceAsStream(IMAGE);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int len;
        int size = 1024;
        byte[] buf = new byte[size];
        try {
            while ((len = ins.read(buf, 0, size)) != -1) bos.write(buf, 0, len);
            ins.close();
        } catch (IOException e) {
        }
        return bos.toByteArray();
    }

    public void test2ImageInEmptyOut(byte[] image) {
    }

    public void test3ImageInImageOut(Holder<byte[]> arg0) {
    }

    public void test4EmptyInImageWithInfoOut(Holder<String> arg0, Holder<byte[]> arg1) {
    }

    public void test5ImageWithInfoInEmptyOut(String info, byte[] image) {
    }

    public void test6ImageWithInfoInImageWithInfoOut(Holder<String> arg0, Holder<byte[]> arg1) {
    }
}
