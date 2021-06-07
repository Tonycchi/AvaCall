package com.example.model.robot.ev3;

import android.util.Log;

import com.example.model.connection.ConnectionService;
import com.example.model.robot.Controller;
import com.example.model.robot.ControllerInput;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class EV3Controller implements Controller {

    private final String TAG = "EV3Controller";

    private ConnectionService service;
    private List<EV3ControlElement> controlElements;

    public EV3Controller(String specs, ConnectionService service) {
        this.service = service;

        Log.d(TAG, specs);
        createElements(specs);
    }

    private void createElements(String specs) {
        /* TODO document
        joystick:50;1,8|slider:30;4|button:20;2;2000
         */
        controlElements = new ArrayList<>();

        String[] tmp = specs.split("\\|");
        HashMap<String, String> elements = new HashMap<>();
        for (String t : tmp) {
            String[] a = t.split(":");
            elements.put(a[0], a[1]);
        }
        for (String k : elements.keySet()) {
            String[] attrs = elements.get(k).split(";");
            int maxPower = Integer.parseInt(attrs[0]);
            switch (k) {
                case "joystick":
                    // $maxPower$;$right$,$left$
                    String[] ports = attrs[1].split(",");
                    controlElements.add(new EV3ControlElement.JoystickRight(Integer.parseInt(ports[0]), maxPower));
                    controlElements.add(new EV3ControlElement.JoystickLeft(Integer.parseInt(ports[1]), maxPower));
                    break;
                case "slider":
                    // $maxPower$;$port$
                    controlElements.add(new EV3ControlElement.Slider(Integer.parseInt(attrs[1]), maxPower));
                    break;
                case "button":
                    // $maxPower$;$port$;$duration$
                    controlElements.add(new EV3ControlElement.Button(Integer.parseInt(attrs[1]), maxPower));
                    break;
                default:
            }
        }

    }

    @Override
    public void sendInput(ControllerInput input) {
        service.write(createCommand(input));
    }

    public byte[] createCommand(ControllerInput input) {
        //0x|14:00|2A:00|80|00:00|A4|00|01|81:RP|A4|00|08|81:LP|A6|00|09
        //   0  1  2  3  4  5  6  7  8  9  10 11 12 13 14 15 16 17 18 19
        // 00: length 20
        // 11 & 16: right and left motor speeds respectively

        int length = 10 + controlElements.size() * 5;

        byte[] directCommand = new byte[length];
        directCommand[0] = (byte) (length - 2);
        directCommand[2] = 0x2a;
        directCommand[4] = (byte) 0x80;

        int i = 7;
        byte[] t;
        for (EV3ControlElement e : controlElements) {
            t = commandPart(e.port, e.getMotorPower(input));
            for (int k = 0; k < t.length; k++) {
                directCommand[i + k] = t[k];
            }
            i += t.length;
        }

        directCommand[17] = (byte) 0xa6;
        directCommand[19] = (byte) 0x0f;

        /*
        byte length = 20;
        byte[] directCommand = new byte[length];

        directCommand[0] = (byte) (length - 2);
        directCommand[2] = 0x2a;
        directCommand[4] = (byte) 0x80;
        directCommand[7] = (byte) 0xa4;
        directCommand[10] = (byte) 0x81;
        directCommand[12] = (byte) 0xa4;
        directCommand[15] = (byte) 0x81;
        directCommand[17] = (byte) 0xa6;
        directCommand[19] = (byte) 0x0f; // TODO

        directCommand[9] = PORT_RIGHT;    // PORT right motor
        directCommand[11] = rightPower;  // POWER right motor

        directCommand[14] = PORT_LEFT;   // PORT left motor
        directCommand[16] = leftPower;   // POWER left motor
         */
        return directCommand;
    }

    private byte[] commandPart(int port, int power) {
        byte[] r = new byte[5];
        r[0] = (byte) 0xA4; // command type: output power
        r[0] = (byte) 0x00; // filler
        r[0] = (byte) port; // which port
        r[0] = (byte) 0x81; // power prefix
        r[0] = (byte) power;// which power
        return r;
    }

    /*
    private float[] computePowers(int angle, int strength) {
        float right = 0.0f;
        float left = 0.0f;

        if (angle >= 0 && angle < 90) { //0°-89°
            left = 100; //100 to 100
            right = -100 + angle*20/9.0f; //-100 to 100

        } else if (angle >= 90 && angle < 180) { //90°-179°
            left = 100 - (angle-90)*20/9.0f; //100 to -100
            right = 100; //100 to 100

        } else if (angle >= 180 && angle < 270) { //180°-269°
            left = -100; //-100 to -100
            right = 100 - (angle-180)*20/9.0f; //50 to -100

        } else if (angle >= 270 && angle <= 360) {//270°-359°
            left = -100 + (angle-270)*20/9.0f; //-100 to 100
            right = -100; //-100 to -100
        }

        float[] output = new float[2];
        output[0] = right * strength / 10000;
        output[1] = left * strength / 10000;
        //Log.d("Motorsignale", "Links:"+output[1]+" Rechts:"+output[0]);

        return output;
    }
    */
}
