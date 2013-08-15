package edu.buffalo.cse.cse486586.simpledynamo;




import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class OnPut3ClickListener implements OnClickListener 
{

	private final TextView mTextView;
	private final ContentResolver mContentResolver;
	private final Uri mUri;
	private  ContentValues[] mContentValues;
	private static final String KEY_FIELD = "key";
	private static final String VALUE_FIELD = "value";

	public OnPut3ClickListener(TextView _tv, ContentResolver _cr) 
	{
		mTextView = _tv;
		mContentResolver = _cr;
		mUri = buildUri("content", "edu.buffalo.cse.cse486586.simpledynamo.provider");
	
	}

	private Uri buildUri(String scheme, String authority) {
		Uri.Builder uriBuilder = new Uri.Builder();
		uriBuilder.authority(authority);
		uriBuilder.scheme(scheme);
		return uriBuilder.build();
	}

	private ContentValues[] initTestValues() {
		ContentValues[] cv = new ContentValues[20];
		for (int i = 0; i < 20; i++) {
			cv[i] = new ContentValues();
			cv[i].put(KEY_FIELD, Integer.toString(i));
			cv[i].put(VALUE_FIELD, "Put3" + Integer.toString(i)+"ver"+SimpleDynamoProvider.myVersionNumber);
		}

		return cv;
	}

	

	@Override
	public void onClick(View arg0) {
		// TODO Auto-generated method stub
		SimpleDynamoProvider.myVersionNumber++;
		mContentValues = initTestValues();
		Put3Thread Put3Thread = new Put3Thread(mTextView,mUri,mContentResolver,mContentValues);
		Put3Thread.start();
		
	}
	}