package com.opencvtest.jake.opencvtest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

public class BTComm {
    BluetoothAdapter localAdapter;
    BluetoothSocket socket_nxt;
    boolean success = false;

    // Enables Bluetooth if not enabled
    public void enableBT(){
        localAdapter = BluetoothAdapter.getDefaultAdapter();
        // If Bluetooth not enable then do it
        if (!localAdapter.isEnabled()) {
            localAdapter.enable();
            while(!(localAdapter.isEnabled()));
        }
        localAdapter.cancelDiscovery();
    }

    // Connect to NXT
    public boolean connectToNXT() {

        BluetoothDevice nxt_device = null;

        Set<BluetoothDevice> pairedDevices = localAdapter.getBondedDevices();
        // If there are paired devices
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                // Hardcoded NXT name; bad idea, but works for what I need
                Log.i("BT", device.getName());
                if (device.getName().equals("NXT")) {
                    nxt_device = device;
                }
            }
        }

        // Get the BluetoothDevice of the NXT
        // Try to connect to the nxt
        try {
            socket_nxt = nxt_device.createRfcommSocketToServiceRecord(nxt_device.getUuids()[0].getUuid());
            socket_nxt.connect();
            success = true;
        } catch (IOException e) {
            Log.d("Bluetooth","Err: Device not found or cannot connect");
            success=false;
        }
        return success;
    }

    private byte[] packMsg(int mailbox, String message) {

        byte[] cmd = new byte[80];

        cmd[2] = (byte)0x80;
        cmd[3] = (byte)0x09;

        cmd[4] = (byte)mailbox;

        String msg = message + "\0";

        cmd[5] = (byte)( msg.length() & 0xff );

        for( int i = 0; i < msg.length(); i++ )
            cmd[6+i] = (byte)msg.charAt(i);

        cmd[0] = (byte)( (cmd.length - 2) & 0xFF );
        cmd[1] = (byte)( (cmd.length - 2) >> 8 );

        return cmd;
    }

    private String unpackMsg(byte[] buffer, int length) {
        String result = "";
        for (int i = 0; i < length - 1; i++) {
            if(buffer[6+i] == (byte)"\0".charAt(0)) break;
            result += (char)buffer[6+i];
        }
        return result;
    }

    public void writeMessage(String msg) throws InterruptedException {
        if (socket_nxt!=null) {
            try {
                byte[] writeMessage = packMsg(0, msg);
                OutputStream out = socket_nxt.getOutputStream();
                out.write(writeMessage);
                out.flush();

                // Log.i("BluetoothWrite", ""+msg);

            } catch (IOException e) {
                Log.i("ERROR", e.getMessage());
            }
        } else {
            Log.i("ERROR", "Connection is Null");
        }
    }

    public String readMessage() {
        if (socket_nxt!=null) {
            try {
                byte[] buffer = new byte[129];
                InputStream in = socket_nxt.getInputStream();
                int n = in.read(buffer);
                return unpackMsg(buffer, n);

            } catch (IOException e) {
                Log.i("ERROR", e.getMessage());
                return null;
            }
        } else {
            Log.i("ERROR", "Connection is Null");
            return null;
        }
    }

    public void close() {
        try {
            socket_nxt.close();
        } catch (IOException e) {
            Log.i("ERROR", e.getMessage());
        }
    }
}