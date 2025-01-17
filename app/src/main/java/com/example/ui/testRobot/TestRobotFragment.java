package com.example.ui.testRobot;

import static com.example.Constants.USER_RELEASE;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.transition.TransitionInflater;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.MainViewModel;
import com.example.rcvc.R;
import com.example.ui.HostActivity;
import com.example.ui.HostedFragment;
import com.example.ui.serverConnection.VideoConnectionFragment;
import com.example.ui.editControls.EditControlsFragment;

import java.util.ArrayList;
import java.util.List;

import io.github.controlwear.virtual.joystick.android.JoystickView;

/**
 * {@link android.app.Fragment Fragment} for controlling the robot from the app. Holds control
 * elements (joysticks, sliders, buttons) and shows stalls.
 */
public class TestRobotFragment extends HostedFragment {

    private static final String TAG = "TestRobotFragment";

    //true if this fragment was started directly from model selection
    private boolean cameFromModelSelection;

    private MainViewModel viewModel;
    private View thisView;
    private TextView motorStrengthText;
    private boolean borderVisible;
    private Handler handler;
    private boolean stallDetected;


    // Observer checks the changed motorStrengths for the textview
    public final Observer<String> motorStrengthObserver = new Observer<String>() {
        @Override
        public void onChanged(@Nullable final String newStrength) {
            // Update the UI
            motorStrengthText.setText(newStrength);
        }
    };

    // Observer checks if there is a stall signal
    public final Observer<Boolean> stallObserver = new Observer<Boolean>() {
        @Override
        public void onChanged(Boolean booleans) {
            stallDetected = booleans;
        }
    };

    public TestRobotFragment() { super(R.layout.test_robot); }

    /**
     * show red border when there is a stall
     */
    public void showBorder() {
        if(!borderVisible) {
            borderVisible = !borderVisible;
            thisView.setBackgroundResource(R.drawable.faded_border);
        }
    }

    /**
     * hide border if there is no stall
     */
    public void hideBorder() {
        if(borderVisible) {
            borderVisible = !borderVisible;
            thisView.setBackground(null);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        cameFromModelSelection = false;
        try {
            cameFromModelSelection = (requireArguments().getInt("cameFromModelSelection") == 1);
        } catch(IllegalStateException ignore) { }

        TransitionInflater inflater = TransitionInflater.from(requireContext());
        setExitTransition(inflater.inflateTransition(R.transition.fade));
        setEnterTransition(inflater.inflateTransition(R.transition.slide));
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        borderVisible = false;

        thisView = (getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
                ? view.findViewById(R.id.test_robot_fragment)
                : view.findViewById(R.id.test_robot_fragment_landscape);

        ConstraintLayout constraintLayout = (getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
                ? view.findViewById(R.id.test_robot_fragment)
                : view.findViewById(R.id.test_robot_fragment_landscape);

        String t = viewModel.getSelectedModelElements(); //joystick|button|slider|button
        String[] order = rankOrder(t); // sort the elements
        stallDetected = false;
        Log.d(TAG, t);
        createControlElements(order, constraintLayout); // create the elements into the constraint layout

        if(!USER_RELEASE) {
            // this is an extra thread which updates the showed motor strengths every 50ms
            handler = new Handler();
            handler.post(getMotorOutput); // start the runnable
            motorStrengthText = view.findViewById(R.id.text_motor_strength);
            MutableLiveData<String> motorStrength = viewModel.getMotorStrength();
            motorStrength.observe(getViewLifecycleOwner(), motorStrengthObserver);
        }

        viewModel.getStall().observe(getViewLifecycleOwner(), stallObserver);

        Button buttonYes = view.findViewById(R.id.button_yes);
        Button buttonNo = view.findViewById(R.id.button_no);

        buttonYes.setOnClickListener(this::onClickYes);
        buttonNo.setOnClickListener(this::onClickNo);

        getActivity().setTitle(R.string.title_test_robot);
    }

    // runnable for the continuous motor strengths to show
    Runnable getMotorOutput = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "loop loop");
            viewModel.getControlOutput();
            handler.postDelayed(this, 50);
        }
    };

    /**
     * go to the next fragment
     */
    private void onClickYes(View v) {
        Log.d(TAG, "YES BABY");
        if (!USER_RELEASE) handler.removeCallbacks(getMotorOutput); // stop the runnable
        FragmentManager fragmentManager = getParentFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.fragment_container_view, VideoConnectionFragment.class, null, getResources().getString(R.string.fragment_tag_hosted))
                .setReorderingAllowed(true)
                .addToBackStack(null)
                .commit();
    }

    /**
     * go the previous fragment
     */
    private void onClickNo(View v) {
        Log.d(TAG, "NO BABY");
        if (!USER_RELEASE) handler.removeCallbacks(getMotorOutput); // stop the runnable
        FragmentManager fragmentManager = getParentFragmentManager();
        if(cameFromModelSelection) {    //if cameFromModelSelection: pop to modelselection and then switch to editcontrols
            fragmentManager.popBackStack();
            fragmentManager.beginTransaction()
                    .replace(R.id.fragment_container_view, EditControlsFragment.class, null, getResources().getString(R.string.fragment_tag_hosted))
                    .setReorderingAllowed(true)
                    .addToBackStack(null)
                    .commit();
        }else{  //if cameFromEdit: simply pop back
            fragmentManager.popBackStack();
        }
    }

    // TODO: WAS MACHEN WIR HIER?!?!?!
    @Override
    public void robotConnectionStatusChanged(Integer newConnectionStatus) {
        ((HostActivity)getActivity()).showToast("Irgendwas mit Bluetooth hat sich geändert - noch nicht weiter geregelt, was jetzt passiert!");
    }

    /**
     * Puts the control elements we have to create in a certain order 1.joystick 2.slider 3.button
     * @param input string of control elements we need for the selected model e.g. joystick|button|slider|button
     * @return String Array with the order we defined above
     */
    public String[] rankOrder(String input) {
        String[] cases = {"joystick", "slider", "button"};
        List<String> temp = new ArrayList<>();
        int lastIndex;
        for (String s : cases) {
            lastIndex = 0;
            while (lastIndex != -1) {
                lastIndex = input.indexOf(s, lastIndex);
                if (lastIndex != -1) {
                    temp.add(s);
                    lastIndex += s.length();
                }
            }
        }
        Log.d(TAG, "Reihenfolge" + temp);
        String[] result = new String[temp.size()];
        temp.toArray(result);
        return result;
    }

    /**
     * All control elements are created in this method
     * @param order the order in which we want to create the control elements
     * @param constraintLayout the current constraintLayout we are using
     */
    @SuppressLint("ClickableViewAccessibility")
        public void createControlElements(String[] order, ConstraintLayout constraintLayout) {
        int[] controlElements = new int[order.length];
        ConstraintSet set = new ConstraintSet();
        // used to rotate all sliders at the same time
        List<SeekBar> sliderList = new ArrayList<>();
        //iterate through the order Array
        for (int i = 0; i < order.length; i++) {
            // id of position in the order of control elements
            int id = i;
            switch (order[i]) {
                case "joystick":
                    // create the joystick
                    JoystickView joystick = new JoystickView(getContext());
                    joystick.setId(View.generateViewId());
                    joystick.setBorderWidth((int) getResources().getDimension(R.dimen.joystick_border_width));
                    joystick.setBackgroundSizeRatio(0.5f);
                    joystick.setButtonSizeRatio(0.3f);
                    joystick.setBorderColor(ContextCompat.getColor(getContext(), R.color.joystick_border));
                    joystick.setButtonColor(ContextCompat.getColor(getContext(), R.color.joystick_button));
                    joystick.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.joystick_background));

                    // add the joystick to the constraintLayout
                    // set constraintHeight and constraintWidth (this depends on the orientation)
                    // update the constraintSet based on the orientation in dependency of control elements created before
                    constraintLayout.addView(joystick);
                    set.constrainHeight(joystick.getId(), (int) getResources().getDimension(R.dimen.control_element_size));
                    if ((getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)) {
                        updateConstraintSet(i, joystick.getId(), controlElements, set);
                        set.constrainWidth(joystick.getId(), (int) getResources().getDimension(R.dimen.control_element_size));
                    } else {
                        updateConstraintSetLandscape(i, joystick.getId(), controlElements, set);
                        set.constrainWidth(joystick.getId(), (int) getResources().getDimension(R.dimen.control_element_size_landscape));
                    }
                    // apply all changes to the constraintLayout
                    set.applyTo(constraintLayout);

                    /**
                     * Code executed when you first touch the joystick and/or release the joystick+
                     */
                    joystick.setOnTouchListener((v, event) -> {
                        // When first touched joystick
                        if(event.getAction() == MotionEvent.ACTION_DOWN){
                            viewModel.setInputFromWebClient(false);
                            viewModel.setLastUsedId(id);
                            Log.d(TAG, "move");
                            // When released joystick
                        } else if(event.getAction() == MotionEvent.ACTION_UP) {
                            Log.d(TAG, "cancel");
                            stallDetected= false;
                            hideBorder();
                            joystick.setBorderColor(ContextCompat.getColor(getContext(), R.color.joystick_border));
                            joystick.setButtonColor(ContextCompat.getColor(getContext(), R.color.joystick_button));
                        }
                        return false;
                    });

                    /**
                     * Code executed when you move the joystick
                     */
                    // set a onMoveListener for the functionality when the joystick is used
                    joystick.setOnMoveListener((angle, strength) -> {
                        // use a thread, because otherwise the joystick is lagging because of the stall detection
                        Thread joystickInput = new Thread() {
                            public void run() {
                                viewModel.sendControlInput(id, angle, strength);
                                Log.d(TAG, "Joystick angle;strength: " + angle + ";" + strength);
                            }
                         };
                        joystickInput.start();
                        // change the color when a stall is detected
                        if (!stallDetected) {
                            hideBorder();
                            joystick.setBorderColor(ContextCompat.getColor(getContext(), R.color.joystick_border));
                            joystick.setButtonColor(ContextCompat.getColor(getContext(), R.color.joystick_button));
                        } else {
                            // show a border when a stall is detected
                            showBorder();
                            joystick.setBorderColor(ContextCompat.getColor(getContext(), R.color.border));
                            joystick.setButtonColor(ContextCompat.getColor(getContext(), R.color.joystick_border));
                        }
                    });

                    // save the unique id of this element
                    controlElements[i] = joystick.getId();
                    break;
                case "slider":
                    // create the slider
                    SeekBar slider = new SeekBar(getContext());
                    sliderList.add(slider);
                    slider.setId(View.generateViewId());
                    slider.setProgress(50);
                    slider.setThumb(ContextCompat.getDrawable(getContext(), R.drawable.slider_thumb));
                    slider.setProgressDrawable(ContextCompat.getDrawable(getContext(), R.drawable.slider_progressbar));
                    slider.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.transparent));

                    // add the slider to the constraintLayout
                    // set constraintHeight and constraintWidth (this depends on the orientation)
                    // update the constraintSet based on the orientation in dependency of control elements created before
                    constraintLayout.addView(slider);
                    set.constrainHeight(slider.getId(), (int) getResources().getDimension(R.dimen.control_element_size));
                    if ((getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)) {
                        updateConstraintSet(i, slider.getId(), controlElements, set);
                        set.constrainWidth(slider.getId(), (int) getResources().getDimension(R.dimen.control_element_size));
                    } else {
                        updateConstraintSetLandscape(i, slider.getId(), controlElements, set);
                        set.constrainWidth(slider.getId(), (int) getResources().getDimension(R.dimen.control_element_size_landscape));
                    }
                    // apply all changes to the constraintLayout
                    set.applyTo(constraintLayout);

                    // set a onSeekBarChangeListener for the functionality when the slider is used
                    /**
                     * Code executed when you move the Slider progressbar
                     */
                    slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        }

                        /**
                         * Code exectued when you first touch a slider (sets the usedId parameter)
                         * @param seekBar the slider you touched
                         */
                        @Override
                        public void onStartTrackingTouch(SeekBar seekBar) {
                            viewModel.setInputFromWebClient(false);
                            viewModel.setLastUsedId(id);
           //                 showBorder();
            //                slider.getProgressDrawable().setTint(ContextCompat.getColor(getContext(), R.color.border));
            //                slider.getThumb().setTint(ContextCompat.getColor(getContext(), R.color.joystick_border));
                        }

                        /**
                         * Code executed when you stop touching a slider
                         * @param seekBar the slider you stopped touching
                         */
                        @Override
                        public void onStopTrackingTouch(SeekBar seekBar) {
                            // use a thread, because otherwise the slider is lagging because of the stall detection
                            Thread sliderProgressReset = new Thread() {
                                public void run() {
                                    viewModel.sendControlInput(id, 50);
                                }
                            };
                            sliderProgressReset.start();
                            seekBar.setProgress(50);
                            // hide the border when no stall is detected
                            hideBorder();
                            // change the color when no stall is detected
                            slider.getProgressDrawable().setTint(ContextCompat.getColor(getContext(), R.color.joystick_border));
                            slider.getThumb().setTint(ContextCompat.getColor(getContext(), R.color.border));
                            stallDetected = false;
                        }
                    });

                    // set a onTouchListener for the functionality when the slider is moved
                    slider.setOnTouchListener((v, event) -> {
                        if (event.getAction() == MotionEvent.ACTION_MOVE) {
                            Log.d(TAG, "MOVE MOVE MOVE");
                            // use a thread, because otherwise the slider is lagging because of the stall detection
                            Thread sliderInput = new Thread() {
                                public void run() {
                                        viewModel.sendControlInput(id, slider.getProgress());
                                        Log.d(TAG, "Slider deflection: " + slider.getProgress());
                                }
                            };
                            sliderInput.start();
                            if(stallDetected){
                                // show a border when a stall is detected
                                showBorder();
                                slider.getProgressDrawable().setTint(ContextCompat.getColor(getContext(), R.color.border));
                                slider.getThumb().setTint(ContextCompat.getColor(getContext(), R.color.joystick_border));
                            } else {
                                // hide the border when no stall is detected
                                hideBorder();
                                slider.getProgressDrawable().setTint(ContextCompat.getColor(getContext(), R.color.joystick_border));
                                slider.getThumb().setTint(ContextCompat.getColor(getContext(), R.color.border));
                            }
                        }
                        return false;
                    });

                    // save the unique id of this  element
                    controlElements[i] = slider.getId();
                    break;
                case "button":
                    // create the button
                    ContextThemeWrapper newContext = new ContextThemeWrapper(getContext(), R.style.button_control_element);
                    android.widget.Button button = new android.widget.Button(newContext);
                    button.setId(View.generateViewId());
                    button.setText(R.string.button_fire);
                    button.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.standard_button));

                    // add the button to the constraintLayout
                    // set constraintHeight and constraintWidth (this depends on the orientation)
                    // update the constraintSet based on the orientation in dependency of control elements created before
                    constraintLayout.addView(button);
                    set.constrainHeight(button.getId(), (int) getResources().getDimension(R.dimen.standard_button_height));
                    if ((getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)) {
                        updateConstraintSet(i, button.getId(), controlElements, set);
                        set.constrainWidth(button.getId(), (int) getResources().getDimension(R.dimen.control_element_size));
                    } else {
                        updateConstraintSetLandscape(i, button.getId(), controlElements, set);
                        set.constrainWidth(button.getId(), (int) getResources().getDimension(R.dimen.control_element_size_landscape));
                    }
                    // apply all changes to the constraintLayout
                    set.applyTo(constraintLayout);

                    /**
                     * Code executed when you touch a control element button
                     */
                    button.setOnTouchListener((v, event) -> {
                        switch(event.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                                viewModel.setInputFromWebClient(false);
                                viewModel.setLastUsedId(id);
                                // use a thread, because otherwise the button is lagging because of the stall detection
                                Thread buttonInput = new Thread(){
                                    public void run(){
                                        viewModel.sendControlInput(id, 1);
                                        Log.d(TAG, "Button activity: " + 1);
                                    }
                                };
                                buttonInput.start();
                                if(stallDetected) {
                                    showBorder();
                                    button.getBackground().setTint(ContextCompat.getColor(getContext(), R.color.border));
                                } else {
                                    hideBorder();
                                    button.getBackground().setTint(ContextCompat.getColor(getContext(), R.color.joystick_border));
                                }
                                return false;
                            case MotionEvent.ACTION_UP:
                            case MotionEvent.ACTION_CANCEL:
                                Log.d(TAG, "Button activity: " + 0);
                                // hide the border when no stall is detected
                                hideBorder();
                                // change the color when no stall is detected
                                button.getBackground().setTint(ContextCompat.getColor(getContext(), R.color.joystick_border));
                                stallDetected = false;
                                return false;
                        }
                        return true;
                    });

                    // save the unique id of this element
                    controlElements[i] = button.getId();
                    break;
            }
        }
        //rotate all sliders so that they are vertical
        for (SeekBar slider: sliderList) {
            slider.setRotation(270);
        }
    }

    /**
     * sets the Constraints for each of the created control elements in portrait mode
     * @param i index of the created element
     * @param controlElementid id of the created element we want to give constraints
     * @param controlElements Array of all currently created control elements
     * @param set the current ConstraintSet we are using
     */
    public void updateConstraintSet(int i, int controlElementid, int[] controlElements, ConstraintSet set){
        switch(i) {
            case 0:
                set.connect(controlElementid, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, (int) getResources().getDimension(R.dimen.margin_top));
                set.connect(controlElementid, ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT, (int) getResources().getDimension(R.dimen.margin_side));
                set.connect(controlElementid, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, (int) getResources().getDimension(R.dimen.margin_bottom));
                break;
            case 1:
                set.connect(controlElementid, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, (int) getResources().getDimension(R.dimen.margin_top));
                set.connect(controlElementid, ConstraintSet.RIGHT, controlElements[0], ConstraintSet.LEFT, (int) getResources().getDimension(R.dimen.margin_horizontal_small));
                set.connect(controlElementid, ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT, (int) getResources().getDimension(R.dimen.margin_side));
                set.connect(controlElementid, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, (int) getResources().getDimension(R.dimen.margin_bottom));
                set.connect(controlElements[0], ConstraintSet.LEFT, controlElementid, ConstraintSet.RIGHT, (int) getResources().getDimension(R.dimen.margin_horizontal_small));
                break;
            case 2:
                set.connect(controlElementid, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, (int) getResources().getDimension(R.dimen.margin_top));
                set.connect(controlElementid, ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT, (int) getResources().getDimension(R.dimen.margin_side));
                set.connect(controlElementid, ConstraintSet.BOTTOM, controlElements[0], ConstraintSet.TOP, (int) getResources().getDimension(R.dimen.margin_bottom));
                break;
            case 3:
                set.connect(controlElementid, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, (int) getResources().getDimension(R.dimen.margin_top));
                set.connect(controlElementid, ConstraintSet.RIGHT, controlElements[2], ConstraintSet.LEFT, (int) getResources().getDimension(R.dimen.margin_horizontal_small));
                set.connect(controlElementid, ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT, (int) getResources().getDimension(R.dimen.margin_side));
                set.connect(controlElementid, ConstraintSet.BOTTOM, controlElements[1], ConstraintSet.TOP, (int) getResources().getDimension(R.dimen.margin_bottom));
                set.connect(controlElements[2], ConstraintSet.LEFT, controlElementid, ConstraintSet.RIGHT,(int) getResources().getDimension(R.dimen.margin_horizontal_small));
                break;
        }
    }

    /**
     * sets the Constraints for each of the created control elements in landscape mode
     * @param i index of the created element
     * @param controlElementid id of the created element we want to give constraints
     * @param controlElements Array of all currently created control elements
     * @param set the current ConstraintSet we are using
     */
    public void updateConstraintSetLandscape(int i, int controlElementid, int[] controlElements, ConstraintSet set){
        switch(i) {
            case 0:
                set.connect(controlElementid, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, (int) getResources().getDimension(R.dimen.margin_top));
                set.connect(controlElementid, ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT, (int) getResources().getDimension(R.dimen.margin_side));
                set.connect(controlElementid, ConstraintSet.BOTTOM, R.id.text_control_question, ConstraintSet.TOP, (int) getResources().getDimension(R.dimen.margin_bottom));
                break;
            case 1:
                set.connect(controlElementid, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, (int) getResources().getDimension(R.dimen.margin_top));
                set.connect(controlElementid, ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT, (int) getResources().getDimension(R.dimen.margin_side));
                set.connect(controlElementid, ConstraintSet.BOTTOM, R.id.text_control_question, ConstraintSet.TOP, (int) getResources().getDimension(R.dimen.margin_bottom));
                break;
            case 2:
                set.connect(controlElementid, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, (int) getResources().getDimension(R.dimen.margin_top));
                set.connect(controlElementid, ConstraintSet.RIGHT, controlElements[0], ConstraintSet.LEFT, (int) getResources().getDimension(R.dimen.margin_horizontal_small));
                set.connect(controlElementid, ConstraintSet.LEFT, controlElements[1], ConstraintSet.RIGHT, (int) getResources().getDimension(R.dimen.margin_horizontal_small));
                set.connect(controlElementid, ConstraintSet.BOTTOM, R.id.text_control_question, ConstraintSet.TOP, (int) getResources().getDimension(R.dimen.margin_bottom));
                break;
            case 3:
                set.connect(controlElementid, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, (int) getResources().getDimension(R.dimen.margin_top));
                set.connect(controlElementid, ConstraintSet.RIGHT, controlElements[2], ConstraintSet.LEFT, (int) getResources().getDimension(R.dimen.margin_horizontal_small));
                set.connect(controlElementid, ConstraintSet.LEFT, controlElements[1], ConstraintSet.RIGHT, (int) getResources().getDimension(R.dimen.margin_horizontal_small));
                set.connect(controlElementid, ConstraintSet.BOTTOM, R.id.text_control_question, ConstraintSet.TOP, (int) getResources().getDimension(R.dimen.margin_bottom));
                set.connect(controlElements[0], ConstraintSet.LEFT, controlElements[2], ConstraintSet.RIGHT, (int) getResources().getDimension(R.dimen.margin_horizontal_small));
                set.connect(controlElements[1], ConstraintSet.RIGHT, controlElementid, ConstraintSet.LEFT, (int) getResources().getDimension(R.dimen.margin_horizontal_small));
                set.connect(controlElements[2], ConstraintSet.LEFT, controlElementid, ConstraintSet.RIGHT, (int) getResources().getDimension(R.dimen.margin_horizontal_small));
                break;
        }
    }
}