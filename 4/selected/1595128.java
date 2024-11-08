package ch.zarzu.champions.builder.logic;

import java.util.LinkedList;
import java.util.HashMap;
import java.util.HashSet;
import java.io.*;
import org.w3c.dom.NodeList;
import ch.zarzu.champions.builder.*;
import ch.zarzu.champions.builder.gui.*;
import ch.zarzu.util.*;

public class BuildUpdater {

    LinkedList<Build> build_lists;

    Build build_powers;

    PowerList power_list;

    private MainInterface user_interface;

    private PrefLink pref_link;

    private SystemLink sys_link;

    private HashMap<String, HashSet<String>> filters;

    private String current_filter_type;

    Description description;

    AdvDescription adv_description;

    PowerData locked_power, description_power;

    HashMap<String, HashMap<String, PowerData>> advantage_map;

    HashMap<String, PowerData> power_data_map;

    HashMap<String, LinkedList<PowerListItem>> power_map;

    HashMap<String, Double> characteristics, base_characteristics;

    CharacterSheet char_sheet;

    private boolean frameworks_active;

    private PowerData current_framework;

    private static BuildUpdater instance = null;

    private BuildUpdater() {
    }

    public void initialize(HashMap<String, PowerData> p_data_map, HashMap<String, HashMap<String, PowerData>> a_map, LinkedList<Build> b_lists, Description d, AdvDescription ad) {
        power_data_map = p_data_map;
        advantage_map = a_map;
        build_lists = b_lists;
        build_powers = b_lists.get(0);
        description = d;
        adv_description = ad;
        char_sheet = CharacterSheet.getInstance();
        user_interface = MainInterface.getInstance();
        pref_link = PrefLink.getInstance();
        sys_link = SystemLink.getInstance();
        power_list = PowerList.getInstance();
        frameworks_active = true;
        current_framework = null;
        filters = new HashMap<String, HashSet<String>>();
        filters.put("powers", new HashSet<String>());
        filters.put("advantages", new HashSet<String>());
        filters.put("frameworks", new HashSet<String>());
        current_filter_type = "powers";
        characteristics = new HashMap<String, Double>();
        characteristics.put("level", 40.0);
        characteristics.put("adv_points", new Double(build_lists.get(0).getAdvantagePoints()));
        characteristics.put("max_health", 4120.0);
        characteristics.put("max_energy", 100.0);
        characteristics.put("strength", 5.0);
        characteristics.put("dexterity", 5.0);
        characteristics.put("constitution", 5.0);
        characteristics.put("endurance", 5.0);
        characteristics.put("recovery", 5.0);
        characteristics.put("intelligence", 5.0);
        characteristics.put("presence", 5.0);
        characteristics.put("ego", 5.0);
        characteristics.put("critical_chance", 0.0);
        characteristics.put("bonus_health", 0.0);
        characteristics.put("critical_severity", 100.0);
        characteristics.put("kb_strength", 0.0);
        characteristics.put("kb_resistance", 0.0);
        characteristics.put("power_recharge", 0.0);
        characteristics.put("threat_generation", 0.0);
        characteristics.put("energy_equilibrium", 0.0);
        characteristics.put("bonus_energy", 0.0);
        characteristics.put("damage_resistance", 0.0);
        characteristics.put("elemental_damage_resistance", 0.0);
        characteristics.put("fire_damage_resistance", 0.0);
        characteristics.put("cold_damage_resistance", 0.0);
        characteristics.put("toxic_damage_resistance", 0.0);
        characteristics.put("energy_damage_resistance", 0.0);
        characteristics.put("electrical_damage_resistance", 0.0);
        characteristics.put("sonic_damage_resistance", 0.0);
        characteristics.put("particle_damage_resistance", 0.0);
        characteristics.put("physical_damage_resistance", 0.0);
        characteristics.put("non_physical_damage_resistance", 0.0);
        characteristics.put("slashing_damage_resistance", 0.0);
        characteristics.put("crushing_damage_resistance", 0.0);
        characteristics.put("piercing_damage_resistance", 0.0);
        characteristics.put("paranormal_damage_resistance", 0.0);
        characteristics.put("ego_damage_resistance", 0.0);
        characteristics.put("dimensional_damage_resistance", 0.0);
        characteristics.put("magic_damage_resistance", 0.0);
        characteristics.put("damage_strength", 100.0);
        characteristics.put("elemental_damage_strength", 100.0);
        characteristics.put("fire_damage_strength", 100.0);
        characteristics.put("cold_damage_strength", 100.0);
        characteristics.put("toxic_damage_strength", 100.0);
        characteristics.put("energy_damage_strength", 100.0);
        characteristics.put("electrical_damage_strength", 100.0);
        characteristics.put("sonic_damage_strength", 100.0);
        characteristics.put("particle_damage_strength", 100.0);
        characteristics.put("physical_damage_strength", 100.0);
        characteristics.put("slashing_damage_strength", 100.0);
        characteristics.put("crushing_damage_strength", 100.0);
        characteristics.put("piercing_damage_strength", 100.0);
        characteristics.put("paranormal_damage_strength", 100.0);
        characteristics.put("ego_damage_strength", 100.0);
        characteristics.put("dimensional_damage_strength", 100.0);
        characteristics.put("magic_damage_strength", 100.0);
        base_characteristics = (HashMap<String, Double>) characteristics.clone();
        updateCharacteristics();
    }

    /**
	 * adds or removes a power
	 */
    public void addRemovePower(String id) {
        BuildPower active_slot = getActiveSlot();
        PowerData power = power_data_map.get(id);
        if (current_framework != null && active_slot.getLevel() == 1) {
            current_framework.setUsed(false);
            current_framework = null;
        }
        if (power.isUsed()) {
            for (Build b : build_lists) {
                if (b.isVisible()) {
                    for (BuildPower bp : b.getSlots()) {
                        if (bp.getId().equals(id)) {
                            bp.removePower();
                            break;
                        }
                    }
                }
            }
            power.setUsed(false);
        } else {
            if (!active_slot.isEmpty()) {
                active_slot.removePower();
            }
            active_slot.setPower(power);
            if (locked_power != null && active_slot.isPowerEqual(locked_power)) active_slot.setLockVisible(true);
            power.setUsed(true);
            jumpToEmpty();
        }
        updateCharacteristics();
        updateAdvantages();
        checkBuild();
        user_interface.setChanged(true);
    }

    /**
	 * return the slot which is active in the currently visible buildlist
	 */
    public BuildPower getActiveSlot() {
        BuildPower bp = null;
        for (Build b : build_lists) {
            if (b.isVisible()) {
                for (BuildPower p : b.getSlots()) {
                    if (p.isActive()) {
                        bp = p;
                    }
                }
            }
        }
        return bp;
    }

    /**
	 * adds or removes and advantage
	 */
    public void addRemoveAdvantage(String id) {
        BuildPower active_slot = getActiveSlot();
        PowerData advantage = power_data_map.get(id);
        if (active_slot.containsAdvantage(advantage)) {
            active_slot.removeAdvantage(advantage);
        } else if (advantage.isAvailable()) {
            active_slot.addAdvantage(advantage);
        }
        updateAdvantages();
        updateCharacteristics();
        forceUpdateDescription();
        user_interface.setChanged(true);
    }

    public void addRemoveFramework(String id) {
        PowerData framework = power_data_map.get(id);
        if (id.equals("x000")) {
            frameworks_active = true;
            enforceFilter();
        } else if (id.equals("x001")) {
            frameworks_active = false;
            enforceFilter();
        } else if (framework.isUsed()) {
            for (Build b : build_lists) {
                for (BuildPower bp : b.getSlots()) {
                    if (bp.getLevel() == 1) {
                        bp.removePower();
                    }
                }
            }
            framework.setUsed(false);
            current_framework = null;
            user_interface.setChanged(true);
        } else {
            if (current_framework != null) current_framework.setUsed(false);
            for (Build b : build_lists) {
                for (BuildPower bp : b.getSlots()) {
                    if (bp.getLevel() == 1) {
                        if (bp.getType().equals("innate talent")) addPowerFromFramework(framework.getInnate(), bp); else if (bp.getType().equals("power t-1")) addPowerFromFramework(framework.getEnergyBuilder(), bp); else if (bp.getType().equals("power t0")) addPowerFromFramework(framework.getTierZeroPower(), bp);
                    }
                }
            }
            framework.setUsed(true);
            current_framework = framework;
            setPoolToFramework(framework);
            user_interface.setChanged(true);
            jumpToEmpty();
        }
        checkBuild();
        updateAdvantages();
        updateCharacteristics();
    }

    private void addPowerFromFramework(String id, BuildPower slot) {
        PowerData power = power_data_map.get(id);
        if (power.isUsed()) {
            for (Build b : build_lists) {
                if (b.isVisible()) {
                    for (BuildPower bp : b.getSlots()) {
                        if (bp.getId().equals(id)) {
                            bp.removePower();
                            break;
                        }
                    }
                }
            }
            power.setUsed(false);
        }
        if (!slot.isEmpty()) {
            slot.removePower();
        }
        slot.setPower(power);
        if (locked_power != null && slot.isPowerEqual(locked_power)) slot.setLockVisible(true);
        power.setUsed(true);
    }

    /**
	 * change the power pool according to the chosen framework
	 */
    private void setPoolToFramework(PowerData framework) {
        HashSet<String> set = new HashSet<String>();
        set.add(framework.getDefaultPool());
        Listener.getInstance().selectPools(set);
        user_interface.clearPoolButtons();
        user_interface.setPool(framework.getDefaultPool());
    }

    /**
	 * jumps to the next empty slot
	 */
    private void jumpToEmpty() {
        for (Build b : build_lists) {
            if (b.isVisible()) {
                boolean set_active = false;
                BuildPower last_active = null;
                BuildPower first_free = null;
                for (BuildPower p : b.getSlots()) {
                    if (set_active && p.getId().equals("zzzz")) {
                        Listener.getInstance().buildPowerClick(p);
                        set_active = false;
                    } else if (p.isActive()) {
                        last_active = p;
                        set_active = true;
                    } else if (p.getId().equals("zzzz") && first_free == null) {
                        first_free = p;
                    }
                }
                if (set_active && first_free != null) Listener.getInstance().buildPowerClick(first_free); else if (!set_active) last_active.setActive(false);
            }
        }
    }

    /**
	 * updates all advantages from powers that are slotted.
	 * updating includes deciding whether there are enough points left to choose it and deciding whether the requirements are met.
	 */
    public void updateAdvantages() {
        int adv_points = base_characteristics.get("adv_points").intValue();
        for (BuildPower bp : build_lists.getFirst().getSlots()) {
            for (PowerData a : bp.getAdvantages()) adv_points -= a.getCost();
        }
        characteristics.put("adv_points", new Double(adv_points));
        adv_points = base_characteristics.get("adv_points").intValue();
        for (BuildPower bp : build_lists.getFirst().getSlots()) {
            if (!bp.isEmpty()) {
                String power_id = bp.getId();
                HashMap<String, PowerData> map = advantage_map.get(power_id);
                for (String adv_id : map.keySet()) {
                    PowerData a = map.get(adv_id);
                    if (!bp.canTakeAdvantage(a) || (!bp.containsAdvantage(a) && new Double(a.getCost()) > characteristics.get("adv_points"))) {
                        if (bp.containsAdvantage(a)) bp.removeAdvantage(a);
                        a.setAvailable(false);
                    } else {
                        a.setAvailable(true);
                    }
                }
            }
        }
        for (BuildPower bp : build_lists.getFirst().getSlots()) {
            for (PowerData a : bp.getAdvantages()) adv_points -= a.getCost();
        }
        characteristics.put("adv_points", new Double(adv_points));
        CharacteristicObserver.getInstance().update(characteristics);
    }

    /**
	 * removes all pool filters
	 */
    public void clearPoolFilter(String filter_type) {
        HashSet<String> filter = filters.get(filter_type);
        HashSet<String> new_filter = (HashSet<String>) filter.clone();
        for (String s : filter) {
            if (s.startsWith("pool:")) new_filter.remove(s);
        }
        filters.put(filter_type, new_filter);
    }

    /**
	 * removes all pool filters
	 */
    public void clearPowerFilter(String filter_type) {
        HashSet<String> filter = filters.get(filter_type);
        HashSet<String> new_filter = (HashSet<String>) filter.clone();
        for (String s : filter) {
            if (s.startsWith("power:")) new_filter.remove(s);
        }
        filters.put(filter_type, new_filter);
    }

    /**
	 * removes all search filters
	 */
    public void clearSearchFilter(String filter_type) {
        HashSet<String> filter = filters.get(filter_type);
        HashSet<String> new_filter = (HashSet<String>) filter.clone();
        for (String s : filter) {
            if (s.startsWith("text:") || s.startsWith("effect:") || s.startsWith("regex:") || s.startsWith("damage:") || s.startsWith("buff")) new_filter.remove(s);
        }
        filters.put(filter_type, new_filter);
    }

    /**
	 * adds a new argument to the filter
	 */
    public void extendFilter(String filter_type, HashSet<String> filter_values) {
        HashSet<String> filter = filters.get(filter_type);
        for (String s : filter_values) {
            if (!filter.contains(s)) filter.add(s); else filter.remove(s);
        }
    }

    /**
	 * sets the current filter type
	 */
    public void setFilterType(String filter_type) {
        current_filter_type = filter_type;
    }

    public String getFilterType() {
        return current_filter_type;
    }

    /**
	 * enforces the currently set filter for whichever filter type is active
	 */
    public void enforceFilter() {
        if (areFrameworksActive() && current_filter_type.equals("powers") && getActiveSlot().getLevel() == 1) setFilterType("frameworks"); else if (current_filter_type.equals("frameworks") && (getActiveSlot().getLevel() > 1 || !areFrameworksActive())) setFilterType("powers");
        HashSet<String> set = new HashSet<String>();
        HashSet<PowerData> power_data_set = new HashSet<PowerData>();
        HashSet<String> filter = filters.get(current_filter_type);
        boolean no_search = true;
        for (String arg : filter) {
            if (!arg.startsWith("pool:")) {
                no_search = false;
            }
        }
        PowerData p;
        boolean take, search_take;
        String arg_value, power_id;
        for (String id : power_data_map.keySet()) {
            p = power_data_map.get(id);
            if (current_filter_type.equals("powers") && (p.isPower() || (p.getId().equals("x000") && getActiveSlot().getLevel() == 1))) {
                take = false;
                search_take = no_search;
                if (p.getId().equals("x000")) take = true; else {
                    for (String arg : filter) {
                        if (arg.startsWith("pool:")) {
                            arg_value = arg.substring(5);
                            if (p.belongsToPool(arg_value) || p.belongsToPool("zz") || p.belongsToPool("y0") || p.belongsToPool("y1") || p.belongsToPool("y2")) take = true;
                        }
                    }
                    if (take) {
                        for (String arg : filter) {
                            boolean tmp_search_take = true;
                            for (String ind_arg : arg.split("[+]")) {
                                if (ind_arg.startsWith("text:")) {
                                    arg_value = ind_arg.substring(5);
                                    if (!p.plainTextSearch(arg_value)) tmp_search_take = false;
                                } else if (ind_arg.startsWith("effect:")) {
                                    arg_value = ind_arg.substring(7);
                                    if (!p.effectSearch(arg_value)) tmp_search_take = false;
                                } else if (ind_arg.startsWith("damage:")) {
                                    arg_value = ind_arg.substring(7);
                                    if (!p.damageTypeSearch(arg_value)) tmp_search_take = false;
                                } else if (ind_arg.startsWith("buff:")) {
                                    arg_value = ind_arg.substring(5);
                                    if (!p.buffSearch(arg_value)) tmp_search_take = false;
                                } else {
                                    tmp_search_take = false;
                                }
                            }
                            if (tmp_search_take) {
                                search_take = true;
                            }
                        }
                    }
                }
                if (take && search_take) power_data_set.add(p);
                power_data_set = filterPowerList(power_data_set);
            } else if (current_filter_type.equals("advantages") && p.isAdvantage()) {
                take = false;
                for (String arg : filter) {
                    if (arg.startsWith("power:")) {
                        power_id = arg.substring(6);
                        if (advantage_map.get(power_id).values().contains(p)) take = true;
                    }
                }
                if (take) power_data_set.add(p);
            } else if (current_filter_type.equals("frameworks") && p.isFramework()) {
                if (!p.getId().equals("x000")) power_data_set.add(p);
            }
        }
        for (PowerData pd : power_data_set) {
            set.add(pd.getId());
        }
        power_list.update(set);
        if (current_filter_type.equals("advantages")) updateAdvantages();
    }

    /**
	 * filters out the powers that aren't selectable
	 */
    public HashSet<PowerData> filterPowerList(HashSet<PowerData> power_set) {
        String filter = "";
        HashMap<String, Integer> power_set_count = new HashMap<String, Integer>();
        HashMap<String, Integer> power_archtype_count = new HashMap<String, Integer>();
        Integer total_power_count = 0;
        HashSet<String> set;
        for (Build b : build_lists) {
            if (b.isVisible()) {
                for (BuildPower p : b.getSlots()) {
                    if (p.isActive()) {
                        if (p.getType().equals("power t-1")) {
                            filter = "-1";
                        } else if (p.getType().equals("power t0")) {
                            filter = "0";
                        } else if (p.getType().equals("power t1-")) {
                            filter = "1";
                        } else if (p.getType().equals("travel power")) {
                            filter = "travel";
                        } else {
                            filter = p.getType();
                        }
                        break;
                    } else {
                        if (p.getType().contains("power") && !p.getType().equals("travel power") && !p.getId().equals("zzzz") && !p.isErroneous()) {
                            set = new HashSet<String>();
                            set = sys_link.getPoolsById(p.getId());
                            for (String s : set) {
                                if (power_set_count.containsKey(s)) {
                                    power_set_count.put(s, power_set_count.get(s) + 1);
                                } else {
                                    power_set_count.put(s, 1);
                                }
                                String archtype = sys_link.getArchtypeById(p.getId());
                                if (archtype != null && !archtype.equals("")) {
                                    if (power_archtype_count.containsKey(archtype)) {
                                        power_archtype_count.put(archtype, power_archtype_count.get(archtype) + 1);
                                    } else {
                                        power_archtype_count.put(archtype, 1);
                                    }
                                }
                            }
                        }
                        if (p.getType().contains("power") && !p.getType().equals("travel power") && !p.getType().equals("power t-1")) {
                            total_power_count++;
                        }
                    }
                }
            }
        }
        Integer set_power_count;
        Integer archtype_power_count;
        String power_tier;
        HashSet<PowerData> result = (HashSet<PowerData>) power_set.clone();
        for (PowerData p : power_set) {
            power_tier = p.getTier();
            if (!power_tier.equals("")) {
                if (filter.equals("-1") || filter.equals("travel") || filter.equals("superstat") || filter.contains("talent")) {
                    if (!power_tier.equals(filter)) {
                        result.remove(p);
                    }
                } else {
                    if (power_set_count.containsKey(sys_link.getBuiltOnPoolById(p.getId()))) {
                        set_power_count = power_set_count.get(sys_link.getBuiltOnPoolById(p.getId()));
                    } else {
                        set_power_count = 0;
                    }
                    if (power_archtype_count.containsKey(sys_link.getArchtypeById(p.getId()))) {
                        archtype_power_count = power_archtype_count.get(sys_link.getArchtypeById(p.getId()));
                    } else {
                        archtype_power_count = 0;
                    }
                    if (power_tier.equals("0") || (power_tier.equals("1") && (set_power_count >= 1 || total_power_count >= 2)) || (power_tier.equals("2") && (set_power_count >= 3 || total_power_count >= 5)) || (power_tier.equals("3") && (set_power_count >= 5 || total_power_count >= 8)) || (power_tier.equals("4") && (archtype_power_count >= 10 && total_power_count >= 12))) {
                    } else {
                        result.remove(p);
                    }
                }
            }
        }
        return result;
    }

    public void checkBuild() {
        String build_power_id = "";
        Integer total_power_count;
        for (int j = 0; j < build_powers.getSlots().size(); j++) {
            total_power_count = 0;
            String filter = "";
            HashMap<String, Integer> power_set_count = new HashMap<String, Integer>();
            HashMap<String, Integer> power_archtype_count = new HashMap<String, Integer>();
            HashSet<String> set;
            int build_power_count = 0;
            for (BuildPower p : build_powers.getSlots()) {
                if (build_power_count == j) {
                    if (p.getType().equals("power t-1")) {
                        filter = "-1";
                    } else if (p.getType().equals("power t0")) {
                        filter = "0";
                    } else if (p.getType().equals("power t1-")) {
                        filter = "1";
                    } else if (p.getType().equals("travel power")) {
                        filter = "travel";
                    }
                    build_power_id = p.getId();
                    break;
                } else {
                    if (p.getType().contains("power") && !p.getType().equals("travel power") && !p.getId().equals("zzzz") && !p.isErroneous()) {
                        set = new HashSet<String>();
                        set = sys_link.getPoolsById(p.getId());
                        for (String s : set) {
                            if (power_set_count.containsKey(s)) {
                                power_set_count.put(s, power_set_count.get(s) + 1);
                            } else {
                                power_set_count.put(s, 1);
                            }
                        }
                        String archtype = sys_link.getArchtypeById(p.getId());
                        if (archtype != null && !archtype.equals("")) {
                            if (power_archtype_count.containsKey(archtype)) {
                                power_archtype_count.put(archtype, power_archtype_count.get(archtype) + 1);
                            } else {
                                power_archtype_count.put(archtype, 1);
                            }
                        }
                    }
                    if (p.getType().contains("power") && !p.getType().equals("travel power") && !p.getType().equals("power t-1")) {
                        total_power_count++;
                    }
                }
                build_power_count++;
            }
            if (!build_power_id.equals("zzzz")) {
                Integer set_power_count;
                Integer archtype_power_count;
                PowerData power;
                String power_tier;
                boolean erroneous = true;
                power = power_data_map.get(build_power_id);
                power_tier = power.getTier();
                if (filter.equals("-1") || filter.equals("travel")) {
                    if (power_tier.equals(filter)) {
                        erroneous = false;
                    }
                } else {
                    if (power_set_count.containsKey(sys_link.getBuiltOnPoolById(power.getId()))) {
                        set_power_count = power_set_count.get(sys_link.getBuiltOnPoolById(power.getId()));
                    } else {
                        set_power_count = 0;
                    }
                    if (power_archtype_count.containsKey(sys_link.getArchtypeById(power.getId()))) {
                        archtype_power_count = power_archtype_count.get(sys_link.getArchtypeById(power.getId()));
                    } else {
                        archtype_power_count = 0;
                    }
                    if (power_tier.equals("0") || (power_tier.equals("1") && (set_power_count >= 1 || total_power_count >= 2)) || (power_tier.equals("2") && (set_power_count >= 3 || total_power_count >= 5)) || (power_tier.equals("3") && (set_power_count >= 5 || total_power_count >= 8)) || (power_tier.equals("4") && (archtype_power_count >= 10 && total_power_count >= 12))) {
                        erroneous = false;
                    }
                }
                build_powers.getSlots().get(j).setErroneous(erroneous);
            } else {
                build_powers.getSlots().get(j).setErroneous(false);
            }
        }
    }

    public void updateCharacteristics() {
        if (base_characteristics != null) {
            for (int i = 0; i < 1; i++) {
                HashMap<String, Double> map = (HashMap<String, Double>) base_characteristics.clone();
                HashMap<String, HashMap<String, Double>> map2 = new HashMap<String, HashMap<String, Double>>();
                HashMap<String, Double> contribution_map;
                for (String s : base_characteristics.keySet()) {
                    if (base_characteristics.get(s) != 0.0) {
                        if (map2.containsKey(s)) contribution_map = map2.get(s); else contribution_map = new HashMap<String, Double>();
                        contribution_map.put("Base", base_characteristics.get(s));
                        map2.put(s, contribution_map);
                    }
                }
                addEffectsFromBuild(map, map2, true);
                for (String k : map.keySet()) if (!k.equals("adv_points")) characteristics.put(k, map.get(k));
                CharacteristicObserver.getInstance().update(characteristics, map2);
                addEffectsFromBuild(map, map2, false);
                for (String k : map.keySet()) if (!k.equals("adv_points")) characteristics.put(k, map.get(k));
                CharacteristicObserver.getInstance().update(characteristics, map2);
                String formula;
                for (String s : map.keySet()) {
                    formula = CharacteristicObserver.getInstance().getFormula(s);
                    if (!formula.equals("")) {
                        ValueCalculator calculator = new ValueCalculator(formula);
                        Double result = calculator.compute();
                        map.put(s, result);
                    }
                }
                for (String k : map.keySet()) if (!k.equals("adv_points")) characteristics.put(k, map.get(k));
                CharacteristicObserver.getInstance().update(characteristics, map2);
            }
        }
    }

    private void addEffectsFromBuild(HashMap<String, Double> map, HashMap<String, HashMap<String, Double>> map2, boolean only_stats) {
        HashMap<String, Double> contribution_map;
        String effect_target, item_target;
        LinkedList<String> new_chars;
        for (Build b : build_lists) {
            for (BuildPower p : b.getSlots()) {
                if (p.isPowerActivated()) {
                    LinkedList<LinkedList<HashMap<String, String>>> effects = p.getEffects();
                    if (effects != null) {
                        effects = calculateEffects(effects);
                        for (LinkedList<HashMap<String, String>> effect : effects) {
                            effect_target = effect.getFirst().get("target");
                            for (HashMap<String, String> item : effect) {
                                item_target = effect_target;
                                if (item.containsKey("target")) item_target = item.get("target");
                                if (item_target.contains("self")) {
                                    if (item.get("tag").equals("buff")) {
                                        if (map.containsKey(item.get("char")) && ((only_stats && CharacteristicObserver.getInstance().isStat(item.get("char"))) || (!only_stats && !CharacteristicObserver.getInstance().isStat(item.get("char"))))) {
                                            new_chars = CharacteristicObserver.getInstance().getChildren(item.get("char"));
                                            new_chars.add(item.get("char"));
                                            for (String c : new_chars) {
                                                String concat = CharacteristicObserver.getInstance().getConcat(c);
                                                if (concat.equals("")) map.put(c, map.get(c) + Double.parseDouble(item.get("value"))); else {
                                                    concat = concat.replace("value1", map.get(c).toString()).replace("value2", item.get("value"));
                                                    ValueCalculator calculator = new ValueCalculator(concat);
                                                    map.put(c, calculator.compute());
                                                }
                                                if (map2.containsKey(c)) contribution_map = map2.get(c); else contribution_map = new HashMap<String, Double>();
                                                Double power_specific_value = Double.parseDouble(item.get("value"));
                                                if (contribution_map.containsKey(effect.getFirst().get("name"))) power_specific_value += contribution_map.get(effect.getFirst().get("name"));
                                                contribution_map.put(effect.getFirst().get("name"), power_specific_value);
                                                map2.put(c, contribution_map);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void saveBuild(File file) {
        String output = buildToString();
        try {
            FileWriter file_writer = new FileWriter(file);
            file_writer.write(output);
            file_writer.close();
            user_interface.setChanged(false);
            pref_link.updateLastSaved(file.getAbsolutePath());
            user_interface.setTopText("saved to " + file.getName(), 0);
        } catch (IOException e) {
            sys_link.writeError("saving build", e);
            e.printStackTrace();
            user_interface.setTopText(sys_link.translate("error_saving"), 0);
        }
    }

    public void exportBuild(String type) {
        ClipboardTexter clip = new ClipboardTexter();
        String output = "";
        if (type.equals("normalforum")) {
            output += "[b][url=http://champions.zarzu.ch]" + sys_link.translate("build_by") + " championBuilder " + AppConst.VERSION + "[/url][/b]";
            output += "\n\n[b][url=http://champions.zarzu.ch/download.php?download=" + buildToString() + "][color=#c3320b]" + sys_link.translate("download_here") + "[/color][/url][/b]";
            output += "\n\n[b][color=#f78112]" + char_sheet.getCharacterName() + ":[/color] [color=#fec530]" + sys_link.translate("level_champ").replace("(1)", "40") + "[/color][/b]";
            output += "\n\n[b][color=#f78112]" + sys_link.translate("superstats") + ":[/color][/b]";
            for (BuildPower p : build_lists.get(2).getSlots()) {
                if (!p.getId().equals("zzzz")) output += "\n[b][color=#f78112]" + sys_link.translate("level") + " " + p.getLevel() + ":[/color] [color=#fec530]" + p.getName() + "[/color][/b]";
            }
            output += "\n\n[b][color=#f78112]" + sys_link.translate("powers") + ":[/color][/b]";
            for (BuildPower p : build_lists.get(0).getSlots()) {
                if (!p.getId().equals("zzzz")) {
                    output += "\n[b][color=#f78112]" + sys_link.translate("level") + " " + p.getLevel() + ":[/color] [color=#fec530]" + p.getName() + "[/color][/b]";
                    if (p.getAdvantages().size() > 0) output += "[color=#ce6c10] -- ";
                    for (PowerData a : p.getAdvantages()) {
                        output += "" + a.getName() + ", ";
                    }
                    if (p.getAdvantages().size() > 0) {
                        output = output.substring(0, output.length() - 2) + "[/color]";
                    }
                }
            }
            output += "\n\n[b][color=#f78112]" + sys_link.translate("talents") + ":[/color][/b]";
            for (BuildPower p : build_lists.get(1).getSlots()) {
                if (!p.getId().equals("zzzz")) output += "\n[b][color=#f78112]" + sys_link.translate("level") + " " + p.getLevel() + ":[/color] [color=#fec530]" + p.getName() + "[/color][/b]";
            }
        } else if (type.equals("plaintext")) {
            output += "" + char_sheet.getCharacterName() + ": " + sys_link.translate("level_champ").replace("(1)", "40");
            output += "\n\n" + sys_link.translate("superstats") + ":";
            for (BuildPower p : build_lists.get(2).getSlots()) {
                if (!p.getId().equals("zzzz")) output += "\n" + sys_link.translate("level") + " " + p.getLevel() + ": " + p.getName();
            }
            output += "\n\n" + sys_link.translate("powers") + ":";
            for (BuildPower p : build_lists.get(0).getSlots()) {
                if (!p.getId().equals("zzzz")) {
                    output += "\n" + sys_link.translate("level") + " " + p.getLevel() + ": " + p.getName();
                    if (p.getAdvantages().size() > 0) output += " -- ";
                    for (PowerData a : p.getAdvantages()) {
                        output += "" + a.getName() + ", ";
                    }
                    if (p.getAdvantages().size() > 0) {
                        output = output.substring(0, output.length() - 2);
                    }
                }
            }
            output += "\n\n" + sys_link.translate("talents") + ":";
            for (BuildPower p : build_lists.get(1).getSlots()) {
                if (!p.getId().equals("zzzz")) output += "\n" + sys_link.translate("level") + " " + p.getLevel() + ": " + p.getName();
            }
        }
        clip.setContents(output);
        user_interface.setTopText(sys_link.translate("exported"), 0);
    }

    public LinkedList<String> getPrintBuild() {
        LinkedList<String> list = new LinkedList<String>();
        String output;
        list.add("bold");
        list.add(char_sheet.getCharacterName() + ":");
        list.add("indent");
        list.add("plain");
        list.add(sys_link.translate("level_champ").replace("(1)", "40"));
        list.add("nl");
        list.add("nl");
        list.add("bold");
        list.add(sys_link.translate("superstats") + ":");
        for (BuildPower p : build_lists.get(2).getSlots()) {
            list.add("nl");
            list.add("bold");
            list.add(sys_link.translate("level") + " " + p.getLevel() + ":");
            list.add("indent");
            list.add("plain");
            list.add(p.getName());
        }
        list.add("nl");
        list.add("nl");
        list.add("bold");
        list.add(sys_link.translate("powers") + ":");
        for (BuildPower p : build_lists.get(0).getSlots()) {
            list.add("nl");
            list.add("bold");
            list.add(sys_link.translate("level") + " " + p.getLevel() + ":");
            list.add("indent");
            list.add("plain");
            output = p.getName();
            if (p.getAdvantages().size() > 0) output += " -- ";
            for (PowerData a : p.getAdvantages()) {
                output += "" + a.getName() + ", ";
            }
            if (p.getAdvantages().size() > 0) {
                output = output.substring(0, output.length() - 2);
            }
            list.add(output);
        }
        list.add("nl");
        list.add("nl");
        list.add("bold");
        list.add(sys_link.translate("talents") + ":");
        for (BuildPower p : build_lists.get(1).getSlots()) {
            list.add("nl");
            list.add("bold");
            list.add(sys_link.translate("level") + " " + p.getLevel() + ":");
            list.add("indent");
            list.add("plain");
            list.add(p.getName());
        }
        return list;
    }

    private String buildToString() {
        String output = "", build_output, power_output, header = "", length, activated_powers = "";
        for (Build b : build_lists) {
            build_output = "";
            for (BuildPower p : b.getSlots()) {
                power_output = p.getId();
                for (PowerData a : p.getAdvantages()) {
                    power_output += a.getId().substring(4);
                }
                length = Integer.toString(power_output.length() + 1, 36);
                build_output += length + power_output;
            }
            length = Integer.toString(b.getSlots().size(), 36);
            output += length + build_output;
        }
        header = "41" + Integer.toString(build_lists.size(), 36) + "2";
        output += Integer.toString(char_sheet.getCharacterName().length() + 1, 36) + char_sheet.getCharacterName();
        for (BuildPower p : build_lists.getFirst().getSlots()) {
            if (p.isPowerActivated()) activated_powers += "1"; else activated_powers += "0";
        }
        output += Integer.toString(activated_powers.length() + 1, 36) + activated_powers;
        output = header + output;
        return output;
    }

    public void loadBuild(File file) {
        String input = "";
        try {
            FileInputStream fstream = new FileInputStream(file);
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            while (br.ready()) {
                input += br.readLine();
            }
            br.close();
            in.close();
            fstream.close();
            clearEverything();
            user_interface.setTopText(sys_link.translate("loaded").replace("(1)", file.getName()), 0);
            loadBuildByString(input);
            user_interface.setChanged(false);
            pref_link.updateLastSaved(file.getAbsolutePath());
        } catch (FileNotFoundException e) {
            sys_link.writeError("loading file", e);
            e.printStackTrace();
            user_interface.setTopText(sys_link.translate("error_loading"), 0);
        } catch (IOException e) {
            sys_link.writeError("loading file", e);
            e.printStackTrace();
            user_interface.setTopText(sys_link.translate("error_loading"), 0);
        }
    }

    private void loadBuildByString(String input) {
        String header, power, power_id, name;
        int header_length, build_length, power_length, name_length, activated_powers_length, build_count, trail_count, length_digits;
        BuildPower build_power;
        PowerData a;
        try {
            header_length = Integer.valueOf(input.substring(0, 1), 36);
            header = input.substring(1, header_length);
            length_digits = Integer.valueOf(header.substring(0, 1), 36);
            build_count = Integer.valueOf(header.substring(1, 2), 36);
            trail_count = Integer.valueOf(header.substring(2, 3), 36);
            input = input.substring(header_length);
            for (int i = 0; i < build_count; i++) {
                build_length = Integer.valueOf(input.substring(0, length_digits), 36);
                input = input.substring(length_digits);
                for (int j = 0; j < build_length; j++) {
                    power_length = Integer.valueOf(input.substring(0, length_digits), 36);
                    power = input.substring(1, power_length);
                    power_id = power.substring(0, 4);
                    input = input.substring(power_length);
                    try {
                        build_power = build_lists.get(i).getSlots().get(j);
                        if (!power_id.equals("zzzz")) {
                            try {
                                build_power.setPower(power_data_map.get(power_id));
                                if (power_length > 5) {
                                    for (String a_id : advantage_map.get(power_id).keySet()) {
                                        a = advantage_map.get(power_id).get(a_id);
                                        if (power.substring(4).contains(a_id)) {
                                            build_power.addAdvantage(a);
                                            a.setUsed(true);
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                sys_link.writeError("reading power from file", e);
                                e.printStackTrace();
                                user_interface.setTopText(sys_link.translate("error_unknown"), 0);
                            }
                        }
                    } catch (Exception e) {
                        sys_link.writeError("reading build from file", e);
                        e.printStackTrace();
                        user_interface.setTopText(sys_link.translate("error_parse"), 0);
                    }
                }
            }
            if (trail_count > 0) {
                name_length = Integer.valueOf(input.substring(0, length_digits), 36);
                name = input.substring(1, name_length);
                char_sheet.setCharacterName(name);
                input = input.substring(name_length);
                if (trail_count > 1) {
                    activated_powers_length = Integer.valueOf(input.substring(0, length_digits), 36);
                    for (int i = 1; i < activated_powers_length; i++) {
                        try {
                            build_power = build_lists.getFirst().getSlots().get(i - 1);
                            if (!build_power.isEmpty()) build_power.setPowerActivated(input.substring(i, i + 1).equals("1"));
                        } catch (Exception e) {
                            sys_link.writeError("reading build from file", e);
                            e.printStackTrace();
                            user_interface.setTopText(sys_link.translate("error_parse"), 0);
                        }
                    }
                    if (trail_count > 2) user_interface.setTopText(sys_link.translate("error_parse"), 0);
                }
            }
        } catch (Exception e) {
            sys_link.writeError("reading main load", e);
            e.printStackTrace();
            user_interface.setTopText(sys_link.translate("error_corrupted"), 0);
            clearBuild();
        }
        frameworks_active = false;
        updateAdvantages();
        enforceFilter();
        checkBuild();
        updateCharacteristics();
    }

    private LinkedList<LinkedList<HashMap<String, String>>> mergeEffects(LinkedList<LinkedList<HashMap<String, String>>> effects, LinkedList<LinkedList<HashMap<String, String>>> adv_effects) {
        LinkedList<LinkedList<HashMap<String, String>>> new_effects = new LinkedList<LinkedList<HashMap<String, String>>>();
        LinkedList<HashMap<String, String>> list;
        HashMap<String, String> h_map, fork_map;
        boolean remove_list, remove_map;
        LinkedList<String> adv_ids;
        for (LinkedList<HashMap<String, String>> effect : effects) {
            list = new LinkedList<HashMap<String, String>>();
            remove_list = false;
            for (HashMap<String, String> map : effect) {
                h_map = new HashMap<String, String>();
                for (String s : map.keySet()) h_map.put(s, map.get(s));
                remove_map = false;
                fork_map = null;
                for (LinkedList<HashMap<String, String>> adv_effect : adv_effects) {
                    HashMap<String, String> adv_head = adv_effect.getFirst();
                    if (adv_head.containsKey("id") && effect.getFirst().containsKey("id")) {
                        adv_ids = new LinkedList<String>();
                        for (String s : adv_head.get("id").split("[,]")) adv_ids.add(s);
                        if (adv_ids.contains(effect.getFirst().get("id"))) {
                            for (HashMap<String, String> adv_map : adv_effect) {
                                if ((adv_map.containsKey("id") && h_map.containsKey("id") && adv_map.get("id").equals(h_map.get("id"))) || adv_map.get("tag").equals("head") && h_map.get("tag").equals("head")) {
                                    if (adv_map.get("tag").equals("delete")) remove_map = true;
                                    if (adv_map.get("tag").equals("fork")) fork_map = (HashMap<String, String>) h_map.clone();
                                    for (String s : adv_map.keySet()) {
                                        if (!s.equals("id") && !s.equals("tag") && !s.equals("newid")) {
                                            if (!adv_map.get(s).equals("null")) {
                                                String value = adv_map.get(s);
                                                if (h_map.containsKey("value")) value = value.replace("value", h_map.get("value"));
                                                if (h_map.containsKey(s)) {
                                                    h_map.put(s, value.replace("original", h_map.get(s)));
                                                } else h_map.put(s, value);
                                            } else h_map.remove(s);
                                        } else if (s.equals("newid")) h_map.put("id", adv_map.get(s));
                                    }
                                }
                            }
                        }
                    }
                }
                if (fork_map != null) list.add(fork_map);
                if (!remove_map) list.add(h_map);
            }
            for (LinkedList<HashMap<String, String>> adv_effect : adv_effects) {
                HashMap<String, String> adv_head = adv_effect.getFirst();
                if (adv_head.containsKey("id") && effect.getFirst().containsKey("id")) {
                    adv_ids = new LinkedList<String>();
                    for (String s : adv_head.get("id").split("[,]")) adv_ids.add(s);
                    if (adv_ids.contains(effect.getFirst().get("id"))) {
                        if (adv_head.containsKey("delete")) remove_list = true;
                        for (HashMap<String, String> adv_map : adv_effect) {
                            if (!adv_map.containsKey("id") && !adv_map.get("tag").equals("head")) {
                                h_map = new HashMap<String, String>();
                                for (String s : adv_map.keySet()) h_map.put(s, adv_map.get(s));
                                list.add(h_map);
                            }
                        }
                    }
                }
            }
            if (!remove_list) new_effects.add(list);
        }
        list = new LinkedList<HashMap<String, String>>();
        for (LinkedList<HashMap<String, String>> adv_effect : adv_effects) {
            HashMap<String, String> adv_head = adv_effect.getFirst();
            if (!adv_head.containsKey("id")) {
                for (HashMap<String, String> adv_map : adv_effect) {
                    h_map = new HashMap<String, String>();
                    for (String s : adv_map.keySet()) h_map.put(s, adv_map.get(s));
                    list.add(h_map);
                }
                new_effects.add(list);
            }
        }
        return new_effects;
    }

    private LinkedList<LinkedList<HashMap<String, String>>> calculateEffects(LinkedList<LinkedList<HashMap<String, String>>> effects) {
        LinkedList<LinkedList<HashMap<String, String>>> new_effects = new LinkedList<LinkedList<HashMap<String, String>>>();
        ValueCalculator calculator;
        LinkedList<HashMap<String, String>> list;
        HashMap<String, String> h_map;
        for (LinkedList<HashMap<String, String>> effect : effects) {
            list = new LinkedList<HashMap<String, String>>();
            for (HashMap<String, String> map : effect) {
                h_map = new HashMap<String, String>();
                for (String s : map.keySet()) h_map.put(s, map.get(s));
                if (h_map.containsKey("value") && !h_map.get("value").matches("\\-?[0-9.]+\\-?[0-9.]*%?")) {
                    calculator = new ValueCalculator(h_map.get("value"));
                    Double result = new Double(Math.round(calculator.compute() * 10)) / 10;
                    String output = result.toString();
                    if (result > 10) output = Integer.toString(result.intValue());
                    h_map.put("value", output);
                }
                if (h_map.containsKey("maxvalue") && !h_map.get("maxvalue").matches("\\-?[0-9.]+\\-?[0-9.]*%?")) {
                    calculator = new ValueCalculator(h_map.get("maxvalue"));
                    Double result = new Double(Math.round(calculator.compute() * 10)) / 10;
                    String output = result.toString();
                    if (result > 10) output = Integer.toString(result.intValue());
                    h_map.put("maxvalue", output);
                }
                list.add(h_map);
            }
            new_effects.add(list);
        }
        return new_effects;
    }

    /**
	 * updates the description according to the given id
	 */
    public void updateDescription(String id, boolean force) {
        PowerData power = power_data_map.get(id);
        LinkedList<LinkedList<HashMap<String, String>>> effects;
        LinkedList<LinkedList<String>> descriptions;
        String base_power_id = id;
        String adv_id = "";
        if (locked_power == null || locked_power == power) {
            if (power.isAdvantage()) {
                base_power_id = id.substring(0, 4);
                adv_id = id.substring(4);
            }
            effects = power_data_map.get(base_power_id).getEffects();
            descriptions = power.getDescriptions();
            for (BuildPower bp : build_lists.getFirst().getSlots()) {
                if (bp.getId().equals(base_power_id)) {
                    String already_slotted = adv_id;
                    for (PowerData slotted_adv : bp.getAdvantages()) already_slotted += slotted_adv.getId().substring(4);
                    PowerData a;
                    for (String a_id : advantage_map.get(base_power_id).keySet()) {
                        a = advantage_map.get(base_power_id).get(a_id);
                        if (power.getRequirements().contains(a_id) || already_slotted.contains(a_id)) {
                            effects = mergeEffects(effects, a.getEffects());
                        }
                    }
                    break;
                }
            }
            effects = calculateEffects(effects);
            description.setDescription(id, descriptions, filters.get("powers"), force);
            adv_description.setDescription(id, effects, filters.get("powers"), force);
            description_power = power;
        }
    }

    public void forceUpdateDescription() {
        if (description_power != null) updateDescription(description_power.getId(), true);
    }

    public void lockDescription(String id) {
        PowerData power = power_data_map.get(id);
        for (Build b : build_lists) {
            for (BuildPower bp : b.getSlots()) {
                if (bp.getId().equals(id)) bp.setLockVisible(true);
                if (locked_power != null && bp.isPowerEqual(locked_power)) bp.setLockVisible(false);
            }
        }
        if (locked_power != null) {
            locked_power.setLockVisible(false);
        }
        if (locked_power != power) {
            power.setLockVisible(true);
            locked_power = power;
            updateDescription(id, false);
            description.setLockVisible(true);
            adv_description.setLockVisible(true);
        } else {
            locked_power = null;
            description.setLockVisible(false);
            adv_description.setLockVisible(false);
        }
    }

    public void deactivateAllBuildPowers() {
        for (Build b : build_lists) {
            if (b.isVisible()) {
                for (BuildPower p : b.getSlots()) {
                    p.setActive(false);
                }
            }
        }
    }

    public void clearBuild() {
        for (Build b : build_lists) {
            for (BuildPower p : b.getSlots()) {
                if (!p.isEmpty()) p.removePower();
            }
        }
    }

    public void clearEverything() {
        clearBuild();
        updateAdvantages();
        updateCharacteristics();
        char_sheet.setCharacterName("");
        user_interface.setTopText("", 0);
        frameworks_active = true;
        enforceFilter();
    }

    public boolean areFrameworksActive() {
        return frameworks_active;
    }

    /**
	 * update all powers to the current language
	 */
    public void updateLanguage() {
        NodeList xml_power_list = sys_link.getPowers();
        NodeList xml_stat_list = sys_link.getStats();
        NodeList xml_talent_list = sys_link.getTalents();
        NodeList xml_frameworks = sys_link.getFrameworks();
        String id, child_id;
        NodeList child_list, grand_child_list;
        for (int i = 0; i < xml_frameworks.getLength(); i++) {
            id = xml_frameworks.item(i).getAttributes().getNamedItem("id").getNodeValue();
            power_data_map.get(id).updateLanguage(xml_frameworks.item(i));
        }
        for (int i = 0; i < xml_power_list.getLength(); i++) {
            id = xml_power_list.item(i).getAttributes().getNamedItem("id").getNodeValue();
            power_data_map.get(id).updateLanguage(xml_power_list.item(i));
            child_list = xml_power_list.item(i).getChildNodes();
            for (int j = 1; j < child_list.getLength(); j += 2) {
                if (child_list.item(j).getNodeName().equals("advantages")) {
                    grand_child_list = child_list.item(j).getChildNodes();
                    for (int k = 1; k < grand_child_list.getLength(); k += 2) {
                        child_id = grand_child_list.item(k).getAttributes().getNamedItem("id").getNodeValue();
                        power_data_map.get(id + child_id).updateLanguage(grand_child_list.item(k));
                    }
                }
            }
        }
        for (int i = 0; i < xml_stat_list.getLength(); i++) {
            id = xml_stat_list.item(i).getAttributes().getNamedItem("id").getNodeValue();
            power_data_map.get(id).updateLanguage(xml_stat_list.item(i));
        }
        for (int i = 0; i < xml_talent_list.getLength(); i++) {
            id = xml_talent_list.item(i).getAttributes().getNamedItem("id").getNodeValue();
            power_data_map.get(id).updateLanguage(xml_talent_list.item(i));
        }
    }

    public static BuildUpdater getInstance() {
        if (instance == null) instance = new BuildUpdater();
        return instance;
    }
}
