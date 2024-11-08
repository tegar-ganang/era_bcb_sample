package input;

import gamestates.PinballGameState;
import main.Main;
import com.jme.input.FirstPersonHandler;
import com.jme.input.InputHandler;
import com.jme.input.KeyInput;
import com.jme.input.MouseInput;
import com.jme.input.action.InputAction;
import com.jme.input.action.InputActionEvent;
import com.jme.math.FastMath;
import com.jme.math.Vector3f;
import com.jme.renderer.Camera;
import com.jmex.physics.DynamicPhysicsNode;
import components.Flipper;
import components.Plunger;

/**
 * Es el controlador de input para el tablero de juego en si.
 * Hereda de FirstPersonHandler para poder permitir al jugador el movimiento de la camara
 * con WSAD.
 */
public class PinballInputHandler extends FirstPersonHandler {

    private PinballGameState game;

    private static int tiltsAllowed = 5;

    private static long tiltPenalizationTimeInterval = 5000;

    private static long tiltFreeTimeInterval = 5000;

    private boolean tiltActive = true;

    @Override
    public void setEnabled(boolean value) {
        super.setEnabled(value);
        KeyInput.get().clear();
        MouseInput.get().clear();
    }

    public boolean isTiltActive() {
        return tiltActive;
    }

    public void setTiltActive(boolean tiltActive) {
        this.tiltActive = tiltActive;
    }

    private boolean newGameActive = false;

    public boolean isNewGameActive() {
        return newGameActive;
    }

    public void setNewGameActive(boolean newGameActive) {
        this.newGameActive = newGameActive;
    }

    public PinballInputHandler(PinballGameState game) {
        super(game.getCamera(), game.getPinballSettings().getCamMoveSpeed(), game.getPinballSettings().getCamTurnSpeed());
        this.game = game;
        setActions();
    }

    private void setActions() {
        addAction(new OpenMenuAction(), InputHandler.DEVICE_KEYBOARD, KeyInput.KEY_ESCAPE, InputHandler.AXIS_NONE, false);
        addAction(new RightFlippersActionOnce(), InputHandler.DEVICE_KEYBOARD, KeyInput.KEY_M, InputHandler.AXIS_NONE, false);
        addAction(new RightFlippersActionOnce(), InputHandler.DEVICE_KEYBOARD, KeyInput.KEY_RCONTROL, InputHandler.AXIS_NONE, false);
        addAction(new RightFlippersActionOnce(), InputHandler.DEVICE_KEYBOARD, KeyInput.KEY_RSHIFT, InputHandler.AXIS_NONE, false);
        addAction(new LeftFlippersActionOnce(), InputHandler.DEVICE_KEYBOARD, KeyInput.KEY_C, InputHandler.AXIS_NONE, false);
        addAction(new LeftFlippersActionOnce(), InputHandler.DEVICE_KEYBOARD, KeyInput.KEY_LCONTROL, InputHandler.AXIS_NONE, false);
        addAction(new LeftFlippersActionOnce(), InputHandler.DEVICE_KEYBOARD, KeyInput.KEY_LSHIFT, InputHandler.AXIS_NONE, false);
        addAction(new ChargePlungerAction(), InputHandler.DEVICE_KEYBOARD, KeyInput.KEY_RETURN, InputHandler.AXIS_NONE, false);
        addAction(new tiltAction(), InputHandler.DEVICE_KEYBOARD, KeyInput.KEY_SPACE, InputHandler.AXIS_NONE, false);
        addAction(new NewGameAction(), InputHandler.DEVICE_KEYBOARD, KeyInput.KEY_N, InputHandler.AXIS_NONE, false);
        addAction(new ChangeCameraAction(game.getCamera(), game.getPinballSettings().getCamStartPos(), game.getPinballSettings().getCamStartLookAt()), InputHandler.DEVICE_KEYBOARD, KeyInput.KEY_1, InputHandler.AXIS_NONE, false);
        addAction(new ChangeCameraAction(game.getCamera(), new Vector3f(-0.92f, 0.5f, 1.0f), new Vector3f(-0.92f, 0.90f, -1.30f)), InputHandler.DEVICE_KEYBOARD, KeyInput.KEY_2, InputHandler.AXIS_NONE, false);
        addAction(new ChangeCameraAction(game.getCamera(), new Vector3f(-0.90f, 13.5f, -18.5f), new Vector3f(-0.90f, -12.0f, 0f)), InputHandler.DEVICE_KEYBOARD, KeyInput.KEY_3, InputHandler.AXIS_NONE, false);
        addAction(new ChangeCameraAction(game.getCamera(), new Vector3f(27, 19, 27), new Vector3f(0f, 5f, 0f)), InputHandler.DEVICE_KEYBOARD, KeyInput.KEY_4, InputHandler.AXIS_NONE, false);
        addAction(new ChangeCameraAction(game.getCamera(), new Vector3f(-0.92f, 25.3f, 7.4f), new Vector3f(-.92f, 0f, -13.8f)), InputHandler.DEVICE_KEYBOARD, KeyInput.KEY_5, InputHandler.AXIS_NONE, false);
        addAction(new ChangeCameraAction(game.getCamera(), game.getPinballSettings().getCamStartPos(), game.getPinballSettings().getCamStartLookAt()), InputHandler.DEVICE_KEYBOARD, KeyInput.KEY_HOME, InputHandler.AXIS_NONE, false);
        addAction(new ScreenShotAction(), InputHandler.DEVICE_KEYBOARD, KeyInput.KEY_F1, InputHandler.AXIS_NONE, false);
        addAction(new ShowFPSAction(), InputHandler.DEVICE_KEYBOARD, KeyInput.KEY_F, InputHandler.AXIS_NONE, false);
        addAction(new ToggleMuteAction(), InputHandler.DEVICE_KEYBOARD, KeyInput.KEY_O, InputHandler.AXIS_NONE, false);
    }

    private class ToggleMuteAction extends InputAction {

        public void performAction(InputActionEvent evt) {
            if (evt.getTriggerPressed()) Main.toggleMuteAudio();
        }
    }

    private class ScreenShotAction extends InputAction {

        private int counter = 0;

        public void performAction(InputActionEvent evt) {
            if (evt.getTriggerPressed()) {
                game.getDisplay().getRenderer().takeScreenShot("ScreenShot" + counter++);
            }
        }
    }

    private class ShowFPSAction extends InputAction {

        public void performAction(InputActionEvent evt) {
            if (evt.getTriggerPressed()) {
                game.toggleShowFPS();
            }
        }
    }

    private class NewGameAction extends InputAction {

        public void performAction(InputActionEvent evt) {
            if (evt.getTriggerPressed() && newGameActive) {
                game.reinitGame();
            }
        }
    }

    private class ChangeCameraAction extends InputAction {

        private Vector3f location, lookAtpos;

        private Camera cam;

        public ChangeCameraAction(Camera cam, Vector3f location, Vector3f lookAtpos) {
            this.cam = cam;
            this.location = location;
            this.lookAtpos = lookAtpos;
        }

        public void performAction(InputActionEvent evt) {
            if (evt.getTriggerPressed()) {
                cam.setLocation(new Vector3f(location));
                cam.lookAt(new Vector3f(lookAtpos), new Vector3f(0.0f, 1.0f, 0.0f));
            }
        }
    }

    private class OpenMenuAction extends InputAction {

        public void performAction(InputActionEvent event) {
            if (event.getTriggerPressed()) {
                Main.pauseCurrentPinballGame();
                Main.newMenu().setActive(true);
            }
        }
    }

    private class RightFlippersActionOnce extends InputAction {

        public void performAction(InputActionEvent event) {
            if (event.getTriggerPressed()) {
                for (DynamicPhysicsNode flipper : game.getFlippers()) {
                    Flipper actualFlipper = (Flipper) flipper.getChild(0);
                    if (actualFlipper.isRightFlipper() && actualFlipper.isActive()) {
                        game.getGameLogic().rightFlipperMove(actualFlipper);
                        actualFlipper.setInUse(true);
                    }
                }
            } else {
                for (DynamicPhysicsNode flipper : game.getFlippers()) {
                    Flipper actualFlipper = (Flipper) flipper.getChild(0);
                    if (actualFlipper.isRightFlipper()) {
                        actualFlipper.setInUse(false);
                    }
                }
            }
        }
    }

    private class LeftFlippersActionOnce extends InputAction {

        public void performAction(InputActionEvent event) {
            if (event.getTriggerPressed()) {
                for (DynamicPhysicsNode flipper : game.getFlippers()) {
                    Flipper actualFlipper = (Flipper) flipper.getChild(0);
                    if (actualFlipper.isLeftFlipper() && actualFlipper.isActive()) {
                        game.getGameLogic().leftFlipperMove(actualFlipper);
                        actualFlipper.setInUse(true);
                    }
                }
            } else {
                for (DynamicPhysicsNode flipper : game.getFlippers()) {
                    Flipper actualFlipper = (Flipper) flipper.getChild(0);
                    if (actualFlipper.isLeftFlipper()) {
                        actualFlipper.setInUse(false);
                    }
                }
            }
        }
    }

    private class ChargePlungerAction extends InputAction {

        public void performAction(InputActionEvent event) {
            if (game.getPlunger() == null) return;
            Plunger plunger = (Plunger) (game.getPlunger().getChild(0));
            if (event.getTriggerPressed()) {
                plunger.setLoose(false);
                game.getGameLogic().plungerCharge(plunger);
            } else if (!event.getTriggerPressed()) {
                plunger.setLoose(true);
                plunger.setDistance(game.getPlunger().getLocalTranslation().z);
                game.getGameLogic().plungerRelease(plunger);
            }
        }
    }

    private class tiltAction extends InputAction {

        int actualTiltArrayElems = 0;

        long[] tiltArrayTimes = new long[tiltsAllowed];

        long lastTiltTime = 0;

        boolean pendingRestore = false;

        public void performAction(InputActionEvent event) {
            Vector3f cameraMovement = new Vector3f(3, 3, 3);
            Vector3f initCameraPos = game.getCamera().getLocation();
            if (!event.getTriggerPressed() && (tiltActive || pendingRestore)) {
                game.getCamera().setLocation(initCameraPos.subtract(cameraMovement));
                if (pendingRestore) {
                    pendingRestore = false;
                }
            } else if (event.getTriggerPressed() && tiltActive) {
                float forceIntensity = 150;
                Vector3f force = new Vector3f(FastMath.sign(FastMath.nextRandomInt(-1, 1)), 0.0f, FastMath.sign(FastMath.nextRandomInt(-1, 1))).mult(forceIntensity);
                for (DynamicPhysicsNode ball : game.getBalls()) {
                    ball.addForce(force.mult(ball.getMass()));
                    game.getCamera().setLocation(initCameraPos.add(cameraMovement));
                }
                long now = System.currentTimeMillis();
                if (now - lastTiltTime > tiltFreeTimeInterval && lastTiltTime != 0) {
                    for (int i = 0; i < tiltArrayTimes.length; i++) {
                        tiltArrayTimes[i] = 0;
                    }
                    actualTiltArrayElems = 0;
                    lastTiltTime = 0;
                }
                shiftLeft(tiltArrayTimes);
                tiltArrayTimes[tiltsAllowed - 1] = now;
                lastTiltTime = tiltArrayTimes[tiltsAllowed - 1];
                if (actualTiltArrayElems < tiltsAllowed) {
                    actualTiltArrayElems++;
                }
                if (lastTiltTime - tiltArrayTimes[0] < tiltPenalizationTimeInterval && actualTiltArrayElems == tiltsAllowed) {
                    game.getGameLogic().abuseTilt();
                    pendingRestore = true;
                } else {
                    game.getGameLogic().tilt();
                }
            }
        }

        private void shiftLeft(long[] arr) {
            for (int i = 0; i < arr.length - 1; i++) {
                arr[i] = arr[i + 1];
            }
        }
    }
}
