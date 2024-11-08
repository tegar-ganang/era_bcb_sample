package org.lateralgm.file;

import java.awt.Dimension;
import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;
import java.util.zip.DataFormatException;
import org.lateralgm.components.impl.ResNode;
import org.lateralgm.file.GmFile.ResourceHolder;
import org.lateralgm.file.iconio.ICOFile;
import org.lateralgm.main.Util;
import org.lateralgm.messages.Messages;
import org.lateralgm.resources.Background;
import org.lateralgm.resources.Background.PBackground;
import org.lateralgm.resources.Extensions;
import org.lateralgm.resources.Font;
import org.lateralgm.resources.Font.PFont;
import org.lateralgm.resources.GameInformation;
import org.lateralgm.resources.GameInformation.PGameInformation;
import org.lateralgm.resources.GameSettings;
import org.lateralgm.resources.GameSettings.IncludeFolder;
import org.lateralgm.resources.GameSettings.PGameSettings;
import org.lateralgm.resources.GameSettings.ProgressBar;
import org.lateralgm.resources.GmObject;
import org.lateralgm.resources.GmObject.PGmObject;
import org.lateralgm.resources.Include;
import org.lateralgm.resources.InstantiableResource;
import org.lateralgm.resources.Path;
import org.lateralgm.resources.Path.PPath;
import org.lateralgm.resources.Resource;
import org.lateralgm.resources.ResourceReference;
import org.lateralgm.resources.Room;
import org.lateralgm.resources.Room.PRoom;
import org.lateralgm.resources.Script;
import org.lateralgm.resources.Script.PScript;
import org.lateralgm.resources.Sound;
import org.lateralgm.resources.Sound.PSound;
import org.lateralgm.resources.Sprite;
import org.lateralgm.resources.Sprite.BBMode;
import org.lateralgm.resources.Sprite.PSprite;
import org.lateralgm.resources.Timeline;
import org.lateralgm.resources.library.LibAction;
import org.lateralgm.resources.library.LibArgument;
import org.lateralgm.resources.library.LibManager;
import org.lateralgm.resources.sub.Action;
import org.lateralgm.resources.sub.ActionContainer;
import org.lateralgm.resources.sub.Argument;
import org.lateralgm.resources.sub.BackgroundDef;
import org.lateralgm.resources.sub.BackgroundDef.PBackgroundDef;
import org.lateralgm.resources.sub.Constant;
import org.lateralgm.resources.sub.Event;
import org.lateralgm.resources.sub.Instance;
import org.lateralgm.resources.sub.Instance.PInstance;
import org.lateralgm.resources.sub.MainEvent;
import org.lateralgm.resources.sub.Moment;
import org.lateralgm.resources.sub.PathPoint;
import org.lateralgm.resources.sub.Tile;
import org.lateralgm.resources.sub.Tile.PTile;
import org.lateralgm.resources.sub.Trigger;
import org.lateralgm.resources.sub.View;
import org.lateralgm.resources.sub.View.PView;
import org.lateralgm.util.PropertyMap;

public final class GmFileReader {

    private GmFileReader() {
    }

    static Queue<PostponedRef> postpone = new LinkedList<PostponedRef>();

    static interface PostponedRef {

        boolean invoke();
    }

    static class DefaultPostponedRef<K extends Enum<K>> implements PostponedRef {

        ResourceList<?> list;

        String name;

        PropertyMap<K> p;

        K key;

        DefaultPostponedRef(ResourceList<?> list, PropertyMap<K> p, K key, String name) {
            this.list = list;
            this.p = p;
            this.key = key;
            this.name = name;
        }

        public boolean invoke() {
            Resource<?, ?> temp = list.get(name);
            if (temp != null) p.put(key, temp.reference);
            return temp != null;
        }
    }

    private static class GmFileContext {

        GmFile f;

        GmStreamDecoder in;

        RefList<Timeline> timeids;

        RefList<GmObject> objids;

        RefList<Room> rmids;

        public GmFileContext(GmFile f, GmStreamDecoder in, RefList<Timeline> timeids, RefList<GmObject> objids, RefList<Room> rmids) {
            this.f = f;
            this.in = in;
            this.timeids = timeids;
            this.objids = objids;
            this.rmids = rmids;
        }

        public GmFileContext copy() {
            return new GmFileContext(f, in, timeids, objids, rmids);
        }
    }

    private static GmFormatException versionError(GmFile f, String error, String res, int ver) {
        return versionError(f, error, res, 0, ver);
    }

    private static GmFormatException versionError(GmFile f, String error, String res, int i, int ver) {
        return new GmFormatException(f, Messages.format("GmFileReader.ERROR_UNSUPPORTED", Messages.format("GmFileReader." + error, Messages.getString("LGM." + res), i), ver));
    }

    public static GmFile readGmFile(InputStream stream, URI uri, ResNode root) throws GmFormatException {
        GmFile f = new GmFile();
        f.uri = uri;
        GmStreamDecoder in = null;
        RefList<Timeline> timeids = new RefList<Timeline>(Timeline.class);
        RefList<GmObject> objids = new RefList<GmObject>(GmObject.class);
        RefList<Room> rmids = new RefList<Room>(Room.class);
        try {
            long startTime = System.currentTimeMillis();
            in = new GmStreamDecoder(stream);
            GmFileContext c = new GmFileContext(f, in, timeids, objids, rmids);
            int identifier = in.read4();
            if (identifier != 1234321) throw new GmFormatException(f, Messages.format("GmFileReader.ERROR_INVALID", uri, identifier));
            int ver = in.read4();
            f.format = GmFile.FormatFlavor.getVersionFlavor(ver);
            if (ver != 530 && ver != 600 && ver != 701 && ver != 800 && ver != 810) {
                String msg = Messages.format("GmFileReader.ERROR_UNSUPPORTED", uri, ver);
                throw new GmFormatException(f, msg);
            }
            if (ver == 530) in.skip(4);
            if (ver == 701) {
                int s1 = in.read4();
                int s2 = in.read4();
                in.skip(s1 * 4);
                int seed = in.read4();
                in.skip(s2 * 4);
                int b1 = in.read();
                in.setSeed(seed);
                f.gameSettings.put(PGameSettings.GAME_ID, b1 | in.read3() << 8);
            } else f.gameSettings.put(PGameSettings.GAME_ID, in.read4());
            in.read((byte[]) f.gameSettings.get(PGameSettings.DPLAY_GUID));
            readSettings(c);
            if (ver >= 800) {
                readTriggers(c);
                readConstants(c);
            }
            readSounds(c);
            readSprites(c);
            readBackgrounds(c);
            readPaths(c);
            readScripts(c);
            readFonts(c);
            readTimelines(c);
            readGmObjects(c);
            readRooms(c);
            f.lastInstanceId = in.read4();
            f.lastTileId = in.read4();
            if (ver >= 700) {
                readIncludedFiles(c);
                readPackages(c);
            }
            readGameInformation(c);
            ver = in.read4();
            if (ver != 500) throw new GmFormatException(f, Messages.format("GmFileReader.ERROR_UNSUPPORTED", Messages.getString("GmFileReader.AFTERINFO"), ver));
            int no = in.read4();
            for (int j = 0; j < no; j++) in.skip(in.read4());
            ver = in.read4();
            if (ver != 500 && ver != 540 && ver != 700) throw new GmFormatException(f, Messages.format("GmFileReader.ERROR_UNSUPPORTED", Messages.getString("GmFileReader.AFTERINFO2"), ver));
            in.skip(in.read4() * 4);
            readTree(c, root, ver);
            System.out.println(Messages.format("GmFileReader.LOADTIME", System.currentTimeMillis() - startTime));
        } catch (Exception e) {
            if ((e instanceof GmFormatException)) throw (GmFormatException) e;
            throw new GmFormatException(f, e);
        } finally {
            try {
                if (in != null) {
                    in.close();
                    in = null;
                }
            } catch (IOException ex) {
                String key = Messages.getString("GmFileReader.ERROR_CLOSEFAILED");
                throw new GmFormatException(f, key);
            }
        }
        return f;
    }

    private static void readSettings(GmFileContext c) throws IOException, GmFormatException, DataFormatException {
        GmStreamDecoder in = c.in;
        GameSettings g = c.f.gameSettings;
        PropertyMap<PGameSettings> p = g.properties;
        int ver = in.read4();
        if (ver != 530 && ver != 542 && ver != 600 && ver != 702 && ver != 800) {
            String msg = Messages.format("GmFileReader.ERROR_UNSUPPORTED", "", ver);
            throw new GmFormatException(c.f, msg);
        }
        if (ver == 800) in.beginInflate();
        in.readBool(p, PGameSettings.START_FULLSCREEN);
        if (ver >= 600) in.readBool(p, PGameSettings.INTERPOLATE);
        in.readBool(p, PGameSettings.DONT_DRAW_BORDER, PGameSettings.DISPLAY_CURSOR);
        in.read4(p, PGameSettings.SCALING);
        if (ver == 530) in.skip(8); else {
            in.readBool(p, PGameSettings.ALLOW_WINDOW_RESIZE, PGameSettings.ALWAYS_ON_TOP);
            p.put(PGameSettings.COLOR_OUTSIDE_ROOM, Util.convertGmColor(in.read4()));
        }
        in.readBool(p, PGameSettings.SET_RESOLUTION);
        int colorDepth = 0, frequency;
        if (ver == 530) {
            in.skip(8);
            p.put(PGameSettings.RESOLUTION, GmFile.GS5_RESOLS[in.read4()]);
            byte b = (byte) in.read4();
            frequency = (b == 4) ? 0 : (byte) (b + 1);
            in.skip(8);
        } else {
            colorDepth = (byte) in.read4();
            p.put(PGameSettings.RESOLUTION, GmFile.GS_RESOLS[in.read4()]);
            frequency = (byte) in.read4();
        }
        p.put(PGameSettings.COLOR_DEPTH, GmFile.GS_DEPTHS[colorDepth]);
        p.put(PGameSettings.FREQUENCY, GmFile.GS_FREQS[frequency]);
        in.readBool(p, PGameSettings.DONT_SHOW_BUTTONS);
        if (ver > 530) in.readBool(p, PGameSettings.USE_SYNCHRONIZATION);
        if (ver >= 800) in.readBool(p, PGameSettings.DISABLE_SCREENSAVERS);
        in.readBool(p, PGameSettings.LET_F4_SWITCH_FULLSCREEN, PGameSettings.LET_F1_SHOW_GAME_INFO, PGameSettings.LET_ESC_END_GAME, PGameSettings.LET_F5_SAVE_F6_LOAD);
        if (ver == 530) in.skip(8);
        if (ver > 600) in.readBool(p, PGameSettings.LET_F9_SCREENSHOT, PGameSettings.TREAT_CLOSE_AS_ESCAPE);
        p.put(PGameSettings.GAME_PRIORITY, GmFile.GS_PRIORITIES[in.read4()]);
        in.readBool(p, PGameSettings.FREEZE_ON_LOSE_FOCUS);
        p.put(PGameSettings.LOAD_BAR_MODE, GmFile.GS_PROGBARS[in.read4()]);
        if (p.get(PGameSettings.LOAD_BAR_MODE) == ProgressBar.CUSTOM) {
            if (ver < 800) {
                if (in.read4() != -1) p.put(PGameSettings.BACK_LOAD_BAR, in.readZlibImage());
                if (in.read4() != -1) p.put(PGameSettings.FRONT_LOAD_BAR, in.readZlibImage());
            } else {
                if (in.readBool()) p.put(PGameSettings.BACK_LOAD_BAR, in.readZlibImage());
                if (in.readBool()) p.put(PGameSettings.FRONT_LOAD_BAR, in.readZlibImage());
            }
        }
        in.readBool(p, PGameSettings.SHOW_CUSTOM_LOAD_IMAGE);
        if (p.get(PGameSettings.SHOW_CUSTOM_LOAD_IMAGE)) {
            if (ver < 800) {
                if (in.read4() != -1) p.put(PGameSettings.LOADING_IMAGE, in.readZlibImage());
            } else if (in.readBool()) p.put(PGameSettings.LOADING_IMAGE, in.readZlibImage());
        }
        in.readBool(p, PGameSettings.IMAGE_PARTIALLY_TRANSPARENTY);
        in.read4(p, PGameSettings.LOAD_IMAGE_ALPHA);
        in.readBool(p, PGameSettings.SCALE_PROGRESS_BAR);
        int length = in.read4();
        byte[] data = new byte[length];
        in.read(data, 0, length);
        try {
            g.put(PGameSettings.GAME_ICON, new ICOFile(data));
        } catch (Exception e) {
            e.printStackTrace();
        }
        in.readBool(p, PGameSettings.DISPLAY_ERRORS, PGameSettings.WRITE_TO_LOG, PGameSettings.ABORT_ON_ERROR);
        int errors = in.read4();
        p.put(PGameSettings.TREAT_UNINIT_AS_0, ((errors & 0x01) != 0));
        p.put(PGameSettings.ERROR_ON_ARGS, ((errors & 0x02) != 0));
        in.readStr(p, PGameSettings.AUTHOR);
        if (ver > 600) in.readStr(p, PGameSettings.VERSION); else p.put(PGameSettings.VERSION, Integer.toString(in.read4()));
        in.readD(p, PGameSettings.LAST_CHANGED);
        in.readStr(p, PGameSettings.INFORMATION);
        if (ver < 800) {
            int no = in.read4();
            for (int i = 0; i < no; i++) {
                Constant con = new Constant();
                c.f.constants.add(con);
                con.name = in.readStr();
                con.value = in.readStr();
            }
        }
        if (ver > 600) {
            in.read4(p, PGameSettings.VERSION_MAJOR, PGameSettings.VERSION_MINOR, PGameSettings.VERSION_RELEASE, PGameSettings.VERSION_BUILD);
            in.readStr(p, PGameSettings.COMPANY, PGameSettings.PRODUCT, PGameSettings.COPYRIGHT, PGameSettings.DESCRIPTION);
            if (ver >= 800) in.skip(8);
        } else if (ver > 530) readSettingsIncludes(c.f, in);
        in.endInflate();
    }

    private static void readSettingsIncludes(GmFile f, GmStreamDecoder in) throws IOException {
        int no = in.read4();
        for (int i = 0; i < no; i++) {
            Include inc = new Include();
            f.includes.add(inc);
            inc.filepath = in.readStr();
            inc.filename = new File(inc.filepath).getName();
        }
        f.gameSettings.put(PGameSettings.INCLUDE_FOLDER, GmFile.GS_INCFOLDERS[in.read4()]);
        in.readBool(f.gameSettings.properties, PGameSettings.OVERWRITE_EXISTING, PGameSettings.REMOVE_AT_GAME_END);
        for (Include inc : f.includes) {
            inc.export = f.gameSettings.get(PGameSettings.INCLUDE_FOLDER) == IncludeFolder.TEMP ? 1 : 2;
            inc.overwriteExisting = f.gameSettings.get(PGameSettings.OVERWRITE_EXISTING);
            inc.removeAtGameEnd = f.gameSettings.get(PGameSettings.REMOVE_AT_GAME_END);
        }
    }

    private static void readTriggers(GmFileContext c) throws IOException, GmFormatException {
        GmFile f = c.f;
        GmStreamDecoder in = c.in;
        int ver = in.read4();
        if (ver != 800) throw versionError(f, "BEFORE", "SND", ver);
        int no = in.read4();
        for (int i = 0; i < no; i++) {
            in.beginInflate();
            if (!in.readBool()) {
                in.endInflate();
                continue;
            }
            ver = in.read4();
            if (ver != 800) throw versionError(f, "BEFORE", "SND", ver);
            Trigger trig = new Trigger();
            f.triggers.add(trig);
            trig.name = in.readStr();
            trig.condition = in.readStr();
            trig.checkStep = in.read4();
            trig.constant = in.readStr();
            in.endInflate();
        }
        in.skip(8);
    }

    private static void readConstants(GmFileContext c) throws IOException, GmFormatException {
        GmFile f = c.f;
        GmStreamDecoder in = c.in;
        int ver = in.read4();
        if (ver != 800) throw versionError(f, "BEFORE", "SND", ver);
        int no = in.read4();
        for (int i = 0; i < no; i++) {
            Constant con = new Constant();
            f.constants.add(con);
            con.name = in.readStr();
            con.value = in.readStr();
        }
        in.skip(8);
    }

    private static void readSounds(GmFileContext c) throws IOException, GmFormatException, DataFormatException {
        GmFile f = c.f;
        GmStreamDecoder in = c.in;
        int ver = in.read4();
        if (ver != 400 && ver != 800) throw versionError(f, "BEFORE", "SND", ver);
        int noSounds = in.read4();
        for (int i = 0; i < noSounds; i++) {
            if (ver == 800) in.beginInflate();
            if (!in.readBool()) {
                f.resMap.getList(Sound.class).lastId++;
                in.endInflate();
                continue;
            }
            Sound snd = f.resMap.getList(Sound.class).add();
            snd.setName(in.readStr());
            if (ver == 800) in.skip(8);
            ver = in.read4();
            if (ver != 440 && ver != 600 && ver != 800) throw versionError(f, "IN", "SND", i, ver);
            int kind53 = -1;
            if (ver == 440) kind53 = in.read4(); else snd.put(PSound.KIND, GmFile.SOUND_KIND[in.read4()]);
            in.readStr(snd.properties, PSound.FILE_TYPE);
            if (ver == 440) {
                if (kind53 != -1) snd.data = in.decompress(in.read4());
                in.skip(8);
                snd.put(PSound.PRELOAD, !in.readBool());
            } else {
                snd.put(PSound.FILE_NAME, in.readStr());
                if (in.readBool()) {
                    if (ver == 600) snd.data = in.decompress(in.read4()); else {
                        int s = in.read4();
                        snd.data = new byte[s];
                        in.read(snd.data);
                    }
                }
                int effects = in.read4();
                for (PSound k : GmFile.SOUND_FX_FLAGS) {
                    snd.put(k, (effects & 1) != 0);
                    effects >>= 1;
                }
                in.readD(snd.properties, PSound.VOLUME, PSound.PAN);
                snd.put(PSound.PRELOAD, in.readBool());
            }
            in.endInflate();
        }
    }

    private static void readSprites(GmFileContext c) throws IOException, GmFormatException, DataFormatException {
        GmFile f = c.f;
        GmStreamDecoder in = c.in;
        int ver = in.read4();
        if (ver != 400 && ver != 800) throw versionError(f, "BEFORE", "SPR", ver);
        int noSprites = in.read4();
        for (int i = 0; i < noSprites; i++) {
            if (ver == 800) in.beginInflate();
            if (!in.readBool()) {
                f.resMap.getList(Sprite.class).lastId++;
                in.endInflate();
                continue;
            }
            Sprite spr = f.resMap.getList(Sprite.class).add();
            spr.put(PSprite.BB_MODE, BBMode.MANUAL);
            BBMode actualBBMode = null;
            spr.setName(in.readStr());
            if (ver == 800) in.skip(8);
            ver = in.read4();
            if (ver != 400 && ver != 542 && ver != 800) throw versionError(f, "IN", "SPR", i, ver);
            int w = 0, h = 0;
            if (ver < 800) {
                w = in.read4();
                h = in.read4();
                in.read4(spr.properties, PSprite.BB_LEFT, PSprite.BB_RIGHT, PSprite.BB_BOTTOM, PSprite.BB_TOP);
                spr.put(PSprite.TRANSPARENT, in.readBool());
                if (ver > 400) {
                    in.readBool(spr.properties, PSprite.SMOOTH_EDGES, PSprite.PRELOAD);
                }
                actualBBMode = GmFile.SPRITE_BB_MODE[in.read4()];
                boolean precise = in.readBool();
                spr.put(PSprite.SHAPE, precise ? Sprite.MaskShape.PRECISE : Sprite.MaskShape.RECTANGLE);
                if (ver == 400) {
                    in.skip(4);
                    spr.put(PSprite.PRELOAD, !in.readBool());
                }
            } else spr.put(PSprite.TRANSPARENT, false);
            in.read4(spr.properties, PSprite.ORIGIN_X, PSprite.ORIGIN_Y);
            int nosub = in.read4();
            for (int j = 0; j < nosub; j++) {
                if (ver >= 800) {
                    int subver = in.read4();
                    if (subver != 800) throw versionError(f, "IN", "SPR", i, subver);
                    w = in.read4();
                    h = in.read4();
                    if (w != 0 && h != 0) spr.subImages.add(in.readBGRAImage(w, h));
                } else {
                    if (in.read4() == -1) continue;
                    spr.subImages.add(in.readZlibImage(w, h));
                }
            }
            if (ver >= 800) {
                spr.put(PSprite.SHAPE, GmFile.SPRITE_MASK_SHAPE[in.read4()]);
                spr.put(PSprite.ALPHA_TOLERANCE, in.read4());
                spr.put(PSprite.SEPARATE_MASK, in.readBool());
                actualBBMode = GmFile.SPRITE_BB_MODE[in.read4()];
                in.read4(spr.properties, PSprite.BB_LEFT, PSprite.BB_RIGHT, PSprite.BB_BOTTOM, PSprite.BB_TOP);
            }
            spr.put(PSprite.BB_MODE, actualBBMode);
            in.endInflate();
        }
    }

    private static void readBackgrounds(GmFileContext c) throws IOException, GmFormatException, DataFormatException {
        GmFile f = c.f;
        GmStreamDecoder in = c.in;
        int ver = in.read4();
        if (ver != 400 && ver != 800) throw versionError(f, "BEFORE", "BKG", ver);
        int noBackgrounds = in.read4();
        for (int i = 0; i < noBackgrounds; i++) {
            if (ver == 800) in.beginInflate();
            if (!in.readBool()) {
                f.resMap.getList(Background.class).lastId++;
                in.endInflate();
                continue;
            }
            Background back = f.resMap.getList(Background.class).add();
            back.setName(in.readStr());
            if (ver == 800) in.skip(8);
            ver = in.read4();
            if (ver != 400 && ver != 543 && ver != 710) throw versionError(f, "IN", "BKG", i, ver);
            if (ver < 710) {
                int w = in.read4();
                int h = in.read4();
                back.put(PBackground.TRANSPARENT, in.readBool());
                if (ver > 400) {
                    in.readBool(back.properties, PBackground.SMOOTH_EDGES, PBackground.PRELOAD, PBackground.USE_AS_TILESET);
                    in.read4(back.properties, PBackground.TILE_WIDTH, PBackground.TILE_HEIGHT, PBackground.H_OFFSET, PBackground.V_OFFSET, PBackground.H_SEP, PBackground.V_SEP);
                } else {
                    in.skip(4);
                    back.put(PBackground.PRELOAD, !in.readBool());
                }
                if (in.readBool()) {
                    if (in.read4() == -1) continue;
                    back.setBackgroundImage(in.readZlibImage(w, h));
                }
            } else {
                back.put(PBackground.USE_AS_TILESET, in.readBool());
                in.read4(back.properties, PBackground.TILE_WIDTH, PBackground.TILE_HEIGHT, PBackground.H_OFFSET, PBackground.V_OFFSET, PBackground.H_SEP, PBackground.V_SEP);
                ver = in.read4();
                if (ver != 800) throw versionError(f, "IN", "BKG", i, ver);
                int w = in.read4();
                int h = in.read4();
                if (w != 0 && h != 0) back.setBackgroundImage(in.readBGRAImage(w, h));
            }
            in.endInflate();
        }
    }

    private static void readPaths(GmFileContext c) throws IOException, GmFormatException {
        GmFile f = c.f;
        GmStreamDecoder in = c.in;
        int ver = in.read4();
        if (ver != 420 && ver != 800) throw versionError(f, "BEFORE", "PTH", ver);
        int noPaths = in.read4();
        for (int i = 0; i < noPaths; i++) {
            if (ver == 800) in.beginInflate();
            if (!in.readBool()) {
                f.resMap.getList(Path.class).lastId++;
                in.endInflate();
                continue;
            }
            Path path = f.resMap.getList(Path.class).add();
            path.setName(in.readStr());
            if (ver == 800) in.skip(8);
            int ver2 = in.read4();
            if (ver2 != 530) throw versionError(f, "IN", "PTH", i, ver2);
            in.readBool(path.properties, PPath.SMOOTH, PPath.CLOSED);
            path.put(PPath.PRECISION, in.read4());
            path.put(PPath.BACKGROUND_ROOM, c.rmids.get(in.read4()));
            in.read4(path.properties, PPath.SNAP_X, PPath.SNAP_Y);
            int nopoints = in.read4();
            for (int j = 0; j < nopoints; j++) {
                path.points.add(new PathPoint((int) in.readD(), (int) in.readD(), (int) in.readD()));
            }
            in.endInflate();
        }
    }

    private static void readScripts(GmFileContext c) throws IOException, GmFormatException {
        GmFile f = c.f;
        GmStreamDecoder in = c.in;
        int ver = in.read4();
        if (ver != 400 && ver != 800) throw versionError(f, "BEFORE", "SCR", ver);
        int noScripts = in.read4();
        for (int i = 0; i < noScripts; i++) {
            if (ver == 800) in.beginInflate();
            if (!in.readBool()) {
                f.resMap.getList(Script.class).lastId++;
                in.endInflate();
                continue;
            }
            Script scr = f.resMap.getList(Script.class).add();
            scr.setName(in.readStr());
            if (ver == 800) in.skip(8);
            ver = in.read4();
            if (ver != 400 && ver != 800) throw versionError(f, "IN", "SCR", i, ver);
            String code = in.readStr();
            scr.put(PScript.CODE, code);
            in.endInflate();
        }
    }

    private static void readFonts(GmFileContext c) throws IOException, GmFormatException {
        GmFile f = c.f;
        GmStreamDecoder in = c.in;
        int ver = in.read4();
        if (ver != 440 && ver != 540 && ver != 800) throw versionError(f, "BEFORE", "FNT", (int) in.getPos());
        if (ver == 440) {
            int noDataFiles = in.read4();
            for (int i = 0; i < noDataFiles; i++) {
                if (!in.readBool()) continue;
                in.skip(in.read4());
                if (in.read4() != 440) throw new GmFormatException(f, Messages.format("GmFileReader.ERROR_UNSUPPORTED", Messages.getString("GmFileReader.INDATAFILES"), ver));
                Include inc = new Include();
                f.includes.add(inc);
                inc.filepath = in.readStr();
                inc.filename = new File(inc.filepath).getName();
                if (in.readBool()) {
                    inc.size = in.read4();
                    inc.data = new byte[inc.size];
                    in.read(inc.data, 0, inc.size);
                }
                inc.export = in.read4();
                inc.overwriteExisting = in.readBool();
                inc.freeMemAfterExport = in.readBool();
                inc.removeAtGameEnd = in.readBool();
            }
            return;
        }
        int noFonts = in.read4();
        for (int i = 0; i < noFonts; i++) {
            if (ver == 800) in.beginInflate();
            if (!in.readBool()) {
                f.resMap.getList(Font.class).lastId++;
                in.endInflate();
                continue;
            }
            Font font = f.resMap.getList(Font.class).add();
            font.setName(in.readStr());
            if (ver == 800) in.skip(8);
            ver = in.read4();
            if (ver != 540 && ver != 800) throw versionError(f, "IN", "FNT", i, ver);
            font.put(PFont.FONT_NAME, in.readStr());
            font.put(PFont.SIZE, in.read4());
            in.readBool(font.properties, PFont.BOLD, PFont.ITALIC);
            font.put(PFont.RANGE_MIN, in.read2());
            font.put(PFont.CHARSET, in.read());
            int aa = in.read();
            if (aa == 0 && f.format != GmFile.FormatFlavor.GM_810) aa = 3;
            font.put(PFont.ANTIALIAS, aa);
            font.put(PFont.RANGE_MAX, in.read4());
            in.endInflate();
        }
    }

    private static void readTimelines(GmFileContext c) throws IOException, GmFormatException {
        GmFile f = c.f;
        GmStreamDecoder in = c.in;
        int ver = in.read4();
        if (ver != 500 && ver != 800) throw versionError(f, "BEFORE", "TML", ver);
        int noTimelines = in.read4();
        for (int i = 0; i < noTimelines; i++) {
            if (ver == 800) in.beginInflate();
            if (!in.readBool()) {
                in.endInflate();
                continue;
            }
            ResourceReference<Timeline> r = c.timeids.get(i);
            Timeline time = r.get();
            f.resMap.getList(Timeline.class).add(time);
            time.setName(in.readStr());
            if (ver == 800) in.skip(8);
            int ver2 = in.read4();
            if (ver2 != 500) throw versionError(f, "IN", "TML", i, ver2);
            int nomoms = in.read4();
            for (int j = 0; j < nomoms; j++) {
                Moment mom = time.addMoment();
                mom.stepNo = in.read4();
                GmFileContext fc = c.copy();
                fc.in = in;
                readActions(fc, mom, "INTIMELINEACTION", i, mom.stepNo);
            }
            in.endInflate();
        }
        f.resMap.getList(Timeline.class).lastId = noTimelines - 1;
    }

    private static void readGmObjects(GmFileContext c) throws IOException, GmFormatException {
        GmFile f = c.f;
        GmStreamDecoder in = c.in;
        int ver = in.read4();
        if (ver != 400 && ver != 800) throw versionError(f, "BEFORE", "OBJ", ver);
        int noGmObjects = in.read4();
        for (int i = 0; i < noGmObjects; i++) {
            if (ver == 800) in.beginInflate();
            if (!in.readBool()) {
                in.endInflate();
                continue;
            }
            ResourceReference<GmObject> r = c.objids.get(i);
            GmObject obj = r.get();
            f.resMap.getList(GmObject.class).add(obj);
            obj.setName(in.readStr());
            if (ver == 800) in.skip(8);
            int ver2 = in.read4();
            if (ver2 != 430) throw versionError(f, "IN", "OBJ", i, ver2);
            Sprite temp = f.resMap.getList(Sprite.class).getUnsafe(in.read4());
            if (temp != null) obj.put(PGmObject.SPRITE, temp.reference);
            in.readBool(obj.properties, PGmObject.SOLID, PGmObject.VISIBLE);
            obj.put(PGmObject.DEPTH, in.read4());
            obj.put(PGmObject.PERSISTENT, in.readBool());
            obj.put(PGmObject.PARENT, c.objids.get(in.read4()));
            temp = f.resMap.getList(Sprite.class).getUnsafe(in.read4());
            if (temp != null) obj.put(PGmObject.MASK, temp.reference);
            int noEvents = in.read4() + 1;
            for (int j = 0; j < noEvents; j++) {
                MainEvent me = obj.mainEvents.get(j);
                boolean done = false;
                while (!done) {
                    int first = in.read4();
                    if (first != -1) {
                        Event ev = new Event();
                        me.events.add(0, ev);
                        if (j == MainEvent.EV_COLLISION) ev.other = c.objids.get(first); else ev.id = first;
                        ev.mainId = j;
                        GmFileContext fc = c.copy();
                        fc.in = in;
                        readActions(fc, ev, "INOBJECTACTION", i, j * 1000 + ev.id);
                    } else done = true;
                }
            }
            in.endInflate();
        }
        f.resMap.getList(GmObject.class).lastId = noGmObjects - 1;
    }

    private static void readRooms(GmFileContext c) throws IOException, GmFormatException {
        GmFile f = c.f;
        GmStreamDecoder in = c.in;
        int ver = in.read4();
        if (ver != 420 && ver != 800) throw versionError(f, "BEFORE", "RMM", ver);
        int noRooms = in.read4();
        for (int i = 0; i < noRooms; i++) {
            if (ver == 800) in.beginInflate();
            if (!in.readBool()) {
                in.endInflate();
                continue;
            }
            ResourceReference<Room> r = c.rmids.get(i);
            Room rm = r.get();
            f.resMap.getList(Room.class).add(rm);
            rm.setName(in.readStr());
            if (ver == 800) in.skip(8);
            int ver2 = in.read4();
            if (ver2 != 520 && ver2 != 541) throw versionError(f, "IN", "RMM", i, ver2);
            rm.put(PRoom.CAPTION, in.readStr());
            in.read4(rm.properties, PRoom.WIDTH, PRoom.HEIGHT, PRoom.SNAP_Y, PRoom.SNAP_X);
            rm.put(PRoom.ISOMETRIC, in.readBool());
            rm.put(PRoom.SPEED, in.read4());
            rm.put(PRoom.PERSISTENT, in.readBool());
            rm.put(PRoom.BACKGROUND_COLOR, Util.convertGmColor(in.read4()));
            rm.put(PRoom.DRAW_BACKGROUND_COLOR, in.readBool());
            rm.put(PRoom.CREATION_CODE, in.readStr());
            int nobackgrounds = in.read4();
            for (int j = 0; j < nobackgrounds; j++) {
                BackgroundDef bk = rm.backgroundDefs.get(j);
                in.readBool(bk.properties, PBackgroundDef.VISIBLE, PBackgroundDef.FOREGROUND);
                Background temp = f.resMap.getList(Background.class).getUnsafe(in.read4());
                if (temp != null) bk.properties.put(PBackgroundDef.BACKGROUND, temp.reference);
                in.read4(bk.properties, PBackgroundDef.X, PBackgroundDef.Y);
                in.readBool(bk.properties, PBackgroundDef.TILE_HORIZ, PBackgroundDef.TILE_VERT);
                in.read4(bk.properties, PBackgroundDef.H_SPEED, PBackgroundDef.V_SPEED);
                bk.properties.put(PBackgroundDef.STRETCH, in.readBool());
            }
            rm.put(PRoom.ENABLE_VIEWS, in.readBool());
            int noviews = in.read4();
            for (int j = 0; j < noviews; j++) {
                View vw = rm.views.get(j);
                in.readBool(vw.properties, PView.VISIBLE);
                in.read4(vw.properties, PView.VIEW_X, PView.VIEW_Y, PView.VIEW_W, PView.VIEW_H, PView.PORT_X, PView.PORT_Y);
                if (ver2 > 520) in.read4(vw.properties, PView.PORT_W, PView.PORT_H);
                in.read4(vw.properties, PView.BORDER_H, PView.BORDER_V, PView.SPEED_H, PView.SPEED_V);
                GmObject temp = f.resMap.getList(GmObject.class).getUnsafe(in.read4());
                if (temp != null) vw.properties.put(PView.OBJECT, temp.reference);
            }
            int noinstances = in.read4();
            for (int j = 0; j < noinstances; j++) {
                Instance inst = rm.addInstance();
                inst.setPosition(new Point(in.read4(), in.read4()));
                GmObject temp = f.resMap.getList(GmObject.class).getUnsafe(in.read4());
                if (temp != null) inst.properties.put(PInstance.OBJECT, temp.reference);
                inst.properties.put(PInstance.ID, in.read4());
                inst.setCreationCode(in.readStr());
                inst.setLocked(in.readBool());
            }
            int notiles = in.read4();
            for (int j = 0; j < notiles; j++) {
                Tile t = new Tile(rm);
                t.setRoomPosition(new Point(in.read4(), in.read4()));
                Background temp = f.resMap.getList(Background.class).getUnsafe(in.read4());
                ResourceReference<Background> bkg = null;
                if (temp != null) bkg = temp.reference;
                t.properties.put(PTile.BACKGROUND, bkg);
                t.setBackgroundPosition(new Point(in.read4(), in.read4()));
                t.setSize(new Dimension(in.read4(), in.read4()));
                t.setDepth(in.read4());
                t.properties.put(PTile.ID, in.read4());
                t.setLocked(in.readBool());
                rm.tiles.add(t);
            }
            rm.put(PRoom.REMEMBER_WINDOW_SIZE, in.readBool());
            in.read4(rm.properties, PRoom.EDITOR_WIDTH, PRoom.EDITOR_HEIGHT);
            in.readBool(rm.properties, PRoom.SHOW_GRID, PRoom.SHOW_OBJECTS, PRoom.SHOW_TILES, PRoom.SHOW_BACKGROUNDS, PRoom.SHOW_FOREGROUNDS, PRoom.SHOW_VIEWS, PRoom.DELETE_UNDERLYING_OBJECTS, PRoom.DELETE_UNDERLYING_TILES);
            if (ver2 == 520) in.skip(6 * 4);
            in.read4(rm.properties, PRoom.CURRENT_TAB, PRoom.SCROLL_BAR_X, PRoom.SCROLL_BAR_Y);
            in.endInflate();
        }
        f.resMap.getList(Room.class).lastId = noRooms - 1;
    }

    private static void readIncludedFiles(GmFileContext c) throws IOException, GmFormatException {
        GmFile f = c.f;
        GmStreamDecoder in = c.in;
        int ver = in.read4();
        if (ver != 430 && ver != 600 && ver != 620 && ver != 800) throw versionError(f, "BEFORE", "GMI", ver);
        int noIncludes = in.read4();
        for (int i = 0; i < noIncludes; i++) {
            if (ver == 800) {
                in.beginInflate();
                in.skip(8);
            }
            ver = in.read4();
            if (ver != 620 && ver != 800) throw new GmFormatException(f, Messages.format("GmFileReader.ERROR_UNSUPPORTED", Messages.getString("GmFileReader.ININCLUDEDFILES"), ver));
            Include inc = new Include();
            f.includes.add(inc);
            inc.filename = in.readStr();
            inc.filepath = in.readStr();
            inc.isOriginal = in.readBool();
            inc.size = in.read4();
            if (in.readBool()) {
                int s = in.read4();
                inc.data = new byte[s];
                in.read(inc.data, 0, s);
            }
            inc.export = in.read4();
            inc.exportFolder = in.readStr();
            inc.overwriteExisting = in.readBool();
            inc.freeMemAfterExport = in.readBool();
            inc.removeAtGameEnd = in.readBool();
            in.endInflate();
        }
    }

    private static void readPackages(GmFileContext c) throws IOException, GmFormatException {
        GmFile f = c.f;
        GmStreamDecoder in = c.in;
        int ver = in.read4();
        if (ver != 700) throw versionError(f, "BEFORE", "EXT", ver);
        int noPackages = in.read4();
        for (int i = 0; i < noPackages; i++) f.packages.add(in.readStr());
    }

    private static void readGameInformation(GmFileContext c) throws IOException, GmFormatException {
        GmStreamDecoder in = c.in;
        GameInformation gameInfo = c.f.gameInfo;
        PropertyMap<PGameInformation> p = gameInfo.properties;
        int ver = in.read4();
        if (ver != 430 && ver != 600 && ver != 620 && ver != 800) throw versionError(c.f, "BEFORE", "GMI", ver);
        if (ver == 800) in.beginInflate();
        int bc = in.read4();
        if (bc >= 0) p.put(PGameInformation.BACKGROUND_COLOR, Util.convertGmColor(bc));
        if (ver < 800) in.readBool(p, PGameInformation.MIMIC_GAME_WINDOW); else p.put(PGameInformation.MIMIC_GAME_WINDOW, !in.readBool());
        if (ver > 430) {
            in.readStr(p, PGameInformation.FORM_CAPTION);
            in.read4(p, PGameInformation.LEFT, PGameInformation.TOP, PGameInformation.WIDTH, PGameInformation.HEIGHT);
            in.readBool(p, PGameInformation.SHOW_BORDER, PGameInformation.ALLOW_RESIZE, PGameInformation.STAY_ON_TOP, PGameInformation.PAUSE_GAME);
        }
        if (ver == 800) in.skip(8);
        in.readStr(p, PGameInformation.TEXT);
        in.endInflate();
    }

    private static void readTree(GmFileContext c, ResNode root, int ver) throws IOException {
        GmFile f = c.f;
        GmStreamDecoder in = c.in;
        Stack<ResNode> path = new Stack<ResNode>();
        Stack<Integer> left = new Stack<Integer>();
        path.push(root);
        int rootnodes = (ver > 540) ? 12 : 11;
        while (rootnodes-- > 0) {
            byte status = (byte) in.read4();
            Class<?> type = GmFile.RESOURCE_KIND[(byte) in.read4()];
            int ind = in.read4();
            String name = in.readStr();
            boolean hasRef;
            if (status == ResNode.STATUS_SECONDARY) hasRef = type == Font.class ? ver != 500 : (type == null ? false : InstantiableResource.class.isAssignableFrom(type)); else hasRef = false;
            ResourceList<?> rl = hasRef ? (ResourceList<?>) f.resMap.get(type) : null;
            ResNode node = new ResNode(name, status, type, hasRef ? rl.getUnsafe(ind).reference : null);
            if (ver == 500 && status == ResNode.STATUS_PRIMARY && type == Font.class) path.peek().addChild(Messages.getString("LGM.FNT"), status, type); else path.peek().add(node);
            int contents = in.read4();
            if (contents > 0) {
                left.push(new Integer(rootnodes));
                rootnodes = contents;
                path.push(node);
            }
            while (rootnodes == 0 && !left.isEmpty()) {
                rootnodes = left.pop().intValue();
                path.pop();
            }
        }
        if (ver <= 540) root.addChild(Messages.getString("LGM.EXT"), ResNode.STATUS_SECONDARY, Extensions.class);
    }

    private static void readActions(GmFileContext c, ActionContainer container, String errorKey, int format1, int format2) throws IOException, GmFormatException {
        final GmFile f = c.f;
        GmStreamDecoder in = c.in;
        int ver = in.read4();
        if (ver != 400) {
            throw new GmFormatException(f, Messages.format("GmFileReader.ERROR_UNSUPPORTED", Messages.format("GmFileReader." + errorKey, format1, format2), ver));
        }
        int noacts = in.read4();
        for (int k = 0; k < noacts; k++) {
            in.skip(4);
            int libid = in.read4();
            int actid = in.read4();
            LibAction la = LibManager.getLibAction(libid, actid);
            boolean unknownLib = la == null;
            if (unknownLib) {
                la = new LibAction();
                la.id = actid;
                la.parentId = libid;
                la.actionKind = (byte) in.read4();
                la.allowRelative = in.readBool();
                la.question = in.readBool();
                la.canApplyTo = in.readBool();
                la.execType = (byte) in.read4();
                if (la.execType == Action.EXEC_FUNCTION) la.execInfo = in.readStr(); else in.skip(in.read4());
                if (la.execType == Action.EXEC_CODE) la.execInfo = in.readStr(); else in.skip(in.read4());
            } else {
                in.skip(20);
                in.skip(in.read4());
                in.skip(in.read4());
            }
            Argument[] args = new Argument[in.read4()];
            byte[] argkinds = new byte[in.read4()];
            for (int x = 0; x < argkinds.length; x++) argkinds[x] = (byte) in.read4();
            if (unknownLib) {
                la.libArguments = new LibArgument[argkinds.length];
                for (int x = 0; x < argkinds.length; x++) {
                    la.libArguments[x] = new LibArgument();
                    la.libArguments[x].kind = argkinds[x];
                }
            }
            Action act = container.addAction(la);
            int appliesTo = in.read4();
            switch(appliesTo) {
                case -1:
                    act.setAppliesTo(GmObject.OBJECT_SELF);
                    break;
                case -2:
                    act.setAppliesTo(GmObject.OBJECT_OTHER);
                    break;
                default:
                    act.setAppliesTo(c.objids.get(appliesTo));
            }
            act.setRelative(in.readBool());
            int actualnoargs = in.read4();
            for (int l = 0; l < actualnoargs; l++) {
                if (l >= args.length) {
                    in.skip(in.read4());
                    continue;
                }
                args[l] = new Argument(argkinds[l]);
                String strval = in.readStr();
                args[l].setVal(strval);
                Class<? extends Resource<?, ?>> kind = Argument.getResourceKind(argkinds[l]);
                if (kind != null && Resource.class.isAssignableFrom(kind)) try {
                    final int id = Integer.parseInt(strval);
                    final Argument arg = args[l];
                    PostponedRef pr = new PostponedRef() {

                        public boolean invoke() {
                            ResourceHolder<?> rh = f.resMap.get(Argument.getResourceKind(arg.kind));
                            Resource<?, ?> temp = null;
                            if (rh instanceof ResourceList<?>) temp = ((ResourceList<?>) rh).getUnsafe(id); else temp = rh.getResource();
                            if (temp != null) arg.setRes(temp.reference);
                            return temp != null;
                        }
                    };
                    if (!pr.invoke()) postpone.add(pr);
                } catch (NumberFormatException e) {
                }
                act.setArguments(args);
            }
            act.setNot(in.readBool());
        }
    }
}
