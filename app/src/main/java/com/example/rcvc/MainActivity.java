package com.example.rcvc;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.jitsi.meet.sdk.JitsiMeetActivity;

import java.util.ArrayList;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    // zum Testen von nicht implementierten Funktionen
    private boolean btIsClicked = false;
    //Declare all the xml objects
    private Button buttonBluetooth;
    private Button buttonOpenRoom;
    private Button buttonShareLink;
    private Button buttonSwitchToRoom;
    private Button buttonMoveForward;
    private Button buttonMoveBackward;
    private Button buttonTurnRight;
    private Button buttonTurnLeft;
    private TextView textviewConnectionStatus;
    private ListView listviewDevices;

    private JitsiRoom room;

    private static final String TAG = "MainActivity";

    BluetoothConnectionService mBluetoothConnection;

    // Bluetooth adapter of our device
    private BluetoothAdapter btAdapter;
    // Device we want to connect with
    private BluetoothDevice selectedDevice;
    // The UUIDs of the device we want to connect with
    private ParcelUuid[] mDeviceUUIDs;
    // All paired devices
    private ArrayList<BluetoothDevice> pairedDevices = new ArrayList<>();
    // Connected Robot
    private RobotController robot;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // get all buttons
        buttonBluetooth = findViewById(R.id.button_bluetooth);
        buttonOpenRoom = findViewById(R.id.button_open_room);
        buttonShareLink = findViewById(R.id.button_share_link);
        buttonSwitchToRoom = findViewById(R.id.button_switch_to_room);
        textviewConnectionStatus = findViewById(R.id.connection_status);
        listviewDevices = findViewById(R.id.list_paired_devices);
        buttonMoveForward = findViewById(R.id.button_forward);
        buttonMoveBackward = findViewById(R.id.button_backward);
        buttonTurnRight = findViewById(R.id.button_right);
        buttonTurnLeft = findViewById(R.id.button_left);

        btAdapter = BluetoothAdapter.getDefaultAdapter();

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(receiverActionStateChanged, filter);

        // Custom IntentFilter for catching Intent from ConnectedThread
        IntentFilter filter2 = new IntentFilter(getString(R.string.action_check_connection));
        registerReceiver(receiverConnection, filter2);

        buttonMoveForward.setOnTouchListener((v, event) -> {
            if(event.getAction() == MotionEvent.ACTION_DOWN) {
                //when button is being pressed down, direct command for moving forward is send to ev3
                robot.sendCommands(RobotController.FORWARD);
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                //when button is being released, direct command for stopping is send to ev3
                robot.sendCommands(RobotController.STOP);
            }

            return true;
        });

        buttonMoveBackward.setOnTouchListener((v, event) -> {
            if(event.getAction() == MotionEvent.ACTION_DOWN) {
                //when button is being pressed down, direct command for moving backward is send to ev3
                robot.sendCommands(RobotController.BACKWARD);
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                //when button is being released, direct command for stopping is send to ev3
                robot.sendCommands(RobotController.STOP);
            }

            return true;
        });

        buttonTurnRight.setOnTouchListener((v, event) -> {
            if(event.getAction() == MotionEvent.ACTION_DOWN) {
                //when button is being pressed down, direct commands for turning to the right are send to ev3
                robot.sendCommands(RobotController.TURN_RIGHT);
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                //when button is being released, direct command for stopping is send to ev3
                robot.sendCommands(RobotController.STOP);
            }

            return true;
        });

        buttonTurnLeft.setOnTouchListener((v, event) -> {
            if(event.getAction() == MotionEvent.ACTION_DOWN) {
                //when button is being pressed down, direct commands for turning to the left are send to ev3
                robot.sendCommands(RobotController.TURN_LEFT);
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                //when button is being released, direct command for stopping is send to ev3
                robot.sendCommands(RobotController.STOP);
            }

            return true;
        });

        // Try to start bluetooth connection with paired device that was clicked
        listviewDevices.setOnItemClickListener((parent, view, position, id) -> {
            selectedDevice = pairedDevices.get(position);
            mDeviceUUIDs = selectedDevice.getUuids();
            mBluetoothConnection = new BluetoothConnectionService(MainActivity.this);
            startBTConnection(selectedDevice, mDeviceUUIDs);
        });
    }

    /**
     * Starts a connection between our device and the device we want to connect with
     * @param device the device to connect with
     * @param uuid the uuids of the device
     */
    public void startBTConnection(BluetoothDevice device, ParcelUuid[] uuid) {
        Log.d(TAG, "startBTConnection: Initializing RFCOM Bluetooth Connection.");
        mBluetoothConnection.startClient(device,mDeviceUUIDs);
        robot = new RobotController(mBluetoothConnection);
    }

    /**
     * Checks if the connection is valid and changes variables and buttons on screen accordingly
     */
    public void onConnection() {
        robot.sendCommands(RobotController.STOP);
        switch(mBluetoothConnection.getConnectionStatus()) {
            case 1: // Connection was successful
                textviewConnectionStatus.setText(String.format(getResources().getString(R.string.connection_status_true), selectedDevice.getName()));
                buttonBluetooth.setText(getString(R.string.button_bluetooth_connected));
                btIsClicked = true;
                buttonOpenRoom.setEnabled(true);
                listviewDevices.setVisibility(View.INVISIBLE);
                setVisibilityControlButtons(true);
                break;
            case 2: // Could not connect
                showToast(getString(R.string.connection_init_error));
                resetConnection();
                listviewDevices.setVisibility(View.INVISIBLE);
                break;
            case 3: // Connection lost
                showToast(getString(R.string.connection_lost));
                resetConnection();
                break;
            default: // connectionStatus was not set yet
                break;
        }
    }

    /**
     * On destroy, all receivers will be unregistered
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiverActionStateChanged);
        unregisterReceiver(receiverConnection);
    }

    /**
     * Create a BroadcastReceiver that catches Intent in ConnectedThread and runs onConnection
     */
    private BroadcastReceiver receiverConnection = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            onConnection();
        }
    };

    /**
     * Create a BroadcastReceiver for ACTION_STATE_CHANGED changes
     * Whenever Bluetooth is turned off while we are in a connection, reset everything
     */
    private BroadcastReceiver receiverActionStateChanged = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                // Bluetooth Status has been turned off
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                if(state == BluetoothAdapter.STATE_OFF || state == BluetoothAdapter.STATE_TURNING_OFF){
                    resetConnection();
                }
            }
        }
    };

    /**
     * @param v
     * If bluetooth is disabled, this button will enable it, if bluetooth is is enabled and this button is clicked, it will show all paired devices.
     * If this button is clicked while we have a connection, it will reset the connection
     */
    public void onClickBluetooth(View v) {
        if(btIsClicked){
            resetConnection();
        } else {
            if (!btAdapter.isEnabled()) {
                Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivity(enableBTIntent);
            }
            if (btAdapter.isEnabled()) {

                ArrayList<String> names = getPairedDevices();
                ArrayAdapter<String> listAdapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, names);
                listviewDevices.setAdapter(listAdapter);
                listviewDevices.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * @param v On click of the openRoom button we create a jitsi room with some options and enable the shareLink and
     *          switchToRoom button.
     */
    public void onClickOpenRoom(View v) {
        if (room == null) {
            room = new JitsiRoom();
        }

        setEnableLinkAndRoom(true);
        showToast(getString(R.string.toast_room_opened));
    }

    /**
     * @param v The link for the jitsi room gets copied to the clipboard
     */
    public void onClickShareLink(View v) {
        if (room == null) {
            showToast(getString(R.string.toast_no_open_room));
        } else {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Jitsi Room Link", room.url);
            clipboard.setPrimaryClip(clip);

            showToast(getString(R.string.toast_link_copied));
        }
    }

    /**
     * @param v Opens the jitsi room with the options created before and switches to a new window with the jitsi room
     */
    public void onClickSwitchToRoom(View v) {
        JitsiMeetActivity.launch(this, room.options);
    }

    /**
     * @param message The message to pop up at the bottom of the screen
     */
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * @param enabled The boolean to decide if we want to enable or disable the buttons
     *                Enables or disables the shareLink and switchToRoom button since they are only used together.
     */
    private void setEnableLinkAndRoom(boolean enabled) {
        buttonShareLink.setEnabled(enabled);
        buttonSwitchToRoom.setEnabled(enabled);
    }

    public ArrayList<String> getPairedDevices(){
        Set<BluetoothDevice> devices = btAdapter.getBondedDevices();
        ArrayList<String> names = new ArrayList<>();
        if (devices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : devices) {
                pairedDevices.add(device);
                names.add(device.getName());
            }
        } else {
            Bundle bundle = new Bundle();
            // first put id of error message in bundle using defined key
            bundle.putInt(ErrorDialogFragment.MSG_KEY, R.string.error_no_paired_devices);
            ErrorDialogFragment error = new ErrorDialogFragment();
            // then pass bundle to dialog and show
            error.setArguments(bundle);
            error.show(this.getSupportFragmentManager(), TAG);

            // if there are no paired devices, open bluetooth settings
            Intent intentOpenBluetoothSettings = new Intent();
            intentOpenBluetoothSettings.setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
            startActivity(intentOpenBluetoothSettings);
        }
        return names;
    }

    /**
     * reset connection and change variables when we disconnect (via button or bluetooth)
     */
    public void resetConnection(){
        robot.sendCommands(RobotController.STOP);
        btIsClicked = false;
        buttonBluetooth.setText(getString(R.string.button_bluetooth_disconnected));
        buttonOpenRoom.setEnabled(false);
        setEnableLinkAndRoom(false);
        textviewConnectionStatus.setText(getString(R.string.connection_status_false));
        setVisibilityControlButtons(false);
        mBluetoothConnection.cancel();
        selectedDevice = null;
        pairedDevices = new ArrayList<>();
        mDeviceUUIDs = null;
    }

    /**
     * Set the visibility of the control buttons according to the given param
     * @param vis the visibility that the control buttons will the get set to
     */
    public void setVisibilityControlButtons(boolean vis){
        if (vis) {
            buttonMoveForward.setVisibility(View.VISIBLE);
            buttonMoveBackward.setVisibility(View.VISIBLE);
            buttonTurnRight.setVisibility(View.VISIBLE);
            buttonTurnLeft.setVisibility(View.VISIBLE);
        } else {
            buttonMoveForward.setVisibility(View.INVISIBLE);
            buttonMoveBackward.setVisibility(View.INVISIBLE);
            buttonTurnRight.setVisibility(View.INVISIBLE);
            buttonTurnLeft.setVisibility(View.INVISIBLE);
        }
    }
}