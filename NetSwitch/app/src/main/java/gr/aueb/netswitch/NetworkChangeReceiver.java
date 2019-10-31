/**
 *  class NetworkChangeReceiver listen for connection changes and begins a service for counting mobile data consumption.
 */

package gr.aueb.netswitch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;

public class NetworkChangeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, final Intent intent) {

        if ("android.net.conn.CONNECTIVITY_CHANGE".equals(intent.getAction()) || Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {

            ConnectivityManager connMgr = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

            if(networkInfo != null && networkInfo.getTypeName().equals("MOBILE")){
                try{
                    Intent counting = new Intent(context, CountMobileDataService.class);
                    context.startService(counting);
                }catch (Exception e){
                    Toast.makeText(context, e.toString(), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
