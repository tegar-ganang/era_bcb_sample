package servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Servlet implementation class RechercheSauvegarde
 */
@WebServlet("/RechercheSauvegarde")
public class RechercheSauvegarde extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public RechercheSauvegarde() {
        super();
    }

    /**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        gestion_amie.Connexion co = new gestion_amie.Connexion();
        String url = co.getUrl();
        String login = co.getLogin();
        String password = co.getPassword();
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        HttpSession sess = request.getSession(false);
        String action = (String) request.getParameter("action");
        if (action == null) {
        } else if (action.equals("afficher")) {
            String statut = (String) sess.getAttribute("statut");
            if (statut != null) {
                if (statut.equals("parent")) {
                    int idParent = (java.lang.Integer) sess.getAttribute("idParent");
                    try {
                        Class.forName("com.mysql.jdbc.Driver");
                        Connection con = DriverManager.getConnection(url, login, password);
                        try {
                            Statement requete = con.createStatement();
                            String requeteString = "SELECT * FROM recherche WHERE idParent=" + idParent;
                            ResultSet rs = requete.executeQuery(requeteString);
                            out.println("<table border=1>");
                            gestion_amie.Disponibilite recherche;
                            while (rs.next()) {
                                out.println("<form action=\"consulterRecherche.jsp\" method=POST");
                                String[] criteres = rs.getString("criteres").split(",");
                                int[] c = new int[7];
                                for (int i = 0; i < 7; i++) {
                                    c[i] = Integer.parseInt(criteres[i]);
                                }
                                recherche = new gestion_amie.Disponibilite(c[0], c[1], c[2], c[3], c[4], c[5], c[6]);
                                out.println("<tr><td>" + recherche.toString() + "</td><td width=\"100\" align=\"center\">");
                                if (demandeSatisfaite(idParent, rs.getString("criteres"), url, login, password)) {
                                    out.println("<img src=\"images/vert.jpg\" width=\"30\"/></td>");
                                    out.println("<td>" + "<input type=hidden name=\"criteres\" value=\"" + rs.getString("criteres") + "\"/>" + "<input type=\"submit\" name=\"consulter\" value=\"Consulter les r�sultats\"></td>");
                                } else {
                                    out.println("<img src=\"images/rouge.jpg\" width=\"30\"/></td>");
                                    out.println("<td>Aucun r�sultat</td>");
                                }
                                out.println("</tr></form><tr><td colspan=3>");
                                out.println("<form action=\"RechercheSauvegarde\" method=POST>");
                                out.println("<input type=hidden name=\"idRecherche\" value=\"" + rs.getString("idRecherche") + "\"/>" + "<input type=hidden name=\"action\" value=\"supprimer\"/>" + "<input type=\"submit\" name=\"supprimer\" value=\"Supprimer la recherche\">");
                                out.println("</form>");
                                out.println("</td></tr>");
                            }
                            out.println("</table>");
                        } catch (Exception e) {
                            out.println(e);
                        }
                    } catch (Exception e) {
                        out.println(e);
                    }
                }
            }
        } else if (action.equals("supprimer")) {
            out.println("suppression demand�e<br/>");
            try {
                String idRecherche = (java.lang.String) request.getParameter("idRecherche");
                Class.forName("com.mysql.jdbc.Driver");
                Connection con = DriverManager.getConnection(url, login, password);
                try {
                    Statement requete = con.createStatement();
                    String requeteString = "DELETE FROM recherche where idRecherche=" + idRecherche;
                    requete.executeUpdate(requeteString);
                    response.sendRedirect("consulterRecherche.jsp");
                } catch (Exception e) {
                    out.println(e);
                }
            } catch (Exception e) {
                out.println(e);
            }
        } else if (action.equals("afficherRecherches")) {
            String statut = (java.lang.String) sess.getAttribute("statut");
            if (statut != null) {
                if (statut.equals("parent")) {
                    int idParent = (java.lang.Integer) sess.getAttribute("idParent");
                    try {
                        Class.forName("com.mysql.jdbc.Driver");
                        Connection con = DriverManager.getConnection(url, login, password);
                        try {
                            Statement requete = con.createStatement();
                            String requeteString = "SELECT count(idParent) FROM recherche WHERE idParent=" + idParent;
                            ResultSet rs = requete.executeQuery(requeteString);
                            int nb_demandes = 0;
                            int nb_resultats_demande = 0;
                            while (rs.next()) {
                                nb_demandes = rs.getInt(1);
                            }
                            out.println("Nombre de demandes enregistr�es : " + nb_demandes + "<br/>");
                            if (nb_demandes > 0) {
                                requeteString = "SELECT criteres FROM recherche WHERE idParent=" + idParent;
                                rs = requete.executeQuery(requeteString);
                                while (rs.next()) {
                                    if (demandeSatisfaite(idParent, rs.getString("criteres"), url, login, password)) {
                                        nb_resultats_demande++;
                                    }
                                }
                            }
                            out.println("Nombre de demandes satisfaites <font style=\"font-size: 8pt;\">(Pertinence > 4/8)</font> : " + nb_resultats_demande + "<br/>");
                            out.println("<a href=\"consulterRecherche.jsp\">Voir</a><br/>");
                        } catch (Exception e) {
                            out.println(e);
                        }
                    } catch (Exception e) {
                        out.println(e);
                    }
                }
            }
        }
    }

    private boolean demandeSatisfaite(int idParent, String criteres, String url, String login, String password) {
        boolean ok = false;
        int[] c = new int[7];
        String[] critere = criteres.split(",");
        for (int k = 0; k < 7; k++) {
            c[k] = Integer.parseInt(critere[k]);
        }
        gestion_amie.Disponibilite recherche = new gestion_amie.Disponibilite(c[0], c[1], c[2], c[3], c[4], c[5], c[6]);
        try {
            Class.forName("com.mysql.jdbc.Driver");
            Connection con = DriverManager.getConnection(url, login, password);
            try {
                Statement requete = con.createStatement();
                String requeteString = "SELECT COUNT(D.idDispo),COUNT(A.idAmie) FROM dispoamie D, amie A";
                ResultSet rs = requete.executeQuery(requeteString);
                gestion_amie.Disponibilite[] d;
                int taille = 0;
                while (rs.next()) {
                    taille = rs.getInt(1);
                }
                d = new gestion_amie.Disponibilite[taille];
                requeteString = "SELECT D.*,A.nb_enfants_scol,A.idAmie,A.nom,A.prenom FROM dispoamie D,amie A where A.idAmie=D.idAmie";
                rs = requete.executeQuery(requeteString);
                int i = 0;
                int[] pertinence = new int[taille];
                while (rs.next()) {
                    d[i] = new gestion_amie.Disponibilite(rs.getInt("lundi"), rs.getInt("mardi"), rs.getInt("mercredi"), rs.getInt("jeudi"), rs.getInt("vendredi"), rs.getInt("samedi"), rs.getInt("nb_enfants_scol"));
                    pertinence[i] = recherche.correspondance(d[i]);
                    i++;
                }
                pertinence = Tri(pertinence, taille);
                for (i = 0; i < taille; i++) {
                    if (pertinence[i] >= 3) {
                        ok = true;
                        i = taille + 1;
                    }
                }
            } catch (Exception e) {
            }
        } catch (Exception e) {
        }
        return ok;
    }

    private int[] Tri(int[] pertinence, int taille) {
        boolean change = true;
        int tmp;
        while (change) {
            change = false;
            for (int i = 0; i < taille - 2; i++) {
                if (pertinence[i] < pertinence[i + 1]) {
                    tmp = pertinence[i];
                    pertinence[i] = pertinence[i + 1];
                    pertinence[i + 1] = tmp;
                    change = true;
                }
            }
        }
        return pertinence;
    }
}
