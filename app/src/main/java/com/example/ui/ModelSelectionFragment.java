package com.example.ui;

import android.os.Bundle;
import android.transition.TransitionInflater;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import com.example.rcvc.R;
import com.example.ui.editControls.EditControlsFragment;

public class ModelSelectionFragment extends HostedFragment {

    private static final String TAG = "ModelSelectionFragment";
    private NumberPicker modelPicker;

    public ModelSelectionFragment() {
        super(R.layout.model_selection);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TransitionInflater inflater = TransitionInflater.from(requireContext());
        setExitTransition(inflater.inflateTransition(R.transition.fade));
        setEnterTransition(inflater.inflateTransition(R.transition.slide));
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        //super.onViewCreated(view, savedInstanceState);

        Button useModel = view.findViewById(R.id.button_use_model);
        Button editModel = view.findViewById(R.id.button_edit_model);

        useModel.setOnClickListener(this::onClickUseModel);

        editModel.setOnClickListener(this::onClickEditModel);

        modelPicker = view.findViewById(R.id.model_picker);
        String[] test = {"Kettenroboter", "Kettenroboter mit Greifarm", "Tony stinkt nach maggi", "mit Ei"};
        modelPicker.setMaxValue(test.length-1);
        modelPicker.setMinValue(0);
        modelPicker.setWrapSelectorWheel(true);
        modelPicker.setDisplayedValues(test);



        getActivity().setTitle(R.string.title_model_selection);
    }

    private void onClickEditModel(View v) {
        FragmentManager fragmentManager = getParentFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.fragment_container_view, EditControlsFragment.class, null, getResources().getString(R.string.fragment_tag_hosted))
                .setReorderingAllowed(true)
                .addToBackStack(null)
                .commit();
    }

    private void onClickUseModel(View v) {
        FragmentManager fragmentManager = getParentFragmentManager();
        Bundle bundle = new Bundle();
        bundle.putInt("cameFromModelSelection", 1);

        fragmentManager.beginTransaction()
                .replace(R.id.fragment_container_view, TestRobotFragment.class, bundle, getResources().getString(R.string.fragment_tag_hosted))
                .setReorderingAllowed(true)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void connectionStatusChanged(Integer newConnectionStatus) {
        //TODO: implement
        ((HostActivity)getActivity()).showToast("Irgendwas mit Bluetooth hat sich geändert - noch nicht weiter geregelt, was jetzt passiert!");
    }
}
