package com.vmks.bluetootharduinolock;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.scottyab.aescrypt.AESCrypt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class LockControl extends AppCompatActivity {

    @BindView(R.id.usernameTextView)
    EditText usernameEdtText;

    private static final String TAG = LockControl.class.getSimpleName();
    private String deviceAddress;

    private Command lastCommand = Command.NONE;

    private BluetoothSocket bluetoothSocket;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice bluetoothDevice;
    private OutputStream outputStream;
    private InputStream inputStream;
    private Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    int counter;
    volatile boolean stopWorker;

    private boolean isBluetoothConnected;
    //TODO: fill the uuid
    private static final UUID bluetoothUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private List<String> messages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock_controll);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        String address = getIntent().getStringExtra(MainActivity.EXTRA_ADDRESS);
        ButterKnife.bind(this);

        messages = new ArrayList<>();
        deviceAddress = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        new EstablishBluetoothConnection().execute(address);
    }

    String encr = "";

    @OnClick(R.id.loginBtn)
    public void loginClick() {
        String msg = usernameEdtText.getText().toString();
        sendMessage(msg);
//        myLabel.setText("Data Sent");
    }

    @OnClick(R.id.lockBtn)
    public void lockBtn() {
        sendMessage("l:" + deviceAddress + "/");
        lastCommand = Command.LOCK;
    }

    private void sendMessage(String msg) {
        if (bluetoothSocket != null) {
            msg += "\n";
            try {
                outputStream.write(msg.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(this, "Error with socket", Toast.LENGTH_SHORT).show();
        }
    }

    @OnClick(R.id.registerBtn)
    public void registerClick() {
        startInfoDialog();
    }

    private void startInfoDialog() {
        new MaterialDialog.Builder(this)
                .title("Registration")
                .content("Press the button on the Arduino for pairing and then press \"Next\". (You will have 5 seconds)")
                .positiveText("Next")
                .negativeText("Cancel")
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        sendRegistrationToArduino();
                    }

                })
                .show();
    }

    private void sendRegistrationToArduino() {
        sendMessage(deviceAddress + "/");
        lastCommand = Command.REGISTRATION;
    }

    private void decrypt(String message, String key) {
//        String password = "password";
//        String encryptedMsg = "2B22cS3UC5s35WBihLBo8w==";
        try {
            String messageAfterDecrypt = AESCrypt.decrypt(key, message);
            Log.d(TAG, "Decrypted: " + messageAfterDecrypt);
        } catch (GeneralSecurityException e) {
            //handle error - could be due to incorrect password or tampered encryptedMsg
            Log.d(TAG, "Decrypted: failed");
        }
    }

    private void listenForData() {
        final Handler handler = new Handler();
        final byte delimiter = 10;

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted() && !stopWorker) {
                    try {
                        int bytesAvailable = inputStream.available();
                        if (bytesAvailable > 0) {
                            byte[] packedBytes = new byte[bytesAvailable];
                            inputStream.read(packedBytes);
                            for (int i = 0; i < bytesAvailable; i++) {
                                byte b = packedBytes[i];
                                if (b == delimiter) {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "UTF-8");

                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            Log.d(TAG, "INCOMING: " + data);
                                            messages.add(data);
                                            processIncome(data);
                                            readBuffer = new byte[1024];
                                            readBufferPosition = 0;
                                        }
                                    });
                                } else {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        stopWorker = true;
                    }
                }
            }
        });
        workerThread.start();
    }

    private void processIncome(String data) {
        switch (lastCommand) {
            case LOCK:
                String splitData[] = data.split("\\.");
                for (String s : splitData) {
                    Log.d(TAG, "Split: " + s);
                }

                if (splitData.length > 2)
                    try {
                        int period = Integer.parseInt(splitData[1]);
                        Log.d(TAG, "Period: " + period);
                        Log.d(TAG, "Pass: " + PasswordManager.passwords[period]);

                        String pass = PasswordManager.passwords[period];

                        sendMessage("o:" + pass + "/");
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Try logging again", Toast.LENGTH_SHORT).show();
                    }
                break;
        }
    }

    private class EstablishBluetoothConnection extends AsyncTask<String, Void, Void> {

        private MaterialDialog dialog;

        @Override
        protected void onPreExecute() {
            dialog = new MaterialDialog.Builder(LockControl.this)
                    .title(R.string.connecting)
                    .content("Please wait.")
                    .progress(true, 0)
                    .show();
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(String... params) {
            Log.i(TAG, "params: " + Arrays.toString(params));

            try {
                bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                bluetoothDevice = bluetoothAdapter.getRemoteDevice(String.valueOf(params[0]));
                bluetoothSocket = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(bluetoothUUID);
                bluetoothAdapter.cancelDiscovery();
                bluetoothSocket.connect();

                outputStream = bluetoothSocket.getOutputStream();
                inputStream = bluetoothSocket.getInputStream();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        listenForData();
                    }
                });
            } catch (IOException e) {
                dialog.dismiss();
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            dialog.dismiss();
        }
    }

}
