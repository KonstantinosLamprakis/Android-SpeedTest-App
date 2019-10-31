/*
 class MainActivity represents the main Activity of program.
 */

package gr.aueb.netswitch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity {

    private TextView currentBatteryTxt;
    private TextView minBatteryTxt;
    private TextView resultTxt;
    private static TextView currentDataConsumptionTxt;
    private TextView maxDataConsumptionTxt;
    private TextView speedStateTextView;
    private Button calculateButton;
    private Button restoreButton;
    private Button wifiTestButton;
    private Button mobileDataTestButton;
    private Spinner wifiSpinner;
    private Spinner mobileDataSpinner;

    private boolean wifiTestCompleted = false;
    private boolean mobileDataTestCompleted = false;
    private float downloadMbpsWifi = 0;
    private float downloadMbpsMobileData = 0;

    private Handler mHandler = new Handler();

    // this method is called when the mainActivity is created.
    @Override
    protected void onCreate(Bundle savedInstanceState){

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        makeActionBar();
        makeInitialization();

        // restores data consumption to zero level.
        restoreButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                writeDataFile("0", getApplicationContext());
            }
        });

        // starts wifi speedtest
        wifiTestButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ConnectivityManager connMgr = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
                final NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
                if(networkInfo == null){
                    Toast.makeText(getApplicationContext(), "Wifi is disabled. Please connect to Wifi network.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(networkInfo.getTypeName().equals("WIFI")) {
                    makeSpeedtest("wifi");
                }else{
                    Toast.makeText(getApplicationContext(), "Wifi is disabled. Please connect to Wifi network.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        mobileDataTestButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ConnectivityManager connMgr = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
                final NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
                if(networkInfo == null) {
                    Toast.makeText(getApplicationContext(), "Mobile data are disabled. Please enable mobile data.", Toast.LENGTH_SHORT).show();
                    return;
                }

                if(networkInfo.getTypeName().equals("MOBILE")){
                    makeSpeedtest("mobile");
                }else{
                    Toast.makeText(getApplicationContext(), "Mobile data are disabled. Please enabled mobile data.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // choose between wifi and mobile data depends on parameters.
        calculateButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                resultTxt.setText(""); // clear result area from previous calculation.
                batteryControl();
                dataConsumptionControl();
                finalControl();// includes speedtest (download speed) conditions.
            }
        });
    }

    // this method shows menu at main_activity
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    //this method is called when user press any button of navigation bar(wifi, cellular, refresh)
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.wifi:
                startActivity(new Intent(this, WifiProperties.class));
                break;
            default:
                //refresh
                try{
                    finish();
                    startActivity(getIntent());
                }catch (Exception e){
                    Toast.makeText(getApplicationContext(), "Error: refresh failed." + e, Toast.LENGTH_SHORT).show();
                }

        }
        return super.onOptionsItemSelected(item);
    }

    // makes the action bar.
    private void makeActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setIcon(R.mipmap.app_icon);
        actionBar.setDisplayUseLogoEnabled(true);// display app_icon.
        actionBar.setDisplayShowHomeEnabled(true);// display back button.
    }

    // initialization of GUI components.
    private void makeInitialization(){

        // set current battery level.
        BroadcastReceiver BatteryInfoReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
                currentBatteryTxt.setText(String.valueOf(level) + "%");
            }
        };
        currentBatteryTxt = (TextView) findViewById(R.id.currentBatteryTxt);
        this.registerReceiver(BatteryInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        // set a listener for network changes(counts mobile data if network switch to mobile data).
        NetworkChangeReceiver receiver = new NetworkChangeReceiver();
        getApplicationContext().registerReceiver(receiver, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));

        minBatteryTxt = (TextView) findViewById(R.id.minBatteryTxt);
        currentDataConsumptionTxt = (TextView) findViewById(R.id.currentDataConsumption);
        currentDataConsumptionTxt.setText(readDataFile(getApplicationContext())+ " MB");
        restoreButton = (Button)findViewById(R.id.restoreButton);
        maxDataConsumptionTxt = (TextView) findViewById(R.id.maxDataConsumpiotTxt);
        resultTxt = (TextView) findViewById(R.id.resultTxt);
        calculateButton = (Button) findViewById(R.id.calculateButton);
        wifiTestButton = (Button) findViewById(R.id.wifiTestButton);
        mobileDataTestButton = (Button) findViewById(R.id.mobileDataTestButton);
        wifiSpinner = (Spinner) findViewById(R.id.wifiSpinner);
        mobileDataSpinner = (Spinner) findViewById(R.id.mobileDataSpinner);
        speedStateTextView = (TextView) findViewById(R.id.speedStateTextView);

        spinnerUpdate("latency: -", "download: -", "upload: -", wifiSpinner);
        spinnerUpdate("latency: -", "download: -", "upload: -", mobileDataSpinner);
    }

    // control battery level and updates result field if its necessary.
    private void batteryControl(){

        // input controls
        // minimum Battery level input control.
        String minBattery = minBatteryTxt.getText().toString().trim();

        if (!integerInputControl(minBattery)) {
            Toast.makeText(getApplicationContext(), "Invalid input for minimum battery level.", Toast.LENGTH_SHORT).show();
            return;
        }else if(!minBattery.equals("")){
            if(Integer.parseInt(minBattery)>100 || Integer.parseInt(minBattery)<1){
                Toast.makeText(getApplicationContext(), "Invalid input for minimum battery level.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Check conditions for result:

        // check battery level condition.
        if (!minBattery.equals("")) {
            String currentBattery = currentBatteryTxt.getText().toString();
            currentBattery = currentBattery.substring(0, currentBattery.length() - 1); // without character %
            if (Integer.parseInt(currentBattery) < Integer.parseInt(minBattery)) {
                resultTxt.setText("Wifi");
                return;
            }
        }
    }

    // dataConsumptionControl makes control for data consumption values and updates result field if its necessary.
    private void dataConsumptionControl(){

        // max Consumption input control.
        String maxConsum = maxDataConsumptionTxt.getText().toString().trim();
        if(maxConsum.equals(""))return;
        if (!integerInputControl(maxConsum)) {
            Toast.makeText(getApplicationContext(), "Invalid input for maximum data consumption.", Toast.LENGTH_SHORT).show();
            return;
        }else if(Integer.parseInt(maxConsum) < 0){
            Toast.makeText(getApplicationContext(), "Invalid input for maximum data consumption.", Toast.LENGTH_SHORT).show();
            return;
        }
        int maxDataConsumption = Integer.parseInt(maxConsum);
        double currentDataConsumtion = Double.parseDouble(readDataFile(getApplicationContext()));

        if(maxDataConsumption <= currentDataConsumtion) resultTxt.setText("Wifi");

    }

    // Set final result depends on speedtest result.
    private void finalControl(){
        if(resultTxt.getText().equals("Wifi"))return; // dataConsumptionControl() or batteryControl() have priority.
        if(wifiTestCompleted && mobileDataTestCompleted){
            if(downloadMbpsMobileData > downloadMbpsWifi){
                resultTxt.setText("Mobile Data");
            }else{
                resultTxt.setText("Wifi");
            }
        }else{
            resultTxt.setText("Undefined");
            Toast.makeText(getApplicationContext(), "Please, make speedtest to choose the most fast connection.", Toast.LENGTH_SHORT).show();
        }
    }

    // update file with current mobile data consumption.
    public static void writeDataFile(String data,Context context) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput("dataConsumption.txt", Context.MODE_PRIVATE));
            outputStreamWriter.write(data);
            outputStreamWriter.close();

            currentDataConsumptionTxt.setText(data + " MB"); // refresh data consumption field.
        }
        catch (IOException e) {
            Toast.makeText(context, "File write failed: " + e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    // read file with current mobile data consumption.
    public static String readDataFile(Context context) {

        String ret = "";

        try {
            InputStream inputStream = context.openFileInput("dataConsumption.txt");

            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString;
                StringBuilder stringBuilder = new StringBuilder();

                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append(receiveString);
                }

                inputStream.close();
                ret = stringBuilder.toString();
            }
        }
        catch (FileNotFoundException e) {
            Toast.makeText(context, "File not found: " + e.toString(), Toast.LENGTH_SHORT).show();
            ret = "0";
        } catch (IOException e) {
            Toast.makeText(context, "Can not read file: " + e.toString(), Toast.LENGTH_SHORT).show();
            ret = "0";
        }

        return ret;
    }

    // makes integer input control.
    private static boolean integerInputControl(String str) {
        return str.matches("\\d*");
    }

    // creates a 5 MB file which is going to be transmit.
    private void createTransmissionFile(String path, String name){
        try
        {
            // Create a random file of 5 MB size for transmission.
            new File(path + name).delete(); // remove previous files.
            RandomAccessFile transmitFile = new RandomAccessFile(path + name,"rw"); // create new file to transmit it.
            transmitFile.setLength(1024*1024*5); // set size of file to 5 MB.

        }catch (Exception e){
            e.printStackTrace();
            Log.e("error", "Error creating file for transmission. " + e);
        }
    }

    // update spinner with information about speedtest result.
    private void spinnerUpdate(String latency, String download, String upload, Spinner spinner){

        if(spinner.equals(wifiSpinner) && download.contains("Mbps")){
            wifiTestCompleted = true;
        }else if(spinner.equals(mobileDataSpinner) && download.contains("Mbps")){
            mobileDataTestCompleted = true;
        }

        // Spinner Drop down elements
        List<String> categories = new ArrayList<>();
        categories.add(latency);
        categories.add(download);
        categories.add(upload);

        // Creating adapter for spinner
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // attaching data adapter to spinner
        spinner.setAdapter(dataAdapter);
    }

    // make speed test for wifi or mobile data and updates GUI elements.
    private void makeSpeedtest(final String kind){
        try {
            Runnable wifiTest = new Runnable() {
                @Override
                public void run() {

                    String path = getFilesDir().getAbsolutePath(); // path to store received and transmitted file.
                    String name = File.separator + "transmitFile.txt"; // name of transmitted file.
                    createTransmissionFile(path, name);
                    SpeedtestClient client = new SpeedtestClient("netswitchaueb.ddns.net", 5050, new File(path + name), path);

                    ExecutorService executor = Executors.newCachedThreadPool();
                    Future<Boolean> futureCall = executor.submit(client);

                    try {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                speedStateTextView.setText("Speed Test is running...");
                            }
                        });
                        if (futureCall.get(10, TimeUnit.MINUTES)) {
                            // update speedtest information.
                            final long latency = client.getLatency();
                            if(kind.equals("wifi")) {
                                downloadMbpsWifi = (5.2f*8)/(client.getDownloadMs()/1000.0f); // Mbit per second. (5.2 MB file, 1000ms = 1 second, 8 Mbit = 1 MByte).
                            }else{
                                downloadMbpsMobileData = (5.2f*8)/(client.getDownloadMs()/1000.0f); // Mbit per second. (5.2 MB file, 1000ms = 1 second,8 Mbit = 1 MByte).
                            }
                            final float upLoadMbps = (5.2f*8)/(client.getUplpoadMs()/1000.0f); // Mbit per second. (5.2 MB file, 1000ms = 1 second, 8 Mbit = 1 MByte).

                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if(kind.equals("wifi")){
                                        spinnerUpdate("latency: "+ latency + " ms", "download: " + downloadMbpsWifi + " Mbps", "upload: " + upLoadMbps + " Mbps", wifiSpinner);
                                    }else{
                                        spinnerUpdate("latency: "+ latency + " ms", "download: " + downloadMbpsMobileData + " Mbps", "upload: " + upLoadMbps + " Mbps", mobileDataSpinner);
                                    }
                                    speedStateTextView.setText("Speed Test complete.");
                                }
                            });
                        } else {
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getApplicationContext(), "Error: speedest failed.", Toast.LENGTH_SHORT).show();
                                    speedStateTextView.setText("Speed Test failed.");
                                }
                            });
                        }

                    } catch (final Exception e) {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(), "Error: speedest failed.", Toast.LENGTH_SHORT).show();
                                speedStateTextView.setText("Speed Test failed.");
                            }
                        });
                    }
                }
            };
            new Thread(wifiTest).start();
        }catch(Exception e){
            Toast.makeText(getApplicationContext(), "Error: speedest failed." + e, Toast.LENGTH_SHORT).show();
            speedStateTextView.setText("Speed Test failed.");
        }
    }
}