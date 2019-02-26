package tw.org.cic.morsensor_mobile;

//to internet

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.util.Log;



public class Csmapi {

    //Endpoint will change in DAN
    static String Endpoint = "http://"+"140.113.199.199"+":9999";
    int Timeout = 10;

    /*
    def register(mac_addr, profile, UsingSession=IoTtalk):
    r = UsingSession.post(
        ENDPOINT + '/' + mac_addr,
        json={'profile': profile}, timeout=TIMEOUT
    )
    if r.status_code != 200: raise CSMError(r.text)
    return True
     */

    static public boolean register(String mac_addr, JSONObject profile)throws JSONException,InterruptedIOException{
        //Response res = request("POST", url, tmp.toString());
        JSONObject json =new JSONObject();
        json.put("profile",profile);
        new Thread(new Runnable(){
            @Override
            public void run() {

            }
        }).start();
        try{
            URL url = new URL(Endpoint+"/"+mac_addr);
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            //post请求需要setDoOutput(true)，这个默认是false的。
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type","application/json");
            OutputStream output = conn.getOutputStream();
            output.write(json.toString().getBytes());

            //get response
            int code = conn.getResponseCode();
            Log.d("csmpai","code is "+code);
            InputStream input;
            if(code>=400) {//error code
                input = new BufferedInputStream(conn.getErrorStream());

            }
            else{
                input = new BufferedInputStream(conn.getInputStream());
            }


            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            String texts="";
            //String readstring = reader.readLine();
            String readstring;
            while((readstring = reader.readLine())!=null){
                texts = texts+readstring+"\n";
            }
            Log.e("csmpai","read end "+texts);
            reader.close();
            conn.disconnect();
            if(code!=200){
                Log.e("Csmpai","register code isn,t 200");
            }
            Log.d("Csmpai","register");
            return true;
        }
        catch (Exception e){
            Log.e("Csmpai","register failed "+e);
        }
        return false;
    }

    /*
    def push(mac_addr,df_name, data, UsingSession=IoTtalk):
    r = UsingSession.put(
    ENDPOINT + '/' + mac_addr + '/' + df_name,
    json={'data': data}, timeout=TIMEOUT
    )
            if r.status_code != 200: raise CSMError(r.text)
    return True
     */
    //lack parameter data
    static public boolean push(String mac_addr,String df_name,JSONArray data){
        try{
            JSONObject json = new JSONObject();
            json.put("data",data);
            URL url = new URL(Endpoint+"/"+mac_addr+"/"+df_name);
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("PUT");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type","application/json");
            OutputStream output = conn.getOutputStream();
            output.write(json.toString().getBytes());
            //get response
            int code = conn.getResponseCode();
            Log.v("csmapi push ","codenumber is "+code);

            InputStream input;
            if(code>=400) {//error code
                input = new BufferedInputStream(conn.getErrorStream());
            }
            else {
                input = new BufferedInputStream(conn.getInputStream());
            }
            if(code!=200){
                Log.e("csmpai push ","error code number");
                return false;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            String texts="";
            String readstring;
            while((readstring = reader.readLine())!=null){
                texts = texts+readstring+"\n";
            }
            Log.v("csmpai push ","texts "+texts);
            reader.close();
            conn.disconnect();
            return true;
        }
        catch(Exception e){
            Log.e("csmpai push ","error cuz "+e);
        }
        return false;
    }

    //lack return
    static public JSONArray pull(String mac_addr,String df_name){

        try{
            JSONObject json;
            URL url = new URL(Endpoint+"/"+mac_addr+"/"+df_name);
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("GET");
            //conn.setDoInput(true);
            int code = conn.getResponseCode();
            Log.v("csmpai pull ","code number is "+code);
            InputStream input;

            if(code>=400) {
                //Log.e("csmapi","pull failed code number is wrong");
                input = new BufferedInputStream(conn.getErrorStream());
            }
            else {
                input = new BufferedInputStream(conn.getInputStream());
            }

            if(code!=200){
                Log.v("csmpai pull","pull failed cuz code number");
                return null;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            String texts="";

            String readstring;
            while((readstring = reader.readLine()) !=null ){
                texts = texts+readstring+"\n";
            }
            Log.v("csmapi pull ",df_name+" texts is "+texts);
            reader.close();
            conn.disconnect();

            json = new JSONObject(texts);
            //Log.v("csmpai pull","json "+json);
            return json.getJSONArray("samples");
        }
        catch(Exception e){
            Log.e("csmpai pull","catch error "+e);
        }
        return null;
    }
}


