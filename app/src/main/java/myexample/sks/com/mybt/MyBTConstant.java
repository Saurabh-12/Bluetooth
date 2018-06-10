package myexample.sks.com.mybt;

import android.os.Handler;

import java.util.UUID;

public class MyBTConstant {

    private static final String TAG = "MY_APP_DEBUG_TAG";
    public static final UUID MY_UUID =
    UUID.fromString("00001101-0000-3000-9000-00805f9b34fb");

    // Defines several constants used when transmitting messages between the
    // service and the UI.
    public static final int MESSAGE_READ = 0;
    public static final int MESSAGE_WRITE = 1;
    public static final int MESSAGE_TOAST = 2;

    // ... (Add other message types here as needed.)

}
