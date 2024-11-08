package control;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import model.DAO;

/**
 * Servlet implementation class SongDownload
 */
public class SongDownload extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /**
	 * @see HttpServlet#HttpServlet()
	 */
    public SongDownload() {
        super();
    }

    /**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        int songid = Integer.parseInt(request.getParameter("songid"));
        System.out.println(songid);
        DAO dao = new DAO();
        BufferedInputStream inp = new BufferedInputStream(dao.downloadSongResource(songid));
        BufferedOutputStream out = new BufferedOutputStream(response.getOutputStream());
        int read;
        byte[] array = new byte[1024];
        while ((read = inp.read(array)) != -1) {
            out.write(array, 0, read);
        }
        out.close();
        inp.close();
        dao.close();
    }

    /**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    }
}
