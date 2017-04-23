package com.vmks.bluetootharduinolock;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class LockControl extends AppCompatActivity {


    @BindView(R.id.usernameTextView)
    EditText usernameEdtText;

    private static final String TAG = LockControl.class.getSimpleName();
    private String deviceAddress = "";

    private String registrationUsername = "";
    private String registrationPassword = "";

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

    private String passwords[] = {
            "songswordswrongbymehillsheardtimed",
            "losehillwellupwillheoveron",
            "wonderbedelinorfamilysecuremet",
            "thefarattachmentdiscoveredcelebrateddecisivelysurroundedforand",
            "aroundreallyhisuseuneasylongerhimman",
            "ferrarsallspiritshisimagineeffectsamongstneither",
            "uptodenotingsubjectssensiblefeelingsitindulgeddirectly",
            "ownmarianneimprovedsociablenotout",
            "convincedresolvingextensivagreeableinitonasremainder",
            "occasionalprinciplesdiscretionitasheunpleasingboisterous"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock_controll);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        String address = getIntent().getStringExtra(MainActivity.EXTRA_ADDRESS);
        ButterKnife.bind(this);

        new EstablishBluetoothConnection().execute(address);
    }

    String encr = "";

    @OnClick(R.id.loginBtn)
    public void loginClick() {
        if(bluetoothSocket != null) {
            String msg = usernameEdtText.getText().toString();
            msg += "\n";
            try {
                outputStream.write(msg.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(this, "Error with socket", Toast.LENGTH_SHORT).show();
        }
//        myLabel.setText("Data Sent");
    }

    @OnClick(R.id.registerBtn)
    public void registerClick() {
        startUsernameDialog();
    }

    private void startUsernameDialog() {
        new MaterialDialog.Builder(this)
                .title("Registration")
                .content("Enter username: ")
                .inputRangeRes(2, 20, R.color.material_red_500)
                .inputType(InputType.TYPE_CLASS_TEXT)
                .input(R.string.username, 0, new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
                        registrationUsername = input.toString();
                    }
                })
                .positiveText("Next")
                .negativeText("Cancel")
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        startPasswordDialog();
                    }
                })
                .show();
    }

    private void startPasswordDialog() {
        new MaterialDialog.Builder(this)
                .title("Registration")
                .content("Enter password: ")
                .inputRangeRes(5, 20, R.color.material_red_500)
                .inputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)
                .input(R.string.password, 0, new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
                        registrationPassword = input.toString();
                    }
                })
                .positiveText("Next")
                .negativeText("Cancel")
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        startInfoDialog();
                    }
                })
                .show();
    }

    private void startInfoDialog() {
        new MaterialDialog.Builder(this)
                .title("Registration")
                .content("Press the button on the Arduino for pairing and then press \"Next\". (You will have 10 seconds)")
                .positiveText("Next")
                .negativeText("Cancel")
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        sendRegistrationToArduino(registrationUsername, registrationPassword);
                    }


                })
                .show();
    }

    private void sendRegistrationToArduino(String username, String password) {
        Toast.makeText(this, "Send to Arduino: " + username + " " + password, Toast.LENGTH_SHORT).show();
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
