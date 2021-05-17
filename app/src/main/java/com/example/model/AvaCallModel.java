package com.example.model;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.ParcelUuid;

import androidx.lifecycle.MutableLiveData;

import com.example.robotConnection.BluetoothModel;
import com.example.robotConnection.Device;
import com.example.robotConnection.RobotConnectionModel;

import java.util.ArrayList;

public class AvaCallModel {

    private Context context;

    private RobotConnectionModel robotConnectionModel;

    // Model for ModelSelectionFragment
    // TODO modelle abspeichern?

    // Model for EditControlsFragment
    // TODO Liste von eigener controller klasse???????

    // Model for VideoConnectionFragment
    private URLFactory urlFactory;
    private WebClient wc;
    private SessionData session;

    public AvaCallModel() {
        robotConnectionModel = new BluetoothModel();
    }

    public MutableLiveData<ArrayList<Device>> getPairedDevices() {
        return robotConnectionModel.getPairedDevices();
    }

    public MutableLiveData<Integer> getConnectionStatus() {
        return robotConnectionModel.getConnectionStatus();
    }

    public void startConnection(Device device) {
        robotConnectionModel.startConnection(device);
    }
}
