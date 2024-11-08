package android.os.cts;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import android.content.Context;
import android.content.res.AssetManager;
import android.os.RecoverySystem;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.MediumTest;
import android.util.Log;

public class RecoverySystemTest extends AndroidTestCase {

    private static final String TAG = "RecoverySystemTest";

    private AssetManager mAssets;

    @Override
    protected void setUp() throws Exception {
        Log.v(TAG, "setup");
        super.setUp();
        mAssets = mContext.getAssets();
    }

    /** Write the given asset to a file of the same name and return a File. */
    private File getAsset(String name) throws Exception {
        FileOutputStream fos = mContext.openFileOutput(name, 0);
        InputStream is = mAssets.open(name);
        byte[] b = new byte[4096];
        int read;
        while ((read = is.read(b)) != -1) {
            fos.write(b, 0, read);
        }
        is.close();
        fos.close();
        return mContext.getFileStreamPath(name);
    }

    @MediumTest
    public void testVerify() throws Exception {
        File otacerts = getAsset("otacerts.zip");
        File packageFile;
        Log.v(TAG, "testing otasigned.zip");
        packageFile = getAsset("otasigned.zip");
        RecoverySystem.verifyPackage(packageFile, null, otacerts);
        packageFile.delete();
        expectVerifyFail("alter-footer.zip", otacerts);
        expectVerifyFail("alter-metadata.zip", otacerts);
        expectVerifyFail("fake-eocd.zip", otacerts);
        expectVerifyFail("jarsigned.zip", otacerts);
        expectVerifyFail("random.zip", otacerts);
        expectVerifyFail("unsigned.zip", otacerts);
        otacerts.delete();
    }

    /**
     * Try verifying the given file against the given otacerts,
     * expecting verification to fail.
     */
    private void expectVerifyFail(String name, File otacerts) throws Exception {
        Log.v(TAG, "testing " + name);
        File packageFile = getAsset(name);
        try {
            RecoverySystem.verifyPackage(packageFile, null, otacerts);
            fail("verification of " + name + " succeeded when it shouldn't have");
        } catch (GeneralSecurityException e) {
        }
        packageFile.delete();
    }
}
