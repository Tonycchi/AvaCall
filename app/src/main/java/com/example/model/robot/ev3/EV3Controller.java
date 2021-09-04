package com.example.model.robot.ev3;

import android.os.Handler;
import android.util.Log;

import com.example.data.RobotModel;
import com.example.model.connection.ConnectionService;
import com.example.model.robot.Controller;
import com.example.model.robot.Robot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class EV3Controller implements Controller {

    private final String TAG = "EV3Controller";

    private RobotModel model;

    public ConnectionService service;
    private ArrayList<EV3ControlElement> controlElements;
    private String controlElementString = "";
    private int[] ids = new int[4];
    private int usedId;

    public void setUsedId(int id){
        usedId = id;
    }

    public ArrayList<EV3ControlElement> getControlElements(){
        return controlElements;
    }

    public int getUsedId() {
        return usedId;
    }

    public EV3Controller(RobotModel model, ConnectionService service) {
        this.service = service;

        this.model = model;

        Log.d(TAG, model.specs);
        createElements(model.specs);
    }

    @Override
    public void sendInput(int... input) {
        Log.d(TAG, Arrays.toString(input));
        byte[] inputCommand = createCommand(input);
        byte[] outputCommand = createStallCommand(input);
        service.write(inputCommand);
        try {
            Log.d(TAG, "before sleep");
            Thread.sleep(50);
            Log.d(TAG, "after sleep");
            service.write(outputCommand);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void getOutput() {
        service.write(createOutputCommand());
    }


    public String getControlElementString() {
        return controlElementString;
    }

    public RobotModel getCurrentModel() {
        return model;
    }

    /**
     * creates EV3ControlElement objects according to specifications
     *
     * @param specs string specifying control element to port mapping
     */
    private void createElements(String specs) {
        /* TODO document outside of code
        we get:
        -$controlElement$|$controlElement$|...
         */
        controlElements = new ArrayList<>();

        // split into $controlElement$ = $element$:$attributes$
        String[] tmp = specs.split("\\|");
        // put into list with [0] = $element$, [1] = $attributes$
        ArrayList<String[]> list = new ArrayList<>();
        for (String t : tmp) {
            String[] a = t.split(":");
            list.add(t.split(":"));
        }
        // now translate each $attributes$ into corresponding Objects:
        for (String[] k : list) {
            String[] attrs = k[1].split(";");
            int maxPower = Integer.parseInt(attrs[0]);
            int[] ports;
            switch (k[0]) {
                case "joystick":
                    // $maxPower$;$right$,$left$
                    String[] portsString = attrs[1].split(",");
                    ports = new int[2];
                    ports[0] = Integer.parseInt(portsString[0]);
                    ports[1] = Integer.parseInt(portsString[1]);
                    controlElements.add(new EV3ControlElement.Joystick(ports, maxPower));
                    addToString("joystick");
                    break;
                case "slider":
                    // $maxPower$;$port$
                    ports = new int[1];
                    ports[0] = Integer.parseInt(attrs[1]);
                    controlElements.add(new EV3ControlElement.Slider(ports, maxPower));
                    addToString("slider");
                    break;
                case "button":
                    // $maxPower$;$port$;$duration$
                    ports = new int[1];
                    ports[0] = Integer.parseInt(attrs[1]);
                    int dur = Integer.parseInt(attrs[2]);
                    controlElements.add(new EV3ControlElement.Button(ports, maxPower, dur));
                    addToString("button");
                    break;
                default:
            }
        }

        String s = "";
        for (int i = 0; i < controlElements.size(); i++) {
            EV3ControlElement e = controlElements.get(i);
            s += i + ": ";
            s += e.getClass().getName() + " ports: ";
            s += Arrays.toString(e.port) + "\n";
        }
        Log.d(TAG, s);

    }

    /**
     * @param input [id, value, ...]
     * @return direct command for EV3
     */
    private byte[] createCommand(int... input) {
        //0x|14:00|2A:00|80|00:00|A4|00|0p|81:po|...|A6|00|0P
        //   0  1  2  3  4  5  6  7  8  9  10 11
        // 0 length of command minus 2
        // 2-6 predefined
        // 7-11 command for one motor (see commandPart)
        // last 3 bytes: A6 opcode for start output
        //               00 filler
        //               0P = sum of used ports

        int id = input[0];              // get id of input
        EV3ControlElement e = controlElements.get(id);  // for this
        Log.d("TAG", e.getClass().toString());
        byte[] output = e.getCommand(Arrays.copyOfRange(input, 1, input.length));

        int length = 10 + output.length;            // e.g. joystick may return two values
        int lastCommand = 7 + output.length;
        byte[] directCommand = new byte[length];        // this will be the command

        directCommand[0] = (byte) (length - 2);         // pre defined parts of direct command

        //TODO:
        //directCommand[2] = port;            //message counter is used as info which port is used
        if (e.port.length == 1) {
            directCommand[2] = Byte.parseByte(Integer.toHexString(e.port[0]), 16);
        }
        else {
            Log.d(TAG, "Port 1: "+e.port[0]+" Port 2: "+e.port[1]);
            directCommand[2] = Byte.parseByte(Integer.toHexString((e.port[0]<<4)+e.port[1]), 16);
        }

        directCommand[3] = (byte) id;              //message counter is used as info which control element is writing

        directCommand[4] = (byte) 0x80;
        directCommand[5] = (byte) 0x04;

        int commandPos = 7;                                      // position of first output power command
        byte portSum = 0;
        System.arraycopy(output, 0, directCommand, commandPos, output.length);
        for (int p : e.port)
            portSum += p;

        directCommand[lastCommand] = (byte) 0xa6;                // end of direct command
        directCommand[lastCommand + 2] = portSum;
        return directCommand;
    }

    private byte[] createOutputCommand(int... input) {
        //0x|14:00|2A:00|80|00:00|A4|00|0p|81:po|...|A6|00|0P
        //   0  1  2  3  4  5  6  7  8  9  10 11
        // 0 length of command minus 2
        // 2-6 predefined
        // 7-11 command for one motor (see commandPart)
        // last 3 bytes: A6 opcode for start output
        //               00 filler
        //               0P = sum of used ports
        for (int i=0; i< controlElements.size(); i++) {
            switch (controlElements.get(i).port[0]) {
                case 1:
                    ids[0] = i;
                    break;
                case 2:
                    ids[1] = i;
                    break;
                case 4:
                    ids[2] = i;
                    break;
                case 8:
                    ids[3] = i;
                    break;
                default:
            }
            if ((controlElements.get(i).port.length) > 1) {
                switch (controlElements.get(i).port[1]) {
                    case 1:
                        ids[0] = i;
                        break;
                    case 2:
                        ids[1] = i;
                        break;
                    case 4:
                        ids[2] = i;
                        break;
                    case 8:
                        ids[3] = i;
                        break;
                    default:
                }
            }
        }

        byte[] directCommand = new byte[39];
        byte[] tmp;
        directCommand[0] = (byte) 0x25;
        directCommand[2] = Byte.parseByte(Integer.toHexString((ids[0]<<4)+ids[1]), 16);
        directCommand[3] = Byte.parseByte(Integer.toHexString((ids[2]<<4)+ids[3]), 16);
        directCommand[5] = (byte) 0x04;

        tmp = commandPart((byte) 0x10, (byte) 0x60);
        System.arraycopy(tmp, 0, directCommand, 7, 8);
        tmp = commandPart((byte) 0x11, (byte) 0x61);
        System.arraycopy(tmp, 0, directCommand, 15, 8);
        tmp = commandPart((byte) 0x12, (byte) 0x62);
        System.arraycopy(tmp, 0, directCommand, 23, 8);
        tmp = commandPart((byte) 0x13, (byte) 0x63);
        System.arraycopy(tmp, 0, directCommand, 31, 8);
        return directCommand;
    }

    private byte[] createStallCommand(int... input) {
        byte[] tmp;

        EV3ControlElement controlElement = controlElements.get(input[0]);
        byte[] directCommand = controlElement.port.length > 1 ? new byte[23] : new byte[15];
        directCommand[0] = controlElement.port.length > 1 ? (byte) 0x15 : (byte) 0x0D;
        byte[] motorPower = controlElement.getMotorPower(input);
        directCommand[2] = controlElement.getMotorPower(input)[0];
        directCommand[3] = motorPower.length > 1 ? motorPower[1] : (byte) 0x00;
        directCommand[5] = (byte) 0x02;

        int port1 = controlElement.port[0];
        tmp = commandPart(intToBytePort(port1), (byte) 0x60);
        System.arraycopy(tmp, 0, directCommand, 7, 8);
        if (controlElement.port.length > 1) {
            int port2 = controlElement.port[1];
            tmp = commandPart(intToBytePort(port2), (byte) 0x61);
            System.arraycopy(tmp, 0, directCommand, 15, 8);
        }

        return directCommand;
    }

    /**
     * @param port  ev3 motor port
     * @param counter offset of global memory
     * @return part of direct command for given port
     */
    private byte[] commandPart(byte port, byte counter) {
        byte[] r = new byte[8];
        r[0] = (byte) 0x99;
        r[1] = (byte) 0x1C;
        r[3] = port;
        r[4] = (byte) 0x08;
        r[5] = (byte) 0x02;
        r[6] = (byte) 0x01;
        r[7] = counter;
        return r;
    }

    /**
     *
     * @param element string to be added to controlElementString
     */
    private void addToString(String element) {
        if (!controlElementString.equals(""))
            controlElementString += "|";
        controlElementString += element;
    }

    private byte intToBytePort(int port) {
        switch (port) {
            case 1:
                return 0x10;
            case 2:
                return 0x11;
            case 4:
                return 0x12;
            case 8:
                return 0x13;
            default:
                return 0x00;
        }
    }

    /**
     * Puts the control elements we have to create in a certain order 1.joystick 2.slider 3.button
     * @param input string of control elements we need for the selected model f.e. joystick|button|slider|button
     * @return String Array with the order we defined above
     */
}


