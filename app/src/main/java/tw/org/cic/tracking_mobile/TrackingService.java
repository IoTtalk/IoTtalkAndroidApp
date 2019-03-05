package tw.org.cic.tracking_mobile;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import tw.org.cic.morsensor_mobile.R;

import static tw.org.cic.tracking_mobile.TrackingMainViewActivity.settings;

public class TrackingService extends Service {
    private String trackingName;
    private String trackingTime;
    private int trackingId;
    private int trackingApp;
    private boolean isWebOpen = false;

    private String ANDROID_CHANNEL_ID = "1";
    private String CHANNEL_ID = "TrackingNow";
    private String CHANNEL_ID2 = "TrackingEnd";

    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback;

    Intent timeNotificationIntent;
    PendingIntent timePendingIntent;
    Notification.Builder timeBuilder;
    NotificationManager timeNotificationManager;
    int TIME_NOTIFICATION_ID = 2;

    Notification.Builder builder;
    int NOTIFICATION_ID = 1;
    Notification notification;

    private boolean isFirstLoc = true;
    private double latitude; // degree
    private double longitude; // degree

    private CountDownTimer trackingTimer;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            builder = new Notification.Builder(this, ANDROID_CHANNEL_ID)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText("Tracking")
                    .setAutoCancel(true);

            notification = builder.build();
            startForeground(NOTIFICATION_ID, notification);


        } else {

            builder = new Notification.Builder(this)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText("test")
                    .setPriority(Notification.PRIORITY_DEFAULT)
                    .setAutoCancel(true);

            notification = builder.build();

            startForeground(NOTIFICATION_ID, notification);
        }

        trackingName = intent.getExtras().getString("trackingName");
        trackingId = Integer.parseInt(intent.getExtras().getString("trackingId"));
        trackingApp = Integer.parseInt(intent.getExtras().getString("trackingApp"));
        trackingTime = intent.getExtras().getString("trackingTime");
        Log.v("trackingTime", "trackingTime1 "+ isNumeric(trackingTime));
        setTrackingTime();
        return START_NOT_STICKY;
    }

    @SuppressLint("ResourceAsColor")
    @Override
    public void onCreate() {
        super.onCreate();
        Log.v("ServiceonCreate", "ServiceonCreate");
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mFusedLocationClient.getLastLocation()
                .addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            // Logic to handle location object
                            Log.v("getLastLocation", String.valueOf(location));
                            setCurrentLocation(location);
                        }
                    }
                });

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    reStartLocationUpdate();
                    return;
                }

                for (Location location : locationResult.getLocations()) {
                    Log.v("LocationCallback", String.valueOf(location) + "\n" + location.getAccuracy() + "\n" + location.getSpeed());
                    if(location.getAccuracy() >= 50) {
                        reStartLocationUpdate();
                        return;
                    }
                    setCurrentLocation(location);
                }
            }

            ;
        };

        startLocationUpdates();

        createNotificationChannel();

        timeNotificationIntent = new Intent(this, TrackingMainViewActivity.class);
        timeNotificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        timePendingIntent = PendingIntent.getActivity(this, 0, timeNotificationIntent, 0);

        timeBuilder = new Notification.Builder(this)
                .setSmallIcon(R.mipmap.iottalk_icon)
                .setColor(ContextCompat.getColor(this, R.color.iottalk_icon_color))
                .setContentTitle("Tracking Remaining Time")
                .setContentText("")
                .setChannelId(CHANNEL_ID)
                .setPriority(Notification.PRIORITY_MAX)
                .setContentIntent(timePendingIntent)
                .setOngoing(true)
                .setAutoCancel(true);

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                //code to do the HTTP request
                TrackingDAI.main();
                Log.v("Thread", "TrackingDAI.main finish");
            }
        });
        thread.start();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();

        if(trackingTimer != null) {
            trackingTimer.cancel();
            timeNotificationManager.cancel(TIME_NOTIFICATION_ID);
            stopForeground(true);
            trackingTimer = null;
        }

        Log.v("onDestroy", "onDestroy");
    }

    private void setTrackingTime() {
        if(isNumeric(trackingTime)) {
            final int countdownMins = Integer.parseInt(trackingTime);
            Log.v("timeThread", "CountDownTimer start " + countdownMins);
            trackingTimer = new CountDownTimer(countdownMins*1000*60, 1000) {

                public void onTick(long millisUntilFinished) {
                    long remainMins = millisUntilFinished / 1000 / 60;
                    long remainSeconds = millisUntilFinished / 1000 - remainMins*60;
                    Log.v("setTrackingTime", remainMins + ":" + remainSeconds);

                    timeBuilder.setContentText(remainMins + ":" + remainSeconds);
                    timeNotificationManager.notify(TIME_NOTIFICATION_ID, timeBuilder.build());
                }

                public void onFinish() {
                    // notificationId is a unique int for each notification that you must define
                    timeBuilder.setContentTitle("Tracking End")
                               .setContentText("Please tap here if you want to track again.")
                               .setChannelId(CHANNEL_ID2)
                               .setOngoing(false);
                    timeNotificationManager.notify(TIME_NOTIFICATION_ID, timeBuilder.build());
                    stopForeground(false);

                    Boolean isTracking;
                    isTracking = settings.getBoolean("trackingStatus", false);
                    Log.v("setTrackingTime", String.valueOf(isTracking));


                    Log.v("setTrackingTime", String.valueOf(settings));
                    settings.edit().putBoolean("trackingStatus", false).commit();


                    isTracking = settings.getBoolean("trackingStatus", false);
                    Log.v("setTrackingTime", String.valueOf(isTracking));

                    Intent i = new Intent("tracking_status_update");
                    i.putExtra("trackingStatus", isTracking);
                    sendBroadcast(i);

                    trackingTimer = null;
                    stopSelf();
                }

            };

            trackingTimer.start();

        }
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //NotificationChannel One
            CharSequence name = "TrackingNow";
            String description = "TrackingNow";
            int importance = NotificationManager.IMPORTANCE_LOW;
            //int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setSound(null, null);
            channel.setDescription(description);
            channel.enableLights(false);
            //channel.enableVibration(true);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            timeNotificationManager = getSystemService(NotificationManager.class);
            timeNotificationManager.createNotificationChannel(channel);
            Log.v("NotificationChannel", "createNotificationChannel success");

            //NotificationChannel Two
            CharSequence name2 = "TrackingEnd";
            String description2 = "TrackingEnd";
            int importance2 = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel2 = new NotificationChannel(CHANNEL_ID2, name2, importance2);
            channel2.setDescription(description2);
            channel2.enableLights(true);
            channel2.enableVibration(true);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            //timeNotificationManager = getSystemService(NotificationManager.class);
            timeNotificationManager.createNotificationChannel(channel2);
            Log.v("NotificationChannel", "createNotificationChannel success");
        }
    }

    private void setCurrentLocation(Location location) {
        if(isFirstLoc) {
            latitude = location.getLatitude();
            longitude = location.getLongitude();
            isFirstLoc = false;
        }

        if(location.getSpeed() > 0.6) {
            latitude = location.getLatitude();
            longitude = location.getLongitude();
        }

        Intent i = new Intent("location_update");
        i.putExtra("coordinates", location.getSpeed() + ", " + location.getAccuracy() +", "+location.getAltitude());
        sendBroadcast(i);
        Log.v("onchange", location.getProvider()+" "+Double.toString(latitude)+" "+Double.toString(longitude));
        Thread thread = new Thread(new Runnable(){
            @Override
            public void run(){
                //code to do the HTTP request
                TrackingDAI.push(latitude, longitude, trackingName, trackingId, getCurrentLocalDateTimeStamp());
                Log.v("Thread", "TrackingDAI.GeoData finish");
            }
        });
        thread.start();

        if(!isWebOpen) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(TrackingConfig.mapURL+"?name="+trackingName+"&app="+trackingApp));
            startActivity(browserIntent);
            isWebOpen = true;
        }
    }

    public String getCurrentLocalDateTimeStamp() {
        //String date = String.valueOf(android.text.format.DateFormat.format("yyyy-MM-dd HH:mm:ss", new java.util.Date()));
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // HH是24小時制，hh是12小時制
        sdf.setTimeZone(TimeZone.getTimeZone("GMT+8") ); //設定為台灣時間
        return sdf.format(date);
    }

    public static boolean isNumeric(String str)
    {
        try
        {
            double d = Double.parseDouble(str);
        }
        catch(NumberFormatException nfe)
        {
            return false;
        }
        return true;
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mFusedLocationClient.requestLocationUpdates(TrackingMainViewActivity.mLocationRequest,
                mLocationCallback,
                null /* Looper */);

        Log.v("startLocationUpdates", "startLocationUpdates");
    }

    private void stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        Log.v("stopLocationUpdates", "stopLocationUpdates");
    }

    private void reStartLocationUpdate() {
        stopLocationUpdates();
        startLocationUpdates();
        Log.v("reStartLocationUpdate", "reStartLocationUpdate***************************************************");
    }
}

