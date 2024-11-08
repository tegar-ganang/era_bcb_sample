package net.slashie.expedition.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import net.slashie.expedition.domain.Expedition.DeathCause;
import net.slashie.expedition.game.ExpeditionGame;
import net.slashie.expedition.ui.ExpeditionUserInterface;
import net.slashie.serf.action.Actor;
import net.slashie.serf.game.Equipment;
import net.slashie.serf.ui.UserInterface;
import net.slashie.util.Pair;
import net.slashie.utils.Util;

public class BattleManager {

    public static void battle(String battleName, Expedition attacker, Actor defender) {
        List<Equipment> attackingUnitsFullGroup = attacker.getGoods(GoodType.PEOPLE);
        List<Equipment> defendingUnitsFullGroup = null;
        int attackingMoraleModifier = attacker.getMoraleAttackModifier();
        if (Util.chance(20)) attackingMoraleModifier = 0;
        int defendingMoraleModifier = 0;
        if (defender instanceof NativeTown) {
            NativeTown town = (NativeTown) defender;
            if (town.getTotalUnits() == 0) {
                if (attacker == ExpeditionGame.getCurrentGame().getPlayer()) {
                    ((ExpeditionUserInterface) UserInterface.getUI()).transferFromCache("Select the goods to plunder", null, town);
                }
                return;
            } else {
                defendingUnitsFullGroup = town.getGoods(GoodType.PEOPLE);
                town.resetTurnsBeforeNextExpedition();
                defender.getLevel().getDispatcher().removeActor(town);
                defender.getLevel().getDispatcher().addActor(town, true);
            }
        } else if (defender instanceof Expedition) {
            Expedition npe = (Expedition) defender;
            defendingUnitsFullGroup = npe.getGoods(GoodType.PEOPLE);
            defendingMoraleModifier = npe.getMoraleAttackModifier();
            if (Util.chance(20)) defendingMoraleModifier = 0;
        } else {
            return;
        }
        defender.setInterrupted();
        List<Equipment> attackingUnits = selectSquad(cloneEquipmentList(attackingUnitsFullGroup));
        List<Equipment> defendingUnits = selectSquad(cloneEquipmentList(defendingUnitsFullGroup));
        List<Equipment> originalAttackingUnits = cloneEquipmentList(attackingUnits);
        List<Equipment> originalDefendingUnits = cloneEquipmentList(defendingUnits);
        ((ExpeditionUserInterface) UserInterface.getUI()).showBattleScene(battleName, attackingUnits, defendingUnits);
        AssaultOutcome attackerRangedAttackOutcome = rangedAttack(attackingUnits, attackingMoraleModifier, defendingUnits, defendingMoraleModifier, (UnitContainer) defender);
        AssaultOutcome defenderRangedAttackOutcome = rangedAttack(defendingUnits, defendingMoraleModifier, attackingUnits, attackingMoraleModifier, attacker);
        Pair<AssaultOutcome, AssaultOutcome> attackerMountedAttackOutcome = mountedAttack(attackingUnits, attackingMoraleModifier, attacker, defendingUnits, defendingMoraleModifier, (UnitContainer) defender);
        Pair<AssaultOutcome, AssaultOutcome> attackerMeleeAttackOutcome = meleeAttack(attackingUnits, attackingMoraleModifier, attacker, defendingUnits, defendingMoraleModifier, (UnitContainer) defender);
        int attackerScore = 0;
        int defenderScore = 0;
        attackerScore += eval(attackerRangedAttackOutcome);
        defenderScore += eval(defenderRangedAttackOutcome);
        attackerScore += eval(attackerMountedAttackOutcome.getA());
        defenderScore += eval(attackerMountedAttackOutcome.getB());
        attackerScore += eval(attackerMeleeAttackOutcome.getA());
        defenderScore += eval(attackerMeleeAttackOutcome.getB());
        if (attackerScore > defenderScore) {
            attacker.increaseWinBalance();
            if (defender instanceof NativeTown) {
                ((NativeTown) defender).increaseScaredLevel();
            } else if (defender instanceof Expedition) {
                ((Expedition) defender).decreaseWinBalance();
            }
        } else {
            attacker.decreaseWinBalance();
            if (defender instanceof NativeTown) {
                ((NativeTown) defender).reduceScaredLevel();
            } else if (defender instanceof Expedition) {
                ((Expedition) defender).increaseWinBalance();
            }
        }
        ((ExpeditionUserInterface) UserInterface.getUI()).showBattleResults(originalAttackingUnits, originalDefendingUnits, battleName, attackerRangedAttackOutcome, defenderRangedAttackOutcome, attackerMountedAttackOutcome, attackerMeleeAttackOutcome, attackerScore, defenderScore);
        if (attacker != ExpeditionGame.getCurrentGame().getPlayer()) {
            attacker.checkDeath();
        }
        if (defender != ExpeditionGame.getCurrentGame().getPlayer()) {
            if (defender instanceof Expedition) {
                ((Expedition) defender).checkDeath();
            }
        }
    }

    private static int eval(AssaultOutcome assaultOutcome) {
        int score = 0;
        for (Pair<ExpeditionUnit, Integer> wound : assaultOutcome.getWounds()) {
            score += wound.getB();
        }
        for (Pair<ExpeditionUnit, Integer> death : assaultOutcome.getDeaths()) {
            score += death.getB() * 3;
        }
        return score;
    }

    private static List<Equipment> cloneEquipmentList(List<Equipment> originalList) {
        List<Equipment> clonedList = new ArrayList<Equipment>();
        for (Equipment e : originalList) {
            clonedList.add(new Equipment(e.getItem(), e.getQuantity()));
        }
        return clonedList;
    }

    private static Pair<AssaultOutcome, AssaultOutcome> meleeAttack(List<Equipment> attackingUnits, int attackingMoraleModifier, UnitContainer attackingExpedition, List<Equipment> defendingUnits, int defendingMoraleModifier, UnitContainer defendingExpedition) {
        Pair<AssaultOutcome, AssaultOutcome> ret = new Pair<AssaultOutcome, AssaultOutcome>(new AssaultOutcome(), new AssaultOutcome());
        for (Equipment equipment : attackingUnits) {
            for (int i = 0; i < equipment.getQuantity(); i++) {
                Equipment randomTarget = pickRandomTargetFairly(defendingUnits);
                if (randomTarget == null) {
                    return ret;
                }
                singleAttack(equipment, attackingMoraleModifier, randomTarget, defendingExpedition, ret.getA());
                singleAttack(randomTarget, defendingMoraleModifier, equipment, attackingExpedition, ret.getB());
                if (randomTarget.getQuantity() == 0) {
                    defendingUnits.remove(randomTarget);
                }
            }
        }
        return ret;
    }

    private static Pair<AssaultOutcome, AssaultOutcome> mountedAttack(List<Equipment> attackingUnits, int attackingMoraleModifier, UnitContainer attackingExpedition, List<Equipment> defendingUnits, int defendingMoraleModifier, UnitContainer defendingExpedition) {
        Pair<AssaultOutcome, AssaultOutcome> ret = new Pair<AssaultOutcome, AssaultOutcome>(new AssaultOutcome(), new AssaultOutcome());
        for (Equipment equipment : attackingUnits) {
            ExpeditionUnit unit = (ExpeditionUnit) equipment.getItem();
            if (!unit.isRangedAttack() && unit.isMounted()) {
                for (int i = 0; i < equipment.getQuantity(); i++) {
                    Equipment randomTarget = pickRandomTargetFairly(defendingUnits);
                    if (randomTarget == null) {
                        return ret;
                    }
                    singleAttack(equipment, attackingMoraleModifier, randomTarget, defendingExpedition, ret.getA());
                    singleAttack(randomTarget, defendingMoraleModifier, equipment, attackingExpedition, ret.getB());
                    if (randomTarget.getQuantity() == 0) {
                        defendingUnits.remove(randomTarget);
                    }
                }
            }
        }
        return ret;
    }

    private static AssaultOutcome rangedAttack(List<Equipment> attackingUnits, int attackingMoraleModifier, List<Equipment> defendingUnits, int defendingMoraleModifier, UnitContainer defendingExpedition) {
        AssaultOutcome ret = new AssaultOutcome();
        for (Equipment equipment : attackingUnits) {
            ExpeditionUnit unit = (ExpeditionUnit) equipment.getItem();
            if (unit.isRangedAttack()) {
                for (int i = 0; i < equipment.getQuantity(); i++) {
                    Equipment randomTarget = pickRandomTargetFairly(defendingUnits);
                    if (randomTarget == null) {
                        return ret;
                    }
                    singleAttack(equipment, attackingMoraleModifier, randomTarget, defendingExpedition, ret);
                    if (randomTarget.getQuantity() == 0) {
                        defendingUnits.remove(randomTarget);
                    }
                }
            }
        }
        return ret;
    }

    private static void singleAttack(Equipment attackerEquipment, int attackingMoraleModifier, Equipment defendingEquipment, UnitContainer defendingExpedition, AssaultOutcome outcome) {
        ExpeditionUnit attackingUnit = (ExpeditionUnit) attackerEquipment.getItem();
        ExpeditionUnit defendingUnit = (ExpeditionUnit) defendingEquipment.getItem();
        if (Util.chance(attackingUnit.getHitChance())) {
            if (!Util.chance(defendingUnit.getEvadeChance())) {
                ExpeditionUnit targetUnit = (ExpeditionUnit) defendingEquipment.getItem();
                int damage = attackingUnit.getAttack().roll();
                damage += attackingMoraleModifier;
                int defense = targetUnit.getDefense().roll();
                int realDamage = damage - defense;
                if (realDamage <= 0) {
                } else if (realDamage <= targetUnit.getResistance()) {
                    if (targetUnit.isWounded()) {
                        defendingExpedition.reduceUnits(targetUnit, 1, DeathCause.DEATH_BY_SLAYING);
                        defendingEquipment.reduceQuantity(1);
                        outcome.addDeath(targetUnit);
                    } else {
                        defendingExpedition.reduceUnits(targetUnit, 1, DeathCause.DEATH_BY_SLAYING);
                        ExpeditionUnit woundedUnit = (ExpeditionUnit) targetUnit.clone();
                        woundedUnit.setWounded(true);
                        defendingExpedition.addUnits(woundedUnit, 1);
                        defendingEquipment.reduceQuantity(1);
                        outcome.addWound(targetUnit);
                    }
                } else {
                    defendingExpedition.reduceUnits(targetUnit, 1, DeathCause.DEATH_BY_SLAYING);
                    defendingEquipment.reduceQuantity(1);
                    outcome.addDeath(targetUnit);
                }
            }
        }
    }

    private static Equipment pickRandomTargetFairly(List<Equipment> targetUnits) {
        int count = 0;
        for (Equipment eq : targetUnits) {
            count += eq.getQuantity();
        }
        int rand = Util.rand(0, count - 1);
        count = 0;
        for (Equipment eq : targetUnits) {
            if (eq.getQuantity() == 0) continue;
            count += eq.getQuantity();
            if (rand < count) return eq;
        }
        return null;
    }

    private static List<Equipment> selectSquad(List<Equipment> fullGroup) {
        int remaining = 60;
        int remainingRanged = 20;
        int remainingMounted = 20;
        List<Equipment> squad = new ArrayList<Equipment>();
        Map<String, Equipment> squadMap = new Hashtable<String, Equipment>();
        fullGroup = orderByCombatValue(fullGroup);
        for (Equipment eq : fullGroup) {
            if (eq.getQuantity() == 0) continue;
            if (remaining == 0) break;
            if (remainingRanged == 0) break;
            ExpeditionUnit unit = (ExpeditionUnit) eq.getItem();
            if (unit.isWounded()) continue;
            if (unit.isRangedAttack()) {
                int quantity = eq.getQuantity();
                if (quantity > remainingRanged) {
                    quantity = remainingRanged;
                }
                remainingRanged -= quantity;
                remaining -= quantity;
                addToSquad(squad, squadMap, eq, quantity);
            }
        }
        for (Equipment eq : fullGroup) {
            if (eq.getQuantity() == 0) continue;
            if (remaining == 0) break;
            if (remainingMounted == 0) break;
            ExpeditionUnit unit = (ExpeditionUnit) eq.getItem();
            if (unit.isWounded()) continue;
            if (unit.isMounted()) {
                int quantity = eq.getQuantity();
                if (quantity > remainingMounted) {
                    quantity = remainingMounted;
                }
                remainingMounted -= quantity;
                remaining -= quantity;
                addToSquad(squad, squadMap, eq, quantity);
            }
        }
        for (Equipment eq : fullGroup) {
            if (eq.getQuantity() == 0) continue;
            if (remaining == 0) break;
            ExpeditionUnit unit = (ExpeditionUnit) eq.getItem();
            if (unit.isWounded()) continue;
            if (!unit.isMounted() && !unit.isRangedAttack()) {
                int quantity = eq.getQuantity();
                if (quantity > remaining) {
                    quantity = remaining;
                }
                remaining -= quantity;
                addToSquad(squad, squadMap, eq, quantity);
            }
        }
        for (Equipment eq : fullGroup) {
            if (eq.getQuantity() == 0) continue;
            if (remaining == 0) break;
            ExpeditionUnit unit = (ExpeditionUnit) eq.getItem();
            if (unit.isWounded()) continue;
            int quantity = eq.getQuantity();
            if (quantity > remaining) {
                quantity = remaining;
            }
            remaining -= quantity;
            addToSquad(squad, squadMap, eq, quantity);
        }
        for (Equipment eq : fullGroup) {
            if (eq.getQuantity() == 0) continue;
            if (remaining == 0) break;
            int quantity = eq.getQuantity();
            if (quantity > remaining) {
                quantity = remaining;
            }
            remaining -= quantity;
            addToSquad(squad, squadMap, eq, quantity);
        }
        return squad;
    }

    private static void addToSquad(List<Equipment> squad, Map<String, Equipment> squadMap, Equipment eq, int quantity) {
        eq.setQuantity(eq.getQuantity() - quantity);
        Equipment current = squadMap.get(eq.getItem().getFullID());
        if (current == null) {
            Equipment clone = eq.clone();
            clone.setQuantity(quantity);
            squad.add(clone);
            squadMap.put(eq.getItem().getFullID(), clone);
        } else {
            current.setQuantity(current.getQuantity() + quantity);
        }
    }

    private static List<Equipment> orderByCombatValue(List<Equipment> fullGroup) {
        List<Equipment> ret = new ArrayList<Equipment>(fullGroup);
        Collections.sort(ret, new Comparator<Equipment>() {

            @Override
            public int compare(Equipment o1, Equipment o2) {
                return ((ExpeditionUnit) o2.getItem()).getPower() - ((ExpeditionUnit) o1.getItem()).getPower();
            }
        });
        return ret;
    }
}
