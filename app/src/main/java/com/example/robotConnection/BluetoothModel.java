package com.example.robotConnection;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.ParcelUuid;

import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.Set;

public class BluetoothModel extends RobotConnectionModel{

    private Context context;

    // Model for BluetoothFragment
    // bluetooth
    private BluetoothConnectionService bluetoothConnection;
    // Bluetooth adapter of our device
    private BluetoothAdapter bluetoothAdapter;
    // Device we want to connect with
    private BluetoothDevice selectedDevice;
    // The UUIDs of the device we want to connect with
    private ParcelUuid[] deviceUUIDs;

    public BluetoothModel(Context context) {
        this.context = context;

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public void updatePairedDeviceNames(){
        Set<BluetoothDevice> devices = bluetoothAdapter.getBondedDevices();
        ArrayList<String> bluetoothNames = new ArrayList<>();
        if (devices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : devices) {
                bluetoothNames.add(device.getName());
            }

            pairedDeviceNames.postValue(bluetoothNames);
        } else {
            //TODO: handle error
        }
    }

    @Override
    public MutableLiveData<ArrayList<String>> getPairedDevicesName() {
        return pairedDeviceNames;
    }

}
