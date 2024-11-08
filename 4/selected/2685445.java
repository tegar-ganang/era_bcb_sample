package onepoint.project.modules.documents;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.imageio.ImageIO;
import onepoint.persistence.OpSiteObject;
import onepoint.project.modules.project.OpAttachment;
import onepoint.project.modules.project.OpAttachmentVersion;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.report.OpReportData;
import onepoint.project.modules.report.Report;
import onepoint.project.modules.report.ReportMethod;
import onepoint.project.modules.settings.OpSetting;
import onepoint.service.XSizeInputStream;

@Report
public class OpContent extends OpSiteObject implements DataSource {

    public static final String CONTENT = "OpContent";

    public static final String MEDIA_TYPE = "MediaType";

    public static final String ATTACHMENTS = "Attachments";

    public static final String ATTACHMENT_VERSIONS = "AttachmentVersions";

    public static final String DOCUMENT_NODES = "DocumentNodes";

    public static final String DOCUMENTS = "Documents";

    public static final String SIZE = "Size";

    public static final String STREAM = "Stream";

    private int refCount;

    private String mediaType;

    private long size;

    private XSizeInputStream stream;

    private Set<OpAttachment> attachments = new HashSet<OpAttachment>();

    private Set<OpAttachmentVersion> attachmentVersions = new HashSet<OpAttachmentVersion>();

    private Set<OpDocumentNode> documentNodes = new HashSet<OpDocumentNode>();

    private Set<OpDocument> documents = new HashSet<OpDocument>();

    private Set<OpSetting> settings;

    private Set<OpReportData> reportContents;

    private Set<OpReportData> dataContents;

    private Set<OpReportData> typeContents;

    private Set<OpProjectNode> projects;

    private final Object objectMutex = new Object();

    private transient Object object = null;

    Image imageCache = null;

    /**
	 * Default constructor
	 */
    @Deprecated
    public OpContent() {
    }

    /**
	 * Create a new instance of <code>OpContent</code> based on the specified stream
	 *
	 * @param stream an <code>XSizeInputStream</code> instance
	 */
    public OpContent(XSizeInputStream stream) {
        this.stream = stream;
        this.size = stream == null ? 0 : stream.getSize();
    }

    /**
	 * Create a new instance of <code>OpContent</code> based on the specified stream and the specified size
	 *
	 * @param stream the <code>InputStream</code> instance that contains the data
	 * @param size   the number of bytes from the provided stream. [0..Long.MAX_VALUE]
	 */
    public OpContent(InputStream stream, long size) {
        this.stream = new XSizeInputStream(stream, size);
        this.size = size;
    }

    public OpContent(byte[] data) {
        this(new ByteArrayInputStream(data), data.length);
    }

    public void setRefCount(int refCount) {
        this.refCount = refCount;
    }

    public int getRefCount() {
        return refCount;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public String getMediaType() {
        return mediaType;
    }

    /**
	 * Sets the size of the Content. Only for internal use. (Hibernate)
	 *
	 * @param size the size of the Content data in bytes
	 */
    public void setSize(long size) {
        this.size = size;
    }

    /**
	 * Gets the size of the Content.
	 *
	 * @return the size of the Content data in bytes
	 */
    @ReportMethod
    public long getSize() {
        return size;
    }

    /**
	 * Sets the input stream to be written into the database. This method will overwrite the value of the <code>size</code> code.
	 *
	 * @param stream an <code>XSizeInputStream</code> instance. Ignored if null.
	 */
    public void setStream(XSizeInputStream stream) {
        if (stream != null) {
            if (stream.getSize() == XSizeInputStream.UNKNOW_STREAM_SIZE) {
                stream.setSize(this.size);
            }
            this.stream = stream;
            synchronized (objectMutex) {
                object = null;
            }
        }
    }

    /**
	 * Gets the input stream from the database. The size of the stream is defined by the <code>size</code> property.
	 *
	 * @return an <code>XSizeInputStream</code> instance.
	 */
    public XSizeInputStream getStream() {
        if (stream != null && stream.getSize() == XSizeInputStream.UNKNOW_STREAM_SIZE) {
            stream.setSize(size);
        }
        return stream;
    }

    public byte[] toByteArray() {
        InputStream dataStream = stream.getInputStream();
        ByteArrayOutputStream bis = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        try {
            read = dataStream.read(buffer);
            while (read > 0) {
                bis.write(buffer, 0, read);
                read = dataStream.read(buffer);
            }
            return bis.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }

    /**
	 * Sets the input stream to be written into the database. This method will overwrite the value of the <code>size</code> code.
	 * Should not be used. Present only for back-up compatibility.
	 *
	 * @param stream an <code>XSizeInputStream</code> instance. Ignored if null.
	 * @see OpContent#setStream(onepoint.service.XSizeInputStream)
	 */
    @Deprecated
    public void setBytes(XSizeInputStream stream) {
        setStream(stream);
    }

    public void setAttachments(Set<OpAttachment> attachments) {
        this.attachments = attachments;
    }

    public void addAttachment(OpAttachment tgt) {
        if (getAttachments() == null) {
            setAttachments(new HashSet<OpAttachment>());
        }
        addIfInitialized(getAttachments(), tgt);
        tgt.setContent(this);
        setRefCount(getRefCount() + 1);
    }

    public void removeAttachment(OpAttachment tgt) {
        removeIfInitialized(getAttachments(), tgt);
        tgt.setContent(null);
        setRefCount(getRefCount() - 1);
    }

    public Set<OpAttachment> getAttachments() {
        return attachments;
    }

    public Set<OpDocumentNode> getDocumentNodes() {
        return documentNodes;
    }

    public void setDocumentNodes(Set<OpDocumentNode> documentNodes) {
        this.documentNodes = documentNodes;
    }

    public Set<OpDocument> getDocuments() {
        return documents;
    }

    public void setDocuments(Set<OpDocument> documents) {
        this.documents = documents;
    }

    public Set<OpAttachmentVersion> getAttachmentVersions() {
        return attachmentVersions;
    }

    public void setAttachmentVersions(Set<OpAttachmentVersion> attachmentVersions) {
        this.attachmentVersions = attachmentVersions;
    }

    public void addAttachmentVersion(OpAttachmentVersion av) {
        if (getAttachmentVersions() == null) {
            setAttachmentVersions(new HashSet<OpAttachmentVersion>());
        }
        addIfInitialized(getAttachmentVersions(), av);
        av.setContent(this);
        setRefCount(getRefCount() + 1);
    }

    public void removeAttachmentVersion(OpAttachmentVersion av) {
        removeIfInitialized(getAttachmentVersions(), av);
        av.setContent(this);
        setRefCount(getRefCount() - 1);
    }

    public Set<OpSetting> getSettings() {
        return settings;
    }

    public void setSettings(Set<OpSetting> settings) {
        this.settings = settings;
    }

    public void addSetting(OpSetting setting) {
        if (getSettings() == null) {
            setSettings(new HashSet<OpSetting>());
        }
        addIfInitialized(getSettings(), setting);
        setting.setContent(this);
        setRefCount(getRefCount() + 1);
    }

    public void removeSetting(OpSetting setting) {
        removeIfInitialized(getSettings(), setting);
        setting.setContent(this);
        setRefCount(getRefCount() - 1);
    }

    public Set<OpReportData> getReportContents() {
        return reportContents;
    }

    public void setReportContents(Set<OpReportData> reportContents) {
        this.reportContents = reportContents;
    }

    public void addReportContent(OpReportData setting) {
        if (getReportContents() == null) {
            setReportContents(new HashSet<OpReportData>());
        }
        addIfInitialized(getReportContents(), setting);
        setting.setReportContent(this);
        setRefCount(getRefCount() + 1);
    }

    public void removeReportContent(OpReportData setting) {
        removeIfInitialized(getReportContents(), setting);
        setting.setReportContent(this);
        setRefCount(getRefCount() - 1);
    }

    public Set<OpReportData> getDataContents() {
        return reportContents;
    }

    public void setDataContents(Set<OpReportData> reportContents) {
        this.reportContents = reportContents;
    }

    public void addDataContent(OpReportData setting) {
        if (getDataContents() == null) {
            setDataContents(new HashSet<OpReportData>());
        }
        addIfInitialized(getDataContents(), setting);
        setting.setDataContent(this);
        setRefCount(getRefCount() + 1);
    }

    public void removeDataContent(OpReportData setting) {
        removeIfInitialized(getDataContents(), setting);
        setting.setDataContent(this);
        setRefCount(getRefCount() - 1);
    }

    public Set<OpReportData> getTypeContents() {
        return typeContents;
    }

    public void setTypeContents(Set<OpReportData> typeContents) {
        this.typeContents = typeContents;
    }

    public void addTypeContent(OpReportData setting) {
        if (getTypeContents() == null) {
            setTypeContents(new HashSet<OpReportData>());
        }
        addIfInitialized(getTypeContents(), setting);
        setting.setTypeContent(this);
        setRefCount(getRefCount() + 1);
    }

    public void removeTypeContent(OpReportData setting) {
        removeIfInitialized(getTypeContents(), setting);
        setting.setTypeContent(this);
        setRefCount(getRefCount() - 1);
    }

    public void addDocumentNode(OpDocumentNode documentNode) {
        if (getDocumentNodes() == null) {
            setDocumentNodes(new HashSet<OpDocumentNode>());
        }
        addIfInitialized(getDocumentNodes(), documentNode);
        documentNode.setContent(this);
    }

    public void removeDocumentNode(OpDocumentNode documentNode) {
        removeIfInitialized(getDocumentNodes(), documentNode);
        documentNode.setContent(null);
    }

    public Set<OpProjectNode> getProjects() {
        return projects;
    }

    public void setProjects(Set<OpProjectNode> projects) {
        this.projects = projects;
    }

    public void addProject(OpProjectNode project) {
        if (getProjects() == null) {
            setProjects(new HashSet<OpProjectNode>());
        }
        addIfInitialized(getProjects(), project);
        project.setOrganizationalChart(this);
    }

    public void removeProject(OpProjectNode project) {
        removeIfInitialized(getProjects(), project);
        project.setOrganizationalChart(null);
    }

    @ReportMethod(inMemory = true)
    public String getContentType() {
        return getMediaType();
    }

    public Object getContent() throws IOException {
        synchronized (objectMutex) {
            if (object == null) {
                DataHandler dataHandler = new DataHandler(this);
                object = dataHandler.getContent();
            }
            return object;
        }
    }

    @ReportMethod(inMemory = true)
    public Image getImage() throws IOException {
        if (imageCache != null) {
            return imageCache;
        }
        BufferedImage image = ImageIO.read(getStream());
        this.imageCache = image;
        return imageCache;
    }

    public InputStream getInputStream() throws IOException {
        return getStream();
    }

    public OutputStream getOutputStream() throws IOException {
        throw new IOException("not yet implemented");
    }

    @ReportMethod(inMemory = true)
    public String getName() {
        return Long.toString(getId());
    }
}
