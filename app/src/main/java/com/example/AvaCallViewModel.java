package com.example;

import android.app.Activity;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.model.AvaCallModel;
import com.example.model.SessionData;
import com.example.model.VideoConnectionModel;
import com.example.model.WebClient;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

public class AvaCallViewModel extends ViewModel {

    AvaCallModel model = new AvaCallModel();

    // Data for BluetoothFragment
    private MutableLiveData<Boolean> bluetoothConnected;
    private MutableLiveData<List<String>> pairedDevicesList;

    // Data for ModelSelectionFragment
    private MutableLiveData<String> selectedModel; //TODO evtl String abändern (je nachdem wie wir Modelle abspeichern wollen)
    private MutableLiveData<List<String>> modelList;

    // Data for EditControlsFragment
    private MutableLiveData<String> selectedModelName;
    private MutableLiveData<List<String>> robotModelList;
    private MutableLiveData<Boolean> controllerSettings; //TODO eigene Klasse für die Controllerauswahl erstellen

    public void invitePartner() {
        model.invitePartner();
    }

    public MutableLiveData<String> getInviteLink() {
        return model.getInviteLink();
    }

    public SessionData getSession() {
        return model.getSession();
    }
}
