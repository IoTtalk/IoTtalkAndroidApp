package tw.org.cic.tracking_mobile;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import java.io.InterruptedIOException;


public class TrackingDAN extends Thread {

    final int IOTTALK_BROADCAST_PORT = 17000;
    final int RETRY_COUNT = 3;
    final int RETRY_INTERVAL = 2000;
//    DAN2DAI dan2dai_ref;
    String mac_addr = "TRACKING_DEFAULT";
    boolean registered;


    public boolean push(String idf_name, JSONArray data) {
        Log.v("dan_push", String.valueOf(data));
//        logging("push(%s)", idf_name);
        try {
//            if (idf_name.equals("Control")) {
//                idf_name = "__Ctl_I__";
//            }
//            for (int i = 0; i < df_list.length; i++) {
//                if (idf_name.equals(df_list[i])) {
//                    df_is_odf[i] = false;
//                    if (!df_selected[i]) {
//                        return false;
//                    }
//                }
//            }
//            if (suspended && !idf_name.equals("__Ctl_I__")) {
//                return false;
//            }
            return TrackingCSMapi.push(mac_addr, idf_name, data);
        } catch (TrackingCSMapi.CSMError e) {
            logging("push(): CSMError: %s", e.getMessage());
        } catch (JSONException e) {
            logging("push(): JSONException: %s", e.getMessage());
        } catch (InterruptedIOException e) {
            logging("deregister(): DEREGISTER: InterruptedIOException: %s", e.getMessage());
        }
        return false;
    }

    public boolean deregister() {
        logging("deregister()");
        if (!registered) {
            return true;
        }
        // stop polling first
        registered = false;
        for (int i = 0; i < RETRY_COUNT; i++) {
            try {
                if (TrackingCSMapi.deregister(mac_addr)) {
                    logging("deregister(): Deregister succeed: %s", TrackingCSMapi.ENDPOINT);
                    return true;
                }
            } catch (TrackingCSMapi.CSMError e) {
                logging("deregister(): DEREGISTER: CSMError: %s", e.getMessage());
            } catch (InterruptedIOException e) {
                logging("deregister(): DEREGISTER: InterruptedIOException: %s", e.getMessage());
            }
            logging("deregister(): Deregister failed, wait %d milliseconds before retry", RETRY_INTERVAL);
            try {
                Thread.sleep(RETRY_INTERVAL);
            } catch (InterruptedException e) {
                logging("deregister(): InterruptedException");
            }
        }
        // sorry, I give up
        return false;
    }

    public void run () {
//        logging("Polling: starts");
//        while (registered) {
//            try {
//                JSONArray data = pull(-1);
//                if (check_command_message(data)) {
//                    dan2dai_ref.pull("Control", data);
//                } else {
//                    logging("The command message is null or is problematic, abort");
//                }
//
//                for (int i = 0; i < df_list.length; i++) {
//                    if (!registered || suspended) {
//                        break;
//                    }
//                    if (!df_is_odf[i] || !df_selected[i]) {
//                        continue;
//                    }
//                    data = pull(i);
//                    if (data == null) {
//                        continue;
//                    }
//                    dan2dai_ref.pull(df_list[i], data);
//                }
//            } catch (JSONException e) {
//                logging("Polling: JSONException: %s", e.getMessage());
//            } catch (TrackingCSMapi.CSMError e) {
//                logging("Polling: CSMError: %s", e.getMessage());
//            } catch (InterruptedIOException e) {
//                logging("Polling: InterruptedIOException: %s", e.getMessage());
//                break;
//            }
//
//            try {
//                Thread.sleep(50);
//            } catch (InterruptedException e) {
//                logging("Polling: InterruptedException: %s", e.getMessage());
//            }
//        }
//        logging("Polling: stops");
    }


    void logging (String format, Object... args) {
        logging(String.format(format, args));
    }

    void logging (String message) {
        System.out.printf("[DAN] %s%n", message);
    }
}

