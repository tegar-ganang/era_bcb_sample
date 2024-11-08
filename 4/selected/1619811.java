package net.sf.lanwork.nfs;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.ArrayList;
import net.sf.lanwork.connect.*;
import net.sf.lanwork.connect.action.*;
import net.sf.lanwork.util.Datas;

/**
 * NetFileSystem的服务器端。
 * 
 * @author Thomas Ting
 * @version 0.1 2009.1.3
 */
public class NetFileSystemServer {

    /**
	 * 初始化一个NetFileSystem服务器。
	 * 
	 * @param sharePath
	 *            共享的根目录。
	 * @param processor
	 *            SocketProcessor对象（已经连接到了远程客户端）。
	 */
    public NetFileSystemServer(String sharePath, SocketProcessor processor) {
        this.sharePath = sharePath;
        if (!(new File(sharePath).exists())) throw new IllegalArgumentException();
        processor.addAction(MessageType.CheckFileExist, new CheckFileExistsRequest());
        processor.addAction(MessageType.DownloadFile, new DownloadFileRequest());
        processor.addAction(MessageType.GetFiles, new LsRequest());
    }

    /**
	 * 显示当前目录下的所有文件。
	 * @author Thomas Ting
	 * @version 0.1 2009.1.3
	 */
    private class LsRequest implements Action {

        public Data process(Data request) {
            String dir = (String) request.getData();
            File file = new File(sharePath + dir);
            if (file.exists() && file.isDirectory()) {
                String[] files = file.list();
                ArrayList<FileInfo> infos = new ArrayList<FileInfo>();
                for (String sf : files) {
                    File f = new File(file, sf);
                    infos.add(new FileInfo(sf, f.canRead(), f.length(), f.isDirectory()));
                }
                return Datas.create(infos.toArray(new FileInfo[0]));
            } else return Datas.create(new FileInfo[0]);
        }
    }

    private class CheckFileExistsRequest implements Action {

        public Data process(Data request) {
            String fileName = (String) request.getData();
            File file = new File(sharePath + fileName);
            return Datas.create(file.exists());
        }
    }

    private class DownloadFileRequest implements Action {

        public Data process(Data request) {
            String fileName = (String) request.getData();
            File file = new File(sharePath + fileName);
            if (file.exists() && !file.isDirectory()) {
                FileChannel in = null;
                try {
                    in = new FileInputStream(file).getChannel();
                    long size = file.length();
                    ByteBuffer buffer = ByteBuffer.allocate((int) size);
                    in.read(buffer);
                    buffer.flip();
                    FileHolder holder = new FileHolder();
                    holder.setFile(buffer.array(), new FileInfo(file.getName(), file.canRead(), file.length(), file.isDirectory()));
                    return Datas.create(holder);
                } catch (Exception e) {
                    e.printStackTrace();
                    return Datas.create(new FileHolder());
                } finally {
                    try {
                        in.close();
                    } catch (Exception e) {
                    }
                }
            }
            return Datas.create(new FileHolder());
        }
    }

    private String sharePath;
}
