package com.ewansilver.raindrop.demo.httpserver;

import java.io.File;
import com.ewansilver.raindrop.HandlerImpl;
import com.ewansilver.raindrop.TaskQueueException;
import com.ewansilver.raindrop.UnknownTaskQueueException;
import com.ewansilver.raindrop.fileio.WriteFileEvent;
import com.ewansilver.raindrop.fileio.WriteFileTask;

/**
 * FileWriter is a test handler that will attempt to write the full URL path to
 * a file called write.htm in the webroot.
 * 
 * This is a test to check that we can do URL routing in the UrlDispatcher
 * handler and that the File IO write capability also works. Fairly pointless
 * other than that.
 * 
 * <code>handle</code> will receive an HttpConnection from the urlDispatcher
 * handler and a WriteFileEvent from the File IO Stage.
 * 
 * @author ewan.silver AT gmail.com
 */
public class FileWriter extends HandlerImpl {

    public void handle(Object aTask) {
        if (aTask instanceof HttpConnection) handleHttpConnection((HttpConnection) aTask); else if (aTask instanceof WriteFileEvent) handleWriteFileEvent((WriteFileEvent) aTask);
    }

    /**
	 * We have received a WriteFileEvent back from the IO stage. Return an
	 * indication to the user.
	 * 
	 * @param event the WriteFileEvent
	 */
    private void handleWriteFileEvent(WriteFileEvent event) {
        String mimetype = "text/html";
        byte[] data;
        data = "file written".getBytes();
        HttpConnection request = (HttpConnection) event.getAttachment();
        request.setResponse(data, mimetype);
        request.sendToClient();
    }

    /**
	 * Got a new HttpConnection - lets write its path to the write.html file.
	 * 
	 * @param anHttpConnection the connection.
	 */
    private void handleHttpConnection(HttpConnection anHttpConnection) {
        WriteFileTask writeTask = new WriteFileTask(new File("C:\\write.html"), anHttpConnection, getHandleName(), anHttpConnection.getPath().getBytes());
        try {
            enqueue("filereader", writeTask);
        } catch (TaskQueueException e) {
            e.printStackTrace();
        } catch (UnknownTaskQueueException e) {
            e.printStackTrace();
        }
    }
}
