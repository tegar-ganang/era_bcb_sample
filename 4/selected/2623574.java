package wheel.components;

import wheel.WheelException;
import wheel.json.JSONArray;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;

/**
 * Extending this component makes it relatively easty to produce JSON-stream from components.
 * @author Henri Frilund
 */
public abstract class JSONComponent extends Component {

    protected JSONComponent(String componentId) {
        super(componentId);
    }

    /**
     * Renders a JSONArray to the response. 
     * @param jsonArray
     */
    protected void renderJSON(JSONArray jsonArray) {
        HttpServletResponse response = getEngine().getResponse();
        response.setContentType("text/javascript");
        try {
            response.setCharacterEncoding(getEngine().getEncoding());
            Writer writer = response.getWriter();
            jsonArray.write(writer);
            writer.close();
        } catch (IOException e) {
            throw new WheelException("Could not write json array to output writer. Perhaps the writer has already been initialized?", e, this);
        }
    }
}
