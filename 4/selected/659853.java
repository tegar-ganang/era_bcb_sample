package jmovie;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class FilmUPWrapper {

    public static ArrayList<FilmUPQueryResult> query(String titoloCercato) {
        ArrayList<FilmUPQueryResult> res = new ArrayList<FilmUPQueryResult>();
        try {
            titoloCercato = titoloCercato.replaceAll(" ", "+");
            Document doc = Jsoup.connect("http://filmup.leonardo.it/cgi-bin/search.cgi?ps=100&fmt=long&q=" + titoloCercato + "&ul=%25%2Fsc_%25&x=47&y=12&m=all&wf=0020&wm=sub&sy=0").get();
            Elements dls = doc.getElementsByTag("dl");
            for (Element dl : dls) {
                Element link = dl.getElementsByTag("a").first();
                if (link.text().contains("FilmUP - Scheda: ")) {
                    Element td = dl.getElementsByTag("td").first();
                    String info = td.text();
                    Pattern pt = Pattern.compile("FilmUP.com (.*) Titolo originale.* Anno: (.{4}).* Regia: (.*) Sito ufficiale.*");
                    Matcher m = pt.matcher(info);
                    if (m.find()) {
                        String titolo = m.group(1);
                        System.out.print(titolo + " - ");
                        String anno = m.group(2);
                        System.out.print(anno + " - ");
                        String regia = m.group(3);
                        System.out.print(regia + " - URL: ");
                        String url = link.attr("href");
                        System.out.println(url);
                        FilmUPQueryResult fpr = new FilmUPQueryResult(titolo, regia, anno, url);
                        res.add(fpr);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Timeout");
            return query(titoloCercato);
        }
        return res;
    }

    public static Film getDetailedInfo(FilmUPQueryResult fup) {
        Film film = new Film();
        film.set(Film.TITOLO, fup.titolo);
        try {
            Document doc = Jsoup.connect(fup.url).get();
            Elements tds = doc.getElementsByTag("td");
            for (int i = 0; i < tds.size(); i++) {
                Element td = tds.get(i);
                if (td.text().equals("Titolo originale:�")) {
                    System.out.println(td.text() + " " + tds.get(++i).text());
                    film.set(Film.TITOLO_ORIGINALE, tds.get(i).text());
                }
                if (td.text().equals("Nazione:�")) {
                    System.out.println(td.text() + " " + tds.get(++i).text());
                    film.set(Film.NAZIONE, tds.get(i).text());
                }
                if (td.text().equals("Anno:�")) {
                    System.out.println(td.text() + " " + tds.get(++i).text());
                    film.set(Film.ANNO, tds.get(i).text());
                }
                if (td.text().equals("Genere:�")) {
                    System.out.println(td.text() + " " + tds.get(++i).text());
                    film.set(Film.GENERE, tds.get(i).text());
                }
                if (td.text().equals("Durata:�")) {
                    System.out.println(td.text() + " " + tds.get(++i).text());
                    film.set(Film.DURATA, tds.get(i).text());
                }
                if (td.text().equals("Regia:�")) {
                    System.out.println(td.text() + " " + tds.get(++i).text());
                    film.set(Film.REGIA, tds.get(i).text());
                }
                if (td.text().equals("Sito ufficiale:�")) {
                    System.out.println(td.text() + " " + tds.get(++i).text());
                    film.set(Film.HOMEPAGE, tds.get(i).text());
                }
                if (td.text().equals("Cast:�")) {
                    System.out.println(td.text() + " " + tds.get(++i).text());
                    film.set(Film.CAST, tds.get(i).text());
                }
                if (td.text().equals("Produzione:�")) {
                    System.out.println(td.text() + " " + tds.get(++i).text());
                    film.set(Film.PRODUZIONE, tds.get(i).text());
                }
                if (td.text().equals("Distribuzione:�")) {
                    System.out.println(td.text() + " " + tds.get(++i).text());
                    film.set(Film.DISTRIBUZIONE, tds.get(i).text());
                }
            }
            Elements fonts = doc.getElementsByTag("font");
            for (int i = 0; i < fonts.size(); i++) {
                Element font = fonts.get(i);
                if (font.text().contains("Trama:")) {
                    System.out.println(font.text());
                    film.set(Film.TRAMA, font.text().substring(7));
                    break;
                }
            }
            Elements imgs = doc.getElementsByTag("img");
            for (int i = 0; i < imgs.size(); i++) {
                Element img = imgs.get(i);
                if (img.attr("src").contains("locand/")) {
                    System.out.println(img.attr("src"));
                    String nomeLocandina = fup.titolo;
                    nomeLocandina.replaceAll(":", "");
                    String thumbPath = "data/images/locandine/" + nomeLocandina + ".jpg";
                    download("http://filmup.leonardo.it/" + img.attr("src"), thumbPath);
                    film.set(Film.LOCANDINA, thumbPath);
                    String bigImgPath = "data/images/locandine/big/" + nomeLocandina + ".jpg";
                    download("http://filmup.leonardo.it/posters/loc/500/" + img.attr("src").substring(7), bigImgPath);
                    film.set(Film.LOCANDINA_GRANDE, bigImgPath);
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println("Timeout");
            return getDetailedInfo(fup);
        }
        return film;
    }

    public static void download(String url, String dest) {
        try {
            URL source = new URL(url);
            InputStream in = new BufferedInputStream(source.openStream());
            OutputStream out = new BufferedOutputStream(new FileOutputStream(dest));
            int readbyte = in.read();
            while (readbyte >= 0) {
                out.write(readbyte);
                readbyte = in.read();
            }
            in.close();
            out.close();
        } catch (Exception e) {
            System.out.println("Impossibile scaricare: " + url);
        }
    }
}
