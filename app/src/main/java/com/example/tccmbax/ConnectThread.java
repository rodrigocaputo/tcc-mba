package com.example.tccmbax;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.UUID;

public class ConnectThread extends Thread {
    Context context = null;
    private final BluetoothSocket mmSocket;
    private final BluetoothDevice mmDevice;
    InputStream input = null;
    OutputStream output = null;
    public byte[] class_result = null;

    public ConnectThread(Context context, BluetoothDevice device) {
        this.context = context;
        // Use a temporary object that is later assigned to mmSocket
        // because mmSocket is final.
        BluetoothSocket tmp = null;
        mmDevice = device;
        try {
            // Get a BluetoothSocket to connect with the given BluetoothDevice.
            // MY_UUID is the app's UUID string, also used in the server code.
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                tmp = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
            } else {
                Toast.makeText(context, "Sem permissão para conexão BlueTooth", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Toast.makeText(context, "Socket's create() method failed", Toast.LENGTH_SHORT).show();
        }
        mmSocket = tmp;
    }

    public void run() {
        // Cancel discovery because it otherwise slows down the connection.
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
        } else {
            Log.e("TCC-MBA", "Sem permissão para BlueTooth Scan");
        }

        try {
            // Connect to the remote device through the socket. This call blocks
            // until it succeeds or throws an exception.
            mmSocket.connect();
        } catch (IOException connectException) {
            // Unable to connect; close the socket and return.
            try {
                mmSocket.close();
            } catch (IOException closeException) {
                Log.e("TCC-MBA", "Could not close the client socket");
            }
            return;
        }

        // The connection attempt succeeded. Perform work associated with
        // the connection in a separate thread.
        manageMyConnectedSocket(mmSocket);
    }

    private void manageMyConnectedSocket(BluetoothSocket mmSocket) {
        try {
            input = mmSocket.getInputStream();
            output = mmSocket.getOutputStream();

            byte[] buffer = new byte[1024];
            int bytes;

            while (mmSocket != null) {
                bytes = input.read(buffer);
                String mensagem = new String(Arrays.copyOfRange(buffer, 0, bytes));
                Log.e("TCC-MBA", "MENSAGEM RECEBIDA: " + mensagem);
                if (class_result != null) {
                    write(class_result);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void write(byte[] data) {
        if (output != null) {
            try {
                output.write(data);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Log.d("TCCMBAX", "ERRO! output é null");
        }
    }

    // Closes the client socket and causes the thread to finish.
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) {
            Log.e("TCC-MBA", "Could not close the client socket");
        }
    }
}
