package com.example.rcvc;

public class RobotController {
    public final int STOP = 0;
    public final int FORWARD = 1;
    public final int BACKWARD = 2;
    public final int TURN_RIGHT = 3;
    public final int TURN_LEFT = 4;

    private final String startDirCom = "0D002A00800000A4000";
    private final String endDirCom = "A6000";

    private final String plus_100 = "8164";
    private final String plus_50 = "8132";
    private final String minus_50 = "81CE";
    private final String plus_25 = "8119";
    private final String minus_25 = "81E7";
    private final String port_BC = "6";
    private final String port_B = "2";
    private final String port_C = "4";

    //start + port + power + end + port
    private final String directCommandForward = startDirCom + port_BC + plus_50 + endDirCom + port_BC;
    private final String directCommandBackward = startDirCom + port_BC + minus_50 + endDirCom + port_BC;
    private final String directCommandRightPortB = startDirCom + port_B + minus_50 + endDirCom + port_B;
    private final String directCommandRightPortC = startDirCom + port_C + plus_50 + endDirCom + port_C;
    private final String directCommandLeftPortB = startDirCom + port_B + plus_50 + endDirCom + port_B;
    private final String directCommandLeftPortC = startDirCom + port_C + minus_50 + endDirCom + port_C;
    private final String directCommandStop = "09002A00000000A3000F00";

    private BluetoothConnectionService b;

    public RobotController(BluetoothConnectionService bluetoothConnectionService) {
        this.b = bluetoothConnectionService;
    }

    public void sendCommands(int command) throws Exception {
        switch (command) {
            case STOP:
                b.write(hexStringToByteArray(directCommandStop));
                break;
            case FORWARD:
                b.write(hexStringToByteArray(directCommandForward));
                break;
            case BACKWARD:
                b.write(hexStringToByteArray(directCommandBackward));
                break;
            case TURN_RIGHT:
                b.write(hexStringToByteArray(directCommandRightPortB));
                b.write(hexStringToByteArray(directCommandRightPortC));
                break;
            case TURN_LEFT:
                b.write(hexStringToByteArray(directCommandLeftPortB));
                b.write(hexStringToByteArray(directCommandLeftPortC));
                break;
            default:
                throw new Exception();
        }
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
}
