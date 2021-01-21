package com.example.rcvc;

public class DirectCommander {

    private final BluetoothConnectionService B;

    //definitions will probably be able to be set in settings
    //should probably make library of ev3 command parts
    private static final byte PORT_RIGHT = 0x01;
    private static final byte PORT_LEFT = 0x08;

    private int maxPower = 50;

    public DirectCommander(BluetoothConnectionService b, int maxPower) {
        setMaxPow(maxPower);
        B = b;
    }

    /**
     * bounds maxPow to [-100, 100]
     *
     * @param maxPow maximum motor power
     */
    public void setMaxPow(int maxPow) {
        int pow = maxPow;
        if (maxPow > 100) {
            pow = 100;
        }
        if (maxPow < -100) {
            pow = -100;
        }
        maxPower = pow;
    }

    /**
     * sends created direct command to bluetooth connection
     *
     * @param right,left motor speed scales
     */
    public void send(float right, float left) {
        byte[] command = createCommand(calcPower(right), calcPower(left));
        B.write(command);
    }

    /**
     * creates a direct command for movement
     *
     * @param right,left motor speeds
     * @return direct command as byte array
     */
    public byte[] createCommand(byte right, byte left) {
        //0x|14:00|2A:00|80|00:00|A4|00|01|81:RP|A4|0|08|81:LP|A6|00|09
        //   0  1  2  3  4  5  6  7  8  9  10 11 12 13 14 15 16 17 18 19
        // 00: length 20
        // 11 & 16: right and left motor speeds respectively

        byte length = 20;
        byte[] y = new byte[length];
        for (int i = 0; i < length; i++) {
            y[i] = 0;
        }

        y[0] = (byte) (length - 2);
        y[2] = 0x2a;
        y[4] = (byte) 0x80;
        y[7] = (byte) 0xa4;
        y[10] = (byte) 0x81;
        y[12] = (byte) 0xa4;
        y[15] = (byte) 0x81;
        y[17] = (byte) 0xa6;
        y[19] = PORT_RIGHT + PORT_LEFT;

        y[9] = PORT_RIGHT;    // PORT right motor
        y[11] = right;  // POWER right motor

        y[14] = PORT_LEFT;   // PORT left motor
        y[16] = left;   // POWER left motor

        return y;
    }

    public byte calcPower(float x) {
        return (byte) (x * maxPower);
    }
}