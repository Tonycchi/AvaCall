package com.example.rcvc;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.ParcelUuid;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;

/**
 * Created by User on 12/21/2016.
 */

@SuppressLint("LogNotTimber")
public class BluetoothConnectionService {
    private static final String TAG = "BluetoothConnectionServ";

    private static final String APP_NAME = "AppName";

    private static final UUID MY_UUID =
            UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

    private int connectionStatus = 0;

    private BluetoothAdapter bluetoothAdapter;
    Context context;

    private AcceptThread acceptThread;

    private ConnectThread connectThread;
    private BluetoothDevice bluetoothDevice;
    ProgressDialog progressDialog;

    private ConnectedThread connectedThread;

    public BluetoothConnectionService(Context context) {
        this.context = context;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        start();
    }

    /**
     * @return the current connection status
     * 0 is not tested, 1 is connected, 2 is could not connect, 3 is connection lost
     */
    public int getConnectionStatus() {
        return connectionStatus;
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread {

        // The local server socket
        private final BluetoothServerSocket SERVER_SOCKET;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;

            // Create a new listening server socket
            try {
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, MY_UUID);

                Log.d(TAG, "AcceptThread: Setting up Server using: " + MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "AcceptThread: IOException: " + e.getMessage());
            }

            SERVER_SOCKET = tmp;
        }

        public void run() {
            Log.d(TAG, "run: AcceptThread Running.");

            BluetoothSocket socket = null;

            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                Log.d(TAG, "run: RFCOM server socket start.....");

                socket = SERVER_SOCKET.accept();

                Log.d(TAG, "run: RFCOM server socket accepted connection.");

            } catch (IOException e) {
                Log.e(TAG, "AcceptThread: IOException: " + e.getMessage());
            }

            if (socket != null) {
                connected(socket);
            }

            Log.i(TAG, "END mAcceptThread ");
        }

        public void cancel() {
            Log.d(TAG, "cancel: Canceling AcceptThread.");
            try {
                SERVER_SOCKET.close();
            } catch (IOException e) {
                Log.e(TAG, "cancel: Close of AcceptThread ServerSocket failed. " + e.getMessage());
            }
        }

    }

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private BluetoothSocket bluetoothSocket;
        ParcelUuid[] deviceUUIDs;

        public ConnectThread(BluetoothDevice device, ParcelUuid[] deviceUUIDs) {
            Log.d(TAG, "ConnectThread: started.");
            bluetoothDevice = device;
            this.deviceUUIDs = deviceUUIDs;
        }

        public void run() {
            BluetoothSocket tmp = null;
            Log.i(TAG, "RUN mConnectThread ");

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                for (ParcelUuid mDeviceUUID : deviceUUIDs) {
                    Log.d(TAG, "ConnectThread: Trying to create RfcommSocket using UUID: "
                            + mDeviceUUID.getUuid());
                    tmp = bluetoothDevice.createRfcommSocketToServiceRecord(mDeviceUUID.getUuid());
                }
            } catch (IOException e) {
                Log.e(TAG, "ConnectThread: Could not create RfcommSocket " + e.getMessage());
            }

            bluetoothSocket = tmp;

            // Always cancel discovery because it will slow down a connection
            bluetoothAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket

            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                bluetoothSocket.connect();

                Log.d(TAG, "run: ConnectThread connected.");
            } catch (IOException e) {
                // Close the socket
                try {
                    bluetoothSocket.close();
                    Log.d(TAG, "run: Closed Socket.");
                } catch (IOException e1) {
                    Log.e(TAG, "mConnectThread: run: Unable to close connection in socket " + e1.getMessage());
                }
                Log.d(TAG, "run: ConnectThread: Could not connect to UUID: " + MY_UUID);
            }

            connected(bluetoothSocket);
        }

        public void cancel() {
            try {
                Log.d(TAG, "cancel: Closing Client Socket.");
                bluetoothSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "cancel: close() of mmSocket in Connectthread failed. " + e.getMessage());
            }
        }
    }


    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     */
    public synchronized void start() {
        Log.d(TAG, "start");

        // Cancel any thread attempting to make a connection
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }
        if (acceptThread == null) {
            acceptThread = new AcceptThread();
            acceptThread.start();
        }
    }

    /**
     * AcceptThread starts and sits waiting for a connection.
     * Then ConnectThread starts and attempts to make a connection with the other devices AcceptThread.
     **/

    public void startClient(BluetoothDevice device, ParcelUuid[] deviceUUIDs) {
        Log.d(TAG, "startClient: Started.");

        //initprogress dialog
        progressDialog = ProgressDialog.show(context, "Connecting Bluetooth"
                , "Please Wait...", true);

        connectThread = new ConnectThread(device, deviceUUIDs);
        connectThread.start();
    }

    /**
     * Finally the ConnectedThread which is responsible for maintaining the BTConnection, Sending the data, and
     * receiving incoming data through input/output streams respectively.
     **/
    private class ConnectedThread extends Thread {
        private final BluetoothSocket bluetoothSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "ConnectedThread: Starting.");

            bluetoothSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            //dismiss the progressdialog when connection is established
            try {
                progressDialog.dismiss();
            } catch (NullPointerException e) {
                e.printStackTrace();
            }


            try {
                tmpIn = bluetoothSocket.getInputStream();
                tmpOut = bluetoothSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            inputStream = tmpIn;
            outputStream = tmpOut;
            // check connection with broadcast
            sendConnectionStatusBroadcast();
            Log.d(TAG, "Connected Thread gestartet");
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream

            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                // Read from the InputStream
                try {
                    bytes = inputStream.read(buffer);
                    String incomingMessage = new String(buffer, 0, bytes);
                    Log.d(TAG, "InputStream: " + incomingMessage);

                    Intent incomingMessageIntent = new Intent("incomingMessage");
                    incomingMessageIntent.putExtra("theMessage", incomingMessage);

                } catch (IOException e) {
                    // in case of exception check connection with broadcast
                    sendConnectionStatusBroadcast();
                    Log.e(TAG, "write: Error reading Input Stream. " + e.getMessage());
                    break;
                }
            }
        }

        /**
         * this method gets called from main activity to send data to the remote device
         * this method also gets called once at the start to make sure the connection was successful
         *
         * @param bytes the bytes to be send
         */
        public void write(byte[] bytes) {
            String text = new String(bytes, Charset.defaultCharset());
            Log.d(TAG, "write: Writing to outputstream: " + text);
            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                // could not connect, so connection status gets set to 2
                if (connectionStatus == 0) {
                    connectionStatus = 2;
                }
                // connection got lost, so status gets set to 3
                if (connectionStatus == 1) {
                    connectionStatus = 3;
                }
                Log.e(TAG, "write: Error writing to output stream. " + e.getMessage());
            }
            // if connection status is still 0 at this point,
            // the connection was successful and it gets set to 1
            if (connectionStatus == 0) {
                connectionStatus = 1;
            }
        }

        /**
         * This method gets called from main activity to shutdown the connection
         */
        public void cancel() {
            try {
                bluetoothSocket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void connected(BluetoothSocket bluetoothSocket) {
        Log.d(TAG, "connected: Starting.");

        // Start the thread to manage the connection and perform transmissions
        connectedThread = new ConnectedThread(bluetoothSocket);
        connectedThread.start();
    }

    public void cancel() {
        Log.d(TAG, "cancel: Connection cancelled.");
        connectedThread.cancel();
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     *
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {
        // Synchronize a copy of the ConnectedThread
        Log.d(TAG, "write: Write Called.");
        //perform the write
        connectedThread.write(out);
    }

    private void sendConnectionStatusBroadcast() {
        Intent intent = new Intent(context.getString(R.string.action_check_connection));
        context.sendBroadcast(intent, "com.example.rcvc.permission.signature");
    }

}