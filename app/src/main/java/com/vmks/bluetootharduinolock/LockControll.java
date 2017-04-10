package com.vmks.bluetootharduinolock;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class LockControll extends AppCompatActivity {


    private static final String TAG = LockControll.class.getSimpleName();
    private String deviceAddress = "";

    private String registrationUsername = "";
    private String registrationPassword = "";

    private BluetoothSocket bluetoothSocket;
    private BluetoothAdapter bluetoothAdapter;
    private boolean isBluetoothConnected;
    //TODO: fill the uuid
    private static final UUID bluetoothUUID = UUID.fromString("");


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

    @OnClick(R.id.loginBtn)
    public void loginClick() {

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


    private class EstablishBluetoothConnection extends AsyncTask<String, Void, Void> {

        private MaterialDialog dialog;
        @Override
        protected void onPreExecute() {
            dialog = new MaterialDialog.Builder(LockControll.this)
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
                BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(String.valueOf(params));
                bluetoothSocket = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(bluetoothUUID);
                bluetoothAdapter.cancelDiscovery();
                bluetoothSocket.connect();
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
