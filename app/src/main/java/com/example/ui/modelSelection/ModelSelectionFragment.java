package com.example.ui.modelSelection;

import android.os.Bundle;
import android.transition.TransitionInflater;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.example.MainViewModel;
import com.example.data.RobotModel;
import com.example.data.RobotModelDAO;
import com.example.rcvc.R;
import com.example.ui.HostActivity;
import com.example.ui.HostedFragment;
import com.example.ui.TestRobotFragment;
import com.example.ui.editControls.EditControlsFragment;

import net.simonvt.numberpicker.NumberPicker;

import java.util.List;
import java.util.stream.Collectors;

public class ModelSelectionFragment extends HostedFragment {

    private static final String TAG = "ModelSelectionFragment";
    private NumberPicker modelPicker;
    private MainViewModel viewModel;
    private TextView modelDescription;
    private ImageView modelPicture;

    public ModelSelectionFragment() {
        super(R.layout.model_selection);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

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

        String[] allRobotNames = viewModel.getAllRobotNames();
        modelPicker.setMaxValue(allRobotNames.length-1);
        modelPicker.setMinValue(0);
        modelPicker.setDisplayedValues(allRobotNames);

        modelPicker.setOnValueChangedListener(this::onSelectedModelChanged);

        modelDescription = view.findViewById(R.id.model_description_text);
        RobotModel robotModel = viewModel.getRobotModel(modelPicker.getValue());
        modelDescription.setText(robotModel.name+": "+robotModel.specs);

        //TODO: other picture
        modelPicture = view.findViewById(R.id.model_picture);
        modelPicture.setImageResource(R.drawable.no_image_available);

        getActivity().setTitle(R.string.title_model_selection);
    }

    private void onSelectedModelChanged(NumberPicker modelPicker, int oldVal, int newVal) {
        RobotModel robotModel = viewModel.getRobotModel(modelPicker.getValue());
        modelDescription.setText(robotModel.name+": "+robotModel.specs);
        //TODO: other picture
        modelPicture.setImageResource(R.drawable.no_image_available);
        Log.d(TAG, "Model at Position "+modelPicker.getValue()+" selected! Name:"+robotModel.name+" Id:"+robotModel.id);
    }

    private void onClickEditModel(View v) {
        viewModel.modelSelected(modelPicker.getValue());

        FragmentManager fragmentManager = getParentFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.fragment_container_view, EditControlsFragment.class, null, getResources().getString(R.string.fragment_tag_hosted))
                .setReorderingAllowed(true)
                .addToBackStack(null)
                .commit();
    }

    private void onClickUseModel(View v) {
        viewModel.modelSelected(modelPicker.getValue());

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
         //0 is not tested, 1 is connected, 2 is could not connect, 3 is connection lost, 4 connection is accepted = correct device, 5 connection is not accepted = wrong device
        switch (newConnectionStatus) {
            case 3:
                Log.d(TAG, "Case 3: Connection lost!");
                ((HostActivity) getActivity()).showToast(getResources().getString(R.string.connection_lost));
                break;

            default:
                Log.d(TAG, "Default: Something strange or nothing(Case -1) happend with the connection.");
                break;
        }

    }
}