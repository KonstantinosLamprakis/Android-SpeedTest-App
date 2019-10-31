/*
*  class WifiProperties represents an activity that shows information about wifi connection (ssid, rssi and state).
* */

package gr.aueb.netswitch;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

public class WifiProperties extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_properties);

        makeActionBar();

        // initialization:
        final WifiManager wifi =(WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        final TextView ssidText = (TextView) findViewById(R.id.ssid_textview);
        final TextView signalText = (TextView) findViewById(R.id.signal_textview);
        final RadioButton on_button = (RadioButton) findViewById(R.id.on_radio);
        final RadioButton off_button = (RadioButton) findViewById(R.id.off_radio);
        final RadioGroup groupOnOff = (RadioGroup) findViewById(R.id.radioGroup);

        ConnectivityManager connMgr = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        // ssid - signal  strength - on/off view
        if(!(networkInfo == null) && networkInfo.getTypeName().equals("WIFI") && wifi.isWifiEnabled() && networkInfo.isConnected()){

            // display ssid
            String ssidFullName = wifi.getConnectionInfo().getSSID();
            ssidText.setText(ssidFullName.substring(1, ssidFullName.length()-1));// remove "" from name
            on_button.setChecked(true);

            // display signal level
            switch (wifi.calculateSignalLevel(wifi.getConnectionInfo().getRssi(), 4)){
                case 1:
                    signalText.setText("Weak (" + wifi.getConnectionInfo().getRssi() + ")");
                    break;
                case 2:
                    signalText.setText("Medium (" + wifi.getConnectionInfo().getRssi() + ")");
                    break;
                case 3:
                    signalText.setText("Good (" + wifi.getConnectionInfo().getRssi() + ")");
                    break;
                default:
                    signalText.setText("Excellent (" + wifi.getConnectionInfo().getRssi() + ")");

            }
        }else{
            off_button.setChecked(true);
            ssidText.setText("");
            signalText.setText("");
        }

        // Choice for wifi enabling/disabling, on/off functionallity
        groupOnOff.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if(off_button.isChecked()){
                    wifi.setWifiEnabled(false);
                    ssidText.setText("");
                    signalText.setText("");
                }else{
                    wifi.setWifiEnabled(true);
                }
            }
        });
    }

    // makes the action bar.
    private void makeActionBar(){
        ActionBar actionBar = getSupportActionBar();
        actionBar.setIcon(R.mipmap.app_icon);
        actionBar.setDisplayUseLogoEnabled(true);// display app_icon.
        actionBar.setDisplayShowHomeEnabled(true);// display back button.
        actionBar.setTitle("Wifi");
        actionBar.setDisplayHomeAsUpEnabled(true);// display home button
    }

}
