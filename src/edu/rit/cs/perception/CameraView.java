package edu.rit.cs.perception;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.tomgibara.android.camera.CameraSource;
import com.tomgibara.android.camera.HttpCamera;

public class CameraView extends SurfaceView implements SurfaceHolder.Callback {

	/**
	 * Handle to the application context, used to e.g. fetch Drawables.
	 */
	@SuppressWarnings("unused")
	private Context mContext;

	/**
	 * The thread that actually draws the camera preview.
	 */
	private CameraThread thread;

	public CameraView(Context context, AttributeSet attrs) {
		super(context, attrs);

		// register our interest in hearing about changes to our surface
		SurfaceHolder holder = getHolder();
		holder.addCallback(this);

		// create thread only; it's started in surfaceCreated()
		thread = new CameraThread(holder, context);

		setFocusable(true); // make sure we get key events
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// TODO Auto-generated method stub

	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// start the thread here so that we don't busy-wait in run()
		// waiting for the surface to be created
		thread.setRunning(true);
		thread.start();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// we have to tell thread to shut down & wait for it to finish, or else
		// it might touch the Surface after we return and explode
		boolean retry = true;
		thread.setRunning(false);
		while (retry) {
			try {
				thread.join();
				retry = false;
			} catch (InterruptedException e) {
			}
		}
	}

	class CameraThread extends Thread {

		private CameraSource camera = new HttpCamera(
				"http://129.21.60.184:8080/webcam.jpg", 640, 480, true);

		/**
		 * Handle to the surface manager object we interact with.
		 */
		private SurfaceHolder mSurfaceHolder;

		/**
		 * Indicate whether the surface has been created & is ready to draw.
		 */
		private boolean mRun = false;

		public CameraThread(SurfaceHolder surfaceHolder, Context context) {
			mSurfaceHolder = surfaceHolder;
			mContext = context;
			camera.open();
		}

		/**
		 * Used to signal the thread whether it should be running or not.
		 * Passing true allows the thread to run; passing false will shut it
		 * down if it's already running. Calling start() after this was most
		 * recently called with false will result in an immediate shutdown.
		 * 
		 * @param b
		 *            true to run, false to shut down
		 */
		public void setRunning(boolean b) {
			mRun = b;
		}

		public void run() {
			while (mRun) {
				Canvas c = null;
				try {
					c = mSurfaceHolder.lockCanvas(null);
					synchronized (mSurfaceHolder) {
						camera.capture(c);
					}
				} finally {
					// do this in a finally so that if an exception is thrown
					// during the above, we don't leave the Surface in an
					// inconsistent state
					if (c != null) {
						mSurfaceHolder.unlockCanvasAndPost(c);
					}
				}
			}
		}
	}

}
