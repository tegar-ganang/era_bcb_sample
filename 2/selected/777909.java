package com.dcivision.workflow.applet;

import java.applet.Applet;
import java.awt.Graphics;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

public class TestDataStreamApplet extends Applet {

    String response;

    public void init() {
        try {
            System.out.println("Hello world!");
            java.net.URL url = new java.net.URL(getDocumentBase(), "servlet/DataStreamEcho");
            java.net.URLConnection con = url.openConnection();
            con.setUseCaches(true);
            con.setDoOutput(true);
            con.setDoInput(true);
            ByteArrayOutputStream byteout = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(byteout);
            out.writeUTF("Hello world!");
            out.flush();
            byte buf[] = byteout.toByteArray();
            con.setRequestProperty("Content-type", "application/octest-stream");
            con.setRequestProperty("Content-length", "" + buf.length);
            DataOutputStream dataout = new DataOutputStream(con.getOutputStream());
            dataout.write(buf);
            dataout.flush();
            dataout.close();
            DataInputStream in = new DataInputStream(con.getInputStream());
            response = in.readUTF();
            System.out.println("read from server:" + response);
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void paint(Graphics g) {
        g.drawString("read from server:" + response, 10, 20);
    }
}
