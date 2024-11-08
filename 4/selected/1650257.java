package net.sf.autoshare;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.SecretKeySpec;
import net.sf.autoshare.data.DataConstants;
import net.sf.autoshare.data.util.DataUtil;
import net.sf.autoshare.data.util.SecurityUtil;
import net.sf.autoshare.data.filelist.FileListNotExistsException;
import net.sf.autoshare.data.filelist.LocalFileListStore;
import net.sf.autoshare.user.priv.GroupManager;
import net.sf.autoshare.user.priv.LocalAccount;
import net.sf.autoshare.user.priv.LocalParticipation;
import net.sf.autoshare.user.pub.Account;
import net.sf.autoshare.user.pub.Group;
import net.sf.autoshare.user.pub.Participant;
import org.apache.commons.codec.binary.Base64;
import org.junit.Before;

public class BaseTest {

    private LocalAccount localAccount = null;

    private LocalFileListStore localFileListStore = null;

    private LocalAccount groupLeader = null;

    private GroupManager groupManager = null;

    private SecretKeySpec defaultSecretKey = null;

    @Before
    public synchronized void init() {
        File testBaseDir = getTestBaseDir();
        if (!testBaseDir.exists()) {
            testBaseDir.mkdir();
        }
    }

    public synchronized SecretKeySpec getDefaultSecretKey() {
        if (defaultSecretKey == null) {
            defaultSecretKey = SecurityUtil.generateBlockCipherKey();
        }
        return defaultSecretKey;
    }

    protected KeyPair generateKeyPair() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(DataConstants.ASYNC_CIPHER_KEYGEN_TYPE);
            SecureRandom random = SecureRandom.getInstance(DataConstants.SECURE_RANDOM_TYPE, DataConstants.SECURE_RANDOM_PROVIDER);
            keyGen.initialize(DataConstants.KEY_LENGTH, random);
            return keyGen.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected synchronized Participant getLocalParticipant() {
        Account account = getLocalAccount().getAccount();
        return createTestParticipant(account);
    }

    protected synchronized Participant createTestParticipant(Account account) {
        LocalAccount groupLeader = getGroupLeader();
        Group group = new Group("testGroup", account);
        byte[] groupLeaderSignedPublicKey;
        try {
            Cipher cipher = Cipher.getInstance(DataConstants.ASYNC_CIPHER);
            cipher.init(Cipher.ENCRYPT_MODE, groupLeader.getPrivateKey());
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            CipherOutputStream cos = new CipherOutputStream(bos, cipher);
            try {
                cos.write(account.getPublicKey().getEncoded());
            } finally {
                cos.close();
            }
            groupLeaderSignedPublicKey = bos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Participant participant = new Participant(account, account.getUserName() + "_in_" + group.getName(), group, groupLeaderSignedPublicKey);
        group.add(participant);
        return participant;
    }

    protected synchronized LocalAccount getLocalAccount() {
        if (localAccount != null) {
            return localAccount;
        }
        localAccount = createNewAccount("testUser");
        return localAccount;
    }

    protected synchronized void setGroupLeaderToLocalAccount() {
        groupLeader = localAccount;
    }

    protected synchronized LocalAccount getGroupLeader() {
        if (groupLeader != null) {
            return groupLeader;
        }
        groupLeader = createNewAccount("groupLeader");
        return groupLeader;
    }

    protected synchronized GroupManager getLocalGroupManager() {
        if (groupManager == null) {
            groupManager = new GroupManager();
        }
        return groupManager;
    }

    protected LocalAccount createNewAccount(String name) {
        KeyPair keyPair = generateKeyPair();
        Account account = new Account(keyPair.getPublic(), name);
        return new LocalAccount(account, keyPair.getPrivate());
    }

    protected synchronized LocalParticipation createTestParticipation(File sharedDir) throws FileListNotExistsException, IOException {
        return createTestParticipation(sharedDir, getLocalAccount().getAccount());
    }

    protected synchronized LocalParticipation createTestParticipation(File sharedDir, Account account) throws FileListNotExistsException, IOException {
        Participant participant = createTestParticipant(account);
        return new LocalParticipation(participant, sharedDir, localFileListStore.getFileList(participant), true);
    }

    protected synchronized LocalFileListStore initLocalFileListStore(File fileListDir) {
        localFileListStore = new LocalFileListStore(fileListDir);
        return localFileListStore;
    }

    protected synchronized LocalFileListStore getLocalFileListStore() {
        return localFileListStore;
    }

    protected File getTestBaseDir() {
        return DataUtil.convertRelativeFile(new File("test"));
    }

    protected File getTestDataBaseDir() {
        return DataUtil.convertRelativeFile(new File("testdata"));
    }

    private void deleteRecursively(File dir) throws IOException {
        for (File file : dir.listFiles()) {
            if (!file.getCanonicalPath().contains("generatedTestdata")) {
                throw new RuntimeException("Cannot delete: " + file.getCanonicalPath());
            }
            if (file.isDirectory()) {
                deleteRecursively(file);
            }
            System.out.println("Deleting " + file.getCanonicalPath());
            file.delete();
        }
    }

    protected synchronized File getGeneratedTestDataBaseDir() throws IOException {
        File dir = DataUtil.convertRelativeFile(new File("generatedTestdata"));
        if (!dir.exists()) {
            dir.mkdir();
        } else {
            deleteRecursively(dir);
        }
        return dir;
    }

    protected void copyFile(File from, File to) throws IOException {
        to.getParentFile().mkdirs();
        InputStream in = new FileInputStream(from);
        try {
            OutputStream out = new FileOutputStream(to);
            try {
                byte[] buf = new byte[1024];
                int readLength;
                while ((readLength = in.read(buf)) > 0) {
                    out.write(buf, 0, readLength);
                }
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }

    protected void printKey(Key key) {
        try {
            System.out.println(new String(Base64.encodeBase64(key.getEncoded()), "utf-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
