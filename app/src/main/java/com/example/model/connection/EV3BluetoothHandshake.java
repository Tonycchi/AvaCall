package com.example.model.connection;

import java.util.ArrayList;
import java.util.List;

public class EV3BluetoothHandshake implements ByteArrayHandshake {
    @Override
    public List<byte[]> getSyn() {
        List<byte[]> directCommands = new ArrayList<>();

        byte[] noOp = new byte[8];
        noOp[0] = 0x06;                     //length
        noOp[1] = 0x00;                    //length
        noOp[2] = 0x00;                    //first message
        noOp[3] = 0x00;                    //first message
        noOp[4] = 0x00;                    // Direct command, reply required
        noOp[5] = 0x00;                    //global variables
        noOp[6] = 0x00;                    //global and local variables
        noOp[7] = 0x01;                    //opcode
        directCommands.add(noOp);

        //TODO: needed??
        byte[] waitForSoundReady = new byte[8];
        waitForSoundReady[0] = 0x06;                    //length
        waitForSoundReady[1] = 0x00;                    //length
        waitForSoundReady[2] = 0x01;                    //message counter
        waitForSoundReady[3] = 0x00;                    //message counter
        waitForSoundReady[4] = 0x08;                    // Direct command, reply required
        waitForSoundReady[5] = 0x00;                    //global variables
        waitForSoundReady[6] = 0x00;                    //global and local variables
        waitForSoundReady[7] = (byte) 0x96;             //opcode
        directCommands.add(waitForSoundReady);

        //TODO: make methods and cooler stuff
        byte[] playSound = new byte[32];
        playSound[0] = 0x1E;                    //length
        playSound[1] = 0x00;                    //length
        playSound[2] = 0x02;                    //message counter
        playSound[3] = 0x00;                    //message counter
        playSound[4] = 0x00;                    // Direct command, reply required
        playSound[5] = 0x00;                    //global variables
        playSound[6] = 0x00;                    //global and local variables
        playSound[7] = (byte) 0x94;             //opcode
        playSound[8] = (byte) 0x02;             //command
        playSound[9] = (byte) 0x81;             //string starts
        playSound[10] = (byte) 0x64;            //byte representation of ascii
        playSound[11] = (byte) 0x84;
        playSound[12] = (byte) 0x2E;
        playSound[13] = (byte) 0x2F;
        playSound[14] = (byte) 0x75;
        playSound[15] = (byte) 0x69;
        playSound[16] = (byte) 0x2F;
        playSound[17] = (byte) 0x44;
        playSound[18] = (byte) 0x6F;
        playSound[19] = (byte) 0x77;
        playSound[20] = (byte) 0x6E;
        playSound[21] = (byte) 0x6C;
        playSound[22] = (byte) 0x6F;
        playSound[23] = (byte) 0x61;
        playSound[24] = (byte) 0x64;
        playSound[25] = (byte) 0x53;
        playSound[26] = (byte) 0x75;
        playSound[27] = (byte) 0x63;
        playSound[28] = (byte) 0x63;
        playSound[29] = (byte) 0x65;
        playSound[30] = (byte) 0x73;
        playSound[31] = (byte) 0x00;            //string end
        directCommands.add(playSound);

        return directCommands;
    }

    @Override
    public boolean isAckCorrect(byte[] ack) {
        return ack[2] == 0x00 && ack[4] == 0x02;
    }
}
