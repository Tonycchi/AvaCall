package com.example.ui;

import android.os.Bundle;
import android.transition.TransitionInflater;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.rcvc.R;

public class ModelSelectionFragment extends Fragment {

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

        getActivity().setTitle(R.string.title_model_selection);
    }

    private void onClickEditModel(View v) {
        FragmentManager fragmentManager = getParentFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.fragment_container_view, EditControlsFragment.class, null)
                .setReorderingAllowed(true)
                .addToBackStack(null)
                .commit();
    }

    private void onClickUseModel(View v) {
        FragmentManager fragmentManager = getParentFragmentManager();
        Bundle bundle = new Bundle();
        bundle.putInt("cameFromModelSelection", 1);

        fragmentManager.beginTransaction()
                .replace(R.id.fragment_container_view, VideoConnectionFragment.class, bundle)
                .setReorderingAllowed(true)
                .addToBackStack(null)
                .commit();
    }
}