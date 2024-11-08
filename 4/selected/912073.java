package justsftp;

import java.util.List;
import org.eclipse.core.runtime.IAdaptable;
import com.jcraft.jsch.Channel;

public class RemoteFile implements IAdaptable, Comparable<RemoteFile> {

    private boolean file;

    private String name;

    private String completePath;

    private List<RemoteFile> children;

    private RemoteFile parent;

    private Connection connection;

    private boolean isBase;

    private Channel channel;

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if (channel != null) {
            channel.disconnect();
            if (channel.getSession() != null) {
                channel.getSession().disconnect();
            }
        }
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public boolean isBase() {
        return isBase;
    }

    public void setBase(boolean isBase) {
        this.isBase = isBase;
    }

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public RemoteFile getParent() {
        return parent;
    }

    public void setParent(RemoteFile parent) {
        this.parent = parent;
    }

    public List<RemoteFile> getChildren() {
        return children;
    }

    public void setChildren(List<RemoteFile> children) {
        this.children = children;
    }

    public boolean isFile() {
        return file;
    }

    public void setFile(boolean file) {
        this.file = file;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCompletePath() {
        return completePath;
    }

    public void setCompletePath(String completePath) {
        this.completePath = completePath;
    }

    @Override
    public Object getAdapter(Class adapter) {
        return null;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int compareTo(RemoteFile o) {
        return this.name.compareTo(o.getName());
    }
}
