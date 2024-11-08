package be.gnx.fogo.application.ui;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Transaction;
import be.gnx.fogo.application.dao.AddressDao;
import be.gnx.fogo.application.dao.FirstnameDao;
import be.gnx.fogo.application.dao.GivennameDao;
import be.gnx.fogo.application.dao.PersonDao;
import be.gnx.fogo.application.dao.SurnameDao;
import be.gnx.fogo.application.model.Address;
import be.gnx.fogo.application.model.Category;
import be.gnx.fogo.application.model.Communication;
import be.gnx.fogo.application.model.ExtraInfo;
import be.gnx.fogo.application.model.Firstname;
import be.gnx.fogo.application.model.Givenname;
import be.gnx.fogo.application.model.ISO_3166_1_alpha2;
import be.gnx.fogo.application.model.Person;
import be.gnx.fogo.application.model.Sexe;
import be.gnx.fogo.application.model.Surname;
import be.gnx.fogo.application.util.HibernateUtil;
import be.gnx.fogo.application.util.StringUtil;

public class MenuBar extends Composite {

    private Menu menuBar;

    private ResourceBundle resourceBundle;

    public MenuBar(Shell shell, int style, ResourceBundle resourceBundle) {
        super(shell, style);
        this.resourceBundle = resourceBundle;
        menuBar = new Menu(shell, SWT.BAR);
        MenuItem fileMenuHeader = new MenuItem(menuBar, SWT.CASCADE);
        fileMenuHeader.setText("&File");
        Menu fileMenu = new Menu(shell, SWT.DROP_DOWN);
        fileMenuHeader.setMenu(fileMenu);
        MenuItem fileImportMenuItem = new MenuItem(fileMenu, SWT.PUSH);
        fileImportMenuItem.setText("&Import ...");
        MenuItem fileExportMenuItem = new MenuItem(fileMenu, SWT.PUSH);
        fileExportMenuItem.setText("&Export ...");
        MenuItem fileSaveItem = new MenuItem(fileMenu, SWT.PUSH);
        fileSaveItem.setText("&Save");
        MenuItem fileExitItem = new MenuItem(fileMenu, SWT.PUSH);
        fileExitItem.setText("E&xit");
        MenuItem helpMenuHeader = new MenuItem(menuBar, SWT.CASCADE);
        helpMenuHeader.setText("&Help");
        Menu helpMenu = new Menu(shell, SWT.DROP_DOWN);
        helpMenuHeader.setMenu(helpMenu);
        MenuItem helpGetHelpItem = new MenuItem(helpMenu, SWT.PUSH);
        helpGetHelpItem.setText("&Get Help");
        fileImportMenuItem.addSelectionListener(new FileImportMenuItemListener());
        fileExportMenuItem.addSelectionListener(new FileExportMenuItemListener());
        fileExitItem.addSelectionListener(new fileExitItemListener());
        fileSaveItem.addSelectionListener(new fileSaveItemListener());
        helpGetHelpItem.addSelectionListener(new helpGetHelpItemListener());
        shell.setMenuBar(menuBar);
        addDisposeListener(new DisposeListener() {

            public void widgetDisposed(DisposeEvent disposeEvent) {
                MenuBar.this.widgetDisposed(disposeEvent);
            }
        });
        addControlListener(new ControlAdapter() {

            public void controlResized(ControlEvent controlEvent) {
                MenuBar.this.controlResized(controlEvent);
            }
        });
    }

    private void widgetDisposed(DisposeEvent disposeEvent) {
    }

    private void controlResized(ControlEvent controlEvent) {
        resize();
    }

    private void resize() {
    }

    class FileImportMenuItemListener implements SelectionListener {

        private FirstnameDao firstnameDao = new FirstnameDao();

        private SurnameDao surnameDao = new SurnameDao();

        private AddressDao addressDao = new AddressDao();

        private PersonDao personDao = new PersonDao();

        private GivennameDao givennameDao = new GivennameDao();

        public void widgetSelected(SelectionEvent event) {
            loadCsvData();
        }

        public void widgetDefaultSelected(SelectionEvent event) {
            loadCsvData();
        }

        private List<String> splitCsvDataLine(String stringData) {
            List<String> stringList = new ArrayList<String>();
            int index = 0;
            String entry = "";
            while (index < stringData.length()) {
                if (stringData.charAt(index) == '"') {
                    entry += stringData.charAt(index);
                    index++;
                    while ((index < stringData.length()) && (stringData.charAt(index) != '"')) {
                        entry += stringData.charAt(index);
                        index++;
                    }
                    if (index < stringData.length()) {
                        entry += stringData.charAt(index);
                        index++;
                        index++;
                    }
                    stringList.add(entry);
                    entry = "";
                } else {
                    while ((index < stringData.length()) && (stringData.charAt(index) != ',')) {
                        entry += stringData.charAt(index);
                        index++;
                    }
                    stringList.add(entry);
                    index++;
                    entry = "";
                }
            }
            return stringList;
        }

        private void loadCsvData() throws RuntimeException {
            List<List<String>> data = new ArrayList<List<String>>();
            HashMap<String, Person> personHashMap = new HashMap<String, Person>();
            try {
                FileInputStream fileInputStream = new FileInputStream("database" + File.separator + "personen.csv");
                ByteArrayOutputStream byteArrayOutputStream;
                int read = fileInputStream.read();
                while (read != -1) {
                    byteArrayOutputStream = new ByteArrayOutputStream();
                    while ((read != -1) && (read != '\n')) {
                        byteArrayOutputStream.write(read);
                        read = fileInputStream.read();
                    }
                    String stringData = new String(byteArrayOutputStream.toByteArray(), "UTF-8");
                    data.add(splitCsvDataLine(stringData));
                    read = fileInputStream.read();
                }
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
            for (List<String> entry : data) {
                if ((entry.size() > 0) && (!entry.get(0).equalsIgnoreCase("\"id\""))) {
                    Transaction transaction = HibernateUtil.beginTransaction();
                    Surname surname = surnameDao.findOrCreate(StringUtil.normalizeName(entry.get(1)));
                    Firstname firstname = firstnameDao.findOrCreate(StringUtil.normalizeName(entry.get(2)));
                    Address address;
                    if ((entry.get(8).length() >= 4) && (entry.get(8).substring(1, 3).equalsIgnoreCase("be"))) {
                        address = addressDao.findOrCreate("BE", entry.get(6), entry.get(7), entry.get(3), entry.get(4), entry.get(5));
                    } else if ((entry.get(8).length() >= 4) && (entry.get(8).substring(1, 3).equalsIgnoreCase("ne"))) {
                        address = addressDao.findOrCreate("NL", entry.get(6), entry.get(7), entry.get(3), entry.get(4), entry.get(5));
                    } else if ((entry.get(8).length() >= 4) && (entry.get(8).substring(1, 3).equalsIgnoreCase("du"))) {
                        address = addressDao.findOrCreate("DE", entry.get(6), entry.get(7), entry.get(3), entry.get(4), entry.get(5));
                    } else if ((entry.get(8).length() >= 4) && (entry.get(8).substring(1, 3).equalsIgnoreCase("lu"))) {
                        address = addressDao.findOrCreate("LU", entry.get(6), entry.get(7), entry.get(3), entry.get(4), entry.get(5));
                    } else if ((entry.get(8).length() >= 4) && (entry.get(8).substring(1, 3).equalsIgnoreCase("fr"))) {
                        address = addressDao.findOrCreate("FR", entry.get(6), entry.get(7), entry.get(3), entry.get(4), entry.get(5));
                    } else {
                        address = addressDao.findOrCreate("ZZ", entry.get(6), entry.get(7), entry.get(3), entry.get(4), entry.get(5));
                    }
                    Person person = new Person();
                    person.setSurname(surname);
                    person.setAlternativeName(firstname);
                    String sexe = entry.get(31);
                    if (sexe == null) {
                        sexe = "U";
                    }
                    sexe = sexe.trim();
                    if (sexe.length() == 0) {
                        sexe = "U";
                    }
                    if (sexe.charAt(0) == '"') {
                        sexe = sexe.substring(1, sexe.length());
                    }
                    if (sexe.charAt(sexe.length() - 1) == '"') {
                        sexe = sexe.substring(0, sexe.length() - 1);
                    }
                    sexe = sexe.trim();
                    if (sexe.length() == 0) {
                        sexe = "U";
                    }
                    if (sexe.equalsIgnoreCase("f")) {
                        person.setSexe(Sexe.FEMALE);
                    } else if (sexe.equalsIgnoreCase("m")) {
                        person.setSexe(Sexe.MALE);
                    } else {
                        person.setSexe(Sexe.UNKNOWN);
                    }
                    String birth = entry.get(15);
                    if ((birth != null) && (birth.length() > 0)) {
                        String[] birthSplit = birth.split("/");
                        if ((birthSplit != null) && (birthSplit.length == 3)) {
                            int year = Integer.parseInt(birthSplit[2]);
                            int month = Integer.parseInt(birthSplit[1]);
                            int day = Integer.parseInt(birthSplit[0]);
                            if ((year > 1700) && (year < 2100)) {
                                person.setDateOfBirthYear(year);
                            }
                            person.setDateOfBirthMonth(month);
                            person.setDateOfBirthDay(day);
                        }
                    }
                    String death = entry.get(16);
                    if ((death != null) && (death.length() > 0)) {
                        String[] deathSplit = death.split("/");
                        if ((deathSplit != null) && (deathSplit.length == 3)) {
                            int year = Integer.parseInt(deathSplit[2]);
                            int month = Integer.parseInt(deathSplit[1]);
                            int day = Integer.parseInt(deathSplit[0]);
                            if ((year > 1700) && (year < 2100)) {
                                person.setDateOfDeathYear(year);
                            }
                            person.setDateOfDeathMonth(month);
                            person.setDateOfDeathDay(day);
                        }
                    }
                    if ((entry.get(9) != null) && (entry.get(9).length() > 0)) {
                        Communication communication = new Communication();
                        communication.setNumber(Long.parseLong(entry.get(9)));
                        communication.setCategory(Category.get("COMMUNICATION", "Primary Personal Phone Number"));
                        person.getCommunicationSet().add(communication);
                    }
                    if ((entry.get(10) != null) && (entry.get(10).length() > 0)) {
                        Communication communication = new Communication();
                        communication.setNumber(Long.parseLong(entry.get(10)));
                        communication.setCategory(Category.get("COMMUNICATION", "Primary Business Phone Number"));
                        person.getCommunicationSet().add(communication);
                    }
                    if ((entry.get(11) != null) && (entry.get(11).length() > 0)) {
                        Communication communication = new Communication();
                        communication.setNumber(Long.parseLong(entry.get(11)));
                        communication.setCategory(Category.get("COMMUNICATION", "Primary Personal Mobile Number"));
                        person.getCommunicationSet().add(communication);
                    }
                    if ((entry.get(12) != null) && (entry.get(12).length() > 0)) {
                        Communication communication = new Communication();
                        communication.setNumber(Long.parseLong(entry.get(12)));
                        communication.setCategory(Category.get("COMMUNICATION", "Primary Personal Fax Number"));
                        person.getCommunicationSet().add(communication);
                    }
                    if ((entry.get(13) != null) && (entry.get(13).length() > 2)) {
                        String[] emails = entry.get(13).substring(1, entry.get(13).length() - 1).split(" ");
                        boolean first = true;
                        for (String email : emails) {
                            Communication communication = new Communication();
                            communication.setString(email);
                            if (first) {
                                communication.setCategory(Category.get("COMMUNICATION", "Primary Personal E-Mail"));
                                first = false;
                            } else {
                                communication.setCategory(Category.get("COMMUNICATION", "Auxiliary Personal E-Mail"));
                            }
                            person.getCommunicationSet().add(communication);
                        }
                    }
                    if ((entry.get(14) != null) && (entry.get(14).length() > 2)) {
                        String[] emails = entry.get(14).substring(1, entry.get(14).length() - 1).split(" ");
                        boolean first = true;
                        for (String email : emails) {
                            Communication communication = new Communication();
                            communication.setString(email);
                            if (first) {
                                communication.setCategory(Category.get("COMMUNICATION", "Primary Business E-Mail"));
                                first = false;
                            } else {
                                communication.setCategory(Category.get("COMMUNICATION", "Auxiliary Business E-Mail"));
                            }
                            person.getCommunicationSet().add(communication);
                        }
                    }
                    Set<Communication> communicationSet = person.getCommunicationSet();
                    Communication communicationPrimaryPersonalAddress = new Communication();
                    communicationPrimaryPersonalAddress.setCategory(Category.get("COMMUNICATION", "Primary Personal Address"));
                    communicationPrimaryPersonalAddress.setAddress(address);
                    communicationSet.add(communicationPrimaryPersonalAddress);
                    if ((entry.get(23) != null) && (entry.get(23).length() == 1) && (entry.get(23).charAt(0) == '1')) {
                        person.getCategorySet().add(Category.get("PERSON", "Genodigde mis huwelijk Leen en Wim"));
                    }
                    if ((entry.get(24) != null) && (entry.get(24).length() == 1) && (entry.get(24).charAt(0) == '1')) {
                        person.getCategorySet().add(Category.get("PERSON", "Genodigde receptie huwelijk Leen en Wim"));
                    }
                    if ((entry.get(25) != null) && (entry.get(25).length() == 1) && (entry.get(25).charAt(0) == '1')) {
                        person.getCategorySet().add(Category.get("PERSON", "Genodigde diner huwelijk Leen en Wim"));
                    }
                    if ((entry.get(26) != null) && (entry.get(26).length() == 1) && (entry.get(26).charAt(0) == '1')) {
                        person.getCategorySet().add(Category.get("PERSON", "Uitnodiging verstuurd huwelijk Leen en Wim"));
                    }
                    if ((entry.get(27) != null) && (entry.get(27).length() == 1) && (entry.get(27).charAt(0) == '1')) {
                        person.getCategorySet().add(Category.get("PERSON", "Komt naar huwelijk Leen en Wim"));
                    }
                    if ((entry.get(27) != null) && (entry.get(27).length() == 1) && (entry.get(27).charAt(0) == '0')) {
                        person.getCategorySet().add(Category.get("PERSON", "Komt niet naar huwelijk Leen en Wim"));
                    }
                    if ((entry.get(28) != null) && (entry.get(28).length() == 1) && (entry.get(28).charAt(0) == '1')) {
                        person.getCategorySet().add(Category.get("PERSON", "Genodigde surprise party 30 jaar Els Van Hoeck"));
                    }
                    if ((entry.get(29) != null) && (entry.get(29).length() == 1) && (entry.get(29).charAt(0) == '1')) {
                        person.getCategorySet().add(Category.get("PERSON", "Krijgt een kerstkaart van ons"));
                    }
                    if ((entry.get(30) != null) && (entry.get(30).length() > 0)) {
                        String[] givennames = entry.get(30).split(" ");
                        int listIndex = 0;
                        for (String givenNameString : givennames) {
                            Firstname givenFirstname = firstnameDao.findOrCreate(StringUtil.normalizeName(givenNameString));
                            Givenname givenname = new Givenname();
                            givenname.setPerson(person);
                            givenname.setFirstname(givenFirstname);
                            givenname.setListIndex(listIndex);
                            person.getGivennameList().add(givenname);
                            listIndex++;
                        }
                    }
                    if ((entry.get(32) != null) && (entry.get(32).length() > 4)) {
                        String[] entrySplit = entry.get(32).split(": ");
                        if (entrySplit.length > 1) {
                            Category category = Category.get("PERSON", entrySplit[0].substring(1));
                            if (category != null) {
                                person.getCategorySet().add(category);
                            } else {
                                category = Category.get("PERSONEXTRAINFO", entrySplit[0].substring(1));
                                if (category != null) {
                                    ExtraInfo extraInfo = new ExtraInfo();
                                    extraInfo.setCategory(category);
                                    extraInfo.setValue(entrySplit[1].substring(0, entrySplit[1].length() - 1));
                                    person.getExtraInfoSet().add(extraInfo);
                                }
                            }
                        }
                    }
                    if ((entry.get(33) != null) && (entry.get(33).length() == 1) && (entry.get(33).charAt(0) == '1')) {
                        person.getCategorySet().add(Category.get("PERSON", "Krijgt suikerbonen van ons"));
                    }
                    if ((entry.get(34) != null) && (entry.get(34).length() == 1) && (entry.get(34).charAt(0) == '1')) {
                        person.getCategorySet().add(Category.get("PERSON", "Krijgt geboortekaart jef van ons"));
                    }
                    if ((entry.get(35) != null) && (entry.get(35).length() > 4)) {
                        String[] entrySplit = entry.get(35).split(": ");
                        if (entrySplit.length > 1) {
                            Category category = Category.get("PERSONEXTRAINFO", "Cadeau Jef");
                            if (category != null) {
                                ExtraInfo extraInfo = new ExtraInfo();
                                extraInfo.setCategory(category);
                                extraInfo.setValue(entrySplit[1].substring(0, entrySplit[1].length() - 1));
                                person.getExtraInfoSet().add(extraInfo);
                            }
                        }
                    }
                    if ((entry.get(36) != null) && (entry.get(36).length() == 1) && (entry.get(36).charAt(0) == '1')) {
                        person.getCategorySet().add(Category.get("PERSON", "Krijgt geboortekaart emma van ons"));
                    }
                    personDao.save(person);
                    transaction.commit();
                    personHashMap.put(entry.get(0), person);
                }
            }
            for (List<String> entry : data) {
                if ((entry.size() > 0) && (!entry.get(0).equalsIgnoreCase("\"id\""))) {
                    Person person = personHashMap.get(entry.get(0));
                    if ((entry.get(17) != null) && (entry.get(17).length() > 0)) {
                        person.setMother(personHashMap.get(entry.get(17)));
                    }
                    if ((entry.get(19) != null) && (entry.get(19).length() > 0)) {
                        person.setFather(personHashMap.get(entry.get(19)));
                    }
                    if ((entry.get(21) != null) && (entry.get(21).length() > 0)) {
                        person.setSpouse(personHashMap.get(entry.get(21)));
                    }
                    Transaction transaction = HibernateUtil.beginTransaction();
                    personDao.update(person);
                    transaction.commit();
                }
            }
        }
    }

    class FileExportMenuItemListener implements SelectionListener {

        PersonDao personDao = new PersonDao();

        public void widgetSelected(SelectionEvent event) {
            saveCsvData();
        }

        public void widgetDefaultSelected(SelectionEvent event) {
            saveCsvData();
        }

        private void saveCsvData() throws RuntimeException {
            Transaction transaction = HibernateUtil.beginTransaction();
            try {
                List<Person> persons = personDao.getAll();
                FileOutputStream fileOutputStream = new FileOutputStream("database" + File.separator + "export.csv");
                String header = "\"ID\",\"Naam\",\"Voornaam\",\"Straat\",\"Nummer\",\"Bus\",\"Postnummer\",\"Gemeente\",\"Land\"," + "\"Telefoon_thuis\",\"Telefoon_werk\",\"Telefoon_mobiel\",\"Fax\",\"Email_home\",\"Email_work\"," + "\"Geboortedatum\",\"Sterfdatum\",\"Mother_ID\",\"Mother\",\"Father_ID\",\"Father\",\"Spouse_ID\",\"Spouse\"," + "\"Uitnodiging_trouwmis\",\"Uitnodiging_trouwreceptie\",\"Uitnodiging_trouwdiner\",\"Uitnodiging_verstuurd\",\"Uitnodiging_bevestigd\"" + ",\"Els\",\"Kerstkaart\",\"Namen\",\"Geslacht\",\"Nota\",\"Suikerbonen\",\"Geboortekaart Jef\",\"Cadeau Jef\",\"Geboortekaart 2\"," + "\"Laatste kolom\"\n";
                fileOutputStream.write(header.getBytes("UTF-8"));
                for (Person person : persons) {
                    String birthDate = "";
                    if ((person.getDateOfBirthDay() != null) || (person.getDateOfBirthMonth() != null) || (person.getDateOfBirthYear() == null)) {
                        if (person.getDateOfBirthDay() != null) {
                            birthDate += ((person.getDateOfBirthDay() + 100) + "").substring(1);
                            birthDate += "/";
                        }
                        if (person.getDateOfBirthMonth() != null) {
                            birthDate += ((person.getDateOfBirthMonth() + 100) + "").substring(1);
                            birthDate += "/";
                        }
                        if (person.getDateOfBirthYear() != null) {
                            birthDate += ((person.getDateOfBirthYear() + 10000) + "").substring(1);
                        } else if ((person.getDateOfBirthDay() != null) && (person.getDateOfBirthMonth() != null)) {
                            birthDate += "3000";
                        }
                    }
                    String deathDate = "";
                    if ((person.getDateOfDeathDay() != null) || (person.getDateOfDeathMonth() != null) || (person.getDateOfDeathYear() == null)) {
                        if (person.getDateOfDeathDay() != null) {
                            deathDate += ((person.getDateOfDeathDay() + 100) + "").substring(1);
                            deathDate += "/";
                        }
                        if (person.getDateOfDeathMonth() != null) {
                            deathDate += ((person.getDateOfDeathMonth() + 100) + "").substring(1);
                            deathDate += "/";
                        }
                        if (person.getDateOfDeathYear() != null) {
                            deathDate += ((person.getDateOfDeathYear() + 10000) + "").substring(1);
                        } else if ((person.getDateOfDeathDay() != null) && (person.getDateOfDeathMonth() != null)) {
                            deathDate += "3000";
                        }
                    }
                    String sexe = person.getSexe().getCode() + "";
                    Address address = null;
                    String phoneHome = "";
                    String phoneWork = "";
                    String phoneMobile = "";
                    String phoneFax = "";
                    String emailHome = null;
                    String emailWork = null;
                    if ((person.getCommunicationSet() != null) && (person.getCommunicationSet().size() > 0)) {
                        for (Communication communication : person.getCommunicationSet()) {
                            if (communication.getCategory().getName().equals("Primary Personal Address")) {
                                address = communication.getAddress();
                            }
                            if (communication.getCategory().getName().equals("Primary Personal E-Mail")) {
                                if (emailHome == null) {
                                    emailHome = "";
                                }
                                emailHome = communication.getString() + emailHome;
                            }
                            if (communication.getCategory().getName().equals("Auxiliary Personal E-Mail")) {
                                if (emailHome == null) {
                                    emailHome = "";
                                }
                                emailHome += " " + communication.getString();
                            }
                            if (communication.getCategory().getName().equals("Primary Business E-Mail")) {
                                if (emailWork == null) {
                                    emailWork = "";
                                }
                                emailWork = communication.getString() + emailWork;
                            }
                            if (communication.getCategory().getName().equals("Auxiliary Business E-Mail")) {
                                if (emailWork == null) {
                                    emailWork = "";
                                }
                                emailWork += " " + communication.getString();
                            }
                            if (communication.getCategory().getName().equals("Primary Personal Phone Number")) {
                                phoneHome = communication.getNumber() + phoneHome;
                            }
                            if (communication.getCategory().getName().equals("Auxiliary Personal Phone Number")) {
                                phoneHome += " " + communication.getNumber();
                            }
                            if (communication.getCategory().getName().equals("Primary Business Phone Number")) {
                                phoneWork = communication.getNumber() + phoneWork;
                            }
                            if (communication.getCategory().getName().equals("Auxiliary Business Phone Number")) {
                                phoneWork += " " + communication.getNumber();
                            }
                            if (communication.getCategory().getName().equals("Primary Personal Mobile Number")) {
                                phoneMobile = communication.getNumber() + phoneMobile;
                            }
                            if (communication.getCategory().getName().equals("Auxiliary Personal Mobile Number")) {
                                phoneMobile += " " + communication.getNumber();
                            }
                            if (communication.getCategory().getName().equals("Primary Personal Fax Number")) {
                                phoneFax = communication.getNumber() + phoneFax;
                            }
                            if (communication.getCategory().getName().equals("Auxiliary Personal Fax Number")) {
                                phoneFax += " " + communication.getNumber();
                            }
                        }
                    }
                    boolean Uitnodiging_trouwmis = false;
                    boolean Uitnodiging_trouwreceptie = false;
                    boolean Uitnodiging_trouwdiner = false;
                    boolean Uitnodiging_verstuurd = false;
                    boolean Uitnodiging_bevestigd = false;
                    boolean Uitnodiging_niet_bevestigd = false;
                    boolean Els = false;
                    boolean Kerstkaart = false;
                    boolean Suikerbonen = false;
                    boolean Geboortekaart_Jef = false;
                    boolean Cadeau_Jef = false;
                    boolean Geboortekaart_emma = false;
                    boolean Einde = false;
                    String nota = null;
                    if ((person.getCategorySet() != null) && (person.getCategorySet().size() > 0)) {
                        for (Category category : person.getCategorySet()) {
                            if (category.getName().equals("Genodigde mis huwelijk Leen en Wim")) {
                                Uitnodiging_trouwmis = true;
                            } else if (category.getName().equals("Genodigde receptie huwelijk Leen en Wim")) {
                                Uitnodiging_trouwreceptie = true;
                            } else if (category.getName().equals("Genodigde diner huwelijk Leen en Wim")) {
                                Uitnodiging_trouwdiner = true;
                            } else if (category.getName().equals("Uitnodiging verstuurd huwelijk Leen en Wim")) {
                                Uitnodiging_verstuurd = true;
                            } else if (category.getName().equals("Komt naar huwelijk Leen en Wim")) {
                                Uitnodiging_bevestigd = true;
                            } else if (category.getName().equals("Komt niet naar huwelijk Leen en Wim")) {
                                Uitnodiging_niet_bevestigd = true;
                            } else if (category.getName().equals("Genodigde surprise party 30 jaar Els Van Hoeck")) {
                                Els = true;
                            } else if (category.getName().equals("Krijgt een kerstkaart van ons")) {
                                Kerstkaart = true;
                            } else if (category.getName().equals("Krijgt suikerbonen van ons")) {
                                Suikerbonen = true;
                            } else if (category.getName().equals("Krijgt geboortekaart jef van ons")) {
                                Geboortekaart_Jef = true;
                            } else if (category.getName().equals("Krijgt geboortekaart emma van ons")) {
                                Geboortekaart_emma = true;
                            } else {
                                if (nota == null) {
                                    nota = "";
                                }
                                nota += category.getName() + ": " + category.getName();
                            }
                        }
                    }
                    String cadeauJef = null;
                    if ((person.getExtraInfoSet() != null) && (person.getExtraInfoSet().size() > 0)) {
                        for (ExtraInfo extraInfo : person.getExtraInfoSet()) {
                            if (extraInfo.getCategory().getName().equals("Cadeau Jef")) {
                                cadeauJef = "Cadeau Jef: " + extraInfo.getValue();
                            } else {
                                if (nota == null) {
                                    nota = "";
                                }
                                nota += extraInfo.getCategory().getName() + ": " + extraInfo.getValue();
                            }
                        }
                    }
                    fileOutputStream.write((person.getId() + "").getBytes("UTF-8"));
                    fileOutputStream.write(",\"".getBytes("UTF-8"));
                    fileOutputStream.write(person.getSurname().getValue().getBytes("UTF-8"));
                    fileOutputStream.write("\",\"".getBytes("UTF-8"));
                    fileOutputStream.write(person.getAlternativeName().getValue().getBytes("UTF-8"));
                    fileOutputStream.write("\",".getBytes("UTF-8"));
                    if (address != null) {
                        if (address.getStreetname() != null) {
                            fileOutputStream.write("\"".getBytes("UTF-8"));
                            fileOutputStream.write(address.getStreetname().getBytes("UTF-8"));
                            fileOutputStream.write("\"".getBytes("UTF-8"));
                        }
                        fileOutputStream.write(",".getBytes("UTF-8"));
                        if (address.getNumber() != null) {
                            boolean hasAlpha = false;
                            for (int index = 0; index < address.getNumber().length(); index++) {
                                if (!Character.isDigit(address.getNumber().charAt(index))) {
                                    hasAlpha = true;
                                }
                            }
                            if (hasAlpha) {
                                fileOutputStream.write("\"".getBytes("UTF-8"));
                            }
                            fileOutputStream.write(address.getNumber().getBytes("UTF-8"));
                            if (hasAlpha) {
                                fileOutputStream.write("\"".getBytes("UTF-8"));
                            }
                        }
                        fileOutputStream.write(",".getBytes("UTF-8"));
                        if (address.getBus() != null) {
                            boolean hasAlpha = false;
                            for (int index = 0; index < address.getBus().length(); index++) {
                                if (!Character.isDigit(address.getBus().charAt(index))) {
                                    hasAlpha = true;
                                }
                            }
                            if (hasAlpha) {
                                fileOutputStream.write("\"".getBytes("UTF-8"));
                            }
                            fileOutputStream.write(address.getBus().getBytes("UTF-8"));
                            if (hasAlpha) {
                                fileOutputStream.write("\"".getBytes("UTF-8"));
                            }
                        }
                        fileOutputStream.write(",".getBytes("UTF-8"));
                        if (address.getPostcode() != null) {
                            boolean hasAlpha = false;
                            for (int index = 0; index < address.getPostcode().length(); index++) {
                                if (!Character.isDigit(address.getPostcode().charAt(index))) {
                                    hasAlpha = true;
                                }
                            }
                            if (hasAlpha) {
                                fileOutputStream.write("\"".getBytes("UTF-8"));
                            }
                            fileOutputStream.write(address.getPostcode().getBytes("UTF-8"));
                            if (hasAlpha) {
                                fileOutputStream.write("\"".getBytes("UTF-8"));
                            }
                        }
                        fileOutputStream.write(",".getBytes("UTF-8"));
                        if (address.getCity() != null) {
                            fileOutputStream.write("\"".getBytes("UTF-8"));
                            fileOutputStream.write(address.getCity().getBytes("UTF-8"));
                            fileOutputStream.write("\"".getBytes("UTF-8"));
                        }
                        fileOutputStream.write(",".getBytes("UTF-8"));
                        if (address.getCountry() != null) {
                            fileOutputStream.write("\"".getBytes("UTF-8"));
                            if (address.getCountry().getIso_3166_1_alpha2_as_string().equals("BE")) {
                                fileOutputStream.write("België".getBytes("UTF-8"));
                            } else if (address.getCountry().getIso_3166_1_alpha2_as_string().equals("NL")) {
                                fileOutputStream.write("Nederland".getBytes("UTF-8"));
                            } else if (address.getCountry().getIso_3166_1_alpha2_as_string().equals("FR")) {
                                fileOutputStream.write("Frankrijk".getBytes("UTF-8"));
                            } else if (address.getCountry().getIso_3166_1_alpha2_as_string().equals("LU")) {
                                fileOutputStream.write("Luxemburg".getBytes("UTF-8"));
                            } else {
                                fileOutputStream.write("Unknown".getBytes("UTF-8"));
                            }
                            ;
                            fileOutputStream.write("\"".getBytes("UTF-8"));
                        }
                    } else {
                        fileOutputStream.write(",,,,,".getBytes("UTF-8"));
                    }
                    fileOutputStream.write(",".getBytes("UTF-8"));
                    if (phoneHome != null) {
                        fileOutputStream.write(phoneHome.getBytes("UTF-8"));
                    }
                    fileOutputStream.write(",".getBytes("UTF-8"));
                    if (phoneWork != null) {
                        fileOutputStream.write(phoneWork.getBytes("UTF-8"));
                    }
                    fileOutputStream.write(",".getBytes("UTF-8"));
                    if (phoneMobile != null) {
                        fileOutputStream.write(phoneMobile.getBytes("UTF-8"));
                    }
                    fileOutputStream.write(",".getBytes("UTF-8"));
                    if (phoneFax != null) {
                        fileOutputStream.write(phoneFax.getBytes("UTF-8"));
                    }
                    fileOutputStream.write(",".getBytes("UTF-8"));
                    if (emailHome != null) {
                        fileOutputStream.write("\"".getBytes("UTF-8"));
                        fileOutputStream.write(emailHome.getBytes("UTF-8"));
                        fileOutputStream.write("\"".getBytes("UTF-8"));
                    }
                    fileOutputStream.write(",".getBytes("UTF-8"));
                    if (emailWork != null) {
                        fileOutputStream.write("\"".getBytes("UTF-8"));
                        fileOutputStream.write(emailWork.getBytes("UTF-8"));
                        fileOutputStream.write("\"".getBytes("UTF-8"));
                    }
                    fileOutputStream.write(",".getBytes("UTF-8"));
                    fileOutputStream.write(birthDate.getBytes("UTF-8"));
                    fileOutputStream.write(",".getBytes("UTF-8"));
                    fileOutputStream.write(deathDate.getBytes("UTF-8"));
                    fileOutputStream.write(",".getBytes("UTF-8"));
                    if (person.getMother() != null) {
                        fileOutputStream.write((person.getMother().getId() + "").getBytes("UTF-8"));
                        fileOutputStream.write(",\"".getBytes("UTF-8"));
                        fileOutputStream.write((person.getMother().getSurname().getValue() + " " + person.getMother().getAlternativeName().getValue()).getBytes("UTF-8"));
                        fileOutputStream.write("\",".getBytes("UTF-8"));
                    } else {
                        fileOutputStream.write(",\"\",".getBytes("UTF-8"));
                    }
                    if (person.getFather() != null) {
                        fileOutputStream.write((person.getFather().getId() + "").getBytes("UTF-8"));
                        fileOutputStream.write(",\"".getBytes("UTF-8"));
                        fileOutputStream.write((person.getFather().getSurname().getValue() + " " + person.getFather().getAlternativeName().getValue()).getBytes("UTF-8"));
                        fileOutputStream.write("\",".getBytes("UTF-8"));
                    } else {
                        fileOutputStream.write(",\"\",".getBytes("UTF-8"));
                    }
                    if (person.getSpouse() != null) {
                        fileOutputStream.write((person.getSpouse().getId() + "").getBytes("UTF-8"));
                        fileOutputStream.write(",\"".getBytes("UTF-8"));
                        fileOutputStream.write((person.getSpouse().getSurname().getValue() + " " + person.getSpouse().getAlternativeName().getValue()).getBytes("UTF-8"));
                        fileOutputStream.write("\",".getBytes("UTF-8"));
                    } else {
                        fileOutputStream.write(",\"\",".getBytes("UTF-8"));
                    }
                    if (Uitnodiging_trouwmis) {
                        fileOutputStream.write("1".getBytes("UTF-8"));
                    }
                    fileOutputStream.write(",".getBytes("UTF-8"));
                    if (Uitnodiging_trouwreceptie) {
                        fileOutputStream.write("1".getBytes("UTF-8"));
                    }
                    fileOutputStream.write(",".getBytes("UTF-8"));
                    if (Uitnodiging_trouwdiner) {
                        fileOutputStream.write("1".getBytes("UTF-8"));
                    }
                    fileOutputStream.write(",".getBytes("UTF-8"));
                    if (Uitnodiging_verstuurd) {
                        fileOutputStream.write("1".getBytes("UTF-8"));
                    }
                    fileOutputStream.write(",".getBytes("UTF-8"));
                    if (Uitnodiging_bevestigd) {
                        fileOutputStream.write("1".getBytes("UTF-8"));
                    }
                    if (Uitnodiging_niet_bevestigd) {
                        fileOutputStream.write("0".getBytes("UTF-8"));
                    }
                    fileOutputStream.write(",".getBytes("UTF-8"));
                    if (Els) {
                        fileOutputStream.write("1".getBytes("UTF-8"));
                    }
                    fileOutputStream.write(",".getBytes("UTF-8"));
                    if (Kerstkaart) {
                        fileOutputStream.write("1".getBytes("UTF-8"));
                    }
                    fileOutputStream.write(",".getBytes("UTF-8"));
                    if ((person.getGivennameList() != null) && (person.getGivennameList().size() > 0)) {
                        fileOutputStream.write("\"".getBytes("UTF-8"));
                        boolean first = true;
                        for (Givenname name : person.getGivennameList()) {
                            if (!first) {
                                fileOutputStream.write(" ".getBytes("UTF-8"));
                            }
                            fileOutputStream.write(name.getFirstname().getValue().getBytes("UTF-8"));
                            first = false;
                        }
                        fileOutputStream.write("\"".getBytes("UTF-8"));
                    }
                    fileOutputStream.write(",".getBytes("UTF-8"));
                    fileOutputStream.write("\"".getBytes("UTF-8"));
                    fileOutputStream.write(sexe.getBytes("UTF-8"));
                    fileOutputStream.write("\",".getBytes("UTF-8"));
                    if (nota != null) {
                        fileOutputStream.write("\"".getBytes("UTF-8"));
                        fileOutputStream.write(nota.getBytes("UTF-8"));
                        fileOutputStream.write("\"".getBytes("UTF-8"));
                    }
                    fileOutputStream.write(",".getBytes("UTF-8"));
                    if (Suikerbonen) {
                        fileOutputStream.write("1".getBytes("UTF-8"));
                    }
                    fileOutputStream.write(",".getBytes("UTF-8"));
                    if (Geboortekaart_Jef) {
                        fileOutputStream.write("1".getBytes("UTF-8"));
                    }
                    fileOutputStream.write(",".getBytes("UTF-8"));
                    if (cadeauJef != null) {
                        fileOutputStream.write("\"".getBytes("UTF-8"));
                        fileOutputStream.write(cadeauJef.getBytes("UTF-8"));
                        fileOutputStream.write("\"".getBytes("UTF-8"));
                    }
                    fileOutputStream.write(",".getBytes("UTF-8"));
                    if (Geboortekaart_emma) {
                        fileOutputStream.write("1".getBytes("UTF-8"));
                    }
                    fileOutputStream.write(",".getBytes("UTF-8"));
                    fileOutputStream.write("\"Laatste kolom\"".getBytes("UTF-8"));
                    fileOutputStream.write("\n".getBytes("UTF-8"));
                }
                fileOutputStream.flush();
                fileOutputStream.close();
            } catch (IOException exception) {
                throw new RuntimeException(exception);
            } finally {
                transaction.commit();
            }
        }
    }

    class fileExitItemListener implements SelectionListener {

        public void widgetSelected(SelectionEvent event) {
            getShell().close();
            getDisplay().dispose();
        }

        public void widgetDefaultSelected(SelectionEvent event) {
            getShell().close();
            getDisplay().dispose();
        }
    }

    class fileSaveItemListener implements SelectionListener {

        public void widgetSelected(SelectionEvent event) {
        }

        public void widgetDefaultSelected(SelectionEvent event) {
        }
    }

    class helpGetHelpItemListener implements SelectionListener {

        public void widgetSelected(SelectionEvent event) {
        }

        public void widgetDefaultSelected(SelectionEvent event) {
        }
    }
}
