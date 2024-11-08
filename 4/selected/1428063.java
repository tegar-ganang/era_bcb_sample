package com.dsb.barkas.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Hashtable;

public class ServerConnection2 {

    private Socket socket;

    private BufferedReader in;

    private PrintWriter out;

    private ObjectInputStream objIn;

    private ObjectOutputStream objOut;

    private boolean connected;

    private boolean loggedin;

    public final String KASTURF_NAAM = "SET Kas";

    public final String EXTERNTURF_NAAM = "SET Extern Kas";

    /**
	 * Opent een Socket naar de server
	 */
    public ServerConnection2(String host, int port) throws UnknownHostException, IOException {
        socket = new Socket(host, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
        objIn = new ObjectInputStream(socket.getInputStream());
        objOut = new ObjectOutputStream(socket.getOutputStream());
        connected = true;
    }

    /**
	 * Log in op de server
	 */
    public void login(String client_name, String client_code) throws ServerErrorException {
        if (!connected) throw new ServerErrorException("Not connected");
        Hashtable args = new Hashtable();
        args.put("client_name", client_name);
        args.put("client_code", client_code);
        args.put("REQUEST", "login");
        try {
            objOut.writeObject(args);
        } catch (IOException e) {
            throw new ServerErrorException("Server draait niet");
        }
        Hashtable result = readObj();
        if (failure(result)) throw new ServerErrorException("May not log in");
        loggedin = true;
    }

    /**
	 * Registreer een client
	 */
    public void register(String client_location, String client_beheerder, String client_ddl) throws ServerErrorException {
        if (!connected) throw new ServerErrorException("Not connected");
        if (!loggedin) throw new ServerErrorException("Not logged in");
        Hashtable request = new Hashtable();
        request.put("REQUEST", "register_client");
        request.put("client_location", client_location);
        request.put("client_beheerder", client_beheerder);
        request.put("client_ddl", client_ddl);
        if (!writeObj(request)) throw new ServerErrorException("Server draait niet");
        Hashtable result = readObj();
        if (failure(result)) throw new ServerErrorException("Register failed!");
    }

    /**
	 * Haal de prijslijst op
	 */
    public Prijslijst getPrijslijst() throws ServerErrorException {
        String version;
        int itemcount;
        Prijslijst p;
        if (!connected) throw new ServerErrorException("Not connected");
        if (!loggedin) throw new ServerErrorException("Not logged in");
        Hashtable request = new Hashtable();
        request.put("REQUEST", "request_prijslijst");
        if (!writeObj(request)) throw new ServerErrorException("Kan prijslijst niet opvragen");
        Hashtable result = readObj();
        if (failure(result)) throw new ServerErrorException("Server fout");
        Hashtable prijslijst = (Hashtable) result.get("prijslijst");
        String prijsLijstVersion = (String) prijslijst.get("version");
        String prijsLijstItemCount = (String) prijslijst.get("itemCount");
        String[] ID = (String[]) prijslijst.get("itemID");
        String[] Naam = (String[]) prijslijst.get("itemNaam");
        String[] Categorie = (String[]) prijslijst.get("itemCategorie");
        double[] S = (double[]) prijslijst.get("itemPrijsS");
        double[] SI = (double[]) prijslijst.get("itemPrijsSI");
        double[] S225 = (double[]) prijslijst.get("itemPrijsS225");
        double[] S50 = (double[]) prijslijst.get("itemPrijsS50");
        double[] S80 = (double[]) prijslijst.get("itemPrijsS80");
        double[] EUR = (double[]) prijslijst.get("itemPrijsEUR");
        p = new Prijslijst(Integer.parseInt(prijsLijstItemCount));
        p.setVersion(prijsLijstVersion);
        for (int i = 0; i < Integer.parseInt(prijsLijstItemCount); i++) {
            p.set(i, ID[i], Naam[i], Categorie[i], S[i], SI[i], S225[i], S50[i], S80[i], EUR[i]);
        }
        return p;
    }

    /**
	 * Haal een Prijslijst met S dingen op
	 * 
	 * @return Prijslijst met alleen items die een S prijs hebben
	 * @throws ServerErrorException
	 */
    public Prijslijst getPrijslijstS() throws ServerErrorException {
        Prijslijst p = getPrijslijst();
        return p.searchS();
    }

    /**
	 * Haal een Prijslijst met S50 dingen op
	 * 
	 * @return Prijslijst met alleen items die een S50 prijs hebben
	 * @throws ServerErrorException
	 */
    public Prijslijst getPrijslijstS50() throws ServerErrorException {
        Prijslijst p = getPrijslijst();
        return p.searchS50();
    }

    /**
	 * Haal de debiteurlijst op
	 */
    public Debiteur[] getDebiteurlijst() throws ServerErrorException {
        ArrayList dl = getDebiteurlijst2();
        for (int i = 0; i < dl.size(); i++) {
            if (((Debiteur) dl.get(i)).getNaam().equals(KASTURF_NAAM)) {
                dl.remove(i);
                i--;
            } else if (((Debiteur) dl.get(i)).getNaam().equals(EXTERNTURF_NAAM)) {
                dl.remove(i);
                i--;
            }
        }
        return (Debiteur[]) dl.toArray(new Debiteur[0]);
    }

    private ArrayList getDebiteurlijst2() throws ServerErrorException {
        Debiteur[] d;
        int itemcount;
        if (!connected) throw new ServerErrorException("Not connected");
        if (!loggedin) throw new ServerErrorException("Not logged in");
        Hashtable request = new Hashtable();
        request.put("REQUEST", "request_debiteurlijst");
        if (!writeObj(request)) throw new ServerErrorException("Server error");
        Hashtable result = readObj();
        if (failure(result)) throw new ServerErrorException("Could not get debiteur lijst");
        Hashtable debiteurlijst = (Hashtable) result.get("debiteurlijst");
        String Version = (String) debiteurlijst.get("version");
        String DebCount = (String) debiteurlijst.get("debCount");
        String[] DebID = (String[]) debiteurlijst.get("debID");
        String[] DebNaam = (String[]) debiteurlijst.get("debNaam");
        String[] DebSoort = (String[]) debiteurlijst.get("debSoort");
        ArrayList debs = new ArrayList(Integer.parseInt(DebCount));
        for (int i = 0; i < Integer.parseInt(DebCount); i++) {
            debs.add(new Debiteur(DebID[i], DebNaam[i], DebSoort[i]));
        }
        return debs;
    }

    /**
	 * Voeg een notitie toe
	 */
    public void addNotitie(String s) throws ServerErrorException {
        Hashtable request = new Hashtable();
        request.put("REQUEST", "add_notitie");
        request.put("notitie", s);
        if (!writeObj(request) || failure(readObj())) throw new ServerErrorException("Could not add notitie");
    }

    /**
	 * Mag iemand een bon openen?
	 */
    public boolean magBonOpenen(String debiteur_id) throws ServerErrorException {
        Hashtable request = new Hashtable();
        request.put("REQUEST", "may_open_bon");
        request.put("debiteur_id", debiteur_id);
        if (!writeObj(request)) throw new ServerErrorException("Server error");
        Hashtable result = readObj();
        if (failure(result)) throw new ServerErrorException("Server error");
        Boolean toestemming = (Boolean) result.get("toestemming");
        if (toestemming == null) return false;
        return toestemming.booleanValue();
    }

    /**
	 * Open een bon
	 */
    public String openBon(String debiteur_id) throws ServerErrorException {
        Hashtable request = new Hashtable();
        request.put("REQUEST", "open_bon");
        request.put("debiteur_id", debiteur_id);
        if (!writeObj(request)) throw new ServerErrorException("Server error");
        Hashtable result = readObj();
        if (failure(result)) throw new ServerErrorException("Server error");
        Boolean toestemming = (Boolean) result.get("toestemming");
        if (toestemming == null || toestemming.booleanValue() == false) throw new ServerErrorException("Geen toestemming!");
        String bonID = (String) result.get("bon_id");
        return bonID;
    }

    /**
	 * Sluit een bon
	 */
    public double closeBon(String bon_id) throws ServerErrorException {
        Hashtable request = new Hashtable();
        request.put("REQUEST", "close_bon");
        request.put("bon_id", bon_id);
        if (!writeObj(request)) throw new ServerErrorException("Could not close bon");
        Hashtable result = readObj();
        if (failure(result)) throw new ServerErrorException("Could not close bon");
        String bedrag = (String) result.get("bedrag");
        if (bedrag == null) return 0.0;
        return Double.parseDouble(bedrag);
    }

    /**
	 * Voeg een bestelling toe
	 */
    public void addBestelling(String bon_id, BestellingItem[] items) throws ServerErrorException {
        int count = items.length;
        String[] prijsID = new String[count];
        String[] opmerking = new String[count];
        String[] opnaam = new String[count];
        String[] aantalS = new String[count];
        String[] aantalSI = new String[count];
        String[] aantalS225 = new String[count];
        String[] aantalS50 = new String[count];
        String[] aantalS80 = new String[count];
        String[] aantalEUR = new String[count];
        for (int i = 0; i < count; i++) {
            prijsID[i] = items[i].getID();
            opmerking[i] = items[i].getOpmerking();
            opnaam[i] = items[i].getOpNaam();
            aantalS[i] = "" + items[i].getAantalS();
            aantalSI[i] = "" + items[i].getAantalSI();
            aantalS225[i] = "" + items[i].getAantalS225();
            aantalS50[i] = "" + items[i].getAantalS50();
            aantalS80[i] = "" + items[i].getAantalS80();
            aantalEUR[i] = "" + items[i].getAantalEUR();
        }
        Hashtable request = new Hashtable();
        request.put("REQUEST", "add_bestelling");
        request.put("bon_id", bon_id);
        request.put("prijs_id", prijsID);
        request.put("opmerking", opmerking);
        request.put("opnaam", opnaam);
        request.put("s", aantalS);
        request.put("si", aantalSI);
        request.put("s225", aantalS225);
        request.put("s50", aantalS50);
        request.put("s80", aantalS80);
        request.put("eur", aantalEUR);
        if (!writeObj(request)) throw new ServerErrorException("Could not do bestelling");
        Hashtable result = readObj();
        if (failure(result)) throw new ServerErrorException("Could not do bestelling");
        String[] bestellingID = (String[]) result.get("bestelling_ids");
    }

    /**
	 * Haal de bonnen van een bep. debiteur op
	 */
    public Bon[] getBonnen(String debiteur_id) throws ServerErrorException {
        Hashtable request = new Hashtable();
        request.put("REQUEST", "request_all_bonnen_of_debiteur");
        request.put("debiteur_id", debiteur_id);
        if (!writeObj(request)) throw new ServerErrorException("Server error");
        Hashtable result = readObj();
        if (failure(result)) throw new ServerErrorException("Server error");
        Hashtable bonnen = (Hashtable) result.get("bonnen");
        String bonCount = (String) bonnen.get("bonCount");
        String[] bonID = (String[]) bonnen.get("bonID");
        String[] bonDatum = (String[]) bonnen.get("bonDatum");
        String[] bonBedrag = (String[]) bonnen.get("bonBedrag");
        int count = Integer.parseInt(bonCount);
        Bon[] bon = new Bon[count];
        for (int i = 0; i < count; i++) {
            bon[i] = new Bon(bonID[i], bonDatum[i], debiteur_id, Double.parseDouble(bonBedrag[i]));
        }
        return bon;
    }

    /**
	 * Haal alle open bonnen
	 */
    public OpenBon[] getOpenBonnen() throws ServerErrorException {
        Hashtable request = new Hashtable();
        request.put("REQUEST", "request_open_bonnen");
        if (!writeObj(request)) throw new ServerErrorException("Server error");
        Hashtable result = readObj();
        if (failure(result)) throw new ServerErrorException("Server error");
        Hashtable bonnen = (Hashtable) result.get("bonnen");
        String bonCount = (String) bonnen.get("bonCount");
        String[] bonID = (String[]) bonnen.get("bonID");
        String[] bonDeb = (String[]) bonnen.get("bonDeb");
        String[] bonBedrag = (String[]) bonnen.get("bonBedrag");
        String[] debNaam = (String[]) bonnen.get("debNaam");
        String[] bonDatum = (String[]) bonnen.get("bonDatum");
        int count = Integer.parseInt(bonCount);
        ArrayList bon = new ArrayList(count);
        for (int i = 0; i < count; i++) {
            String id = bonID[i];
            String debID = bonDeb[i];
            String debName = debNaam[i];
            double bedrag = Double.parseDouble(bonBedrag[i]);
            String datum = bonDatum[i];
            if (!debName.equals(KASTURF_NAAM) && !debName.equals(EXTERNTURF_NAAM)) {
                bon.add(new OpenBon(id, debName, bedrag, datum, debID));
            }
        }
        return (OpenBon[]) bon.toArray(new OpenBon[0]);
    }

    public OpenBon openKasTurf() throws ServerErrorException {
        ArrayList debs;
        debs = getDebiteurlijst2();
        Debiteur kasDeb = null;
        for (int i = 0; i < debs.size(); i++) {
            if (((Debiteur) debs.get(i)).getNaam().equalsIgnoreCase(KASTURF_NAAM)) {
                kasDeb = (Debiteur) debs.get(i);
            }
        }
        if (kasDeb == null) throw new ServerErrorException("Could not find " + KASTURF_NAAM + " !");
        OpenBon kasTurf = new OpenBon();
        kasTurf.setDebNaam(kasDeb.getNaam());
        kasTurf.setBonID(openBon(kasDeb.getID()));
        return kasTurf;
    }

    public OpenBon openExternTurf() throws ServerErrorException {
        ArrayList debs;
        debs = getDebiteurlijst2();
        Debiteur kasDeb = null;
        for (int i = 0; i < debs.size(); i++) {
            if (((Debiteur) debs.get(i)).getNaam().equalsIgnoreCase(EXTERNTURF_NAAM)) {
                kasDeb = (Debiteur) debs.get(i);
            }
        }
        if (kasDeb == null) throw new ServerErrorException("Could not find " + EXTERNTURF_NAAM + " !");
        OpenBon kasTurf = new OpenBon();
        kasTurf.setDebNaam(kasDeb.getNaam());
        kasTurf.setBonID(openBon(kasDeb.getID()));
        return kasTurf;
    }

    public void closeAllOpenBonnen() throws ServerErrorException {
        OpenBon[] bonnen = getOpenBonnen();
        for (int i = 0; i < bonnen.length; i++) {
            closeBon(bonnen[i].getBonID());
        }
    }

    /**
	 * Betaal een bon
	 */
    public void betaalBon(String bon_id, double bedrag) throws ServerErrorException {
        Hashtable request = new Hashtable();
        request.put("REQUEST", "betaal_bon");
        request.put("bon_id", bon_id);
        request.put("bedrag", "" + bedrag);
        if (!writeObj(request)) throw new ServerErrorException("Server error");
        Hashtable result = readObj();
        if (failure(result)) throw new ServerErrorException("Server error");
        Boolean betaald = (Boolean) result.get("betaald");
        if (betaald == null || betaald.booleanValue() == false) throw new ServerErrorException("Could not betaal bon");
    }

    /**
	 * Sluit de connectie naar de server
	 */
    public void quit() {
        Hashtable request = new Hashtable();
        request.put("REQUEST", "quit");
        Hashtable result = new Hashtable();
        writeObj(request);
        result = readObj();
        if (failure(result)) System.err.println("Could not quit properly.");
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean writeObj(Hashtable o) {
        try {
            objOut.writeObject(o);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private Hashtable readObj() {
        try {
            return (Hashtable) objIn.readObject();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return new Hashtable();
    }

    /**
	 * @return
	 */
    public Debiteur[] getDebiteurWithBonLijst() throws ServerErrorException {
        ArrayList dl = getDebiteurWithBonLijst2();
        return (Debiteur[]) dl.toArray(new Debiteur[0]);
    }

    /**
	 * @return
	 */
    private ArrayList getDebiteurWithBonLijst2() throws ServerErrorException {
        Debiteur[] d;
        int itemcount;
        if (!connected) throw new ServerErrorException("Not connected");
        if (!loggedin) throw new ServerErrorException("Not logged in");
        Hashtable request = new Hashtable();
        request.put("REQUEST", "request_debiteuren_with_bon");
        if (!writeObj(request)) throw new ServerErrorException("Server error");
        Hashtable result = readObj();
        if (failure(result)) throw new ServerErrorException("Could not get debiteurlijst with open bon");
        Hashtable debiteurlijst = (Hashtable) result.get("debiteurlijst");
        if (debiteurlijst == null) throw new ServerErrorException("Could not get debiteurlijst with open bon");
        String DebCount = (String) debiteurlijst.get("debCount");
        String[] DebID = (String[]) debiteurlijst.get("debID");
        String[] DebNaam = (String[]) debiteurlijst.get("debNaam");
        String[] DebSoort = (String[]) debiteurlijst.get("debSoort");
        ArrayList debs = new ArrayList(Integer.parseInt(DebCount));
        int count = Integer.parseInt(DebCount);
        for (int i = 0; i < count; i++) {
            if (!DebNaam[i].equals(KASTURF_NAAM) && !DebNaam[i].equals(EXTERNTURF_NAAM)) debs.add(new Debiteur(DebID[i], DebNaam[i], DebSoort[i]));
        }
        return debs;
    }

    /**
	 * Haal bon details op
	 */
    public OpenBon getBonDetails(String bonID) throws ServerErrorException {
        Hashtable request = new Hashtable();
        request.put("REQUEST", "request_bon_details");
        request.put("bon_id", bonID);
        if (!writeObj(request)) throw new ServerErrorException("Server error");
        Hashtable result = readObj();
        if (failure(result)) throw new ServerErrorException("Could not get bon details");
        Hashtable bonDetails = (Hashtable) result.get("bon_details");
        if (bonDetails == null) throw new ServerErrorException("Could not get bonDetails");
        double totaalBedrag = Double.parseDouble((String) bonDetails.get("totaalBedrag"));
        String detailCount = (String) bonDetails.get("detailCount");
        String[] prijsNaam = (String[]) bonDetails.get("prijsNaam");
        String[] aantalS = (String[]) bonDetails.get("aantalS");
        String[] aantalSI = (String[]) bonDetails.get("aantalSI");
        String[] aantalS225 = (String[]) bonDetails.get("aantalS225");
        String[] aantalS50 = (String[]) bonDetails.get("aantalS50");
        String[] aantalS80 = (String[]) bonDetails.get("aantalS80");
        String[] aantalEUR = (String[]) bonDetails.get("aantalEUR");
        OpenBon bon = new OpenBon(bonID, "<nog geen naam>", totaalBedrag);
        ArrayList items = new ArrayList();
        int count = Integer.parseInt(detailCount);
        for (int i = 0; i < count; i++) {
            int sCount = (int) Double.parseDouble(aantalS[i]);
            int s50Count = (int) Double.parseDouble(aantalS50[i]);
            if (sCount > 0) items.add(new BestellingItem("", prijsNaam[i], BestellingItem.S, sCount));
            if (s50Count > 0) items.add(new BestellingItem("", prijsNaam[i], BestellingItem.S50, s50Count));
        }
        bon.setItems(items);
        return bon;
    }

    /**
	 * @param string
	 * @return
	 */
    public boolean hasDebiteurTurf(String debID) {
        OpenBon[] bonnen;
        try {
            bonnen = getOpenBonnen();
        } catch (ServerErrorException se) {
            return false;
        }
        if (bonnen == null) return false;
        for (int i = 0; i < bonnen.length; i++) {
            if (bonnen[i].getDebID().equals(debID)) return true;
        }
        return false;
    }

    private boolean failure(Hashtable o) {
        if (o == null) return true;
        Boolean s = (Boolean) o.get("SUCCESS");
        if (s == null || s.booleanValue() == false) return true;
        return false;
    }

    /**
	 * @param string
	 * @return
	 */
    public String[][] getBonOpNaam(String bonID) throws ServerErrorException {
        Hashtable request = new Hashtable();
        request.put("REQUEST", "request_bon_opnaam");
        request.put("bon_id", bonID);
        if (!writeObj(request)) throw new ServerErrorException("Server error");
        Hashtable result = readObj();
        if (failure(result)) throw new ServerErrorException("Could not get bon opnaam");
        Hashtable data = (Hashtable) result.get("bon_opnaam");
        if (data == null) throw new ServerErrorException("Could not get bonOpNaam");
        String[] naam = (String[]) data.get("naam");
        double[] bedrag = (double[]) data.get("bedrag");
        if (naam == null || bedrag == null) return null;
        String[][] d = new String[naam.length][2];
        for (int i = 0; i < naam.length; i++) {
            d[i][0] = naam[i];
            d[i][1] = BarkasClient.formatEuro(bedrag[i]);
        }
        return d;
    }
}
