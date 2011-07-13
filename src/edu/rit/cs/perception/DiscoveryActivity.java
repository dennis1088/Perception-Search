package edu.rit.cs.perception;

import java.io.IOException;
import java.util.ArrayList;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class DiscoveryActivity extends ListActivity {
	
	public final static String LIVECAP_SERVICE_TYPE = "_pslivecap._tcp.local.";
	public final static int DELAY = 1000;
	
	private JmDNS jmdns = null;
	private MulticastLock mLock;
	private ServiceListener listener;
	private Handler handler = new Handler();
	private boolean foundService = false;
	private ProgressDialog pd;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        
        setListAdapter(new ArrayAdapter<String>(this,
        		android.R.layout.simple_list_item_1,
        		new ArrayList<String>()));

        ListView lv = getListView();
        lv.setTextFilterEnabled(true);

        lv.setOnItemClickListener(new OnItemClickListener() {
          @SuppressWarnings("deprecation")
		public void onItemClick(AdapterView<?> parent, View view,
              int position, long id) {
            ServiceInfo serviceInfo = jmdns.getServiceInfo(LIVECAP_SERVICE_TYPE, 
            		(String)((TextView) view).getText());
            StringBuilder builder = new StringBuilder(2048);
            builder.append(serviceInfo.getAddress());
            builder.append(":");
            builder.append(serviceInfo.getPort());
            Toast.makeText(getApplicationContext(), builder.toString(), Toast.LENGTH_SHORT).show();
          }
        });
        
        handler.postDelayed(new Runnable() {

			@Override
			public void run() {
				setUp();
			}
        	
        }, DELAY);
        
        pd = ProgressDialog.show(DiscoveryActivity.this, "", "Looking for Eye Trackers");
    }

	private void setUp() {
		WifiManager wifi = (WifiManager) getSystemService(android.content.Context.WIFI_SERVICE);
		mLock = wifi.createMulticastLock("mylockthereturn");
        mLock.setReferenceCounted(true);
        mLock.acquire();
        
        try {
			jmdns = JmDNS.create();
		} catch (IOException e) {
			e.printStackTrace();
		}
        
		jmdns.addServiceListener(LIVECAP_SERVICE_TYPE, new ServiceListener() {

			@Override
			public void serviceAdded(ServiceEvent event) {
				final String name = event.getName();
				handler.postDelayed(new Runnable() {

					@SuppressWarnings("unchecked")
					@Override
					public void run() {
						((ArrayAdapter<String>)getListAdapter()).add(name);
						if(!foundService) {
							foundService = true;
							pd.dismiss();
						}
					}
					
				}, 1);
			}

			@Override
			public void serviceRemoved(ServiceEvent event) {
				final String name = event.getName();
				handler.postDelayed(new Runnable() {

					@SuppressWarnings("unchecked")
					@Override
					public void run() {
						((ArrayAdapter<String>)getListAdapter()).remove(name);
					}
					
				}, 1);
			}

			@Override
			public void serviceResolved(ServiceEvent event) {
				
			}
			
		});
	}

	@Override
	protected void onStop() {
		if (jmdns != null) {
            if (listener != null) {
                jmdns.removeServiceListener(LIVECAP_SERVICE_TYPE, listener);
                listener = null;
            }
            try {
                jmdns.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            jmdns = null;
    	}
    	
        mLock.release();
		super.onStop();
	}

}