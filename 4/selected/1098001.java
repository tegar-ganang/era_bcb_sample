package eyes.blue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

public class FileSysManager {

    static int targetFileIndex = -1;

    static String logTag = null;

    static int NO_CACHE = 0;

    static final int EXTERNAL_MEMORY = 1;

    static final int INTERNAL_MEMORY = 2;

    static final int MEDIA_FILE = 0;

    static final int SUBTITLE_FILE = 1;

    static final int THEORY_FILE = 2;

    public static final long[] mp3FileCRC32 = { 3930594200l, 1443240686l, 1329943845l, 467912282l, 2146280557l, 599779824l, 382788444l, 2951178734l, 1032306740l, 1586192556l, 1936909l, 2372119436l, 1683933831l, 1841047200l, 1433830288l, 1562531171l, 3557800346l, 209130593l, 283271084l, 1732306146l, 1453525748l, 411010063l, 875116622l, 788786197l, 2169543162l, 1221266016l, 743196761l, 3869160580l, 4256204074l, 473430835l, 3538901105l, 2862755139l, 118463217l, 3508599741l, 4217112253l, 3832179651l, 4128557960l, 3838975110l, 4093473722l, 741847341l, 3512197479l, 1401256640l, 2771294600l, 3153427137l, 3180686897l, 1567738342l, 2281161133l, 3851533376l, 3556497198l, 751037598l, 3475456312l, 1298806513l, 3103322329l, 4087499476l, 22404615l, 2821936302l, 1649847124l, 2625121102l, 824230974l, 713710592l, 3705204155l, 4051933476l, 2936988589l, 4166561745l, 348732671l, 3925594779l, 4111096685l, 530669864l, 2768291539l, 1726307911l, 1261092614l, 1018443800l, 2713848952l, 1467930965l, 2515459784l, 859077812l, 1601509743l, 2020691357l, 672167593l, 3513293040l, 4061257249l, 13423710l, 4120478332l, 2155370672l, 3615663589l, 411776358l, 2685781589l, 2013856102l, 1871972595l, 3427230271l, 1308798892l, 1535264541l, 1549660605l, 2905867700l, 515678669l, 260394303l, 741743665l, 2899035331l, 98094457l, 3043576613l, 2522413194l, 815662150l, 1732888722l, 818346842l, 651863362l, 2216435406l, 3713959451l, 2061999244l, 2489036756l, 3679801913l, 1149822301l, 247163965l, 646453220l, 174698987l, 2551691481l, 2996507709l, 711469511l, 4071466816l, 417802954l, 473528038l, 1543468414l, 2586981813l, 3584063611l, 910907613l, 3110219779l, 1107790300l, 1908758396l, 1869460032l, 453856574l, 1632973980l, 383775741l, 1181042248l, 3144555926l, 1300427001l, 3640665270l, 3321296016l, 1802450904l, 2566902041l, 1971180698l, 641484064l, 2096528480l, 2440493607l, 1805589026l, 426479323l, 1408937355l, 3508916897l, 2708401883l, 4219197965l, 2189920456l, 1807945691l, 958339036l, 796998927l, 3448289450l, 707515447l, 1992354549l, 1656554919l, 2505350402l, 3701166463l, 1530517062l, 1118524672l, 1780502256l, 3724568330l, 2707248344l, 4200817881l, 2671572974l, 2445679698l, 2843439343l, 4231640977l, 4207772350l, 1753831802l, 4230407172l, 3622206121l, 2195347465l, 1263119786l, 1600395794l, 155504299l, 3910310524l, 190723373l, 613068740l, 1849266978l, 2474165888l, 3595780126l, 1338005602l, 2000573956l, 2237660908l, 91555546l, 2569766669l, 2163433245l, 429388306l, 1705644257l, 3628488381l, 3522708218l, 3312807111l, 2017861132l, 2840056514l, 3294064500l, 3271080339l, 900967736l, 3149241479l, 1185770239l, 3568370529l, 1774700913l, 2518472420l, 784403896l, 545623990l, 907778426l, 3246373136l, 2661771025l, 1307131157l, 931976214l, 1674592616l, 2244663557l, 2825469336l, 3520520055l, 3797403120l, 3487225732l, 2678801086l, 923853763l, 2364385799l, 2044274168l, 412611812l, 2209203805l, 3244324875l, 1493819382l, 1010481020l, 3694881076l, 2751904554l, 3417703138l, 2346185185l, 679434278l, 2245057462l, 1495594739l, 631680398l, 2375975288l, 1352112789l, 2938938591l, 2053635136l, 1785283766l, 12436648l, 1584594692l, 3079046301l, 1615253572l, 218466716l, 1820993217l, 2095037773l, 2767384165l, 2271271797l, 1883127263l, 2152617470l, 1229355022l, 2316601922l, 1537555962l, 3999582880l, 1905702081l, 2330968834l, 3674021572l, 521666911l, 2702299932l, 342696415l, 750755347l, 2257744372l, 3720884681l, 3791163628l, 472147320l, 13947527l, 4179755343l, 3238358799l, 702795126l, 1834190316l, 3268101443l, 1860264656l, 2091546412l, 2850568991l, 2381657473l, 219692277l, 1974553991l, 79011347l, 4141722067l, 26087033l, 341021882l, 2375343861l, 2642597622l, 2061586334l, 3710090442l, 49967060l, 818213711l, 1716600896l, 2546277888l, 598604442l, 3735816745l, 473951027l, 1059130020l, 2692139059l, 3140504517l, 3590020525l, 1103588034l, 1530557453l, 1514897637l, 1807549499l, 2757303974l, 3487450109l, 1449973205l, 932873999l, 3398398395l, 3317236411l, 4070813761l, 1588795092l, 1846278982l, 328191358l, 1396410921l, 737409055l, 411489884l, 89226904l, 2575485682l, 907311500l, 224945231l, 1291376273l, 1019971282l, 3744726240l, 3709797972l };

    static SharedPreferences options = null;

    static StatFs[] statFs = null;

    static Context context = null;

    static ArrayList<RemoteSource> remoteResources = new ArrayList<RemoteSource>();

    static DiskSpaceFullListener diskFullListener = null;

    DownloadListener downloadListener = null;

    static int downloadFromSite = -1;

    public FileSysManager(Context context) {
        FileSysManager.logTag = context.getString(R.string.app_name);
        FileSysManager.context = context;
        options = context.getSharedPreferences(context.getString(R.string.optionFile), 0);
    }

    public void setDownloadListener(DownloadListener listener) {
        this.downloadListener = listener;
    }

    public static boolean downloadSubtitleFromGoogle(int index) {
        System.out.println("Download subtitle " + index);
        RemoteSource rs = remoteResources.get(0);
        String sitePath = rs.getSubtitleFileAddress(index);
        int respCode = -1;
        HttpClient httpclient = new DefaultHttpClient();
        HttpGet httpget = new HttpGet(sitePath);
        HttpResponse response = null;
        try {
            response = httpclient.execute(httpget);
            respCode = response.getStatusLine().getStatusCode();
            if (respCode != 200) {
                System.out.println("CheckRemoteThread: check " + sitePath + " return " + respCode);
                return false;
            }
            InputStream is = response.getEntity().getContent();
            FileOutputStream fos = new FileOutputStream(FileSysManager.getLocalSubtitleFile(index));
            byte[] buf = new byte[16384];
            int readLen = -1;
            int counter = 0;
            Log.d(logTag, "Start read stream from remote site, is=" + ((is == null) ? "NULL" : "exist") + ", buf=" + ((buf == null) ? "NULL" : "exist"));
            while ((readLen = is.read(buf)) != -1) {
                counter += readLen;
                fos.write(buf, 0, readLen);
            }
            is.close();
            fos.close();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean checkFileStructure() {
        boolean extWritable = (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()));
        File appRoot = null;
        if (extWritable) appRoot = context.getExternalFilesDir(context.getString(R.string.app_name)); else appRoot = context.getFileStreamPath(context.getString(R.string.app_name));
        if (appRoot.isFile()) {
            if (!appRoot.delete()) return false;
            if (!appRoot.mkdirs()) return false;
        }
        String[] dirs = { context.getString(R.string.audioDirName), context.getString(R.string.subtitleDirName), context.getString(R.string.theoryDirName) };
        for (String s : dirs) {
            File subDir = new File(appRoot + File.separator + s);
            if (subDir.isFile()) if (!subDir.delete()) return false;
            if (!subDir.exists()) if (!subDir.mkdirs()) return false;
        }
        return true;
    }

    public static File getLocalMediaFile(int i) {
        if (isExtMemWritable()) return new File(context.getExternalFilesDir(File.separator + context.getString(R.string.app_name)).getAbsoluteFile() + File.separator + context.getString(R.string.audioDirName) + File.separator + context.getResources().getStringArray(R.array.fileName)[i] + "." + context.getString(R.string.defMediaType)); else return new File(context.getFileStreamPath(context.getString(R.string.app_name)).getAbsoluteFile() + File.separator + context.getString(R.string.audioDirName) + File.separator + context.getResources().getStringArray(R.array.fileName)[i] + "." + context.getString(R.string.defMediaType));
    }

    public static File getLocalSubtitleFile(int i) {
        if (isExtMemWritable()) return new File(context.getExternalFilesDir(File.separator + context.getString(R.string.app_name)).getAbsoluteFile() + File.separator + context.getString(R.string.subtitleDirName) + File.separator + context.getResources().getStringArray(R.array.fileName)[i] + "." + context.getString(R.string.defSubtitleType)); else return new File(context.getFileStreamPath(context.getString(R.string.app_name)).getAbsoluteFile() + File.separator + context.getString(R.string.subtitleDirName) + File.separator + context.getResources().getStringArray(R.array.fileName)[i] + "." + context.getString(R.string.defSubtitleType));
    }

    public static File getLocalTheoryFile(int i) {
        if (isExtMemWritable()) return new File(context.getExternalFilesDir(File.separator + context.getString(R.string.app_name)).getAbsoluteFile() + File.separator + context.getString(R.string.theoryDirName) + File.separator + context.getResources().getStringArray(R.array.fileName)[i] + "." + context.getString(R.string.defTheoryType)); else return new File(context.getFileStreamPath(context.getString(R.string.app_name)).getAbsoluteFile() + File.separator + context.getString(R.string.theoryDirName) + File.separator + context.getResources().getStringArray(R.array.fileName)[i] + "." + context.getString(R.string.defTheoryType));
    }

    public static boolean isFileValid(int i, int resType) {
        Log.d(logTag, Thread.currentThread().getName() + ":Check the existed file");
        File file = null;
        if (resType == context.getResources().getInteger(R.integer.MEDIA_TYPE)) {
            file = getLocalMediaFile(i);
            if (!file.exists()) return false;
            int size = context.getResources().getIntArray(R.array.mediaFileSize)[i];
            if (file.length() != size) {
                Log.d(logTag, "The size of file is not correct, should be " + size + ", but " + file.length());
                return false;
            }
            try {
                return Util.isFileCorrect(file, mp3FileCRC32[i]);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        } else if (resType == context.getResources().getInteger(R.integer.SUBTITLE_TYPE)) {
            file = getLocalSubtitleFile(i);
            if (file.exists()) return true;
            Log.d(logTag, "The subtitle file " + file.getAbsolutePath() + " is not exist");
            return false;
        } else if (resType == context.getResources().getInteger(R.integer.THEORY_TYPE)) {
            file = getLocalTheoryFile(i);
            if (file.exists()) return true;
            Log.d(logTag, "The theory file " + file.getAbsolutePath() + " is not exist");
            return false;
        }
        Log.e(logTag, "FileSysManager.isFileValid(): Logical error: the resource type shouldn't " + resType);
        return false;
    }

    public int getRecommandStoreLocate() {
        int extFree = FreeMemory(EXTERNAL_MEMORY);
        int intFree = FreeMemory(INTERNAL_MEMORY);
        if (extFree > 2000) return EXTERNAL_MEMORY;
        if (intFree > 2000) return INTERNAL_MEMORY;
        return NO_CACHE;
    }

    public static boolean isExtMemWritable() {
        return (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()));
    }

    public void setDiskSpaceFullListener(DiskSpaceFullListener dsfl) {
        this.diskFullListener = dsfl;
    }

    public int TotalMemory(int locate) {
        return (statFs[locate].getBlockCount() * statFs[locate].getBlockSize()) >>> 20;
    }

    public int FreeMemory(int locate) {
        return (statFs[locate].getAvailableBlocks() * statFs[locate].getBlockSize()) >>> 20;
    }

    class DiskSpaceFullListener {

        public void diskSpaceFull() {
        }
    }
}
