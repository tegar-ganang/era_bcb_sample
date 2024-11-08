package pcgen.CharacterViewer.resources;

import java.io.File;
import java.io.FileInputStream;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import pcgen.android.Logger;
import android.os.Parcel;
import android.os.Parcelable;
import pcgen.CharacterViewer.resources.GameResourceAttribute.Types;

public class GameResourceCharacter extends GameResource {

    public GameResourceCharacter() {
    }

    public GameResourceCharacter(Parcel in) {
        super(in);
    }

    public GameResourceCharacter(String name, String location, ResourceTypes type) {
        super(name, location, type);
    }

    public GameResourceAttributeAbility getAbilityById(int id) {
        switch(id) {
            case GameAbilities.Strength:
                return getAbilityStr();
            case GameAbilities.Dexterity:
                return getAbilityDex();
            case GameAbilities.Constitution:
                return getAbilityCon();
            case GameAbilities.Intelligence:
                return getAbilityInt();
            case GameAbilities.Wisdom:
                return getAbilityWis();
            case GameAbilities.Charisma:
                return getAbilityCha();
        }
        return null;
    }

    public GameResourceAttributeAbility getAbilityCha() {
        return _abilityCha;
    }

    public GameResourceAttributeAbility getAbilityCon() {
        return _abilityCon;
    }

    public GameResourceAttributeAbility getAbilityDex() {
        return _abilityDex;
    }

    public GameResourceAttributeAbility getAbilityInt() {
        return _abilityInt;
    }

    public GameResourceAttributeAbility getAbilityStr() {
        return _abilityStr;
    }

    public GameResourceAttributeAbility getAbilityWis() {
        return _abilityWis;
    }

    public int getAttackBase() {
        return _attackBase;
    }

    public GameResourceAttributeAttack getAttackGrapple() {
        return _attackGrapple;
    }

    public GameResourceAttributeAttack getAttackMelee() {
        return _attackMelee;
    }

    public GameResourceAttributeAttack getAttackRanged() {
        return _attackRanged;
    }

    public GameResourceAttributeAttack getAttackUnarmed() {
        return _attackUnarmed;
    }

    public CombatStates getCombatState() {
        return _combatState;
    }

    public ArrayList<GameResourceCondition> getConditionalModifiers() {
        ArrayList<GameResourceCondition> list = new ArrayList<GameResourceCondition>();
        for (GameResourceCondition condition : _condition) {
            if (!condition.isVisible()) continue;
            list.add(condition);
        }
        return list;
    }

    public GameResourceAttributeDamage getDamagePrimary() {
        return _damagePrimary;
    }

    public int getDamagePrimaryCurrent() {
        return _damagePrimary.getValue();
    }

    public int getDamagePrimaryMax() {
        return _damagePrimary.getValueMax();
    }

    public int getDamagePrimaryMaxDamage() {
        return _damagePrimary.getValueMaxDamage();
    }

    public int getDamagePrimaryMaxDamageAdjustment() {
        return _damagePrimary.getValueMaxDamageAdjustment();
    }

    public int getDamagePrimaryMaxHeal() {
        return _damagePrimary.getValueMaxHeal();
    }

    public int getDamagePrimaryMaxHealAdjustment() {
        return _damagePrimary.getValueMaxHealAdjustment();
    }

    public int getDamagePrimaryMin() {
        return _damagePrimary.getValueMin();
    }

    public int getDamagePrimaryMinDamage() {
        return _damagePrimary.getValueMin();
    }

    public int getDamagePrimaryTemp() {
        return _damagePrimary.getValueTemp();
    }

    public GameResourceAttributeDamage getDamageReduction() {
        return _damageReduction;
    }

    public GameResourceAttributeDamage getDamageSecondary() {
        return _damageSecondary;
    }

    public int getDamageSecondaryCurrent() {
        return _damageSecondary.getValue();
    }

    public int getDamageSecondaryMax() {
        return _damageSecondary.getValueMax();
    }

    public int getDamageSecondaryMaxDamage() {
        return _damageSecondary.getValueMaxDamage();
    }

    public int getDamageSecondaryMaxDamageAdjustment() {
        return _damageSecondary.getValueMaxDamageAdjustment();
    }

    public int getDamageSecondaryMaxHeal() {
        return _damageSecondary.getValueMaxHeal();
    }

    public int getDamageSecondaryMaxHealAdjustment() {
        return _damageSecondary.getValueMaxHealAdjustment();
    }

    public int getDamageSecondaryMin() {
        return _damageSecondary.getValueMin();
    }

    public int getDamageSecondaryMinDamage() {
        return _damageSecondary.getValueMinDamage();
    }

    public int getDamageSecondaryTemp() {
        return _damageSecondary.getValueTemp();
    }

    public GameResourceAttributeDefense getDefense() {
        return _defense;
    }

    public int getInitiative() {
        return _initiativeCurrent + getInitiativeModifier();
    }

    public int getInitiativeModifier() {
        return getInitiativeStatModifier() + _initiativeModifier;
    }

    public int getInitiativeStat() {
        return _abilityDex.getValue();
    }

    public int getInitiativeStatModifier() {
        return _abilityDex.getAbilityModifier();
    }

    public String getGameType() {
        return _type;
    }

    public int getLevel() {
        return _level;
    }

    public GameResourceNote getNote(int type) {
        for (GameResourceNote note : _notes) {
            if (note.isType(type)) return note;
        }
        return null;
    }

    public GameResourceAttribute getSaveById(int id) {
        switch(id) {
            case 0:
                return getSaveFortitude();
            case 1:
                return getSaveReflex();
            case 2:
                return getSaveWill();
        }
        return null;
    }

    public GameResourceAttribute getSaveFortitude() {
        return _saveFort;
    }

    public GameResourceAttribute getSaveReflex() {
        return _saveRef;
    }

    public GameResourceAttribute getSaveWill() {
        return _saveWill;
    }

    public GameResourceAttributeSkill getSkill(int index) {
        if ((index >= 0) && (index < _skills.size())) return _skills.get(index);
        return null;
    }

    public GameResourceAttributeSkill getSkill(String name) {
        for (GameResourceAttributeSkill skill : _skills) {
            if (skill.getName().equalsIgnoreCase(name)) return skill;
        }
        return null;
    }

    public GameResourceAttributeSkill getSkill(GameResourceAttributeSkill.SkillTypes type) {
        for (GameResourceAttributeSkill skill : _skills) {
            if (skill.getSkillType() == type) return skill;
        }
        return null;
    }

    public GameResourceAttributeSkillList getSkills() {
        return _skills;
    }

    public String getSize() {
        return _size;
    }

    public int getSizeBonus() {
        return getFolder().getSizeBonus(_size);
    }

    public GameResourceWeapon getWeapon(int index) {
        if ((index >= 0) && (index < _weapons.size())) return _weapons.get(index);
        return null;
    }

    public GameResourceWeaponList getWeapons() {
        return _weapons;
    }

    public boolean hasCondition(int type) {
        for (GameResourceCondition condition : _condition) {
            if (condition.isType(type)) return true;
        }
        return false;
    }

    public boolean hasNote(int type) {
        for (GameResourceNote note : _notes) {
            if (note.isType(type)) return true;
        }
        return false;
    }

    @Override
    public void init(GameFolder folder) {
        super.init(folder);
        initAttributes();
    }

    public void initFromLocation() throws Throwable {
        try {
            StringBuffer output = new StringBuffer();
            File file = new File(getLocation());
            FileChannel inChannel = null;
            try {
                Charset encoding = Charset.forName("UTF-8");
                ByteBuffer buf = ByteBuffer.allocate(4096);
                inChannel = (new FileInputStream(file)).getChannel();
                while (inChannel.read(buf) != -1) {
                    buf.rewind();
                    output.append(encoding.decode(buf).toString());
                    buf.clear();
                }
            } catch (Exception e) {
            } finally {
                try {
                    if (inChannel != null) inChannel.close();
                } catch (Exception e) {
                }
            }
            String content = output.toString();
            if (StringUtils.isEmpty(content)) return;
            if (_regexData == null) _regexData = Pattern.compile("<DataBlock>(.*?)</DataBlock>", Pattern.DOTALL);
            Matcher matcher = _regexData.matcher(content);
            if (matcher.find()) {
                try {
                    String data = matcher.group(1);
                    DocumentBuilder db = (DocumentBuilderFactory.newInstance()).newDocumentBuilder();
                    InputSource source = new InputSource();
                    source.setCharacterStream(new StringReader("<document>" + data + "</document>"));
                    Document doc = db.parse(source);
                    Element root = getElement(doc.getDocumentElement(), "/document");
                    setGameType(getElementAttribute(root, "game", "type"));
                    setLevel(getElementAttributeAsInt(root, "level", "value"));
                    setSize(getElementAttribute(root, "size", "value"));
                    _abilityStr.setValue(getElementAttributeAsInt(root, "abilities/str", "value"), Types.Ability);
                    _abilityDex.setValue(getElementAttributeAsInt(root, "abilities/dex", "value"), Types.Ability);
                    _abilityCon.setValue(getElementAttributeAsInt(root, "abilities/con", "value"), Types.Ability);
                    _abilityInt.setValue(getElementAttributeAsInt(root, "abilities/int", "value"), Types.Ability);
                    _abilityWis.setValue(getElementAttributeAsInt(root, "abilities/wis", "value"), Types.Ability);
                    _abilityCha.setValue(getElementAttributeAsInt(root, "abilities/cha", "value"), Types.Ability);
                    _saveFort.setValue(getElementAttributeAsInt(root, "saves/fort", "value"), getElementAttributeAsInt(root, "saves/fort", "misc"), GameAbilities.Constitution, Types.Save);
                    _saveRef.setValue(getElementAttributeAsInt(root, "saves/ref", "value"), getElementAttributeAsInt(root, "saves/ref", "misc"), GameAbilities.Dexterity, Types.Save);
                    _saveWill.setValue(getElementAttributeAsInt(root, "saves/will", "value"), getElementAttributeAsInt(root, "saves/will", "misc"), GameAbilities.Wisdom, Types.Save);
                    setInitiativeModifier(getElementAttributeAsInt(root, "initiative", "value"));
                    setAttackBase(getElementAttributeAsInt(root, "attacks", "base"));
                    _attackGrapple.setValue(getElementAttributeAsInt(root, "attacks/grapple", "value"), GameResourceAttributeAttack.ATTACK_TYPE_MELEE, GameResourceAttributeAttack.ATTACK_TYPE_SECONDARY_NONE);
                    _attackMelee.setValue(getElementAttributeAsInt(root, "attacks/melee", "value"), GameResourceAttributeAttack.ATTACK_TYPE_MELEE, GameResourceAttributeAttack.ATTACK_TYPE_SECONDARY_NONE);
                    _attackRanged.setValue(getElementAttributeAsInt(root, "attacks/ranged", "value"), GameResourceAttributeAttack.ATTACK_TYPE_RANGED, GameResourceAttributeAttack.ATTACK_TYPE_SECONDARY_NONE);
                    _attackUnarmed.setValue(getElementAttributeAsInt(root, "attacks/unarmed", "value"), GameResourceAttributeAttack.ATTACK_TYPE_MELEE, GameResourceAttributeAttack.ATTACK_TYPE_SECONDARY_UNARMED);
                    _attackUnarmed.setFinessable(getElementAttributeAsInt(root, "attacks/unarmed", "finessable") > 0 ? true : false);
                    _attackUnarmed.setDamage(getElementAttribute(root, "attacks/unarmed", "damage_base"));
                    _attackUnarmed.setMisc(getElementAttributeAsInt(root, "attacks/unarmed", "damage_bonus"));
                    if (!_reload) {
                        _damagePrimary.setValueCurrent(getElementAttributeAsInt(root, "damage/primary", "current"));
                        _damagePrimary.setValue(getElementAttributeAsInt(root, "damage/primary", "value"));
                        _damageSecondary.setValueCurrent(getElementAttributeAsInt(root, "damage/secondary", "current"));
                        _damageSecondary.setValue(getElementAttributeAsInt(root, "damage/secondary", "value"));
                        _damagePrimary.setSecondary(_damageSecondary);
                        _damageReduction.setValue(getElementAttributeAsInt(root, "damage", "dr"));
                    }
                    _defense.setValue(getElementAttributeAsInt(root, "defense/primary", "value"));
                    _defense.setValueFlatFooted(getElementAttributeAsInt(root, "defense/flatfooted", "value"));
                    _defense.setValueTouch(getElementAttributeAsInt(root, "defense/touch", "value"));
                    _defense.setDexModifierIncludeFlatFooted(getElementAttributeAsInt(root, "defense/flatfooted", "include_dex_modifier"));
                    _defense.setDexModifierMax(getElementAttributeAsInt(root, "defense", "max_dex_modifier"));
                    Element skills = getElement(root, "skills");
                    NodeList nodes = skills.getChildNodes();
                    nodes = (NodeList) _xpath.evaluate("skills/skill", root, XPathConstants.NODESET);
                    for (int i = 0; i < nodes.getLength(); i++) {
                        Element element = (Element) nodes.item(i);
                        GameResourceAttributeSkill skill = new GameResourceAttributeSkill();
                        skill.setCharacter(this);
                        skill.setValue(element.getTextContent(), getElementAttribute(element, "ability"), getElementAttributeAsDecimal(element, "rank"), getElementAttributeAsInt(element, "misc"), getElementAttributeAsBoolean(element, "untrained"));
                        _skills.add(skill);
                    }
                    nodes = (NodeList) _xpath.evaluate("attacks/weapon", root, XPathConstants.NODESET);
                    for (int i = 0; i < nodes.getLength(); i++) {
                        Element element = (Element) nodes.item(i);
                        GameResourceWeapon weapon = new GameResourceWeapon();
                        weapon.setCharacter(this);
                        weapon.setValue(element.getTextContent(), getElementAttributeAsInt(element, "melee"), getElementAttributeAsInt(element, "ranged"), getElementAttributeAsInt(element, "hands"), getElementAttributeAsInt(element, "finessable"), getElementAttributeAsInt(element, "light"), getElementAttributeAsInt(element, "hit_bonus"), getElementAttributeAsInt(element, "twoweapon_modifier_primary"), getElementAttributeAsInt(element, "twoweapon_modifier_secondary"), getElementAttributeAsInt(element, "attack_separator"), getElementAttribute(element, "damage_base"), getElementAttributeAsInt(element, "damage_bonus"));
                        _weapons.add(weapon);
                    }
                    initAttributes();
                } catch (ParserConfigurationException e) {
                    Logger.e(TAG, "init - XML parse error: " + e.getMessage());
                    throw new GameResourceLoadException();
                } catch (SAXException e) {
                    Logger.e(TAG, "init - Wrong XML file structure: " + e.getMessage());
                    throw new GameResourceLoadException();
                }
            }
        } catch (Throwable tr) {
            Logger.e(TAG, "init", tr);
            throw tr;
        }
    }

    public JSONObject load(JSONObject data) throws JSONException {
        super.load(data);
        setGameType(getJSONString(data, "gameType"));
        setSize(getJSONString(data, "size"));
        setLevel(getJSONInt(data, "level"));
        setCombatState(getJSONString(data, "combatState"));
        setInitiativeCurrent(getJSONInt(data, "initiativeCurrent"));
        setInitiativeModifier(getJSONInt(data, "initiativeModifier"));
        _abilityStr.load(getJSONObject(data, "abilityStr"));
        _abilityDex.load(getJSONObject(data, "abilityDex"));
        _abilityCon.load(getJSONObject(data, "abilityCon"));
        _abilityInt.load(getJSONObject(data, "abilityInt"));
        _abilityWis.load(getJSONObject(data, "abilityWis"));
        _abilityCha.load(getJSONObject(data, "abilityCha"));
        setAttackBase(getJSONInt(data, "attackBase"));
        _attackGrapple.load(getJSONObject(data, "attackGrapple"));
        _attackMelee.load(getJSONObject(data, "attackMelee"));
        _attackRanged.load(getJSONObject(data, "attackRanged"));
        _attackUnarmed.load(getJSONObject(data, "attackUnarmed"));
        _damagePrimary.load(getJSONObject(data, "damagePrimary"));
        _damageReduction.load(getJSONObject(data, "damageReduction"));
        _damageSecondary.load(getJSONObject(data, "damageSecondary"));
        _damagePrimary.setSecondary(_damageSecondary);
        _defense.load(getJSONObject(data, "defense"));
        _saveFort.load(getJSONObject(data, "saveFort"));
        _saveRef.load(getJSONObject(data, "saveRef"));
        _saveWill.load(getJSONObject(data, "saveWill"));
        _saveDamage.load(getJSONObject(data, "saveDamage"));
        _skills.clear();
        JSONArray skills = data.getJSONArray("skills");
        for (int index = 0; index < skills.length(); index++) {
            if (skills.isNull(index)) continue;
            JSONObject itemJSON = skills.getJSONObject(index);
            GameResourceAttributeSkill item = new GameResourceAttributeSkill();
            item.load(itemJSON);
            _skills.add(item);
        }
        _weapons.clear();
        JSONArray weapons = data.getJSONArray("skills");
        for (int index = 0; index < weapons.length(); index++) {
            if (weapons.isNull(index)) continue;
            JSONObject itemJSON = weapons.getJSONObject(index);
            GameResourceWeapon item = new GameResourceWeapon();
            item.load(itemJSON);
            _weapons.add(item);
        }
        _condition.clear();
        JSONArray condition = data.getJSONArray("condition");
        for (int index = 0; index < condition.length(); index++) {
            if (condition.isNull(index)) continue;
            JSONObject itemJSON = condition.getJSONObject(index);
            GameResourceCondition item = new GameResourceCondition();
            item.load(itemJSON);
            _condition.add(item);
        }
        _notes.clear();
        JSONArray note = data.getJSONArray("notes");
        for (int index = 0; index < note.length(); index++) {
            if (note.isNull(index)) continue;
            JSONObject itemJSON = note.getJSONObject(index);
            GameResourceNote item = new GameResourceNote();
            item.load(itemJSON);
            _notes.add(item);
        }
        initAttributes();
        return data;
    }

    public void onAttributeChanged() {
        if (getFolder().getSettingsDamageTypeHitPoints()) {
            int half = getDamagePrimaryMax() / 2;
            int current = getDamagePrimaryCurrent();
            int currentSecondary = getDamageSecondaryCurrent();
            if (current <= getDamagePrimaryMin()) setConditionsForDeath(); else if (current < 0) setConditionsForDying(); else if (current == 0) setConditionsForDisabled(); else {
                if (getFolder().getSettingsDamageTypeHitPoints()) {
                    if (current <= half) setCondition(GameTypesCondition.Bloodied); else removeCondition(GameTypesCondition.Bloodied);
                }
                if (current == currentSecondary) setConditionsForStaggered(); else if (current < currentSecondary) setConditionsForUnconscious(); else setConditionsForAlive();
            }
        } else if (getFolder().getSettingsDamageTypeVitalityPoints()) {
            int current = getDamageSecondaryCurrent();
            if (current <= getDamageSecondaryMin()) setConditionsForDeath(); else if (current < 0) setConditionsForDying(); else if (current == 0) setConditionsForDisabled(); else setConditionsForAlive();
        }
        removeCondition(GameTypesCondition.TemporaryDamaged);
        removeCondition(GameTypesCondition.TemporaryHealed);
    }

    public void modifyDamagePrimaryCurrentDamage(int value) {
        _damagePrimary.modifyValueCurrentDamage(value);
    }

    public void modifyDamagePrimaryCurrentHealing(int value, boolean special) {
        _damagePrimary.modifyValueCurrentHealing(value, special);
    }

    public void modifyDamageSecondaryCurrentDamage(int value) {
        _damageSecondary.modifyValueCurrentDamage(value);
    }

    public void modifyDamageSecondaryCurrentHealing(int value, boolean special) {
        _damageSecondary.modifyValueCurrentHealing(value, special);
    }

    public void reload() throws Throwable {
        try {
            _reload = true;
            initFromLocation();
        } finally {
            _reload = false;
        }
    }

    public void removeCondition(int type) {
        if (!hasCondition(type)) return;
        int index = 0;
        for (GameResourceCondition condition : _condition) {
            if (condition.isType(type)) break;
            index++;
        }
        _condition.remove(index);
    }

    public JSONObject save() throws JSONException {
        JSONObject data = super.save();
        data.put("gameType", _type);
        data.put("level", _level);
        data.put("initiativeCurrent", _initiativeCurrent);
        data.put("initiativeModifier", _initiativeModifier);
        data.put("abilityStr", _abilityStr.save());
        data.put("abilityDex", _abilityDex.save());
        data.put("abilityCon", _abilityCon.save());
        data.put("abilityInt", _abilityInt.save());
        data.put("abilityWis", _abilityWis.save());
        data.put("abilityCha", _abilityCha.save());
        data.put("attackBase", _attackBase);
        data.put("attackGrapple", _attackGrapple.save());
        data.put("attackMelee", _attackMelee.save());
        data.put("attackRanged", _attackRanged.save());
        data.put("attackUnarmed", _attackUnarmed.save());
        data.put("combatState", _combatState.toString());
        data.put("damagePrimary", _damagePrimary.save());
        data.put("damageReduction", _damageReduction.save());
        data.put("damageSecondary", _damageSecondary.save());
        data.put("defense", _defense.save());
        data.put("saveFort", _saveFort.save());
        data.put("saveRef", _saveRef.save());
        data.put("saveWill", _saveWill.save());
        data.put("saveDamage", _saveDamage.save());
        data.put("size", _size);
        JSONArray skillsJSON = new JSONArray();
        for (GameResourceAttributeSkill item : _skills) {
            JSONObject itemJSON = item.save();
            skillsJSON.put(itemJSON);
        }
        data.put("skills", skillsJSON);
        JSONArray weaponsJSON = new JSONArray();
        for (GameResourceWeapon item : _weapons) {
            JSONObject itemJSON = item.save();
            weaponsJSON.put(itemJSON);
        }
        data.put("weapons", weaponsJSON);
        JSONArray conditionJSON = new JSONArray();
        for (GameResourceCondition item : _condition) {
            JSONObject itemJSON = item.save();
            conditionJSON.put(itemJSON);
        }
        data.put("condition", conditionJSON);
        JSONArray noteJSON = new JSONArray();
        for (GameResourceNote item : _notes) {
            JSONObject itemJSON = item.save();
            noteJSON.put(itemJSON);
        }
        data.put("notes", noteJSON);
        return data;
    }

    public void setAttackBase(int value) {
        _attackBase = value;
    }

    public void setCombatState(CombatStates state) {
        _combatState = state;
    }

    public void setCombatState(String state) {
        if (StringUtils.isEmpty(state)) {
            _combatState = CombatStates.Normal;
            return;
        }
        _combatState = CombatStates.valueOf(state);
        ;
    }

    public void setCondition(int type) {
        if (hasCondition(type)) return;
        if (type == GameTypesCondition.Fatigued) {
            if (hasCondition(GameTypesCondition.Exhausted)) return;
        }
        if (type == GameTypesCondition.Exhausted) {
            if (hasCondition(GameTypesCondition.Fatigued)) removeCondition(GameTypesCondition.Fatigued);
        }
        _condition.add(new GameResourceCondition(this, type));
    }

    public void setConditionsForAlive() {
        removeCondition(GameTypesCondition.Dead);
        removeCondition(GameTypesCondition.Disabled);
        removeCondition(GameTypesCondition.Dying);
        removeCondition(GameTypesCondition.Stable);
        removeCondition(GameTypesCondition.Staggered);
        removeCondition(GameTypesCondition.Unconscious);
        setCondition(GameTypesCondition.Alive);
    }

    public void setConditionsForDeath() {
        removeCondition(GameTypesCondition.Alive);
        removeCondition(GameTypesCondition.Blinded);
        removeCondition(GameTypesCondition.Cowering);
        removeCondition(GameTypesCondition.Dazed);
        removeCondition(GameTypesCondition.Dazzeled);
        removeCondition(GameTypesCondition.Deafened);
        removeCondition(GameTypesCondition.Diseased);
        removeCondition(GameTypesCondition.Disabled);
        removeCondition(GameTypesCondition.Dying);
        removeCondition(GameTypesCondition.Exhausted);
        removeCondition(GameTypesCondition.Fascinated);
        removeCondition(GameTypesCondition.Fatigued);
        removeCondition(GameTypesCondition.Frightened);
        removeCondition(GameTypesCondition.Nauseated);
        removeCondition(GameTypesCondition.Panicked);
        removeCondition(GameTypesCondition.Paralyzed);
        removeCondition(GameTypesCondition.Poisoned);
        removeCondition(GameTypesCondition.Shaken);
        removeCondition(GameTypesCondition.Sickened);
        removeCondition(GameTypesCondition.Stable);
        removeCondition(GameTypesCondition.Staggered);
        removeCondition(GameTypesCondition.Stunned);
        removeCondition(GameTypesCondition.Unconscious);
        setCondition(GameTypesCondition.Dead);
    }

    public void setConditionsForDisabled() {
        removeCondition(GameTypesCondition.Dead);
        removeCondition(GameTypesCondition.Dying);
        removeCondition(GameTypesCondition.Stable);
        removeCondition(GameTypesCondition.Staggered);
        removeCondition(GameTypesCondition.Unconscious);
        setCondition(GameTypesCondition.Alive);
        setCondition(GameTypesCondition.Disabled);
    }

    public void setConditionsForDying() {
        if (getFolder().getSettingsDamageTypeVitalityPoints()) {
            if (hasCondition(GameTypesCondition.TemporaryDamaged)) setCondition(GameTypesCondition.Fatigued);
        }
        setCondition(GameTypesCondition.Alive);
        setCondition(GameTypesCondition.Dying);
        removeCondition(GameTypesCondition.Stable);
        if (hasCondition(GameTypesCondition.TemporaryHealed)) setCondition(GameTypesCondition.Stable);
    }

    public void setConditionsForStaggered() {
        setCondition(GameTypesCondition.Alive);
        setCondition(GameTypesCondition.Staggered);
    }

    public void setConditionsForUnconscious() {
        removeCondition(GameTypesCondition.Dazed);
        removeCondition(GameTypesCondition.Dead);
        removeCondition(GameTypesCondition.Dying);
        removeCondition(GameTypesCondition.Shaken);
        removeCondition(GameTypesCondition.Stable);
        removeCondition(GameTypesCondition.Staggered);
        setCondition(GameTypesCondition.Alive);
        setCondition(GameTypesCondition.Unconscious);
    }

    public void setInitiativeCurrent(int value) {
        _initiativeCurrent = value;
    }

    public void setInitiativeModifier(int value) {
        _initiativeModifier = value;
    }

    public void setGameType(String type) {
        _type = type;
    }

    public void setLevel(int level) {
        _level = level;
    }

    public void setNote(int type, String value) {
        GameResourceNote note = getNote(type);
        if (note == null) {
            note = new GameResourceNote(type);
            note.setCharacter(this);
        }
        note.setText(value);
        _notes.add(note);
    }

    public void setSize(String value) {
        _size = value;
    }

    public static final Parcelable.Creator<GameResource> CREATOR = new Parcelable.Creator<GameResource>() {

        public GameResource createFromParcel(Parcel in) {
            return new GameResource(in);
        }

        public GameResource[] newArray(int size) {
            return new GameResource[size];
        }
    };

    @Override
    protected void readFromParcelTransform(Parcel in) throws Throwable {
        try {
            super.readFromParcelTransform(in);
            setGameType(in.readString());
            setSize(in.readString());
            setLevel(in.readInt());
            setCombatState(in.readString());
            setInitiativeCurrent(in.readInt());
            setInitiativeModifier(in.readInt());
            setAttackBase(in.readInt());
            _abilityStr = in.readParcelable(GameResourceAttributeAbility.class.getClassLoader());
            _abilityDex = in.readParcelable(GameResourceAttributeAbility.class.getClassLoader());
            _abilityCon = in.readParcelable(GameResourceAttributeAbility.class.getClassLoader());
            _abilityInt = in.readParcelable(GameResourceAttributeAbility.class.getClassLoader());
            _abilityWis = in.readParcelable(GameResourceAttributeAbility.class.getClassLoader());
            _abilityCha = in.readParcelable(GameResourceAttributeAbility.class.getClassLoader());
            _attackGrapple = in.readParcelable(GameResourceAttributeAttack.class.getClassLoader());
            _attackMelee = in.readParcelable(GameResourceAttributeAttack.class.getClassLoader());
            _attackRanged = in.readParcelable(GameResourceAttributeAttack.class.getClassLoader());
            _attackUnarmed = in.readParcelable(GameResourceAttributeAttack.class.getClassLoader());
            _damagePrimary = in.readParcelable(GameResourceAttributeDamage.class.getClassLoader());
            _damageReduction = in.readParcelable(GameResourceAttributeDamage.class.getClassLoader());
            _damageSecondary = in.readParcelable(GameResourceAttributeDamage.class.getClassLoader());
            _damagePrimary.setSecondary(_damageSecondary);
            _defense = in.readParcelable(GameResourceAttributeDefense.class.getClassLoader());
            _saveFort = in.readParcelable(GameResourceAttribute.class.getClassLoader());
            _saveRef = in.readParcelable(GameResourceAttribute.class.getClassLoader());
            _saveWill = in.readParcelable(GameResourceAttribute.class.getClassLoader());
            _saveDamage = in.readParcelable(GameResourceAttribute.class.getClassLoader());
            _skills = in.readParcelable(GameResourceAttributeSkillList.class.getClassLoader());
            _weapons = in.readParcelable(GameResourceWeaponList.class.getClassLoader());
            _condition = in.readParcelable(GameResourceConditionList.class.getClassLoader());
            _notes = in.readParcelable(GameResourceNoteList.class.getClassLoader());
            initAttributes();
        } catch (Throwable tr) {
            Logger.e(TAG, "readFromParcelTransform", tr);
            throw tr;
        }
    }

    @Override
    protected void writeToParcelTransform(Parcel dest, int flags) throws Throwable {
        try {
            super.writeToParcelTransform(dest, flags);
            dest.writeString(_type);
            dest.writeString(_size);
            dest.writeInt(getLevel());
            dest.writeString(_combatState.toString());
            dest.writeInt(_initiativeCurrent);
            dest.writeInt(_initiativeModifier);
            dest.writeInt(_attackBase);
            dest.writeParcelable(_abilityStr, flags);
            dest.writeParcelable(_abilityDex, flags);
            dest.writeParcelable(_abilityCon, flags);
            dest.writeParcelable(_abilityInt, flags);
            dest.writeParcelable(_abilityWis, flags);
            dest.writeParcelable(_abilityCha, flags);
            dest.writeParcelable(_attackMelee, flags);
            dest.writeParcelable(_attackRanged, flags);
            dest.writeParcelable(_attackUnarmed, flags);
            dest.writeParcelable(_damagePrimary, flags);
            dest.writeParcelable(_damageReduction, flags);
            dest.writeParcelable(_damageSecondary, flags);
            dest.writeParcelable(_defense, flags);
            dest.writeParcelable(_saveFort, flags);
            dest.writeParcelable(_saveRef, flags);
            dest.writeParcelable(_saveWill, flags);
            dest.writeParcelable(_saveDamage, flags);
            dest.writeParcelable(_skills, flags);
            dest.writeParcelable(_weapons, flags);
            dest.writeParcelable(_condition, flags);
            dest.writeParcelable(_notes, flags);
        } catch (Throwable tr) {
            Logger.e(TAG, "writeToParcelTransform", tr);
            throw tr;
        }
    }

    private String getElementAttribute(Element element, String attribute) throws XPathExpressionException {
        if (!element.hasAttribute(attribute)) return "";
        return element.getAttribute(attribute);
    }

    private boolean getElementAttributeAsBoolean(Element element, String attribute) throws XPathExpressionException {
        int value = getElementAttributeAsInt(element, attribute);
        return (value == 1);
    }

    private double getElementAttributeAsDecimal(Element element, String attribute) throws XPathExpressionException {
        String value = getElementAttribute(element, attribute);
        value = value.replace("+", "");
        double output = 0;
        try {
            output = Double.valueOf(value);
        } catch (Exception ex) {
        }
        return output;
    }

    private int getElementAttributeAsInt(Element element, String attribute) throws XPathExpressionException {
        String value = getElementAttribute(element, attribute);
        value = value.replace("+", "");
        int output = 0;
        try {
            output = Integer.valueOf(value);
        } catch (Exception ex) {
        }
        return output;
    }

    private String getElementAttribute(Node node, String expression, String attribute) throws XPathExpressionException {
        if (_xpath == null) _xpath = XPathFactory.newInstance().newXPath();
        Element element = getElement(node, expression);
        if (element == null) return "";
        return getElementAttribute(element, attribute);
    }

    private Element getElement(Node node, String expression) throws XPathExpressionException {
        if (_xpath == null) _xpath = XPathFactory.newInstance().newXPath();
        Element element = (Element) _xpath.evaluate(expression, node, XPathConstants.NODE);
        return element;
    }

    private int getElementAttributeAsInt(Node node, String expression, String attribute) throws XPathExpressionException {
        String value = getElementAttribute(node, expression, attribute);
        if (StringUtils.isEmpty(value)) return 0;
        value = value.replace("+", "");
        int output = 0;
        try {
            output = Integer.valueOf(value);
        } catch (Exception ex) {
        }
        return output;
    }

    private String getElementAttributeByTagName(Element root, String name, String attribute) {
        Element element = getElementByTagName(root, name);
        if (element == null) return "";
        return element.getAttribute(attribute);
    }

    private int getElementAttributeByTagNameAsInt(Element root, String name, String attribute) {
        String value = getElementAttributeByTagName(root, name, attribute);
        if (StringUtils.isEmpty(value)) return 0;
        return Integer.valueOf(value);
    }

    private String getElementValueByTagName(Element root, String name) {
        Element element = getElementByTagName(root, name);
        if (element == null) return "";
        return element.getTextContent();
    }

    private int getElementValueByTagNameAsInt(Element root, String name) {
        String value = getElementValueByTagName(root, name);
        if (StringUtils.isEmpty(value)) return 0;
        return Integer.valueOf(value);
    }

    private Element getElementByTagName(Element root, String name) {
        NodeList list = root.getElementsByTagName(name);
        if (list.getLength() == 0) return null;
        return (Element) list.item(0);
    }

    private void initAttributes() {
        _abilityStr.setCharacter(this);
        _abilityDex.setCharacter(this);
        _abilityCon.setCharacter(this);
        _abilityInt.setCharacter(this);
        _abilityWis.setCharacter(this);
        _abilityCha.setCharacter(this);
        _saveFort.setCharacter(this);
        _saveRef.setCharacter(this);
        _saveWill.setCharacter(this);
        _saveDamage.setCharacter(this);
        _attackGrapple.setCharacter(this);
        _attackMelee.setCharacter(this);
        _attackRanged.setCharacter(this);
        _attackUnarmed.setCharacter(this);
        _damagePrimary.setCharacter(this);
        _damageReduction.setCharacter(this);
        _damageSecondary.setCharacter(this);
        _defense.setCharacter(this);
        for (GameResourceAttributeSkill item : _skills) item.setCharacter(this);
        for (GameResourceWeapon item : _weapons) item.setCharacter(this);
        for (GameResourceCondition item : _condition) item.setCharacter(this);
        for (GameResourceNote item : _notes) item.setCharacter(this);
    }

    private static Pattern _regexData = null;

    private XPath _xpath;

    private GameResourceHistoryList _history = new GameResourceHistoryList();

    private GameResourceAttributeAbility _abilityStr = new GameResourceAttributeAbility();

    private GameResourceAttributeAbility _abilityDex = new GameResourceAttributeAbility();

    private GameResourceAttributeAbility _abilityCon = new GameResourceAttributeAbility();

    private GameResourceAttributeAbility _abilityInt = new GameResourceAttributeAbility();

    private GameResourceAttributeAbility _abilityWis = new GameResourceAttributeAbility();

    private GameResourceAttributeAbility _abilityCha = new GameResourceAttributeAbility();

    private int _attackBase;

    private String _size;

    private GameResourceAttributeAttack _attackGrapple = new GameResourceAttributeAttack();

    private GameResourceAttributeAttack _attackMelee = new GameResourceAttributeAttack();

    private GameResourceAttributeAttack _attackRanged = new GameResourceAttributeAttack();

    private GameResourceAttributeAttack _attackUnarmed = new GameResourceAttributeAttack();

    private CombatStates _combatState = CombatStates.Normal;

    private GameResourceConditionList _condition = new GameResourceConditionList();

    private GameResourceAttributeDamage _damagePrimary = new GameResourceAttributeDamage(true);

    private GameResourceAttributeDamage _damageReduction = new GameResourceAttributeDamage(false);

    private GameResourceAttributeDamage _damageSecondary = new GameResourceAttributeDamage(false);

    private GameResourceAttributeDefense _defense = new GameResourceAttributeDefense();

    private int _initiativeCurrent;

    private int _initiativeModifier;

    private int _level;

    private GameResourceNoteList _notes = new GameResourceNoteList();

    private GameResourceAttribute _saveFort = new GameResourceAttribute();

    private GameResourceAttribute _saveRef = new GameResourceAttribute();

    private GameResourceAttribute _saveWill = new GameResourceAttribute();

    private GameResourceAttribute _saveDamage = new GameResourceAttribute();

    private GameResourceAttributeSkillList _skills = new GameResourceAttributeSkillList();

    private GameResourceWeaponList _weapons = new GameResourceWeaponList();

    private String _type;

    private boolean _reload;

    public enum CombatStates {

        Normal, FightingDefensively, TotalDefense
    }

    private static final String TAG = GameResourceCharacter.class.getSimpleName();
}
