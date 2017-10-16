/**
 * Example of using libmuse library on android.
 * Interaxon, Inc. 2016
 */

package com.choosemuse.example.libmuse;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.choosemuse.libmuse.Accelerometer;
import com.choosemuse.libmuse.AnnotationData;
import com.choosemuse.libmuse.ConnectionState;
import com.choosemuse.libmuse.Eeg;
import com.choosemuse.libmuse.LibmuseVersion;
import com.choosemuse.libmuse.MessageType;
import com.choosemuse.libmuse.Muse;
import com.choosemuse.libmuse.MuseArtifactPacket;
import com.choosemuse.libmuse.MuseConfiguration;
import com.choosemuse.libmuse.MuseConnectionListener;
import com.choosemuse.libmuse.MuseConnectionPacket;
import com.choosemuse.libmuse.MuseDataListener;
import com.choosemuse.libmuse.MuseDataPacket;
import com.choosemuse.libmuse.MuseDataPacketType;
import com.choosemuse.libmuse.MuseFileFactory;
import com.choosemuse.libmuse.MuseFileReader;
import com.choosemuse.libmuse.MuseFileWriter;
import com.choosemuse.libmuse.MuseListener;
import com.choosemuse.libmuse.MuseManagerAndroid;
import com.choosemuse.libmuse.MuseVersion;
import com.choosemuse.libmuse.Result;
import com.choosemuse.libmuse.ResultLevel;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This example will illustrate how to connect to a Muse headband,
 * register for and receive EEG data and disconnect from the headband.
 * Saving EEG data to a .muse file is also covered.
 *
 * For instructions on how to pair your headband with your Android device
 * please see:
 * http://developer.choosemuse.com/hardware-firmware/bluetooth-connectivity/developer-sdk-bluetooth-connectivity-2
 *
 * Usage instructions:
 * 1. Pair your headband if necessary.
 * 2. Run this project.
 * 3. Turn on the Muse headband.
 * 4. Press "Refresh". It should display all paired Muses in the Spinner drop down at the
 *    top of the screen.  It may take a few seconds for the headband to be detected.
 * 5. Select the headband you want to connect to and press "Connect".
 * 6. You should see EEG and accelerometer data as well as connection status,
 *    version information and relative alpha values appear on the screen.
 * 7. You can pause/resume data transmission with the button at the bottom of the screen.
 * 8. To disconnect from the headband, press "Disconnect"
 */
public class MainActivity extends Activity implements OnClickListener {

    /**
     * Tag used for logging purposes.
     */
    private final String TAG = "TestLibMuseAndroid";

    /**
     * The MuseManager is how you detect Muse headbands and receive notifications
     * when the list of available headbands changes.
     */
    private MuseManagerAndroid manager;

    /**
     * A Muse refers to a Muse headband.  Use this to connect/disconnect from the
     * headband, register listeners to receive EEG data and get headband
     * configuration and version information.
     */
    private Muse muse;

    /**
     * The ConnectionListener will be notified whenever there is a change in
     * the connection state of a headband, for example when the headband connects
     * or disconnects.
     *
     * Note that ConnectionListener is an inner class at the bottom of this file
     * that extends MuseConnectionListener.
     */
    private ConnectionListener connectionListener;

    /**
     * The DataListener is how you will receive EEG (and other) data from the
     * headband.
     *
     * Note that DataListener is an inner class at the bottom of this file
     * that extends MuseDataListener.
     */
    private DataListener dataListener;

    /**
     * Data comes in from the headband at a very fast rate; 220Hz, 256Hz or 500Hz,
     * depending on the type of headband and the preset configuration.  We buffer the
     * data that is read until we can update the UI.
     *
     * The stale flags indicate whether or not new data has been received and the buffers
     * hold the values of the last data packet received.  We are displaying the EEG, ALPHA_RELATIVE
     * and ACCELEROMETER values in this example.
     *
     * Note: the array lengths of the buffers are taken from the comments in
     * MuseDataPacketType, which specify 3 values for accelerometer and 6
     * values for EEG and EEG-derived packets.
     */

    //private final double[] eegBuffer = new double[6];
    //private boolean eegStale;

    //Alpha values
    private final double[] alphaBuffer = new double[6];
    private boolean alphaStale;
    //private final double[] alphaAbsoluteBuffer = new double[6];
    //private boolean alphaAbsoluteStale;
    //private final double[] alphaScore = new double[6];
    //private boolean alphaScoreStale;

    //Beta values
    private final double[] betaBuffer = new double[6];
    private boolean betaStale;
    //private final double[] betaAbsoluteBuffer = new double[6];
    //private boolean betaAbsoluteStale;
    //private final double[] betaScore = new double[6];
    //private boolean betaScoreStale;

    //Delta values
    //private final double[] deltaBuffer = new double[6];
    //private boolean deltaStale;
    //private final double[] deltaAbsoluteBuffer = new double[6];
    //private boolean deltaAbsoluteStale;
    //private final double[] deltaScore = new double[6];
    //private boolean deltaScoreStale;

    //Gamma Values
    //private final double[] gammaBuffer = new double[6];
    //private boolean gammaStale;
    //private final double[] gammaAbsoluteBuffer = new double[6];
    //private boolean gammaAbsoluteStale;
    //private final double[] gammaScore = new double[6];
    //private boolean gammaScoreStale;

    //Theta Values
    private final double[] thetaBuffer = new double[6];
    private boolean thetaStale;
    //private final double[] thetaAbsoluteBuffer = new double[6];
    //private boolean thetaAbsoluteStale;
    //private final double[] thetaScore = new double[6];
    //private boolean thetaScoreStale;

    //Accelerometer Values
    //private final double[] accelBuffer = new double[3];
    //private boolean accelStale;

    //Artifacts
    //Array to store the Blinks:
    ArrayList<Long> intArray = new ArrayList<Long>();
    private int blinkCountReal = 0;
    private boolean EyeClosed = false;
    private int BlinkCountAll = 0;
    private long lastblink = 0;
    // private int Blinks_per_minute = 0;
    private boolean blinkStale = false;
    private boolean blinkStaleLong = false;
    private boolean blinkStaleShort = false;
    private boolean BlinkAlert = false;

    //private int JawChlenchCounter = 0;
    private boolean HeadbandOn = false;

    //Electrode fitting values
    private final double[] HSIBuffer = new double[4];
    private boolean HSIStale;

    //Battery Values
    //private final double[] batteryBuffer = new double[3];
    //private boolean batteryStale;


    // Mean values per frequency band over all electrodes
    private double meanalpha = 0;
    private double meanbeta = 0;
    //private double meandelta = 0;
    private double meantheta = 0;
    //private double meangamma = 0;

    // Array to store the information whether threshold is exceeded (1) or not (0)
    ArrayList<Integer> ThresholdList = new ArrayList<Integer>(Collections.nCopies(1000, 0));

    // Drowsiness Score: (Alpha + Theta)/Beta
    private double drowsinessScore = 0;
    private boolean scoreStale;

    // Thresholds used for critical drowsiness detection
    private double Threshold = 7;
    private int ThresholdPeriod = 500;

    // EEG alert due to drowsiness detection:
    private boolean EEGAlert = false;

    /**
     * We will be updating the UI using a handler instead of in packet handlers because
     * packets come in at a very high frequency and it only makes sense to update the UI
     * at about 60fps. The update functions do some string allocation, so this reduces our memory
     * footprint and makes GC pauses less frequent/noticeable.
     */
    private final Handler handler = new Handler();

    /**
     * In the UI, the list of Muses you can connect to is displayed in a Spinner object for this example.
     * This spinner adapter contains the MAC addresses of all of the headbands we have discovered.
     */
    private ArrayAdapter<String> spinnerAdapter;

    /**
     * It is possible to pause the data transmission from the headband.  This boolean tracks whether
     * or not the data transmission is enabled as we allow the user to pause transmission in the UI.
     */
    private boolean dataTransmission = true;

    /**
     * To save data to a file, you should use a MuseFileWriter.  The MuseFileWriter knows how to
     * serialize the data packets received from the headband into a compact binary format.
     * To read the file back, you would use a MuseFileReader.
     */
    private final AtomicReference<MuseFileWriter> fileWriter = new AtomicReference<>();

    /**
     * We don't want file operations to slow down the UI, so we will defer those file operations
     * to a handler on a separate thread.
     */
    private final AtomicReference<Handler> fileHandler = new AtomicReference<>();


    //--------------------------------------
    // Lifecycle / Connection code


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // We need to set the context on MuseManagerAndroid before we can do anything.
        // This must come before other LibMuse API calls as it also loads the library.
        manager = MuseManagerAndroid.getInstance();
        manager.setContext(this);

        Log.i(TAG, "LibMuse version=" + LibmuseVersion.instance().getString());

        WeakReference<MainActivity> weakActivity =
                new WeakReference<MainActivity>(this);
        // Register a listener to receive connection state changes.
        connectionListener = new ConnectionListener(weakActivity);
        // Register a listener to receive data from a Muse.
        dataListener = new DataListener(weakActivity);
        // Register a listener to receive notifications of what Muse headbands
        // we can connect to.
        manager.setMuseListener(new MuseL(weakActivity));

        // Muse 2016 (MU-02) headbands use Bluetooth Low Energy technology to
        // simplify the connection process.  This requires access to the COARSE_LOCATION
        // or FINE_LOCATION permissions.  Make sure we have these permissions before
        // proceeding.
        ensurePermissions();

        // Load and initialize our UI.
        initUI();

        // Start up a thread for asynchronous file operations.
        // This is only needed if you want to do File I/O.
        //fileThread.start();

        // Start our asynchronous updates of the UI.
        handler.post(tickUi);
    }

    protected void onPause() {
        super.onPause();
        // It is important to call stopListening when the Activity is paused
        // to avoid a resource leak from the LibMuse library.
        manager.stopListening();
    }

    public boolean isBluetoothEnabled() {
        return BluetoothAdapter.getDefaultAdapter().isEnabled();
    }

    @Override
    public void onClick(View v) {

        if (v.getId() == R.id.refresh) {
            // The user has pressed the "Refresh" button.
            // Start listening for nearby or paired Muse headbands. We call stopListening
            // first to make sure startListening will clear the list of headbands and start fresh.
            manager.stopListening();
            manager.startListening();

            List<Muse> availableMuses = manager.getMuses();
            Spinner musesSpinner = (Spinner) findViewById(R.id.muses_spinner);

            // Check that we actually have something to connect to.
            if (availableMuses.size() > 0  || musesSpinner.getAdapter().getCount() > 0) {
                Button But = (Button)findViewById(R.id.refresh);
                But.setBackgroundResource(R.drawable.buttonconnected);
            }
            else {
                Button But = (Button)findViewById(R.id.refresh);
                But.setBackgroundResource(R.drawable.button);
            }
        }
        else if (v.getId() == R.id.connect) {

            // The user has pressed the "Connect" button to connect to
            // the headband in the spinner.

            // Listening is an expensive operation, so now that we know
            // which headband the user wants to connect to we can stop
            // listening for other headbands.
            manager.stopListening();

            List<Muse> availableMuses = manager.getMuses();
            Spinner musesSpinner = (Spinner) findViewById(R.id.muses_spinner);

            // Check that we actually have something to connect to.
            if (availableMuses.size() < 1 || musesSpinner.getAdapter().getCount() < 1) {
                Log.w(TAG, "There is nothing to connect to");
            }
            else {

                // Cache the Muse that the user has selected.
                muse = availableMuses.get(musesSpinner.getSelectedItemPosition());
                // Unregister all prior listeners and register our data listener to
                // receive the MuseDataPacketTypes we are interested in.  If you do
                // not register a listener for a particular data type, you will not
                // receive data packets of that type.
                muse.unregisterAllListeners();
                muse.registerConnectionListener(connectionListener);
                muse.registerDataListener(dataListener, MuseDataPacketType.EEG);

                muse.registerDataListener(dataListener, MuseDataPacketType.ALPHA_RELATIVE);
                //muse.registerDataListener(dataListener, MuseDataPacketType.ALPHA_ABSOLUTE);
                //muse.registerDataListener(dataListener, MuseDataPacketType.ALPHA_SCORE);

                muse.registerDataListener(dataListener, MuseDataPacketType.BETA_RELATIVE);
                //muse.registerDataListener(dataListener, MuseDataPacketType.BETA_ABSOLUTE);
                //muse.registerDataListener(dataListener, MuseDataPacketType.BETA_SCORE);

                //muse.registerDataListener(dataListener, MuseDataPacketType.GAMMA_RELATIVE);
                //muse.registerDataListener(dataListener, MuseDataPacketType.GAMMA_ABSOLUTE);
                //muse.registerDataListener(dataListener, MuseDataPacketType.GAMMA_SCORE);

                //muse.registerDataListener(dataListener, MuseDataPacketType.DELTA_RELATIVE);
                //muse.registerDataListener(dataListener, MuseDataPacketType.DELTA_ABSOLUTE);
                //muse.registerDataListener(dataListener, MuseDataPacketType.DELTA_SCORE);

                muse.registerDataListener(dataListener, MuseDataPacketType.THETA_RELATIVE);
                //muse.registerDataListener(dataListener, MuseDataPacketType.THETA_ABSOLUTE);
                //muse.registerDataListener(dataListener, MuseDataPacketType.THETA_SCORE);

                muse.registerDataListener(dataListener, MuseDataPacketType.HSI_PRECISION);
                //muse.registerDataListener(dataListener, MuseDataPacketType.ACCELEROMETER);
                //muse.registerDataListener(dataListener, MuseDataPacketType.BATTERY);
                //muse.registerDataListener(dataListener, MuseDataPacketType.DRL_REF);
                //muse.registerDataListener(dataListener, MuseDataPacketType.QUANTIZATION);

                //Register for Artifacts
                muse.registerDataListener(dataListener, MuseDataPacketType.ARTIFACTS);

                // Initiate a connection to the headband and stream the data asynchronously.
                muse.runAsynchronously();
            }

        }
        //else if (v.getId() == R.id.disconnect) {

            // The user has pressed the "Disconnect" button.
            // Disconnect from the selected Muse.
        //    if (muse != null) {
        //        muse.disconnect();
        //    }
        //}
        //else if (v.getId() == R.id.pause) {

            // The user has pressed the "Pause/Resume" button to either pause or
            // resume data transmission.  Toggle the state and pause or resume the
            // transmission on the headband.
        //    if (muse != null) {
        //        dataTransmission = !dataTransmission;
        //        muse.enableDataTransmission(dataTransmission);
        //    }
        //}
    }

    //--------------------------------------
    // Permissions

    /**
     * The ACCESS_COARSE_LOCATION permission is required to use the
     * Bluetooth Low Energy library and must be requested at runtime for Android 6.0+
     * On an Android 6.0 device, the following code will display 2 dialogs,
     * one to provide context and the second to request the permission.
     * On an Android device running an earlier version, nothing is displayed
     * as the permission is granted from the manifest.
     *
     * If the permission is not granted, then Muse 2016 (MU-02) headbands will
     * not be discovered and a SecurityException will be thrown.
     */
    private void ensurePermissions() {

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            // We don't have the ACCESS_COARSE_LOCATION permission so create the dialogs asking
            // the user to grant us the permission.

            DialogInterface.OnClickListener buttonListener =
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which){
                            dialog.dismiss();
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                    0);
                        }
                    };

            // This is the context dialog which explains to the user the reason we are requesting
            // this permission.  When the user presses the positive (I Understand) button, the
            // standard Android permission dialog will be displayed (as defined in the button
            // listener above).
            AlertDialog introDialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.permission_dialog_title)
                    .setMessage(R.string.permission_dialog_description)
                    .setPositiveButton(R.string.permission_dialog_understand, buttonListener)
                    .create();
            introDialog.show();
        }
    }


    //--------------------------------------
    // Listeners

    /**
     * You will receive a callback to this method each time a headband is discovered.
     * In this example, we update the spinner with the MAC address of the headband.
     */
    public void museListChanged() {
        final List<Muse> list = manager.getMuses();
        spinnerAdapter.clear();
        for (Muse m : list) {
            spinnerAdapter.add(m.getName() + " - " + m.getMacAddress());
        }
    }

    /**
     * You will receive a callback to this method each time there is a change to the
     * connection state of one of the headbands.
     * @param p     A packet containing the current and prior connection states
     * @param muse  The headband whose state changed.
     */
    public void receiveMuseConnectionPacket(final MuseConnectionPacket p, final Muse muse) {

        final ConnectionState current = p.getCurrentConnectionState();

        // Format a message to show the change of connection state in the UI.
        //final String status = p.getPreviousConnectionState() + " -> " + current;
        //Log.i(TAG, status);

        // Update the UI with the change in connection state.
        handler.post(new Runnable() {
            @Override
            public void run() {

                //final TextView statusText = (TextView) findViewById(R.id.con_status);
                //statusText.setText(status);

                if (current == ConnectionState.CONNECTED){
                    Button But = (Button)findViewById(R.id.connect);
                    But.setBackgroundResource(R.drawable.buttonconnected);
                }
                else if (current == ConnectionState.DISCONNECTED){
                    Button But = (Button)findViewById(R.id.connect);
                    But.setBackgroundResource(R.drawable.buttondisconnected);
                }
                else if (current == ConnectionState.CONNECTING){
                    View But = (Button)findViewById(R.id.connect);
                    But.setBackgroundResource(R.drawable.buttonconnecting);
                }
                final MuseVersion museVersion = muse.getMuseVersion();
                //final TextView museVersionText = (TextView) findViewById(R.id.version);
                // If we haven't yet connected to the headband, the version information
                // will be null.  You have to connect to the headband before either the
                // MuseVersion or MuseConfiguration information is known.
                //if (museVersion != null) {
                //    final String version = museVersion.getFirmwareType() + " - "
                //            + museVersion.getFirmwareVersion() + " - "
                //            + museVersion.getProtocolVersion();
                //    museVersionText.setText(version);
                //} else {
                //    museVersionText.setText(R.string.undefined);
                //}
            }
        });

        if (current == ConnectionState.DISCONNECTED) {
            Log.i(TAG, "Muse disconnected:" + muse.getName());
            // Save the data file once streaming has stopped.
            saveFile();
            // We have disconnected from the headband, so set our cached copy to null.
            this.muse = null;
        }
    }

    /**
     * You will receive a callback to this method each time the headband sends a MuseDataPacket
     * that you have registered.  You can use different listeners for different packet types or
     * a single listener for all packet types as we have done here.
     * @param p     The data packet containing the data from the headband (eg. EEG data)
     * @param muse  The headband that sent the information.
     */
    public void receiveMuseDataPacket(final MuseDataPacket p, final Muse muse) {
        writeDataPacketToFile(p);

        // valuesSize returns the number of data values contained in the packet.
        final long n = p.valuesSize();
        switch (p.packetType()) {
            //case EEG:
            //    assert(eegBuffer.length >= n);
            //    getEegChannelValues(eegBuffer,p);
            //    eegStale = true;
            //    break;

            //case ACCELEROMETER:
            //    assert(accelBuffer.length >= n);
            //    getAccelValues(p);
            //    accelStale = true;
            //    break;

            case ALPHA_RELATIVE:
                assert(alphaBuffer.length >= n);
                getEegChannelValues(alphaBuffer,p);
                meanalpha = calcMean(alphaBuffer);
               // FatigueThresholdsExceeded(meanalpha, meanbeta, meantheta, Threshold, ThresholdList, drowsinessScore);
                scoreStale = true;
                alphaStale = true;
                break;

            //case ALPHA_ABSOLUTE:
            //    assert(alphaAbsoluteBuffer.length >= n);
            //    getEegChannelValues(alphaAbsoluteBuffer,p);
            //    alphaAbsoluteStale = true;
            //    break;

            //case ALPHA_SCORE:
            //    assert(alphaScore.length >= n);
            //    getEegChannelValues(alphaScore,p);
            //    alphaScoreStale = true;
            //    break;

            case BETA_RELATIVE:
                assert(betaBuffer.length >= n);
                getEegChannelValues(betaBuffer,p);
                meanbeta = calcMean(betaBuffer);
               // FatigueThresholdsExceeded(meanalpha, meanbeta, meantheta, Threshold, ThresholdList, drowsinessScore);
                scoreStale = true;
                betaStale = true;
                break;

            //case BETA_ABSOLUTE:
            //    assert(betaAbsoluteBuffer.length >= n);
            //    getEegChannelValues(betaAbsoluteBuffer,p);
            //    betaAbsoluteStale = true;
            //    break;

            //case BETA_SCORE:
            //    assert(alphaAbsoluteBuffer.length >= n);
            //    getEegChannelValues(alphaAbsoluteBuffer,p);
            //    betaStale = true;
            //    break;

            //case GAMMA_RELATIVE:
            //    assert(gammaBuffer.length >= n);
            //    getEegChannelValues(gammaBuffer,p);
            //    gammaStale = true;
            //    break;

            //case GAMMA_ABSOLUTE:
            //    assert(gammaAbsoluteBuffer.length >= n);
            //    getEegChannelValues(alphaAbsoluteBuffer,p);
            //    gammaAbsoluteStale = true;
            //    break;

            //case GAMMA_SCORE:
            //    assert(gammaScore.length >= n);
            //    getEegChannelValues(gammaScore,p);
            //    gammaScoreStale = true;
            //    break;

            //case DELTA_RELATIVE:
            //    assert(deltaBuffer.length >= n);
            //    getEegChannelValues(deltaBuffer,p);
            //    deltaStale = true;
            //    break;

            //case DELTA_ABSOLUTE:
            //    assert(deltaAbsoluteBuffer.length >= n);
            //    getEegChannelValues(deltaAbsoluteBuffer,p);
            //    deltaAbsoluteStale = true;
            //    break;

            //case DELTA_SCORE:
            //    assert(deltaScore.length >= n);
            //    getEegChannelValues(deltaScore,p);
            //    deltaScoreStale = true;
            //    break;

            case THETA_RELATIVE:
                assert(thetaBuffer.length >= n);
                getEegChannelValues(thetaBuffer,p);
                meantheta = calcMean(thetaBuffer);
                scoreStale = true;
                thetaStale = true;
                break;

            //case THETA_ABSOLUTE:
            //    assert(thetaAbsoluteBuffer.length >= n);
            //    getEegChannelValues(thetaAbsoluteBuffer,p);
            //    thetaAbsoluteStale = true;
            //    break;

            //case THETA_SCORE:
            //    assert(thetaScore.length >= n);
            //    getEegChannelValues(thetaScore,p);
            //    thetaScoreStale = true;
            //    break;

            //A Value which indicates the fit of the Headband
            case HSI_PRECISION:
                assert(HSIBuffer.length >= n);
                getHSIChannelValues(HSIBuffer,p);
                HSIStale = true;
                break;


            //case BATTERY:
            //    assert(batteryBuffer.length >= n);
            //    break;

            //case DRL_REF:
            //    break;
            //case QUANTIZATION:
            //    break;
            //default:
            //    break;
        }

        FatigueThresholdsExceeded(meanalpha, meanbeta, meantheta, Threshold, ThresholdList, drowsinessScore);
        //System.out.println("Mean Alpha --   " + meanalpha);
        //System.out.println("Mean Beta --    " + meanbeta);
        //System.out.println("Mean Theta --    " + meantheta);

        if (!EEGAlert) {
            Alarm(ThresholdList, ThresholdPeriod);
            VibrationAlert(EEGAlert);
            VisualAlertEEG(EEGAlert);
            //SoundAlert(EEGAlert);
        }
    }

    /**
     * You will receive a callback to this method each time an artifact packet is generated if you
     * have registered for the ARTIFACTS data type.  MuseArtifactPackets are generated when
     * eye blinks are detected, the jaw is clenched and when the headband is put on or removed.
     * @param p     The artifact packet with the data from the headband.
     * @param muse  The headband that sent the information.
     */
    public void receiveMuseArtifactPacket(final MuseArtifactPacket p, final Muse muse) {

        //if (!BlinkAlert) {
            ArtifactHandler(p,muse);
            //SoundAlert(BlinkAlert);
        //}
    }

    public void ArtifactHandler(final MuseArtifactPacket p, final Muse muse){
        if (p.getBlink()) {

            // Muse detects 2 Artefacts per Eyeblink:
            // One for opening and one for closing the eyes
            BlinkCountAll++;

            // Get the actual number of Eyeblinks
            if ((p.getTimestamp() - lastblink) > 500000){
                blinkStale = true;
                //blinkStaleLong = true;
                long intervall = p.getTimestamp() - lastblink;
                lastblink = p.getTimestamp();
                blinkCountReal++;

                // Calculate Blinks per Minute:
                //if ((p.getTimestamp() - lastblinkminute) < 60000000) {

                //    Blinks_per_minute++;
                //}
                //else{
                //    Blinks_per_minute = 0;
                //}

                if(intArray.size()==3) {
                    long diff = intArray.get(2) - intArray.get(0);
                    intArray.remove(0);
                    if(diff < 1000000 && BlinkAlert==false)
                    {
                        BlinkAlert = true;
                        intArray.clear();
                    }
                }
                intArray.add((p.getTimestamp()));
            }
            //else {
                //blinkStaleShort = true;
            //}
        }
        //else if (p.getJawClench()) {
        //    JawChlenchCounter = JawChlenchCounter + 1;
        //}
        else if (p.getHeadbandOn()) {
            HeadbandOn = true;
        }
        else if (!p.getHeadbandOn()){
            HeadbandOn = false;
           // PopupWindowHeadbandOff(findViewById(R.id.main));
        }

        // System.out.println("Blink Buffer --    " + blinkCountReal);
        // System.out.println("Blink Count --    " + BlinkCountAll);
        // System.out.println("Blink Alert --    " + BlinkAlert);
    }


    //--------------------------------------
    // Fatigue Detection


    // Calculates the mean of an array
    public double calcMean(double[] values)
    {
        double sum = 0;
        int notnull = 0;
        for(int i=1; i<=2;i++)
        {
            if(values[i] != 0) {
                notnull = notnull + 1;
                sum = sum + values[i];
            }
        }
        if(notnull<1)
        {
            notnull = 1;
        }
        return sum/notnull;
    }


    //FatigueThresholdsExceeded: modifies ThresholdList -> if the fatigue threshold is exceeded,
    // corresponding value in ThresholdList is set to 1
    //
    private void FatigueThresholdsExceeded(double Alpha, double Beta, double Theta, double Threshold, ArrayList<Integer> ThresholdList, double Score){
        if (Beta != 0) {
            drowsinessScore = (Alpha + Theta) / Beta;
            //System.out.println("Score --    " + drowsinessScore);
            if (Score > Threshold) {
                //System.out.println("Above Threshold --    ");
                ThresholdList.add(1);
                ThresholdList.remove(0);
                //System.out.println("Size:"+ ThresholdList.size());
            } else {
                //System.out.println("Below Threshold --    ");
                ThresholdList.add(0);
                ThresholdList.remove(0);
                //System.out.println("Last:"+ ThresholdList.get(ThresholdList.size() - 1));
            }
        }
    }


    // Alarm: returns boolean --> if the fatigue threshold is exceeded more than
    // n times in the monitored period, Alarm is set to true
    //
    private void Alarm(ArrayList<Integer> ThresholdList, int ThresholdPeriod) {
        int sum = 0;
        //System.out.println("ThresholdList --    " + ThresholdList);
        for(int i = 0; i < ThresholdList.size(); i++) {
            sum += ThresholdList.get(i);
        }
        //System.out.println("Sum_Alarm --    " + sum);
        if (sum > ThresholdPeriod) {
            //System.out.println("AboveThresholdPeriod --    ");
            EEGAlert = true;
        }else {
            //System.out.println("BelowThresholdPeriod --    ");
            EEGAlert = false;
        }
        System.out.println("EEG Alert --    " + EEGAlert);
    }


    //Visual Alert
    private void VisualAlertEEG(boolean alarm) {
        if (alarm) {
            PopupWindowEEG(findViewById(R.id.main));
        }
    }

    //Vibration Alert if critical situation is detected
    private void VibrationAlert(boolean alarm){
        //System.out.println("VibrationAlarm --    " + alarm);
        if (alarm) {
            //System.out.println("GiveVibrAlert! --    ");
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            // Vibrate for 500 milliseconds
            v.vibrate(500);
        }
    }

    //Sound Alert
    private void SoundAlert(boolean alarm){
        if (alarm) {
            try {
                Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
                Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                r.play();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }



    /**
     * Helper methods to get different packet values.  These methods simply store the
     * data in the buffers for later display in the UI.
     *
     * getEegChannelValue can be used for any EEG or EEG derived data packet type
     * such as EEG, ALPHA_ABSOLUTE, ALPHA_RELATIVE or HSI_PRECISION.  See the documentation
     * of MuseDataPacketType for all of the available values.
     * Specific packet types like ACCELEROMETER, GYRO, BATTERY and DRL_REF have their own
     * getValue methods.
     */

    private void getEegChannelValues(double[] buffer, MuseDataPacket p) {
        buffer[0] = p.getEegChannelValue(Eeg.EEG1);
        buffer[1] = p.getEegChannelValue(Eeg.EEG2);
        buffer[2] = p.getEegChannelValue(Eeg.EEG3);
        buffer[3] = p.getEegChannelValue(Eeg.EEG4);
        buffer[4] = p.getEegChannelValue(Eeg.AUX_LEFT);
        buffer[5] = p.getEegChannelValue(Eeg.AUX_RIGHT);
    }


    private void getHSIChannelValues(double[] buffer, MuseDataPacket p){
        buffer[0] = p.getEegChannelValue(Eeg.EEG1);
        buffer[1] = p.getEegChannelValue(Eeg.EEG2);
        buffer[2] = p.getEegChannelValue(Eeg.EEG3);
        buffer[3] = p.getEegChannelValue(Eeg.EEG4);
    }

    //private void getAccelValues(MuseDataPacket p) {
    //    accelBuffer[0] = p.getAccelerometerValue(Accelerometer.X);
    //    accelBuffer[1] = p.getAccelerometerValue(Accelerometer.Y);
    //    accelBuffer[2] = p.getAccelerometerValue(Accelerometer.Z);
    //}


    //--------------------------------------
    // UI Specific methods

    /**
     * Initializes the UI of the example application.
     */
    private void initUI() {

        setContentView(R.layout.activity_main);
        Button refreshButton = (Button) findViewById(R.id.refresh);
        refreshButton.setOnClickListener(this);
        Button connectButton = (Button) findViewById(R.id.connect);
        connectButton.setOnClickListener(this);
        //Button disconnectButton = (Button) findViewById(R.id.disconnect);
        //disconnectButton.setOnClickListener(this);
        //Button pauseButton = (Button) findViewById(R.id.pause);
        //pauseButton.setOnClickListener(this);

        spinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
        Spinner musesSpinner = (Spinner) findViewById(R.id.muses_spinner);
        musesSpinner.setAdapter(spinnerAdapter);
    }


    /**
     * The runnable that is used to update the UI at 60Hz.
     *
     * We update the UI from this Runnable instead of in packet handlers
     * because packets come in at high frequency -- 220Hz or more for raw EEG
     * -- and it only makes sense to update the UI at about 60fps. The update
     * functions do some string allocation, so this reduces our memory
     * footprint and makes GC pauses less frequent/noticeable.
     */
    private final Runnable tickUi = new Runnable() {
        @Override
        public void run() {

            //if (eegStale) {

            //}
            if(blinkStale){
                updateBlinks();
                ImageView Eye = (ImageView)findViewById(R.id.Eye);

                Eye.setImageResource(R.drawable.eyeclosed);
                Eye.setImageResource(R.drawable.eyeopen);

                //updateBlinkIcon();
            }
            if (HSIStale) {
                updateQuality();
            }
            //if (alphaStale) {
                //updateAlpha();
                //updateMean();
            //}
            //if (betaStale){
                //updateBeta();
                //updateMean();
            //}
            //if (gammaStale){

            //}
            //if (deltaStale){

            //}
            //if (thetaStale){
                //updateTheta();
                //updateMean();
            //}
            if (scoreStale){
                updateScore();
            }

            handler.postDelayed(tickUi, 1000 / 60);
        }
    };

    /**
     * The following methods update the TextViews in the UI with the data
     * from the buffers.
     */
    private void updateBeta() {
        TextView Beta0 = (TextView)findViewById(R.id.Beta0);
        TextView Beta1 = (TextView)findViewById(R.id.Beta1);
        TextView Beta2 = (TextView)findViewById(R.id.Beta2);
        TextView Beta3 = (TextView)findViewById(R.id.Beta3);
        Beta0.setText(String.format("%6.2f", betaBuffer[0]));
        Beta1.setText(String.format("%6.2f", betaBuffer[1]));
        Beta2.setText(String.format("%6.2f", betaBuffer[2]));
        Beta3.setText(String.format("%6.2f", betaBuffer[3]));
    }

    private void updateTheta() {
        TextView Theta0 = (TextView)findViewById(R.id.Theta0);
        TextView Theta1 = (TextView)findViewById(R.id.Theta1);
        TextView Theta2 = (TextView)findViewById(R.id.Theta2);
        TextView Theta3 = (TextView)findViewById(R.id.Theta3);
        Theta0.setText(String.format("%6.2f", thetaBuffer[0]));
        Theta1.setText(String.format("%6.2f", thetaBuffer[1]));
        Theta2.setText(String.format("%6.2f", thetaBuffer[2]));
        Theta3.setText(String.format("%6.2f", thetaBuffer[3]));
    }

    private void updateScore(){
        TextView Score = (TextView)findViewById(R.id.Index);
        ProgressBar ScoreBar = (ProgressBar)findViewById(R.id.progressBar);

        if(drowsinessScore<4){
            Drawable bar = getResources().getDrawable(R.drawable.score);
            ScoreBar.setProgressDrawable(bar);
            Score.setTextColor(Color.parseColor("#27ae60"));
        }
        else if (drowsinessScore<6){
            Drawable bar = getResources().getDrawable(R.drawable.scoremedium);
            ScoreBar.setProgressDrawable(bar);
            Score.setTextColor(Color.parseColor("#f1c40f"));
        }
        else{
            Drawable bar = getResources().getDrawable(R.drawable.scorehigh);
            ScoreBar.setProgressDrawable(bar);
            Score.setTextColor(Color.parseColor("#e64c3c"));
        }

        Score.setText(String.format("%6.2f", drowsinessScore));
        ScoreBar.setProgress((int) drowsinessScore);
    }

    private void updateBlinks(){
        TextView Score = (TextView)findViewById(R.id.Blinks);
        Score.setText(String.format("%d", blinkCountReal));
    }

    private void updateBlinkIcon(){
        ImageView Eye = (ImageView)findViewById(R.id.Eye);
        Eye.setImageResource(R.drawable.eyeclosed);
        Eye.setImageResource(R.drawable.eyeopen);
    }


private void updateQuality() {
        ImageView Quality0 = (ImageView) findViewById(R.id.E1);
        ImageView Quality1 = (ImageView) findViewById(R.id.E2);
        ImageView Quality2 = (ImageView) findViewById(R.id.E3);
        ImageView Quality3 = (ImageView) findViewById(R.id.E4);

        switch ((int) HSIBuffer[0]){
            case 1:
                Quality0.setImageResource(R.drawable.circle);
            //    Quality0.setVisibility(View.GONE);
                break;
            case 2:
                Quality0.setImageResource(R.drawable.circle);
            //    Quality0.setVisibility(View.GONE);
                break;
            case 4:
                Quality0.setVisibility(View.VISIBLE);
                Quality0.setImageResource(R.drawable.circlered);
                break;
        }
        switch ((int) HSIBuffer[1]){
            case 1:
                Quality1.setImageResource(R.drawable.circle);
            //    Quality1.setVisibility(View.GONE);
                break;
            case 2:
                Quality1.setImageResource(R.drawable.circle);
            //    Quality1.setVisibility(View.GONE);
                break;
            case 4:
                Quality1.setVisibility(View.VISIBLE);
                Quality1.setImageResource(R.drawable.circlered);
                break;
        }
        switch ((int) HSIBuffer[2]){
            case 1:
                Quality2.setImageResource(R.drawable.circle);
            //    Quality2.setVisibility(View.GONE);
                break;
            case 2:
                Quality2.setImageResource(R.drawable.circle);
            //    Quality2.setVisibility(View.GONE);
                break;
            case 4:
                Quality2.setVisibility(View.VISIBLE);
                Quality2.setImageResource(R.drawable.circlered);
                break;
        }
        switch ((int) HSIBuffer[3]){
            case 1:
                Quality3.setImageResource(R.drawable.circle);
            //    Quality3.setVisibility(View.GONE);
                break;
            case 2:
               Quality3.setImageResource(R.drawable.circleyellow);
            //   Quality3.setVisibility(View.GONE);
                break;
            case 4:
                Quality3.setVisibility(View.VISIBLE);
                Quality3.setImageResource(R.drawable.circlered);
                break;
        }
    }

    private void updateAlpha() {
        TextView elem1 = (TextView)findViewById(R.id.elem1);
        elem1.setText(String.format("%6.2f", alphaBuffer[0]));
        TextView elem2 = (TextView)findViewById(R.id.elem2);
        elem2.setText(String.format("%6.2f", alphaBuffer[1]));
        TextView elem3 = (TextView)findViewById(R.id.elem3);
        elem3.setText(String.format("%6.2f", alphaBuffer[2]));
        TextView elem4 = (TextView)findViewById(R.id.elem4);
        elem4.setText(String.format("%6.2f", alphaBuffer[3]));
    }

    private void updateMean() {
        TextView elem1 = (TextView)findViewById(R.id.alphamean);
        elem1.setText(String.format("%6.2f", meanalpha));
        TextView elem2 = (TextView)findViewById(R.id.betamean);
        elem2.setText(String.format("%6.2f", meanbeta));
        TextView elem3 = (TextView)findViewById(R.id.thetamean);
        elem3.setText(String.format("%6.2f", meantheta));
    }

    public void PopupWindowEEG(View view) {

        // get a reference to the already created main layout
        LinearLayout mainLayout = (LinearLayout) findViewById(R.id.main);

        // inflate the layout of the popup window
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.popup_window, null);

        // create the popup window
        int width = LinearLayout.LayoutParams.FILL_PARENT;
        int height = LinearLayout.LayoutParams.FILL_PARENT;
        boolean focusable = true; // lets taps outside the popup also dismiss it
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, focusable);

        // show the popup window
        popupWindow.showAtLocation(mainLayout, Gravity.CENTER, 0, 0);

        // dismiss the popup window when touched
        popupView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                popupWindow.dismiss();
                return true;
            }
        });
    }

    public void PopupWindowHeadbandOff(View view) {

        // get a reference to the already created main layout
        LinearLayout mainLayout = (LinearLayout) findViewById(R.id.main);

        // inflate the layout of the popup window
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.headband_off, null);

        // create the popup window
        int width = LinearLayout.LayoutParams.FILL_PARENT;
        int height = LinearLayout.LayoutParams.FILL_PARENT;
        boolean focusable = true; // lets taps outside the popup also dismiss it
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, focusable);

        // show the popup window
        popupWindow.showAtLocation(mainLayout, Gravity.CENTER, 0, 0);

        // dismiss the popup window when touched
        popupView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                popupWindow.dismiss();
                return true;
            }
        });
    }

    //--------------------------------------
    // File I/O

    /**
     * We don't want to block the UI thread while we write to a file, so the file
     * writing is moved to a separate thread.
     */
    private final Thread fileThread = new Thread() {
        @Override
        public void run() {
            Looper.prepare();
            fileHandler.set(new Handler());
            final File dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            final File file = new File(dir, "new_muse_file.muse" );
            // MuseFileWriter will append to an existing file.
            // In this case, we want to start fresh so the file
            // if it exists.
            if (file.exists()) {
                file.delete();
            }
            Log.i(TAG, "Writing data to: " + file.getAbsolutePath());
            fileWriter.set(MuseFileFactory.getMuseFileWriter(file));
            Looper.loop();
        }
    };

    /**
     * Writes the provided MuseDataPacket to the file.  MuseFileWriter knows
     * how to write all packet types generated from LibMuse.
     * @param p     The data packet to write.
     */
    private void writeDataPacketToFile(final MuseDataPacket p) {
        Handler h = fileHandler.get();
        if (h != null) {
            h.post(new Runnable() {
                @Override
                public void run() {
                    fileWriter.get().addDataPacket(0, p);
                }
            });
        }
    }

    /**
     * Flushes all the data to the file and closes the file writer.
     */
    private void saveFile() {
        Handler h = fileHandler.get();
        if (h != null) {
            h.post(new Runnable() {
                @Override public void run() {
                    MuseFileWriter w = fileWriter.get();
                    // Annotation strings can be added to the file to
                    // give context as to what is happening at that point in
                    // time.  An annotation can be an arbitrary string or
                    // may include additional AnnotationData.
                    w.addAnnotationString(0, "Disconnected");
                    w.flush();
                    w.close();
                }
            });
        }
    }

    /**
     * Reads the provided .muse file and prints the data to the logcat.
     * @param name  The name of the file to read.  The file in this example
     *              is assumed to be in the Environment.DIRECTORY_DOWNLOADS
     *              directory.
     */
    private void playMuseFile(String name) {

        File dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        File file = new File(dir, name);

        final String tag = "Muse File Reader";

        if (!file.exists()) {
            Log.w(tag, "file doesn't exist");
            return;
        }

        MuseFileReader fileReader = MuseFileFactory.getMuseFileReader(file);

        // Loop through each message in the file.  gotoNextMessage will read the next message
        // and return the result of the read operation as a Result.
        Result res = fileReader.gotoNextMessage();
        while (res.getLevel() == ResultLevel.R_INFO && !res.getInfo().contains("EOF")) {

            MessageType type = fileReader.getMessageType();
            int id = fileReader.getMessageId();
            long timestamp = fileReader.getMessageTimestamp();

            Log.i(tag, "type: " + type.toString() +
                    " id: " + Integer.toString(id) +
                    " timestamp: " + String.valueOf(timestamp));

            switch(type) {
                // EEG messages contain raw EEG data or DRL/REF data.
                // EEG derived packets like ALPHA_RELATIVE and artifact packets
                // are stored as MUSE_ELEMENTS messages.
                case EEG:
                case BATTERY:
                case ACCELEROMETER:
                case QUANTIZATION:
                case GYRO:
                case MUSE_ELEMENTS:
                    MuseDataPacket packet = fileReader.getDataPacket();
                    Log.i(tag, "data packet: " + packet.packetType().toString());
                    break;
                case VERSION:
                    MuseVersion version = fileReader.getVersion();
                    Log.i(tag, "version" + version.getFirmwareType());
                    break;
                case CONFIGURATION:
                    MuseConfiguration config = fileReader.getConfiguration();
                    Log.i(tag, "config" + config.getBluetoothMac());
                    break;
                case ANNOTATION:
                    AnnotationData annotation = fileReader.getAnnotation();
                    Log.i(tag, "annotation" + annotation.getData());
                    break;
                default:
                    break;
            }

            // Read the next message.
            res = fileReader.gotoNextMessage();
        }
    }

    //--------------------------------------
    // Listener translators
    //
    // Each of these classes extend from the appropriate listener and contain a weak reference
    // to the activity.  Each class simply forwards the messages it receives back to the Activity.
    class MuseL extends MuseListener {
        final WeakReference<MainActivity> activityRef;

        MuseL(final WeakReference<MainActivity> activityRef) {
            this.activityRef = activityRef;
        }

        @Override
        public void museListChanged() {
            activityRef.get().museListChanged();
        }
    }

    class ConnectionListener extends MuseConnectionListener {
        final WeakReference<MainActivity> activityRef;

        ConnectionListener(final WeakReference<MainActivity> activityRef) {
            this.activityRef = activityRef;
        }

        @Override
        public void receiveMuseConnectionPacket(final MuseConnectionPacket p, final Muse muse) {
            activityRef.get().receiveMuseConnectionPacket(p, muse);
        }
    }

    class DataListener extends MuseDataListener {
        final WeakReference<MainActivity> activityRef;

        DataListener(final WeakReference<MainActivity> activityRef) {
            this.activityRef = activityRef;
        }

        @Override
        public void receiveMuseDataPacket(final MuseDataPacket p, final Muse muse) {
            activityRef.get().receiveMuseDataPacket(p, muse);
        }

        @Override
        public void receiveMuseArtifactPacket(final MuseArtifactPacket p, final Muse muse) {
            activityRef.get().receiveMuseArtifactPacket(p, muse);
        }
    }
}

