package edu.rit.cs.perception;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicBoolean;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.mindprod.ledatastream.LEDataInputStream;
import com.mindprod.ledatastream.LEDataOutputStream;

public class PreviewActivity extends Activity implements OnClickListener {

	public final static String START_COMMAND = "stream:start";
	public final static String STOP_COMMAND = "stream:stop";
	public final static int COMMAND_TYPE = 1;
	public final static int EYE_IMAGE_TYPE = 2;
	public final static int SCENE_IMAGE_TYPE = 3;

	private Socket mServerSocket;
	private LEDataInputStream mIn;
	private LEDataOutputStream mOut;
	private AtomicBoolean isReading = new AtomicBoolean(false);
	private ImageView mSceneImage;

	private Runnable readingStream = new Runnable() {

		@Override
		public void run() {
			while (isReading.get()) {

				try {
					int sizeOfData = mIn.readInt();
					int typeOfData = mIn.readInt();

					Log.i("Perception Search", "Read header with dataSize: "
							+ sizeOfData + ", dataType:" + typeOfData);

					final byte[] data = new byte[sizeOfData];

					int bytesRead = 0;
					while (bytesRead != sizeOfData) {
						bytesRead += mIn.read(data, bytesRead, sizeOfData
								- bytesRead);
					}

					switch (typeOfData) {
					case COMMAND_TYPE:
						String command = new String(data);
						Log.i("Perception Search", "Command recieved: "
								+ command);
						break;
					case EYE_IMAGE_TYPE:
						break;
					case SCENE_IMAGE_TYPE:
						runOnUiThread(new Runnable() {

							@Override
							public void run() {
								Bitmap bMap = BitmapFactory.decodeByteArray(
										data, 0, data.length);
								mSceneImage.setImageBitmap(bMap);
							}

						});
						break;
					default:
						Log.i("Perception Search",
								"Recived data of unknown type: " + typeOfData);
						break;
					}
				} catch (IOException e) {
					Log.e("Perception Search",
							"Could not read header and data.");
				}

			}
		}

	};

	/**
	 * 
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.preview_layout);

		Bundle extras = getIntent().getExtras();
		String name = extras.getString("name");
		String address = extras.getString("address");
		int port = extras.getInt("port");

		TextView nameView = (TextView) findViewById(R.id.serverName);
		nameView.setText(name);

		mSceneImage = (ImageView) findViewById(R.id.previewImage);

		ToggleButton previewButton = (ToggleButton) findViewById(R.id.toggleButton1);
		previewButton.setOnClickListener(this);

		createConnection(address, port);

		isReading.set(true);

		new Thread(readingStream).start();
	}

	/**
	 * 
	 */
	@Override
	protected void onStart() {
		super.onStart();
	}

	/**
	 * 
	 * @param address
	 * @param port
	 */
	private void createConnection(String address, int port) {
		try {
			mServerSocket = new Socket(address, port);
			mIn = new LEDataInputStream(mServerSocket.getInputStream());
			mOut = new LEDataOutputStream(mServerSocket.getOutputStream());
		} catch (UnknownHostException e) {
			Log.e("Perception Search", "Could not find host.");
		} catch (IOException e) {
			Log.e("Perception Search", "Could not create connection with host.");
		}

	}

	/**
	 * 
	 */
	@Override
	public void onClick(View v) {
		if (((ToggleButton) v).isChecked()) {
			sendCommand(START_COMMAND);
		} else {
			sendCommand(STOP_COMMAND);
		}
	}

	/**
	 * 
	 * @param startCommand
	 */
	private void sendCommand(String command) {
		try {
			mOut.writeInt(command.length());
			mOut.writeInt(COMMAND_TYPE);
			mOut.writeBytes(command);
		} catch (IOException e) {
			Log.e("Perception Search", "Could not send command: " + command);
		}
	}

	/**
	 * 
	 */
	@Override
	protected void onStop() {
		super.onStop();
	}

	/**
	 * 
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();

		isReading.set(false);

		try {
			mOut.close();
			mIn.close();
			mServerSocket.close();
		} catch (IOException e) {
			Log.e("Perception Search", "Could not close connection.");
		}
	}

}
