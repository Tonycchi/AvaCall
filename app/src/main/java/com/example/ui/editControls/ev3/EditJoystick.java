package com.example.ui.editControls.ev3;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.example.rcvc.R;

public class EditJoystick extends EditControlElement {

    public EditJoystick() {
        super(R.layout.ev3_joystick);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle saveInstanceState) {
        return inflater.inflate(R.layout.ev3_joystick, parent, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {

    }
}