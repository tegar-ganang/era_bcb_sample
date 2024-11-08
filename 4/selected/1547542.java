package hardware;

import java.net.URL;
import javax.swing.SwingUtilities;

public class Machine implements Runnable {

    private volatile Thread runner = null;

    public Memory mem;

    private M6809 micro;

    private Screen screen;

    public Keyboard keyboard;

    private Mouse mouse;

    private ThomSound sound;

    private boolean appletMode = false;

    private URL appletCodeBase;

    private int vbl;

    private long screenClock = 0;

    private sap mysap;

    public Machine(Screen screen) {
        this.appletMode = false;
        this.mem = new Memory(screen, this);
        this.screen = screen;
        this.micro = new M6809(mem, this);
        this.keyboard = new Keyboard(screen, mem);
        this.mouse = new Mouse(screen, mem);
        this.screen.init(mem);
        this.vbl = 0;
        this.mysap = new sap();
        this.sound = new ThomSound();
        sound.init(19968, 1);
    }

    public Machine(Screen screen, URL appletCodeBase) {
        this.appletMode = true;
        this.mem = new Memory(appletCodeBase, screen, this);
        System.out.println("Memory OK");
        this.screen = screen;
        this.micro = new M6809(mem, this);
        System.out.println("6809 OK");
        this.keyboard = new Keyboard(screen, mem);
        System.out.println("keyboard OK");
        this.mouse = new Mouse(screen, mem);
        System.out.println("Mouse OK");
        this.screen.init(mem);
        System.out.println("screen OK");
        this.vbl = 0;
        this.mysap = new sap();
        System.out.println("sap OK");
        this.sound = new ThomSound();
        System.out.println("sound1 OK");
        sound.init(19968, 1);
        System.out.println("sound2 OK");
    }

    public void start() {
        if (runner == null) {
            runner = new Thread(this);
            runner.start();
        }
    }

    public void stop() {
        if (runner != null) {
            runner = null;
        }
    }

    public void run() {
        Thread thisThread = Thread.currentThread();
        while (runner == thisThread) {
            fullSpeed();
            synchronize();
        }
    }

    public long getClock() {
        return micro.getClock();
    }

    public void traceLog(String s) {
        System.out.println(micro.hex(micro.getPC(), 4) + " - " + s);
    }

    public long getScreenClock() {
        return screenClock;
    }

    private boolean IRQ = false;

    public boolean warp = false;

    private void fullSpeed() {
        int line;
        int CC;
        screenClock = micro.getClock();
        sound.startFrame();
        micro.FetchUntil(12);
        CC = micro.readCC();
        if (((CC & 0x10) == 0) && (mem.TO_keypressed)) {
            mem.TO_keypressed = false;
            micro.IRQ();
        }
        micro.FetchUntil(40);
        micro.FetchUntil(12);
        for (line = 1; line < 56; line++) {
            micro.FetchUntil(12);
            micro.FetchUntil(40);
            micro.FetchUntil(12);
        }
        for (line = 0; line < 200; line++) {
            micro.FetchUntil(12);
            micro.FetchUntil(40);
            micro.FetchUntil(12);
            screen.doLine(line);
        }
        for (line = 0; line < 55; line++) {
            micro.FetchUntil(12);
            micro.FetchUntil(40);
            micro.FetchUntil(12);
        }
        micro.FetchUntil(12);
        micro.FetchUntil(20);
        if (mem.do_vbl(vbl)) {
            CC = micro.readCC();
            if ((CC & 0x10) == 0) micro.IRQ();
        }
        micro.FetchUntil(20);
        micro.FetchUntil(12);
        mem.do_vbl_end(vbl);
        this.vbl++;
        if (screen.isPainted()) {
            screen.updateImage(true);
        }
        sound.updateSamples();
    }

    public void setPseudoStereo(boolean pseudo) {
        sound.setPseudoStereo(pseudo);
    }

    public void addSoundEvent(int val) {
        sound.addFrameEvent((int) (micro.getClock() - screenClock), val);
    }

    private long lastTime = System.currentTimeMillis();

    private void synchronize() {
        int realTimeMillis = (int) (System.currentTimeMillis() - lastTime);
        int sleepMillis = 20 - realTimeMillis - 1;
        if (sleepMillis < 0) {
            lastTime = System.currentTimeMillis();
            return;
        }
        try {
            if (!warp) {
                runner.sleep(sleepMillis);
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        lastTime = System.currentTimeMillis();
    }

    public boolean setK7FileFromUrl(String K7) {
        return mem.setK7FileFromUrl(K7);
    }

    public boolean setDiskFromUrl(String image, int unit) {
        this.mysap.setSAPFromUrl(image, unit);
        return true;
    }

    public boolean setDiskFromUrlZip(String zip, String image, int unit) {
        this.mysap.setSAPFromUrlZip(zip, image, unit);
        return true;
    }

    public boolean setDiskFromFile(String image, int unit) {
        this.mysap.setSAPFromFile(image, unit);
        return true;
    }

    public boolean setK7File(String K7) {
        return mem.setK7File(K7);
    }

    public void setKey(int i) {
        mem.setKey(i);
    }

    public void remKey(int i) {
        mem.remKey(i);
    }

    public void resetSoft() {
        screen.splashtimer = 200;
        this.micro.reset();
    }

    public void resetHard() {
        screen.splashtimer = 200;
        int i;
        this.mem.reset();
        this.micro.reset();
        this.screen.SetGraphicMode(0);
    }

    public String dumpRegisters() {
        return this.micro.printState();
    }

    public String unassembleFromPC(int nblines) {
        return this.micro.unassemble(this.micro.getPC(), nblines);
    }

    public String dumpSystemStack(int nblines) {
        return "00";
    }

    public void step() {
        if (runner == null) micro.Fetch();
    }

    public String dumpMemory(int start, int len) {
        String s = "";
        int i;
        for (i = 0; i < len; i++) {
            if ((i % 16) == 0) {
                s += micro.hex(start + i, 4);
                s += " ";
            }
            s += micro.hex(this.mem.get(start + i), 2);
            if ((i % 16) == 15) {
                s += "\n";
            }
        }
        return s;
    }

    public void InitDiskCtrl() {
        micro.andcc(0xFE);
        mem.write(0x604E, 'D');
    }

    void ReadSector() {
        sapSector l_SAPsector = new sapSector();
        int l_index;
        int l_drive;
        int l_destData;
        int l_err;
        screen.flash_led();
        l_drive = mem.read(0x6049);
        l_SAPsector.piste = (mem.read(0x604A) << 8) | mem.read(0x604B);
        l_SAPsector.secteur = mem.read(0x604C);
        l_destData = (mem.read(0x604F) << 8) | mem.read(0x6050);
        if ((l_drive > 3) || (l_SAPsector.piste >= 80) || (l_SAPsector.secteur > 16)) {
            micro.orcc(0x01);
            mem.write(0x604E, 0x10);
            return;
        }
        l_err = 0;
        l_err = mysap.SapGetSector(l_drive, l_SAPsector);
        if (l_err != 0) System.out.println("error read sector" + l_err);
        if (l_err == 0) {
            l_err = mysap.verify_sap_lect(l_SAPsector);
            if (l_err != 0) System.out.println("error verify sector" + l_err);
        }
        if (l_err == 0) {
            for (l_index = 0; l_index < 256; l_index++) mem.write((l_index + l_destData) & 0xFFFF, l_SAPsector.data[l_index] ^ 0xB3);
        }
        if (l_err == 0) {
            mem.write(0x604E, 0x00);
            micro.andcc(0xFE);
        } else {
            mem.write(0x604E, l_err);
            micro.orcc(0x01);
        }
    }

    public int periph(int PC, int S, int res) {
        switch(PC) {
            case 0xF42A:
                int x = mem.getX();
                int y = mem.getY();
                mem.write(0x60D6, y >> 8);
                mem.write(0x60D7, y & 0xFF);
                mem.write(0x60D8, x >> 8);
                mem.write(0x60D9, x & 0xFF);
                mem.mouse_upd();
                return res;
            case 0xE0FF:
                InitDiskCtrl();
                return res;
            case 0xE3A8:
                ReadSector();
                break;
            case 0xE178:
                break;
            case 0xFCF5:
                micro.Fetch(0xD4);
                mem.write(0xE7DA, mem.read(0xE7DA));
                return 0;
            case 0x315A:
                micro.Fetch(0x8E);
                break;
            case 0x337E:
            case 0x3F97:
                micro.setX(mem.getX());
                micro.setY(mem.getY());
                res = 0xff;
                break;
        }
        return res;
    }
}
