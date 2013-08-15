package edu.buffalo.cse.cse486586.simpledynamo;


import android.content.ContentResolver;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;

public class lDumpClickListener implements OnClickListener {

	private final TextView mTextView;
	private final ContentResolver mContentResolver;
	private final Uri mUri;
	Handler handler = new Handler();
	String key;
	String value;
	protected	String nl = System.getProperty("line.separator");
	public lDumpClickListener(TextView _tv, ContentResolver _cr) {
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
	
	
	
	public void onClick(View arg0) {
		// TODO Auto-generated method stub
		final Cursor resultCursor = mContentResolver.query(mUri, null,null, null, "LDUMP");
		mTextView.setText("");
		resultCursor.moveToFirst();
		handler.post(new Runnable() {						  
			 @Override 
			 public void run() { 
			
				 
				 for(int i=0;i<resultCursor.getCount();i++)
				 {
				
					 key=resultCursor.getString(resultCursor.getColumnIndex("key"));
					 value=resultCursor.getString(resultCursor.getColumnIndex("value"));
					 String temp[] = value.split("ver");
					 value=temp[0];
					 mTextView.append("KEY:"+key);
					 mTextView.append(" VALUE:"+value);
					 mTextView.append(nl);
					 resultCursor.moveToNext();
				 }
			 

			  } });
		
		
	}

}
