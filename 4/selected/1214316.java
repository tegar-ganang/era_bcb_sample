package org.amlfilter.loader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import org.amlfilter.service.GenericService;
import org.amlfilter.service.LoggerService;
import org.amlfilter.service.SynonymServiceInterface;
import org.hibernate.SessionFactory;
import org.springframework.orm.hibernate3.HibernateTransactionManager;

public class SuspectsLoader extends GenericService {

    /**
     * The session factory
     */
    private SessionFactory mSessionFactory;

    private HibernateTransactionManager mTransactionManager;

    /**
     * Get the session factory
     * @return The session factory
     */
    public SessionFactory getSessionFactory() {
        return mSessionFactory;
    }

    /**
     * Set the session factory
     * @param pSessionFactory The session factory
     */
    public void setSessionFactory(SessionFactory pSessionFactory) {
        mSessionFactory = pSessionFactory;
    }

    /**
     * Get the transaction manager
     * @return The transaction manager
     */
    public HibernateTransactionManager getTransactionManager() {
        return mTransactionManager;
    }

    /**
     * Set the transaction manager
     * @param pTransactionManager The transaction manager
     */
    public void setTransactionManager(HibernateTransactionManager pTransactionManager) {
        mTransactionManager = pTransactionManager;
    }

    private SynonymServiceInterface mSynonymService;

    /**
     * Get the synonym service
     * @return The synonym service
     */
    public SynonymServiceInterface getSynonymService() {
        return mSynonymService;
    }

    public void setSynonymService(SynonymServiceInterface pSynonymService) {
        mSynonymService = pSynonymService;
    }

    /**
     * The file size
     */
    protected long mFileSize = 0;

    protected StringBuilder mEntitiesLog;

    /**
     * Get the entities log
     * @return The entities log
     */
    public StringBuilder getEntitiesLog() {
        return mEntitiesLog;
    }

    protected long mTotalTime = -1L;

    /**
     * Get the total time
     * @retutn The total time
     */
    public long getTotalTime() {
        return mTotalTime;
    }

    /**
     * Set the total time
     * @param pTotalTime The total time
     */
    public void setTotalTime(long pTotalTime) {
        mTotalTime = pTotalTime;
    }

    protected void resetVariables() {
        mTotalTime = 0;
        mEntitiesLog = new StringBuilder();
    }

    protected RandomAccessFile getFile(String pFileName) throws FileNotFoundException {
        File input = new File(pFileName);
        RandomAccessFile raf = new RandomAccessFile(input, "r");
        return raf;
    }

    protected MappedByteBuffer getMappedBuffer(RandomAccessFile pRaf) throws IOException {
        FileChannel channel = pRaf.getChannel();
        int fileLength = (int) channel.size();
        MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, fileLength);
        return buffer;
    }

    protected long getFileSize() throws IOException {
        return mFileSize;
    }

    protected void setFileSize(long pFileSize) {
        mFileSize = pFileSize;
    }
}
