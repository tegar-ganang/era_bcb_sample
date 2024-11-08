package gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Ellipse2D;
import java.net.URL;
import java.util.ArrayList;
import java.util.Timer;
import javax.swing.*;
import javax.swing.border.Border;
import things.*;

/**
 * @author Tom Hargrave
 * @author Miranda Howe
 *
 */
public class TheatreView extends Box implements ActionListener, MouseListener {

    /**
	 * @serial
	 */
    private static final long serialVersionUID = -5723116862809346015L;

    public DrawingCanvas canvas;

    public ItemInfo item_info;

    private Home thespis;

    private ArrayList<Production> productions = new ArrayList<Production>();

    private ArrayList<Scene> scenes = new ArrayList<Scene>();

    private ArrayList<MyEllipse> temp_items = new ArrayList<MyEllipse>();

    private ArrayList<MyEllipse> frame_items = new ArrayList<MyEllipse>();

    private int current_production_ID = -1;

    private JList list;

    public JComboBox thing_list;

    private JScrollPane item_scroll_pane, canvas_scroll_pane;

    private Box main_box;

    public Box list_box;

    private JTextField search_box;

    public JTextArea errors_text;

    private JTextField current_scene_text_box;

    private JButton next_button = new JButton(resizeIcon(("right.png"), 20, 14));

    private JButton previous_button = new JButton(resizeIcon(("left.png"), 20, 14));

    private JLabel current_scene_name_label;

    private JTextField scene_name = new JTextField();

    private int current_scene = 0, current_frame = 0, last_frame = -1, next_frame = -1;

    public Timer t;

    public Animation play;

    public boolean pause = false;

    public boolean start = false;

    /**
	 * Theatre View Constructor
	 * @param i - denotes whether to create horizontal or vertical box
	 * @param t Thespis home object
	 */
    TheatreView(int i, Home t) {
        super(i);
        thespis = t;
        main_box = Box.createHorizontalBox();
        main_box.add(left());
        main_box.add(right());
        this.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        this.add(main_box);
        refreshProductions();
        int z = thespis.database.getAllProductions().size();
        if (z == 0) {
            createNewProduction(true);
        } else if (z == 1) {
            current_production_ID = thespis.database.latestProductionID();
            loadProduction(current_production_ID, true);
        } else {
            productions = thespis.database.getAllProductions();
            String[] possibilities = new String[productions.size()];
            for (int k = 0; k < productions.size(); k++) {
                possibilities[k] = productions.get(k).getName();
            }
            Icon icon = (Icon) resizeIcon(("open.png"), 30, 30);
            String s = (String) JOptionPane.showInputDialog(canvas, "Please choose a production to load:    \n" + "\n", "Loading Production", JOptionPane.PLAIN_MESSAGE, icon, possibilities, "");
            for (int k = 0; k < productions.size(); k++) {
                if (s == possibilities[k]) current_production_ID = productions.get(k).getID();
            }
            if (current_production_ID != -1) {
                loadProduction(current_production_ID, true);
            }
        }
    }

    /**
	 * @param isInit
	 */
    public void createNewProduction(boolean isInit) {
        if (!isInit) {
            saveProduction(current_production_ID);
        }
        if (!isInit) resetTheatre();
        Icon icon = (Icon) resizeIcon(("save.png"), 30, 30);
        String name = null;
        while (name == null) {
            name = (String) JOptionPane.showInputDialog(canvas, "New production being created...\n" + "Please enter a name for the production:    ", "New Production", JOptionPane.PLAIN_MESSAGE, icon, null, "");
            if (name == null) JOptionPane.showMessageDialog(canvas, "You must enter a production name", "Error", JOptionPane.ERROR_MESSAGE);
        }
        current_production_ID = thespis.database.latestProductionID();
        current_production_ID++;
        Production temp = new Production(name, "");
        thespis.database.addProduction(temp);
        refreshList();
        if (!isInit) thespis.toolbar.refreshProductionList();
        current_production_ID = thespis.database.latestProductionID();
        loadProduction(current_production_ID, isInit);
    }

    /**
	 * <p> Resizes ImageIcons </p>
	 * @param y The location of the image to be displayed. 
	 * @return an ImageIcon to be used in the navigation bar. 
	 */
    private ImageIcon resizeIcon(String y, int w, int h) {
        URL url = getClass().getResource(y);
        ImageIcon icon = new ImageIcon(url);
        Image img = icon.getImage();
        Image newimg = img.getScaledInstance(w, h, java.awt.Image.SCALE_SMOOTH);
        ImageIcon new_icon = new ImageIcon(newimg);
        return new_icon;
    }

    /**
	 * <p> Updates the list of the productions to synchronise with the database. </p>
	 */
    public void refreshProductions() {
        productions = thespis.database.getAllProductions();
    }

    /**
	 * <p> Adds a new item into the theatre </p>
	 * @param x New items x co-ordinate
	 * @param y New items y co-ordinate
	 * @param w New items witdth
	 * @param h New items height
	 * @param t New items object
	 */
    public void addItem(double x, double y, double w, double h, Thing t) {
        int scene_index = getSceneIndex(getCurrentScene());
        if (scene_index == -1) {
            String name = newSceneDialog();
            if (name == null) return;
            scenes.add(new Scene(name, "", getCurrentScene()));
            scene_index = getSceneIndex(getCurrentScene());
            scene_name.setText(name);
        }
        scenes.get(scene_index).addItem(t);
        getTempItems().add(new MyEllipse(x, y, w, h, scenes.get(scene_index).getItemIndex(t)));
    }

    /**
	 * Dialogue to ask a user to enter a new scene name
	 * @return Name of the new Scene being created
	 */
    private String newSceneDialog() {
        String name = null;
        Icon icon = (Icon) resizeIcon(("save.png"), 30, 30);
        while (name == null) {
            name = (String) JOptionPane.showInputDialog(canvas, "New scene being created...\n" + "Please enter a name for the scene: ", "New Scene", JOptionPane.PLAIN_MESSAGE, icon, null, "");
            if (name == null) return null;
        }
        return name;
    }

    /**
	 * Dialogue to ask a user to enter a new production name
	 * @return Name of the new production being created
	 */
    private String savingProductionDialog() {
        Icon icon = (Icon) resizeIcon(("save.png"), 30, 30);
        String name = null;
        while (name == null) {
            name = (String) JOptionPane.showInputDialog(canvas, "Saving Production...\n" + "Please enter a name for the production: ", "New Production", JOptionPane.PLAIN_MESSAGE, icon, null, "");
            if (name == null) JOptionPane.showMessageDialog(canvas, "You must enter a production name", "Error", JOptionPane.ERROR_MESSAGE);
        }
        return name;
    }

    /**
	 * Delete an item from the theatre
	 * @param i Index of item to be deleted
	 */
    public void delItem(int i) {
        getTempItems().remove(i);
        canvas.setHovering(false);
    }

    /**
	 * Gets an Item at given Index
	 * @param i Index of item
	 * @return
	 */
    public Ellipse2D getItem(int i) {
        return getTempItems().get(i);
    }

    /**
	 * Populates the frame_items array list with items from a given frame
	 * @param frame
	 */
    public void setFrameItems(int frame) {
        int sceneIndex;
        sceneIndex = getSceneIndex(getCurrentScene());
        if (sceneIndex != -1) {
            frame_items.clear();
            int frameIndex = scenes.get(sceneIndex).getFrameIndex(frame);
            if (frameIndex != -1) {
                for (int i = 0; i < scenes.get(sceneIndex).getKeyframes().get(frameIndex).getPositions().size(); i++) {
                    double x = scenes.get(sceneIndex).getKeyframes().get(frameIndex).getPositions().get(i).getX();
                    double y = scenes.get(sceneIndex).getKeyframes().get(frameIndex).getPositions().get(i).getY();
                    double w = scenes.get(sceneIndex).getKeyframes().get(frameIndex).getPositions().get(i).getW();
                    double h = scenes.get(sceneIndex).getKeyframes().get(frameIndex).getPositions().get(i).getH();
                    int index = scenes.get(sceneIndex).getKeyframes().get(frameIndex).getPositions().get(i).getIndex();
                    frame_items.add(new MyEllipse(x, y, w, h, index));
                }
            }
        }
        canvas.repaint();
    }

    /**
	 * Deletes all items in the theatre plan
	 */
    public void initialiseItems() {
        getTempItems().clear();
    }

    /**
	 * Get the index of a scene
	 * @param scene_num The scene number of the scene
	 * @return
	 */
    public int getSceneIndex(int scene_num) {
        for (int i = 0; i < scenes.size(); i++) {
            if (scenes.get(i).getScene_num() == scene_num) {
                return i;
            }
        }
        return -1;
    }

    /**
	 * Plays the animation
	 * @return true if the animation successfully played, false if not
	 */
    public boolean playAnimation() {
        if (pause == true) {
            pause = false;
            play.setPaused(false);
        } else {
            canvas.deleteBoundingRec();
            play = new Animation(thespis);
            int prev = getLastFrame();
            int next = getNextFrame();
            if (!start) {
                if (next == -1 && prev == -1) {
                    JOptionPane.showMessageDialog(canvas, "You must keyframe at least 2 frames to run keyframing.      ");
                } else if (next == -1 && prev != -1) {
                    JOptionPane.showMessageDialog(canvas, "Cannot run keyframing from here.      ");
                } else if (next != -1 && prev == -1) {
                    thespis.keyframe_bar.setKeyframeBarToFrame(thespis.keyframe_bar.getFrameSetIconIndex(next));
                } else if (prev != -1 && next != -1) {
                    t = new Timer();
                    play.initialiseAnimation();
                    t.schedule(play, 0, 100);
                    start = true;
                    pause = false;
                    canvas.play = true;
                    return true;
                }
            }
        }
        return false;
    }

    /**
	 * Skips through the animation to a given point
	 * @param x Point to skip the animation to. Relates to the pixel value along the timeline widget
	 */
    public void skipPlay(double x) {
        double position = thespis.keyframe_bar.getFrameDouble();
        if (x > position) {
            x = x - position;
            for (int i = 0; i < play.animation_items.size(); i++) {
                play.calculatePlay();
                play.animation_items.get(i).item.setFrame(play.animation_items.get(i).item.getX() + (x * play.animation_items.get(i).x_interval), play.animation_items.get(i).item.getY() + (x * play.animation_items.get(i).y_interval), play.animation_items.get(i).item.getWidth(), play.animation_items.get(i).item.getHeight());
            }
        } else {
            x = position - x;
            for (int i = 0; i < play.animation_items.size(); i++) {
                play.calculatePlay();
                play.animation_items.get(i).item.setFrame(play.animation_items.get(i).item.getX() - (x * play.animation_items.get(i).x_interval), play.animation_items.get(i).item.getY() - (x * play.animation_items.get(i).y_interval), play.animation_items.get(i).item.getWidth(), play.animation_items.get(i).item.getHeight());
            }
        }
    }

    /**
	 * Sets a keyframe at a given frame in a given scene in the current production
	 * @param scene
	 * @param frame
	 */
    public int keyframe(int scene, int frame) {
        ArrayList<Position> temp = new ArrayList<Position>();
        int scene_index = getSceneIndex(scene);
        if (scene_index == -1) {
            String name = newSceneDialog();
            if (name == null) return -1;
            scenes.add(new Scene(name, "", scene));
            scene_index = getSceneIndex(scene);
            scene_name.setText(name);
        }
        int frame_index = scenes.get(scene_index).getFrameIndex(frame);
        boolean proceed = true;
        if (frame_index != -1) {
            if (JOptionPane.showConfirmDialog(canvas, "There is already a frame keyframed here, would you like to overwrite it?") == 0) {
                proceed = true;
            } else {
                proceed = false;
            }
        }
        if (proceed) {
            for (int i = 0; i < getTempItems().size(); i++) {
                temp.add(new Position(getTempItems().get(i).getIndex(), getTempItems().get(i).getX(), getTempItems().get(i).getY(), getTempItems().get(i).getWidth(), getTempItems().get(i).getHeight()));
            }
            for (int i = 0; i < getFrameItems().size(); i++) {
                temp.add(new Position(getFrameItems().get(i).getIndex(), getFrameItems().get(i).getX(), getFrameItems().get(i).getY(), getFrameItems().get(i).getWidth(), getFrameItems().get(i).getHeight()));
            }
            scenes.get(scene_index).keyframe(frame_index, new Keyframe(frame, temp));
            canvas.deleteBoundingRec();
            frame_index = scenes.get(scene_index).getFrameIndex(frame);
            if (frame_index != -1) scenes.get(scene_index).getKeyframes().get(frame_index).setPositions(temp);
            initialiseItems();
            setLastFrame(thespis.keyframe_bar.getFrame());
            setCurrentFrame(thespis.keyframe_bar.getFrame());
            canvas.repaint();
            return 1;
        }
        return -1;
    }

    /**
	 * Resets a scene, deleting all items in the theatre plan from that scene
	 * @param i - the number of the scene being reset
	 */
    public void resetScene(int i) {
        for (int j = 0; j < scenes.size(); j++) {
            if (scenes.get(j).getScene_num() == i) {
                thespis.keyframe_bar.resetScene(i);
                item_info.setNullInfo();
                scenes.remove(j);
                canvas.deleteBoundingRec();
                temp_items.clear();
                frame_items.clear();
                thespis.keyframe_bar.repaint();
                canvas.repaint();
                last_frame = -1;
                next_frame = -1;
                thespis.keyframe_bar.setFrameDouble(15);
                scene_name.setText("[Enter Scene Name]");
                item_info.setNullInfo();
            }
        }
    }

    /**
	 * Deletes a given keyframe
	 * @param f Frame number
	 */
    public void deleteFrame(int f) {
        int scene_index = getSceneIndex(getCurrentScene());
        int frame_index = scenes.get(scene_index).getFrameIndex(f);
        getScenes().get(scene_index).getKeyframes().remove(frame_index);
    }

    /**
	 * Function to set all the components of the left side of the splitPane in the Theatre View
	 * @return A box containing a list of items from the database 
	 */
    private Box left() {
        Box box = Box.createVerticalBox();
        box.setPreferredSize(new Dimension(140, 530));
        list_box = Box.createVerticalBox();
        thing_list = new JComboBox(new String[] { "Props", "Set Dressing", "Actors", "Search Results" });
        Dimension combo_box_size = new Dimension(120, 50);
        thing_list.setMaximumSize(combo_box_size);
        thing_list.addActionListener(this);
        if (thespis.database.getAllProps().isEmpty()) {
            list = new JList();
        } else {
            list = new JList(thespis.database.getAllProps().toArray());
        }
        list.setSelectionBackground(Color.LIGHT_GRAY);
        list.setSelectedIndex(0);
        Dimension list_size = new Dimension(100, 400);
        list.setPreferredSize(list_size);
        search_box = new JTextField("[Search items]");
        Dimension text_box_size = new Dimension(150, 100);
        search_box.setMaximumSize(text_box_size);
        search_box.addActionListener(this);
        search_box.addMouseListener(this);
        item_scroll_pane = new JScrollPane(list);
        item_scroll_pane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        item_scroll_pane.setPreferredSize(new Dimension(125, 300));
        item_info = new ItemInfo();
        Border border = BorderFactory.createLineBorder(Color.BLACK, 1);
        list_box.setBorder(border);
        list_box.add(thing_list);
        list_box.add(item_scroll_pane);
        list_box.add(search_box);
        box.add(list_box);
        return box;
    }

    /**
	 * Function to set all the components of the right side of the splitPane in the Theatre View
	 * @return A scrollPane containing the view of the theatre. 
	 */
    private JScrollPane right() {
        Dimension d = new Dimension(700, 530);
        Border border = BorderFactory.createEtchedBorder(Color.BLACK, Color.BLACK);
        Box canvas_box = Box.createVerticalBox();
        canvas = new DrawingCanvas(thespis);
        canvas.setPreferredSize(d);
        canvas_box.add(sceneInformation());
        canvas_box.add(canvas);
        canvas_box.setBorder(border);
        canvas_scroll_pane = new JScrollPane(canvas_box);
        canvas_scroll_pane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        canvas_scroll_pane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        return canvas_scroll_pane;
    }

    /**
	 * Creates a toolbar for navigation through scenes
	 * @return
	 */
    private JToolBar sceneInformation() {
        JToolBar timeline_toolbar = new JToolBar("Timeline Toolbar");
        timeline_toolbar.addSeparator();
        current_scene_name_label = new JLabel("Current Scene:  ");
        current_scene_text_box = new JTextField("1");
        current_scene_text_box.setPreferredSize(new Dimension(30, 23));
        scene_name.setPreferredSize(new Dimension(470, 23));
        scene_name.setText("[Enter Scene Name]");
        scene_name.addActionListener(this);
        current_scene_text_box.addActionListener(this);
        timeline_toolbar.add(current_scene_name_label);
        timeline_toolbar.add(scene_name);
        timeline_toolbar.add(previous_button);
        timeline_toolbar.add(current_scene_text_box);
        timeline_toolbar.add(next_button);
        timeline_toolbar.setAlignmentX(Component.CENTER_ALIGNMENT);
        timeline_toolbar.setFloatable(false);
        timeline_toolbar.setRollover(true);
        next_button.addActionListener(this);
        previous_button.addActionListener(this);
        this.add(timeline_toolbar);
        return timeline_toolbar;
    }

    /**
	 * This function refreshes the list displayed on the LHS of the screen
	 * It obtains an up to date list of all the relevant items in the database. 
	 */
    public void refreshList() {
        switch(thing_list.getSelectedIndex()) {
            case 0:
                {
                    list = new JList(thespis.database.getAllProps().toArray());
                    break;
                }
            case 1:
                {
                    list = new JList(thespis.database.getAllSetDressings().toArray());
                    break;
                }
            case 2:
                {
                    list = new JList(thespis.database.getAllActors().toArray());
                    break;
                }
            default:
                break;
        }
        item_scroll_pane = new JScrollPane(list);
        item_scroll_pane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        list.setSelectionBackground(Color.LIGHT_GRAY);
        list_box.remove(1);
        list_box.add(item_scroll_pane, 1);
        search_box.setText("[Search items]");
        list_box.validate();
        this.validate();
    }

    /**
	 * Searches the database for the text currently in the searchBox. 
	 */
    private void search() {
        if (search_box.getText().equals("")) {
            refreshList();
        } else {
            String search_string = search_box.getText();
            ArrayList<Thing> result_list = thespis.database.searchOnString(search_string);
            list = new JList(result_list.toArray());
            item_scroll_pane = new JScrollPane(list);
            item_scroll_pane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            list_box.remove(1);
            list_box.add(item_scroll_pane, 1);
            list_box.validate();
            this.validate();
            thing_list.setSelectedIndex(3);
            search_box.setText(search_string);
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(next_button)) {
            int x = setCurrentScene(getCurrentScene() + 1);
            if (x != -1) current_scene_text_box.setText(Integer.toString((Integer.parseInt(current_scene_text_box.getText()) + 1)));
            thespis.keyframe_bar.repaint();
        } else if (e.getSource().equals(previous_button)) {
            int x = setCurrentScene(getCurrentScene() - 1);
            if (Integer.parseInt(current_scene_text_box.getText()) != 0 && x != -1) {
                current_scene_text_box.setText(Integer.toString((Integer.parseInt(current_scene_text_box.getText()) - 1)));
            }
            thespis.keyframe_bar.repaint();
        }
        if (e.getSource().equals(search_box)) {
            search();
        } else {
            refreshList();
        }
        if (e.getSource().equals(scene_name)) {
            if (getSceneIndex(current_scene) != -1) {
                if (scenes.get(getSceneIndex(current_scene)) != null) scenes.get(getSceneIndex(current_scene)).setName(scene_name.getText());
            } else {
                scenes.add(new Scene(scene_name.getText(), "", getCurrentScene()));
            }
            JOptionPane.showMessageDialog(canvas, "Scene Name changed to: " + scene_name.getText());
        }
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
        search_box.setText("");
    }

    public void mouseExited(MouseEvent e) {
        search_box.setText("[Search Items]");
    }

    /**
	 * Sets the last frame variable
	 * @param last_frame
	 */
    public void setLastFrame(int last_frame) {
        this.last_frame = last_frame;
        setFrameItems(last_frame);
    }

    /**
	 * Gets the last frame variable
	 * @return
	 */
    public int getLastFrame() {
        return last_frame;
    }

    /**
	 * Sets the next frame variable
	 * @param next_frame
	 */
    public void setNextFrame(int next_frame) {
        this.next_frame = next_frame;
    }

    /**
	 * Gets the next frame variable
	 * @return
	 */
    public int getNextFrame() {
        return next_frame;
    }

    /**
	 * Gets the temp items Array List
	 * @return
	 */
    public ArrayList<MyEllipse> getTempItems() {
        return temp_items;
    }

    /**
	 * Gets the frame items Array List
	 * @return
	 */
    public ArrayList<MyEllipse> getFrameItems() {
        return frame_items;
    }

    /**
	 * Gets the Scenes Array List
	 * @return
	 */
    public ArrayList<Scene> getScenes() {
        return scenes;
    }

    /**
	 * Sets the Scenes Array List
	 * @param x
	 */
    public void setScenes(ArrayList<Scene> x) {
        scenes = x;
    }

    /**
	 * Gets the number of Temp Items in the theatre
	 * @return
	 */
    public int getItemNo() {
        return getTempItems().size();
    }

    /**
	 * Sets the current scene
	 * @param i - the current scene number
	 */
    public int setCurrentScene(int i) {
        if (getSceneIndex(i) == -1) {
            String name = newSceneDialog();
            if (name == null) return -1;
            scenes.add(new Scene(name, "", i));
            scene_name.setText(name);
        }
        getTempItems().clear();
        getFrameItems().clear();
        canvas.deleteBoundingRec();
        current_scene = i;
        thespis.keyframe_bar.calculateLastAndNextFrame();
        if (scenes.get(getSceneIndex(i)).getName() == null) {
            scene_name.setText("[Enter Scene Name]");
        } else {
            scene_name.setText(scenes.get(getSceneIndex(i)).getName());
        }
        canvas.repaint();
        thespis.keyframe_bar.repaint();
        return 1;
    }

    /**
	 * Gets the Current Scene number
	 * @return
	 */
    public int getCurrentScene() {
        return current_scene;
    }

    /**
	 * Sets the current frame
	 * @param i - the current frame number
	 */
    public void setCurrentFrame(int i) {
        current_frame = i;
        canvas.deleteBoundingRec();
    }

    /**
	 * Gets the Current Frame number
	 * @return the current frame
	 */
    public int getCurrentFrame() {
        return current_frame;
    }

    /**
	 * Saves a production
	 * @param production_ID
	 */
    public void saveProduction(int production_ID) {
        Production temp;
        if (production_ID == -1) {
            String name = savingProductionDialog();
            temp = new Production(name, "", scenes);
            setCurrentProductionID(thespis.database.latestProductionID());
            thespis.database.addProduction(temp);
            refreshList();
            thespis.database.saveProduction(temp);
            thespis.toolbar.refreshProductionList();
        } else {
            temp = thespis.database.loadProduction(production_ID);
            temp.setScenes(scenes);
            thespis.database.saveProduction(temp);
        }
    }

    /**
	 * Resets the whole Theatre
	 */
    public void resetTheatre() {
        scenes.clear();
        temp_items.clear();
        frame_items.clear();
        thespis.keyframe_bar.frame_set_icon.clear();
        current_production_ID = -1;
        current_scene = 0;
        current_frame = 0;
        last_frame = -1;
        next_frame = -1;
        current_scene_text_box.setText("1");
        thespis.keyframe_bar.setFrameDouble(15);
        item_info.setNullInfo();
        canvas.deleteBoundingRec();
        scene_name.setText("[Enter Scene Name]");
        if (getSceneIndex(current_scene) == -1) {
        } else if (scenes.get(getSceneIndex(current_scene)).getName() == null) {
            scene_name.setText("[Enter Scene Name]");
        } else {
            scene_name.setText(scenes.get(getSceneIndex(current_scene)).getName());
        }
        canvas.repaint();
        thespis.keyframe_bar.repaint();
    }

    /**
	 * Loads a production
	 * @param production_ID
	 */
    public void loadProduction(int production_ID, boolean is_init) {
        setScenes(thespis.database.loadProduction(production_ID).getScenes());
        if (!is_init) thespis.keyframe_bar.loadProduction();
        if (!is_init) thespis.toolbar.setProductionMenuItemSelected(production_ID);
        setCurrentProductionID(production_ID);
        if (getSceneIndex(current_scene) != -1) {
            if (scenes.get(getSceneIndex(current_scene)) != null) scene_name.setText(scenes.get(getSceneIndex(current_scene)).getName());
        }
    }

    /**
	 * Sets the current production ID variable.
	 * @param current_production_ID
	 */
    public void setCurrentProductionID(int current_production_ID) {
        this.current_production_ID = current_production_ID;
    }

    /**
	 * Gets the current production ID variable.
	 * @return
	 */
    public int getCurrentProductionID() {
        return current_production_ID;
    }

    /**
	 * Gets the Index of the selected item from the item list
	 * @return The selected index of the main itemList on the LHS. 
	 */
    public int getListIndex() {
        return list.getSelectedIndex();
    }

    /**
	 * Gets the value of the selected item in the item list
	 * @return The Thing which is currently selected in the ItemList on the LHS. 
	 */
    public Thing getListValue() {
        return (Thing) list.getSelectedValue();
    }
}
