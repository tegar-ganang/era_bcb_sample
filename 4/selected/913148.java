package org.jcvi.vics.shared.fasta;

import org.apache.log4j.Logger;
import org.jcvi.vics.model.genomics.BaseSequenceEntity;
import org.jcvi.vics.model.user_data.FastaFileNode;
import org.jcvi.vics.shared.genomics.SequenceEntityFactory;
import org.jcvi.vics.shared.utils.FileUtil;
import org.jcvi.vics.shared.utils.InFileChannelHandler;
import org.jcvi.vics.shared.utils.OutFileChannelHandler;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class FASTAFileNodeHelper {

    private static Logger logger = Logger.getLogger(FASTAFileNodeHelper.class);

    static class FASTASequenceCache {

        private static class FASTASequenceCacheEntry {

            private String accession;

            private long pos;

            private BaseSequenceEntity sequenceEntity;

            FASTASequenceCacheEntry(String accession) {
                this.accession = accession;
                this.pos = -1;
                this.sequenceEntity = null;
            }
        }

        private LinkedHashMap<String, FASTASequenceCacheEntry> fastaSequenceCache;

        FASTASequenceCache() {
            fastaSequenceCache = new LinkedHashMap<String, FASTASequenceCacheEntry>();
        }

        void cachePos(String accession, long pos) {
            FASTASequenceCacheEntry cachedSeqeuenceEntry = fastaSequenceCache.get(accession);
            if (cachedSeqeuenceEntry == null) {
                cachedSeqeuenceEntry = new FASTASequenceCacheEntry(accession);
                fastaSequenceCache.put(accession, cachedSeqeuenceEntry);
            }
            cachedSeqeuenceEntry.pos = pos;
        }

        void cacheSequenceEntity(String accession, long pos, String defline, String sequence) {
            FASTASequenceCacheEntry cachedSeqeuenceEntry = fastaSequenceCache.get(accession);
            if (cachedSeqeuenceEntry == null) {
                cachedSeqeuenceEntry = new FASTASequenceCacheEntry(accession);
                fastaSequenceCache.put(accession, cachedSeqeuenceEntry);
            }
            cachedSeqeuenceEntry.pos = pos;
            cachedSeqeuenceEntry.sequenceEntity = SequenceEntityFactory.createSequenceEntity(accession, defline, null, sequence);
        }

        long getSequenceEntityPos(String accession) {
            FASTASequenceCacheEntry cachedSequenceEntry = fastaSequenceCache.get(accession);
            if (cachedSequenceEntry != null) {
                return cachedSequenceEntry.pos;
            } else {
                return -1;
            }
        }

        BaseSequenceEntity getSequenceEntity(String accession) {
            FASTASequenceCacheEntry cachedSequenceEntry = fastaSequenceCache.get(accession);
            if (cachedSequenceEntry != null) {
                return cachedSequenceEntry.sequenceEntity;
            } else {
                return null;
            }
        }
    }

    private static class FASTAFileWalker {

        private static final int START = 0;

        private static final int READDEFLINEID = 1;

        private static final int READDEFLINE = 2;

        private static final int CONTINUEDEFLINEORSEQUENCE = 3;

        private static final int SEQUENCE = 4;

        private static final int READCOMMENT = 5;

        private InFileChannelHandler channelHandler;

        private FASTAFileVisitor fastaFileVisitor;

        FASTAFileWalker(InFileChannelHandler channelHandler) {
            this.channelHandler = channelHandler;
        }

        FASTAFileWalker(InFileChannelHandler channelHandler, FASTAFileVisitor fastaFileVisitor) {
            this.channelHandler = channelHandler;
            this.fastaFileVisitor = fastaFileVisitor;
        }

        void setFastaFileVisitor(FASTAFileVisitor fastaFileVisitor) {
            this.fastaFileVisitor = fastaFileVisitor;
        }

        void traverseFile() throws Exception {
            FASTAFileTokenizer tokenizer = new FASTAFileTokenizer(channelHandler);
            int state = START;
            fastaFileVisitor.begin();
            int token;
            for (; ; ) {
                if (fastaFileVisitor.isDone()) {
                    break;
                }
                long currentPos = tokenizer.getPosition();
                token = tokenizer.nextToken();
                if (token == FASTAFileTokenizer.EOF) {
                    fastaFileVisitor.done();
                    break;
                }
                char c = (char) tokenizer.getTokenValue();
                switch(state) {
                    case START:
                        if (token == FASTAFileTokenizer.DEFLINESTART) {
                            fastaFileVisitor.beginDeflineId(currentPos);
                            state = READDEFLINEID;
                        }
                        break;
                    case READCOMMENT:
                        if (token == FASTAFileTokenizer.NL) {
                            state = CONTINUEDEFLINEORSEQUENCE;
                        } else {
                            fastaFileVisitor.visitCommentPart(c, currentPos);
                        }
                        break;
                    case READDEFLINEID:
                        if (token == FASTAFileTokenizer.OTHERBYTE) {
                            if (Character.isLetterOrDigit(c) || c == '-' || c == '_' || c == '.') {
                                fastaFileVisitor.visitIdPart(c, currentPos);
                            } else {
                                fastaFileVisitor.endDeflineId(currentPos);
                                state = READDEFLINE;
                            }
                        } else if (token == FASTAFileTokenizer.NL) {
                            fastaFileVisitor.endDeflineId(currentPos);
                            state = CONTINUEDEFLINEORSEQUENCE;
                        }
                        break;
                    case READDEFLINE:
                        if (token == FASTAFileTokenizer.OTHERBYTE) {
                            fastaFileVisitor.visitDeflinePart(c, currentPos);
                        } else if (token == FASTAFileTokenizer.NL) {
                            state = CONTINUEDEFLINEORSEQUENCE;
                        } else if (token == FASTAFileTokenizer.DEFLINESTART) {
                            throw new IllegalArgumentException("Invalid character " + "'" + c + "'" + " at " + tokenizer.getPosition());
                        }
                        break;
                    case CONTINUEDEFLINEORSEQUENCE:
                        if (token == FASTAFileTokenizer.DEFLINESTART) {
                            fastaFileVisitor.beginDeflineId(currentPos);
                            state = READDEFLINEID;
                        } else if (token == FASTAFileTokenizer.OTHERBYTE && c == ';') {
                            state = READCOMMENT;
                        } else if (token == FASTAFileTokenizer.NL) {
                        } else {
                            fastaFileVisitor.visitSequencePart(c, currentPos);
                            state = SEQUENCE;
                        }
                        break;
                    case SEQUENCE:
                        if (token == FASTAFileTokenizer.OTHERBYTE) {
                            fastaFileVisitor.visitSequencePart(c, currentPos);
                        } else if (token == FASTAFileTokenizer.DEFLINESTART) {
                            state = READDEFLINEID;
                        } else if (token == FASTAFileTokenizer.NL) {
                            state = CONTINUEDEFLINEORSEQUENCE;
                        }
                        break;
                }
            }
        }
    }

    private abstract static class FASTAFileVisitor {

        FASTAFileVisitor() {
        }

        abstract void begin();

        abstract void beginDeflineId(long pos);

        abstract void done();

        abstract void endDeflineId(long pos);

        abstract boolean isDone();

        abstract void visitCommentPart(char c, long pos);

        abstract void visitDeflinePart(char c, long pos);

        abstract void visitIdPart(char c, long pos);

        abstract void visitSequencePart(char c, long pos);
    }

    private static class IndexingFASTAFileVisitor extends FASTAFileVisitor {

        private WriteFASTAIndexHandler indexHandler;

        private long tentativeIdPos = -1;

        private long nSequences = -1;

        private long nBases = 0;

        private String entityId = null;

        private StringBuffer idBuffer = new StringBuffer();

        IndexingFASTAFileVisitor(OutFileChannelHandler channelHandler) {
            indexHandler = new WriteFASTAIndexHandler(channelHandler, null);
        }

        void begin() {
            try {
                indexHandler.skipNRecords();
                nSequences = 0;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        void beginDeflineId(long pos) {
            tentativeIdPos = pos;
        }

        void done() {
            try {
                indexHandler.setNRecords(nSequences);
                indexHandler.setNBases(nBases);
                indexHandler.writeSignature();
                indexHandler.flush();
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                try {
                    indexHandler.close();
                } catch (Exception ignore) {
                }
            }
        }

        void endDeflineId(long pos) {
            if (idBuffer.length() > 0) {
                if (entityId == null || !idBuffer.toString().equals(entityId)) {
                    try {
                        indexHandler.writeId(idBuffer.toString(), tentativeIdPos);
                        nSequences++;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
                entityId = idBuffer.toString();
                idBuffer.setLength(0);
            }
        }

        boolean isDone() {
            return false;
        }

        void visitCommentPart(char c, long pos) {
        }

        void visitDeflinePart(char c, long pos) {
        }

        void visitIdPart(char c, long pos) {
            idBuffer.append(c);
        }

        void visitSequencePart(char c, long pos) {
            nBases++;
        }
    }

    private static class MultiSequenceSearchFASTAFileVisitor extends FASTAFileVisitor {

        private Set<String> sequenceAccessions;

        private String entityId = null;

        private long tentativeIdPos = -1;

        private long entityIdPos = -1;

        private StringBuffer idBuffer = new StringBuffer();

        private StringBuffer deflineBuffer = new StringBuffer();

        private StringBuffer sequenceBuffer = new StringBuffer();

        private Set<BaseSequenceEntity> sequenceEntities;

        private FASTASequenceCache fastaSequenceCache;

        MultiSequenceSearchFASTAFileVisitor(Set<String> sequenceAccessions, FASTASequenceCache fastaSequenceCache) {
            this.sequenceAccessions = sequenceAccessions;
            this.fastaSequenceCache = fastaSequenceCache;
            sequenceEntities = new HashSet<BaseSequenceEntity>();
        }

        Set<BaseSequenceEntity> getSequenceEntities() {
            return sequenceEntities;
        }

        void begin() {
        }

        void beginDeflineId(long pos) {
            tentativeIdPos = pos;
            if (entityIdPos == -1) {
                entityIdPos = pos;
            }
        }

        void done() {
            if (fastaSequenceCache != null && fastaSequenceCache.getSequenceEntity(entityId) == null) {
                fastaSequenceCache.cacheSequenceEntity(entityId, entityIdPos, deflineBuffer.toString(), sequenceBuffer.toString());
            }
            if (sequenceAccessions == null || sequenceAccessions.contains(entityId)) {
                sequenceEntities.add(SequenceEntityFactory.createSequenceEntity(entityId, deflineBuffer.toString(), null, sequenceBuffer.toString()));
                if (sequenceAccessions != null) {
                    sequenceAccessions.remove(entityId);
                }
            }
        }

        void endDeflineId(long pos) {
            if (idBuffer.length() > 0) {
                if (entityId == null || !idBuffer.toString().equals(entityId)) {
                    if (entityId != null) {
                        if (fastaSequenceCache != null) {
                            fastaSequenceCache.cacheSequenceEntity(entityId, entityIdPos, deflineBuffer.toString(), sequenceBuffer.toString());
                        }
                        if (sequenceAccessions == null || sequenceAccessions.contains(entityId)) {
                            if (fastaSequenceCache != null) {
                                sequenceEntities.add(fastaSequenceCache.getSequenceEntity(entityId));
                            } else {
                                sequenceEntities.add(SequenceEntityFactory.createSequenceEntity(entityId, deflineBuffer.toString(), null, sequenceBuffer.toString()));
                            }
                            if (sequenceAccessions != null) {
                                sequenceAccessions.remove(entityId);
                            }
                        }
                    }
                    entityIdPos = tentativeIdPos;
                    deflineBuffer.setLength(0);
                    sequenceBuffer.setLength(0);
                }
                entityId = idBuffer.toString();
                idBuffer.setLength(0);
            }
        }

        boolean isDone() {
            return sequenceAccessions != null && sequenceAccessions.size() == 0;
        }

        void visitCommentPart(char c, long pos) {
        }

        void visitDeflinePart(char c, long pos) {
            deflineBuffer.append(c);
        }

        void visitIdPart(char c, long pos) {
            idBuffer.append(c);
        }

        void visitSequencePart(char c, long pos) {
            sequenceBuffer.append(c);
        }
    }

    private static class SequenceSearchFASTAFileVisitor extends FASTAFileVisitor {

        private String sequenceAccessionNo;

        private String entityId = null;

        private long tentativeIdPos = -1;

        private long entityIdPos = -1;

        private boolean doneFlag = false;

        private StringBuffer idBuffer = new StringBuffer();

        private StringBuffer deflineBuffer = new StringBuffer();

        private StringBuffer sequenceBuffer = new StringBuffer();

        private BaseSequenceEntity sequenceEntity;

        private FASTASequenceCache fastaSequenceCache;

        SequenceSearchFASTAFileVisitor(String sequenceAccessionNo, FASTASequenceCache fastaSequenceCache) {
            this.sequenceAccessionNo = sequenceAccessionNo;
            this.fastaSequenceCache = fastaSequenceCache;
        }

        BaseSequenceEntity getSequenceEntity() {
            return sequenceEntity;
        }

        boolean foundSeqeuence() {
            return doneFlag;
        }

        void begin() {
        }

        void beginDeflineId(long pos) {
            tentativeIdPos = pos;
            if (entityIdPos == -1) {
                entityIdPos = pos;
            }
        }

        void done() {
            if (fastaSequenceCache != null && fastaSequenceCache.getSequenceEntity(entityId) == null) {
                fastaSequenceCache.cacheSequenceEntity(entityId, entityIdPos, deflineBuffer.toString(), sequenceBuffer.toString());
            }
            if (entityId.equals(sequenceAccessionNo)) {
                if (fastaSequenceCache != null) {
                    sequenceEntity = fastaSequenceCache.getSequenceEntity(entityId);
                } else {
                    sequenceEntity = SequenceEntityFactory.createSequenceEntity(entityId, deflineBuffer.toString(), null, sequenceBuffer.toString());
                }
                doneFlag = true;
            }
        }

        void endDeflineId(long pos) {
            if (idBuffer.length() > 0) {
                if (entityId == null || !idBuffer.toString().equals(entityId)) {
                    if (entityId != null) {
                        if (fastaSequenceCache != null) {
                            fastaSequenceCache.cacheSequenceEntity(entityId, entityIdPos, deflineBuffer.toString(), sequenceBuffer.toString());
                        }
                        if (entityId.equals(sequenceAccessionNo)) {
                            if (fastaSequenceCache != null) {
                                sequenceEntity = fastaSequenceCache.getSequenceEntity(entityId);
                            } else {
                                sequenceEntity = SequenceEntityFactory.createSequenceEntity(entityId, deflineBuffer.toString(), null, sequenceBuffer.toString());
                            }
                            doneFlag = true;
                        }
                    }
                    entityIdPos = tentativeIdPos;
                    deflineBuffer.setLength(0);
                    sequenceBuffer.setLength(0);
                }
                entityId = idBuffer.toString();
                idBuffer.setLength(0);
            }
        }

        boolean isDone() {
            return doneFlag;
        }

        void visitCommentPart(char c, long pos) {
        }

        void visitDeflinePart(char c, long pos) {
            deflineBuffer.append(c);
        }

        void visitIdPart(char c, long pos) {
            idBuffer.append(c);
        }

        void visitSequencePart(char c, long pos) {
            sequenceBuffer.append(c);
        }
    }

    private abstract static class FASTAIndexHandler {

        protected static final String FASTAINDEXFILESIGNATURE = "FASTANDX";

        protected FASTASequenceCache fastaSequenceCache;

        protected long nSequences;

        protected long nBases;

        FASTAIndexHandler(FASTASequenceCache fastaSequenceCache) {
            this.fastaSequenceCache = fastaSequenceCache;
            nSequences = -1;
            nBases = -1;
        }

        public long getNBases() {
            return nBases;
        }

        public void setNBases(long nBases) {
            this.nBases = nBases;
        }

        long getNRecords() {
            return nSequences;
        }

        void setNRecords(long nRecords) {
            this.nSequences = nRecords;
        }

        abstract void skipNRecords() throws Exception;
    }

    private static class ReadFASTAIndexHandler extends FASTAIndexHandler {

        private InFileChannelHandler channelHandler;

        ReadFASTAIndexHandler(InFileChannelHandler channelHandler, FASTASequenceCache fastaSequenceCache) {
            super(fastaSequenceCache);
            this.channelHandler = channelHandler;
        }

        void close() throws Exception {
            channelHandler.close();
        }

        long searchId(String id) throws Exception {
            long idPos = -1;
            if (id == null) {
                return idPos;
            }
            skipNRecords();
            for (; ; ) {
                if (channelHandler.eof()) {
                    break;
                }
                String currId = channelHandler.readString();
                long currPos = channelHandler.readLong();
                if (fastaSequenceCache != null) {
                    fastaSequenceCache.cachePos(currId, currPos);
                }
                if (currId != null && currId.equals(id)) {
                    idPos = currPos;
                    break;
                }
            }
            return idPos;
        }

        void searchIds(Set<String> ids, Map<String, Long> idPosMap) throws Exception {
            if (ids == null || ids.size() == 0) {
                return;
            }
            skipNRecords();
            for (; ; ) {
                if (channelHandler.eof()) {
                    break;
                }
                String currId = channelHandler.readString();
                long currPos = channelHandler.readLong();
                if (currId != null && ids.contains(currId)) {
                    idPosMap.put(currId, currPos);
                    ids.remove(currId);
                    if (ids.size() == 0) {
                        break;
                    }
                }
            }
        }

        boolean verifySignature() throws Exception {
            boolean result = false;
            channelHandler.setPosition(0);
            String signature = channelHandler.readString();
            if (FASTAINDEXFILESIGNATURE.equals(signature)) {
                nSequences = channelHandler.readLong();
                result = true;
            }
            return result;
        }

        void skipNRecords() throws Exception {
            channelHandler.setPosition(FASTAINDEXFILESIGNATURE.length() + 4 + 8);
        }
    }

    private static class WriteFASTAIndexHandler extends FASTAIndexHandler {

        private OutFileChannelHandler channelHandler;

        WriteFASTAIndexHandler(OutFileChannelHandler channelHandler, FASTASequenceCache fastaSequenceCache) {
            super(fastaSequenceCache);
            this.channelHandler = channelHandler;
        }

        void close() throws Exception {
            channelHandler.close();
        }

        void flush() throws Exception {
            channelHandler.flush();
        }

        void writeId(String id, long pos) throws Exception {
            channelHandler.writeString(id);
            channelHandler.writeLong(pos);
        }

        void writeSignature() throws Exception {
            channelHandler.setPosition(0);
            channelHandler.writeString(FASTAINDEXFILESIGNATURE);
            channelHandler.writeLong(nSequences);
        }

        void skipNRecords() throws Exception {
            channelHandler.setPosition(FASTAINDEXFILESIGNATURE.length() + 4 + 8);
        }
    }

    private LinkedHashMap<Long, FASTASequenceCache> fastaNodesCache;

    public FASTAFileNodeHelper() {
        this(false);
    }

    public FASTAFileNodeHelper(boolean useCache) {
        if (useCache) {
            fastaNodesCache = new LinkedHashMap<Long, FASTASequenceCache>();
        }
    }

    public BaseSequenceEntity readSequence(FastaFileNode fastaFileNode, String accessionNo) throws Exception {
        BaseSequenceEntity sequenceEntity = null;
        RandomAccessFile fastaFile = null;
        long startReadSequenceTime = System.currentTimeMillis();
        long endReadSequenceTime;
        try {
            String fastaFileLocation = fastaFileNode.getDirectoryPath();
            if (!FileUtil.fileExists(fastaFileLocation)) {
                throw new IllegalArgumentException("FASTA file location " + "'" + fastaFileLocation + "'" + " not found for " + fastaFileNode.getObjectId());
            }
            FASTASequenceCache fastaSequenceCache = null;
            if (fastaNodesCache != null) {
                fastaSequenceCache = fastaNodesCache.get(fastaFileNode.getObjectId());
                if (fastaSequenceCache == null) {
                    fastaSequenceCache = new FASTASequenceCache();
                    fastaNodesCache.put(fastaFileNode.getObjectId(), fastaSequenceCache);
                }
                sequenceEntity = fastaSequenceCache.getSequenceEntity(accessionNo);
            }
            if (sequenceEntity == null) {
                long seqPos = -1;
                if (fastaSequenceCache != null) {
                    fastaSequenceCache.getSequenceEntityPos(accessionNo);
                }
                if (seqPos < 0) {
                    seqPos = searchSequencePos(fastaFileNode.getFastaIndexFilePath(), accessionNo, fastaSequenceCache);
                }
                fastaFile = new RandomAccessFile(fastaFileNode.getFastaFilePath(), "r");
                InFileChannelHandler fastaFileChannelHandler = new InFileChannelHandler(fastaFile.getChannel());
                if (seqPos > 0) {
                    fastaFileChannelHandler.setPosition(seqPos);
                }
                sequenceEntity = readSequence(fastaFileChannelHandler, accessionNo, fastaSequenceCache);
            }
        } finally {
            if (fastaFile != null) {
                try {
                    fastaFile.close();
                } catch (Exception ignore) {
                }
            }
            endReadSequenceTime = System.currentTimeMillis();
        }
        logger.debug("Finished reading sequence for " + accessionNo + " in " + (endReadSequenceTime - startReadSequenceTime) + "ms");
        return sequenceEntity;
    }

    public Set<BaseSequenceEntity> readSequences(FastaFileNode fastaFileNode, Set<String> accessions) throws Exception {
        String fastaFileLocation = fastaFileNode.getDirectoryPath();
        if (!FileUtil.fileExists(fastaFileLocation)) {
            throw new IllegalArgumentException("FASTA file location " + "'" + fastaFileLocation + "'" + " not found for " + fastaFileNode.getObjectId());
        }
        return readSequences(fastaFileNode.getFastaFilePath(), fastaFileNode.getFastaIndexFilePath(), accessions);
    }

    public void indexFASTAFile(FastaFileNode fastaFileNode) throws Exception {
        String fastaFileLocation = fastaFileNode.getDirectoryPath();
        if (!FileUtil.fileExists(fastaFileLocation)) {
            throw new IllegalArgumentException("FASTA file location " + "'" + fastaFileLocation + "'" + " not found for " + fastaFileNode.getObjectId());
        }
        createFASTAIndexFile(fastaFileNode.getFastaFilePath(), fastaFileNode.getFastaIndexFilePath());
    }

    void createFASTAIndexFile(String fastaFileName, String fastaIndexFileName) throws Exception {
        RandomAccessFile fastaFile = null;
        RandomAccessFile fastaFileIndex = null;
        long startCreateIndexTime = System.currentTimeMillis();
        long endCreateIndexTime;
        try {
            fastaFile = new RandomAccessFile(fastaFileName, "r");
            fastaFileIndex = new RandomAccessFile(fastaIndexFileName, "rw");
            InFileChannelHandler fastaFileChannelHandler = new InFileChannelHandler(fastaFile.getChannel());
            OutFileChannelHandler fastaFileIndexChannelHandler = new OutFileChannelHandler(fastaFileIndex.getChannel());
            IndexingFASTAFileVisitor indexingVisitor = new IndexingFASTAFileVisitor(fastaFileIndexChannelHandler);
            FASTAFileWalker fastaWalker = new FASTAFileWalker(fastaFileChannelHandler, indexingVisitor);
            fastaWalker.traverseFile();
        } finally {
            if (fastaFile != null) {
                try {
                    fastaFile.close();
                } catch (Exception ignore) {
                }
            }
            if (fastaFileIndex != null) {
                try {
                    fastaFileIndex.close();
                } catch (Exception ignore) {
                }
            }
            endCreateIndexTime = System.currentTimeMillis();
        }
        logger.debug("Finished creating FASTA index file " + fastaIndexFileName + " in " + (endCreateIndexTime - startCreateIndexTime) + "ms");
    }

    BaseSequenceEntity readSequence(InFileChannelHandler fastaFileChannelHandler, String accessionNo, FASTASequenceCache fastaSequenceCache) throws Exception {
        BaseSequenceEntity sequenceEntity = null;
        SequenceSearchFASTAFileVisitor sequenceSearchVisitor = new SequenceSearchFASTAFileVisitor(accessionNo, fastaSequenceCache);
        FASTAFileWalker fastaWalker = new FASTAFileWalker(fastaFileChannelHandler, sequenceSearchVisitor);
        fastaWalker.traverseFile();
        if (sequenceSearchVisitor.foundSeqeuence()) {
            sequenceEntity = sequenceSearchVisitor.getSequenceEntity();
        }
        return sequenceEntity;
    }

    Set<BaseSequenceEntity> readSequences(String fastaFileName, String fastaFileIndexName, Set<String> accessions) throws Exception {
        Set<BaseSequenceEntity> sequenceEntities = null;
        RandomAccessFile fastaFile = null;
        long startReadSequenceTime = System.currentTimeMillis();
        long endReadSequenceTime;
        int nSequences = -1;
        try {
            Map<String, Long> seqPosMap = null;
            if (accessions != null) {
                nSequences = accessions.size();
                seqPosMap = searchSequencesPos(fastaFileIndexName, accessions);
            }
            fastaFile = new RandomAccessFile(fastaFileName, "r");
            InFileChannelHandler fastaFileChannelHandler = new InFileChannelHandler(fastaFile.getChannel());
            sequenceEntities = readSequences(fastaFileChannelHandler, seqPosMap);
        } finally {
            if (fastaFile != null) {
                try {
                    fastaFile.close();
                } catch (Exception ignore) {
                }
            }
            endReadSequenceTime = System.currentTimeMillis();
        }
        logger.debug("Finished reading " + (nSequences >= 0 ? nSequences : "all") + " sequences in " + (endReadSequenceTime - startReadSequenceTime) + "ms");
        return sequenceEntities;
    }

    Set<BaseSequenceEntity> readSequences(InFileChannelHandler fastaFileChannelHandler, Map<String, Long> accessionPosMap) throws Exception {
        Set<BaseSequenceEntity> sequenceEntities;
        boolean allIndexedFlag = false;
        if (accessionPosMap != null) {
            allIndexedFlag = true;
            for (String accession : accessionPosMap.keySet()) {
                Long pos = accessionPosMap.get(accession);
                if (pos == null || pos < 0) {
                    allIndexedFlag = false;
                    break;
                }
            }
        }
        if (allIndexedFlag) {
            sequenceEntities = new HashSet<BaseSequenceEntity>();
            FASTAFileWalker fastaWalker = new FASTAFileWalker(fastaFileChannelHandler);
            for (String accession : accessionPosMap.keySet()) {
                Long pos = accessionPosMap.get(accession);
                SequenceSearchFASTAFileVisitor sequenceSearchVisitor = new SequenceSearchFASTAFileVisitor(accession, null);
                fastaFileChannelHandler.setPosition(pos);
                fastaWalker.setFastaFileVisitor(sequenceSearchVisitor);
                fastaWalker.traverseFile();
                if (sequenceSearchVisitor.foundSeqeuence()) {
                    sequenceEntities.add(sequenceSearchVisitor.getSequenceEntity());
                }
            }
        } else {
            FASTAFileWalker fastaWalker = new FASTAFileWalker(fastaFileChannelHandler);
            MultiSequenceSearchFASTAFileVisitor sequenceSearchVisitor = new MultiSequenceSearchFASTAFileVisitor(accessionPosMap == null ? null : accessionPosMap.keySet(), null);
            fastaFileChannelHandler.setPosition(0);
            fastaWalker.setFastaFileVisitor(sequenceSearchVisitor);
            fastaWalker.traverseFile();
            sequenceEntities = sequenceSearchVisitor.getSequenceEntities();
        }
        return sequenceEntities;
    }

    long searchSequencePos(String fastaIndexFileName, String accessionNo, FASTASequenceCache fastaSequenceCache) throws Exception {
        long seqPos = -1;
        if (FileUtil.fileExists(fastaIndexFileName)) {
            RandomAccessFile fastaFileIndex = null;
            try {
                fastaFileIndex = new RandomAccessFile(fastaIndexFileName, "r");
                InFileChannelHandler fastaFileIndexChannelHandler = new InFileChannelHandler(fastaFileIndex.getChannel());
                ReadFASTAIndexHandler fastaIndex = new ReadFASTAIndexHandler(fastaFileIndexChannelHandler, fastaSequenceCache);
                if (fastaIndex.verifySignature()) {
                    seqPos = fastaIndex.searchId(accessionNo);
                }
                fastaIndex.close();
            } finally {
                if (fastaFileIndex != null) {
                    try {
                        fastaFileIndex.close();
                    } catch (IOException ignore) {
                    }
                }
            }
        }
        return seqPos;
    }

    Map<String, Long> searchSequencesPos(String fastaIndexFileName, Set<String> accessions) throws Exception {
        Map<String, Long> accessionsPosMap = new LinkedHashMap<String, Long>();
        if (FileUtil.fileExists(fastaIndexFileName)) {
            RandomAccessFile fastaFileIndex = null;
            try {
                fastaFileIndex = new RandomAccessFile(fastaIndexFileName, "r");
                InFileChannelHandler fastaFileIndexChannelHandler = new InFileChannelHandler(fastaFileIndex.getChannel());
                ReadFASTAIndexHandler fastaIndex = new ReadFASTAIndexHandler(fastaFileIndexChannelHandler, null);
                if (fastaIndex.verifySignature()) {
                    fastaIndex.searchIds(accessions, accessionsPosMap);
                }
                fastaIndex.close();
            } finally {
                if (fastaFileIndex != null) {
                    try {
                        fastaFileIndex.close();
                    } catch (IOException ignore) {
                    }
                }
            }
        }
        for (String id : accessions) {
            accessionsPosMap.put(id, -1L);
        }
        return accessionsPosMap;
    }
}
