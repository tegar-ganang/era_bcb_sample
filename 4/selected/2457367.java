package test;

import java.io.IOException;
import java.security.Security;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import javadata.data.Data;
import javadata.data.DataFactory;
import javadata.data.DataManager;
import javadata.encryption.KeyManager;
import javadata.io.IOUtils;
import javadata.io.IOXMLFile;
import javadata.util.Message;

/**
 * <p>
 * <b>Title: </b>Test program for {@link javadata.io.IOXMLFile}.
 * </p>
 *
 * <p>
 * <b>Description: </b>Test program for {@link javadata.io.IOXMLFile}.
 * </p>
 * 
 * <p><b>Version: </b>1.0</p>
 * 
 * <p>
 * <b>Author: </b> Matthew Pearson, Copyright 2006, 2007
 * </p>
 * 
 * <p>
 * <b>License: </b>This file is part of JavaData.
 * </p>
 * <p>
 * JavaData is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * </p>
 * <p>
 * JavaData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * </p>
 * <p>
 * You should have received a copy of the GNU Lesser General Public License
 * along with JavaData.  If not, see 
 * <a href="http://www.gnu.org/licenses/">GNU licenses</a>.
 * </p> 
 * 
 */
public class Test_DataManagerIOXMLFile {

    /**
	 * @param args
	 */
    public static void main(String[] args) {
        Security.addProvider(new BouncyCastleProvider());
        Message.debug("Starting test program for DataManager class...", Message.LEVEL.TWO);
        Data theData = DataFactory.createData("data1");
        theData.setRecord("d", "1");
        theData.setRecord("g", "2");
        theData.setRecord("c", "3");
        Message.debug("Adding a Data object to the DataManager...", Message.LEVEL.TWO);
        DataManager dm = new DataManager("dm1");
        dm.add("1", theData);
        Message.debug("Name of data manager: " + dm.getName(), Message.LEVEL.TWO);
        Data theData2 = DataFactory.createData("data2");
        theData2.setRecord("name", "Matthew Pearson");
        IOXMLFile dmFile = new IOXMLFile();
        try {
            System.out.print("Enter encryption password:  ");
            System.out.flush();
            dmFile.writeDataManager(dm, "C:\\hello1.xml", KeyManager.readPassword(System.in));
        } catch (IOException e) {
            e.printStackTrace();
        }
        Message.debug("Now try to read file....", Message.LEVEL.TWO);
        DataManager dm2 = null;
        try {
            System.out.print("Enter decryption password:  ");
            System.out.flush();
            dm2 = dmFile.readDataManager("C:\\hello1.xml", KeyManager.readPassword(System.in));
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (dm2 != null) {
            IOUtils.dump(dm2);
        }
        Message.debug("Finished.", Message.LEVEL.TWO);
    }
}
