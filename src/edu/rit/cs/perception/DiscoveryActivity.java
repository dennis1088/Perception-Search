package edu.rit.cs.perception;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import android.app.ListActivity;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class DiscoveryActivity extends ListActivity implements ServiceListener,
		OnItemClickListener {

	public final static String LIVECAP_SERVICE_TYPE = "_pslivecap._tcp.local.";

	private JmDNS mJmdns;
	private MulticastLock mLock;
	private ArrayAdapter<String> mListAdapter;
	private ListView mListView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		
		mListAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, new ArrayList<String>());
		setListAdapter(mListAdapter);

		mListView = getListView();
		mListView.setTextFilterEnabled(true);
		mListView.setOnItemClickListener(this);

		try {
			mJmdns = JmDNS.create();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		setProgressBarIndeterminateVisibility(true);
	}

	@Override
	protected void onStart() {
		super.onStart();
		WifiManager wifi = (WifiManager) getSystemService(android.content.Context.WIFI_SERVICE);
		mLock = wifi.createMulticastLock("mylockthereturn");
		mLock.setReferenceCounted(true);
		mLock.acquire();
		
		new Thread(new Runnable(){

			@Override
			public void run() {
				startServiceListening();
			}
			
		}).start();
	}

	@Override
	protected void onStop() {
		super.onStop();
		mLock.release();
		mJmdns.removeServiceListener(LIVECAP_SERVICE_TYPE, this);
		mListAdapter.clear();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		try {
			mJmdns.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
		String name = (String) ((TextView) v).getText();
		ServiceInfo serviceInfo = mJmdns.getServiceInfo(LIVECAP_SERVICE_TYPE,
				name);
		String serviceName = serviceInfo.getName();
		String serviceAddress = serviceInfo.getHostAddresses()[0];
		int servicePort = serviceInfo.getPort();

		Intent startPreview = new Intent(this, PreviewActivity.class);
		startPreview.putExtra("name", serviceName);
		startPreview.putExtra("address", serviceAddress);
		startPreview.putExtra("port", servicePort);

		startActivity(startPreview);
	}

	@Override
	public void serviceAdded(ServiceEvent event) {
		final String name = event.getName();
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				mListAdapter.add(name);
			}

		});
	}

	@Override
	public void serviceRemoved(ServiceEvent event) {
		final String name = event.getName();
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				mListAdapter.remove(name);
			}

		});
	}

	@Override
	public void serviceResolved(ServiceEvent event) {

	}
	
	private void startServiceListening() {
		mJmdns.addServiceListener(LIVECAP_SERVICE_TYPE, this);
	}

}