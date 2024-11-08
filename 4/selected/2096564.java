package org.dag.dmj.data;

import java.awt.*;
import javax.swing.*;
import java.io.*;
import org.dag.dmj.*;
import org.dag.dmj.item.Torch;

public class HeroData extends JLabel {

    String name;

    String lastname;

    String picname;

    Image pic;

    Item weapon = Item.fistfoot;

    Item head = null;

    Item torso = null;

    Item legs = null;

    Item feet = null;

    Item hand = null;

    Item neck = null;

    Item pouch1 = null;

    Item pouch2 = null;

    Item[] pack = new Item[16];

    Item[] quiver = new Item[6];

    int heronumber;

    int subsquare;

    int number;

    int maxmana;

    int maxhealth;

    int maxstamina;

    int mana;

    int health;

    int stamina;

    int food;

    int water;

    int strength;

    int vitality;

    int dexterity;

    int intelligence;

    int wisdom;

    int defense;

    int magicresist;

    int flevel;

    int nlevel;

    int plevel;

    int wlevel;

    int fxp = 0;

    int nxp = 0;

    int pxp = 0;

    int wxp = 0;

    int strengthboost, intelligenceboost, wisdomboost, dexterityboost, vitalityboost, defenseboost, magicresistboost, flevelboost, nlevelboost, wlevelboost, plevelboost;

    float maxload, load;

    boolean isleader = false;

    boolean isdead = false;

    boolean wepready = true;

    boolean ispoisoned = false;

    boolean silenced = false;

    boolean hurthead = false, hurttorso = false, hurtlegs = false, hurtfeet = false, hurthand = false, hurtweapon = false;

    int silencecount = 0;

    int poison = 0;

    int spellcount = 0;

    int weaponcount = 0;

    int timecounter = 0;

    int poisoncounter = 0;

    int walkcounter = 0;

    int kuswordcount = 0;

    int rosbowcount = 0;

    String currentspell = "";

    SpecialAbility[] abilities;

    public HeroData() {
        setOpaque(true);
        setBackground(new Color(100, 100, 100));
        setPreferredSize(new Dimension(66, 60));
        picname = "balaan.gif";
        setIcon(new ImageIcon("Heroes" + File.separator + picname));
        pic = ((ImageIcon) getIcon()).getImage();
        setVerticalAlignment(JLabel.CENTER);
        setHorizontalAlignment(JLabel.CENTER);
        name = "Balaan";
        lastname = "Gor Priest";
        maxhealth = 60;
        health = 60;
        maxstamina = 40;
        stamina = 40;
        maxmana = 15;
        mana = 15;
        food = 1000;
        water = 1000;
        strength = 40;
        dexterity = 40;
        vitality = 40;
        intelligence = 40;
        wisdom = 40;
        defense = 4;
        magicresist = 4;
        maxload = strength * 4 / 5;
    }

    public HeroData(String picname) {
        setOpaque(true);
        setBackground(new Color(100, 100, 100));
        setPreferredSize(new Dimension(66, 60));
        if (picname.indexOf("Heroes") >= 0) picname = picname.substring(picname.indexOf("Heroes") + 7);
        this.picname = picname;
        setIcon(new ImageIcon("Heroes" + File.separator + picname));
        pic = ((ImageIcon) getIcon()).getImage();
        setVerticalAlignment(JLabel.CENTER);
        setHorizontalAlignment(JLabel.CENTER);
    }

    public HeroData(HeroData h) {
        name = new String(h.name);
        lastname = new String(h.lastname);
        picname = new String(h.picname);
        pic = h.pic;
        setIcon(new ImageIcon(((ImageIcon) h.getIcon()).getImage()));
        if (h.weapon != Item.fistfoot) weapon = Item.createCopy(h.weapon); else weapon = Item.fistfoot;
        if (h.hand != null) hand = Item.createCopy(h.hand);
        if (h.head != null) head = Item.createCopy(h.head);
        if (h.neck != null) neck = Item.createCopy(h.neck);
        if (h.torso != null) torso = Item.createCopy(h.torso);
        if (h.legs != null) legs = Item.createCopy(h.legs);
        if (h.feet != null) feet = Item.createCopy(h.feet);
        if (h.pouch1 != null) pouch1 = Item.createCopy(h.pouch1);
        if (h.pouch2 != null) pouch2 = Item.createCopy(h.pouch2);
        for (int i = 0; i < 16; i++) {
            if (h.pack[i] != null) pack[i] = Item.createCopy(h.pack[i]);
        }
        for (int i = 0; i < 6; i++) {
            if (h.quiver[i] != null) quiver[i] = Item.createCopy(h.quiver[i]);
        }
        heronumber = h.heronumber;
        subsquare = h.subsquare;
        number = h.number;
        maxmana = h.maxmana;
        maxhealth = h.maxhealth;
        maxstamina = h.maxstamina;
        mana = h.mana;
        health = h.health;
        stamina = h.stamina;
        food = h.food;
        water = h.water;
        strength = h.strength;
        vitality = h.vitality;
        dexterity = h.dexterity;
        intelligence = h.intelligence;
        wisdom = h.wisdom;
        defense = h.defense;
        magicresist = h.magicresist;
        flevel = h.flevel;
        nlevel = h.nlevel;
        plevel = h.plevel;
        wlevel = h.wlevel;
        flevelboost = h.flevelboost;
        nlevelboost = h.nlevelboost;
        plevelboost = h.plevelboost;
        wlevelboost = h.wlevelboost;
        fxp = h.fxp;
        nxp = h.nxp;
        pxp = h.pxp;
        wxp = h.wxp;
        strengthboost = h.strengthboost;
        intelligenceboost = h.intelligenceboost;
        wisdomboost = h.wisdomboost;
        dexterityboost = h.dexterityboost;
        vitalityboost = h.vitalityboost;
        defenseboost = h.defenseboost;
        magicresistboost = h.magicresistboost;
        maxload = h.maxload;
        load = h.load;
        isleader = h.isleader;
        isdead = h.isdead;
        wepready = h.wepready;
        ispoisoned = h.ispoisoned;
        silenced = h.silenced;
        hurthead = h.hurthead;
        hurttorso = h.hurttorso;
        hurtlegs = h.hurtlegs;
        hurtfeet = h.hurtfeet;
        hurthand = h.hurthand;
        hurtweapon = h.hurtweapon;
        silencecount = h.silencecount;
        poison = h.poison;
        spellcount = h.spellcount;
        weaponcount = h.weaponcount;
        timecounter = h.timecounter;
        poisoncounter = h.poisoncounter;
        walkcounter = h.walkcounter;
        kuswordcount = h.kuswordcount;
        rosbowcount = h.rosbowcount;
        currentspell = new String(h.currentspell);
        if (h.abilities != null) {
            abilities = new SpecialAbility[h.abilities.length];
            for (int j = 0; j < h.abilities.length; j++) {
                abilities[j] = new SpecialAbility(h.abilities[j]);
            }
        }
    }

    public HeroData(DMJava.Hero h) {
        name = new String(h.name);
        lastname = new String(h.lastname);
        picname = new String(h.picname);
        pic = h.pic;
        setIcon(new ImageIcon(pic));
        if (h.weapon != Item.fistfoot) weapon = Item.createCopy(h.weapon); else weapon = Item.fistfoot;
        if (h.hand != null) hand = Item.createCopy(h.hand);
        if (h.head != null) head = Item.createCopy(h.head);
        if (h.neck != null) neck = Item.createCopy(h.neck);
        if (h.torso != null) torso = Item.createCopy(h.torso);
        if (h.legs != null) legs = Item.createCopy(h.legs);
        if (h.feet != null) feet = Item.createCopy(h.feet);
        if (h.pouch1 != null) pouch1 = Item.createCopy(h.pouch1);
        if (h.pouch2 != null) pouch2 = Item.createCopy(h.pouch2);
        for (int i = 0; i < 16; i++) {
            if (h.pack[i] != null) pack[i] = Item.createCopy(h.pack[i]);
        }
        for (int i = 0; i < 6; i++) {
            if (h.quiver[i] != null) quiver[i] = Item.createCopy(h.quiver[i]);
        }
        heronumber = h.heronumber;
        subsquare = h.subsquare;
        number = h.number;
        maxmana = h.maxmana;
        maxhealth = h.maxhealth;
        maxstamina = h.maxstamina;
        mana = h.mana;
        health = h.health;
        stamina = h.stamina;
        food = h.food;
        water = h.water;
        strength = h.strength;
        vitality = h.vitality;
        dexterity = h.dexterity;
        intelligence = h.intelligence;
        wisdom = h.wisdom;
        defense = h.defense;
        magicresist = h.magicresist;
        flevel = h.flevel;
        nlevel = h.nlevel;
        plevel = h.plevel;
        wlevel = h.wlevel;
        flevelboost = h.flevelboost;
        nlevelboost = h.nlevelboost;
        plevelboost = h.plevelboost;
        wlevelboost = h.wlevelboost;
        fxp = h.fxp;
        nxp = h.nxp;
        pxp = h.pxp;
        wxp = h.wxp;
        strengthboost = h.strengthboost;
        intelligenceboost = h.intelligenceboost;
        wisdomboost = h.wisdomboost;
        dexterityboost = h.dexterityboost;
        vitalityboost = h.vitalityboost;
        defenseboost = h.defenseboost;
        magicresistboost = h.magicresistboost;
        maxload = h.maxload;
        load = h.load;
        isleader = h.isleader;
        isdead = h.isdead;
        wepready = h.wepready;
        ispoisoned = h.ispoisoned;
        silenced = h.silenced;
        hurthead = h.hurthead;
        hurttorso = h.hurttorso;
        hurtlegs = h.hurtlegs;
        hurtfeet = h.hurtfeet;
        hurthand = h.hurthand;
        hurtweapon = h.hurtweapon;
        silencecount = h.silencecount;
        poison = h.poison;
        spellcount = h.spellcount;
        weaponcount = h.weaponcount;
        timecounter = h.timecounter;
        poisoncounter = h.poisoncounter;
        walkcounter = h.walkcounter;
        kuswordcount = h.kuswordcount;
        rosbowcount = h.rosbowcount;
        currentspell = new String(h.currentspell);
        if (h.abilities != null) {
            abilities = new SpecialAbility[h.abilities.length];
            for (int j = 0; j < h.abilities.length; j++) {
                abilities[j] = new SpecialAbility(h.abilities[j]);
            }
        }
    }

    public void setLoad() {
        if (head != null) load += head.weight;
        if (neck != null) load += neck.weight;
        if (torso != null) load += torso.weight;
        if (legs != null) load += legs.weight;
        if (feet != null) load += feet.weight;
        if (hand != null) load += hand.weight;
        if (weapon != null) load += weapon.weight;
        if (pouch1 != null) load += pouch1.weight;
        if (pouch2 != null) load += pouch2.weight;
        for (int j = 0; j < 6; j++) if (quiver[j] != null) load += quiver[j].weight;
        for (int i = 0; i < 16; i++) if (pack[i] != null) load += pack[i].weight;
    }

    public void setDefense() {
        if (weapon != null && (weapon.type == Item.WEAPON || weapon.type == Item.SHIELD)) {
            equipEffect(weapon);
        }
        if (head != null) {
            equipEffect(head);
        }
        if (neck != null) {
            equipEffect(neck);
        }
        if (torso != null) {
            equipEffect(torso);
        }
        if (hand != null) {
            equipEffect(hand);
        }
        if (legs != null) {
            equipEffect(legs);
        }
        if (feet != null) {
            equipEffect(feet);
        }
    }

    public void equipEffect(Item it) {
        if (it.epic != null) {
            it.temppic = it.pic;
            it.pic = it.epic;
        }
        defense += it.defense;
        magicresist += it.magicresist;
        if (!it.haseffect) return;
        String whataffected;
        int e = 0;
        for (int i = it.effect.length; i > 0; i--) {
            whataffected = it.effect[i - 1].substring(0, it.effect[i - 1].indexOf(','));
            e = Integer.parseInt(it.effect[i - 1].substring(it.effect[i - 1].indexOf(',') + 1));
            whataffected.toLowerCase();
            if (whataffected.equals("mana")) maxmana += e; else if (whataffected.equals("health")) maxhealth += e; else if (whataffected.equals("stamina")) maxstamina += e; else if (whataffected.equals("strength")) strength += e; else if (whataffected.equals("vitality")) vitality += e; else if (whataffected.equals("dexterity")) dexterity += e; else if (whataffected.equals("intelligence")) intelligence += e; else if (whataffected.equals("wisdom")) wisdom += e; else if (whataffected.equals("flevel")) {
                if (flevel + e < 15) {
                    flevel += e;
                    it.fleveladded = e;
                } else {
                    it.fleveladded = 15 - flevel;
                    flevel = 15;
                }
            } else if (whataffected.equals("nlevel")) {
                if (nlevel + e < 15) {
                    nlevel += e;
                    it.nleveladded = e;
                } else {
                    it.nleveladded = 15 - nlevel;
                    nlevel = 15;
                }
            } else if (whataffected.equals("wlevel")) {
                if (wlevel + e < 15) {
                    wlevel += e;
                    it.wleveladded = e;
                } else {
                    it.wleveladded = 15 - wlevel;
                    wlevel = 15;
                }
            } else if (whataffected.equals("plevel")) {
                if (plevel + e < 15) {
                    plevel += e;
                    it.pleveladded = e;
                } else {
                    it.pleveladded = 15 - plevel;
                    plevel = 15;
                }
            }
        }
        setMaxLoad();
    }

    public void unEquipEffect(Item it) {
        if (it.epic != null) {
            it.pic = it.temppic;
        }
        defense -= it.defense;
        magicresist -= it.magicresist;
        if (!it.haseffect) return;
        String whataffected;
        int e = 0;
        for (int i = it.effect.length; i > 0; i--) {
            whataffected = it.effect[i - 1].substring(0, it.effect[i - 1].indexOf(','));
            e = Integer.parseInt(it.effect[i - 1].substring(it.effect[i - 1].indexOf(',') + 1));
            whataffected.toLowerCase();
            if (whataffected.equals("mana")) {
                maxmana -= e;
                if (mana > maxmana) mana = maxmana;
            } else if (whataffected.equals("health")) {
                maxhealth -= e;
                if (health > maxhealth) health = maxhealth;
            } else if (whataffected.equals("stamina")) {
                maxstamina -= e;
                if (stamina > maxstamina) stamina = maxstamina;
            } else if (whataffected.equals("strength")) strength -= e; else if (whataffected.equals("vitality")) vitality -= e; else if (whataffected.equals("dexterity")) dexterity -= e; else if (whataffected.equals("intelligence")) intelligence -= e; else if (whataffected.equals("wisdom")) wisdom -= e; else if (whataffected.equals("flevel")) {
                flevel -= it.fleveladded;
            } else if (whataffected.equals("nlevel")) {
                nlevel -= it.nleveladded;
            } else if (whataffected.equals("wlevel")) {
                wlevel -= it.wleveladded;
            } else if (whataffected.equals("plevel")) {
                plevel -= it.pleveladded;
            }
        }
        setMaxLoad();
    }

    public void setMaxLoad() {
        maxload = strength * 4 / 5;
        if (stamina < maxstamina / 5) maxload = maxload * 2 / 3; else if (stamina < maxstamina / 3) maxload = maxload * 4 / 5;
    }

    public void save(ObjectOutputStream so) throws IOException {
        so.writeUTF(picname);
        so.writeInt(subsquare);
        so.writeInt(number);
        so.writeUTF(name);
        so.writeUTF(lastname);
        so.writeInt(maxhealth);
        so.writeInt(health);
        so.writeInt(maxstamina);
        so.writeInt(stamina);
        so.writeInt(maxmana);
        so.writeInt(mana);
        so.writeFloat(load);
        so.writeInt(food);
        so.writeInt(water);
        so.writeInt(strength);
        so.writeInt(vitality);
        so.writeInt(dexterity);
        so.writeInt(intelligence);
        so.writeInt(wisdom);
        so.writeInt(defense);
        so.writeInt(magicresist);
        so.writeInt(strengthboost);
        so.writeInt(vitalityboost);
        so.writeInt(dexterityboost);
        so.writeInt(intelligenceboost);
        so.writeInt(wisdomboost);
        so.writeInt(defenseboost);
        so.writeInt(magicresistboost);
        so.writeInt(flevel);
        so.writeInt(nlevel);
        so.writeInt(plevel);
        so.writeInt(wlevel);
        so.writeInt(flevelboost);
        so.writeInt(nlevelboost);
        so.writeInt(plevelboost);
        so.writeInt(wlevelboost);
        so.writeInt(fxp);
        so.writeInt(nxp);
        so.writeInt(pxp);
        so.writeInt(wxp);
        so.writeBoolean(isdead);
        so.writeBoolean(wepready);
        so.writeBoolean(ispoisoned);
        if (ispoisoned) {
            so.writeInt(poison);
            so.writeInt(poisoncounter);
        }
        so.writeBoolean(silenced);
        if (silenced) so.writeInt(silencecount);
        so.writeBoolean(hurtweapon);
        so.writeBoolean(hurthand);
        so.writeBoolean(hurthead);
        so.writeBoolean(hurttorso);
        so.writeBoolean(hurtlegs);
        so.writeBoolean(hurtfeet);
        so.writeInt(timecounter);
        so.writeInt(walkcounter);
        so.writeInt(spellcount);
        so.writeInt(weaponcount);
        so.writeInt(kuswordcount);
        so.writeInt(rosbowcount);
        so.writeUTF(currentspell);
        if (abilities != null) {
            so.writeInt(abilities.length);
            for (int j = 0; j < abilities.length; j++) {
                abilities[j].save(so);
            }
        } else so.writeInt(0);
        if (weapon.name.equals("Fist/Foot")) so.writeBoolean(false); else {
            so.writeBoolean(true);
            so.writeObject(weapon);
        }
        if (hand == null) so.writeBoolean(false); else {
            so.writeBoolean(true);
            so.writeObject(hand);
        }
        if (head == null) so.writeBoolean(false); else {
            so.writeBoolean(true);
            so.writeObject(head);
        }
        if (torso == null) so.writeBoolean(false); else {
            so.writeBoolean(true);
            so.writeObject(torso);
        }
        if (legs == null) so.writeBoolean(false); else {
            so.writeBoolean(true);
            so.writeObject(legs);
        }
        if (feet == null) so.writeBoolean(false); else {
            so.writeBoolean(true);
            so.writeObject(feet);
        }
        if (neck == null) so.writeBoolean(false); else {
            so.writeBoolean(true);
            so.writeObject(neck);
        }
        if (pouch1 == null) so.writeBoolean(false); else {
            so.writeBoolean(true);
            so.writeObject(pouch1);
        }
        if (pouch2 == null) so.writeBoolean(false); else {
            so.writeBoolean(true);
            so.writeObject(pouch2);
        }
        so.writeObject(quiver);
        so.writeObject(pack);
    }

    public void load(ObjectInputStream si) throws IOException, ClassNotFoundException {
        subsquare = si.readInt();
        number = si.readInt();
        name = si.readUTF();
        lastname = si.readUTF();
        maxhealth = si.readInt();
        health = si.readInt();
        maxstamina = si.readInt();
        stamina = si.readInt();
        maxmana = si.readInt();
        mana = si.readInt();
        load = si.readFloat();
        food = si.readInt();
        water = si.readInt();
        strength = si.readInt();
        vitality = si.readInt();
        dexterity = si.readInt();
        intelligence = si.readInt();
        wisdom = si.readInt();
        defense = si.readInt();
        magicresist = si.readInt();
        strengthboost = si.readInt();
        vitalityboost = si.readInt();
        dexterityboost = si.readInt();
        intelligenceboost = si.readInt();
        wisdomboost = si.readInt();
        defenseboost = si.readInt();
        magicresistboost = si.readInt();
        flevel = si.readInt();
        nlevel = si.readInt();
        plevel = si.readInt();
        wlevel = si.readInt();
        flevelboost = si.readInt();
        nlevelboost = si.readInt();
        plevelboost = si.readInt();
        wlevelboost = si.readInt();
        fxp = si.readInt();
        nxp = si.readInt();
        pxp = si.readInt();
        wxp = si.readInt();
        isdead = si.readBoolean();
        wepready = si.readBoolean();
        ispoisoned = si.readBoolean();
        if (ispoisoned) {
            poison = si.readInt();
            poisoncounter = si.readInt();
        }
        silenced = si.readBoolean();
        if (silenced) silencecount = si.readInt();
        hurtweapon = si.readBoolean();
        hurthand = si.readBoolean();
        hurthead = si.readBoolean();
        hurttorso = si.readBoolean();
        hurtlegs = si.readBoolean();
        hurtfeet = si.readBoolean();
        timecounter = si.readInt();
        walkcounter = si.readInt();
        spellcount = si.readInt();
        weaponcount = si.readInt();
        kuswordcount = si.readInt();
        rosbowcount = si.readInt();
        currentspell = si.readUTF();
        int numabils = si.readInt();
        if (numabils > 0) {
            abilities = new SpecialAbility[numabils];
            for (int j = 0; j < numabils; j++) {
                abilities[j] = new SpecialAbility(si);
            }
        }
        if (si.readBoolean()) {
            weapon = (Item) si.readObject();
            if (weapon.number == 9) ((Torch) weapon).setPic();
            if (weapon.number == 6) weapon = Item.fistfoot;
        } else weapon = Item.fistfoot;
        if (si.readBoolean()) {
            hand = (Item) si.readObject();
            if (hand.number == 9) ((Torch) hand).setPic();
        }
        if (si.readBoolean()) head = (Item) si.readObject();
        if (si.readBoolean()) torso = (Item) si.readObject();
        if (si.readBoolean()) legs = (Item) si.readObject();
        if (si.readBoolean()) feet = (Item) si.readObject();
        if (si.readBoolean()) {
            neck = (Item) si.readObject();
        }
        if (si.readBoolean()) pouch1 = (Item) si.readObject();
        if (si.readBoolean()) pouch2 = (Item) si.readObject();
        quiver = (Item[]) si.readObject();
        pack = (Item[]) si.readObject();
        setMaxLoad();
    }
}
