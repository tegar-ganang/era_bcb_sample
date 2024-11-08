package com.coladoro.plugin.ed2k.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.exception.NestableException;
import org.apache.mina.common.IoBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reader of the server met file. It has all the servers in a list.
 * 
 * @author tanis
 */
public class Ed2kServerMetReader {

    /**
     * The server met file.
     */
    private File serverMet;

    /**
     * The list of servers parsed.
     */
    private List<Ed2kServer> serverList;

    /**
     * The header that must come with all the server met files to be accepted.
     */
    public static final short SERVER_MET_HEADER = 0xE0;

    /**
     * The Log of this class.
     */
    private final Logger logger = LoggerFactory.getLogger(Ed2kServerMetReader.class);

    /**
     * @param serverMet
     * @throws NestableException
     */
    public Ed2kServerMetReader(File serverMet) throws NestableException {
        this.serverMet = serverMet;
        try {
            if (!serverMet.exists()) {
                serverMet.createNewFile();
            }
        } catch (IOException e) {
            logger.error("Could not create server met file in " + serverMet.getAbsolutePath(), e);
            throw new NestableException(e);
        }
    }

    /**
     * @param forceReReading
     * @throws NestableException
     */
    public void readServerMet(boolean forceReReading) throws NestableException {
        if (serverList == null || forceReReading) {
            readServerMet();
        } else {
            return;
        }
    }

    /**
     * Reads the server met file represented by the file that went as an
     * argument in the constructor.
     * 
     * @throws NestableException
     *                 if something fails reading.
     */
    private void readServerMet() throws NestableException {
        FileInputStream serverMet = null;
        serverList = new ArrayList<Ed2kServer>();
        try {
            serverMet = new FileInputStream(this.serverMet);
        } catch (FileNotFoundException e) {
            logger.error("Server met file not found", e);
        }
        FileChannel fileReader = serverMet.getChannel();
        ByteBuffer fileBuffer = ByteBuffer.allocate((int) this.serverMet.length());
        try {
            fileReader.read(fileBuffer);
            IoBuffer realBuffer = IoBuffer.wrap(fileBuffer);
            realBuffer.order(ByteOrder.LITTLE_ENDIAN);
            realBuffer.rewind();
            short header = realBuffer.getUnsigned();
            if (header != SERVER_MET_HEADER) {
                throw new NestableException("Server met corrupt, header found: " + header);
            }
            long numberOfServers = realBuffer.getUnsignedInt();
            if (logger.isDebugEnabled()) {
                logger.debug("Number of servers to parse: " + numberOfServers);
            }
            for (int i = 0; i < numberOfServers; i++) {
                Ed2kServer server = Ed2kServer.readServer(realBuffer, i);
                if (logger.isDebugEnabled()) {
                    logger.debug("Server parsed: " + server);
                }
                serverList.add(server);
            }
        } catch (IOException e) {
            logger.error("Error reading server met file", e);
            throw new NestableException(e);
        }
    }

    /**
     * @return the serverMet
     */
    public File getServerMet() {
        return serverMet;
    }

    /**
     * @param serverMet
     *                the serverMet to set
     */
    public void setServerMet(File serverMet) {
        this.serverMet = serverMet;
    }

    /**
     * @return the serverList
     */
    public List<Ed2kServer> getServerList() {
        return serverList;
    }
}
