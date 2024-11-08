import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import java.util.Vector;
import opencard.core.terminal.*;
import opencard.core.service.*;
import opencard.core.event.*;
import opencard.core.util.*;
import com.ibutton.oc.*;
import com.ibutton.oc.terminal.jib.*;

/**
 *  Class for modelling the farmyard. This class is the primary class for
 *  managing and displaying the farmyard.
 *
 *  <p>
 *  This figure illustrates the farmyard configuration
 *  (with three location panels):
 *  <p><img src="doc-files/farmyard.gif" width=444 height=256><p>
 *
 *  @author Henrik Eriksson
 *  @version 0.01
 *  @see LocationPanel
 */
public class Farmyard {

    /** The window frame for the farmyard. */
    protected Frame window = new Frame("The animal farm");

    /** The locations pull-down menu for the farmyard. */
    private Menu locations_menu = new Menu("Locations");

    /** The currently selected location panel. */
    private LocationPanel selected_location_panel = null;

    /** The container for the location panels on the farmyard window. */
    protected Container location_area = new Panel();

    /** A vector holding the location panels on the farmyard. */
    private Vector location_panels = new Vector();

    /**
   *  Creates a new farmyard. This constructor initializes and shows the
   *  farmyard window. Also, it will add listeners for iButton insertion
   *  and removal.
   *
   */
    public Farmyard() {
        window.setSize(800, 600);
        Dimension ss = window.getToolkit().getScreenSize();
        Dimension ws = window.getSize();
        window.setLocation((ss.width - ws.width) / 2, (ss.height - ws.height) / 2);
        MenuBar mb = new MenuBar();
        Menu m = new Menu("File");
        MenuItem mi = new MenuItem("Exit");
        mi.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                window.dispose();
                try {
                    SmartCard.shutdown();
                } catch (CardTerminalException e) {
                    e.printStackTrace();
                }
                System.exit(0);
            }
        });
        mi.setShortcut(new MenuShortcut(KeyEvent.VK_Q));
        m.add(new MenuItem("-"));
        m.add(mi);
        mb.add(m);
        mb.add(locations_menu);
        window.setMenuBar(mb);
        window.addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                e.getWindow().dispose();
                try {
                    SmartCard.shutdown();
                } catch (CardTerminalException ex) {
                    ex.printStackTrace();
                }
                System.exit(0);
            }
        });
        Container window_area = new Panel(new GridBagLayout());
        window_area.setBackground(Color.green);
        LiULogotype LiU = new LiULogotype();
        GridBagConstraints c = new GridBagConstraints();
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.insets = new Insets(15, 0, 0, 15);
        c.anchor = GridBagConstraints.NORTHEAST;
        c.gridwidth = GridBagConstraints.REMAINDER;
        window_area.add(LiU, c);
        GridBagConstraints c1 = new GridBagConstraints();
        c1.weighty = 1.0;
        c1.fill = GridBagConstraints.BOTH;
        c1.anchor = GridBagConstraints.CENTER;
        window_area.add(location_area, c1);
        window.setLayout(new BorderLayout());
        window.add(window_area, BorderLayout.CENTER);
        try {
            SmartCard.start();
            CardTerminalRegistry registry = CardTerminalRegistry.getRegistry();
            registry.setPollInterval(200);
            JibMultiFactory factory = new JibMultiFactory();
            factory.addJiBListener(new JibMultiListener() {

                public void iButtonInserted(JibMultiEvent event) {
                    playSound("inserted.au");
                    int slot = event.getSlotID();
                    SlotChannel channel = event.getChannel();
                    iButtonCardTerminal terminal = (iButtonCardTerminal) channel.getCardTerminal();
                    int[] buttonId = terminal.getiButtonId(channel.getSlotNumber());
                    AnimalProxy re_ap = AnimalRegistry.getReusableAnimalProxy(buttonId);
                    if (re_ap == null) {
                        boolean selected = AnimalProxy.selectApplet(channel);
                        if (selected) {
                            LocationPanel lp = getSelectedLocationPanel();
                            AnimalProxy new_ap;
                            if (lp != null) new_ap = lp.getFactory().createAnimalProxy(channel, buttonId); else new_ap = new AnimalProxy(channel, buttonId);
                            addAnimal(new_ap);
                        } else {
                            System.err.println("iButton applet \"Animal\" not found");
                            playSound("failure.au");
                        }
                    } else {
                        re_ap.setChannel(channel);
                    }
                }

                public void iButtonRemoved(JibMultiEvent event) {
                    playSound("removed.au");
                }
            });
            registry.addCTListener(factory);
        } catch (CardTerminalException e) {
            e.printStackTrace();
        } catch (CardServiceException e) {
            e.printStackTrace();
        } catch (OpenCardPropertyLoadingException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
   *  Shows the farmyard window. This method opens the window.
   */
    public void show() {
        window.show();
        window.setBackground(Color.green);
    }

    /**
   *  Returns the selected location panel.
   *
   *  @return the location panel currently selected
   */
    public synchronized LocationPanel getSelectedLocationPanel() {
        return selected_location_panel;
    }

    /**
   *  Sets the selected location panel.
   *
   *  @param the location panel
   */
    public synchronized void setSelectedLocationPanel(LocationPanel lp) {
        if (getSelectedLocationPanel() != null) getSelectedLocationPanel().setLargeMode(false);
        selected_location_panel = lp;
        getSelectedLocationPanel().setLargeMode(true);
        for (int i = 0; i < locations_menu.getItemCount(); i++) {
            MenuItem menu_item = locations_menu.getItem(i);
            if (menu_item instanceof LocationMenuItem) {
                LocationMenuItem mi = (LocationMenuItem) menu_item;
                mi.setState(mi.getLocationPanel() == getSelectedLocationPanel());
            }
        }
    }

    /**
   *  Help class for the location menu. This class defines custom
   *  menu items for the location menu.
   */
    private class LocationMenuItem extends CheckboxMenuItem {

        private LocationPanel location_panel;

        LocationMenuItem(String label) {
            super(label);
        }

        void setLocationPanel(LocationPanel lp) {
            location_panel = lp;
        }

        LocationPanel getLocationPanel() {
            return location_panel;
        }
    }

    /**
   *  Adds the specified location panel to this farmyard.
   *
   *  @param lp the location panel to add
   */
    public synchronized void addLocationPanel(final LocationPanel lp) {
        location_panels.addElement(lp);
        location_area.add((Component) lp);
        LocationMenuItem mi = new LocationMenuItem(lp.getLocationName());
        mi.setLocationPanel(lp);
        mi.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent event) {
                setSelectedLocationPanel(lp);
            }
        });
        locations_menu.add(mi);
    }

    /**
   *  Removes the specified location panel from this farmyard.
   *
   *  @param lp the location panel to remove
   */
    public synchronized void removeLocationPanel(LocationPanel lp) {
        location_panels.removeElement(lp);
        location_area.remove((Component) lp);
        for (int i = 0; i < locations_menu.getItemCount(); i++) {
            MenuItem menu_item = locations_menu.getItem(i);
            if (menu_item instanceof LocationMenuItem) {
                LocationMenuItem mi = (LocationMenuItem) menu_item;
                if (mi.getLocationPanel() == getSelectedLocationPanel()) locations_menu.remove(mi);
            }
        }
    }

    /**
   *  Adds an animal to the selected location panel.
   *
   *  @param ap the animal proxy for the animal to add
   */
    public void addAnimal(final AnimalProxy ap) {
        final LocationPanel lp = getSelectedLocationPanel();
        if (lp == null) {
            System.err.println("No location selected. The animal cannot be added.");
            return;
        }
        lp.setStatusAnimalEntering();
        AnimalComponent ac = lp.getFactory().createAnimalComponent(ap);
        lp.addAnimalComponent(ac);
        window.validate();
        Thread th = new Thread() {

            public void run() {
                addAnimalConfiguration(ap);
                lp.addAnimalHandshake(ap);
            }
        };
        try {
            th.start();
        } catch (IllegalThreadStateException e) {
        }
    }

    /**
   *  Performs a configuration check on the animal when it is added. Checks
   *  the animal name and requests animal configuration if required by
   *  opening the configuration-sialog window.
   *
   *  @param ap the animal proxy for the new animal
   */
    private void addAnimalConfiguration(final AnimalProxy ap) {
        if (ap.isRemoved()) return;
        String name = ap.getAnimalName();
        if (name != null && name.length() == 0) {
            Thread th = new Thread() {

                public void run() {
                    ConfigurationDialog cp = new ConfigurationDialog(window, ap);
                    if (ap.isRemoved()) return;
                    cp.show();
                    if (ap.isRemoved()) return;
                    cp.setValues(ap);
                }
            };
            try {
                th.start();
            } catch (IllegalThreadStateException e) {
            }
        }
    }

    /**
   *  Plays a sound resource. The sound should be stored in a *.au file
   *  in the same directory as the Farmyard class.
   *
   *  @param name the name of the sound resource (e.g., "hello.au")
   */
    public static void playSound(String name) {
        URL url = Farmyard.class.getResource(name);
        if (url != null) {
            playSound(url);
        } else {
            System.err.println("The sound resource " + name + " is unavailable.");
        }
    }

    /**
   *  Plays a sound available on a URL. The sound should be stored in
   *  a *.au file.
   *
   *  @param url the url for the sound (in *.au format)
   */
    public static void playSound(URL url) {
        java.applet.AudioClip ac = java.applet.Applet.newAudioClip(url);
        ac.play();
    }
}
