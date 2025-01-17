package org.ccnx.ccn.test.repo;

import java.io.IOException;
import java.util.ArrayList;
import org.ccnx.ccn.impl.repo.RepositoryException;
import org.ccnx.ccn.impl.support.DataUtils;
import org.ccnx.ccn.impl.support.Log;
import org.ccnx.ccn.io.CCNReader;
import org.ccnx.ccn.protocol.ContentName;
import org.ccnx.ccn.protocol.ContentObject;
import org.ccnx.ccn.protocol.Exclude;
import org.ccnx.ccn.protocol.Interest;
import org.ccnx.ccn.protocol.PublisherID;
import org.ccnx.ccn.protocol.PublisherPublicKeyDigest;
import org.junit.Assert;
import org.junit.Test;

/**
 * Part of repository test infrastructure.  This test must be run after RFSTest and can be
 * run only once correctly after running RFSTest.
 * 
 * This test tests reading of unusual objects which can't be written to a running repo
 * via the current repo writing algorithms. The data needed for this test can only be created
 * by something like RFSTest which creates and saves a repo file structure without a running
 * repo by calling saveContent methods directly with different kinds of data.  
 * 
 * It is normally desirable that tests such as this can be run repeatedly and achieve the same 
 * results. But since we can't write the data to be tested via the repo, and once we have read the 
 * data once, it may be cached by ccnd, a correct running of the test after the first time can't be 
 * guaranteed and is in fact unlikely with this test. Possibly this test should just be eliminated but 
 * since it is part of the historic repo test suite I am leaving it in for the time being.
 */
public class RepoInitialReadTest extends RepoTestBase {

    public static final long TIMEOUT = 5000;

    @Test
    public void testReadViaRepo() throws Throwable {
        System.out.println("Testing reading objects from repo");
        ContentName name = ContentName.fromNative("/repoTest/data1");
        ContentName name1 = findFullName(name, "Here's my data!");
        ContentName digestName = findFullName(name, "Testing2");
        String tooLongName = "0123456789";
        for (int i = 0; i < 30; i++) tooLongName += "0123456789";
        ContentName longName = findFullName(ContentName.fromNative("/repoTest/" + tooLongName), "Long name!");
        ContentName badCharName = ContentName.fromNative("/repoTest/" + "*x?y<z>u");
        ContentName badCharLongName = ContentName.fromNative("/repoTest/" + tooLongName + "*x?y<z>u");
        checkDataWithDigest(name1, "Here's my data!");
        checkDataWithDigest(digestName, "Testing2");
        checkDataWithDigest(longName, "Long name!");
        checkData(badCharName, "Funny characters!");
        checkData(badCharLongName, "Long and funny");
        CCNReader reader = new CCNReader(getHandle);
        ArrayList<ContentObject> keys = reader.enumerate(new Interest(keyprefix), 4000);
        for (ContentObject keyObject : keys) {
            checkDataAndPublisher(name, "Testing2", new PublisherPublicKeyDigest(keyObject.content()));
        }
    }

    /**
	 * Find a content object by name (without digest) and content, and return the full name (with digest)
	 * @param name ContentName without digest component
	 * @param str The content
	 * @return ContentName with implicit digest
	 * @throws IOException 
	 */
    private ContentName findFullName(ContentName name, String str) throws IOException {
        byte[] content = str.getBytes();
        Exclude e = null;
        for (; ; ) {
            Interest i = Interest.constructInterest(name, e, Interest.CHILD_SELECTOR_LEFT, null, null, null);
            Log.info("searching for {0} content {1}, exclude {2}", name, str, e);
            ContentObject co = getHandle.get(i, TIMEOUT);
            Assert.assertTrue(null != co);
            Log.info("got result {0} digest={1}", co, DataUtils.printHexBytes(co.digest()));
            if (DataUtils.arrayEquals(co.content(), content)) return co.fullName();
            byte[][] omissions = { co.digest() };
            if (e == null) e = new Exclude(omissions); else e.add(omissions);
        }
    }

    private void checkDataWithDigest(ContentName name, String data) throws RepositoryException, IOException, InterruptedException {
        Interest interest = new Interest(name);
        interest.maxSuffixComponents(0);
        checkData(interest, data.getBytes());
    }

    private void checkDataAndPublisher(ContentName name, String data, PublisherPublicKeyDigest publisher) throws IOException, InterruptedException {
        Interest interest = new Interest(name, new PublisherID(publisher));
        ContentObject testContent = getHandle.get(interest, 10000);
        Assert.assertFalse(testContent == null);
        Assert.assertEquals(data, new String(testContent.content()));
        Assert.assertTrue(testContent.signedInfo().getPublisherKeyID().equals(publisher));
    }
}
