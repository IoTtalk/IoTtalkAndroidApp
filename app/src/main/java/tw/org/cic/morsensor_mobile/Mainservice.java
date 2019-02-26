package tw.org.cic.morsensor_mobile;

import android.app.Activity;
import android.app.Service;
import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.text.method.HideReturnsTransformationMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewDebug;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

//------------------------------
import com.ftdi.j2xx.D2xxManager;
import com.ftdi.j2xx.FT_Device;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.ServerSocket;
import java.net.Socket;

import tw.org.cic.dataManage.DataTransform;
import tw.org.cic.dataManage.MorSensorParameter;

import static tw.org.cic.dataManage.DataTransform.bytesToHexString;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbDevice;
import android.app.PendingIntent;
//------------------------------

public class Mainservice extends Service{

    class Httpthread extends Thread{

        @Override
        public void run(){
            Socket socket = null;
            try{
                httpserversocket = new ServerSocket(port);
                Log.v("httpthread","httpserversocket is created");
                while (true){
                    socket = httpserversocket.accept();
                    Httpresponse response = new Httpresponse(socket);
                    response.start();
                }

            }
            catch(IOException e){
                Log.e("httpthread","error "+e);
            }
        }

    }
    class Httpresponse extends Thread{
        Socket socket;
        Httpresponse(Socket socket){
            this.socket = socket;
        }
        @Override
        public void run(){
            BufferedReader input;
            PrintWriter output;
            String request;
            try{
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                request = input.readLine();
                output = new PrintWriter(socket.getOutputStream(),true);
                //String res = Integer.toString(countsend);
                JSONObject jsonobj = new JSONObject();
                JSONArray jsonarr = new JSONArray();
                if(datasend[0]>=0||datasend[1]>=0||datasend[2]>=0){
                    try{
                        jsonobj.put("humi",datasend[0]);
                        jsonobj.put("uv",datasend[1]);
                        jsonobj.put("alc",datasend[2]);
                        //Log.v("httptresponsehread","the data of humi "+datasend[0]);
                        //Log.v("httptresponsehread ","the data of uv is "+datasend[1]);
                    }
                    catch(JSONException e) {
                        Log.e("httptresponsehread", "json error " + e);
                    }
                    output.print("HTTP/1.1 200 \r\n");
                    //output.print("Content-Type: text/plain \r\n");
                    output.print("Content-Type: application/json \r\n");
                    //output.print("Content-Length: "+res.length()+"\r\n");
                    output.print("Content-Length: "+jsonobj.toString().length()+"\r\n");
                    output.print("Access-Control-Allow-Origin: *\r\n");
                    output.print("\r\n");
                    //output.print(res+"\r\n");
                    output.print(jsonobj.toString()+"\r\n");
                    output.flush();
                    socket.close();
                }
                else{
                    output.print("HTTP/1.1 404 \r\n");
                    output.flush();
                    socket.close();
                }

                Log.v("httpresponsethread ","response ");

            }
            catch(IOException e){
                Log.e("httptresponsehread","error "+e);
            }


        }

    }

    ServerSocket httpserversocket;
    static int port = 8080;

    private static String TAG = "MorService---usb---test";
    private final static String ACTION = "android.hardware.usb.action.USB_STATE";
    //final String ACTION_USB_PERMISSION = "tw.org.cic.morsensor_mobile.USB_PERMISSION";
    //final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    // j2xx
    public static D2xxManager ftD2xx = null;
    static FT_Device ftDev;
    int DevCount = -1;
    int currentPortIndex = -1;
    int portIndex = -1;
    static float[] datasend = new float[3];

    // handler event
    final int UPDATE_TEXT_VIEW_CONTENT = 0;
    final int ACT_ZMODEM_AUTO_START_RECEIVE = 21;

    final byte XON = 0x11;    /* Resume transmission */
    final byte XOFF = 0x13;    /* Pause transmission */

    final int MODE_GENERAL_UART = 0;
    final int MODE_X_MODEM_CHECKSUM_RECEIVE = 1;
    final int MODE_X_MODEM_CHECKSUM_SEND = 2;
    final int MODE_X_MODEM_CRC_RECEIVE = 3;
    final int MODE_X_MODEM_CRC_SEND = 4;
    final int MODE_X_MODEM_1K_CRC_RECEIVE = 5;
    final int MODE_X_MODEM_1K_CRC_SEND = 6;
    final int MODE_Y_MODEM_1K_CRC_RECEIVE = 7;
    final int MODE_Y_MODEM_1K_CRC_SEND = 8;
    final int MODE_Z_MODEM_RECEIVE = 9;
    final int MODE_Z_MODEM_SEND = 10;
    final int MODE_SAVE_CONTENT_DATA = 11;

    int transferMode = MODE_GENERAL_UART;
    int tempTransferMode = MODE_GENERAL_UART;

    final byte SOH = 1;    /* Start Of Header */
    final byte STX = 2;    /* Start Of Header 1K */
    final byte EOT = 4;    /* End Of Transmission */
    final byte ACK = 6;    /* ACKnowlege */
    final byte NAK = 0x15; /* Negative AcKnowlege */
    final byte CAN = 0x18; /* Cancel */
    final byte CHAR_C = 0x43; /* Character 'C' */
    final byte CHAR_G = 0x47; /* Character 'G' */


    final int MODEM_BUFFER_SIZE = 2048;
    int[] modemReceiveDataBytes;
    byte[] modemDataBuffer;
    byte[] zmDataBuffer;

    boolean bModemGetNak = false;
    boolean bModemGetAck = false;
    boolean bModemGetCharC = false;
    boolean bModemGetCharG = false;

    // general data count
    int totalReceiveDataBytes = 0;
    int totalUpdateDataBytes = 0;

    // thread to read the data
    HandlerThread handlerThread; // update data to UI
    ReadThread readThread; // read data from USB

    boolean bContentFormatHex = false;

    // variables
//    final int UI_READ_BUFFER_SIZE = 10240; // Notes: 115K:1440B/100ms, 230k:2880B/100ms
    final int UI_READ_BUFFER_SIZE = 10260; // Notes: 115K:1440B/100ms, 230k:2880B/100ms
    static byte[] writeBuffer;
    static byte[] writeBootloaderBuffer;
    byte[] readBuffer;
    char[] readBufferToChar;
    int actualNumBytes;

    int baudRate; /* baud rate */
    byte stopBit; /* 1:1stop bits, 2:2 stop bits */
    byte dataBit; /* 8:8bit, 7: 7bit */
    byte parity; /* 0: none, 1: odd, 2: even, 3: mark, 4: space */
    byte flowControl; /* 0:none, 1: CTS/RTS, 2:DTR/DSR, 3:XOFF/XON */
    public static Context mContext;
    //public static Activity mMainViewActivity;

    // data buffer
    byte[] writeDataBuffer;
    byte[] readDataBuffer; /* circular buffer */

    int iTotalBytes;
    int iReadIndex;

    final int MAX_NUM_BYTES = 65536;

    static boolean bReadTheadEnable = false;

    BroadcastReceiver usbattach;
    BroadcastReceiver usbdeatach;
    BroadcastReceiver usbstate;
    IntentFilter atfilter ;
    IntentFilter defilter ;
    UsbManager usbmanger;
    PendingIntent permissionintent;
    //------------


    @Override
    public IBinder onBind(Intent intent){
        return null;
    }

    @Override
    public void onCreate(){

        super.onCreate();
        Log.v(TAG,"service is on create state");

        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
        final String formatedIpAddress = String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff),
                (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
        Log.v("IP ADDRESS","My IP "+formatedIpAddress);

        Httpthread httpthread = new Httpthread();
        httpthread.start();

        usbattach = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                String action = intent.getAction();
                if(UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)){
                    Log.v("usb-detect-test","usb connected");
                    createDeviceList();
                    if (DevCount > 0) {
                        Log.v("usb-detect-test","DevCount > 0");
                        connectFunction();
                        setConfig(baudRate, dataBit, stopBit, parity, flowControl);
                        checkDevceConnection();
                    }
                    //checkDevceConnection();
                    else{
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                Log.v("usb-detect-test","checkconnection thread");
                                try{
                                    while(DevCount<0){
                                        checkDevceConnection();
                                        Thread.sleep(10000);
                                    }
                                }
                                catch(Exception e){
                                    Log.v("MS on startcommand","err"+e);
                                }
                            }
                        }).start();
                    }
                }
                //registerReceiver(usbdeatach,defilter);
                //unregisterReceiver(usbattach);
            }
        };
        usbdeatach = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                String action = intent.getAction();
                if(UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)){
                    Log.v("usb-detect-test","usb unconnected");
                    datasend[0] = -1;
                    datasend[1] = -1;
                    datasend[2] = -1;

                }
                //registerReceiver(usbattach,atfilter);
                //unregisterReceiver(usbdeatach);
                stopSelf();
            }
        };

        //usbmanger = (UsbManager)getSystemService(Context.USB_SERVICE);

        /*IntentFilter stafilter = new IntentFilter();
        stafilter.addAction(ACTION);
        registerReceiver(usbstate,stafilter);*/

        atfilter = new IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        defilter = new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(usbattach,atfilter);
        registerReceiver(usbdeatach,defilter);

        Intent notificationIntent = new Intent(this, MainViewActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle("My Awesome App")
                .setContentText("Doing some work...")
                .setContentIntent(pendingIntent).build();
        startForeground(1337, notification);
        //Toast.makeText(Mainservice.this, "MainService is on create", Toast.LENGTH_SHORT).show();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){

        //-----------------------------------------------------------
        try {
            ftD2xx = D2xxManager.getInstance(this);
            ftD2xx.setVIDPID(1027, 515);
        } catch (D2xxManager.D2xxException e) {
            Log.e("FTDI_HT", "getInstance fail!!");
        }
        mContext = this;

        // init modem variables
        modemReceiveDataBytes = new int[1];
        modemReceiveDataBytes[0] = 0;
        modemDataBuffer = new byte[MODEM_BUFFER_SIZE];
        zmDataBuffer = new byte[MODEM_BUFFER_SIZE];

        //allocate buffer
        writeBuffer = new byte[20];
        writeBootloaderBuffer = new byte[UI_READ_BUFFER_SIZE];
        readBuffer = new byte[UI_READ_BUFFER_SIZE];
        readBufferToChar = new char[UI_READ_BUFFER_SIZE];
        readDataBuffer = new byte[MAX_NUM_BYTES];
        actualNumBytes = 0;

        // start main text area read thread
        //handler will be defined
        handlerThread = new HandlerThread();
        handlerThread.start();

//		// setup the baud rate list
        baudRate = 115200;
        stopBit = 1;
        dataBit = 8;
        parity = 0;
        flowControl = 0;
        portIndex = 0;

        datasend[0] = -1;
        datasend[1] = -1;
        datasend[2] = -1;

        checkDevceConnection();

        //Toast.makeText(mContext, "Service on start command----------", Toast.LENGTH_SHORT).show();
        //Toast.makeText(mContext, "Morsensor start", Toast.LENGTH_SHORT).show();
        DLog.d(TAG, "onStartCommand----------");

        return START_STICKY;
    }

    @Override
    public void onDestroy(){
        Log.d(TAG, "--- onDestroy ---");
        Toast.makeText(Mainservice.this, "Morsensor MainService is  destroyed", Toast.LENGTH_SHORT).show();
        disconnectFunction();
        unregisterReceiver(usbattach);
        unregisterReceiver(usbdeatach);
        try{
            httpserversocket.close();
        }
        catch (IOException e){
            Log.e("http on destroy ","error "+e);
        }
        super.onDestroy();
    }

    public static void sendCommand(int sendCommand) {
        try {
            switch (sendCommand) {
                case MorSensorParameter.SEND_MORSENSOR_BLE_SENSOR_DATA_ALL:
                    writeBuffer[0] = (byte) 0xF3;
                    writeBuffer[1] = (byte) 0x00;
                    break;
            }
            sendData(writeBuffer.length, writeBuffer);
            requestSensorData = true;

        } catch (IllegalArgumentException e) {
            // midToast("Incorrect input for HEX format."
            // + "\nAllowed charater: 0~9, a~f and A~F", Toast.LENGTH_SHORT);
            Toast.makeText(mContext, "Incorrect input for HEX format.", Toast.LENGTH_LONG).show();
            DLog.e(TAG, "Illeagal HEX input.");
        }
    }

    private void checkDevceConnection() {
        if (null == ftDev || false == ftDev.isOpen()) {
            DLog.e(TAG, "checkDeviceConnection first sentence - reconnect ------------ null");
            requestSensorData = false;
            if (ftDev != null)
                DLog.e(TAG, "checkDeviceConnection - reconnect:" + ftDev.isOpen());
            createDeviceList();
            if (DevCount > 0) {
                connectFunction();
//                setUARTInfoString();
                setConfig(baudRate, dataBit, stopBit, parity, flowControl);

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        sendCommand(MorSensorParameter.SEND_MORSENSOR_BLE_SENSOR_DATA_ALL);
                    }
                }).start();
            } else {
                stopSelf();
            }
        } else {
            DLog.e(TAG, "checkDeviceConnection onResume - reconnect - "+ftDev.isOpen());
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    sendCommand(MorSensorParameter.SEND_MORSENSOR_BLE_SENSOR_DATA_ALL);
                }
            }).start();
        }
    }

    //-----------------
    // j2xx functions +
    public void createDeviceList() {
        Log.d(TAG, "createDeviceList");
        int tempDevCount = ftD2xx.createDeviceInfoList(mContext);

        if (tempDevCount > 0) {
            Log.v("CDL function","tempDevCount > 0 ");
            if (DevCount != tempDevCount) {
                DevCount = tempDevCount;
                Log.v("CDL function","DevCount != tempDevCount ");
            }
        } else {
            Log.v("CDL function","tempDevCount < 0 ");
            DevCount = -1;
            currentPortIndex = -1;
        }
        Log.d(TAG, "createDeviceList  DevCount:" + DevCount + "  currentPortIndex:" + currentPortIndex);
        //Toast.makeText(mContext, "createDeviceList  DevCount:" + DevCount + "  currentPortIndex:" + currentPortIndex, Toast.LENGTH_SHORT).show();
    }

    public void disconnectFunction() {
        Log.d(TAG, "disconnectFunction");
        DevCount = -1;
        currentPortIndex = -1;
        bReadTheadEnable = false;
        requestSensorData = false;
//        readThread.interrupt();
//        handlerThread.interrupt();
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (ftDev != null) {
            if (ftDev.isOpen()) {
                ftDev.close();
                DLog.d(TAG, "disconnectFunction: close ok.");
            } else {
                DLog.d(TAG, "disconnectFunction: FT is not open.");
            }
        }
    }

    public void connectFunction() {
        Log.d(TAG, "-----------connectFunction-----------");
        if (portIndex + 1 > DevCount) {
            portIndex = 0;
        }

        if (currentPortIndex == portIndex
                && ftDev != null
                && ftDev.isOpen()) {
//            Toast.makeText(mContext, "Port(" + portIndex + ") is already opened.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (bReadTheadEnable) {
            bReadTheadEnable = false;
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (null == ftDev) {
            ftDev = ftD2xx.openByIndex(mContext, portIndex);
        } else {
            ftDev = ftD2xx.openByIndex(mContext, portIndex);
        }

        if (ftDev == null) {
//            midToast("Open port(" + portIndex + ") NG!", Toast.LENGTH_LONG);
            return;
        }

        if (ftDev.isOpen()) {
            currentPortIndex = portIndex;
//            Toast.makeText(mContext, "open device port(" + portIndex + ") OK", Toast.LENGTH_SHORT).show();

            if (!bReadTheadEnable) {

                readThread = new ReadThread();
                readThread.start();
            }
        } else {
            //midToast("Open port(" + portIndex + ") NG!", Toast.LENGTH_LONG);
            Toast.makeText(mContext, "Open port(" + portIndex + ") NG!", Toast.LENGTH_LONG).show();
            //Toast.makeText(mContext, "Service is startoncommand!", Toast.LENGTH_LONG).show();
        }
    }


    void setConfig(int baud, byte dataBits, byte stopBits, byte parity, byte flowControl) {
        // configure port
        // reset to UART mode for 232 devices
        ftDev.setBitMode((byte) 0, D2xxManager.FT_BITMODE_RESET);

        ftDev.setBaudRate(baud);

        switch (dataBits) {
            case 7:
                dataBits = D2xxManager.FT_DATA_BITS_7;
                break;
            case 8:
                dataBits = D2xxManager.FT_DATA_BITS_8;
                break;
            default:
                dataBits = D2xxManager.FT_DATA_BITS_8;
                break;
        }

        switch (stopBits) {
            case 1:
                stopBits = D2xxManager.FT_STOP_BITS_1;
                break;
            case 2:
                stopBits = D2xxManager.FT_STOP_BITS_2;
                break;
            default:
                stopBits = D2xxManager.FT_STOP_BITS_1;
                break;
        }

        switch (parity) {
            case 0:
                parity = D2xxManager.FT_PARITY_NONE;
                break;
            case 1:
                parity = D2xxManager.FT_PARITY_ODD;
                break;
            case 2:
                parity = D2xxManager.FT_PARITY_EVEN;
                break;
            case 3:
                parity = D2xxManager.FT_PARITY_MARK;
                break;
            case 4:
                parity = D2xxManager.FT_PARITY_SPACE;
                break;
            default:
                parity = D2xxManager.FT_PARITY_NONE;
                break;
        }

        ftDev.setDataCharacteristics(dataBits, stopBits, parity);

        short flowCtrlSeTAGing;
        switch (flowControl) {
            case 0:
                flowCtrlSeTAGing = D2xxManager.FT_FLOW_NONE;
                break;
            case 1:
                flowCtrlSeTAGing = D2xxManager.FT_FLOW_RTS_CTS;
                break;
            case 2:
                flowCtrlSeTAGing = D2xxManager.FT_FLOW_DTR_DSR;
                break;
            case 3:
                flowCtrlSeTAGing = D2xxManager.FT_FLOW_XON_XOFF;
                break;
            default:
                flowCtrlSeTAGing = D2xxManager.FT_FLOW_NONE;
                break;
        }

        ftDev.setFlowControl(flowCtrlSeTAGing, XON, XOFF);
    }

    static void sendData(int numBytes, byte[] buffer) {
        if (!ftDev.isOpen()) {
            DLog.e(TAG, "SendData: device not open");

            /*mMainViewActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mContext, "Device not open!", Toast.LENGTH_SHORT).show();
                }
            });*/
            Toast.makeText(mContext, "Device not open!", Toast.LENGTH_LONG).show();

            return;
        }

        if (numBytes > 0) {
            try {
                ftDev.write(buffer, numBytes);
            } catch (Exception e) {
                DLog.e(TAG, "ftDev.write Error");
            }

            DLog.e(TAG, "sendData:" + bytesToHexString(buffer));
        }
        else{
            Log.e("sendData: ","numByte <=0");

        }

    }


    byte readData(int numBytes, byte[] buffer) {
        byte intstatus = 0x00; /* success by default */

        /* should be at least one byte to read */
        if ((numBytes < 1) || (0 == iTotalBytes)) {
            actualNumBytes = 0;
            intstatus = 0x01;
            return intstatus;
        }

        if (numBytes > iTotalBytes) {
            numBytes = iTotalBytes;
        }

        /* update the number of bytes available */
        iTotalBytes -= numBytes;
        actualNumBytes = numBytes;

        /* copy to the user buffer */
        for (int count = 0; count < numBytes; count++) {
            buffer[count] = readDataBuffer[iReadIndex];
            iReadIndex++;
            iReadIndex %= MAX_NUM_BYTES;
        }

        return intstatus;
    }

    static boolean requestSensorData = false;

    // Update UI content
    class HandlerThread extends Thread {

        public void run() {
            byte status;
            //Message msg;
            while (true) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (true == bContentFormatHex) // consume input data at hex content format
                {
                    status = readData(UI_READ_BUFFER_SIZE, readBuffer);
                } else if (MODE_GENERAL_UART == transferMode) {
                    status = readData(UI_READ_BUFFER_SIZE, readBuffer);

                    if (0x00 == status) {
                        //msg = mHandler.obtainMessage(UPDATE_TEXT_VIEW_CONTENT);
                        //mHandler.sendMessage(msg);
                        int rawDataLength = actualNumBytes;
                        Log.d(TAG, "UPDATE_TEXT_VIEW_CONTENT readBuffer:" + readBuffer.length + " actualNumBytes:" + actualNumBytes + " totalUpdateDataBytes:" + totalUpdateDataBytes);
                        if (actualNumBytes > 0) {

                            String data = bytesToHexString(readBuffer).substring(0, rawDataLength * 2);
                            Log.e(TAG, "UPDATE_TEXT_VIEW_CONTENT_120:" + data);
                            totalUpdateDataBytes += actualNumBytes;
                            for (int i = 0; i < actualNumBytes; i++) {
                                readBufferToChar[i] = (char) readBuffer[i];
                            }
                            switch (readBuffer[0]) {
                                case MorSensorParameter.IN_BLE_SENSOR_DATA:
                                    if(readBuffer[1] != 0) {
                                        DataTransform.TransformTempHumi(readBuffer);
                                        DataTransform.TransformUV(readBuffer);
                                        DataTransform.TransformAlcohol(readBuffer);
                                        displaySensorData();
                                    }
                                    /*else{
                                        datasend[0]=-1;
                                        datasend[1]=-1;
                                        datasend[2]=-1;
                                    }*/
                                    break;
                            }
                        }

                    }

                }
            }
        }
    }

    class ReadThread extends Thread {
        final int USB_DATA_BUFFER = 8192;

        int index=0;

        ReadThread() {
            this.setPriority(MAX_PRIORITY);
        }

        public void run() {
            Log.d(TAG, "~~~~~~~~~~~~~~~~~~ReadThread~~~~~~~~~~~~~~~~~~");
            byte[] usbdata = new byte[USB_DATA_BUFFER];
            int readcount = 0;
            int iWriteIndex = 0;
            bReadTheadEnable = true;

            while (bReadTheadEnable) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                while (iTotalBytes > (MAX_NUM_BYTES - (USB_DATA_BUFFER + 1))) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                readcount = ftDev.getQueueStatus();
                //Log.e(">>@@","iavailable:" + iavailable);

                if (readcount > 0) {
                    Log.d(TAG, "readcount:" + readcount);
                    if (readcount > USB_DATA_BUFFER) {
                        readcount = USB_DATA_BUFFER;
                    }
                    ftDev.read(usbdata, readcount);

                    if ((MODE_X_MODEM_CHECKSUM_SEND == transferMode)
                            || (MODE_X_MODEM_CRC_SEND == transferMode)
                            || (MODE_X_MODEM_1K_CRC_SEND == transferMode)) {
                        Log.d(TAG, "transferMode:" + transferMode);
                        for (int i = 0; i < readcount; i++) {
                            modemDataBuffer[i] = usbdata[i];
                            DLog.e(TAG, "RT usbdata[" + i + "]:(" + usbdata[i] + ")");
                        }

                        if (NAK == modemDataBuffer[0]) {
                            DLog.e(TAG, "get response - NAK");
                            bModemGetNak = true;
                        } else if (ACK == modemDataBuffer[0]) {
                            DLog.e(TAG, "get response - ACK");
                            bModemGetAck = true;
                        } else if (CHAR_C == modemDataBuffer[0]) {
                            DLog.e(TAG, "get response - CHAR_C");
                            bModemGetCharC = true;
                        }
                        if (CHAR_G == modemDataBuffer[0]) {
                            DLog.e(TAG, "get response - CHAR_G");
                            bModemGetCharG = true;
                        }
                    } else {
                        Log.d(TAG, "transferMode:" + transferMode);
                        totalReceiveDataBytes += readcount;
                        //DLog.e(TAG,"totalReceiveDataBytes:"+totalReceiveDataBytes);

                        //DLog.e(TAG,"readcount:"+readcount);
                        for (int count = 0; count < readcount; count++) {
                            readDataBuffer[iWriteIndex] = usbdata[count];
                            iWriteIndex++;
                            iWriteIndex %= MAX_NUM_BYTES;
                        }

                        if (iWriteIndex >= iReadIndex) {
                            iTotalBytes = iWriteIndex - iReadIndex;
                        } else {
                            iTotalBytes = (MAX_NUM_BYTES - iReadIndex) + iWriteIndex;
                        }

                        //DLog.e(TAG,"iTotalBytes:"+iTotalBytes);
                        if ((MODE_X_MODEM_CHECKSUM_RECEIVE == transferMode)
                                || (MODE_X_MODEM_CRC_RECEIVE == transferMode)
                                || (MODE_X_MODEM_1K_CRC_RECEIVE == transferMode)
                                || (MODE_Y_MODEM_1K_CRC_RECEIVE == transferMode)
                                || (MODE_Z_MODEM_RECEIVE == transferMode)
                                || (MODE_Z_MODEM_SEND == transferMode)) {
                            modemReceiveDataBytes[0] += readcount;
                            DLog.e(TAG, "modemReceiveDataBytes:" + modemReceiveDataBytes[0]);
                        }
                    }
                }
                else{
                    if(readcount<0) {
                        //Log.v("readthread====","readcount= "+readcount);
                        datasend[0] = -1;
                        datasend[1] = -1;
                        datasend[2] = -1;
                    }
                }
            }
            DLog.e(TAG, "read thread terminate...");
        }
    }


    public void displaySensorData() {
        float[] data = DataTransform.getData();
        Log.i(TAG, "Humi:" + data[1] + " UV:" + data[2] + " Alcohol:" + data[3]);
        MorSensorParameter.humi_data = (int) data[1];
        MorSensorParameter.uv_data = ((int) (data[2] * 100) / 100f);
        MorSensorParameter.alcohol_data = ((int) (data[3] * 1000) / 1000f);
        int scale = 5;
        int roundingmode = 4;
        //BigDecimal bdhumi = new BigDecimal((double)MorSensorParameter.humi_data);
        BigDecimal bduv = new BigDecimal((double)MorSensorParameter.uv_data);
        BigDecimal bdalc = new BigDecimal((double)MorSensorParameter.alcohol_data);
        bduv = bduv.setScale(scale,roundingmode);
        bdalc = bdalc.setScale(scale,roundingmode);

        datasend[0] = MorSensorParameter.humi_data;
        datasend[1] = bduv.floatValue();
        datasend[2] = bdalc.floatValue();
        Log.v("datasend ","datasend is "+datasend[1]);
        Log.i(TAG, "Humi:" + data[1] + " UV:" + data[2] + " Alcohol:" + data[3]);
    }

}