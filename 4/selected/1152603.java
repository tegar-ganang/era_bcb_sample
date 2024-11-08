package org.epo.jpxi.clients.test;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UID;
import java.sql.Timestamp;
import java.util.Random;
import java.util.StringTokenizer;
import org.apache.log4j.Logger;
import org.epo.jpxi.rmi.*;
import org.epo.jpxi.shared.*;
import org.epoline.jsf.utils.Log4jManager;
import java.io.*;

/**
 * TestStorageEfficiency : 	Store "TIFF-List" documents into TIFF formats
 *                          (Modify line 259, to change storage format to ST33) 
 * 							Stop when JPXI all buffers are full;
 * 		Tiff images are choiced randomly in a data directory
 * 		Documents are generated with a random number of pages [1-50]
 *
 * Creation date: (29/01/2003 13:44:03)
 */
public class TestStorageEfficiency {

    /**
	 * TestDLTiffList standard constructor.
	 */
    public TestStorageEfficiency() {
        super();
    }

    /**
	 * Convert an array of char into an array of byte
	 *  It's usefull to generate the Meta-Data if the target format is ST33
	 * Creation date: (17/09/2001 09:26:38)
	 * @return byte[]
	 * @param numPageChar char[]
	 */
    private static byte[] charArray2ByteArray(char[] numPageChar) {
        byte temp = 0;
        byte[] numPageByte = new byte[4];
        for (int i = 0; i < numPageChar.length; i++) {
            temp = (byte) numPageChar[i];
            numPageByte[i] = temp;
        }
        return numPageByte;
    }

    /**
	 * fill a number with '0' if the size of the string is
	 *  strictly smaller than 4.
	 *  Example: 12 => 0012; 1 => 0001
	 *  It's usefull to generate the Meta-Data if the target format is ST33.
	 * Creation date: (17/09/2001 09:28:27)
	 * @return char[]
	 * @param numpage int
	 */
    private static char[] int2Char(int numPage) {
        Integer tmp = new Integer(numPage);
        String numPageStr = tmp.toString();
        int length = numPageStr.length();
        char[] numPageChar = new char[4];
        for (int i = 4; i > length; i--) {
            numPageStr = "0" + numPageStr;
        }
        numPageChar = numPageStr.toCharArray();
        return numPageChar;
    }

    /**
	 * MainArgs: %Jpxi URL%;%ParamFilePath%
	   This method can start a number of session define in the ParamFile
	 * Creation date: (14/09/2001 11:34:18)
	 * @param arg java.lang.String[]
	 */
    public static void main(String[] arg) {
        String myServiceURL = null;
        String myTiffDir = null;
        if (arg.length != 2) {
            System.exit(1);
        }
        myServiceURL = arg[0];
        StringTokenizer myStrTok = new StringTokenizer(myServiceURL, "/");
        String myURL = myStrTok.nextToken();
        String myServiceName = null;
        if (myStrTok.hasMoreTokens()) myServiceName = myStrTok.nextToken(); else myServiceName = "jPXI";
        myServiceURL = "rmi://" + myURL + "/" + myServiceName;
        myTiffDir = arg[1];
        File myTiffDirFile = new File(myTiffDir);
        JpxiDmsDbRec myDMS = null;
        JpxiTransactionAck myAck = null;
        String myGenDocId = "STOR_EFF";
        int myAmdt = 0;
        Timestamp myScanDate = Timestamp.valueOf("2003-01-29 00:00:00.000000000");
        String myXml = "<?xml version=\"1.0\" standalone=\"yes\"?>";
        String myOrigFmt = "TIFF";
        String myTgtFmt = "TIFF";
        String myCollection = "DEFAULT";
        java.sql.Timestamp myPubDate = Timestamp.valueOf("2003-01-29 00:00:00.000000000");
        char myCopyright = 'a';
        String myUser = null;
        String myPwd = null;
        String my1stDOcId = null;
        try {
            BufferedReader myIn = new BufferedReader(new InputStreamReader(System.in));
            System.out.print("User: ");
            myUser = myIn.readLine();
            System.out.print("Password: ");
            myPwd = myIn.readLine();
            System.out.print("First ID: ");
            my1stDOcId = myIn.readLine();
        } catch (IOException ioe) {
        }
        if (myUser == null | myPwd == null) System.exit(0);
        Random myRandGen = new Random();
        int id = 0;
        try {
            id = Integer.parseInt(my1stDOcId);
        } catch (NumberFormatException e) {
            System.exit(0);
        }
        String myExtDocId = null;
        int myNbPages = 0;
        File[] myListFile = myTiffDirFile.listFiles();
        int myNbTiff = myListFile.length;
        JpxiServiceInterface obj = null;
        UID mySessionUID = null;
        UID myTransactionUID = null;
        int bytes_to_read = 0;
        int bytes_to_write = 0;
        byte[] buffToWrite = null;
        byte[] metaData = new byte[35];
        try {
            obj = (JpxiServiceInterface) java.rmi.Naming.lookup(myServiceURL);
            mySessionUID = obj.openJpxiSession(myUser, myPwd);
        } catch (MalformedURLException e) {
            System.exit(0);
        } catch (RemoteException e) {
            System.exit(0);
        } catch (NotBoundException e) {
            System.exit(0);
        } catch (JpxiException e) {
            System.exit(0);
        }
        mainloop: while (true) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
            myExtDocId = myGenDocId + id;
            id++;
            myNbPages = myRandGen.nextInt(50) + 1;
            File[] mySelectedImage = new File[myNbPages];
            for (int i = 0; i < myNbPages; i++) {
                mySelectedImage[i] = myListFile[myRandGen.nextInt(myNbTiff)];
            }
            System.out.println("Storing new document : " + myExtDocId + " (" + myNbPages + " pages)");
            try {
                myDMS = new JpxiDmsDbRec(myExtDocId, myAmdt, myScanDate, myXml);
                myAck = obj.newDLTransaction(mySessionUID, myDMS, myOrigFmt, myTgtFmt, myCollection, myPubDate, myCopyright);
            } catch (Exception e) {
                e.printStackTrace();
                if (myAck != null) try {
                    obj.closeTrans(myAck.getTransactionUID());
                } catch (RemoteException e2) {
                } catch (JpxiException je) {
                }
                if (e.getMessage().indexOf("JPXI Error [513]") > -1) break mainloop; else continue mainloop;
            }
            myTransactionUID = myAck.getTransactionUID();
            for (int pageNum = 0; pageNum < myNbPages; pageNum++) {
                bytes_to_read = (int) mySelectedImage[pageNum].length();
                FileInputStream myFileInputStream;
                try {
                    myFileInputStream = new FileInputStream(mySelectedImage[pageNum]);
                } catch (FileNotFoundException e) {
                    continue mainloop;
                }
                BufferedInputStream myBufferedInputStream = new BufferedInputStream(myFileInputStream);
                if (myTgtFmt.equalsIgnoreCase("ST33")) {
                    for (int i = 0; i < 20; i++) metaData[i] = (byte) '0';
                    System.arraycopy(charArray2ByteArray(int2Char(myNbPages)), 0, metaData, 20, 4);
                    System.arraycopy(charArray2ByteArray(int2Char(pageNum + 1)), 0, metaData, 24, 4);
                    for (int i = 28; i < 35; i++) metaData[i] = (byte) '0';
                    bytes_to_write = bytes_to_read + 35;
                    buffToWrite = new byte[bytes_to_write];
                    System.arraycopy(metaData, 0, buffToWrite, 0, 35);
                    try {
                        myBufferedInputStream.read(buffToWrite, 35, bytes_to_read);
                    } catch (IOException e) {
                        continue mainloop;
                    } finally {
                        try {
                            myFileInputStream.close();
                        } catch (IOException e) {
                        }
                    }
                } else {
                    buffToWrite = new byte[bytes_to_read];
                    try {
                        myBufferedInputStream.read(buffToWrite, 0, bytes_to_read);
                    } catch (IOException e) {
                        continue mainloop;
                    } finally {
                        try {
                            myFileInputStream.close();
                        } catch (IOException e) {
                        }
                    }
                }
                try {
                    obj.write(myTransactionUID, buffToWrite);
                } catch (RemoteException e) {
                    try {
                        obj.rollbackTrans(myTransactionUID);
                    } catch (RemoteException e2) {
                    } catch (JpxiException je) {
                    }
                    continue mainloop;
                } catch (JpxiException e) {
                    try {
                        obj.rollbackTrans(myTransactionUID);
                    } catch (RemoteException e2) {
                    } catch (JpxiException je) {
                    }
                    continue mainloop;
                } finally {
                }
            }
            try {
                obj.closeTrans(myTransactionUID);
            } catch (RemoteException e) {
                continue mainloop;
            } catch (JpxiException e) {
                continue mainloop;
            }
        }
        try {
            obj.closeJpxiSession(mySessionUID);
        } catch (Exception e) {
            System.exit(0);
        }
    }
}
