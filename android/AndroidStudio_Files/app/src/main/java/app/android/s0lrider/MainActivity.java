/*
    S0lRider Android App
    Daniel Casado de Luis
    May 2016

    Original joystick PoC taken from Akexorcist http://www.akexorcist.com/
 */

package app.android.s0lrider;
/**
 * Main class that provides joystick control for the car and acts as a proxy with Pebble watch for voice commands
 */

//General Android libraries
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.support.v7.app.AppCompatActivity;

//Socket libraries
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.UUID;

//to implement wait for voice commands

import android.os.StrictMode;
import android.widget.Toast;

//Pebble libraries imports
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import static android.widget.Toast.*;

public class MainActivity extends AppCompatActivity {
    //Android APP layout variables, will be initialized in the onCreate method
    RelativeLayout layout_joystick;
    TextView textViewDirection, textViewPebble;
    EditText editTextIP;
    Button buttonLights;
    //Declaring a joystick variable
    JoyStickClass js;

    //They must match LUA configuration/code on the s0lRider car itself, or one can configure in the
    //text box
    private String carIP = "192.168.4.1";
    private int carPort = 7777;

    //Required to obtain commands from Pebble watch
    private static final UUID APP_UUID = UUID.fromString("30db2c08-00fc-471a-8a03-8862c1918239");
    private PebbleKit.PebbleDataReceiver mDataReceiver;

    //Note: at the time of this writing pebble does not support pairing without the pebble app on your phone
    // therefore make sure you install the pebble app on your phone
    @Override
    protected void onResume() {
        super.onResume();
        boolean isConnected = PebbleKit.isWatchConnected(this);
        makeText(this, "Pebble " + (isConnected ? "is" : "is not") + " connected!", LENGTH_LONG).show();

    }

    public void sendS0lrider(String command){
        try {
            DatagramSocket client_socket = new DatagramSocket();
            InetAddress IPAddress = InetAddress.getByName(carIP);
            byte[] send_data = command.getBytes();
            DatagramPacket send_packet = new DatagramPacket(send_data, send_data.length, IPAddress, carPort);
            client_socket.send(send_packet);
            Log.i("s0lrider","Sent: " + command + " to: " + carIP);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        //This should be avoided if you are doing a more serious/secure app
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //initializing the variables that point to UI elements defined in activity_main.xml
        textViewDirection = (TextView)findViewById(R.id.textViewDirection);
        textViewPebble = (TextView)findViewById(R.id.textViewPebble);
        buttonLights = (Button)findViewById(R.id.buttonLights);
        editTextIP = (EditText)findViewById(R.id.editTextIP);
        //initializing the variable that will point to the joystick
        layout_joystick = (RelativeLayout)findViewById(R.id.layout_joystick);
        js = new JoyStickClass(getApplicationContext(), layout_joystick, R.drawable.image_button);
        //pre-configure the car's IP, can be changed with the editTextIP field
        editTextIP.setText(carIP);
        //set joystick size and format
        js.setStickSize(150, 150);
        js.setLayoutSize(500, 500);
        js.setLayoutAlpha(150);
        js.setStickAlpha(100);
        js.setOffset(90);
        js.setMinimumDistance(50);
        //definition of the listener for events on joystick
        layout_joystick.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View joystickView, MotionEvent joystickEvent) {
                js.drawStick(joystickEvent);
                int direction=-1;
                if(joystickEvent.getAction() == MotionEvent.ACTION_DOWN || joystickEvent.getAction() == MotionEvent.ACTION_MOVE) {
                    direction = js.get4Direction();
                    if(direction == JoyStickClass.STICK_UP) {
                        textViewDirection.setText("Direction: Up");
                        sendS0lrider("up");
                    } else if(direction == JoyStickClass.STICK_RIGHT) {
                        textViewDirection.setText("Direction: Right");
                        sendS0lrider("right");
                    } else if(direction == JoyStickClass.STICK_DOWN) {
                        textViewDirection.setText("Direction: Down");
                        sendS0lrider("down");
                    } else if(direction == JoyStickClass.STICK_LEFT) {
                        textViewDirection.setText("Direction: Left");
                        sendS0lrider("left");
                    } else if(direction == JoyStickClass.STICK_NONE) {
                        textViewDirection.setText("Direction: Center");
                        textViewPebble.setText("Pebble: ");
                        sendS0lrider("center");
                    }
                } else if(joystickEvent.getAction() == MotionEvent.ACTION_UP) {
                    textViewDirection.setText("Direction: ");
                    textViewPebble.setText("Pebble: ");
                    sendS0lrider("center");
                }
                return true;
            }
        });
        //code to receive messages from Pebble
        if(mDataReceiver == null) {
            mDataReceiver = new PebbleKit.PebbleDataReceiver(APP_UUID) {
                @Override
                public void receiveData(Context context, int transactionId, PebbleDictionary dict) {
                    // Always ACK
                    PebbleKit.sendAckToPebble(context, transactionId);

                    // Handling voice command
                    if(dict.getString(0) != null) {
                        String pebbleCommand = dict.getString(0);
                        Log.i("s0lrider", "Received: " + pebbleCommand);
                        textViewPebble.setText("Pebble: " + pebbleCommand);
                        sendS0lrider(pebbleCommand.toLowerCase());
                    }
                }
            };
            PebbleKit.registerReceivedDataHandler(getApplicationContext(), mDataReceiver);
        }
        //code to handle scanner lights button press
        buttonLights.setOnClickListener(
                new Button.OnClickListener() {
                    public void onClick(View v) {
                        sendS0lrider("lights");
                    }
                }
        );

        //code to handle editing car's ip address
        editTextIP.setOnEditorActionListener(
                new EditText.OnEditorActionListener(){
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        // the user is done typing, action id is 6 or the EditorInfo.IME_ACTION_DONE constant
                        if (actionId == EditorInfo.IME_ACTION_DONE) {

                            carIP = editTextIP.getText().toString();
                            Log.i("s0lrider","New IP: " + carIP);
                            Toast.makeText(getApplicationContext(),"New IP: " + carIP,Toast.LENGTH_LONG).show();
                            return true; // consume event
                        }
                        return false; // pass event on to other listeners (if any)
                    }
                }
        );

    }
}
