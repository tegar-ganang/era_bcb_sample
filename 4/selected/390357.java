package megamek.common;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.Vector;
import java.util.zip.ZipFile;
import megamek.common.loaders.BLKAeroFile;
import megamek.common.loaders.BLKBattleArmorFile;
import megamek.common.loaders.BLKConvFighterFile;
import megamek.common.loaders.BLKDropshipFile;
import megamek.common.loaders.BLKFile;
import megamek.common.loaders.BLKFixedWingSupportFile;
import megamek.common.loaders.BLKGunEmplacementFile;
import megamek.common.loaders.BLKInfantryFile;
import megamek.common.loaders.BLKJumpshipFile;
import megamek.common.loaders.BLKLargeSupportTankFile;
import megamek.common.loaders.BLKMechFile;
import megamek.common.loaders.BLKProtoFile;
import megamek.common.loaders.BLKSmallCraftFile;
import megamek.common.loaders.BLKSpaceStationFile;
import megamek.common.loaders.BLKSupportTankFile;
import megamek.common.loaders.BLKSupportVTOLFile;
import megamek.common.loaders.BLKTankFile;
import megamek.common.loaders.BLKVTOLFile;
import megamek.common.loaders.BLKWarshipFile;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.loaders.HmpFile;
import megamek.common.loaders.HmvFile;
import megamek.common.loaders.IMechLoader;
import megamek.common.loaders.MepFile;
import megamek.common.loaders.MtfFile;
import megamek.common.loaders.TdbFile;
import megamek.common.preference.PreferenceManager;
import megamek.common.util.BuildingBlock;
import megamek.common.weapons.ISERPPC;
import megamek.common.weapons.ISHeavyPPC;
import megamek.common.weapons.ISLightPPC;
import megamek.common.weapons.ISPPC;
import megamek.common.weapons.ISSnubNosePPC;

public class MechFileParser {

    private Entity m_entity = null;

    private static Vector<String> canonUnitNames = null;

    private static final File ROOT = new File(PreferenceManager.getClientPreferences().getMechDirectory());

    private static final File OFFICIALUNITS = new File(ROOT, "OfficialUnitList.txt");

    public MechFileParser(File f) throws EntityLoadingException {
        this(f, null);
    }

    public MechFileParser(File f, String entryName) throws EntityLoadingException {
        if (entryName == null) {
            try {
                parse(new FileInputStream(f.getAbsolutePath()), f.getName());
            } catch (Exception ex) {
                ex.printStackTrace();
                if (ex instanceof EntityLoadingException) {
                    throw new EntityLoadingException("While parsing file " + f.getName() + ", " + ex.getMessage());
                }
                throw new EntityLoadingException("Exception from " + ex.getClass() + ": " + ex.getMessage());
            }
        } else {
            ZipFile zFile;
            try {
                zFile = new ZipFile(f.getAbsolutePath());
                parse(zFile.getInputStream(zFile.getEntry(entryName)), entryName);
            } catch (EntityLoadingException ele) {
                throw new EntityLoadingException(ele.getMessage());
            } catch (NullPointerException npe) {
                throw new NullPointerException();
            } catch (Exception ex) {
                ex.printStackTrace();
                throw new EntityLoadingException("Exception from " + ex.getClass() + ": " + ex.getMessage());
            }
        }
    }

    public MechFileParser(InputStream is, String fileName) throws EntityLoadingException {
        try {
            parse(is, fileName);
        } catch (Exception ex) {
            ex.printStackTrace();
            if (ex instanceof EntityLoadingException) {
                throw new EntityLoadingException(ex.getMessage());
            }
            throw new EntityLoadingException("Exception from " + ex.getClass() + ": " + ex.getMessage());
        }
    }

    public Entity getEntity() {
        return m_entity;
    }

    public void parse(InputStream is, String fileName) throws EntityLoadingException {
        String lowerName = fileName.toLowerCase();
        IMechLoader loader;
        if (lowerName.endsWith(".mep")) {
            loader = new MepFile(is);
        } else if (lowerName.endsWith(".mtf")) {
            loader = new MtfFile(is);
        } else if (lowerName.endsWith(".hmp")) {
            loader = new HmpFile(is);
        } else if (lowerName.endsWith(".hmv")) {
            loader = new HmvFile(is);
        } else if (lowerName.endsWith(".xml")) {
            loader = new TdbFile(is);
        } else if (lowerName.endsWith(".blk")) {
            BuildingBlock bb = new BuildingBlock(is);
            if (bb.exists("UnitType")) {
                String sType = bb.getDataAsString("UnitType")[0];
                if (sType.equals("Tank") || sType.equals("Naval") || sType.equals("Surface") || sType.equals("Hydrofoil")) {
                    loader = new BLKTankFile(bb);
                } else if (sType.equals("Infantry")) {
                    loader = new BLKInfantryFile(bb);
                } else if (sType.equals("BattleArmor")) {
                    loader = new BLKBattleArmorFile(bb);
                } else if (sType.equals("ProtoMech")) {
                    loader = new BLKProtoFile(bb);
                } else if (sType.equals("Mech")) {
                    loader = new BLKMechFile(bb);
                } else if (sType.equals("VTOL")) {
                    loader = new BLKVTOLFile(bb);
                } else if (sType.equals("GunEmplacement")) {
                    loader = new BLKGunEmplacementFile(bb);
                } else if (sType.equals("SupportTank")) {
                    loader = new BLKSupportTankFile(bb);
                } else if (sType.equals("LargeSupportTank")) {
                    loader = new BLKLargeSupportTankFile(bb);
                } else if (sType.equals("SupportVTOL")) {
                    loader = new BLKSupportVTOLFile(bb);
                } else if (sType.equals("Aero")) {
                    loader = new BLKAeroFile(bb);
                } else if (sType.equals("FixedWingSupport")) {
                    loader = new BLKFixedWingSupportFile(bb);
                } else if (sType.equals("ConvFighter")) {
                    loader = new BLKConvFighterFile(bb);
                } else if (sType.equals("SmallCraft")) {
                    loader = new BLKSmallCraftFile(bb);
                } else if (sType.equals("Dropship")) {
                    loader = new BLKDropshipFile(bb);
                } else if (sType.equals("Jumpship")) {
                    loader = new BLKJumpshipFile(bb);
                } else if (sType.equals("Warship")) {
                    loader = new BLKWarshipFile(bb);
                } else if (sType.equals("SpaceStation")) {
                    loader = new BLKSpaceStationFile(bb);
                } else {
                    throw new EntityLoadingException("Unknown UnitType: " + sType);
                }
            } else {
                loader = new BLKMechFile(bb);
            }
        } else if (lowerName.endsWith(".dbm")) {
            throw new EntityLoadingException("In order to use mechs from The Drawing Board with MegaMek, you must save your mech as an XML file (look in the 'File' menu of TDB.)  Then use the resulting '.xml' file instead of the '.dbm' file.  Note that only version 2.0.23 or later of TDB is compatible with MegaMek.");
        } else {
            throw new EntityLoadingException("Unsupported file suffix");
        }
        m_entity = loader.getEntity();
        MechFileParser.postLoadInit(m_entity);
    }

    /**
     * File-format agnostic location to do post-load initialization on a unit.
     * Automatically add BattleArmorHandles to all OmniMechs.
     */
    public static void postLoadInit(Entity ent) throws EntityLoadingException {
        if (ent instanceof Mech) {
            ent.getSensors().add(new Sensor(Sensor.TYPE_MEK_RADAR));
            ent.getSensors().add(new Sensor(Sensor.TYPE_MEK_IR));
            ent.getSensors().add(new Sensor(Sensor.TYPE_MEK_MAGSCAN));
            ent.getSensors().add(new Sensor(Sensor.TYPE_MEK_SEISMIC));
            ent.setNextSensor(ent.getSensors().firstElement());
        } else if (ent instanceof VTOL) {
            ent.getSensors().add(new Sensor(Sensor.TYPE_VEE_RADAR));
            ent.getSensors().add(new Sensor(Sensor.TYPE_VEE_IR));
            ent.getSensors().add(new Sensor(Sensor.TYPE_VEE_MAGSCAN));
            ent.setNextSensor(ent.getSensors().firstElement());
        } else if (ent instanceof Tank) {
            ent.getSensors().add(new Sensor(Sensor.TYPE_VEE_RADAR));
            ent.getSensors().add(new Sensor(Sensor.TYPE_VEE_IR));
            ent.getSensors().add(new Sensor(Sensor.TYPE_VEE_MAGSCAN));
            ent.getSensors().add(new Sensor(Sensor.TYPE_VEE_SEISMIC));
            ent.setNextSensor(ent.getSensors().firstElement());
        } else if (ent instanceof BattleArmor) {
            if (ent.hasWorkingMisc(MiscType.F_HEAT_SENSOR)) {
                ent.getSensors().add(new Sensor(Sensor.TYPE_BA_HEAT));
                ent.setNextSensor(ent.getSensors().lastElement());
            }
        }
        for (Mounted m : ent.getMisc()) {
            if ((m.getType().hasFlag(MiscType.F_LASER_INSULATOR))) {
                Mounted weapon = ent.getEquipment().get(ent.getEquipment().indexOf(m) - 1);
                if (weapon.getLinkedBy() != null) {
                    continue;
                }
                if (!(weapon.getType() instanceof WeaponType) && !(weapon.getType().hasFlag(WeaponType.F_LASER))) {
                    continue;
                }
                if (weapon.getLocation() == m.getLocation()) {
                    m.setLinked(weapon);
                    continue;
                }
                if (m.getLinked() == null) {
                    throw new EntityLoadingException("Unable to match laser insulator to laser");
                }
            }
            if ((m.getType().hasFlag(MiscType.F_DETACHABLE_WEAPON_PACK))) {
                for (Mounted mWeapon : ent.getTotalWeaponList()) {
                    if (!mWeapon.isDWPMounted()) {
                        continue;
                    }
                    if (mWeapon.getLinkedBy() != null) {
                        continue;
                    }
                    if (mWeapon.getLocation() == m.getLocation()) {
                        m.setLinked(mWeapon);
                        break;
                    }
                }
                if (m.getLinked() == null) {
                    throw new EntityLoadingException("Unable to match DWP to weapon");
                }
            }
            if ((m.getType().hasFlag(MiscType.F_ARTEMIS) || (m.getType().hasFlag(MiscType.F_ARTEMIS_V))) && (m.getLinked() == null)) {
                for (Mounted mWeapon : ent.getTotalWeaponList()) {
                    WeaponType wtype = (WeaponType) mWeapon.getType();
                    if ((wtype.getAmmoType() != AmmoType.T_LRM) && (wtype.getAmmoType() != AmmoType.T_MML) && (wtype.getAmmoType() != AmmoType.T_SRM) && (wtype.getAmmoType() != AmmoType.T_NLRM) && (wtype.getAmmoType() != AmmoType.T_LRM_TORPEDO) && (wtype.getAmmoType() != AmmoType.T_SRM_TORPEDO) && (wtype.getAmmoType() != AmmoType.T_LRM_TORPEDO_COMBO)) {
                        continue;
                    }
                    if (mWeapon.getLinkedBy() != null) {
                        continue;
                    }
                    if (mWeapon.getLocation() == m.getLocation()) {
                        m.setLinked(mWeapon);
                        break;
                    }
                }
                if (m.getLinked() == null) {
                    throw new EntityLoadingException("Unable to match Artemis to launcher");
                }
            } else if ((m.getType().hasFlag(MiscType.F_STEALTH) || m.getType().hasFlag(MiscType.F_VOIDSIG)) && (m.getLinked() == null) && (ent instanceof Mech)) {
                for (Mounted mEquip : ent.getMisc()) {
                    MiscType mtype = (MiscType) mEquip.getType();
                    if (mtype.hasFlag(MiscType.F_ECM)) {
                        m.setLinked(mEquip);
                        break;
                    }
                }
                if (m.getLinked() == null) {
                    throw new EntityLoadingException("Unable to find an ECM Suite.  Mechs with Stealth Armor or Void-Signature-System must also be equipped with an ECM Suite.");
                }
            } else if (m.getType().hasFlag(MiscType.F_PPC_CAPACITOR) && (m.getLinked() == null)) {
                for (Mounted mWeapon : ent.getWeaponList()) {
                    WeaponType wtype = (WeaponType) mWeapon.getType();
                    if (!wtype.hasFlag(WeaponType.F_PPC)) {
                        continue;
                    }
                    if (mWeapon.getLinkedBy() != null) {
                        continue;
                    }
                    if (mWeapon.getLocation() == m.getLocation()) {
                        if ((mWeapon.getType() instanceof ISPPC) || (mWeapon.getType() instanceof ISLightPPC) || (mWeapon.getType() instanceof ISHeavyPPC) || (mWeapon.getType() instanceof ISERPPC) || (mWeapon.getType() instanceof ISSnubNosePPC)) {
                            m.setLinked(mWeapon);
                            break;
                        }
                    }
                }
            } else if (m.getType().hasFlag(MiscType.F_APOLLO) && (m.getLinked() == null)) {
                for (Mounted mWeapon : ent.getTotalWeaponList()) {
                    WeaponType wtype = (WeaponType) mWeapon.getType();
                    if (wtype.getAmmoType() != AmmoType.T_MRM) {
                        continue;
                    }
                    if (mWeapon.getLinkedBy() != null) {
                        continue;
                    }
                    if (mWeapon.getLocation() == m.getLocation()) {
                        m.setLinked(mWeapon);
                        break;
                    }
                }
                if (m.getLinked() == null) {
                    throw new EntityLoadingException("Unable to match Apollo to launcher");
                }
            }
            if (m.getType().hasFlag(MiscType.F_BAP)) {
                if (m.getType().getInternalName().equals(Sensor.BAP)) {
                    ent.getSensors().add(new Sensor(Sensor.TYPE_BAP));
                    ent.setNextSensor(ent.getSensors().lastElement());
                } else if (m.getType().getInternalName().equals(Sensor.BLOODHOUND)) {
                    ent.getSensors().add(new Sensor(Sensor.TYPE_BLOODHOUND));
                    ent.setNextSensor(ent.getSensors().lastElement());
                } else if (m.getType().getInternalName().equals(Sensor.WATCHDOG)) {
                    ent.getSensors().add(new Sensor(Sensor.TYPE_WATCHDOG));
                    ent.setNextSensor(ent.getSensors().lastElement());
                } else if (m.getType().getInternalName().equals(Sensor.CLAN_AP)) {
                    ent.getSensors().add(new Sensor(Sensor.TYPE_CLAN_BAP));
                    ent.setNextSensor(ent.getSensors().lastElement());
                } else if (m.getType().getInternalName().equals(Sensor.LIGHT_AP)) {
                    ent.getSensors().add(new Sensor(Sensor.TYPE_LIGHT_AP));
                    ent.setNextSensor(ent.getSensors().lastElement());
                } else if (m.getType().getInternalName().equals(Sensor.CLIMPROVED)) {
                    ent.getSensors().add(new Sensor(Sensor.TYPE_BA_IMPROVED));
                    ent.setNextSensor(ent.getSensors().lastElement());
                } else if (m.getType().getInternalName().equals(Sensor.ISIMPROVED)) {
                    ent.getSensors().add(new Sensor(Sensor.TYPE_BA_IMPROVED));
                    ent.setNextSensor(ent.getSensors().lastElement());
                }
            }
            if ((ent instanceof Mech) && (m.getType().hasFlag(MiscType.F_CASE) || m.getType().hasFlag(MiscType.F_CASEII))) {
                ((Mech) ent).setAutoEject(false);
            }
            if ((ent instanceof Mech) && m.getType().hasFlag(MiscType.F_ACTUATOR_ENHANCEMENT_SYSTEM)) {
                if (ent.hasTargComp() || ((Mech) ent).hasTSM() || (((Mech) ent).hasMASC() && !ent.hasWorkingMisc(MiscType.F_MASC, MiscType.S_SUPERCHARGER))) {
                    throw new EntityLoadingException("Unable to load AES due to incompatible systems");
                }
                if ((m.getLocation() != Mech.LOC_LARM) && (m.getLocation() != Mech.LOC_LLEG) && (m.getLocation() != Mech.LOC_RARM) && (m.getLocation() != Mech.LOC_RLEG)) {
                    throw new EntityLoadingException("Unable to load AES due to incompatible location");
                }
            }
            if (m.getType().hasFlag(MiscType.F_HARJEL) && (m.getLocation() == Mech.LOC_HEAD)) {
                throw new EntityLoadingException("Unable to load harjel in head.");
            }
            if (m.getType().hasFlag(MiscType.F_MASS) && ((m.getLocation() != Mech.LOC_HEAD) || ((((Mech) ent).getCockpitType() == Mech.COCKPIT_TORSO_MOUNTED) && (m.getLocation() != Mech.LOC_CT)))) {
                throw new EntityLoadingException("Unable to load MASS!  Must be located in the same location as the cockpit.");
            }
            if (m.getType().hasFlag(MiscType.F_MODULAR_ARMOR) && (((ent instanceof Mech) && (m.getLocation() == Mech.LOC_HEAD)) || ((ent instanceof VTOL) && (m.getLocation() == VTOL.LOC_ROTOR)))) {
                throw new EntityLoadingException("Unable to load Modular Armor in Rotor/Head location");
            }
            if (m.getType().hasFlag(MiscType.F_TALON)) {
                if (ent instanceof BipedMech) {
                    if ((m.getLocation() != Mech.LOC_LLEG) && (m.getLocation() != Mech.LOC_RLEG)) {
                        throw new EntityLoadingException("Talons are only legal in the Legs");
                    }
                    if (!ent.hasWorkingMisc(MiscType.F_TALON, -1, Mech.LOC_RLEG) || !ent.hasWorkingMisc(MiscType.F_TALON, -1, Mech.LOC_LLEG)) {
                        throw new EntityLoadingException("Talons must be in all legs");
                    }
                } else if (ent instanceof QuadMech) {
                    if ((m.getLocation() != Mech.LOC_LLEG) && (m.getLocation() != Mech.LOC_RLEG) && (m.getLocation() != Mech.LOC_LARM) && (m.getLocation() != Mech.LOC_RARM)) {
                        throw new EntityLoadingException("Talons are only legal in the Legs");
                    }
                    if (!ent.hasWorkingMisc(MiscType.F_TALON, -1, Mech.LOC_RLEG) || !ent.hasWorkingMisc(MiscType.F_TALON, -1, Mech.LOC_LLEG) || !ent.hasWorkingMisc(MiscType.F_TALON, -1, Mech.LOC_LARM) || !ent.hasWorkingMisc(MiscType.F_TALON, -1, Mech.LOC_LARM)) {
                        throw new EntityLoadingException("Talons must be in all legs");
                    }
                } else {
                    throw new EntityLoadingException("Unable to load talons in non-Mek entity");
                }
            }
        }
        for (Mounted m : ent.getMisc()) {
            if (m.getType().hasFlag(MiscType.F_PPC_CAPACITOR) && (m.getLinked() == null)) {
                for (Mounted mWeapon : ent.getWeaponList()) {
                    WeaponType wtype = (WeaponType) mWeapon.getType();
                    if (!wtype.hasFlag(WeaponType.F_PPC)) {
                        continue;
                    }
                    if (mWeapon.getCrossLinkedBy() != null) {
                        continue;
                    }
                    if (mWeapon.getLocation() == m.getLocation()) {
                        if ((mWeapon.getType() instanceof ISPPC) || (mWeapon.getType() instanceof ISLightPPC) || (mWeapon.getType() instanceof ISHeavyPPC) || (mWeapon.getType() instanceof ISERPPC) || (mWeapon.getType() instanceof ISSnubNosePPC)) {
                            m.setCrossLinked(mWeapon);
                            break;
                        }
                    }
                }
                if (m.getLinked() == null) {
                    throw new EntityLoadingException("No available PPC to match Capacitor!");
                }
            }
        }
        if (ent.usesWeaponBays()) {
            ent.loadAllWeapons();
        }
        if (ent instanceof Aero) {
            ent.setRapidFire();
        }
        if (ent instanceof BattleArmor) {
            if (((BattleArmor) ent).getChassisType() != BattleArmor.CHASSIS_TYPE_QUAD) {
                int tBasicManipulatorCount = ent.countWorkingMisc(MiscType.F_BASIC_MANIPULATOR);
                int tArmoredGloveCount = ent.countWorkingMisc(MiscType.F_ARMORED_GLOVE);
                int tBattleClawCount = ent.countWorkingMisc(MiscType.F_BATTLE_CLAW);
                switch(ent.getWeightClass()) {
                    case EntityWeightClass.WEIGHT_BA_PAL:
                    case EntityWeightClass.WEIGHT_BA_LIGHT:
                        if ((tArmoredGloveCount > 1) || (tBasicManipulatorCount > 1) || (tBattleClawCount > 0)) {
                            try {
                                ent.addEquipment(EquipmentType.get(Infantry.SWARM_MEK), BattleArmor.LOC_SQUAD, false, false, false);
                                ent.addEquipment(EquipmentType.get(Infantry.STOP_SWARM), BattleArmor.LOC_SQUAD, false, false, false);
                                ent.addEquipment(EquipmentType.get(Infantry.LEG_ATTACK), BattleArmor.LOC_SQUAD, false, false, false);
                            } catch (LocationFullException ex) {
                                throw new EntityLoadingException(ex.getMessage());
                            }
                        }
                        break;
                    case EntityWeightClass.WEIGHT_BA_MEDIUM:
                        if ((tBasicManipulatorCount > 1) || (tBattleClawCount > 0)) {
                            try {
                                ent.addEquipment(EquipmentType.get(Infantry.SWARM_MEK), BattleArmor.LOC_SQUAD, false, false, false);
                                ent.addEquipment(EquipmentType.get(Infantry.STOP_SWARM), BattleArmor.LOC_SQUAD, false, false, false);
                                ent.addEquipment(EquipmentType.get(Infantry.LEG_ATTACK), BattleArmor.LOC_SQUAD, false, false, false);
                            } catch (LocationFullException ex) {
                                throw new EntityLoadingException(ex.getMessage());
                            }
                        }
                        break;
                    case EntityWeightClass.WEIGHT_BA_HEAVY:
                    case EntityWeightClass.WEIGHT_BA_ASSAULT:
                    default:
                        break;
                }
                if (ent.countWorkingMisc(MiscType.F_AP_MOUNT) > 0) {
                    try {
                        ent.addEquipment(EquipmentType.get("InfantryAssaultRifle"), BattleArmor.LOC_SQUAD, false, false, false);
                    } catch (LocationFullException ex) {
                        throw new EntityLoadingException(ex.getMessage());
                    }
                }
            }
        } else if ((ent instanceof Infantry) && ((Infantry) ent).canAttackMeks()) {
            try {
                ent.addEquipment(EquipmentType.get(Infantry.SWARM_MEK), Infantry.LOC_INFANTRY, false, false, false);
                ent.addEquipment(EquipmentType.get(Infantry.STOP_SWARM), Infantry.LOC_INFANTRY, false, false, false);
                ent.addEquipment(EquipmentType.get(Infantry.LEG_ATTACK), Infantry.LOC_INFANTRY, false, false, false);
            } catch (LocationFullException ex) {
                throw new EntityLoadingException(ex.getMessage());
            }
        }
        ent.setCanon(false);
        try {
            if (canonUnitNames == null) {
                canonUnitNames = new Vector<String>();
                BufferedReader br = null;
                try {
                    br = new BufferedReader(new FileReader(OFFICIALUNITS));
                    String s;
                    String name;
                    while ((s = br.readLine()) != null) {
                        int nIndex1 = s.indexOf('|');
                        name = s.substring(0, nIndex1);
                        canonUnitNames.addElement(name);
                    }
                } catch (FileNotFoundException e) {
                }
            }
        } catch (IOException e) {
        }
        for (Enumeration<String> i = canonUnitNames.elements(); i.hasMoreElements(); ) {
            String s = i.nextElement();
            if (s.equals(ent.getShortNameRaw())) {
                ent.setCanon(true);
                break;
            }
        }
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Files in a supported MegaMek file format can be specified on");
            System.out.println("the command line.  Multiple files may be processed at once.");
            System.out.println("The supported formats are:");
            System.out.println("\t.mtf    The native MegaMek format that your file will be converted into");
            System.out.println("\t.blk    Another native MegaMek format");
            System.out.println("\t.hmp    Heavy Metal Pro (c)RCW Enterprises");
            System.out.println("\t.mep    MechEngineer Pro (c)Howling Moon SoftWorks");
            System.out.println("\t.xml    The Drawing Board (c)Blackstone Interactive");
            System.out.println("Note: If you are using the MtfConvert utility, you may also drag and drop files onto it for conversion.");
            MechFileParser.getResponse("Press <enter> to exit...");
            return;
        }
        for (int i = 0; i < args.length; i++) {
            String filename = args[i];
            File file = new File(filename);
            String outFilename = filename.substring(0, filename.lastIndexOf("."));
            BufferedWriter out = null;
            try {
                MechFileParser mfp = new MechFileParser(file);
                Entity e = mfp.getEntity();
                if (e instanceof Mech) {
                    outFilename += ".mtf";
                    File outFile = new File(outFilename);
                    if (outFile.exists()) {
                        if (!MechFileParser.getResponse("File already exists, overwrite? ")) {
                            return;
                        }
                    }
                    out = new BufferedWriter(new FileWriter(outFile));
                    out.write(((Mech) e).getMtf());
                } else if (e instanceof Tank) {
                    outFilename += ".blk";
                    BLKFile.encode(outFilename, e);
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
                MechFileParser.getResponse("Press <enter> to exit...");
            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException ex) {
                    }
                }
            }
        }
    }

    private static boolean getResponse(String prompt) {
        String response = null;
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        System.out.print(prompt);
        try {
            response = in.readLine();
        } catch (IOException ioe) {
        }
        if ((response != null) && (response.toLowerCase().indexOf("y") == 0)) {
            return true;
        }
        return false;
    }

    public static Entity loadEntity(File f, String entityName) {
        Entity entity = null;
        try {
            entity = new MechFileParser(f, entityName).getEntity();
        } catch (megamek.common.loaders.EntityLoadingException e) {
            System.out.println("Exception: " + e.toString());
            e.printStackTrace();
        }
        return entity;
    }

    public static void dispose() {
        canonUnitNames = null;
    }
}
