package com.irveni.doorbell.functions;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.NetworkSpecifier;
import android.net.wifi.WifiManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
public class NetworkUtil {
    public static final int TYPE_WIFI = 1;
    public static final int TYPE_MOBILE = 2;
    public static final int TYPE_NOT_CONNECTED = 0;
    public static final int NETWORK_STATUS_NOT_CONNECTED = 0;
    public static final int NETWORK_STATUS_WIFI = 1;
    public static final int NETWORK_STATUS_MOBILE = 2;

    public static void getConnectivityStatus(Activity context) {
        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .build();


        ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {

                super.onAvailable(network);


                ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                NetworkInfo mData = connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
                WifiManager wifi = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
                if (wifi.isWifiEnabled()){
                    Toast.makeText(context, "Wifi Connected", Toast.LENGTH_SHORT).show();
                }else if(mData.isConnectedOrConnecting()){
                    Toast.makeText(context, "Data Connected", Toast.LENGTH_SHORT).show();
                }


            }

            @Override
            public void onLost(@NonNull Network network) {
                super.onLost(network);

                ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                NetworkInfo mData = connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
                WifiManager wifi = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
                if (!wifi.isWifiEnabled() && !mData.isConnected()){
                    Toast.makeText(context, "No Internet", Toast.LENGTH_SHORT).show();
                }
                else if(!mData.isConnectedOrConnecting()){
                    Toast.makeText(context, "Data Dis-connected", Toast.LENGTH_SHORT).show();
                }else if(!wifi.isWifiEnabled()){
                    Toast.makeText(context, "Wifi Dis-connected", Toast.LENGTH_SHORT).show();
                }

            }

            @Override
            public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
                super.onCapabilitiesChanged(network, networkCapabilities);
                final boolean unmetered = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED);


            /*    if (mWifi.isConnectedOrConnecting()) {
                    // Do whatever
                    Toast.makeText(context.getApplicationContext(), "Wifi Connected", Toast.LENGTH_SHORT).show();
                }
                else if(mData.isConnectedOrConnecting()){
                    Toast.makeText(context.getApplicationContext(), "Data Connected", Toast.LENGTH_SHORT).show();
                }*/


            }
        };


        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(ConnectivityManager.class);
        connectivityManager.requestNetwork(networkRequest, networkCallback);



    }


}