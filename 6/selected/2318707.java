package starcraft.comm.tests;

import java.util.Map;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import starcraft.comm.client.StarCraftMessageClient;
import starcraft.gameclient.ClientRuntime;
import starcraft.gamemodel.model.races.protoss.TassadarFaction;
import starcraft.gamemodel.model.races.protoss.units.DragoonUnitType;
import starcraft.gamemodel.model.races.protoss.units.ZealotUnitType;
import starcraft.gamemodel.model.races.terrans.JimRaynorFaction;
import starcraft.gamemodel.model.races.terrans.units.FirebatUnitType;
import starcraft.gamemodel.model.races.terrans.units.MarineUnitType;
import starcraft.gamemodel.model.races.zerg.TheOvermindFaction;
import starcraft.gamemodel.model.races.zerg.units.HydraliskUnitType;
import starcraft.gamemodel.model.races.zerg.units.ZerglingUnitType;
import starcraft.gameserver.ServerRuntime;
import common.utilities.DeepEquals;
import common.utilities.SystemUtils;

public class TestScenario2 {

    public static void main(String[] args) throws Exception {
        SystemUtils.initializeLogging(TestScenario2.class);
        ServerRuntime.setup(8000);
        String player_1 = "Nicolas";
        String player_2 = "Marc";
        String player_3 = "Nico";
        ClientRuntime cr_1 = new ClientRuntime(new TestClientEventHandler2());
        StarCraftMessageClient sc_1 = cr_1.getMessageClient();
        sc_1.connect("127.0.0.1", 8000, 2000);
        sc_1.login(player_1);
        ClientRuntime cr_2 = new ClientRuntime(new TestClientEventHandler2());
        StarCraftMessageClient sc_2 = cr_2.getMessageClient();
        sc_2.connect("127.0.0.1", 8000, 2000);
        sc_2.login(player_2);
        ClientRuntime cr_3 = new ClientRuntime(new TestClientEventHandler2());
        StarCraftMessageClient sc_3 = cr_3.getMessageClient();
        sc_3.connect("127.0.0.1", 8000, 2000);
        sc_3.login(player_3);
        waitFor(1000);
        System.out.println("--- before: create game --- ");
        sc_1.createGame("battle ground");
        System.out.println("--- after : create game --- ");
        sc_1.joinGame("battle ground");
        sc_2.joinGame("battle ground");
        sc_3.joinGame("battle ground");
        sc_1.setPlayerFaction(TheOvermindFaction.INSTANCE);
        sc_2.setPlayerFaction(JimRaynorFaction.INSTANCE);
        sc_3.setPlayerFaction(TassadarFaction.INSTANCE);
        sc_1.setStartPlayer("Marc");
        sc_1.setPlayerReady(true);
        sc_2.setPlayerReady(true);
        sc_3.setPlayerReady(true);
        Map<String, String[]> drawnPlanets1 = sc_1.getDrawnPlanets();
        Map<String, String[]> drawnPlanets2 = sc_2.getDrawnPlanets();
        Map<String, String[]> drawnPlanets3 = sc_3.getDrawnPlanets();
        if (!DeepEquals.deepEquals(drawnPlanets1, drawnPlanets2)) {
            System.out.println("drawn planets don't match");
        }
        if (!DeepEquals.deepEquals(drawnPlanets1, drawnPlanets3)) {
            System.out.println("drawn planets don't match");
        }
        String[] drawnPlanets_1 = drawnPlanets1.get(player_1);
        String[] drawnPlanets_2 = drawnPlanets1.get(player_2);
        String[] drawnPlanets_3 = drawnPlanets1.get(player_3);
        sc_1.placePlanet(drawnPlanets_1[0], 0, 0, 0, 0);
        sc_1.doneWithTurn();
        waitFor(1000);
        sc_2.placePlanet(drawnPlanets_2[0], -1, 0, 0, null);
        sc_2.doneWithTurn();
        waitFor(1000);
        sc_3.placePlanet(drawnPlanets_3[0], 1, 1, 0, null);
        sc_3.doneWithTurn();
        waitFor(1000);
        sc_3.placePlanet(drawnPlanets_3[1], 1, 2, 0, 1);
        sc_3.doneWithTurn();
        waitFor(1000);
        sc_2.placePlanet(drawnPlanets_2[1], 2, 1, 0, 2);
        sc_2.doneWithTurn();
        waitFor(1000);
        sc_1.placePlanet(drawnPlanets_1[1], 2, 2, 0, 2);
        sc_1.doneWithTurn();
        waitFor(1000);
        sc_1.placeStartingForce(new String[] { ZerglingUnitType.INSTANCE.getName(), ZerglingUnitType.INSTANCE.getName(), HydraliskUnitType.INSTANCE.getName() }, new byte[] { 0, 0, 1 }, drawnPlanets_1[0], (byte) 1);
        sc_1.doneWithTurn();
        waitFor(1000);
        sc_2.placeStartingForce(new String[] { MarineUnitType.INSTANCE.getName(), MarineUnitType.INSTANCE.getName(), FirebatUnitType.INSTANCE.getName() }, new byte[] { 0, 0, 1 }, drawnPlanets_3[1], (byte) 0);
        sc_2.doneWithTurn();
        waitFor(1000);
        sc_3.placeStartingForce(new String[] { ZealotUnitType.INSTANCE.getName(), ZealotUnitType.INSTANCE.getName(), DragoonUnitType.INSTANCE.getName() }, new byte[] { 0, 0, 1 }, drawnPlanets_2[1], (byte) 1);
        sc_3.doneWithTurn();
        waitFor(1000);
        showBlockingDialog();
        sc_1.disconnect();
        waitFor(1000);
        sc_2.disconnect();
        waitFor(1000);
        sc_3.disconnect();
        waitFor(1000);
        ServerRuntime.shutdown();
    }

    private static void waitFor(int milliseconds) throws InterruptedException {
        Object monitor = new Object();
        synchronized (monitor) {
            monitor.wait(milliseconds);
        }
    }

    private static void showBlockingDialog() {
        JFrame frame = new JFrame(TestScenario2.class.getName());
        frame.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        frame.setVisible(true);
        JOptionPane.showConfirmDialog(frame, "waiting...");
        frame.setVisible(false);
        frame.dispose();
    }
}
