package com.example.rcvc;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

public class DirectCommander {

    private final BluetoothConnectionService B;

    //definitions will probably be able to be set in settings
    //should probably make library of ev3 command parts
    private final byte PORT_RIGHT;
    private final byte PORT_LEFT;

    private final int maxPower;


    public DirectCommander(Context context, BluetoothConnectionService service) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);

        PORT_RIGHT = (byte) pref.getInt("motor_right", 1);
        PORT_LEFT = (byte) pref.getInt("motor_left", 8);
        maxPower = pref.getInt("max_speed", 50);
        B = service;
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
        byte[] directCommand = new byte[length];

        directCommand[0] = (byte) (length - 2);
        directCommand[2] = 0x2a;
        directCommand[4] = (byte) 0x80;
        directCommand[7] = (byte) 0xa4;
        directCommand[10] = (byte) 0x81;
        directCommand[12] = (byte) 0xa4;
        directCommand[15] = (byte) 0x81;
        directCommand[17] = (byte) 0xa6;
        directCommand[19] = (byte) (PORT_RIGHT + PORT_LEFT);

        directCommand[9] = PORT_RIGHT;    // PORT right motor
        directCommand[11] = right;  // POWER right motor

        directCommand[14] = PORT_LEFT;   // PORT left motor
        directCommand[16] = left;   // POWER left motor

        return directCommand;
    }

    public byte calcPower(float motorSpeed) {
        return (byte) (motorSpeed * maxPower);
    }
}