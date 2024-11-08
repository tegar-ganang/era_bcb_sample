import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.StringTokenizer;

public class Game {

    UPBA upba;

    int npframes;

    PFrame pframes[];

    PFrame memPFrame;

    PFrame histPFrames[];

    int nhistpframes;

    String playString;

    int maxPFrames = 50;

    int maxHistPFrames = 20;

    int numObjects = 23;

    public Game(UPBA u, String name) {
        upba = u;
        npframes = 0;
        pframes = new PFrame[maxPFrames];
        histPFrames = new PFrame[maxHistPFrames];
        nhistpframes = 0;
        this.loadFile(name);
        this.parsePlayString();
    }

    void loadFile(String aName) {
        playString = "";
        URL theURL;
        BufferedReader in;
        boolean weregood = false;
        boolean malformed = false;
        try {
            theURL = new URL(aName);
            URLConnection conn = null;
            BufferedReader data = null;
            String line;
            StringBuffer buf = new StringBuffer();
            try {
                conn = theURL.openConnection();
                conn.connect();
                data = new BufferedReader(new InputStreamReader(new BufferedInputStream(conn.getInputStream())));
                while ((line = data.readLine()) != null) {
                    buf.append(line);
                }
                playString = buf.toString();
                data.close();
                weregood = true;
            } catch (IOException e) {
                System.out.println("IO Error:" + e.getMessage());
            }
        } catch (MalformedURLException e) {
            System.out.println("Bad URL: " + aName);
            malformed = true;
        }
        if (malformed) {
            System.out.println("trying file: " + aName);
            try {
                File file = new File(aName);
                if (file != null && file.exists()) {
                    long inlen = (file.length());
                    in = new BufferedReader(new FileReader((File) file));
                    if (in != null) {
                        try {
                            int linecount = 0;
                            while ((playString.length() + linecount) < inlen) {
                                playString = playString.concat(in.readLine());
                                linecount++;
                            }
                            weregood = true;
                        } catch (IOException io) {
                            weregood = false;
                        }
                    }
                }
            } catch (SecurityException ex) {
                System.out.println(ex.toString());
            } catch (Exception ex) {
                System.out.println(ex.toString());
            }
        }
        if (weregood) {
        } else {
            playString = "[[BeginPlaybookFile 2.0 1~3000:260,100`3001:260,120`3002:260,140`3003:260,160`3004:260,180`3005:260,200`3006:260,220`3007:260,240`1000:260,260`1001:260,280`1002:260,300`1003:260,320`1004:260,340`1005:260,360`1006:260,380`2000:260,400`2001:260,420`2002:260,440`2003:260,460`2004:260,480`2005:260,500`2006:260,520`4000:260,540`~100~| EndPlaybookFile]]";
            System.out.println("couldn't load url or file; using fallback example instead");
        }
    }

    void parsePlayString() {
        boolean weregood = false;
        String header = playString.substring(0, 23);
        if (header.equals("[[BeginPlaybookFile 2.0")) {
            weregood = true;
        } else if (header.equals("[[BeginPlaybookFile 1.0")) {
            System.out.println("UltiamtePlayBook 1.0 files not supported anymore!");
        } else {
            System.out.println("couldn't parse file; Not a UltiamtePlayBook 2.0 file!");
        }
        if (weregood) {
            String pframesString = playString.substring(23, playString.lastIndexOf('|'));
            for (StringTokenizer t = new StringTokenizer(pframesString, "|"); t.hasMoreTokens(); ) {
                String pframeraw = t.nextToken();
                PFrame apframe = new PFrame(new Position[numObjects], 0, 0, "");
                int lag = Integer.valueOf(pframeraw.substring(pframeraw.indexOf('~', 10) + 1, pframeraw.lastIndexOf('~'))).intValue();
                apframe.setLag(lag);
                String fcomment = pframeraw.substring(pframeraw.lastIndexOf('~') + 1, pframeraw.length());
                apframe.setComment(fcomment);
                int i = pframeraw.indexOf('~') + 1;
                int endi = pframeraw.indexOf('~', 10);
                String pframeString = pframeraw.substring(i, endi);
                for (StringTokenizer tk = new StringTokenizer(pframeString, "`"); tk.hasMoreTokens(); ) {
                    String player = tk.nextToken();
                    Position pos = new Position(0, 0);
                    int si = player.indexOf(':');
                    int ci = player.indexOf(',');
                    int ei = player.length();
                    pos.x = Integer.valueOf(player.substring(si + 1, ci)).intValue();
                    pos.y = Integer.valueOf(player.substring(ci + 1, ei)).intValue();
                    apframe.addPosition(pos);
                }
                this.addPFrame(apframe);
            }
        } else {
            System.out.println("Try another file!");
            this.clearFrames();
            this.initGame();
        }
    }

    void composePlayString() {
        PFrame apframe;
        int posx;
        int posy;
        int[] objectNames = { 3000, 3001, 3002, 3003, 3004, 3005, 3006, 3007, 1000, 1001, 1002, 1003, 1004, 1005, 1006, 2000, 2001, 2002, 2003, 2004, 2005, 2006, 4000 };
        playString = "[[BeginPlaybookFile 2.0 ";
        for (int j = 0; j < npframes; j++) {
            apframe = pframes[j];
            playString = playString + (j + 1) + "~";
            for (int i = 0; i < numObjects; i++) {
                posx = new Double(apframe.positions[i].x).intValue();
                posy = new Double(apframe.positions[i].y).intValue();
                playString = playString + objectNames[i] + ":" + posx + "," + posy + "`";
            }
            String stripcomment = apframe.getComment();
            stripcomment = stripcomment.replace("~", "-");
            stripcomment = stripcomment.replace("`", "'");
            stripcomment = stripcomment.replace("|", ".");
            stripcomment = stripcomment.replace("\r", " ");
            stripcomment = stripcomment.replace("\n", " ");
            playString = playString + "~" + apframe.lag + "~" + stripcomment + "|";
        }
        playString = playString + " EndPlaybookFile]]";
    }

    void clearFrames() {
        pframes = new PFrame[maxPFrames];
        npframes = 0;
        this.resethist();
    }

    void initGame() {
        pframes = new PFrame[maxPFrames];
        npframes = 0;
        PFrame apframe = new PFrame(new Position[numObjects], 0, 0, "");
        for (int i = 0; i < numObjects; i++) {
            Position pos = new Position(260, 100 + (i * 20));
            apframe.addPosition(pos);
            apframe.setLag(100);
            apframe.setComment("");
        }
        this.addPFrame(apframe);
    }

    int addPFrame(PFrame pframe) {
        pframes[npframes] = pframe;
        return npframes++;
    }

    void updatePFrame() {
        addhist();
        for (int i = 0; i < numObjects; i++) {
            Player n = upba.players[i];
            pframes[upba.currentFrameIndex].positions[i].y = n.x;
            pframes[upba.currentFrameIndex].positions[i].x = n.y;
        }
    }

    void resethist() {
        nhistpframes = 0;
    }

    int addhist() {
        PFrame newPFrame = pframes[upba.currentFrameIndex].deepcopy();
        if (nhistpframes < maxHistPFrames - 1) {
            histPFrames[nhistpframes] = newPFrame;
            return nhistpframes++;
        } else {
            for (int i = 0; i < maxHistPFrames - 1; i++) {
                histPFrames[i] = histPFrames[i + 1];
            }
            histPFrames[nhistpframes - 1] = newPFrame;
            return nhistpframes;
        }
    }

    void undo() {
        if (nhistpframes > 0) {
            nhistpframes--;
            PFrame newPFrame = histPFrames[nhistpframes].deepcopy();
            this.pastePFrame(newPFrame);
        } else {
        }
    }

    void memInPFrame() {
        PFrame newPFrame = pframes[upba.currentFrameIndex].deepcopy();
        memPFrame = newPFrame;
    }

    void memRPFrame() {
        this.addhist();
        this.pastePFrame(memPFrame);
    }

    void pastePFrame(PFrame aPFrame) {
        if (aPFrame != null) {
            PFrame newPFrame = aPFrame.deepcopy();
            pframes[upba.currentFrameIndex] = newPFrame;
        }
    }

    void insertPFrame(int index) {
        if (npframes < maxPFrames) {
            for (int i = npframes; i > index; i--) {
                pframes[i] = pframes[i - 1];
            }
            PFrame newPFrame = pframes[index].deepcopy();
            pframes[index + 1] = newPFrame;
            upba.currentFrameIndex++;
            npframes++;
        }
    }

    void deletePFrame() {
        if (npframes > 1) {
            for (int i = upba.currentFrameIndex; i < npframes - 1; i++) {
                pframes[i] = pframes[i + 1];
            }
            pframes[npframes - 1] = null;
            if (upba.currentFrameIndex > 0) {
                upba.currentFrameIndex--;
            }
            npframes--;
            this.resethist();
        }
    }

    void movePFrameUp() {
        PFrame keep;
        if (upba.currentFrameIndex < npframes - 1) {
            keep = pframes[upba.currentFrameIndex];
            pframes[upba.currentFrameIndex] = pframes[upba.currentFrameIndex + 1];
            pframes[upba.currentFrameIndex + 1] = keep;
            upba.currentFrameIndex++;
        }
    }

    void movePFrameDown() {
        PFrame keep;
        if (upba.currentFrameIndex > 0) {
            keep = pframes[upba.currentFrameIndex];
            pframes[upba.currentFrameIndex] = pframes[upba.currentFrameIndex - 1];
            pframes[upba.currentFrameIndex - 1] = keep;
            upba.currentFrameIndex--;
        }
    }

    void populateComments(int index) {
        for (int i = 0; i < npframes; i++) {
            pframes[i].setComment(pframes[index].comment);
        }
    }

    void populateConePositions(int index) {
        Position current[] = pframes[index].getPositions();
        for (int i = 0; i < npframes; i++) {
            Position apositions[] = pframes[i].getPositions();
            for (int j = 0; j < 8; j++) {
                apositions[j] = current[j];
            }
        }
    }
}
