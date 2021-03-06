/*
 * Copyright AllSeen Alliance. All rights reserved.
 *
 *    Permission to use, copy, modify, and/or distribute this software for any
 *    purpose with or without fee is hereby granted, provided that the above
 *    copyright notice and this permission notice appear in all copies.
 *
 *    THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 *    WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 *    MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 *    ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 *    WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 *    ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 *    OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package org.alljoyn.bus.samples.basicclient;

import org.alljoyn.bus.BusAttachment;
import org.alljoyn.bus.BusException;
import org.alljoyn.bus.BusListener;
import org.alljoyn.bus.Mutable;
import org.alljoyn.bus.ProxyBusObject;
import org.alljoyn.bus.SessionListener;
import org.alljoyn.bus.SessionOpts;
import org.alljoyn.bus.Status;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Client extends Activity {
    /* Load the native alljoyn_java library. */
    static {
        System.loadLibrary("alljoyn_java");
    }

    private static final int MESSAGE_PING = 1;
    private static final int MESSAGE_PING_REPLY = 2;
    private static final int MESSAGE_POST_TOAST = 3;
    private static final int MESSAGE_START_PROGRESS_DIALOG = 4;
    private static final int MESSAGE_STOP_PROGRESS_DIALOG = 5;
    private static final int MESSAGE_NAME = 6;
    private static final int MESSAGE_TEMP = 7;
    private static final int MESSAGE_OFF = 8;
    private static final int MESSAGE_FREQ0 = 9;
    private static final int MESSAGE_FREQ1 = 10;
    private static final int MESSAGE_CRITTEMP = 11;

    public String TempAndName;
    private static final String TAG = "BasicClient";
    public Message msgClick;
    public Message msgClick1;
    public Message msgClick2;
    public Message msgClick3;
    public Message msgClick4;
    public Message off;
    private EditText mEditText;
    private ArrayAdapter<String> mListViewArrayAdapter;
    private ListView mListView;
    private Menu menu;
    public Integer sensorsCount;
    public Button getOne;
    public Button getTwo;
    public Button lighOff;
    public double procTemp;
    public String nameSens;


    /* Handler used to make calls to AllJoyn methods. See onCreate(). */
    private BusHandler mBusHandler;

    private ProgressDialog mDialog;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_PING:
                    Integer cat = (Integer) msg.obj;
                    mListViewArrayAdapter.add("Cat args:  " + cat);
                    break;
                case MESSAGE_PING_REPLY:
                    Integer ret = (Integer) msg.obj;
                    mListViewArrayAdapter.add("Количество датчиков температуры:  " + ret);
                    sensorsCount = ret;
                    //mEditText.setText("");
                   // sensorsInfo.setText("Have reply: " + ret);
                    break;
                case MESSAGE_NAME:
                    String name = (String) msg.obj;
                    //mListViewArrayAdapter.add("" + name);
                    TempAndName = name;
                    if (name.equals("Physical id 0\n")) {
                        nameSens = name;
                    }

                    //mEditText.setText("");
                   // sensorsInfo.setText("Have reply: " + ret);
                    break;
                case MESSAGE_TEMP:
                    Integer temp = (Integer) msg.obj;
                    TempAndName = TempAndName + temp/1000 + "˚C ";
                    procTemp = temp;
                    //mListViewArrayAdapter.insert(temp.toString(), mListViewArrayAdapter.getCount());
                    //mListViewArrayAdapter.add("" + TempAndName);
                   // TempAndName = null;
                    //mEditText.setText("");
                   // sensorsInfo.setText("Have reply: " + ret);
                    break;
                case MESSAGE_CRITTEMP:
                    Integer critTempDev = (Integer) msg.obj;
                    procTemp = procTemp/critTempDev * 100;
                    procTemp = new BigDecimal(procTemp).setScale(2, RoundingMode.UP).doubleValue();
                    TempAndName = TempAndName + "(" +procTemp + "% of critical temperature)\n";
                    if (nameSens.equals("Physical id 0\n")) {
                        mListViewArrayAdapter.add("" + TempAndName);
                        TempAndName = null;
                        nameSens = "ABC";
                    }
                    //mListViewArrayAdapter.add("" + TempAndName);
                    //TempAndName = null;
                    break;
                case MESSAGE_FREQ0:
                    Integer freq0 = (Integer) msg.obj;
                    TempAndName = TempAndName + "Freq 0: " + freq0 + " Hz\n";
                    //mListViewArrayAdapter.add("" + TempAndName);
                    //TempAndName = null;
                    break;
                case MESSAGE_FREQ1:
                    Integer freq1 = (Integer) msg.obj;
                    TempAndName = TempAndName + "Freq 1: " + freq1 + "Hz";
                    mListViewArrayAdapter.add("" + TempAndName);
                    TempAndName = null;
                    break;
                case MESSAGE_OFF:
                    Integer var = (Integer) msg.obj;
                    if (var == 0) {
                        mListViewArrayAdapter.add("Удаленное устройство выключено");
                    }

                    //mListViewArrayAdapter.insert(temp.toString(), mListViewArrayAdapter.getCount());
                    //mListViewArrayAdapter.add("" + TempAndName);
                    //TempAndName = null;
                    //mEditText.setText("");
                   // sensorsInfo.setText("Have reply: " + ret);
                    break;

                case MESSAGE_POST_TOAST:
                    Toast.makeText(getApplicationContext(), (String) msg.obj, Toast.LENGTH_LONG).show();
                    break;
                case MESSAGE_START_PROGRESS_DIALOG:
                    mDialog = ProgressDialog.show(Client.this,
                            "",
                            "Finding Basic Service.\nPlease wait...",
                            true,
                            true);
                    break;
                case MESSAGE_STOP_PROGRESS_DIALOG:
                    mDialog.dismiss();
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);
        //sensorsInfo = (TextView) findViewById(R.id.sensorsInfo);
        getOne = (Button)findViewById(R.id.refreshBut);
        getTwo = (Button) findViewById(R.id.button);
        lighOff = (Button)findViewById(R.id.offBut);
        mListViewArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
        mListView = (ListView) findViewById(R.id.listView);
        mListView.setAdapter(mListViewArrayAdapter);
        getTwo.setEnabled(false);
        lighOff.setEnabled(false);

        /* Make all AllJoyn calls through a separate handler thread to prevent blocking the UI. */
        HandlerThread busThread = new HandlerThread("BusHandler");
        busThread.start();
        mBusHandler = new BusHandler(busThread.getLooper());

        /* Connect to an AllJoyn object. */
        mBusHandler.sendEmptyMessage(BusHandler.CONNECT);
        mHandler.sendEmptyMessage(MESSAGE_START_PROGRESS_DIALOG);
        Message msg = mBusHandler.obtainMessage(BusHandler.GETSENSORSCOUNT,
               "abd");
        mBusHandler.sendMessage(msg);
    }
    public void onClick(View view) {
        //sensorsInfo.setText("Have click");
        getOne.setEnabled(false);
        getTwo.setEnabled(true);
        lighOff.setEnabled(true);
        mListViewArrayAdapter.clear();
        //mListViewArrayAdapter.add("Have click");
        Message msg = mBusHandler.obtainMessage(BusHandler.GETSENSORSCOUNT,
                "abd");
        mBusHandler.sendMessage(msg);
    }
    public void onClickTemp(View view) {
        mListViewArrayAdapter.clear();
        mListViewArrayAdapter.add("Количество датчиков температуры: " + sensorsCount);
        int first = 0;
        int second = 1;
        for (int i = 1; i <= sensorsCount; ++i) {
            //mListViewArrayAdapter.add("Have clickTemp");
            msgClick = mBusHandler.obtainMessage(BusHandler.GETNAME, i);
            mBusHandler.sendMessage(msgClick);
            msgClick1 = mBusHandler.obtainMessage(BusHandler.GETTEMP, i);
            mBusHandler.sendMessage(msgClick1);
            msgClick2 = mBusHandler.obtainMessage(BusHandler.GETCRITTEMP, i);
            mBusHandler.sendMessage(msgClick2);

            if (i != 1) {
                msgClick3 = mBusHandler.obtainMessage(BusHandler.GETFREQ0, first);
                mBusHandler.sendMessage(msgClick3);
                msgClick4 = mBusHandler.obtainMessage(BusHandler.GETFREQ1, second);
                mBusHandler.sendMessage(msgClick4);
                first += 2;
                second += 2;
            }
        }
    }
    public void onClickLightOff(View view) {
        mListViewArrayAdapter.clear();
        //mListViewArrayAdapter.add("Удаленная машина выключена");
        off = mBusHandler.obtainMessage(BusHandler.GETOFF);
        mBusHandler.sendMessage(off);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mainmenu, menu);
        this.menu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.quit:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        /* Disconnect to prevent resource leaks. */
        mBusHandler.sendEmptyMessage(BusHandler.DISCONNECT);
    }

    /* This class will handle all AllJoyn calls. See onCreate(). */
    class BusHandler extends Handler {
        /*
         * Name used as the well-known name and the advertised name of the service this client is
         * interested in.  This name must be a unique name both to the bus and to the network as a
         * whole.
         *
         * The name uses reverse URL style of naming, and matches the name used by the service.
         */
        private static final String SERVICE_NAME = "org.alljoyn.Bus.sample";
        private static final short CONTACT_PORT=25;

        private BusAttachment mBus;
        private ProxyBusObject mProxyObj;
        private BasicInterface mBasicInterface;

        private int 	mSessionId;
        private boolean mIsInASession;
        private boolean mIsConnected;
        private boolean mIsStoppingDiscovery;

        /* These are the messages sent to the BusHandler from the UI. */
        public static final int CONNECT = 1;
        public static final int JOIN_SESSION = 2;
        public static final int DISCONNECT = 3;
        public static final int GETSENSORSCOUNT = 4;
        public static final int GETNAME = 5;
        public static final int GETTEMP = 6;
        public static final int GETOFF = 7;
        public static final int GETFREQ0 = 8;
        public static final int GETFREQ1 = 9;
        public static final int GETCRITTEMP = 10;
        public BusHandler(Looper looper) {
            super(looper);

            mIsInASession = false;
            mIsConnected = false;
            mIsStoppingDiscovery = false;
        }

        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
            /* Connect to a remote instance of an object implementing the BasicInterface. */
                case CONNECT: {
                    org.alljoyn.bus.alljoyn.DaemonInit.PrepareDaemon(getApplicationContext());
                /*
                 * All communication through AllJoyn begins with a BusAttachment.
                 *
                 * A BusAttachment needs a name. The actual name is unimportant except for internal
                 * security. As a default we use the class name as the name.
                 *
                 * By default AllJoyn does not allow communication between devices (i.e. bus to bus
                 * communication). The second argument must be set to Receive to allow communication
                 * between devices.
                 */
                    mBus = new BusAttachment(getPackageName(), BusAttachment.RemoteMessage.Receive);
                
                /*
                 * Create a bus listener class
                 */
                    mBus.registerBusListener(new BusListener() {
                        @Override
                        public void foundAdvertisedName(String name, short transport, String namePrefix) {
                            logInfo(String.format("MyBusListener.foundAdvertisedName(%s, 0x%04x, %s)", name, transport, namePrefix));
                    	/*
                    	 * This client will only join the first service that it sees advertising
                    	 * the indicated well-known name.  If the program is already a member of 
                    	 * a session (i.e. connected to a service) we will not attempt to join 
                    	 * another session.
                    	 * It is possible to join multiple session however joining multiple 
                    	 * sessions is not shown in this sample. 
                    	 */
                            if(!mIsConnected) {
                                Message msg = obtainMessage(JOIN_SESSION);
                                msg.arg1 = transport;
                                msg.obj = name;
                                sendMessage(msg);
                            }
                        }
                    });

                /* To communicate with AllJoyn objects, we must connect the BusAttachment to the bus. */
                    Status status = mBus.connect();
                    logStatus("BusAttachment.connect()", status);
                    if (Status.OK != status) {
                        finish();
                        return;
                    }

                /*
                 * Now find an instance of the AllJoyn object we want to call.  We start by looking for
                 * a name, then connecting to the device that is advertising that name.
                 *
                 * In this case, we are looking for the well-known SERVICE_NAME.
                 */
                    status = mBus.findAdvertisedName(SERVICE_NAME);
                    logStatus(String.format("BusAttachement.findAdvertisedName(%s)", SERVICE_NAME), status);
                    if (Status.OK != status) {
                        finish();
                        return;
                    }

                    break;
                }
                case (JOIN_SESSION): {
            	/*
                 * If discovery is currently being stopped don't join to any other sessions.
                 */
                    if (mIsStoppingDiscovery) {
                        break;
                    }
                
                /*
                 * In order to join the session, we need to provide the well-known
                 * contact port.  This is pre-arranged between both sides as part
                 * of the definition of the chat service.  As a result of joining
                 * the session, we get a session identifier which we must use to 
                 * identify the created session communication channel whenever we
                 * talk to the remote side.
                 */
                    short contactPort = CONTACT_PORT;
                    SessionOpts sessionOpts = new SessionOpts();
                    sessionOpts.transports = (short)msg.arg1;
                    Mutable.IntegerValue sessionId = new Mutable.IntegerValue();

                    Status status = mBus.joinSession((String) msg.obj, contactPort, sessionId, sessionOpts, new SessionListener() {
                        @Override
                        public void sessionLost(int sessionId, int reason) {
                            mIsConnected = false;
                            logInfo(String.format("MyBusListener.sessionLost(sessionId = %d, reason = %d)", sessionId,reason));
                            mHandler.sendEmptyMessage(MESSAGE_START_PROGRESS_DIALOG);
                        }
                    });
                    logStatus("BusAttachment.joinSession() - sessionId: " + sessionId.value, status);

                    if (status == Status.OK) {
                	/*
                     * To communicate with an AllJoyn object, we create a ProxyBusObject.  
                     * A ProxyBusObject is composed of a name, path, sessionID and interfaces.
                     * 
                     * This ProxyBusObject is located at the well-known SERVICE_NAME, under path
                     * "/sample", uses sessionID of CONTACT_PORT, and implements the BasicInterface.
                     */
                        mProxyObj =  mBus.getProxyBusObject(SERVICE_NAME,
                                "/sample",
                                sessionId.value,
                                new Class<?>[] { BasicInterface.class });

                	/* We make calls to the methods of the AllJoyn object through one of its interfaces. */
                        mBasicInterface =  mProxyObj.getInterface(BasicInterface.class);

                        mSessionId = sessionId.value;
                        mIsConnected = true;
                        mHandler.sendEmptyMessage(MESSAGE_STOP_PROGRESS_DIALOG);
                    }
                    break;
                }
            
            /* Release all resources acquired in the connect. */
                case DISCONNECT: {
                    mIsStoppingDiscovery = true;
                    if (mIsConnected) {
                        Status status = mBus.leaveSession(mSessionId);
                        logStatus("BusAttachment.leaveSession()", status);
                    }
                    mBus.disconnect();
                    getLooper().quit();
                    break;
                }
            
            /*
             * Call the service's Cat method through the ProxyBusObject.
             *
             * This will also print the String that was sent to the service and the String that was
             * received from the service to the user interface.
             */
                case GETSENSORSCOUNT: {
                    try {
                        if (mBasicInterface != null) {
                            //sendUiMessage(MESSAGE_PING, msg.obj + " and " + msg.obj);
                            int reply = mBasicInterface.getSensorsCount();
                            sendUiMessage(MESSAGE_PING_REPLY, reply);
                        }
                    } catch (BusException ex) {
                        logException("BasicInterface.getSensorsCount()", ex);
                    }
                    break;
                }

                case GETNAME: {
                    try {
                        if (mBasicInterface != null) {
                            //sendUiMessage(MESSAGE_PING, msg.obj + " and " + msg.obj);
                            String name = mBasicInterface.getName((Integer) msg.obj);
                            sendUiMessage(MESSAGE_NAME, name);
                        }
                    } catch (BusException ex) {
                        logException("BasicInterface.getName()", ex);
                    }
                    break;
                }

                case GETTEMP: {
                    try {
                        if (mBasicInterface != null) {
                            //sendUiMessage(MESSAGE_PING, msg.obj + " and " + msg.obj);
                            int temp = mBasicInterface.getTemp((Integer) msg.obj);
                            sendUiMessage(MESSAGE_TEMP, temp);
                        }
                    } catch (BusException ex) {
                        logException("BasicInterface.getTemp()", ex);
                    }
                    break;
                }
                case GETOFF: {
                    try {
                        if (mBasicInterface != null) {
                            //sendUiMessage(MESSAGE_PING, msg.obj + " and " + msg.obj);
                            int var = mBasicInterface.lightOff();
                            sendUiMessage(MESSAGE_OFF, var);
                        }
                    } catch (BusException ex) {
                        logException("BasicInterface.lightOff()", ex);
                    }
                    break;
                }
                case GETFREQ0: {
                    try {
                        if (mBasicInterface != null) {
                            //sendUiMessage(MESSAGE_PING, msg.obj + " and " + msg.obj);
                            int var = mBasicInterface.getFreq0((Integer) msg.obj);
                            sendUiMessage(MESSAGE_FREQ0, var);
                        }
                    } catch (BusException ex) {
                        logException("BasicInterface.getFreq0()", ex);
                    }
                    break;
                }
                case GETFREQ1: {
                    try {
                        if (mBasicInterface != null) {
                            //sendUiMessage(MESSAGE_PING, msg.obj + " and " + msg.obj);
                            int var = mBasicInterface.getFreq1((Integer) msg.obj);
                            sendUiMessage(MESSAGE_FREQ1, var);
                        }
                    } catch (BusException ex) {
                        logException("BasicInterface.getFreq1()", ex);
                    }
                    break;
                }
                case GETCRITTEMP: {
                    try {
                        if (mBasicInterface != null) {
                            //sendUiMessage(MESSAGE_PING, msg.obj + " and " + msg.obj);
                            int var = mBasicInterface.getCritTemp((Integer) msg.obj);
                            sendUiMessage(MESSAGE_CRITTEMP, var);
                        }
                    } catch (BusException ex) {
                        logException("BasicInterface.getCritTemp()", ex);
                    }
                    break;
                }

                default:
                    break;
            }
        }

        /* Helper function to send a message to the UI thread. */
        private void sendUiMessage(int what, Object obj) {
            mHandler.sendMessage(mHandler.obtainMessage(what, obj));
        }
    }

    private void logStatus(String msg, Status status) {
        String log = String.format("%s: %s", msg, status);
        if (status == Status.OK) {
            Log.i(TAG, log);
        } else {
            Message toastMsg = mHandler.obtainMessage(MESSAGE_POST_TOAST, log);
            mHandler.sendMessage(toastMsg);
            Log.e(TAG, log);
        }
    }

    private void logException(String msg, BusException ex) {
        String log = String.format("%s: %s", msg, ex);
        Message toastMsg = mHandler.obtainMessage(MESSAGE_POST_TOAST, log);
        mHandler.sendMessage(toastMsg);
        Log.e(TAG, log, ex);
    }

    /*
     * print the status or result to the Android log. If the result is the expected
     * result only print it to the log.  Otherwise print it to the error log and
     * Sent a Toast to the users screen. 
     */
    private void logInfo(String msg) {
        Log.i(TAG, msg);
    }
}