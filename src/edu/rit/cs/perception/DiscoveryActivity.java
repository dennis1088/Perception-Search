package edu.rit.cs.perception;

import java.io.IOException;
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
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class DiscoveryActivity extends ListActivity implements ServiceListener,
		OnItemClickListener {

	public final static String LIVECAP_SERVICE_TYPE = "_http._tcp.local.";
	public final static String SEARCH_PROGRESS_MESSAGE = "Searching for Trackers...";

	private JmDNS mJmdns;
	private MulticastLock mLock;
	private ArrayAdapter<String> mListAdapter;
	private ListView mListView;
	//private Handler handler = new Handler();

	/**
	 * This method is called when the activity is created. Usually global
	 * resources are created here to be used throughout the activity. UI
	 * Elements are also configured.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Asking for progress on title bar feature
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		// Setting the style of items for this list
		mListAdapter = new ArrayAdapter<String>(this,
				R.layout.discovery_list_item, R.id.label,
				new ArrayList<String>());
		setListAdapter(mListAdapter);

		// enabling list filtering through keyboard and adding listener
		mListView = getListView();
		mListView.setTextFilterEnabled(true);
		mListView.setOnItemClickListener(this);

		// Enabling the progress bar on this activity
		setProgressBarIndeterminateVisibility(true);
	}

	/**
	 * This method is called after onCreate() and is called when an activity is
	 * back in focus. Resources that are only used during the visual lifetime of
	 * an activity should be created here and destroyed in onStop() ex.
	 * MulticastLock should only be acquired when an activity is using it to
	 * allow other activities to use the lock as well.
	 */
	@Override
	protected void onStart() {
		super.onStart();

		// Acquiring multicast lock this is needed on android to receive Wifi
		// Multicasts packets
		// these packets is how services are discovered on the network
		WifiManager wifi = (WifiManager) getSystemService(android.content.Context.WIFI_SERVICE);
		mLock = wifi.createMulticastLock("mylockthereturn");
		mLock.setReferenceCounted(true);
		mLock.acquire();

		// Must create the Jmdns object after acquiring lock
		// Adding this class as a service listener
		try {
			mJmdns = JmDNS.create();
			mJmdns.addServiceListener(LIVECAP_SERVICE_TYPE, this);
		} catch (IOException e) {
			Log.e("Perception Search", "Error creating the JmDNS object.");
		}
	}

	/**
	 * This method is called when a service has been found. This method adds the
	 * name of the service to the list. It uses a utility called runOnUiThread()
	 * in order to perform a update to the GUI this is recommended so that the
	 * operation does not lock up the GUI thread.
	 */
	@Override
	public void serviceAdded(ServiceEvent event) {
		final String name = event.getName();
		Log.i("Perception Search", "Service Added: "+ name);
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				// adding the service name to the list activity
				mListAdapter.add(name);
			}

		});
	}

	/**
	 * This method is called when a service has been removed from the network.
	 * It removes the service name from the list.
	 */
	@Override
	public void serviceRemoved(ServiceEvent event) {
		final String name = event.getName();
		Log.i("Perception Search", "Service Removed: "+ name);
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				// removing the service name from the list activity
				mListAdapter.remove(name);
			}

		});
	}

	/**
	 * This method is called when a service has been resolved. Resolved means
	 * service details have been found. The implementation for this method is
	 * currently not needed in this activity but in the future may be used.
	 */
	@Override
	public void serviceResolved(ServiceEvent event) {

	}

	/**
	 * This method is called when a user has clicked on a list item. This method
	 * retrieves the name of what was clicked and tries get service information
	 * for that service in order to retrieve the ip address and port number of
	 * the service. This service is then placed in a bundle and sent to the next
	 * activity.
	 */
	@Override
	public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
		// getting the name of the item clicked by the user
		final String name = (String) ((TextView) ((LinearLayout) v)
				.getChildAt(1)).getText();

		// Resolving the service i.e. getting the ip address and port number
		new Thread(new Runnable() {

			@Override
			public void run() {
				// Resolving service
				ServiceInfo serviceInfo = mJmdns.getServiceInfo(
						LIVECAP_SERVICE_TYPE, name);
				String serviceName = serviceInfo.getName();
				String serviceAddress = serviceInfo.getHostAddresses()[0];
				int servicePort = serviceInfo.getPort();

				// Placing ip address and port number of tracker in bundle to
				// pass along to next activity
				Intent startPreview = new Intent(DiscoveryActivity.this,
						PreviewActivity.class);
				startPreview.putExtra("name", serviceName);
				startPreview.putExtra("address", serviceAddress);
				startPreview.putExtra("port", servicePort);

				// starting the next activity
				startActivity(startPreview);
			}

		}).start();
	}

	/**
	 * This method is called when an activity is no longer visible on the
	 * device. Here resources created in onStart() should be destoyed.
	 */
	@Override
	protected void onStop() {
		super.onStop();

		// must close the JmDNS object first before releasing object
		try {
			mJmdns.close();
		} catch (IOException e) {
		}

		mLock.release();
		mJmdns.removeServiceListener(LIVECAP_SERVICE_TYPE, this);
		mListAdapter.clear();
	}

}