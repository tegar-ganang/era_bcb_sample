package net.slashie.expedition.domain;

import java.util.List;
import net.slashie.expedition.domain.Expedition.MovementMode;
import net.slashie.expedition.game.ExpeditionGame;
import net.slashie.expedition.game.ExpeditionMusicManager;
import net.slashie.expedition.ui.ExpeditionUserInterface;
import net.slashie.expedition.ui.oryx.ExpeditionOryxUI;
import net.slashie.serf.action.Actor;
import net.slashie.serf.game.Equipment;
import net.slashie.serf.ui.UserInterface;

public class ShipCache extends GoodsCache {

    private static final long serialVersionUID = 1L;

    private List<Vehicle> vehicles;

    public ShipCache(ExpeditionGame game, List<Vehicle> vehicles) {
        super(game, "SHIP");
        this.vehicles = vehicles;
    }

    public int getCarryable(ExpeditionItem item) {
        return (int) Math.floor((getCarryCapacity() - getCurrentWeight()) / item.getWeight());
    }

    public int getCarryCapacity() {
        int carryCapacity = 0;
        for (Vehicle equipment : vehicles) {
            carryCapacity += equipment.getCarryCapacity();
        }
        return carryCapacity;
    }

    public int getTotalShips() {
        return vehicles.size();
    }

    public int getCurrentWeight() {
        int currentlyCarrying = 0;
        List<Equipment> inventory = getItems();
        for (Equipment equipment : inventory) {
            if (!(equipment.getItem() instanceof Vehicle)) {
                currentlyCarrying += ((ExpeditionItem) equipment.getItem()).getWeight() * equipment.getQuantity();
            }
            if (equipment.getItem() instanceof ExpeditionUnit) {
                currentlyCarrying += ((ExpeditionItem) equipment.getItem()).getWeight() * equipment.getQuantity();
            }
        }
        return currentlyCarrying;
    }

    @Override
    public String getClassifierID() {
        return "Ship";
    }

    @Override
    public String getDescription() {
        return "Ships";
    }

    @Override
    public void onStep(Actor a) {
        if (a instanceof Expedition && !(a instanceof NonPrincipalExpedition)) {
            boolean isGFXUI = UserInterface.getUI() instanceof ExpeditionOryxUI;
            int choice = -1;
            if (isGFXUI) {
                choice = UserInterface.getUI().switchChat("Ships", "What do you want to do?", "Transfer equipment", "Board ships", "Do nothing");
                choice++;
            } else {
                choice = UserInterface.getUI().switchChat("Ships", "What do you want to do?", "Transfer to expedition", "Transfer to ships", "Board ships", "Do nothing");
            }
            switch(choice) {
                case 0:
                    ((ExpeditionUserInterface) UserInterface.getUI()).transferFromCache("Select the goods to transfer", null, this);
                    break;
                case 1:
                    ((ExpeditionUserInterface) UserInterface.getUI()).transferFromExpedition(this);
                    break;
                case 2:
                    if (canCarryWeight(((Expedition) a).getWeightToBoardShip())) {
                        boardShips((Expedition) a);
                    } else {
                        UserInterface.getUI().showMessage("The ships are too full!");
                    }
                    break;
                case 3:
            }
        }
    }

    public void boardShips(Expedition expedition) {
        expedition.setMovementMode(MovementMode.SHIP);
        expedition.setCurrentVehicles(vehicles);
        expedition.addAllItems(getItems());
        expedition.getLevel().destroyFeature(this);
        expedition.setPosition(getPosition());
        expedition.setAnchored(true);
        ExpeditionMusicManager.playTune("SEA");
    }

    @Override
    public boolean canCarry(ExpeditionItem item, int quantity) {
        return getCurrentWeight() + item.getWeight() * quantity <= getCarryCapacity();
    }

    public boolean canCarryWeight(int weight) {
        return getCurrentWeight() + weight <= getCarryCapacity();
    }

    @Override
    public boolean isInfiniteCapacity() {
        return false;
    }

    @Override
    public boolean destroyOnEmpty() {
        return false;
    }

    @Override
    public boolean requiresUnitsToContainItems() {
        return false;
    }

    @Override
    public String getTypeDescription() {
        return "Ships";
    }
}
