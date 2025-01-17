package com.example.model.testRobot;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.example.model.MainModel;
import com.example.model.robot.ev3.EV3Controller;

/**
 * MVVM-model for steering a robot from the app. Mostly reacts to replies from the robot during
 * stalls.
 */
public class TestRobotModel {
    private static final String TAG = "TestRobotModel";
    private final MainModel mainModel;
    private final int[] lastExpected;
    MutableLiveData<String> motorStrength;
    MutableLiveData<Boolean> stall;
    private boolean previousStall;

    public TestRobotModel(MainModel mainModel) {
        motorStrength = new MutableLiveData<>();
        this.mainModel = mainModel;
        stall = new MutableLiveData<>();
        this.lastExpected = new int[2];
        previousStall = false;
    }

    public MutableLiveData<String> getMotorStrength() {
        return motorStrength;
    }

    public MutableLiveData<Boolean> getStall() {
        return stall;
    }

    /**
     * this method compares the expected strength with the actual strength and computes if a stall occurred
     * @param expectedStrength the strength send from the input
     * @param actualStrength the strength of the motor output
     * @return true if a stall occurred, false if not
     */
    public boolean detectStall(int expectedStrength, int actualStrength) {
        if (Math.abs(expectedStrength) > Math.abs(actualStrength)) {
            return Math.abs(expectedStrength - actualStrength) > Math.abs(expectedStrength * 0.7);
        }
        return false;
    }

    /**
     * covers a small problem of detecting stalls when the control element is moved too fast
     * when the changes in strengths are too big -> don't detect stalls
     * @param expectedStrength1 first strength from the input
     * @param expectedStrength2 second strength from the input
     * @return true if a big change occurred, false if not
     */
    private boolean detectBigChange(int expectedStrength1, int expectedStrength2) {
        int delta1 = Math.abs((int) (lastExpected[0] * 0.15));
        int delta2 = Math.abs((int) (lastExpected[1] * 0.15));
        return (Math.abs(lastExpected[0] - expectedStrength1) > delta1 || Math.abs(lastExpected[1] - expectedStrength2) > delta2);
    }

    /**
     * this is the complete method to check if there is a stall when we get a reply message
     * if a stall is detected sends a signal either to the website or the app
     * @param message reply message of the EV3 (from stallCommand)
     */
    public void checkStall(byte[] message) {
        EV3Controller controller = (EV3Controller) mainModel.getController();
        boolean stallDetected = false;
        int lastUsed = controller.getLastUsedId();
        int expectedStrength1 = message[2];
        int expectedStrength2 = message[3];
        int actualStrength1 = message[5];
        int actualStrength2 = message[6];
        Log.d(TAG, "ICH BIN AKTUELL 1: " + actualStrength1 + " ICH BIN SOLL 1: " + expectedStrength1);
        Log.d(TAG, "ICH BIN AKTUELL 2: " + actualStrength2 + " ICH BIN SOLL 2: " + expectedStrength2);
//            if (expectedStrength1 >= lastExpected[0] && expectedStrength2 >= lastExpected[1]) {
        if (!detectBigChange(expectedStrength1, expectedStrength2) && !(Math.abs(expectedStrength1) < 13 && expectedStrength1 != 0 && actualStrength1 == 0) && !(Math.abs(expectedStrength2) < 13 && expectedStrength2 != 0 && actualStrength2 == 0)) {
            stallDetected = detectStall(expectedStrength1, actualStrength1) ||
                    detectStall(expectedStrength2, actualStrength2);
        }
        if (stallDetected && controller.getInputFromWebClient()) {
            previousStall = true;
            mainModel.sendStallDetected(controller.getControlElements().get(lastUsed).getType(), lastUsed);
        } else if (!stallDetected && controller.getInputFromWebClient() && previousStall) {
            previousStall = false;
            mainModel.sendStallEnded(controller.getControlElements().get(lastUsed).getType(), lastUsed);
        }
        stall.postValue(stallDetected);
        lastExpected[0] = expectedStrength1;
        lastExpected[1] = expectedStrength2;
    }

    /**
     * a textview for showing all the motor outputs of the used ports to for debug purposes
     * @param message reply message of the EV3 (from outputCommand)
     */
    public void receivedMotorStrengths(byte[] message) {
        StringBuilder sb = new StringBuilder();
        EV3Controller controller = (EV3Controller) mainModel.getController();
        int[] strength = {message[5], message[6], message[7], message[8]};

        for (int i = 0; i < controller.getControlElements().size(); i++) {
            Log.d(TAG, "Hallo display message");
            String newline = i != 0 ? "\n" : "";
            if (controller.getControlElements().get(i).getType().equals("joystick")) {
                int port1 = mapPortToIndex(controller.getControlElements().get(i).getPort()[0]);
                int port2 = mapPortToIndex(controller.getControlElements().get(i).getPort()[1]);
                sb.append(newline)
                        .append("Element ")
                        .append(i)
                        .append(" steuert Port ")
                        .append(controller.getControlElements().get(i).getPort()[0])
                        .append(" an und hat die Stärke ")
                        .append(strength[port1]).append(".");
                sb.append("\n" + "Element ")
                        .append(i)
                        .append(" steuert Port ")
                        .append(controller.getControlElements().get(i).getPort()[1])
                        .append(" an und hat die Stärke ")
                        .append(strength[port2]).append(".");
            } else {
                int port = mapPortToIndex(controller.getControlElements().get(i).getPort()[0]);
                sb.append(newline)
                        .append("Element ")
                        .append(i)
                        .append(" steuert Port ")
                        .append(controller.getControlElements().get(i).getPort()[0])
                        .append(" an und hat die Stärke ")
                        .append(strength[port]).append(".");
            }
        }
        motorStrength.postValue(sb.toString());
    }

    public int mapPortToIndex(int port) {
        switch (port) {
            case (1):
                return 0;
            case (2):
                return 1;
            case (4):
                return 2;
            case (8):
                return 3;
        }
        return -1;
    }
}
