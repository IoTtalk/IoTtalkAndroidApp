package tw.org.cic.morsensor_mobile;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ftdi.j2xx.D2xxManager;
import com.ftdi.j2xx.FT_Device;

import tw.org.cic.dataManage.DataTransform;
import tw.org.cic.dataManage.MorSensorParameter;

import static tw.org.cic.dataManage.DataTransform.bytesToHexString;

import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.content.Intent;
import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;

public class MainViewActivity extends Activity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v("MainActivity", "into");

        //setContentView(R.layout.activity_main_view);
        Intent intent = new Intent(MainViewActivity.this, Mainservice.class);
        startService(intent);
        //Toast.makeText(this, "MainView start", Toast.LENGTH_SHORT).show();
        finish();

    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        //Intent intent = new Intent(MainActivity.this, Countservice.class);
        //stopService(intent);
        Log.v("MainActivity", "out");
    }
    //TextView mytvx = (TextView) findViewById(R.id.tvx);
    //mytvx.setText("hihi");

}
