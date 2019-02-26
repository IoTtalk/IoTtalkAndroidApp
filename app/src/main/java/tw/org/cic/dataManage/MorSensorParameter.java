package tw.org.cic.dataManage;

/**
 * Created by wllai on 2016/5/6.
 */
public class MorSensorParameter {

    public static int humi_data = 50;
    public static float uv_data = 0, alcohol_voltage = 0f, alcohol_data = 0f;

    public static final int IN_BLE_SENSOR_DATA = (byte) 0xF3; //0xF3

    /**
     * SEND COMMAND ID(BLE)
     */
    public static final int SEND_MORSENSOR_BLE_SENSOR_DATA_ALL = 1;

}
