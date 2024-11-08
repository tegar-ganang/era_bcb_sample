package gwt.cassowary.client;

import gwt.cassowary.client.EDU.Washington.grad.gjb.ExCLConstraintNotFound;
import gwt.cassowary.client.EDU.Washington.grad.gjb.ExCLError;
import gwt.cassowary.client.EDU.Washington.grad.gjb.ExCLInternalError;
import gwt.cassowary.client.EDU.Washington.grad.gjb.ExCLNonlinearExpression;
import gwt.cassowary.client.EDU.Washington.grad.gjb.ExCLRequiredFailure;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.*;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class TestApp implements EntryPoint {

    /**
    * This is the entry point method.
    */
    private static TextArea textArea;

    public static void println(String nextLine) {
        textArea.setText(textArea.getText() + nextLine + "\n");
    }

    public static void clearTextArea() {
        textArea.setText("");
    }

    public void onModuleLoad() {
        Button button = new Button("Run Cassowary Test");
        button.addStyleName("pc-template-btn");
        VerticalPanel vPanel = new VerticalPanel();
        vPanel.setWidth("100%");
        vPanel.setHorizontalAlignment(VerticalPanel.ALIGN_CENTER);
        vPanel.add(button);
        textArea = new TextArea();
        textArea.setCharacterWidth(80);
        textArea.setVisibleLines(30);
        vPanel.add(textArea);
        RootPanel.get().add(vPanel);
        button.addClickListener(new ClickListener() {

            public void onClick(Widget sender) {
                clearTextArea();
                try {
                    ClTests.gwtTest(new String[] { "400", "400", "1000" });
                } catch (ExCLInternalError e) {
                    e.printStackTrace();
                } catch (ExCLNonlinearExpression e) {
                    e.printStackTrace();
                } catch (ExCLRequiredFailure e) {
                    e.printStackTrace();
                } catch (ExCLConstraintNotFound e) {
                    e.printStackTrace();
                } catch (ExCLError e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
