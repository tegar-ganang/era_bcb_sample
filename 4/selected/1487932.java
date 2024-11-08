package campos;

import excepciones.ExcepcionEstableciendoValorDelCampo;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Representa el numero de canales de la cancion (stero, dolby ..etc).
 * 
 * @author Rodrigo Villamil Perez
 */
public class CampoCanales extends CampoTAG {

    CampoCanales() {
        super(EnumeracionNombresCampos.Canales);
        this.soloLectura = true;
    }

    @Override
    public Object getValor() {
        try {
            return this.getCabecera().getChannels();
        } catch (Exception ex) {
            Logger.getLogger(CampoTAG.class.getName()).log(Level.WARNING, java.util.ResourceBundle.getBundle("resources/textos").getString("**_No_se_ha_podido_obtener_el_campo_") + this.getClass().getName());
        }
        return "";
    }

    @Override
    public void setValor(Object valor) throws ExcepcionEstableciendoValorDelCampo {
        throw new UnsupportedOperationException(java.util.ResourceBundle.getBundle("resources/textos").getString("No_se_permite_modificar_este_campo.") + valor);
    }
}
