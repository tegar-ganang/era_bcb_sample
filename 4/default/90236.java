import java.io.*;
import java.net.*;
import java.util.*;

public class HttpClientThread extends Thread {

    private String serverProtocol = null;

    private Socket httpSocket = null;

    private ImageBuffer imageBuffer = null;

    private String htmlRootDirectory = null;

    private boolean videoTunnelEnabled = true;

    private long threadStartTime = -1;

    private String requestFile = null;

    public HttpClientThread(String protocol, Socket s, String htmlRoot, boolean enableVideoTunnel, ImageBuffer buffer) {
        serverProtocol = protocol;
        httpSocket = s;
        imageBuffer = buffer;
        htmlRootDirectory = htmlRoot;
        videoTunnelEnabled = enableVideoTunnel;
        threadStartTime = System.currentTimeMillis();
    }

    public InetAddress getInetAddress() {
        return httpSocket.getInetAddress();
    }

    public String getRequestFile() {
        return requestFile;
    }

    public long getThreadStartTime() {
        return threadStartTime;
    }

    public void run() {
        LineNumberReader lin = null;
        BufferedOutputStream bout = null;
        try {
            lin = new LineNumberReader(new InputStreamReader(httpSocket.getInputStream()));
            bout = new BufferedOutputStream(httpSocket.getOutputStream());
            HttpClientRequest clientRequest = new HttpClientRequest(serverProtocol, lin);
            requestFile = clientRequest.getFile();
            if ((!clientRequest.isInternalFile()) && (!clientRequest.isAdminFile())) httpDiskFileResponse(htmlRootDirectory, requestFile, bout);
            if (clientRequest.isAdminFile()) VideoToAppletServer.ProcessHttpAdminCommand(clientRequest, bout);
            if (clientRequest.isInternalFile()) {
                if (!videoTunnelEnabled) {
                    try {
                        lin.close();
                    } catch (Exception ex) {
                    }
                    try {
                        bout.close();
                    } catch (Exception ex) {
                    }
                    try {
                        httpSocket.close();
                    } catch (Exception ex) {
                    }
                    return;
                }
                if (requestFile.compareTo("va-hello.gif") == 0) {
                    bout.write(httpSpecialResponseHeader());
                    DataOutputStream dout = new DataOutputStream(bout);
                    dout.writeInt(5776);
                    dout.flush();
                    dout.close();
                }
                if (requestFile.compareTo("va-split-descriptor.gif") == 0) {
                    bout.write(httpSpecialResponseHeader());
                    DataOutputStream dout = new DataOutputStream(bout);
                    ImageBufferGetFullJpg imageBufferGetFullJpg = new ImageBufferGetFullJpg();
                    ImageBufferItem imageBufferInfo = imageBufferGetFullJpg.compute(imageBuffer);
                    if (imageBufferInfo == null) {
                        dout.writeByte(99);
                    } else {
                        ImageConverter imageConverter = imageBufferInfo.imageConverter;
                        if (imageConverter == null) {
                            dout.writeByte(99);
                        } else {
                            dout.writeByte(1);
                            dout.writeInt(imageConverter.getWidth());
                            dout.writeInt(imageConverter.getHeight());
                            dout.writeInt(imageConverter.getSplitParts());
                            for (int x = 0; x < imageConverter.getSplitParts(); x++) {
                                for (int y = 0; y < imageConverter.getSplitParts(); y++) {
                                    SplitDescriptor splitDescriptor = imageConverter.getSplitDescriptor(x, y);
                                    dout.writeInt(splitDescriptor.getX());
                                    dout.writeInt(splitDescriptor.getY());
                                    dout.writeInt(splitDescriptor.getWidth());
                                    dout.writeInt(splitDescriptor.getHeight());
                                }
                            }
                        }
                    }
                    dout.flush();
                    dout.close();
                }
                if (requestFile.compareTo("va-split-request.gif") == 0) {
                    bout.write(httpSpecialResponseHeader());
                    DataOutputStream dout = new DataOutputStream(bout);
                    int clientSplits = clientRequest.getCgiIntValue("splits");
                    if (clientSplits != imageBuffer.getPictureSplits()) {
                        dout.writeByte(99);
                        transmitImageTime(imageBuffer, dout);
                        Stdout.log("*** client split format error");
                    } else {
                        float splitDiffRatio = clientRequest.getCgiFloatValue("diffRatio");
                        int transferSplitLimit = clientRequest.getCgiIntValue("transferLimit");
                        int fullLastSequenceNumber = 0;
                        int[][] clientSplitSequenceNumber = new int[clientSplits][clientSplits];
                        for (int x = 0; x < clientSplits; x++) for (int y = 0; y < clientSplits; y++) {
                            clientSplitSequenceNumber[x][y] = clientRequest.getCgiIntValue("splitData[" + x + "][" + y + "].splitSequenceNumber");
                            if (clientSplitSequenceNumber[x][y] > fullLastSequenceNumber) fullLastSequenceNumber = clientSplitSequenceNumber[x][y];
                        }
                        ImageSplitBuffer imageSplitBuffer = new ImageSplitBuffer(clientSplits, transferSplitLimit, clientSplitSequenceNumber, splitDiffRatio, imageBuffer.getBuffer());
                        int imageSplitResult = imageSplitBuffer.computeSplitUpdate();
                        switch(imageSplitResult) {
                            case ImageSplitBuffer.RESULT_NO_DATA:
                                dout.writeByte(99);
                                transmitImageTime(imageBuffer, dout);
                                break;
                            case ImageSplitBuffer.RESULT_SPLIT_DATA_READY:
                                SplitDescriptor[] transferSplit = imageSplitBuffer.getResultSplitDescriptors();
                                int[] transferSequence = imageSplitBuffer.getResultSplitSequences();
                                int[] resultSplitX = imageSplitBuffer.getResultX();
                                int[] resultSplitY = imageSplitBuffer.getResultY();
                                dout.writeByte(11);
                                transmitImageTime(imageBuffer, dout);
                                dout.writeInt(transferSplit.length);
                                for (int x = 0; x < transferSplit.length; x++) {
                                    dout.writeInt(resultSplitX[x]);
                                    dout.writeInt(resultSplitY[x]);
                                    transferSplit[x].writeObject(dout, transferSequence[x]);
                                }
                                break;
                            case ImageSplitBuffer.RESULT_USE_FULL_IMAGE:
                                ImageBufferGetLastFullJpg imageBufferGetLastFullJpg = new ImageBufferGetLastFullJpg();
                                ImageBufferItem fullImageBuffer = imageBufferGetLastFullJpg.compute(imageBuffer, fullLastSequenceNumber);
                                if (fullImageBuffer == null) {
                                    Stdout.log("*** never ever ***");
                                    dout.writeByte(99);
                                    transmitImageTime(imageBuffer, dout);
                                } else {
                                    dout.writeByte(10);
                                    transmitImageTime(imageBuffer, dout);
                                    dout.writeInt(fullImageBuffer.sequenceNumber);
                                    dout.writeInt(fullImageBuffer.imageConverter.getWidth());
                                    dout.writeInt(fullImageBuffer.imageConverter.getHeight());
                                    byte[] fullImage = fullImageBuffer.imageConverter.getFullJpg();
                                    dout.writeInt(fullImage.length);
                                    dout.write(fullImage, 0, fullImage.length);
                                }
                                break;
                            default:
                                throw new IOException("client thread error: invalid result from image split buffer: " + imageSplitResult);
                        }
                    }
                    dout.flush();
                    dout.close();
                }
            }
        } catch (Exception ex) {
            Stdout.log("client thread error: processing failed or client disconnected. " + ex);
        }
        try {
            lin.close();
        } catch (Exception ex) {
        }
        try {
            bout.close();
        } catch (Exception ex) {
        }
        try {
            httpSocket.close();
        } catch (Exception ex) {
        }
    }

    private static void transmitImageTime(ImageBuffer imageBuffer, DataOutputStream dout) throws IOException {
        if (imageBuffer.isValid()) dout.writeUTF(VideoToAppletServer.getTimZone() + "  " + ZoneTime.dateToShortString(VideoToAppletServer.getTimZone())); else dout.writeUTF("");
    }

    private static byte[] httpSpecialResponseHeader() {
        return httpSpecialResponseHeader(-1);
    }

    private static byte[] httpSpecialResponseHeader(int contentLength) {
        StringBuffer httpResponseHeader = new StringBuffer();
        httpResponseHeader.append("HTTP/1.0 200 OK\r\n");
        httpResponseHeader.append("Connection: close\r\n");
        httpResponseHeader.append("Content-Type: image/gif\r\n");
        if (contentLength > 0) httpResponseHeader.append("Content-Length: " + contentLength + "\r\n");
        httpResponseHeader.append("\r\n");
        return httpResponseHeader.toString().getBytes();
    }

    private static void httpDiskFileResponse(String htmlRootDirectory, String fileName, BufferedOutputStream bout) throws IOException {
        StringBuffer httpResponseHeader = new StringBuffer();
        File fileDescriptor = new File(htmlRootDirectory + fileName);
        if ((!fileDescriptor.exists()) || (fileName.indexOf(":") >= 0) || (fileName.indexOf("..") >= 0) || (fileName.indexOf("/") >= 0) || (fileName.indexOf("\\") >= 0) || (fileName.indexOf("@") >= 0) || (fileName.indexOf("$") >= 0)) {
            httpResponseHeader.append("HTTP/1.0 404 Not Found\r\n");
            httpResponseHeader.append("Connection: close\r\n");
            httpResponseHeader.append("Content-Type: text/html; charset=iso-8859-1\r\n");
            httpResponseHeader.append("\r\n");
            httpResponseHeader.append("<HTML><BODY>\r\n");
            httpResponseHeader.append("<B>Video Applet Server</B><BR>&nbsp;<BR>\r\n");
            httpResponseHeader.append("Error 404: File not found.\r\n");
            httpResponseHeader.append("<BR>&nbsp;<BR>&nbsp;<BR><HR>\r\n");
            httpResponseHeader.append("&copy; 2001 by David Fischer, Bern - Switzerland\r\n");
            httpResponseHeader.append("</BODY></HTML>\r\n");
            bout.write(httpResponseHeader.toString().getBytes());
            bout.flush();
            return;
        }
        httpResponseHeader.append("HTTP/1.0 200 OK\r\n");
        String contentType = getDiskFileContentType(fileName);
        if (contentType != null) httpResponseHeader.append("Content-Type: " + contentType + "\r\n");
        httpResponseHeader.append("Content-Length: " + fileDescriptor.length() + "\r\n");
        httpResponseHeader.append("Connection: close\r\n");
        httpResponseHeader.append("\r\n");
        bout.write(httpResponseHeader.toString().getBytes());
        byte[] fileData = new byte[8192];
        BufferedInputStream fin = new BufferedInputStream(new FileInputStream(fileDescriptor));
        int readedBytes = fin.read(fileData);
        while (readedBytes != -1) {
            bout.write(fileData, 0, readedBytes);
            readedBytes = fin.read(fileData);
        }
        bout.flush();
    }

    private static String getDiskFileContentType(String filename) {
        String result = null;
        String file = filename.toLowerCase();
        if (filename.endsWith(".html") || filename.endsWith(".htm")) return "text/html";
        if (filename.endsWith(".gif")) return "image/gif";
        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) return "image/jpeg";
        if (filename.endsWith(".doc")) return "application/msword";
        if (filename.endsWith(".class")) return "application/octet-stream";
        return result;
    }
}
