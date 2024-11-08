package neembuu.vfs.readmanager.test;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import jpfm.volume.AbstractFile;
import neembuu.vfs.readmanager.Connection;
import neembuu.vfs.readmanager.DownloadDataStoragePathNegotiator;
import neembuu.vfs.readmanager.DownloadSpeedListener;
import neembuu.vfs.readmanager.NewConnectionParams;
import neembuu.vfs.readmanager.NewConnectionProvider;
import neembuu.vfs.readmanager.ReadRequestState;
import neembuu.vfs.readmanager.ThrottleState;
import neembuu.vfs.readmanager.TransientConnectionListener;
import neembuu.vfs.readmanager.sampleImpl.DownloadManager;
import neembuu.vfs.test.test.BoundaryConditions;
import org.junit.BeforeClass;
import org.junit.Test;
import static junit.framework.Assert.*;

/**
 *
 * @author Shashank Tulsyan
 */
public class NewConProvTest implements TransientConnectionListener {

    private static final String ORIGINAL_FILE = "j:\\neembuu\\realfiles\\test120k.rmvb";

    private static final String DOWNLOAD_URL = "http://localhost:8080/LocalFileServer-war/servlet/FileServer?totalFileSpeedLimit=500&file=test120k.rmvb";

    private static final String TEMP_SAVE_LOCATION = "j:\\neembuu\\heap\\test120k.http.rmvb_neembuu_download_data\\test.rmvb";

    private static FileChannel original_file = null;

    private static NewConnectionProvider newConnectionProvide = new DownloadManager(DOWNLOAD_URL);

    private final Object testOverLock = new Object();

    private Connection connection = null;

    @BeforeClass
    public static void initialize() throws Exception {
        new java.io.File(TEMP_SAVE_LOCATION).delete();
        original_file = new FileInputStream(ORIGINAL_FILE).getChannel();
    }

    public void simpleNewConnection() throws Exception {
        newConnectionProvide.provideNewConnection(new NewConnectionParams.Builder().setDownloadDataChannel(new TestDownloadChannel(TEMP_SAVE_LOCATION)).setDownloadDataStoragePathNegotiator(new DownloadDataStoragePathNegotiator() {

            @Override
            public AbstractFile provide(String store, boolean shouldNeembuuManage) {
                return null;
            }
        }).setMinimumSizeRequired(0).setOffset(0).setReadRequestState(new ReadRequestState() {

            @Override
            public long starting() {
                return 0;
            }

            @Override
            public long ending() {
                return 0;
            }

            @Override
            public long authorityLimit() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public long getRequestSpeed() {
                return 0;
            }

            @Override
            public long lastRequestTime() {
                return System.currentTimeMillis();
            }

            @Override
            public boolean pendingRequestsPresentOutside() {
                return false;
            }

            @Override
            public long requestDownloadGap() {
                return 0;
            }

            @Override
            public boolean requestsPresentAlongSomeConnection() {
                return true;
            }

            @Override
            public boolean requestsPresentAlongThisConnection() {
                return true;
            }
        }).setTransientConnectionListener(this).build());
        synchronized (testOverLock) {
            testOverLock.wait(20000);
        }
        assertNotNull(connection);
    }

    @Test
    public void connectionAndDataCheckAndThrottleCheck() throws Exception {
        final long offset = (long) (Math.random() * original_file.size());
        System.out.println("Offset=" + offset);
        final long pseudo_request_speed = 20 * 1024;
        TestDownloadChannel downloadChannel = new TestDownloadChannel(TEMP_SAVE_LOCATION);
        newConnectionProvide.provideNewConnection(new NewConnectionParams.Builder().setDownloadDataChannel(downloadChannel).setDownloadDataStoragePathNegotiator(new DownloadDataStoragePathNegotiator() {

            @Override
            public AbstractFile provide(String store, boolean shouldNeembuuManage) {
                return null;
            }
        }).setMinimumSizeRequired((int) (original_file.size() - offset)).setOffset(offset).setReadRequestState(new ReadRequestState() {

            @Override
            public long starting() {
                return offset;
            }

            @Override
            public long ending() {
                try {
                    return original_file.size() - 1;
                } catch (Exception any) {
                    any.printStackTrace();
                    return -1;
                }
            }

            @Override
            public long authorityLimit() {
                return ending();
            }

            @Override
            public long getRequestSpeed() {
                return pseudo_request_speed;
            }

            @Override
            public long lastRequestTime() {
                return System.currentTimeMillis();
            }

            @Override
            public boolean pendingRequestsPresentOutside() {
                return false;
            }

            @Override
            public long requestDownloadGap() {
                return 1024 * 1024;
            }

            @Override
            public boolean requestsPresentAlongSomeConnection() {
                return false;
            }

            @Override
            public boolean requestsPresentAlongThisConnection() {
                return false;
            }
        }).setTransientConnectionListener(this).build());
        synchronized (testOverLock) {
            testOverLock.wait(10000);
        }
        assertNotNull(connection);
        connection.addDownloadSpeedListener(new DownloadSpeedListener() {

            @Override
            public final void downloadSpeedUpdated(double newDownloadSpeed) {
                System.out.println("download Speed =" + newDownloadSpeed);
            }

            @Override
            public final void throttleStateChanged(ThrottleState stateListener) {
                System.out.println("throttle state =" + stateListener);
            }
        });
        long downloadTime = 1000 * 20;
        Thread.sleep(downloadTime);
        long amountOfContentThatShouldHaveBeenDownloaded_if_downloadspeed_is_equal_to_requestSpeed = pseudo_request_speed * downloadTime;
        long amountOfContentActuallyDownloaded = downloadChannel.amountDownloaded();
        ByteBuffer actualData = ByteBuffer.allocate((int) (amountOfContentActuallyDownloaded / 100));
        original_file.position(offset);
        original_file.read(actualData);
        ByteBuffer downloadedData = ByteBuffer.allocate((int) (amountOfContentActuallyDownloaded / 100));
        downloadChannel.readDestFile(0, downloadedData);
        BoundaryConditions.printContentPeek(actualData, downloadedData);
        BoundaryConditions.checkBuffers(actualData, downloadedData);
        double actualDownloadSpeed = (amountOfContentActuallyDownloaded / (downloadTime * 1.024));
        boolean throttletestpassed = actualDownloadSpeed > 0.8 * pseudo_request_speed && actualDownloadSpeed < 1.2 * pseudo_request_speed;
        System.out.println("Actual download speed(KBps)=" + actualDownloadSpeed);
        System.out.println("Psuedo request speed(KBps)=" + (pseudo_request_speed / 1024));
        assertTrue(throttletestpassed);
    }

    @Override
    public void describeState(String state) {
        System.out.println("Connection describing state=" + state);
    }

    @Override
    public void reportNumberOfRetriesMadeSoFar(int numberOfretries) {
    }

    @Override
    public void successful(Connection c) {
        connection = c;
        synchronized (testOverLock) {
            testOverLock.notifyAll();
        }
    }

    @Override
    public void failed(Throwable reason) {
        reason.printStackTrace();
        connection = null;
        synchronized (testOverLock) {
            testOverLock.notifyAll();
        }
    }
}
