package com.example.rcvc;

public class ButtonController extends Controller {
    //TODO use DirectCommander

    private final String TAG = "ButtonController";

    public static final int STOP = 0;
    public static final int FORWARD = 1;
    public static final int BACKWARD = 2;
    public static final int TURN_RIGHT = 3;
    public static final int TURN_LEFT = 4;

    public ButtonController(BluetoothConnectionService b) {
        super(b);
    }

    public void sendPowers(int command, int dummy) {
        switch (command) {
            case STOP:
                DirectCommander.send(0.0f, 0.0f, B);
                break;
            case FORWARD:
                DirectCommander.send(1.0f, 1.0f, B);
                break;
            case BACKWARD:
                DirectCommander.send(-1.0f, -1.0f, B);
                break;
            case TURN_RIGHT:
                DirectCommander.send(-1.0f, 1.0f, B);
                break;
            case TURN_LEFT:
                DirectCommander.send(1.0f, -1.0f, B);
                break;
            default:
        }
    }

}