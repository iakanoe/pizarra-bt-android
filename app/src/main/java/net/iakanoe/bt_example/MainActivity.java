package net.iakanoe.bt_example;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

import static android.content.ContentValues.TAG;

public class MainActivity extends AppCompatActivity {

    static final String NOMBRE = "BTRoby";
    static final int REQUEST_ENABLE_BT = 1;
    BluetoothAdapter btAdapter;
    Set<BluetoothDevice> devices;
    Handler mHandler;
    boolean connected = false;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!btAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
    }
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if(requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK) {
            devices.addAll(btAdapter.getBondedDevices());
            checkDevices();
        }
    }
    @Override protected void onDestroy() {
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }
    void checkDevices(){
        for(BluetoothDevice device : devices){
            if(device.getName().equals(NOMBRE)){
                btAdapter.cancelDiscovery();
                unregisterReceiver(mReceiver);
                connect(device);
                break;
            }
        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if(BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) devices.add((BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE));
            checkDevices();
        }
    };

    void connect(BluetoothDevice device){
        BluetoothConnection thr1 = new BluetoothConnection(device);
        thr1.execute();
    }



    public class BluetoothConnection extends AsyncTask<Void, String, Void>{
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private InputStream mmInStream;
        private OutputStream mmOutStream;
        private byte[] mmBuffer;
        static final int MESSAGE_READ = 1;
        static final int MESSAGE_WRITE = 2;
        static final int MESSAGE_TOAST = 3;


        BluetoothConnection(BluetoothDevice device) {
            BluetoothSocket tmp = null;
            mmDevice = device;
            try {
                tmp = device.createRfcommSocketToServiceRecord(mmDevice.getUuids()[0].getUuid());
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        @Override protected Void doInBackground(Void... params) {
            try {
                mmSocket.connect();
            } catch (IOException connectException) {
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                return null;
            }
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = mmSocket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating input stream", e);
            }
            try {
                tmpOut = mmSocket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating output stream", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
            mmBuffer = new byte[1024];
            int numBytes;

            while(true) {
                try {
                    numBytes = mmInStream.read(mmBuffer);
                    publishProgress(new String(mmBuffer));
                    InputStream a = new InputStream() {
                        @Override
                        public int read() throws IOException {
                            return 0;
                        }
                    };
                } catch (IOException e) {
                    Log.d(TAG, "Input stream was disconnected", e);
                    break;
                }
            }
            return null;
        }

        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
                Message writtenMsg = mHandler.obtainMessage(MESSAGE_WRITE, -1, -1, mmBuffer);
                writtenMsg.sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when sending data", e);
                Message writeErrorMsg = mHandler.obtainMessage(MESSAGE_TOAST);
                Bundle bundle = new Bundle();
                bundle.putString("toast", "Couldn't send data to the other device");
                writeErrorMsg.setData(bundle);
                mHandler.sendMessage(writeErrorMsg);
            }
        }

        @Override protected void onProgressUpdate(String... values) {

            super.onProgressUpdate(values);
        }

        @Override protected void onCancelled() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
            super.onCancelled();
        }
    }
}
