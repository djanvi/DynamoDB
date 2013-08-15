
package edu.buffalo.cse.cse486586.simpledynamo;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

public class Put3Thread extends Thread {
	
	
	private final TextView mTextView;
	private final ContentResolver mContentResolver;
	private final Uri mUri;
	private final ContentValues[] mContentValues;
	
	
	

	String key;
	String value;
	protected	String nl = System.getProperty("line.separator");
	public Put3Thread(TextView mTextView,Uri mUri,ContentResolver mContentResolver,ContentValues[] mContentValues)
	{
		this.mTextView=mTextView;
		this.mUri=mUri;
		this.mContentResolver=mContentResolver;
		this.mContentValues=mContentValues;
	}
	
	
	
	
	public  void run()
	{
		try {
			for (int i = 0; i < 20; i++) {
				mContentResolver.insert(mUri, mContentValues[i]);
				Thread.sleep(1000);
			}
		} catch (Exception e) {
			Log.e("Error", e.toString());
		
		}
	}
}
