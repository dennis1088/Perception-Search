package edu.rit.cs.perception;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

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

public class PreviewActivity extends Activity {

	public final static String START_COMMAND = "stream:start";
	public final static String STOP_COMMAND = "stream:stop";
	public final static int COMMAND_TYPE = 1;
	public final static int EYE_IMAGE_TYPE = 2;
	public final static int SCENE_IMAGE_TYPE = 3;

	Socket mServerSocket = null;
	InputStream mIn = null;
	OutputStream mOut = null;
	boolean isStreaming = false;
	ImageView mSceneImage;

	private Runnable streamImages = new Runnable() {

		@Override
		public void run() {
			try {
				sendCommand(START_COMMAND);
				while (isStreaming)
					readBytes();
				sendCommand(STOP_COMMAND);
			} catch (IOException e) {

			}
		}

	};

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// setting the content layout
		super.onCreate(savedInstanceState);
		setContentView(R.layout.preview_layout);

		// retrieving information from previous activity
		Bundle extras = getIntent().getExtras();
		String name = extras.getString("name");
		String address = extras.getString("address");
		int port = extras.getInt("port");

		// setting the name of the eye tracker
		TextView nameView = (TextView) findViewById(R.id.serverName);
		nameView.setText(name);

		// getting the image view to change
		mSceneImage = (ImageView) findViewById(R.id.previewImage);

		// Connect with the eye tracker
		createConnection(address, port);

		// adding listener to toggle button
		final ToggleButton streamButton = (ToggleButton) findViewById(R.id.toggleButton1);
		streamButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (streamButton.isChecked()) {
					isStreaming = true;
					new Thread(streamImages).start();
				} else {
					isStreaming = false;
				}
			}
		});
	}

	private void readBytes() throws IOException {
		if (mIn.available() != -1) {
			int bytesRead = 0;
			int bytesToRead = 8;
			byte[] header = new byte[8];
			final byte[] data;
			int sizeOfData;
			int dataType;
			ByteBuffer converter;

			// read header
			while (bytesRead != bytesToRead) {
				bytesRead += mIn.read(header, bytesRead, bytesToRead
						- bytesRead);
			}

			if (bytesRead == bytesToRead) {
				bytesRead = 0;
				converter = ByteBuffer.wrap(header);
				sizeOfData = swap(converter.getInt());
				dataType = swap(converter.getInt());

				data = new byte[sizeOfData];

				// read data
				while (bytesRead != sizeOfData) {
					bytesRead += mIn.read(data, bytesRead, sizeOfData
							- bytesRead);
				}

				switch (dataType) {
				// Command
				case 1:

					break;
				// Eye image
				case 2:

					break;
				// Scene Image
				case 3:
					runOnUiThread(new Runnable() {

						@Override
						public void run() {
							Bitmap bMap = BitmapFactory.decodeByteArray(data,
									0, data.length);
							mSceneImage.setImageBitmap(bMap);
						}

					});
					break;
				// FileData
				case 4:

					break;
				// Invalid dataType
				default:
					Log.e("Error Found: ", "Invalid data type");
					break;
				}

			} else {
				Log.e("Error Found: ", "Header Recieved is invalid");
			}
		} else {
			// Read end of file
		}

	}

	/**
	 * 
	 */
	@Override
	protected void onStop() {
		try {
			mOut.close();
			mIn.close();
			mServerSocket.close();
		} catch (IOException e) {
			Log.e("Error found: ", e.getMessage());
		}

		super.onStop();
	}

	/**
	 * Creates a connection with the server
	 * 
	 * @param address
	 *            address of the server
	 * @param port
	 *            port number of the server
	 */
	private void createConnection(String address, int port) {
		try {
			mServerSocket = new Socket(address, port);
			mIn = mServerSocket.getInputStream();
			mOut = mServerSocket.getOutputStream();
		} catch (UnknownHostException e) {
			Log.e("Exception Found: ", e.getMessage());
		} catch (IOException e) {
			Log.e("Exception Found: ", e.getMessage());
		}
	}

	/**
	 * Sends command to the output stream
	 * 
	 * @param command
	 *            command to send to server
	 * @throws IOException
	 *             delivery of command not successful
	 */
	private void sendCommand(String command) throws IOException {
		ByteBuffer commandBuffer = ByteBuffer.allocate(command.length());
		commandBuffer.put(command.getBytes());
		commandBuffer.order(ByteOrder.LITTLE_ENDIAN);
		commandBuffer.rewind();

		byte[] commandBytes = new byte[commandBuffer.remaining()];
		commandBuffer.get(commandBytes);

		byte[] length = intToByteArray(swap(commandBytes.length));
		byte[] type = intToByteArray(swap(COMMAND_TYPE));

		mOut.write(length);
		mOut.write(type);
		mOut.write(commandBytes);
	}

	/**
	 * Converting int to byte array in little endian
	 * 
	 * @param value
	 *            number in little endian form
	 * @return
	 */
	private byte[] intToByteArray(int value) {
		return new byte[] { (byte) (value >>> 24), (byte) (value >>> 16),
				(byte) (value >>> 8), (byte) value };
	}

	/**
	 * Changing int to little endian
	 * 
	 * @param value
	 *            number to convert to little endian
	 * @return
	 */
	private int swap(int value) {
		int b1 = (value >> 0) & 0xff;
		int b2 = (value >> 8) & 0xff;
		int b3 = (value >> 16) & 0xff;
		int b4 = (value >> 24) & 0xff;

		return b1 << 24 | b2 << 16 | b3 << 8 | b4 << 0;
	}

}
