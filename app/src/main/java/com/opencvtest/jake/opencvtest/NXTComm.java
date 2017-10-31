package com.opencvtest.jake.opencvtest;

import android.os.Handler;
import android.util.Log;

public class NXTComm extends Thread {
    private BTComm nxtComm;
    private Handler uiHandler;

    private String msgIn;
    private String msgOut;

    NXTComm(Handler uiHandler) {
        this.uiHandler = uiHandler;
        nxtComm = new BTComm();
        nxtComm.enableBT();
        nxtComm.connectToNXT();
    }

    @Override
    public void run() {
        super.run();

        while(true) {
            try {
                Log.i("MESSAGE", "Writing Message " + msgOut);
                nxtComm.writeMessage(msgOut);
                //Log.i("BTWrite", msgOut);
                uiHandler.sendEmptyMessage(0);
                // Optional delay to slow down the message bounce rate and maybe (?) preserve battery
                // Thread.sleep(100);
                msgIn = nxtComm.readMessage();
                //Log.i("BTRead", msgIn);
                uiHandler.sendEmptyMessage(0);
            } catch (Exception e) {
                Log.i("ERROR", e.getMessage());
            }
        }
    }

    public void setMsgOut(String newMsg) {
        msgOut = newMsg;
    }

    public String getMsgIn() {
        return msgIn;
    }

    @Override
    public void interrupt() {
        super.interrupt();
        nxtComm.close();
    }
}