package tw.org.cic.tracking_mobile;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@SuppressWarnings("serial")
public class TrackingDAI {
    static TrackingDAI dai = new TrackingDAI();
    static TrackingDAN dan = new TrackingDAN();
    static String d_name = "";
    static String u_name = "yb";
    static Boolean is_sim = false;
    static String endpoint = "http://140.113.199.248:9999";
    static boolean is_resgister = false;
    static String info = "";


    public void add_shutdownhook() {
        Runtime.getRuntime().addShutdownHook(new Thread () {
            @Override
            public void run () {
                deregister();
            }
        });
    }

    /* deregister() */
    public void deregister() {
        dan.deregister();
    }




    /* The main() function */
    public static void main() {
        final JSONArray df_name_list = new JSONArray();
        df_name_list.put("GeoData-I");

        JSONObject profile = new JSONObject() {{
            try {
                put("d_name", dm_name); //deleted
                put("dm_name", dm_name); //deleted
                put("u_name", "yb");
                put("df_list", df_name_list);
                put("is_sim", false);
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }};

        String d_id = "TRACKING_TEST";
        /*Random rn = new Random();
        for (int i = 0; i < 12; i++) {
            int a = rn.nextInt(16);
            d_id += "0123456789ABCDEF".charAt(a);
        }*/

//        dan.init(dai, endpoint, d_id, profile);
        try {
            d_name = profile.getString("d_name");
        } catch (JSONException e) {
            e.printStackTrace();
        }
//        dai.add_shutdownhook();

    }

    /*--------------------------------------------------*/
    /* Customizable part */
    static String dm_name = "Tracking";


    public static void push(double lat, double lng, String username, Integer id, String time) {
        Log.v("dai_push", "in if");
        JSONArray data = new JSONArray();
        data.put(String.valueOf(lat));
        data.put(String.valueOf(lng));
        data.put(username);
        data.put(id);
        data.put(time);
        dan.push("GeoData-I", data);

    }

}
