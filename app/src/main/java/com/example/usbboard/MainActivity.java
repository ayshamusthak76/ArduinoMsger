package com.example.usbboard;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MainActivity extends Activity {
    public final String ACTION_USB_PERMISSION = "com.hariharan.arduinousb.USB_PERMISSION";
    Button startButton, sendButton, clearButton, stopButton;
    TextView textView;
    EditText editText;
    UsbManager usbManager;
    UsbDevice device;
    UsbSerialDevice serialPort;
    UsbDeviceConnection connection;

    UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() { //Defining a Callback which triggers whenever data is read.
        @Override
        public void onReceivedData(byte[] arg0) {
            String data = null;
            try {
                data = new String(arg0, "UTF-8");
                data.concat("/n");
                tvAppend(textView, data);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }


        }
    };
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { //Broadcast Receiver to automatically start and stop the Serial connection.
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
                boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if (granted) {
                    connection = usbManager.openDevice(device);
                    serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
                    if (serialPort != null) {
                        if (serialPort.open()) { //Set Serial Connection Parameters.
                            setUiEnabled(true);
                            serialPort.setBaudRate(9600);
                            serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                            serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                            serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                            serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                            serialPort.read(mCallback);
                            tvAppend(textView,"Serial Connection Opened!\n");

                        } else {
                            Log.d("SERIAL", "PORT NOT OPEN");
                        }
                    } else {
                        Log.d("SERIAL", "PORT IS NULL");
                    }
                } else {
                    Log.d("SERIAL", "PERM NOT GRANTED");
                }
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                onClickStart(startButton);
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                onClickStop(stopButton);

            }
        }

        ;
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        usbManager = (UsbManager) getSystemService(this.USB_SERVICE);
        startButton = (Button) findViewById(R.id.buttonStart);
        sendButton = (Button) findViewById(R.id.buttonSend);
        clearButton = (Button) findViewById(R.id.buttonClear);
        stopButton = (Button) findViewById(R.id.buttonStop);
        editText = (EditText) findViewById(R.id.editText);
        textView = (TextView) findViewById(R.id.textView);
        setUiEnabled(false);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(broadcastReceiver, filter);


    }

    public void setUiEnabled(boolean bool) {
        startButton.setEnabled(!bool);
        sendButton.setEnabled(bool);
        stopButton.setEnabled(bool);
        textView.setEnabled(bool);

    }

    public void onClickStart(View view) {

        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        if (!usbDevices.isEmpty()) {
            boolean keep = true;
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                device = entry.getValue();
                int deviceVID = device.getVendorId();
                if (deviceVID == 0x2341)//Arduino Vendor ID
                {
                    PendingIntent pi = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
                    usbManager.requestPermission(device, pi);
                    keep = false;
                } else {
                    connection = null;
                    device = null;
                }

                if (!keep)
                    break;
            }
        }


    }

    public void onClickSend(View view) {
        String string = editText.getText().toString();
        serialPort.write(string.getBytes());
        tvAppend(textView, "\nData Sent : " + string + "\n");

    }

    public void onClickStop(View view) {
        setUiEnabled(false);
        serialPort.close();
        tvAppend(textView,"\nSerial Connection Closed! \n");

    }

    public void onClickClear(View view) {
        textView.setText(" ");
    }

    private void tvAppend(TextView tv, CharSequence text) {
        final TextView ftv = tv;
        final CharSequence ftext = text;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ftv.append(ftext);
            }
        });
    }

}

//public class MainActivity extends AppCompatActivity {
//
//
//    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            switch (intent.getAction()) {
//                case UsbService.ACTION_USB_PERMISSION_GRANTED: // USB PERMISSION GRANTED
//                    Toast.makeText(context, "USB Ready", Toast.LENGTH_SHORT).show();
//                    break;
//                case UsbService.ACTION_USB_PERMISSION_NOT_GRANTED: // USB PERMISSION NOT GRANTED
//                    Toast.makeText(context, "USB Permission not granted", Toast.LENGTH_SHORT).show();
//                    break;
//                case UsbService.ACTION_NO_USB: // NO USB CONNECTED
//                    Toast.makeText(context, "No USB connected", Toast.LENGTH_SHORT).show();
//                    break;
//                case UsbService.ACTION_USB_DISCONNECTED: // USB DISCONNECTED
//                    Toast.makeText(context, "USB disconnected", Toast.LENGTH_SHORT).show();
//                    break;
//                case UsbService.ACTION_USB_NOT_SUPPORTED: // USB NOT SUPPORTED
//                    Toast.makeText(context, "USB device not supported", Toast.LENGTH_SHORT).show();
//                    break;
//            }
//        }
//    };
//    private UsbService usbService;
//    private TextView display;
//    private EditText editText;
//    private MyHandler mHandler;
//    private final ServiceConnection usbConnection = new ServiceConnection() {
//        @Override
//        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
//            usbService = ((UsbService.UsbBinder) arg1).getService();
//            usbService.setHandler(mHandler);
//        }
//
//        @Override
//        public void onServiceDisconnected(ComponentName arg0) {
//            usbService = null;
//        }
//    };
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//
//        mHandler = new MyHandler(this);
//
//        display = (TextView) findViewById(R.id.textView1);
//        editText = (EditText) findViewById(R.id.editText1);
//        Button sendButton = (Button) findViewById(R.id.buttonSend);
//        sendButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (!editText.getText().toString().equals("")) {
//                    String data = editText.getText().toString();
//                    if (usbService != null) { // if UsbService was correctly binded, Send data
//                        usbService.write(data.getBytes());
//                    }
//                }
//            }
//        });
//    }
//
//    @Override
//    public void onResume() {
//        super.onResume();
//        setFilters();  // Start listening notifications from UsbService
//        startService(UsbService.class, usbConnection, null); // Start UsbService(if it was not started before) and Bind it
//    }
//
//    @Override
//    public void onPause() {
//        super.onPause();
//        unregisterReceiver(mUsbReceiver);
//        unbindService(usbConnection);
//    }
//
//    private void startService(Class<?> service, ServiceConnection serviceConnection, Bundle extras) {
//        if (!UsbService.SERVICE_CONNECTED) {
//            Intent startService = new Intent(this, service);
//            if (extras != null && !extras.isEmpty()) {
//                Set<String> keys = extras.keySet();
//                for (String key : keys) {
//                    String extra = extras.getString(key);
//                    startService.putExtra(key, extra);
//                }
//            }
//            startService(startService);
//        }
//        Intent bindingIntent = new Intent(this, service);
//        bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
//    }
//
//    private void setFilters() {
//        IntentFilter filter = new IntentFilter();
//        filter.addAction(UsbService.ACTION_USB_PERMISSION_GRANTED);
//        filter.addAction(UsbService.ACTION_NO_USB);
//        filter.addAction(UsbService.ACTION_USB_DISCONNECTED);
//        filter.addAction(UsbService.ACTION_USB_NOT_SUPPORTED);
//        filter.addAction(UsbService.ACTION_USB_PERMISSION_NOT_GRANTED);
//        registerReceiver(mUsbReceiver, filter);
//    }
//
//    /*
//     * This handler will be passed to UsbService. Data received from serial port is displayed through this handler
//     */
//    private static class MyHandler extends Handler {
//        private final WeakReference<MainActivity> mActivity;
//
//        public MyHandler(MainActivity activity) {
//            mActivity = new WeakReference<>(activity);
//        }
//
//        @Override
//        public void handleMessage(Message msg) {
//            switch (msg.what) {
//                case UsbService.MESSAGE_FROM_SERIAL_PORT:
//                    String data = (String) msg.obj;
//                    mActivity.get().display.append(data);
//                    break;
//                case UsbService.CTS_CHANGE:
//                    Toast.makeText(mActivity.get(), "CTS_CHANGE",Toast.LENGTH_LONG).show();
//                    break;
//                case UsbService.DSR_CHANGE:
//                    Toast.makeText(mActivity.get(), "DSR_CHANGE",Toast.LENGTH_LONG).show();
//                    break;
//            }
//        }
//    }
//}