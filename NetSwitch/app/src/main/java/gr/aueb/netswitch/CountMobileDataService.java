/*
 class CountMobileDataService represents a service which is running background and counts mobile data consumption.
 */

package gr.aueb.netswitch;

import android.app.IntentService;
import android.content.Intent;
import android.os.SystemClock;
import android.widget.Toast;

public class CountMobileDataService extends IntentService {

    public CountMobileDataService() {
        super("Mobile Data Counting Service");
    }

    public CountMobileDataService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {

        double  oldSum;
        try{
            oldSum = Double.parseDouble(MainActivity.readDataFile(getApplicationContext()));
        }catch (Exception e){
            Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_SHORT ).show();
            oldSum = 0;
        }

        double initialData =  android.net.TrafficStats.getMobileRxBytes() + android.net.TrafficStats.getMobileTxBytes();// since device boot.
        double previousSum , currentSum = 0;
        while(true){
            try{
                SystemClock.sleep(1000); // delay for 1 sec, so dont overclock CPU.
                previousSum = currentSum;
                currentSum = android.net.TrafficStats.getMobileRxBytes() + android.net.TrafficStats.getMobileTxBytes();
                if((currentSum == 0) && (previousSum != 0)){ // when mobile network disabled, then currentSum = 0 and previousSum = total mobile data for this round.
                    oldSum += (previousSum-initialData)/(1024*1024);
                    MainActivity.writeDataFile((double) Math.round(oldSum * 10) / 10+"", getApplicationContext()); // round double to 1 decimal point and store consumption as MB.
                    return;
                }
            }catch (Exception e){
                Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_SHORT ).show();
                return;
            }
        }
    }
}
