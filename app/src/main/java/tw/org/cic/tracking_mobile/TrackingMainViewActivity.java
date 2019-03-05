package tw.org.cic.tracking_mobile;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import tw.org.cic.morsensor_mobile.R;


public class TrackingMainViewActivity extends Activity {
    static SharedPreferences settings;
    static final String data = "DATA";
    static final String STATE_NAME = "trackingName";
    static final String STATE_TIME = "trackingTime";
    static final String STATE_TRACK = "trackingStatus";
    static final String MAP_URL = "mapUrl";
    public static final String FIRST_RUN = "first";
    private boolean first;

    static LocationRequest  mLocationRequest;

    boolean isTrackingStart = false;

    String openMapUrl = TrackingConfig.mapURL;
    String setIdUrlAddress = TrackingConfig.mapURL+"/secure/_set_tracking_id";
    String setPwdUrlAddress = TrackingConfig.mapURL+"/static/passwd_tracking";

    EditText name;

    Button btnMapOpen;
    Button btnTrackStart;

    private BroadcastReceiver broadcastReceiver;
    private BroadcastReceiver trackStatusBroadcastReceiver;
    private static final int REQUEST_CHECK_SETTINGS = 0x1;
    private Context context;


    private Spinner spnTimeSelector;
    ArrayAdapter<String> adapterMins;
    String[] Mins = new String[] {"10", "20", "30", "40", "50", "60", "Unlimited"};
    private Spinner.OnItemSelectedListener spnTimeSelectorListener;
    String sel;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.v("TrackingMainActivity", "onCreate");

        context = this;

        setContentView(R.layout.activity_tracking_main_view);

        btnMapOpen = (Button) findViewById(R.id.openMap);
        btnMapOpen.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(openMapUrl));
                        startActivity(browserIntent);
                    }
                }
        );

        name = (EditText) findViewById(R.id.inputName);

        spnTimeSelector = (Spinner) findViewById(R.id.timeSelector);
        adapterMins = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, Mins){
            public View getView(int position, View convertView, ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                ((TextView) v).setGravity(Gravity.CENTER);
                return v;
            }
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View v = super.getDropDownView(position, convertView,parent);
                ((TextView) v).setGravity(Gravity.CENTER);
                return v;
            }
        };
        adapterMins.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnTimeSelector.setAdapter(adapterMins);


        spnTimeSelectorListener = new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                sel = parent.getSelectedItem().toString();
                Log.v("spnTimeSelectorListener", sel);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                sel = Mins[0];
                Log.v("onNothingSelected", sel);
            }
        };
        spnTimeSelector.setOnItemSelectedListener(spnTimeSelectorListener);

        Log.v("MainActivity", "into");
        enable_buttons();

        settings = getSharedPreferences(data,MODE_PRIVATE);
        first = settings.getBoolean(FIRST_RUN, true);
        if(!first)
            readData();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(broadcastReceiver == null) {
            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.v("onResume", intent.getExtras().get("coordinates").toString());
//                    ((TextView)findViewById(R.id.tv_lng)).setText(intent.getExtras().get("coordinates").toString());
                }
            };
        }
        registerReceiver(broadcastReceiver, new IntentFilter("location_update"));

        if( trackStatusBroadcastReceiver == null) {
            trackStatusBroadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    isTrackingStart = Boolean.valueOf(intent.getExtras().get("trackingStatus").toString());
                    Log.v("onResume", String.valueOf(isTrackingStart));
                    if(isTrackingStart == false) {
                        openMapUrl = TrackingConfig.mapURL;

                        new Handler(Looper.getMainLooper()).post(new Runnable(){
                            @Override
                            public void run() {
                                ((Button)findViewById(R.id.trackStart)).setText("Start");
                            }
                        });
                    }
                }
            };
        }
        registerReceiver(trackStatusBroadcastReceiver, new IntentFilter("tracking_status_update"));
    }

    public void saveData() {
        settings.edit()
                .putString(STATE_NAME, name.getText().toString())
                .putString(STATE_TIME, sel)
                .putBoolean(STATE_TRACK, isTrackingStart)
                .putBoolean(FIRST_RUN, false)
                .putString(MAP_URL, openMapUrl)
                .commit();

        Log.v("saveData", "saveData----------------------------------------------------------------------------------");
    }

    public void readData() {
        Log.v("readData", String.valueOf(settings));

        name.setText(settings.getString(STATE_NAME, ""));

        sel = settings.getString(STATE_TIME, "");
        int selectionPosition= adapterMins.getPosition(sel);
        spnTimeSelector.setSelection(selectionPosition);

        isTrackingStart = settings.getBoolean(STATE_TRACK, false);

        openMapUrl = settings.getString(MAP_URL, TrackingConfig.mapURL);

        Log.v("readData", "isTrackingStart: "+isTrackingStart);
        setTrackingBtnView();
        Log.v("readData", "readData----------------------------------------------------------------------------------");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.v("TrackingMainActivity", "onStop");

        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.v("TrackingMainActivity", "onDestroy");
        if(broadcastReceiver != null) {
            unregisterReceiver(broadcastReceiver);
        }
        if(trackStatusBroadcastReceiver != null) {
            unregisterReceiver(trackStatusBroadcastReceiver);
        }

        saveData();
    }

    private Boolean enable_buttons() {
        btnTrackStart = (Button)findViewById(R.id.trackStart);
        btnTrackStart.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(isTrackingStart) {
                            Log.v("btnTrackStartIf", String.valueOf(isTrackingStart));
                            stopTrackingService();
                        }
                        else {
                            Log.v("btnTrackStartElse", String.valueOf(isTrackingStart));
                            checkNetWork();
                        }

                    }
                }
        );

        return true;
    }


    private void stopTrackingService() {
        Intent i = new Intent(getApplicationContext(), TrackingService.class);
        stopService(i);
        setTrackingStartBtn();

        openMapUrl = TrackingConfig.mapURL;
    }

    private void startTrackingService(String trackingName, String trackingId, String trackingApp) {

        Intent i = new Intent(getApplicationContext(), TrackingService.class);
        i.putExtra("trackingName", trackingName);
        i.putExtra("trackingId", trackingId);
        i.putExtra("trackingApp", trackingApp);
        i.putExtra("trackingTime", sel);
        Log.v("startTrackingService", "sel"+sel);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(i);
        } else {
            startService(i);
        }

        setTrackingStartBtn();

        openMapUrl = TrackingConfig.mapURL+"?name="+trackingName+"&app="+trackingApp;
    }

    public void setTrackingBtnView() {
        if(isTrackingStart) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    ((Button) findViewById(R.id.trackStart)).setText("Stop");
                }
            });
        }
        else {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    ((Button) findViewById(R.id.trackStart)).setText("Start");
                }
            });
        }
    }

    public void setTrackingStartBtn() {

        if(isTrackingStart) {
            Log.v("setTrackingStartBtnIf", String.valueOf(isTrackingStart));
            new Handler(Looper.getMainLooper()).post(new Runnable(){
                @Override
                public void run() {
                    ((Button)findViewById(R.id.trackStart)).setText("Start");
                }
            });

            isTrackingStart = false;
        }
        else {
            Log.v("setTrackingStartBtnElse", String.valueOf(isTrackingStart));
            new Handler(Looper.getMainLooper()).post(new Runnable(){
                @Override
                public void run() {
                    ((Button)findViewById(R.id.trackStart)).setText("Stop");
                }
            });

            isTrackingStart = true;
        }
    }

    private void get_tracking_name() {
        String trackingName = name.getText().toString();
        String tmp;
        Log.v("get_tracking_name", trackingName);
        trackingName = trackingName.trim(); //delete leading and trailing whitespace.
        tmp = trackingName.replaceAll("[\\s]", "");

        if(!trackingName.equals(tmp)) {
            name.setError("Sorry! Only accept character, digits and decimal point.");
        }
        else {
            Log.v("get_tracking_name", trackingName + "go to set_tracking_id");
            set_tracking_id(trackingName);
        }
    }

    public void set_tracking_id(final String trackingName) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                String response;
                int resCode;
                InputStream in;
                try {
                    URL url = new URL(setIdUrlAddress+"?app=HumanTracking&name="+trackingName);
                    Log.i("set_tracking_id", url.toString());
                    HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                    conn.setAllowUserInteraction(false);
                    conn.setInstanceFollowRedirects(true);
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                    conn.setRequestProperty("Accept","application/json");
                    conn.connect();


                    Log.i("STATUS", String.valueOf(conn.getResponseCode()));
                    Log.i("MSG" , conn.getResponseMessage());

                    resCode = conn.getResponseCode();

                    if (resCode == HttpURLConnection.HTTP_OK) {
                        Log.v("set_tracking_id", "HTTP_OK in");
                        in = conn.getInputStream();

                        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line).append("\n");
                        }
                        in.close();
                        response = sb.toString();

                        Log.v("set_tracking_id", response);

                        JSONObject jsonResponse = new JSONObject(response);
                        JSONArray jsonResultArr = jsonResponse.getJSONArray("result");
                        JSONObject jsonIdObject = (JSONObject) jsonResultArr.get(0);
                        String trackingId = jsonIdObject.getString("id");
                        String trackingApp = jsonIdObject.getString("app_num");
                        Log.v("set_tracking_id", trackingId+" "+trackingApp);

                        set_tracking_pwd();

                        startTrackingService(trackingName, trackingId, trackingApp);
                    }
                    else {
                        checkNetWork();
                    }

                    conn.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        thread.start();
    }

    //get tracking device password
    public void set_tracking_pwd() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                String response;
                int resCode;
                InputStream in;
                try {
                    URL url = new URL(setPwdUrlAddress);
                    Log.i("set_tracking_pwd", url.toString());
                    HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                    conn.setAllowUserInteraction(false);
                    conn.setInstanceFollowRedirects(true);
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                    conn.setRequestProperty("Accept","application/json");
                    conn.connect();


                    Log.i("STATUS", String.valueOf(conn.getResponseCode()));
                    Log.i("MSG" , conn.getResponseMessage());

                    resCode = conn.getResponseCode();

                    if (resCode == HttpURLConnection.HTTP_OK) {
                        Log.v("set_tracking_pwd", "HTTP_OK in");
                        in = conn.getInputStream();

                        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line).append("\n");
                        }
                        in.close();
                        response = sb.toString();

                        Log.v("set_tracking_pwd", response);
                        TrackingConfig.trackingPWD = response;
                    }

                    conn.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        thread.start();
    }

    // lead user open GPS
    protected void gps_permissions() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(6000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);

        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());
        Log.v("GPS", "Comein");
        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                Log.v("GPS", "Success");
                get_tracking_name();
            }
        });

        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    // Location settings are not satisfied, but this can be fixed
                    // by showing the user a dialog.
                    try {
                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult().
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(TrackingMainViewActivity.this,
                                REQUEST_CHECK_SETTINGS);

                    } catch (IntentSender.SendIntentException sendEx) {
                        // Ignore the error.
                    }
                    catch (Exception err) {

                    }
                }
            }
        });
    }
    //catch user open GPS result
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                get_tracking_name();
                // The user picked a contact.
                // The Intent's data Uri identifies which contact was selected.

                // Do something with the contact here (bigger example below)
            }
            else if(resultCode == 0){
                gps_permissions();
            }
        }
    }

    // ask for GPS permission
    protected void runtime_permissions() {
        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, TrackingConfig.MY_PERMISSIONS_REQUEST_LOCATION);
    }
    // catch GPS permission result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.v("onRequestPermissions", "onRequestPermissionsResult");
        if(requestCode == TrackingConfig.MY_PERMISSIONS_REQUEST_LOCATION){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                Log.v("onRequestPermissions", "runtime_permissions if");
                gps_permissions();
            } else {
                Log.v("onRequestPermissions", "runtime_permissions else");
                runtime_permissions();
            }
        }
    }

    private void checkNetWork() {
        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

        if(isConnected) {
            Log.v("isConnected", "runtime_permissions");
            runtime_permissions();
        }
        else {
            new AlertDialog.Builder(this).setMessage("沒有網路")
                    .setPositiveButton("前往設定網路", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Intent callNetSettingIntent = new Intent(
                                    android.provider.Settings.ACTION_WIRELESS_SETTINGS);
                            Toast.makeText(context, "請前往開啟網路", Toast.LENGTH_LONG).show();
                            startActivity(callNetSettingIntent);
                        }
                    })
                    .show();
        }
    }

}
