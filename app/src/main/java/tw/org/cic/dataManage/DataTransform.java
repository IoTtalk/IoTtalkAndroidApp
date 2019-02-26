package tw.org.cic.dataManage;

import android.util.Log;


/**
 * Created by 1404011 on 2015/5/22.
 */

public class DataTransform {
    private static final String TAG = "DataTransform";
    static float data[] = new float[9]; //0-Temp, 1-Humidity, 2-UV, 3-Alcohol
    static short RawData[] = new short[20];


    public static float[] getData() {
        return data;
    }

    public static void TransformTempHumi(byte[] value) {
        float Temp_data = (float) ((value[2] & 0xFF) << 8 | (value[3] & 0xFF));
        float RH_data = (float) ((value[4] & 0xFF) << 8 | (value[5] & 0xFF));

        data[0] = (float) (Temp_data * 175.72 / 65536.0 - 46.85); //Temp
        data[1] = (float) (RH_data * 125.0 / 65536.0 - 6.0); //RH
        data[1] = data[1] + (25f - data[0]) * -1.5f; //RH
        if (data[1] > 100)
            data[1] = 100f;

        Log.i(TAG, "Temp:" + data[0] + " Humi:" + data[1]);
    }

    public static void TransformUV(byte[] value) {

        float UV_data = ((float) (value[3 + 20] << 8 | (value[2 + 20] & 0xFF)) / 100f);
        data[5] = UV_data;
        if (UV_data < 1.8)
            data[2] = (float) (0.9208f * Math.pow(UV_data, 3) - 1.6399f * Math.pow(UV_data, 2f) + 1.2008f * UV_data - 0.054f);
        else if (UV_data < 4)
            data[2] = (float) (0.3389f * Math.pow(UV_data, 2f) + 0.7501f * UV_data - 0.2077f);
        else if (UV_data < 5) {
            if (UV_data > 5) UV_data = 5;
            float x = 0.49f * UV_data + 2.062f;
            data[2] = (float) (0.3389f * Math.pow(x, 2f) + 0.7501f * x - 0.2077f);
        }
        data[2] = UV_data;
//        data[0][2] = (float) (UV_data / 100.0); //UV
        Log.i(TAG, "UV:" + data[2]);
    }


    public static void TransformAlcohol(byte[] value) {
        float Alc_out;
        Alc_out = (float) ((value[2 + 40] << 8 | (value[3 + 40] & 0xFF)) / 4096.0 * 3.3);
        data[3] = 1.004f * (float) Math.pow(Alc_out, 3) - 1.8891f * (float) Math.pow(Alc_out, 2) + 1.3245f * Alc_out - 0.2571f;
        data[4] = Alc_out;
        MorSensorParameter.alcohol_voltage = Alc_out;
        if (data[3] < 0.1)
            data[3] = 0;
        Log.i(TAG, "Alc_out:" + Alc_out + " data[3]:" + data[3]);
    }

    // Byte[] to HexString
    public static String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);

        for (byte byteChar : bytes) {
            sb.append(String.format("%02X", byteChar));
        }
        return sb.toString();
    }

    // HexString to Byte[]
    public static byte[] hexToBytes(String hexString) {
        char[] hex = hexString.toCharArray();
        //轉rawData長度減半
        int length = hex.length / 2;
        byte[] rawData = new byte[length];
        for (int i = 0; i < length; i++) {
            //先將hex資料轉10進位數值
            int high = Character.digit(hex[i * 2], 16);
            int low = Character.digit(hex[i * 2 + 1], 16);
            //將第一個值的二進位值左平移4位,ex: 00001000 => 10000000 (8=>128)
            //然後與第二個值的二進位值作聯集ex: 10000000 | 00001100 => 10001100 (137)
            int value = (high << 4) | low;
            //與FFFFFFFF作補集
            if (value > 127)
                value -= 256;
            //最後轉回byte就OK
            rawData[i] = (byte) value;
        }
        return rawData;
    }

}
