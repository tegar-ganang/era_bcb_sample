package net.sf.katta.index.indexer.merge;

import java.io.IOException;
import net.sf.katta.util.FileUtil;
import net.sf.katta.util.IHadoopConstants;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.MD5Hash;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.FileSplit;
import org.apache.hadoop.mapred.InputSplit;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordReader;
import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.MapFieldSelector;
import org.apache.lucene.index.IndexReader;

public class DfsIndexRecordReader implements RecordReader<Text, DocumentInformation> {

    private static final Logger LOG = Logger.getLogger(DfsIndexRecordReader.class);

    public static final String INVALID = "INVALID";

    private FileSplit _fileSplit;

    private IndexReader _indexReader;

    private Path _uncompressedIndexPath;

    private int _maxDoc;

    private int _doc;

    private IDocumentDuplicateInformation _duplicateInformation;

    public DfsIndexRecordReader(JobConf jobConf, InputSplit inputSplit, IDocumentDuplicateInformation duplicateInformation) throws IOException {
        _duplicateInformation = duplicateInformation;
        FileSystem fileSystem = FileSystem.get(jobConf);
        _fileSplit = (FileSplit) inputSplit;
        Path indexPath = _fileSplit.getPath();
        String md5 = MD5Hash.digest(indexPath.toString()).toString();
        _uncompressedIndexPath = new Path(FileOutputFormat.getOutputPath(jobConf), ".indexes/" + indexPath.getName() + "-" + md5 + "-uncompress");
        FileUtil.unzipInDfs(fileSystem, indexPath, _uncompressedIndexPath);
        try {
            int bufferSize = jobConf.getInt(IHadoopConstants.IO_FILE_BUFFER_SIZE, 4096);
            _indexReader = IndexReader.open(new DfsDirectory(fileSystem, _uncompressedIndexPath, bufferSize));
            _maxDoc = _indexReader.maxDoc();
        } catch (Exception e) {
            LOG.warn("can not open index '" + indexPath + "', ignore this index.", e);
        }
    }

    public boolean next(Text key, DocumentInformation value) throws IOException {
        boolean ret = false;
        if (_doc < _maxDoc) {
            ret = true;
            String keyInfo = null;
            String sortValue = null;
            try {
                MapFieldSelector selector = new MapFieldSelector(new String[] { _duplicateInformation.getKeyField(), _duplicateInformation.getSortField() });
                Document document = _indexReader.document(_doc, selector);
                keyInfo = document.get(_duplicateInformation.getKeyField());
                sortValue = document.get(_duplicateInformation.getSortField());
            } catch (Exception e) {
                LOG.warn("can not read document '" + _doc + "' from split '" + _fileSplit.getPath() + "'", e);
            }
            if ((keyInfo == null || keyInfo.trim().equals(""))) {
                keyInfo = INVALID;
            }
            if ((sortValue == null || sortValue.trim().equals(""))) {
                sortValue = "" + Integer.MIN_VALUE;
            }
            key.set(keyInfo);
            value.setDocId(_doc);
            value.setSortValue(sortValue);
            value.setIndexPath(_uncompressedIndexPath.toString());
            _doc++;
        }
        return ret;
    }

    public Text createKey() {
        return new Text();
    }

    public DocumentInformation createValue() {
        return new DocumentInformation();
    }

    public long getPos() throws IOException {
        return _doc;
    }

    public void close() throws IOException {
        if (_indexReader != null) {
            _indexReader.close();
        }
    }

    public float getProgress() throws IOException {
        return (float) _doc / _maxDoc;
    }
}
