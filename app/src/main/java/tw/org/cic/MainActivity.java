package tw.org.cic;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import tw.org.cic.morsensor_mobile.MainViewActivity;
import tw.org.cic.morsensor_mobile.R;
import tw.org.cic.tracking_mobile.TrackingMainViewActivity;

public class MainActivity extends Activity {
    Button btnMorSensor, btnTracking;
    public static Context aContext;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.index);
        Log.v("MainActivity", "into");
        aContext = this;
        btnMorSensor = (Button)findViewById(R.id.MorSensor);
        btnMorSensor.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(aContext, "Morsensor start", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(MainActivity.this, MainViewActivity.class);
                        startActivity(intent);
                        //finish();
                    }
                }
        );

        btnTracking = (Button)findViewById(R.id.Tracking);
        btnTracking.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(MainActivity.this, TrackingMainViewActivity.class);
                        startActivity(intent);
                    }
                }
        );


    }


}