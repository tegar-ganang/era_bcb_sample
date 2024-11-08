package br.gov.sp.guarulhos.ceu.imagens;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import javax.imageio.ImageIO;
import javax.servlet.ServletContext;
import org.apache.commons.io.IOUtils;
import br.com.caelum.vraptor.interceptor.multipart.UploadedFile;
import br.com.caelum.vraptor.ioc.Component;
import br.gov.sp.guarulhos.ceu.modelo.Usuario;

@Component
public class Imagens {

    private File pastaImagens;

    public Imagens(ServletContext context) {
        String caminhoImagens = context.getRealPath("/WEB-INF/imagens");
        pastaImagens = new File(caminhoImagens);
        if (!pastaImagens.exists()) pastaImagens.mkdir();
    }

    public void salva(UploadedFile imagem, Usuario usuario) {
        File destino;
        if (usuario.getId() == null) {
            destino = new File(pastaImagens, usuario.hashCode() + ".jpg");
        } else {
            destino = new File(pastaImagens, usuario.getId() + ".jpg");
        }
        try {
            IOUtils.copyLarge(imagem.getFile(), new FileOutputStream(destino));
        } catch (Exception e) {
            throw new RuntimeException("Erro ao copiar imagem", e);
        }
        redimensionar(destino.getPath(), destino.getPath(), "jpg", 110, 110);
    }

    public void salva(Blob blob, Usuario usuario) {
        File destino = new File(pastaImagens, usuario.getId() + ".jpg");
        System.out.println("Tamanho do arquivo: " + destino.length());
        InputStream is;
        try {
            is = blob.getBinaryStream();
            FileOutputStream fos = new FileOutputStream(destino);
            int b = 0;
            while ((b = is.read()) != -1) {
                fos.write(b);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public File mostra(Usuario usuario) {
        File file;
        if (usuario.getId() == null) {
            file = new File(pastaImagens, usuario.hashCode() + ".jpg");
        } else {
            file = new File(pastaImagens, usuario.getId() + ".jpg");
        }
        if (file.exists()) {
            return file;
        } else {
            return new File(pastaImagens, "noimage.jpg");
        }
    }

    public void apagaFoto(Usuario usuario) {
        File file;
        if (usuario.getId() == null) {
            file = new File(pastaImagens, usuario.hashCode() + ".jpg");
        } else {
            file = new File(pastaImagens, usuario.getId() + ".jpg");
        }
        if (file.exists()) file.delete();
    }

    public static void redimensionar(String imagemInPath, String imagemOutPath, String extensao, int largura, int altura) {
        try {
            BufferedImage imagemIn = ImageIO.read(new File(imagemInPath));
            BufferedImage imagemOut = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_RGB);
            Graphics2D graph = imagemOut.createGraphics();
            graph.drawImage(imagemIn, 0, 0, largura, altura, null);
            ImageIO.write(imagemOut, extensao, new File(imagemOutPath));
        } catch (IOException e) {
            System.out.println("[Util.redimensionar] Erro: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
