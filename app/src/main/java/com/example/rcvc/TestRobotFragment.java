package com.example.rcvc;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

public class TestRobotFragment extends Fragment {

    public TestRobotFragment(){super(R.layout.test_robot);}

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        Button buttonYes = (Button) view.findViewById(R.id.button_yes);
        Button buttonNo = (Button) view.findViewById(R.id.button_no);
        buttonYes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickYes();
            }
        });
        buttonNo.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                onClickNo();
            }
        });
    }

    private void onClickYes(){
        FragmentManager fragmentManager = getParentFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.fragment_container_view, VideoConnectionFragment.class, null)
                .setReorderingAllowed(true)
                .addToBackStack(null) // name can be null
                .commit();
    }
    private void onClickNo(){
        FragmentManager fragmentManager = getParentFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.fragment_container_view, EditControlsFragment.class, null)
                .setReorderingAllowed(true)
                .commit();
    }
}