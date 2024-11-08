package net.playbesiege.entities;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import javax.swing.JFileChooser;
import net.playbesiege.MainInit;
import net.playbesiege.Settings;
import net.playbesiege.gui.FinalWinningDialog;
import net.playbesiege.gui.LevelsFile;
import net.playbesiege.gui.LoosingDialog;
import net.playbesiege.gui.SelectedPlanet;
import net.playbesiege.gui.WinningDialog;
import net.playbesiege.utils.CoolLine;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Vector3;
import com.ardor3d.scenegraph.Line;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.controller.SpatialController;
import com.ardor3d.scenegraph.event.DirtyType;

public class Level implements SpatialController<Spatial> {

    public static final String[] levels = new String[] { "canis_minor", "aries", "leo_minor", "grosser_wagen", "aquila", "auriga", "draco", "canis_major", "andromeda", "ursa_major", "eridanus" };

    private final MainInit main;

    public final Node levelNode = new Node();

    private int levelPos = 0;

    public Level(MainInit main) {
        this.main = main;
        load(levels[levelPos]);
        main.root.attachChild(levelNode);
        levelNode.addController(this);
    }

    public Node getNode() {
        return levelNode;
    }

    public void load(String name) {
        int index = Arrays.asList(levels).indexOf(name);
        if (index >= 0) levelPos = index;
        load(Level.class.getResource("/net/playbesiege/levels/" + name + ".level"));
    }

    public void load(URL levelURL) {
        removeAllEntities();
        Research.reset();
        Player.HUMAN.credits = 0;
        if (SelectedPlanet.instance != null) if (SelectedPlanet.instance != null && SelectedPlanet.getInstance(main, null).getParent() != null) SelectedPlanet.getInstance(main, null).close();
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            InputStream is = levelURL.openStream();
            int b = 0;
            while ((b = is.read()) != -1) baos.write(b);
            is.close();
            String buffer = baos.toString();
            String[] lines = buffer.split("\n");
            for (String line : lines) {
                String[] meta_and_pos_and_name = line.split("#");
                String meta = meta_and_pos_and_name[0];
                String pos = meta_and_pos_and_name[1];
                String name = "";
                if (meta_and_pos_and_name.length > 2) name = meta_and_pos_and_name[2];
                String[] xyz = pos.split(":");
                float x = Float.parseFloat(xyz[0]);
                float y = Float.parseFloat(xyz[1]);
                float z = Float.parseFloat(xyz[2]);
                String[] type_and_count_and_radius = meta.split(":");
                Player player = Player.valueOf(type_and_count_and_radius[0]);
                int count = Integer.parseInt(type_and_count_and_radius[1]);
                int radius = Integer.parseInt(type_and_count_and_radius[2]);
                if (name.equals("Camera")) {
                    try {
                        Planet humanPlanet = Player.HUMAN.getPlanets().get(0);
                        Vector3 newDirection = new Vector3();
                        main.cameraTranslationNode.setTranslation(0, 0, newDirection.set(x, y, z).subtractLocal(humanPlanet.getPosition()).length());
                        newDirection.set(x, y, z).subtractLocal(humanPlanet.getPosition()).normalizeLocal();
                        main.cameraRotationNode.setTranslation(humanPlanet.getPosition());
                        boolean matrix3LookAt = false;
                        if (matrix3LookAt) {
                            ((Matrix3) main.cameraRotationNode.getRotation()).lookAt(newDirection, main.worldUp);
                            main.cameraRotationNode.markDirty(DirtyType.Transform);
                        } else {
                            Quaternion transformAngle = new Quaternion();
                            transformAngle.lookAt(newDirection, main.worldUp);
                            main.cameraRotationNode.setRotation(transformAngle);
                            transformAngle.lookAt(Vector3.NEG_UNIT_Z, main.worldUp);
                            main.cameraTranslationNode.setRotation(transformAngle);
                        }
                        main.updateListenerPosition();
                    } catch (Exception e) {
                        System.out.println("camera planet must be in level file after a human planet!");
                        System.out.println(e.getMessage());
                    }
                    continue;
                }
                Planet p = null;
                switch(player) {
                    case HUMAN:
                        p = new Planet(main, Player.HUMAN, name, new Vector3(x, y, z), radius / 20d);
                        this.getNode().attachChild(p.getNode());
                        break;
                    case COMPUTER:
                        p = new Planet(main, Player.COMPUTER, name, new Vector3(x, y, z), radius / 20d);
                        this.getNode().attachChild(p.getNode());
                        break;
                    case NEUTRAL:
                        p = new Planet(main, Player.NEUTRAL, name, new Vector3(x, y, z), radius / 20d);
                        this.getNode().attachChild(p.getNode());
                        break;
                }
                for (int i = 0; i < count; i++) {
                    p.spawnNewFighter();
                }
            }
            for (Planet s : Planet.planets) {
                for (Planet s2 : Planet.planets) {
                    double distanceSquared = s.getPosition().distanceSquared(s2.getPosition());
                    if (distanceSquared < Settings.MAX_TRAVEL_DISTANCE_SQUARED) {
                        Entities.connect(s, s2);
                        if (Settings.showPlanetConnectingLines) {
                            Line line = CoolLine.get(s.getPosition(), s2.getPosition());
                            line.setName("path");
                            main.root.attachChild(line);
                        }
                    }
                }
            }
            Planet humanPlanet = Player.HUMAN.getPlanets().get(0);
            if (humanPlanet != null) {
                main.lookHere = new Vector3(Vector3.NEG_UNIT_X);
                main.cameraRotationNode.setTranslation(humanPlanet.getPosition());
            }
            main.timer.reset();
            Laser.lastTime = 0;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void removeAllEntities() {
        List<Fighter> fighters = Fighter.fighters;
        for (int i = fighters.size() - 1; i >= 0; i--) {
            Fighter fighter = fighters.get(i);
            fighter.remove();
        }
        List<Planet> planets = Planet.planets;
        for (int i = planets.size() - 1; i >= 0; i--) {
            Planet planet = planets.get(i);
            planet.remove();
        }
        List<Spatial> children = main.root.getChildren();
        for (int i = 0; i < children.size(); i++) {
            Spatial c = children.get(i);
            if (c instanceof Line && c.getName().equals("path")) {
                c.removeFromParent();
                i--;
            }
        }
    }

    public void loadNextLevel() {
        if (levelPos < levels.length - 1) levelPos++;
        load(levels[levelPos]);
    }

    public void restartLevel() {
        levelPos--;
        loadNextLevel();
    }

    double artificialIntelligenceAge = 0;

    public void update(double time, Spatial caller) {
        artificialIntelligenceAge += time;
        if (artificialIntelligenceAge > 5) {
            int countBlue = 0;
            int countRed = 0;
            for (Player player : Player.values()) {
                for (Fighter fighter : player.getFighters()) {
                    if (fighter.getPlayer() == Player.HUMAN) {
                        countBlue++;
                    }
                    if (fighter.getPlayer() == Player.COMPUTER) {
                        countRed++;
                    }
                }
                for (Planet planet : player.getPlanets()) {
                    if (planet.getPlayer() == Player.HUMAN) {
                        countBlue++;
                    }
                    if (planet.getPlayer() == Player.COMPUTER) {
                        countRed++;
                    }
                }
            }
            int allObjects = Fighter.fighters.size() + Planet.planets.size();
            if (countBlue == allObjects) {
                LevelsFile.getInstance().setProperty(Level.levels[levelPos], "1");
                if (levelPos < Level.levels.length - 1) LevelsFile.getInstance().setProperty(Level.levels[levelPos + 1], "1");
                LevelsFile.getInstance().save();
                if (levelPos < levels.length - 1) {
                    WinningDialog wd = WinningDialog.getInstance(main);
                    main.getHud().add(wd);
                } else {
                    FinalWinningDialog wd = FinalWinningDialog.getInstance(main);
                    main.getHud().add(wd);
                }
            }
            if (countRed == allObjects) {
                LoosingDialog ld = LoosingDialog.getInstance(main);
                main.getHud().add(ld);
            }
            artificialIntelligenceAge -= 5;
        }
    }

    public void showFileChooser() {
        if (MainInit.instance.settings.isFullScreen()) {
            System.out.println("Filechooser unsupported in FullScreen mode.");
            return;
        }
        JFileChooser jfc = new JFileChooser(Settings.levelDir);
        int showSaveDialog = jfc.showOpenDialog(null);
        if (showSaveDialog != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File f = jfc.getSelectedFile();
        try {
            main.getLevel().load(f.toURI().toURL());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
