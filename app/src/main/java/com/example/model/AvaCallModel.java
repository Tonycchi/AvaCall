package com.example.model;

import android.content.Context;

import androidx.lifecycle.MutableLiveData;

import com.example.robotConnection.BluetoothModel;
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

    public void updatePairedDevicesName(){
        robotConnectionModel.updatePairedDeviceNames();
    }

    public MutableLiveData<ArrayList<String>> getPairedDevicesName() {
        return robotConnectionModel.getPairedDevicesName();
    }
}
