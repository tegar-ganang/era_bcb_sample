package org.activision.util.rs2cache;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.activision.util.CacheConstants;

public class ItemDefinitions {

    private static HashMap<Integer, ItemDefinitions> itemsDefs = new HashMap<Integer, ItemDefinitions>();

    private int id;

    public int equipId = -1;

    int maleEquip2;

    public int getId() {
        return id;
    }

    public boolean isStackable() {
        return stackable == 1;
    }

    public boolean isNoted() {
        return noted;
    }

    private boolean noted;

    private int anInt2008;

    private int anInt2009;

    int certID;

    private int anInt2011 = -1;

    String[] groundActions;

    private int anInt2014;

    private short[] originalModelColors;

    private int anInt2016;

    int anInt2017;

    int[] anIntArray2018;

    private int modelID;

    int anInt2024;

    private int anInt2025;

    int anInt2026;

    private int femaleEquip1;

    private int anInt2028 = -1;

    String[] actions;

    int anInt2030;

    private int anInt2031;

    int modelZoom;

    int modelOffset2;

    private int anInt2035;

    int[] stackAmounts;

    int[] stackIDs;

    int modelOffset1;

    int lendID;

    int modelRotation1;

    private int anInt2043;

    private int femaleEquip2;

    int maleEquip1;

    private byte[] aByteArray2047;

    int anInt2049;

    private int anInt2050;

    private int anInt2051;

    public String name;

    boolean membersObject;

    int anInt2054;

    int anInt2055;

    private int anInt2056;

    int stackable;

    int anInt2059;

    private short[] aShortArray2060;

    static int anInt2061;

    int lendTemplateID;

    int anInt2063;

    int value;

    static int anInt2065;

    private int anInt2067;

    private int anInt2071;

    int modelRotation2;

    private short[] aShortArray2075;

    int anInt2077;

    private short[] modifiedModelColors;

    int certTemplateID;

    private int anInt2080;

    int team;

    Class aClass14_2082;

    Class aClass82_2083;

    private int anInt2084;

    boolean aBoolean2086;

    int anInt2089;

    int[] opcodes2;

    Object[] values2;

    public int renderEmote;

    public int questId;

    public List<Integer> skillRequirimentId;

    public List<Integer> skillRequirimentLvl;

    public void loadExtraInformation() {
        if (values2 == null || opcodes2 == null) return;
        int renderEmote = 0;
        int questId = 0;
        List<Integer> skillRequirimentLevelNoId = null;
        List<Integer> skillRequirimentId = null;
        List<Integer> skillRequirimentLvl = null;
        boolean isLvl = false;
        for (int index = 0; index < opcodes2.length; index++) {
            if (values2[index] instanceof Integer) {
                int opcode = opcodes2[index];
                int value = (Integer) values2[index];
                if (opcode == 23) {
                    if (skillRequirimentLevelNoId == null) skillRequirimentLevelNoId = new ArrayList<Integer>();
                    skillRequirimentLevelNoId.add(value);
                }
                if (opcode == 644) renderEmote = value; else if (opcode == 743) questId = value; else if (opcode >= 749) {
                    if (skillRequirimentId == null) {
                        skillRequirimentId = new ArrayList<Integer>();
                        skillRequirimentLvl = new ArrayList<Integer>();
                    }
                    if (isLvl) skillRequirimentLvl.add(value); else skillRequirimentId.add(value);
                    isLvl = !isLvl;
                }
            }
        }
        this.renderEmote = renderEmote;
        this.questId = questId;
        this.skillRequirimentId = skillRequirimentId;
        this.skillRequirimentLvl = skillRequirimentLvl;
        opcodes2 = null;
        values2 = null;
    }

    private static int getContainerId(int id) {
        return id >>> 8;
    }

    public static ItemDefinitions forID(int id) {
        ItemDefinitions itemDef = itemsDefs.get(id);
        if (itemDef != null) return itemDef;
        byte[] is = null;
        try {
            is = (CacheManager.getData(CacheConstants.ITEMDEF_IDX_ID, getContainerId(id), 0xff & id));
        } catch (Exception e) {
            System.out.println("Could not grab item " + id);
            return null;
        }
        itemDef = new ItemDefinitions();
        itemDef.groundActions = new String[] { null, null, "take", null, null };
        itemDef.actions = new String[] { null, null, null, null, "drop" };
        itemDef.id = id;
        if (is != null) {
            try {
                itemDef.getOpCodeLoop(new RSInputStream(new ByteArrayInputStream(is)), is.length);
            } catch (IOException e) {
                System.out.println("Could not load item " + id);
                return null;
            }
        }
        if (itemDef.certTemplateID != -1) itemDef.toNote(forID(itemDef.certTemplateID), forID(itemDef.certID));
        if (itemDef.lendTemplateID != -1) itemDef.toLend(forID(itemDef.lendTemplateID), forID(itemDef.lendID));
        itemDef.loadItemPart2();
        itemDef.loadExtraInformation();
        itemsDefs.put(id, itemDef);
        return itemDef;
    }

    final void toNote(ItemDefinitions class138_6_, ItemDefinitions class138_7_) {
        aShortArray2075 = class138_6_.aShortArray2075;
        (this).modelRotation2 = (class138_6_).modelRotation2;
        (this).modelOffset2 = (class138_6_).modelOffset2;
        (this).modelOffset1 = (class138_6_).modelOffset1;
        (this).membersObject = (class138_7_).membersObject;
        (this).modelZoom = (class138_6_).modelZoom;
        modifiedModelColors = class138_6_.modifiedModelColors;
        (this).value = (class138_7_).value;
        aShortArray2060 = class138_6_.aShortArray2060;
        modelID = class138_6_.modelID;
        (this).name = (class138_7_).name;
        (this).stackable = 1;
        (this).anInt2026 = (class138_6_).anInt2026;
        aByteArray2047 = class138_6_.aByteArray2047;
        (this).modelRotation1 = (class138_6_).modelRotation1;
        originalModelColors = class138_6_.originalModelColors;
        noted = true;
    }

    final void toLend(ItemDefinitions class138_32_, ItemDefinitions class138_31_) {
        anInt2067 = class138_31_.anInt2067;
        originalModelColors = class138_31_.originalModelColors;
        anInt2011 = class138_31_.anInt2011;
        anInt2080 = class138_31_.anInt2080;
        (this).maleEquip2 = (class138_31_).maleEquip2;
        (this).aClass82_2083 = (class138_31_).aClass82_2083;
        anInt2025 = class138_31_.anInt2025;
        (this).actions = new String[5];
        anInt2043 = class138_31_.anInt2043;
        (this).modelOffset1 = (class138_32_).modelOffset1;
        (this).modelRotation2 = (class138_32_).modelRotation2;
        anInt2031 = class138_31_.anInt2031;
        (this).groundActions = (class138_31_).groundActions;
        anInt2016 = class138_31_.anInt2016;
        (this).maleEquip1 = (class138_31_).maleEquip1;
        (this).modelOffset2 = (class138_32_).modelOffset2;
        (this).name = (class138_31_).name;
        anInt2071 = class138_31_.anInt2071;
        (this).modelZoom = (class138_32_).modelZoom;
        aShortArray2075 = class138_31_.aShortArray2075;
        modelID = class138_32_.modelID;
        aByteArray2047 = class138_31_.aByteArray2047;
        femaleEquip1 = class138_31_.femaleEquip1;
        (this).value = 0;
        anInt2035 = class138_31_.anInt2035;
        aShortArray2060 = class138_31_.aShortArray2060;
        (this).membersObject = (class138_31_).membersObject;
        anInt2028 = class138_31_.anInt2028;
        (this).modelRotation1 = (class138_32_).modelRotation1;
        (this).anInt2026 = (class138_32_).anInt2026;
        femaleEquip2 = class138_31_.femaleEquip2;
        anInt2008 = class138_31_.anInt2008;
        modifiedModelColors = class138_31_.modifiedModelColors;
        (this).team = (class138_31_).team;
        anInt2084 = class138_31_.anInt2084;
        if ((class138_31_).actions != null) {
            for (int i_33_ = 0; i_33_ < 4; i_33_++) (this).actions[i_33_] = (class138_31_).actions[i_33_];
        }
        (this).actions[4] = "Discard";
    }

    private ItemDefinitions() {
        bonus = new int[15];
        anInt2025 = -1;
        anInt2043 = -1;
        anInt2026 = 0;
        maleEquip1 = -1;
        modelOffset2 = 0;
        anInt2030 = -1;
        femaleEquip1 = -1;
        anInt2031 = 0;
        modelZoom = 2000;
        lendID = -1;
        anInt2008 = -1;
        anInt2009 = 128;
        anInt2055 = -1;
        anInt2051 = 0;
        name = "null";
        modelOffset1 = 0;
        modelRotation1 = 0;
        anInt2049 = -1;
        anInt2056 = 128;
        stackable = 0;
        membersObject = false;
        anInt2054 = -1;
        value = 1;
        maleEquip2 = -1;
        anInt2050 = 128;
        femaleEquip2 = -1;
        anInt2035 = 0;
        anInt2063 = -1;
        certID = -1;
        lendTemplateID = -1;
        anInt2059 = -1;
        anInt2014 = 0;
        anInt2017 = -1;
        anInt2077 = -1;
        anInt2016 = 0;
        anInt2080 = 0;
        certTemplateID = -1;
        modelRotation2 = 0;
        anInt2084 = 0;
        aBoolean2086 = false;
        anInt2067 = -1;
        anInt2071 = 0;
        team = 0;
        anInt2089 = 0;
    }

    final void getOpCodeLoop(RSInputStream stream, int length) throws IOException {
        for (; ; ) {
            int opcode = stream.readUnsignedByte();
            if (opcode == 0) break;
            readValues(stream, opcode);
        }
    }

    private final void readValues(RSInputStream stream, int opcode) throws IOException {
        if (opcode != 1) {
            if (opcode != 2) {
                if (opcode == 4) modelZoom = stream.readUnsignedShort(); else if (opcode != 5) {
                    if (opcode != 6) {
                        if (opcode == 7) {
                            modelOffset1 = stream.readUnsignedShort();
                            if (modelOffset1 > 32767) modelOffset1 -= 65536;
                            modelOffset1 <<= 0;
                        } else if (opcode != 8) {
                            if (opcode == 11) stackable = 1; else if (opcode == 12) value = stream.readInt(); else if (opcode == 16) membersObject = true; else if (opcode == 23) maleEquip1 = stream.readUnsignedShort(); else if (opcode != 24) {
                                if (opcode == 25) maleEquip2 = stream.readUnsignedShort(); else if (opcode != 26) {
                                    if (opcode < 30 || opcode >= 35) {
                                        if (opcode < 35 || opcode >= 40) {
                                            if (opcode == 40) {
                                                int j = (stream.readUnsignedByte());
                                                originalModelColors = new short[j];
                                                modifiedModelColors = new short[j];
                                                for (int k = 0; j > k; k++) {
                                                    originalModelColors[k] = (short) (stream.readUnsignedShort());
                                                    modifiedModelColors[k] = (short) (stream.readUnsignedShort());
                                                }
                                            } else if (opcode == 41) {
                                                int i_57_ = (stream.readUnsignedByte());
                                                aShortArray2060 = new short[i_57_];
                                                aShortArray2075 = new short[i_57_];
                                                for (int i_58_ = 0; i_57_ > i_58_; i_58_++) {
                                                    aShortArray2060[i_58_] = (short) (stream.readUnsignedShort());
                                                    aShortArray2075[i_58_] = (short) (stream.readUnsignedShort());
                                                }
                                            } else if (opcode == 42) {
                                                int i_59_ = (stream.readUnsignedByte());
                                                aByteArray2047 = new byte[i_59_];
                                                for (int i_60_ = 0; i_60_ < i_59_; i_60_++) aByteArray2047[i_60_] = stream.readByte();
                                            } else if (opcode == 65) aBoolean2086 = true; else if (opcode != 78) {
                                                if (opcode != 79) {
                                                    if (opcode == 90) anInt2043 = (stream.readUnsignedShort()); else if (opcode != 91) {
                                                        if (opcode == 92) anInt2067 = (stream.readUnsignedShort()); else if (opcode == 93) anInt2025 = (stream.readUnsignedShort()); else if (opcode != 95) {
                                                            if (opcode != 96) {
                                                                if (opcode != 97) {
                                                                    if (opcode != 98) {
                                                                        if ((opcode >= 100) && opcode < 110) {
                                                                            if (stackIDs == null) {
                                                                                stackAmounts = new int[10];
                                                                                stackIDs = new int[10];
                                                                            }
                                                                            stackIDs[-100 + opcode] = stream.readUnsignedShort();
                                                                            stackAmounts[-100 + opcode] = stream.readUnsignedShort();
                                                                        } else if (opcode != 110) {
                                                                            if (opcode != 111) {
                                                                                if (opcode != 112) {
                                                                                    if (opcode != 113) {
                                                                                        if (opcode != 114) {
                                                                                            if (opcode != 115) {
                                                                                                if (opcode == 121) lendID = stream.readUnsignedShort(); else if (opcode == 122) lendTemplateID = stream.readUnsignedShort(); else if (opcode == 125) {
                                                                                                    anInt2084 = stream.readByte() << 0;
                                                                                                    anInt2031 = stream.readByte() << 0;
                                                                                                    anInt2035 = stream.readByte() << 0;
                                                                                                } else if (opcode != 126) {
                                                                                                    if (opcode == 127) {
                                                                                                        anInt2054 = stream.readUnsignedByte();
                                                                                                        anInt2030 = stream.readUnsignedShort();
                                                                                                    } else if (opcode == 128) {
                                                                                                        anInt2077 = stream.readUnsignedByte();
                                                                                                        anInt2017 = stream.readUnsignedShort();
                                                                                                    } else if (opcode == 129) {
                                                                                                        anInt2063 = stream.readUnsignedByte();
                                                                                                        anInt2059 = stream.readUnsignedShort();
                                                                                                    } else if (opcode == 130) {
                                                                                                        anInt2055 = stream.readUnsignedByte();
                                                                                                        anInt2049 = stream.readUnsignedShort();
                                                                                                    } else if (opcode != 132) {
                                                                                                        if (opcode == 249) {
                                                                                                            int length = stream.readUnsignedByte();
                                                                                                            opcodes2 = new int[length];
                                                                                                            values2 = new Object[length];
                                                                                                            for (int index = 0; index < length; index++) {
                                                                                                                boolean bool = stream.readUnsignedByte() == 1;
                                                                                                                opcodes2[index] = stream.read24BitInt();
                                                                                                                if (bool) {
                                                                                                                    values2[index] = stream.readCString();
                                                                                                                } else {
                                                                                                                    values2[index] = stream.readInt();
                                                                                                                }
                                                                                                            }
                                                                                                        }
                                                                                                    } else {
                                                                                                        int i_65_ = stream.readUnsignedByte();
                                                                                                        anIntArray2018 = new int[i_65_];
                                                                                                        for (int i_66_ = 0; i_66_ < i_65_; i_66_++) anIntArray2018[i_66_] = stream.readUnsignedShort();
                                                                                                    }
                                                                                                } else {
                                                                                                    anInt2071 = stream.readByte() << 0;
                                                                                                    anInt2016 = stream.readByte() << 0;
                                                                                                    anInt2080 = stream.readByte() << 0;
                                                                                                }
                                                                                            } else team = stream.readUnsignedByte();
                                                                                        } else anInt2014 = stream.readByte() * 5;
                                                                                    } else anInt2051 = stream.readByte();
                                                                                } else anInt2009 = stream.readUnsignedShort();
                                                                            } else anInt2050 = stream.readUnsignedShort();
                                                                        } else anInt2056 = stream.readUnsignedShort();
                                                                    } else certTemplateID = stream.readUnsignedShort();
                                                                } else certID = (stream.readUnsignedShort());
                                                            } else anInt2089 = (stream.readUnsignedByte());
                                                        } else anInt2026 = (stream.readUnsignedShort());
                                                    } else anInt2028 = (stream.readUnsignedShort());
                                                } else anInt2008 = stream.readUnsignedShort();
                                            } else anInt2011 = stream.readUnsignedShort();
                                        } else actions[opcode - 35] = stream.readCString();
                                    } else groundActions[-30 + opcode] = stream.readCString();
                                } else femaleEquip2 = stream.readUnsignedShort();
                            } else femaleEquip1 = stream.readUnsignedShort();
                        } else {
                            modelOffset2 = stream.readUnsignedShort();
                            if (modelOffset2 > 32767) modelOffset2 -= 65536;
                            modelOffset2 <<= 0;
                        }
                    } else modelRotation2 = stream.readUnsignedShort();
                } else modelRotation1 = stream.readUnsignedShort();
            } else name = stream.readCString();
        } else modelID = stream.readUnsignedShort();
    }

    public int[] bonus;

    public static void packItemPart2() {
        try {
            for (int i = 0; i < Cache.getAmountOfItems(); i++) {
                final File item = new File("./Data/Items/bonus/" + i + ".txt");
                if (!item.exists()) continue; else {
                    BufferedReader in = new BufferedReader(new FileReader(item));
                    Cache.getItemDefinitionFile2().seek(31 * i);
                    for (int i2 = 0; i2 < 15; i2++) {
                        Cache.getItemDefinitionFile2().writeShort(Integer.parseInt(in.readLine()));
                    }
                    Cache.getItemDefinitionFile2().writeByte(Integer.parseInt(in.readLine()));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadItemPart2() {
        try {
            if (Cache.getItemDefinitionFile2().length() <= (31 * id)) {
                return;
            }
            Cache.getItemDefinitionFile2().seek(31 * id);
            for (int i2 = 0; i2 < 15; i2++) {
                bonus[i2] = Cache.getItemDefinitionFile2().readShort();
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        }
    }
}
