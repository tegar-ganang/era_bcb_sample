package com.dcivision.framework.notification;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import javax.activation.DataSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.LogFactoryImpl;

/**
  <p>Class Name:    EmailDeliveryMessage.java    </p>
  <p>Description:   This class implements the interface DataSource from: </p>
  <p>                  - an InputStream </p>
  <p>                  - a byte array </p>
  <p>                  - a String </p>

    @author          Zoe Shum
    @company         DCIVision Limited
    @creation date   02/07/2003</p>
    @version         $Revision: 1.7.32.1 $
*/
public class ByteArrayDataSource implements DataSource {

    public static final String REVISION = "$Revision: 1.7.32.1 $";

    protected Log m_log = new LogFactoryImpl().getInstance(this.getClass());

    private static final String DEFAULT_TYPE = "iso-8859-1";

    private byte[] m_abyteData;

    private String m_sType = DEFAULT_TYPE;

    /**
   *  Constructor - create a DataSource  from an input stream.
   *
   *  @param      is                     The InputStream type of data source for pass in as content
   *  @param      sType                  The type of content
   *  @throws     No exception throws
   *
   */
    public ByteArrayDataSource(InputStream is, String sType) {
        this.m_sType = sType;
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            int nCh;
            while ((nCh = is.read()) != -1) os.write(nCh);
            m_abyteData = os.toByteArray();
        } catch (IOException ioex) {
            m_log.error("IOException caught in reading input stream with type=" + sType, ioex);
        }
    }

    /**
   *  Constructor - create a DataSource  from a byte array.
   *
   *  @param      abyteData              The ByteArray type of data source for pass in as content
   *  @param      sType                  The type of content
   *  @return     No return value
   *  @throws     No exception throws
   *
   */
    public ByteArrayDataSource(byte[] abyteData, String sType) {
        this.m_abyteData = abyteData;
        this.m_sType = sType;
    }

    /**
   *  Constructor - create a DataSource  from a String.
   *
   *  @param      sData                  The String type of data source for pass in content
   *  @param      sType                  The type of content
   *  @throws     No exception throws
   *
   */
    public ByteArrayDataSource(String sData, String sType) {
        try {
            if (sType != null) {
                this.m_sType = sType;
            }
            if ("".equals(sType)) {
                this.m_abyteData = sData.getBytes();
            } else {
                this.m_abyteData = sData.getBytes(this.m_sType);
            }
        } catch (UnsupportedEncodingException uex) {
            m_log.error("Error when parsing string for mail:" + uex);
        }
    }

    /**
   *  Method getInputStream() - return the InputStream for the data input.
   *  Note: a new stream must be returned each time.
   *
   *  @param      No pass in parameter
   *  @return     InputStream             return InputStream that passed in
   *  @throws     IOException             The IOException throws if no data is found
   */
    public InputStream getInputStream() throws IOException {
        if (m_abyteData == null) {
            throw new IOException("no data");
        }
        return new ByteArrayInputStream(m_abyteData);
    }

    /**
   *  Method getOutputStream() - return the OutputStream.
   *
   *  @param      No pass in parameter
   *  @return     OutputStream            The OutputStream type
   *  @throws     IOException             The IOException throws if cannot parse the data input
   */
    public OutputStream getOutputStream() throws IOException {
        throw new IOException("cannot do this");
    }

    /**
   *  Method getContentType() - return the type of content input.
   *
   *  @param      No pass in parameter
   *  @return     String                   The type of content type
   *  @throws     No Exception throws
   */
    public String getContentType() {
        return m_sType;
    }

    /**
   *  Method getName() - return the name.
   *
   *  @param      No pass in parameter
   *  @return     String                    The name
   *  @throws     No Exception throws
   */
    public String getName() {
        return "dummy";
    }
}
