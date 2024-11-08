package ch.arpage.collaboweb.services.impl;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.io.StringWriter;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.RangeFilter;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRichTextString;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.pdfbox.pdmodel.PDDocument;
import org.pdfbox.util.PDFTextStripper;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.textmining.text.extraction.FastSavedException;
import org.textmining.text.extraction.WordExtractor;
import ch.arpage.collaboweb.common.Utils;
import ch.arpage.collaboweb.dao.ResourceDao;
import ch.arpage.collaboweb.model.Attribute;
import ch.arpage.collaboweb.model.BinaryAttribute;
import ch.arpage.collaboweb.model.Model;
import ch.arpage.collaboweb.model.Resource;
import ch.arpage.collaboweb.model.ResourceAttribute;
import ch.arpage.collaboweb.model.SearchQuery;
import ch.arpage.collaboweb.model.User;
import ch.arpage.collaboweb.services.IndexerManager;

/**
 * Implementation of the Indexer Manager service based on Lucene. 
 * 
 * @author <a href="mailto:patrick@arpage.ch">Patrick Herber</a>
 */
public class IndexerManagerImpl implements IndexerManager, InitializingBean, DisposableBean {

    private static final Log LOG = LogFactory.getLog(IndexerManagerImpl.class);

    private String repository;

    private ResourceDao resourceDao;

    private IndexWriterThread writerThread;

    private List<QueueEntry> indexQueue = Collections.synchronizedList(new LinkedList<QueueEntry>());

    /**
	 * Set the repository.
	 * @param repository the repository to set
	 */
    public void setRepository(String repository) {
        File file = new File(repository);
        Assert.isTrue(file.isDirectory(), "Repository is not a directory: " + repository);
        this.repository = repository;
    }

    /**
	 * Set the resourceDao.
	 * @param resourceDao the resourceDao to set
	 */
    public void setResourceDao(ResourceDao resourceDao) {
        this.resourceDao = resourceDao;
    }

    public void afterPropertiesSet() throws Exception {
        writerThread = new IndexWriterThread(60000);
        writerThread.start();
    }

    public void destroy() throws Exception {
        if (writerThread != null) {
            writerThread.stopExecution();
        }
    }

    public void index(Resource resource, User user) {
        this.indexQueue.add(new QueueEntry(resource, user));
    }

    @SuppressWarnings("unchecked")
    public List search(SearchQuery searchQuery, User user) throws Exception {
        Directory directory = null;
        IndexSearcher is = null;
        BooleanQuery query = new BooleanQuery();
        boolean timeSearch = true;
        RangeFilter filter = null;
        try {
            directory = getIndexDirectory(user.getCommunityId(), false);
            is = new IndexSearcher(directory);
            if (addFilterFor("name", searchQuery.getName(), query, BooleanClause.Occur.SHOULD, true)) {
                timeSearch = false;
            }
            if (StringUtils.hasText(searchQuery.getQuickSearch())) {
                Collection<String> fieldNames = is.getIndexReader().getFieldNames(IndexReader.FieldOption.ALL);
                if (fieldNames != null) {
                    String[] fields = new String[fieldNames.size()];
                    Occur[] occur = new Occur[fields.length];
                    int i = 0;
                    for (String name : fieldNames) {
                        fields[i] = name;
                        occur[i++] = BooleanClause.Occur.SHOULD;
                    }
                    query.add(MultiFieldQueryParser.parse(searchQuery.getQuickSearch(), fields, occur, new StandardAnalyzer()), BooleanClause.Occur.SHOULD);
                }
            }
            if (searchQuery.getTypeId() != 0) {
                if (addFilterFor("typeId", Integer.toString(searchQuery.getTypeId()), query, BooleanClause.Occur.SHOULD, true)) {
                    timeSearch = false;
                }
            }
            if (StringUtils.hasText(searchQuery.getFree())) {
                query.add(new QueryParser("", new StandardAnalyzer()).parse(searchQuery.getFree()), BooleanClause.Occur.SHOULD);
            }
            Map<String, String> fields = searchQuery.getFields();
            for (String name : fields.keySet()) {
                String value = fields.get(name);
                if (StringUtils.hasText(value)) {
                    if (addFilterFor(name, value, query, BooleanClause.Occur.SHOULD, true)) {
                        timeSearch = false;
                    }
                }
            }
            filter = getTimeFilter(searchQuery, filter);
            if (timeSearch && filter != null) {
                query.add(new WildcardQuery(new Term("name", "*")), BooleanClause.Occur.MUST);
            }
            LOG.debug(query.toString());
            Hits hits = is.search(query, filter);
            if (hits.length() > 0) {
                int length = hits.length();
                List<Map<String, Object>> list = new ArrayList<Map<String, Object>>(length);
                for (int i = 0; i < length; ++i) {
                    Document doc = hits.doc(i);
                    Map<String, Object> map = new HashMap<String, Object>();
                    for (Enumeration enumer = doc.fields(); enumer.hasMoreElements(); ) {
                        Field field = (Field) enumer.nextElement();
                        String name = field.name();
                        if ("modified".equals(name) || "createdate".equals(name)) {
                            String value = doc.get(name);
                            if (StringUtils.hasText(value)) {
                                map.put(name, DateTools.stringToDate(value));
                            }
                        } else {
                            map.put(name, doc.get(name));
                        }
                    }
                    map.put("score", NumberFormat.getPercentInstance().format(hits.score(i)));
                    list.add(map);
                }
                return list;
            }
        } catch (FileNotFoundException fnfe) {
        } catch (BooleanQuery.TooManyClauses tmc) {
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
                }
            }
            close(directory);
        }
        return null;
    }

    /**
	 * Returns the time filter.
	 * @param searchQuery
	 * @param filter
	 * @return the time filter.
	 */
    private RangeFilter getTimeFilter(SearchQuery searchQuery, RangeFilter filter) {
        Date date;
        date = Utils.parseDate(searchQuery.getUpdateDate());
        if (date != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            switch(searchQuery.getUpdateDateComparator()) {
                case -1:
                    Calendar from = Calendar.getInstance();
                    from.setTimeInMillis(0);
                    filter = new RangeFilter("modified", DateTools.dateToString(cal.getTime(), DateTools.Resolution.DAY), DateTools.dateToString(from.getTime(), DateTools.Resolution.DAY), true, true);
                    break;
                case 1:
                    filter = new RangeFilter("modified", DateTools.dateToString(cal.getTime(), DateTools.Resolution.DAY), DateTools.dateToString(Calendar.getInstance().getTime(), DateTools.Resolution.DAY), true, true);
                    break;
                default:
                    String str = DateTools.dateToString(cal.getTime(), DateTools.Resolution.DAY);
                    filter = new RangeFilter("modified", str, str, true, true);
                    break;
            }
        }
        return filter;
    }

    private boolean addFilterFor(String fieldName, String value, BooleanQuery query, BooleanClause.Occur occur, boolean allowWildcard) throws Exception {
        if (StringUtils.hasText(value)) {
            if (!allowWildcard || value.indexOf('*') == -1) {
                QueryParser queryParser = new QueryParser(fieldName, new StandardAnalyzer());
                query.add(queryParser.parse(value), occur);
            } else {
                query.add(new WildcardQuery(new Term(fieldName, value.toLowerCase())), occur);
            }
            return true;
        }
        return false;
    }

    private Directory getDirectory(long cid) {
        Directory directory = null;
        try {
            directory = getIndexDirectory(cid, false);
            if (directory == null) {
                LOG.error("Could not get community directory: " + cid);
            }
        } catch (Exception e) {
            LOG.error("Exception retrieving community directory: " + cid, e);
        }
        return directory;
    }

    private void indexResource(Resource resource, User user) {
        Directory directory = getDirectory(user.getCommunityId());
        if (directory == null) {
            return;
        }
        IndexWriter writer = null;
        try {
            deleteFromIndex(resource.getResourceId(), resource.getCommunityId(), directory);
            try {
                writer = new IndexWriter(directory, new StandardAnalyzer(), false);
            } catch (FileNotFoundException fne) {
                directory = getIndexDirectory(resource.getCommunityId(), true);
                writer = new IndexWriter(directory, new StandardAnalyzer(), true);
            }
            writer.addDocument(indexDocument(resource));
            LOG.info("Successfully indexed file: " + resource.getResourceId());
        } catch (Exception e) {
            LOG.error("Exception indexing file: " + resource.getResourceId(), e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Exception e) {
                }
            }
            close(directory);
        }
    }

    /**
	 * @param resource
	 * @return
	 */
    private Document indexDocument(Resource resource) {
        Document doc = new Document();
        addField("resourceId", resource.getResourceId(), doc);
        addField("parentId", resource.getParentId(), doc);
        addField("name", resource.getName(), doc);
        addField("authorId", resource.getAuthorId(), doc);
        addField("authorName", resource.getAuthorName(), doc);
        addField("typeId", Integer.toString(resource.getTypeId()), doc);
        addField("modified", resource.getUpdateDate(), doc);
        addField("createDate", resource.getCreateDate(), doc);
        for (Attribute attribute : resource.getResourceType().getAttributes()) {
            if (attribute.getSearchFieldType() != 0) {
                ResourceAttribute resourceAttribute = resource.getResourceAttribute(attribute.getAttributeId());
                if (attribute.getDataType() != Model.DATA_TYPE_BINARY) {
                    doc.add(new Field(attribute.getIdentifier(), (String) resourceAttribute.getValue(), Field.Store.YES, Field.Index.UN_TOKENIZED));
                } else {
                    indexBinaryAttribute(resource, attribute, doc);
                }
            }
        }
        return doc;
    }

    /**
	 * @param resource
	 * @param attribute
	 * @param doc
	 */
    private void indexBinaryAttribute(Resource resource, Attribute attribute, Document doc) {
        BinaryAttribute ba = null;
        try {
            ba = resourceDao.readAttribute(resource.getResourceId(), attribute.getIdentifier());
            String contentType = ba.getContentType();
            if (contentType != null) {
                doc.add(new Field("contentType", contentType, Field.Store.YES, Field.Index.UN_TOKENIZED));
            }
            addContent(resource.getResourceId(), attribute.getIdentifier(), ba, doc);
        } catch (Exception e) {
            LOG.error("Exception parsing file: " + resource.getResourceId(), e);
        } finally {
            if (ba != null) {
                try {
                    ba.getInputStream().close();
                } catch (Exception e) {
                }
            }
        }
    }

    /**
	 * 
	 * @param name
	 * @param value
	 * @param doc
	 */
    private void addField(String name, long value, Document doc) {
        addField(name, Long.toString(value), doc);
    }

    private void addField(String name, Date value, Document doc) {
        if (value == null) {
            value = new Date();
        }
        addField(name, DateTools.dateToString(value, DateTools.Resolution.MINUTE), doc);
    }

    /**
	 * 
	 * @param name
	 * @param value
	 * @param doc
	 */
    private void addField(String name, String value, Document doc) {
        if (StringUtils.hasText(value)) {
            doc.add(new Field(name, value, Field.Store.YES, Field.Index.TOKENIZED));
        }
    }

    private void deleteFromIndex(long rid, long cid, Directory directory) {
        deleteFromIndex(rid, new Term("resourceId", Long.toString(rid)), directory);
        close(directory);
    }

    public void deleteFromIndex(long resourceId, User user) {
        Directory directory = null;
        try {
            directory = getIndexDirectory(user.getCommunityId(), false);
            deleteFromIndex(resourceId, new Term("resourceId", Long.toString(resourceId)), directory);
        } catch (Exception e) {
            LOG.warn("Exception deleteting file " + resourceId + " from index", e);
        } finally {
            close(directory);
        }
    }

    private void deleteFromIndex(long rid, Term term, Directory directory) {
        IndexReader reader = null;
        try {
            reader = IndexReader.open(directory);
            int deleted = reader.deleteDocuments(term);
            LOG.info("deleted " + deleted + " documents containing " + term);
        } catch (FileNotFoundException e) {
        } catch (Exception e) {
            if (LOG.isDebugEnabled()) {
                LOG.warn("Exception deleteting file " + rid + " from index", e);
            } else {
                LOG.warn("Exception deleteting file " + rid + " from index: " + e);
            }
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                }
            }
        }
        if (indexQueue.size() > 0) {
            synchronized (indexQueue) {
                for (int i = 0; i < indexQueue.size(); ++i) {
                    if (((QueueEntry) indexQueue.get(i)).resource.getResourceId() == rid) {
                        indexQueue.remove(i);
                        return;
                    }
                }
            }
        }
    }

    private String parseWorldFile(InputStream stream) throws Exception {
        WordExtractor extractor = new WordExtractor();
        return extractor.extractText(stream);
    }

    private String parseExcelFile(InputStream stream) throws Exception {
        HSSFWorkbook workbook = new HSSFWorkbook(stream);
        HSSFSheet sheet;
        Iterator rows;
        StringBuffer sb = new StringBuffer(4096);
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            sheet = workbook.getSheetAt(i);
            rows = sheet.rowIterator();
            while (rows.hasNext()) {
                Iterator cellIterator = ((HSSFRow) rows.next()).cellIterator();
                while (cellIterator.hasNext()) {
                    HSSFCell cell = (HSSFCell) cellIterator.next();
                    switch(cell.getCellType()) {
                        case HSSFCell.CELL_TYPE_STRING:
                            HSSFRichTextString richText = cell.getRichStringCellValue();
                            if (richText != null) {
                                sb.append(richText.getString());
                            }
                            break;
                        case HSSFCell.CELL_TYPE_NUMERIC:
                            sb.append(cell.getNumericCellValue());
                            break;
                        case HSSFCell.CELL_TYPE_BOOLEAN:
                            sb.append(cell.getBooleanCellValue());
                            break;
                    }
                    sb.append(";");
                }
            }
        }
        return sb.toString();
    }

    private String parsePDFFile(InputStream stream, long rid) throws Exception {
        StringWriter output = new StringWriter(4096);
        PDDocument document = null;
        try {
            document = PDDocument.load(stream);
            if (document.isEncrypted()) {
                LOG.warn("Could not parse PDF File " + rid + " since the document is encrypted");
            } else {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setStartPage(1);
                stripper.setEndPage(Integer.MAX_VALUE);
                stripper.writeText(document, output);
            }
            return output.toString();
        } catch (EOFException eofe) {
            String title = "";
            if (document != null) {
                try {
                    title = document.getDocumentInformation().getTitle();
                } catch (Exception e) {
                }
            }
            LOG.error("EOF Exception parsing PDF Document [" + title + "]: " + rid);
            return "";
        } finally {
            if (document != null) {
                try {
                    document.close();
                } catch (Exception e) {
                }
            }
        }
    }

    private void addContent(long resourceId, String fieldName, BinaryAttribute ba, Document doc) throws Exception {
        String contentType = ba.getContentType();
        if ("application/msword".equals(contentType)) {
            try {
                doc.add(new Field(fieldName, parseWorldFile(ba.getInputStream()), Field.Store.NO, Field.Index.TOKENIZED));
            } catch (FastSavedException fse) {
                LOG.error("Document 'fast-saved' indexing not supported: " + resourceId);
                doc.add(new Field("contents", new InputStreamReader(ba.getInputStream())));
            } catch (Exception e) {
                LOG.error("Exception indexing Word file: " + resourceId, e);
                doc.add(new Field(fieldName, new InputStreamReader(ba.getInputStream())));
            }
        } else if (contentType != null && contentType.indexOf("pdf") != -1) {
            doc.add(new Field(fieldName, parsePDFFile(ba.getInputStream(), resourceId), Field.Store.NO, Field.Index.TOKENIZED));
        } else if (contentType != null && contentType.indexOf("text") != -1) {
            doc.add(new Field(fieldName, new InputStreamReader(ba.getInputStream())));
        } else if ("application/vnd.ms-powerpoint".equals(contentType)) {
            MSPowerPointParser parser = new MSPowerPointParser();
            doc.add(new Field(fieldName, parser.getContents(ba.getInputStream()), Field.Store.NO, Field.Index.TOKENIZED));
        } else if ("application/vnd.ms-excel".equals(contentType)) {
            try {
                doc.add(new Field(fieldName, parseExcelFile(ba.getInputStream()), Field.Store.NO, Field.Index.TOKENIZED));
            } catch (FileNotFoundException e) {
                LOG.warn("Could not index Excel file " + resourceId + " " + "probably vervsion prior Excel 97");
            }
        }
    }

    /**
     * This class run as a separate thread and is responsible to call the indexFile method 
     * for each File entry waiting in the queue.
     *
     * @author <a href="mailto:patrick@arpage.ch">Patrick Herber</a>
     */
    class IndexWriterThread extends Thread {

        private long interval;

        private boolean alive = true;

        private boolean canProcess = true;

        public IndexWriterThread(long interval) {
            super("IndexWriterThread");
            setDaemon(true);
            this.interval = interval;
        }

        public void stopExecution() {
            this.alive = false;
        }

        public void setCanProcess(boolean status) {
            this.canProcess = status;
        }

        public void run() {
            while (alive) {
                QueueEntry entry = null;
                if (canProcess) {
                    while (indexQueue.size() > 0) {
                        try {
                            entry = (QueueEntry) indexQueue.remove(0);
                            indexResource(entry.resource, entry.user);
                        } catch (Exception e) {
                            LOG.warn("Exeption trying to remove element from the queue: " + e);
                        }
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Queue size: " + indexQueue.size());
                        }
                    }
                }
                try {
                    sleep(interval);
                } catch (InterruptedException ie) {
                }
            }
        }
    }

    private void close(Directory directory) {
        if (directory != null) {
            try {
                if (IndexReader.isLocked(directory)) {
                    IndexReader.unlock(directory);
                }
            } catch (Exception e) {
            }
            try {
                directory.close();
            } catch (Exception e) {
            }
        }
    }

    private Directory getIndexDirectory(long cid, boolean create) throws Exception {
        return getDirectory(cid, "index", create);
    }

    private Directory getDirectory(long cid, String name, boolean create) throws Exception {
        File communityRoot = new File(repository, Long.toString(cid));
        if (!communityRoot.isDirectory()) {
            if (!communityRoot.mkdir()) {
                LOG.error("Could not create community directory: " + communityRoot.getPath());
                return null;
            }
        }
        File dir = new File(communityRoot, name);
        if (!dir.isDirectory()) {
            if (!dir.mkdir()) {
                LOG.error("Could not create index directory: " + dir.getPath());
                return null;
            }
        }
        return FSDirectory.getDirectory(dir, create);
    }

    private class QueueEntry implements Serializable {

        private static final long serialVersionUID = 1L;

        int type;

        Resource resource;

        User user;

        QueueEntry(Resource resource, User user) {
            this.resource = resource;
            this.user = user;
        }
    }
}
