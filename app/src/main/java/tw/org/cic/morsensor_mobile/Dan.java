package tw.org.cic.morsensor_mobile;

import org.json.JSONObject;
import org.json.JSONException;
import org.json.JSONArray;

import java.io.InterruptedIOException;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import android.util.Log;
import java.net.SocketException;
import java.util.Enumeration;


public class Dan extends Thread{

    public class ControlChannel extends Thread{
        JSONArray ch = new JSONArray();
        String df_status;
        public void run(){
            while(true){
                try{
                    Thread.sleep(2000);
                    //CH = csmapi.pull(MAC,'__Ctl_O__', NewSession)
                    //Log.v("dan control ","mac addr "+mac_addr);
                    ch = Csmapi.pull(mac_addr,"__Ctl_O__");
                    //Log.v("dan control ","ch is "+ch);
                    if(ch!=null){
                        if(c_timestamp.equals(ch.getJSONArray(0).getString(0)))
                            continue;
                        c_timestamp = ch.getJSONArray(0).getString(0);
                        state = ch.getJSONArray(0).getJSONArray(1).getString(0);
                        //Log.v("dan control ","state is "+state);

                        if(state.equals("SET_DF_STATUS")){
                            JSONArray add = new JSONArray();
                            JSONObject addobj = new JSONObject();
                            addobj.put("cmd_params",ch.getJSONArray(0).getJSONArray(1).getJSONObject(1).getJSONArray("cmd_params"));
                            add.put(0,"SET_DF_STATUS_RSP");
                            add.put(1,addobj);
                            Csmapi.push(mac_addr,"__Ctl_I__",add);
                            //df_status = ch.getJSONArray(0).getJSONArray(1).getJSONObject(1).getJSONArray("cmd_params").getString(0);
                        }
                    }
                }
                catch(Exception e){
                    Log.e("dan control ","error cuz "+e);
                }
            }
        }
    }

    /*
        profile = {
        'd_name': None,
        'dm_name': 'MorSensor',
        'u_name': 'yb',
        'is_sim': False,
        'df_list': ['Acceleration', 'Temperature'],
    }
         */
    JSONObject profile;
    JSONArray dflist;
    String d_name;
    String state = "SUSPEND";
    String mac_addr ;
    String dm_name = "mobile_mor";
    String[] timestamp;
    String[] df_list;
    String c_timestamp="";
    String [] selected_df;



    public void init(){

        dflist = new JSONArray();
        //dflist.put("humi");
        //dflist.put("alcohol");
        //dflist.put("uv");
        dflist.put("mobile_alc");
        dflist.put("mobile_humi");
        dflist.put("mobile_uv");
        dflist.put("mobile_alc_o");
        dflist.put("mobile_uv_o");
        dflist.put("mobile_humi_o");

        //mac_addr = get_mac_addr();

        Random random = new Random();
        //profile['d_name']= str(int(random.uniform(1, 100)))+'.'+ profile['dm_name']
        d_name = Integer.toString(random.nextInt(100)+1)+"."+dm_name;

        try{
            profile = new JSONObject(){{
                put("d_name",d_name);
                put("dm_name",dm_name);
                put("u_name","yb");
                put("is_sim",false);
                put("df_list",dflist);
            }
            };
            Log.v("DAN","init success");
        }
        catch(Exception e){
            Log.e("DAN","init failed");
        }

    }
    /*
    def get_mac_addr():
    from uuid import getnode
    mac = getnode()
    mac = ''.join(("%012X" % mac)[i:i+2] for i in range(0, 12, 2))
    return mac
     */

    public String get_mac_addr() throws SocketException {
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        Log.d("dan get_mac", " interfaceName = " + interfaces );
        while (interfaces.hasMoreElements()) {
            NetworkInterface netWork = interfaces.nextElement();
            byte[] by = netWork.getHardwareAddress();
            if (by == null || by.length == 0) {
                continue;
            }
            StringBuilder builder = new StringBuilder();
            for (byte b : by) {
                builder.append(String.format("%02X:", b));
            }
            if (builder.length() > 0) {
                builder.deleteCharAt(builder.length() - 1);
            }
            String mac = builder.toString();
            Log.d("dan get_mac", "interfaceName="+netWork.getName()+", mac="+mac);
            return mac;
            /*if (netWork.getName().equals("wlan0")) {
                Log.d("dan get_mac", " interfaceName ="+netWork.getName()+", mac="+mac);
                address = mac;
            }*/
        }
        //Log.v("dan get_mac","mac "+address);
        //return address;
        return null;
    }
    /*
    def device_registration_with_retry(IP=None, addr=None):
    if IP != None:
        csmapi.ENDPOINT = 'http://' + IP + ':9999'
    success = False
    while not success:
        try:
            register_device(addr)
            success = True
        except Exception as e:
            print ('Attach failed: '),
            print (e)
        time.sleep(1)
     */
    public void device_registration_with_retry(String IP)throws InterruptedException{
        if(IP != null){
            Log.v("DAN","ip isn't null");
            Csmapi.Endpoint = "http://"+IP+":9999";
        }
        boolean success = false;
        while(!success){
            try{
                if(register_device()){
                    success = true;
                    Log.d("DAN device regis","success");
                }
                else
                    Log.e("DAN device regis","failed");

            }
            catch(Exception e){
                Log.e("DAN device regis","error : "+e);
            }
            Thread.sleep(1000);
        }
    }

    /*
    timestamp={}
    MAC=get_mac_addr()
    thx=None
    def register_device(addr):
        global MAC, profile, timestamp, thx

        if csmapi.ENDPOINT == None: detect_local_ec()

        if addr != None: MAC = addr

        if profile['d_name'] == None: profile['d_name']= str(int(random.uniform(1, 100)))+'.'+ profile['dm_name']

        for i in profile['df_list']: timestamp[i] = ''

        print('IoTtalk Server = {}'.format(csmapi.ENDPOINT))
        if csmapi.register(MAC,profile):
            print ('This device has successfully registered.')
            print ('Device name = ' + profile['d_name'])

            if thx == None:
                print ('Create control threading')
                thx=threading.Thread(target=ControlChannel)     #for control channel
                thx.daemon = True                               #for control channel
                thx.start()                                     #for control channel

            return True
        else:
            print ('Registration failed.')
            return False
     */

    public boolean register_device(){
        try{
            mac_addr = get_mac_addr();
            mac_addr = mac_addr.replace(":","");
            //mac_addr = "C860008BD249";
            Log.v("DAN regis ","mac_addr "+mac_addr);

            df_list = new String[dflist.length()];
            timestamp = new String[df_list.length];
            //for i in profile['df_list']: timestamp[i] = ''
            for(int i=0 ;i<df_list.length;i++){
                df_list[i] = dflist.getString(i);
                timestamp[i] = "";
            }
            //if csmapi.register(MAC,profile):
            if(Csmapi.register(mac_addr,profile)){

                Log.v("DAN","This device has successfully registered.");
                ControlChannel c_t_l = new ControlChannel();
                c_t_l.start();
                return true;
            }
            else{
                Log.e("DAN","Registration failed.");
                return false;
            }
        }
        catch(SocketException se){

        }
        catch (InterruptedIOException ie){

        }
        catch(JSONException je){

        }
        return false;
    }
    /*
    def pull(FEATURE_NAME):
        global timestamp

        if state == 'RESUME': data = csmapi.pull(MAC,FEATURE_NAME)
        else: data = []

        if data != []:
            if timestamp[FEATURE_NAME] == data[0][0]:
                return None
            timestamp[FEATURE_NAME] = data[0][0]
            if data[0][1] != []:
                return data[0][1]
            else: return None
        else:
            return None
     */
    public JSONArray pull(String df_name) throws Exception {

        JSONArray data = new JSONArray();
        int index=-1;
        if(state.equals("RESUME")) {
            //Log.v("dan pull ","resume");
            data = Csmapi.pull(mac_addr, df_name);
            Log.v("dan pull","data "+data);
        }
        else
            data = null;
        if(data!=null){
            for(int i =0;i<df_list.length;i++){
                if(df_list[i].equals(df_name)){
                    index = i;
                    break;
                }
            }
            if(index!=-1&&timestamp[index].equals(data.getJSONArray(0).getString(0)))
                return null;
            else
                timestamp[index] = data.getJSONArray(0).getString(0);
            if(data.getJSONArray(0).getJSONArray(1)!=null){
                return data.getJSONArray(0).getJSONArray(1);
            }
        }
        return null;
    }
    /*
    def push(FEATURE_NAME, *data):
    if state == 'RESUME':
        return csmapi.push(MAC, FEATURE_NAME, list(data))
    else: return None
     */
    public boolean push(String df_name,JSONArray data){
        if(state.equals("RESUME")) {
            //Log.v("dan push ","resume");
            return Csmapi.push(mac_addr, df_name, data);
        }
        else
            return false;
    }

}

