import java.io.*;
import java.util.*;

public class projectileInfo {

    String path;

    String current = "Nothing";

    ArrayList general = new ArrayList();

    ArrayList hit = new ArrayList();

    ArrayList playerhit = new ArrayList();

    ArrayList time = new ArrayList();

    ArrayList projectile = new ArrayList();

    ArrayList projectile2 = new ArrayList();

    ArrayList projectile3 = new ArrayList();

    ArrayList projectile4 = new ArrayList();

    ArrayList projectiletrail = new ArrayList();

    String path2;

    projectileInfo projinfo;

    projectileInfo projinfo2;

    projectileInfo projinfo3;

    projectileInfo projinfo4;

    String fname;

    documentIndenter docindent;

    main par;

    boolean exists = true;

    int linenum = 1;

    ArrayList generalnum = new ArrayList();

    ArrayList hitnum = new ArrayList();

    ArrayList playerhitnum = new ArrayList();

    ArrayList timenum = new ArrayList();

    ArrayList projectilenum = new ArrayList();

    ArrayList projectile2num = new ArrayList();

    ArrayList projectile3num = new ArrayList();

    ArrayList projectile4num = new ArrayList();

    ArrayList projectiletrailnum = new ArrayList();

    String type = "";

    String image = "";

    String colour1 = "";

    String colour2 = "";

    String trail = "TRL_NONE";

    String gravity = "100";

    String dampening = "1";

    String timer = "";

    String timervar = "";

    boolean rotating = false;

    String rotincrement = "0";

    String rotspeed = "0";

    boolean useangle = false;

    boolean usespecangle = false;

    String angleimages = "0";

    boolean animating = false;

    String animrate = "0";

    String animtype = "";

    boolean projectiletrails = false;

    String htype = "Nothing";

    String hdamage = "0";

    String hbouncecoeff = "1";

    boolean hbounceexplode = false;

    boolean hprojectiles = false;

    boolean hprojectiles2 = false;

    boolean hprojectiles3 = false;

    boolean hprojectiles4 = false;

    String hshake = "0";

    String hsound = "";

    String phtype = "Nothing";

    String phdamage = "0";

    String phshake = "0";

    String phsound = "";

    boolean pdirt = false;

    boolean pgdirt = false;

    boolean phprojectiles = false;

    boolean phprojectiles2 = false;

    boolean phprojectiles3 = false;

    boolean phprojectiles4 = false;

    boolean istiming = false;

    String ttype = "nothing";

    String tdamage = "0";

    String tshake = "0";

    String tsound = "";

    boolean tprojectiles = false;

    boolean tprojectiles2 = false;

    boolean tprojectiles3 = false;

    boolean tprojectiles4 = false;

    String pprojectile = "";

    String pamount = "0";

    String pspeed = "0";

    String pspeedvar = "0";

    String pspread = "0";

    String pangle = "0";

    String pxoffset = "0";

    String pyoffset = "0";

    boolean puseangle = false;

    String p2projectile = "";

    String p2amount = "0";

    String p2speed = "0";

    String p2speedvar = "0";

    String p2spread = "0";

    String p2angle = "0";

    String p2xoffset = "0";

    String p2yoffset = "0";

    boolean p2useangle = false;

    String p3projectile = "";

    String p3amount = "0";

    String p3speed = "0";

    String p3speedvar = "0";

    String p3spread = "0";

    String p3angle = "0";

    String p3xoffset = "0";

    String p3yoffset = "0";

    boolean p3useangle = false;

    String p4projectile = "";

    String p4amount = "0";

    String p4speed = "0";

    String p4speedvar = "0";

    String p4spread = "0";

    String p4angle = "0";

    String p4xoffset = "0";

    String p4yoffset = "0";

    boolean p4useangle = false;

    String ptprojectile = "";

    String ptamount = "0";

    String ptspeed = "0";

    String ptspeedvar = "0";

    String ptspread = "0";

    String ptangle = "0";

    String ptxoffset = "0";

    String ptyoffset = "0";

    boolean ptuseangle = false;

    boolean ptuseprojvelocity = false;

    String ptdelay = "0";

    public projectileInfo(String path, String path2, String fname, main par) {
        this.path = path;
        this.path2 = path2;
        this.fname = fname;
        this.par = par;
        exec(path);
        if (exists) {
            sortGeneral();
            sortHit();
            sortPlayerHit();
            sortTime();
            if (trail.equalsIgnoreCase("TRL_PROJECTILE")) sortProjectileTrail();
            if (phprojectiles || hprojectiles || tprojectiles) sortProjectile();
            if (phprojectiles2 || hprojectiles2 || tprojectiles2) sortProjectile2();
            if (phprojectiles3 || hprojectiles3 || tprojectiles3) sortProjectile3();
            if (phprojectiles4 || hprojectiles4 || tprojectiles4) sortProjectile4();
            luaRename();
            save();
            docindent = new documentIndenter(path2 + "\\lua\\" + fname + ".lua");
        }
    }

    public void luaRename() {
        if (ptprojectile.length() > 3) {
            int ts = ptprojectile.length() - 4;
            String temp = ptprojectile.substring(0, ts);
            ptprojectile = temp + ".lua";
        }
        if (pprojectile.length() > 3) {
            int ts = pprojectile.length() - 4;
            String temp = pprojectile.substring(0, ts);
            pprojectile = temp + ".lua";
        }
    }

    public void exec(String flag) {
        File file9 = new File(flag);
        BufferedReader br1, br2, br3, br4;
        try {
            br1 = new BufferedReader(new InputStreamReader(new FileInputStream(file9)));
            br2 = new BufferedReader(new InputStreamReader(new FileInputStream(file9)));
            br3 = new BufferedReader(new InputStreamReader(new FileInputStream(file9)));
            br4 = new BufferedReader(new InputStreamReader(new FileInputStream(file9)));
        } catch (FileNotFoundException ex) {
            ((main) par).addError(fname + ".txt not found");
            exists = false;
            return;
        }
        int i = 0;
        String s = "";
        int ssize = 0;
        String s1 = "";
        String s2 = "";
        String temp;
        try {
            while (br2.readLine() != null) {
                if (!br3.readLine().startsWith("#")) {
                    if (br4.readLine().startsWith("[")) {
                        String ttt = br1.readLine();
                        String ttt2 = ttt.trim();
                        getSection(ttt2);
                    } else {
                        String ttt = br1.readLine();
                        String ttt2 = ttt.trim();
                        addCommand(ttt2);
                    }
                } else {
                    temp = br4.readLine();
                    temp = br1.readLine();
                }
                i++;
                linenum++;
            }
        } catch (IOException ex) {
            return;
        }
    }

    public void addCommand(String com) {
        if (current.equalsIgnoreCase("general")) {
            if (com.length() > 1) {
                general.add(com);
                generalnum.add("" + linenum);
            }
        } else if (current.equalsIgnoreCase("hit")) {
            if (com.length() > 1) {
                hit.add(com);
                hitnum.add("" + linenum);
            }
        } else if (current.equalsIgnoreCase("playerhit")) {
            if (com.length() > 1) {
                playerhit.add(com);
                playerhitnum.add("" + linenum);
            }
        } else if (current.equalsIgnoreCase("time")) {
            if (com.length() > 1) {
                time.add(com);
                timenum.add("" + linenum);
            }
        } else if (current.equalsIgnoreCase("projectiletrail")) {
            if (com.length() > 1) {
                projectiletrail.add(com);
                projectiletrailnum.add("" + linenum);
            }
        } else if (current.equalsIgnoreCase("projectile")) {
            if (com.length() > 1) {
                projectile.add(com);
                projectilenum.add("" + linenum);
            }
        } else if (current.equalsIgnoreCase("projectile2")) {
            if (com.length() > 1) {
                projectile2.add(com);
                projectile2num.add("" + linenum);
            }
        } else if (current.equalsIgnoreCase("projectile3")) {
            if (com.length() > 1) {
                projectile3.add(com);
                projectile3num.add("" + linenum);
            }
        } else if (current.equalsIgnoreCase("projectile4")) {
            if (com.length() > 1) {
                projectile4.add(com);
                projectile4num.add("" + linenum);
            }
        } else if (!getCommand(com).trim().equalsIgnoreCase("")) {
            ((main) par).addWarning("Statement out of place: \"" + getCommand(com) + "\" at line " + linenum + " in " + fname + ".txt");
        }
    }

    public void getSection(String sect) {
        int length = sect.length();
        String msect = sect.substring(1, length - 1);
        if (msect.equalsIgnoreCase("general")) current = "general"; else if (msect.equalsIgnoreCase("hit")) current = "hit"; else if (msect.equalsIgnoreCase("playerhit")) current = "playerhit"; else if (msect.equalsIgnoreCase("time")) current = "time"; else if (msect.equalsIgnoreCase("projectiletrail")) current = "projectiletrail"; else if (msect.equalsIgnoreCase("projectile") || msect.equalsIgnoreCase("projectile1")) current = "projectile"; else if (msect.equalsIgnoreCase("projectile2")) current = "projectile2"; else if (msect.equalsIgnoreCase("projectile3")) current = "projectile3"; else if (msect.equalsIgnoreCase("projectile4")) current = "projectile4"; else ((main) par).addWarning("Invalid section: \"" + msect + "\" at line " + linenum + " in " + fname + ".txt");
    }

    public void sortGeneral() {
        for (int i = 0; i < general.size(); i++) {
            String code = (String) general.get(i);
            if (getCommand(code).equalsIgnoreCase("type")) {
                type = getVar(code);
            } else if (getCommand(code).equalsIgnoreCase("image")) {
                image = getVar(code);
            } else if (getCommand(code).equalsIgnoreCase("colour1")) {
                colour1 = getVar(code);
            } else if (getCommand(code).equalsIgnoreCase("colour2")) {
                colour2 = getVar(code);
            } else if (getCommand(code).equalsIgnoreCase("trail")) {
                trail = getVar(code);
            } else if (getCommand(code).equalsIgnoreCase("gravity")) {
                gravity = getVar(code);
            } else if (getCommand(code).equalsIgnoreCase("dampening")) {
                dampening = getVar(code);
            } else if (getCommand(code).equalsIgnoreCase("timer")) {
                timer = getVar(code);
            } else if (getCommand(code).equalsIgnoreCase("timervar")) {
                timervar = getVar(code);
            } else if (getCommand(code).equalsIgnoreCase("rotating")) {
                String temp = getVar(code);
                if (temp.equalsIgnoreCase("True")) rotating = true; else rotating = false;
            } else if (getCommand(code).equalsIgnoreCase("rotincrement")) {
                rotincrement = getVar(code);
            } else if (getCommand(code).equalsIgnoreCase("rotspeed")) {
                rotspeed = getVar(code);
            } else if (getCommand(code).equalsIgnoreCase("useangle")) {
                String temp = getVar(code);
                if (temp.equalsIgnoreCase("True")) useangle = true; else useangle = false;
            } else if (getCommand(code).equalsIgnoreCase("usespecangle")) {
                String temp = getVar(code);
                if (temp.equalsIgnoreCase("True")) usespecangle = true; else usespecangle = false;
            } else if (getCommand(code).equalsIgnoreCase("angleimages")) {
                angleimages = getVar(code);
            } else if (getCommand(code).equalsIgnoreCase("animating")) {
                String temp = getVar(code);
                if (temp.equalsIgnoreCase("True")) animating = true; else animating = false;
            } else if (getCommand(code).equalsIgnoreCase("animrate")) {
                animrate = getVar(code);
            } else if (getCommand(code).equalsIgnoreCase("animtype")) {
                animtype = getVar(code);
            } else {
                ((main) par).addWarning("Invalid statement: \"" + getCommand(code) + "\" at line " + (String) generalnum.get(i) + " in " + fname + ".txt");
            }
        }
    }

    public void sortHit() {
        for (int i = 0; i < hit.size(); i++) {
            String code = (String) hit.get(i);
            if (getCommand(code).equalsIgnoreCase("type")) {
                htype = getVar(code);
            } else if (getCommand(code).equalsIgnoreCase("damage")) {
                hdamage = getVar(code);
            } else if (getCommand(code).equalsIgnoreCase("bouncecoeff")) {
                hbouncecoeff = getVar(code);
            } else if (getCommand(code).equalsIgnoreCase("bounceexplode")) {
                String temp = getVar(code);
                if (temp.equalsIgnoreCase("True")) hbounceexplode = true; else hbounceexplode = false;
            } else if (getCommand(code).equalsIgnoreCase("projectiles") || getCommand(code).equalsIgnoreCase("projectiles1")) {
                String temp = getVar(code);
                if (temp.equalsIgnoreCase("True")) hprojectiles = true; else hprojectiles = false;
            } else if (getCommand(code).equalsIgnoreCase("shake")) {
                hshake = getVar(code);
            } else if (getCommand(code).equalsIgnoreCase("sound")) {
                hsound = getVar(code);
            } else if (getCommand(code).equalsIgnoreCase("projectiles2")) {
                String temp = getVar(code);
                if (temp.equalsIgnoreCase("True")) hprojectiles2 = true; else hprojectiles2 = false;
            } else if (getCommand(code).equalsIgnoreCase("projectiles3")) {
                String temp = getVar(code);
                if (temp.equalsIgnoreCase("True")) hprojectiles3 = true; else hprojectiles3 = false;
            } else if (getCommand(code).equalsIgnoreCase("projectiles4")) {
                String temp = getVar(code);
                if (temp.equalsIgnoreCase("True")) hprojectiles4 = true; else hprojectiles4 = false;
            } else {
                ((main) par).addWarning("Invalid statement: \"" + getCommand(code) + "\" at line " + (String) hitnum.get(i) + " in " + fname + ".txt");
            }
        }
    }

    public void sortPlayerHit() {
        for (int i = 0; i < playerhit.size(); i++) {
            String code = (String) playerhit.get(i);
            if (getCommand(code).equalsIgnoreCase("type")) {
                phtype = getVar(code);
            } else if (getCommand(code).equalsIgnoreCase("damage")) {
                phdamage = getVar(code);
            } else if (getCommand(code).equalsIgnoreCase("projectiles") || getCommand(code).equalsIgnoreCase("projectiles1")) {
                String temp = getVar(code);
                if (temp.equalsIgnoreCase("True")) phprojectiles = true; else phprojectiles = false;
            } else if (getCommand(code).equalsIgnoreCase("shake")) {
                phshake = getVar(code);
            } else if (getCommand(code).equalsIgnoreCase("sound")) {
                phsound = getVar(code);
            } else if (getCommand(code).equalsIgnoreCase("projectiles2")) {
                String temp = getVar(code);
                if (temp.equalsIgnoreCase("True")) phprojectiles2 = true; else phprojectiles2 = false;
            } else if (getCommand(code).equalsIgnoreCase("projectiles3")) {
                String temp = getVar(code);
                if (temp.equalsIgnoreCase("True")) phprojectiles3 = true; else phprojectiles3 = false;
            } else if (getCommand(code).equalsIgnoreCase("projectiles4")) {
                String temp = getVar(code);
                if (temp.equalsIgnoreCase("True")) phprojectiles4 = true; else phprojectiles4 = false;
            } else {
                ((main) par).addWarning("Invalid statement: \"" + getCommand(code) + "\" at line " + (String) playerhitnum.get(i) + " in " + fname + ".txt");
            }
        }
    }

    public void sortTime() {
        for (int i = 0; i < time.size(); i++) {
            String code = (String) time.get(i);
            if (getCommand(code).equalsIgnoreCase("type")) {
                ttype = getVar(code);
            } else if (getCommand(code).equalsIgnoreCase("damage")) {
                tdamage = getVar(code);
            } else if (getCommand(code).equalsIgnoreCase("projectiles") || getCommand(code).equalsIgnoreCase("projectiles1")) {
                String temp = getVar(code);
                if (temp.equalsIgnoreCase("True")) tprojectiles = true; else tprojectiles = false;
            } else if (getCommand(code).equalsIgnoreCase("shake")) {
                tshake = getVar(code);
            } else if (getCommand(code).equalsIgnoreCase("sound")) {
                tsound = getVar(code);
            } else if (getCommand(code).equalsIgnoreCase("projectiles2")) {
                String temp = getVar(code);
                if (temp.equalsIgnoreCase("True")) tprojectiles2 = true; else tprojectiles2 = false;
            } else if (getCommand(code).equalsIgnoreCase("projectiles3")) {
                String temp = getVar(code);
                if (temp.equalsIgnoreCase("True")) tprojectiles3 = true; else tprojectiles3 = false;
            } else if (getCommand(code).equalsIgnoreCase("projectiles4")) {
                String temp = getVar(code);
                if (temp.equalsIgnoreCase("True")) tprojectiles4 = true; else tprojectiles4 = false;
            } else {
                ((main) par).addWarning("Invalid statement: \"" + getCommand(code) + "\" at line " + (String) timenum.get(i) + " in " + fname + ".txt");
            }
        }
    }

    public void sortProjectileTrail() {
        for (int i = 0; i < projectiletrail.size(); i++) {
            String code = (String) projectiletrail.get(i);
            if (getCommand(code).equalsIgnoreCase("Projectile")) {
                ptprojectile = getVar(code);
                System.out.println("Compiling: " + ptprojectile);
                projinfo = new projectileInfo(path2 + "//" + ptprojectile, path2, getFname(ptprojectile), par);
            } else if (getCommand(code).equalsIgnoreCase("Amount")) {
                ptamount = getVar(code);
            } else if (getCommand(code).equalsIgnoreCase("Speed")) {
                ptspeed = getVar(code);
            } else if (getCommand(code).equalsIgnoreCase("SpeedVar")) {
                ptspeedvar = getVar(code);
            } else if (getCommand(code).equalsIgnoreCase("Spread")) {
                ptspread = getVar(code);
            } else if (getCommand(code).equalsIgnoreCase("Angle")) {
                ptangle = getVar(code);
            } else if (getCommand(code).equalsIgnoreCase("Useangle")) {
                String temp = getVar(code);
                if (temp.equalsIgnoreCase("true")) ptuseangle = true; else ptuseangle = false;
            } else if (getCommand(code).equalsIgnoreCase("Useprojvelocity")) {
                String temp = getVar(code);
                if (temp.equalsIgnoreCase("true")) ptuseprojvelocity = true; else ptuseprojvelocity = false;
            } else if (getCommand(code).equalsIgnoreCase("Delay")) {
                ptdelay = getVar(code);
            } else if (getCommand(code).equalsIgnoreCase("yoffset")) {
                ptyoffset = getVar(code);
            } else if (getCommand(code).equalsIgnoreCase("xoffset")) {
                ptxoffset = getVar(code);
            } else {
                ((main) par).addWarning("Invalid statement: \"" + getCommand(code) + "\" at line " + (String) projectiletrailnum.get(i) + " in " + fname + ".txt");
            }
        }
    }

    public void sortProjectile() {
        for (int i = 0; i < projectile.size(); i++) {
            String code = (String) projectile.get(i);
            if (getCommand(code).equalsIgnoreCase("Amount")) {
                pamount = getVar(code);
            } else if (getCommand(code).equalsIgnoreCase("Speed")) {
                pspeed = getVar(code);
            } else if (getCommand(code).equalsIgnoreCase("SpeedVar")) {
                pspeedvar = getVar(code);
            } else if (getCommand(code).equalsIgnoreCase("Spread")) {
                pspread = getVar(code);
            } else if (getCommand(code).equalsIgnoreCase("Projectile")) {
                pprojectile = getVar(code);
                System.out.println("Compiling: " + pprojectile);
                projinfo = new projectileInfo(path2 + "//" + pprojectile, path2, getFname(pprojectile), par);
            } else if (getCommand(code).equalsIgnoreCase("Angle")) {
                pangle = getVar(code);
            } else if (getCommand(code).equalsIgnoreCase("Useangle")) {
                String temp = getVar(code);
                if (temp.equalsIgnoreCase("true")) puseangle = true; else puseangle = false;
            } else if (getCommand(code).equalsIgnoreCase("xoffset")) {
                pxoffset = getVar(code);
            } else if (getCommand(code).equalsIgnoreCase("yoffset")) {
                pyoffset = getVar(code);
            } else {
                ((main) par).addWarning("Invalid statement: \"" + getCommand(code) + "\" at line " + (String) projectilenum.get(i) + " in " + fname + ".txt");
            }
        }
    }

    public void sortProjectile2() {
        for (int i = 0; i < projectile2.size(); i++) {
            String code = (String) projectile2.get(i);
            if (getCommand(code).equalsIgnoreCase("Amount")) {
                p2amount = getVar(code);
            } else if (getCommand(code).equalsIgnoreCase("Speed")) {
                p2speed = getVar(code);
            } else if (getCommand(code).equalsIgnoreCase("SpeedVar")) {
                p2speedvar = getVar(code);
            } else if (getCommand(code).equalsIgnoreCase("Spread")) {
                p2spread = getVar(code);
            } else if (getCommand(code).equalsIgnoreCase("Projectile")) {
                p2projectile = getVar(code);
                System.out.println("Compiling: " + p2projectile);
                projinfo2 = new projectileInfo(path2 + "//" + p2projectile, path2, getFname(p2projectile), par);
            } else if (getCommand(code).equalsIgnoreCase("Angle")) {
                p2angle = getVar(code);
            } else if (getCommand(code).equalsIgnoreCase("Useangle")) {
                String temp = getVar(code);
                if (temp.equalsIgnoreCase("true")) p2useangle = true; else p2useangle = false;
            } else if (getCommand(code).equalsIgnoreCase("xoffset")) {
                p2xoffset = getVar(code);
            } else if (getCommand(code).equalsIgnoreCase("yoffset")) {
                p2yoffset = getVar(code);
            } else {
                ((main) par).addWarning("Invalid statement: \"" + getCommand(code) + "\" at line " + (String) projectile2num.get(i) + " in " + fname + ".txt");
            }
        }
    }

    public void sortProjectile3() {
        for (int i = 0; i < projectile3.size(); i++) {
            String code = (String) projectile3.get(i);
            if (getCommand(code).equalsIgnoreCase("Amount")) {
                p3amount = getVar(code);
            } else if (getCommand(code).equalsIgnoreCase("Speed")) {
                p3speed = getVar(code);
            } else if (getCommand(code).equalsIgnoreCase("SpeedVar")) {
                p3speedvar = getVar(code);
            } else if (getCommand(code).equalsIgnoreCase("Spread")) {
                p3spread = getVar(code);
            } else if (getCommand(code).equalsIgnoreCase("Projectile")) {
                p3projectile = getVar(code);
                System.out.println("Compiling: " + p3projectile);
                projinfo3 = new projectileInfo(path2 + "//" + p3projectile, path2, getFname(p3projectile), par);
            } else if (getCommand(code).equalsIgnoreCase("Angle")) {
                p3angle = getVar(code);
            } else if (getCommand(code).equalsIgnoreCase("Useangle")) {
                String temp = getVar(code);
                if (temp.equalsIgnoreCase("true")) p3useangle = true; else p3useangle = false;
            } else if (getCommand(code).equalsIgnoreCase("xoffset")) {
                p3xoffset = getVar(code);
            } else if (getCommand(code).equalsIgnoreCase("yoffset")) {
                p3yoffset = getVar(code);
            } else {
                ((main) par).addWarning("Invalid statement: \"" + getCommand(code) + "\" at line " + (String) projectile3num.get(i) + " in " + fname + ".txt");
            }
        }
    }

    public void sortProjectile4() {
        for (int i = 0; i < projectile4.size(); i++) {
            String code = (String) projectile4.get(i);
            if (getCommand(code).equalsIgnoreCase("Amount")) {
                p4amount = getVar(code);
            } else if (getCommand(code).equalsIgnoreCase("Speed")) {
                p4speed = getVar(code);
            } else if (getCommand(code).equalsIgnoreCase("SpeedVar")) {
                p4speedvar = getVar(code);
            } else if (getCommand(code).equalsIgnoreCase("Spread")) {
                p4spread = getVar(code);
            } else if (getCommand(code).equalsIgnoreCase("Projectile")) {
                p4projectile = getVar(code);
                System.out.println("Compiling: " + p4projectile);
                projinfo4 = new projectileInfo(path2 + "//" + p4projectile, path2, getFname(p4projectile), par);
            } else if (getCommand(code).equalsIgnoreCase("Angle")) {
                p4angle = getVar(code);
            } else if (getCommand(code).equalsIgnoreCase("Useangle")) {
                String temp = getVar(code);
                if (temp.equalsIgnoreCase("true")) p4useangle = true; else p4useangle = false;
            } else if (getCommand(code).equalsIgnoreCase("xoffset")) {
                p4xoffset = getVar(code);
            } else if (getCommand(code).equalsIgnoreCase("yoffset")) {
                p4yoffset = getVar(code);
            } else {
                ((main) par).addWarning("Invalid statement: \"" + getCommand(code) + "\" at line " + (String) projectile4num.get(i) + " in " + fname + ".txt");
            }
        }
    }

    public String getCommand(String command) {
        String com = command.trim();
        boolean found = false;
        int n1 = 0;
        int n2 = 0;
        for (int i = 0; i < com.length(); i++) {
            if (com.substring(i, i + 1).equalsIgnoreCase("=")) {
                n1 = i;
                n2 = i + 1;
                found = true;
                break;
            }
        }
        if (found) {
            String com2 = com.substring(0, n1 - 1);
            return com2;
        }
        return command;
    }

    public String getTrail() {
        if (trail.equalsIgnoreCase("TRL_CHEMSMOKE")) return "trails:chemsmoke";
        if (trail.equalsIgnoreCase("TRL_DOOMSDAY")) return "trails:doomsday";
        if (trail.equalsIgnoreCase("TRL_EXPLOSIVE")) return "trails:explosive";
        if (trail.equalsIgnoreCase("TRL_SMOKE")) return "trails:smoke";
        if (trail.equalsIgnoreCase("TRL_PROJECTILE")) {
            projectiletrails = true;
            return "trails:projectile";
        }
        if (trail.equalsIgnoreCase("TRL_NONE")) return "trails:none";
        return "trails:none";
    }

    public void saveGeneral(PrintWriter printwriter) {
        if (puseangle) pangle = "me.owner.angle";
        printwriter.println("function initialize()");
        printwriter.println("me.style = " + getType());
        if (getType().equalsIgnoreCase("projectiles.image")) {
            printwriter.println("me.image.source = surface(\"" + image + "\", true)");
            if (!angleimages.trim().equalsIgnoreCase("") && useangle) printwriter.println("me.image.angles = " + angleimages);
        } else if (getType().equalsIgnoreCase("projectiles.pixel")) {
            if (!colour1.trim().equalsIgnoreCase("")) {
                printwriter.println("me.pixel.colour1 = colour(" + colour1 + ")");
                if (!colour2.trim().equalsIgnoreCase("")) printwriter.println("me.pixel.colour2 = colour(" + colour2 + ")");
            }
        } else if (getType().equalsIgnoreCase("projectiles.line")) {
        }
        printwriter.println("me.trail = " + getTrail());
        printwriter.println("-- me.gravity(" + gravity + ")");
        printwriter.println("-- me.dampening(" + dampening + ")");
        if (!timer.equalsIgnoreCase("")) {
            printwriter.println("me.timers.add(" + timer + ", \"tmr_explode\")");
            istiming = true;
            if (!timervar.trim().equalsIgnoreCase("")) printwriter.println("--me.timervar(" + timervar + ")");
        }
        if (rotating) {
            printwriter.println("me.rotating = true");
            printwriter.println("me.rotincrement = " + rotincrement);
            printwriter.println("me.rotspeed = " + rotspeed);
        }
        if (animating) {
            printwriter.println("me.animating = true");
            printwriter.println("me.animrate = " + animrate);
            printwriter.println("me.animtype = " + getanimtype());
        }
        printwriter.println("callbacks.onhit = \"onhit\"");
        printwriter.println("callbacks.onplayerhit = \"onplayerhit\"");
        printwriter.println("end");
        if (!htype.equalsIgnoreCase("Nothing") && !getH().equalsIgnoreCase("Nothing")) {
            printwriter.println("function onhit()");
            printwriter.println(getH());
            if (!hsound.trim().equalsIgnoreCase("")) {
                printwriter.println("sample(\"" + hsound + "\"):play()");
            }
            if (hprojectiles) {
                if (!pspeedvar.equalsIgnoreCase("")) printwriter.println("me:spawn(\"" + pprojectile + "\", " + pamount + ", " + pxoffset + ", " + pyoffset + ", " + pangle + ", " + pspeed + ", " + pspread + ")"); else printwriter.println("me:spawn(\"" + pprojectile + "\", " + pamount + ", " + pxoffset + ", " + pyoffset + ", " + pangle + ", vector(" + pspeed + "," + pspeedvar + "), " + pspread + ")");
            }
            if (hprojectiles2) {
                if (!p2speedvar.equalsIgnoreCase("")) printwriter.println("me:spawn(\"" + p2projectile + "\", " + p2amount + ", " + p2xoffset + ", " + p2yoffset + ", " + p2angle + ", " + p2speed + ", " + p2spread + ")"); else printwriter.println("me:spawn(\"" + p2projectile + "\", " + p2amount + ", " + p2xoffset + ", " + p2yoffset + ", " + p2angle + ", vector(" + p2speed + "," + p2speedvar + "), " + p2spread + ")");
            }
            if (hprojectiles3) {
                if (!p3speedvar.equalsIgnoreCase("")) printwriter.println("me:spawn(\"" + p3projectile + "\", " + p3amount + ", " + p3xoffset + ", " + p3yoffset + ", " + p3angle + ", " + p3speed + ", " + p3spread + ")"); else printwriter.println("me:spawn(\"" + p3projectile + "\", " + p3amount + ", " + p3xoffset + ", " + p3yoffset + ", " + p3angle + ", vector(" + p3speed + "," + p3speedvar + "), " + p3spread + ")");
            }
            if (hprojectiles4) {
                if (!p4speedvar.equalsIgnoreCase("")) printwriter.println("me:spawn(\"" + p4projectile + "\", " + p4amount + ", " + p4xoffset + ", " + p4yoffset + ", " + p4angle + ", " + p4speed + ", " + p4spread + ")"); else printwriter.println("me:spawn(\"" + p4projectile + "\", " + p4amount + ", " + p4xoffset + ", " + p4yoffset + ", " + p4angle + ", vector(" + p4speed + "," + p4speedvar + "), " + p4spread + ")");
            }
            printwriter.println("end");
        }
        if (!phtype.equalsIgnoreCase("Nothing") && !getPH().equalsIgnoreCase("Nothing")) {
            printwriter.println("function onplayerhit(player)");
            printwriter.println(getPH());
            if (!phsound.trim().equalsIgnoreCase("")) {
                printwriter.println("sample(\"" + phsound + "\"):play()");
            }
            if (phprojectiles) {
                if (!pspeedvar.equalsIgnoreCase("")) printwriter.println("me:spawn(\"" + pprojectile + "\", " + pamount + ", " + pxoffset + ", " + pyoffset + ", " + pangle + ", " + pspeed + ", " + pspread + ")"); else printwriter.println("me:spawn(\"" + pprojectile + "\", " + pamount + ", " + pxoffset + ", " + pyoffset + ", " + pangle + ", vector(" + pspeed + "," + pspeedvar + "), " + pspread + ")");
            }
            if (phprojectiles2) {
                if (!p2speedvar.equalsIgnoreCase("")) printwriter.println("me:spawn(\"" + p2projectile + "\", " + p2amount + ", " + p2xoffset + ", " + p2yoffset + ", " + p2angle + ", " + p2speed + ", " + p2spread + ")"); else printwriter.println("me:spawn(\"" + p2projectile + "\", " + p2amount + ", " + p2xoffset + ", " + p2yoffset + ", " + p2angle + ", vector(" + p2speed + "," + p2speedvar + "), " + p2spread + ")");
            }
            if (phprojectiles3) {
                if (!p3speedvar.equalsIgnoreCase("")) printwriter.println("me:spawn(\"" + p3projectile + "\", " + p3amount + ", " + p3xoffset + ", " + p3yoffset + ", " + p3angle + ", " + p3speed + ", " + p3spread + ")"); else printwriter.println("me:spawn(\"" + p3projectile + "\", " + p3amount + ", " + p3xoffset + ", " + p3yoffset + ", " + p3angle + ", vector(" + p3speed + "," + p3speedvar + "), " + p3spread + ")");
            }
            if (phprojectiles4) {
                if (!p4speedvar.equalsIgnoreCase("")) printwriter.println("me:spawn(\"" + p4projectile + "\", " + p4amount + ", " + p4xoffset + ", " + p4yoffset + ", " + p4angle + ", " + p4speed + ", " + p4spread + ")"); else printwriter.println("me:spawn(\"" + p4projectile + "\", " + p4amount + ", " + p4xoffset + ", " + p4yoffset + ", " + p4angle + ", vector(" + p4speed + "," + p4speedvar + "), " + p4spread + ")");
            }
            printwriter.println("end");
        }
        if (istiming) {
            printwriter.println("function tmr_explode");
            if (!ttype.trim().equalsIgnoreCase("Nothing")) {
                printwriter.println(getT());
                if (tprojectiles) {
                    if (!pspeedvar.equalsIgnoreCase("")) printwriter.println("me:spawn(\"" + pprojectile + "\", " + pamount + ", " + pxoffset + ", " + pyoffset + ", " + pangle + ", " + pspeed + ", " + pspread + ")"); else printwriter.println("me:spawn(\"" + pprojectile + "\", " + pamount + ", " + pxoffset + ", " + pyoffset + ", " + pangle + ", vector(" + pspeed + "," + pspeedvar + "), " + pspread + ")");
                }
                if (tprojectiles2) {
                    if (!p2speedvar.equalsIgnoreCase("")) printwriter.println("me:spawn(\"" + p2projectile + "\", " + p2amount + ", " + p2xoffset + ", " + p2yoffset + ", " + p2angle + ", " + p2speed + ", " + p2spread + ")"); else printwriter.println("me:spawn(\"" + p2projectile + "\", " + p2amount + ", " + p2xoffset + ", " + p2yoffset + ", " + p2angle + ", vector(" + p2speed + "," + p2speedvar + "), " + p2spread + ")");
                }
                if (tprojectiles3) {
                    if (!p3speedvar.equalsIgnoreCase("")) printwriter.println("me:spawn(\"" + p3projectile + "\", " + p3amount + ", " + p3xoffset + ", " + p3yoffset + ", " + p3angle + ", " + p3speed + ", " + p3spread + ")"); else printwriter.println("me:spawn(\"" + p3projectile + "\", " + p3amount + ", " + p3xoffset + ", " + p3yoffset + ", " + p3angle + ", vector(" + p3speed + "," + p3speedvar + "), " + p3spread + ")");
                }
                if (tprojectiles4) {
                    if (!p4speedvar.equalsIgnoreCase("")) printwriter.println("me:spawn(\"" + p4projectile + "\", " + p4amount + ", " + p4xoffset + ", " + p4yoffset + ", " + p4angle + ", " + p4speed + ", " + p4spread + ")"); else printwriter.println("me:spawn(\"" + p4projectile + "\", " + p4amount + ", " + p4xoffset + ", " + p4yoffset + ", " + p4angle + ", vector(" + p4speed + "," + p4speedvar + "), " + p4spread + ")");
                }
                if (!tsound.trim().equalsIgnoreCase("")) {
                    printwriter.println("sample(\"" + tsound + "\"):play()");
                }
            }
            printwriter.println("end");
        }
    }

    public String getType() {
        String sclass = "";
        String wclass = type;
        if (wclass.equalsIgnoreCase("PRJ_IMAGE")) return "projectiles.image"; else if (wclass.equalsIgnoreCase("PRJ_PIXEL")) return "projectiles.pixel";
        return "projectiles.none";
    }

    public String getanimtype() {
        if (animtype.equalsIgnoreCase("ANI_ONCE")) {
            return "animating.once";
        }
        if (animtype.equalsIgnoreCase("ANI_LOOP")) {
            return "animating.loop";
        }
        if (animtype.equalsIgnoreCase("ANI_PINGPONG")) {
            return "animating.pingpong";
        }
        return "animating.once";
    }

    public void save() {
        File file9;
        File file1;
        file9 = new File(path2 + "\\lua\\" + fname + ".lua");
        file1 = new File(path2 + "\\lua");
        file1.mkdirs();
        PrintWriter printwriter;
        File file = file9;
        try {
            printwriter = new PrintWriter(new BufferedOutputStream(new FileOutputStream(file)));
        } catch (IOException ioexception) {
            System.out.println(ioexception);
            System.out.println(file);
            System.out.println(file.canRead());
            System.out.println(file.canWrite());
            return;
        }
        String t1 = "";
        String t2 = "";
        String t3 = "";
        printwriter.println("-- Created by the LXE Script Basic Compiler");
        saveGeneral(printwriter);
        printwriter.close();
    }

    public String getHType() {
        if (htype.equalsIgnoreCase("Explode")) {
            return "effects:explode";
        }
        if (htype.equalsIgnoreCase("Carve")) {
            return "me:carve";
        }
        if (htype.equalsIgnoreCase("Bounce")) {
            return "me:bounce";
        }
        return "Nothing";
    }

    public String getPHType() {
        if (phtype.equalsIgnoreCase("Injure")) {
            return "player:injure";
        }
        if (phtype.equalsIgnoreCase("Explode")) {
            return "me:explode";
        }
        return "";
    }

    public String getPH() {
        if (phtype.equalsIgnoreCase("Injure")) {
            return "player:injure(" + phdamage + ")";
        }
        if (phtype.equalsIgnoreCase("Explode")) {
            return "me:explode(" + phdamage + ", " + phshake + ")";
        }
        return "Nothing";
    }

    public String getH() {
        if (htype.equalsIgnoreCase("Explode")) {
            return "me:explode(" + hdamage + ", " + hshake + ")";
        }
        if (htype.equalsIgnoreCase("Carve")) {
            return "me:carve(" + hdamage + ")";
        }
        if (htype.equalsIgnoreCase("Bounce")) {
            return "me:bounce(" + hbouncecoeff + ")";
        }
        return "Nothing";
    }

    public String getT() {
        if (ttype.equalsIgnoreCase("Explode")) {
            return "me:explode(" + tdamage + ", " + tshake + ")";
        }
        if (ttype.equalsIgnoreCase("Carve")) {
            return "me:carve(" + tdamage + ")";
        }
        return "Nothing";
    }

    public String getFname(String fname1) {
        int ilength = fname1.length() - 4;
        String fname2 = fname1.substring(0, ilength);
        return fname2;
    }

    public String getVar(String command) {
        String com = command.trim();
        int n1 = 0;
        int n2 = 0;
        for (int i = 0; i < com.length(); i++) {
            if (com.substring(i, i + 1).equalsIgnoreCase("=")) {
                n1 = i;
                n2 = i + 1;
                break;
            }
        }
        String com2 = com.substring(n2 + 1, com.length());
        return com2;
    }
}
