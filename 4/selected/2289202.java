package br.com.studyLife.imagens;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.servlet.ServletContext;
import org.apache.commons.io.IOUtils;
import br.com.caelum.vraptor.interceptor.multipart.UploadedFile;
import br.com.caelum.vraptor.ioc.Component;
import br.com.studyLife.modelo.Usuario;
import br.com.studyLife.modelo.UsuarioWeb;

@Component
public class Imagens {

    private File pastaImagens;

    public Imagens(ServletContext context, UsuarioWeb usuarioWeb) {
        String caminhoImagens = context.getRealPath("/WEB-INF/imagens");
        pastaImagens = new File(caminhoImagens);
        pastaImagens.mkdir();
    }

    public void salva(UploadedFile imagem, Usuario usuario) {
        File destino = new File(pastaImagens, usuario.getId() + ".imagem");
        try {
            IOUtils.copyLarge(imagem.getFile(), new FileOutputStream(destino));
        } catch (IOException e) {
            throw new RuntimeException("Erro ao copiar imagem", e);
        }
    }

    public File mostra(Usuario usuario) {
        File file = new File(pastaImagens, usuario.getId() + ".imagem");
        return (file.exists()) ? file : new File(pastaImagens, "default.jpg");
    }
}
