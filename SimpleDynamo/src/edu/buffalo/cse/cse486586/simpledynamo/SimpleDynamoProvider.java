package edu.buffalo.cse.cse486586.simpledynamo;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import java.net.UnknownHostException;
import android.content.ContentUris;
import android.content.Context;
import android.content.UriMatcher;
import android.database.MatrixCursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.telephony.TelephonyManager;
import android.util.Log;


public class SimpleDynamoProvider extends ContentProvider {
	ServerSocket serverSocket=null;
	public static final String contentUri="edu.buffalo.cse.cse486586.simpledynamo.provider";
	String mySuccessorHash;
	String myPredecessorHash;
	static String mySuccessorDevID;
	String myPredecessorDevID;
	static String myDeviceId;
	static String myIdentifier;
	int nodeCountInRing=1;
	private SQLiteDatabase simpleDynamoDB;
	simpleDynamoDBHelper createDatabase;
	private static final String dbName = "simpleDynamoDB";
	private static final int dbVersion = 10;
	private static final String tableName = "simpleDynamoTable";
	private static final int messages = 1;
	//column names
	public static String msgId="msgId";
	public static String key="key";
	public static String value="value";

	public static String tempKey=null;
	public static String tempValue=null;

	public static String keyMsg;
	public static String valueMsg;

	private static UriMatcher uriMatcher;
	public static String keyvaluedata=null;
	static int myVersionNumber=0;

	ArrayList<ArrayList<String>> keyvaluePair = new ArrayList<ArrayList<String>>();
	static{
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(contentUri, tableName, 1);
		/*	msgProjMap = new HashMap<String, String>();
		msgProjMap.put(key, key);
		msgProjMap.put(value, value);
		 */
	}


	private static final String createTable = "CREATE TABLE " + tableName + "("



			+key + " LONGTEXT," + value	+ " LONGTEXT );";

	// DB Helper class

	private static class simpleDynamoDBHelper extends SQLiteOpenHelper
	{

		public simpleDynamoDBHelper(Context context) {
			super(context, dbName, null, dbVersion);
			// TODO Auto-generated constructor stub
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			// TODO Auto-generated method stub
			db.execSQL(createTable);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// TODO Auto-generated method stub
			db.execSQL("DROP TABLE IF EXISTS " + tableName);
			onCreate(db);

		}


	}





	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Uri insert(Uri iuri, ContentValues values) 
	{
		System.out.println("In insert");
		String key;
		int nodePresent;
		int valueVersionNo;
		String keyHash=null;
		SQLiteDatabase writeDB= createDatabase.getWritableDatabase();

		System.out.println("value"+values.getAsString("value"));
		key = values.getAsString("key");
		System.out.println("key:"+key);
		String temp1[] = values.getAsString("value").split("ver");
		valueVersionNo=Integer.parseInt(temp1[1]);
		try {
			keyHash=genHash(key);
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//---to insert-----

		if(keyHash.compareTo(myIdentifier)<=0 && myPredecessorHash.compareTo(myIdentifier)>=0)
		{
			//accept
			if(valueVersionNo>=myVersionNumber)
			{	
				writeDB.delete(tableName, "key="+"'"+key+"'", null);
				long rowID = writeDB.insert(tableName, null, values);
				Log.v("Inserted:","key:"+values.getAsString("key")+"value:"+values.getAsString("value"));
				if (rowID > 0)
				{
					Uri uri = ContentUris.withAppendedId(Uri.parse(contentUri), rowID);
					getContext().getContentResolver().notifyChange(uri,null);

					//return uri;
				}
				myVersionNumber=valueVersionNo;
				replicate(values);
			}else
			{Log.v("Discarding:","key:"+values.getAsString("key")+"value:"+values.getAsString("value"));}
		}
		else if(keyHash.compareTo(myIdentifier)<=0 && keyHash.compareTo(myPredecessorHash)>0)
		{
			//accept
			if(valueVersionNo>=myVersionNumber)
			{
				writeDB.delete(tableName, "key="+"'"+key+"'", null);
				long rowID = writeDB.insert(tableName, null, values);
			
				Log.v("Inserted:","key:"+values.getAsString("key")+"value:"+values.getAsString("value"));
				if (rowID > 0)
				{
					Uri uri = ContentUris.withAppendedId(Uri.parse(contentUri), rowID);
					getContext().getContentResolver().notifyChange(uri,null);

					//return uri;
				}
				myVersionNumber=valueVersionNo;
				replicate(values);
			}else
			{Log.v("Discarding:","key:"+values.getAsString("key")+"value:"+values.getAsString("value"));}

		}
		else if(myPredecessorHash.compareTo(myIdentifier)>=0 && keyHash.compareTo(myPredecessorHash)>0)
		{
			//accept
			if(valueVersionNo>=myVersionNumber)
			{
				writeDB.delete(tableName, "key="+"'"+key+"'", null);
				long rowID = writeDB.insert(tableName, null, values);
				Log.v("Inserted:","key:"+values.getAsString("key")+"value:"+values.getAsString("value"));
			
				if (rowID > 0)
				{
					Uri uri = ContentUris.withAppendedId(Uri.parse(contentUri), rowID);
					getContext().getContentResolver().notifyChange(uri,null);

					//return uri;
				}
				//throw new SQLException("Failed to insert into " + iuri);
				myVersionNumber=valueVersionNo;
				replicate(values);
			}else
			{Log.v("Discarding:","key:"+values.getAsString("key")+"value:"+values.getAsString("value"));}
		}
		//it belongs to my successor
		else if(keyHash.compareTo(mySuccessorHash)<=0 && myIdentifier.compareTo(mySuccessorHash)>=0)
		{
			//accept
			MessageFormat messageToForward= new MessageFormat();
			messageToForward.key=values.getAsString("key");
			messageToForward.value=values.getAsString("value");
			messageToForward.devID=mySuccessorDevID;
			messageToForward.cordinatorID=myDeviceId;
			messageToForward.messageType="INSERT_REQ";
			nodePresent=sendMessage(messageToForward,messageToForward.devID);
			if(nodePresent==0)
			{
				Log.v("Didnt receve the ack","node failed");
				//send to its successor
				messageToForward.devID=myPredecessorDevID;
				sendMessage(messageToForward,messageToForward.devID);

			}
			/*try {
				serverSocket.setSoTimeout(5000);
			} catch (IOException e) {
				// TODO Auto-generated catch block

				/
				e.printStackTrace();
			}*/

		}
		else if(keyHash.compareTo(mySuccessorHash)<=0 && keyHash.compareTo(myIdentifier)>0)
		{
			MessageFormat messageToForward= new MessageFormat();
			messageToForward.key=values.getAsString("key");
			messageToForward.value=values.getAsString("value");
			messageToForward.devID=mySuccessorDevID;
			messageToForward.cordinatorID=myDeviceId;
			messageToForward.messageType="INSERT_REQ";
			nodePresent=sendMessage(messageToForward,messageToForward.devID);
			if(nodePresent==0)
			{
				Log.v("Didnt receve the ack","node failed");
				//send to its successor
				messageToForward.devID=myPredecessorDevID;
				sendMessage(messageToForward,messageToForward.devID);

			}
			/*try {
				serverSocket.setSoTimeout(5000);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				Log.v("Didnt receve the ack","node failed");
				//send to its successor
				e.printStackTrace();
			}*/

		}
		else if(myIdentifier.compareTo(mySuccessorHash)>=0 && keyHash.compareTo(myIdentifier)>0)
		{
			MessageFormat messageToForward= new MessageFormat();
			messageToForward.key=values.getAsString("key");
			messageToForward.value=values.getAsString("value");
			messageToForward.devID=mySuccessorDevID;
			messageToForward.cordinatorID=myDeviceId;
			messageToForward.messageType="INSERT_REQ";
			nodePresent=sendMessage(messageToForward,messageToForward.devID);
			if(nodePresent==0)
			{
				Log.v("Didnt receve the ack","node failed");
				//send to its successor
				messageToForward.devID=myPredecessorDevID;
				sendMessage(messageToForward,messageToForward.devID);

			}
			/*try {
				serverSocket.setSoTimeout(5000);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				Log.v("Didnt receve the ack","node failed");
				//send to its successor
				e.printStackTrace();
			}*/
		}
		// it belongs to my predeccesor

		else if(keyHash.compareTo(myPredecessorHash)<=0 && mySuccessorHash.compareTo(myPredecessorHash)>=0)
		{
			//accept

			MessageFormat messageToForward= new MessageFormat();
			messageToForward.key=values.getAsString("key");
			messageToForward.value=values.getAsString("value");
			messageToForward.devID=myPredecessorDevID;
			messageToForward.cordinatorID=myDeviceId;
			messageToForward.messageType="INSERT_REQ";
			nodePresent=sendMessage(messageToForward,messageToForward.devID);
			if(nodePresent==0)
			{
				Log.v("Didnt receve the ack","node failed");
				//send to its successor thats me, so accept

				/*messageToForward.devID=myPredecessorDevID;
				sendMessage(messageToForward,messageToForward.devID);*/
				if(valueVersionNo>=myVersionNumber)
				{
					writeDB.delete(tableName, "key="+"'"+key+"'", null);
					long rowID = writeDB.insert(tableName, null, values);
					Log.v("Inserted:","key:"+values.getAsString("key")+"value:"+values.getAsString("value"));
					
					if (rowID > 0)
					{
						Uri uri = ContentUris.withAppendedId(Uri.parse(contentUri), rowID);
						getContext().getContentResolver().notifyChange(uri,null);

						//return uri;
					}
					//throw new SQLException("Failed to insert into " + iuri);
					myVersionNumber=valueVersionNo;
					replicate(values);
				}else
				{Log.v("Discarding:","key:"+values.getAsString("key")+"value:"+values.getAsString("value"));}



			}
			/*try {
				serverSocket.setSoTimeout(5000);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				Log.v("Didnt receve the ack","node failed");
				//send to its successor
				e.printStackTrace();
			}*/
		}
		else if(keyHash.compareTo(myPredecessorHash)<=0 && keyHash.compareTo(mySuccessorHash)>0)
		{
			//accept
			MessageFormat messageToForward= new MessageFormat();
			messageToForward.key=values.getAsString("key");
			messageToForward.value=values.getAsString("value");
			messageToForward.devID=myPredecessorDevID;
			messageToForward.cordinatorID=myDeviceId;
			messageToForward.messageType="INSERT_REQ";
			nodePresent=sendMessage(messageToForward,messageToForward.devID);
			if(nodePresent==0)
			{
				Log.v("Didnt receve the ack","node failed");
				//send to its successor
				if(valueVersionNo>=myVersionNumber)
				{
					writeDB.delete(tableName, "key="+"'"+key+"'", null);
					long rowID = writeDB.insert(tableName, null, values);
					Log.v("Inserted:","key:"+values.getAsString("key")+"value:"+values.getAsString("value"));
					
					if (rowID > 0)
					{
						Uri uri = ContentUris.withAppendedId(Uri.parse(contentUri), rowID);
						getContext().getContentResolver().notifyChange(uri,null);

						//return uri;
					}
					//throw new SQLException("Failed to insert into " + iuri);
					myVersionNumber=valueVersionNo;
					replicate(values);
				}else
				{Log.v("Discarding:","key:"+values.getAsString("key")+"value:"+values.getAsString("value"));}
			}
			/*try {
				serverSocket.setSoTimeout(5000);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				Log.v("Didnt receve the ack","node failed");
				//send to its successor
				e.printStackTrace();
			}*/
		}
		else if(mySuccessorHash.compareTo(myPredecessorHash)>=0 && keyHash.compareTo(mySuccessorHash)>0)
		{
			MessageFormat messageToForward= new MessageFormat();
			messageToForward.key=values.getAsString("key");
			messageToForward.value=values.getAsString("value");
			messageToForward.devID=myPredecessorDevID;
			messageToForward.cordinatorID=myDeviceId;
			messageToForward.messageType="INSERT_REQ";
			nodePresent=sendMessage(messageToForward,messageToForward.devID);
			if(nodePresent==0)
			{
				Log.v("Didnt receve the ack","node failed");
				//send to its successor
				if(valueVersionNo>=myVersionNumber)
				{
					writeDB.delete(tableName, "key="+"'"+key+"'", null);
					long rowID = writeDB.insert(tableName, null, values);
					Log.v("Inserted:","key:"+values.getAsString("key")+"value:"+values.getAsString("value"));
					
					if (rowID > 0)
					{
						Uri uri = ContentUris.withAppendedId(Uri.parse(contentUri), rowID);
						getContext().getContentResolver().notifyChange(uri,null);

						//return uri;
					}
					//throw new SQLException("Failed to insert into " + iuri);
					myVersionNumber=valueVersionNo;
					replicate(values);
				}else
				{Log.v("Discarding:","key:"+values.getAsString("key")+"value:"+values.getAsString("value"));}


			}
			/*try {
				serverSocket.setSoTimeout(5000);
			}  catch (SocketException e) {
				// TODO Auto-generated catch block
				Log.v("Didnt receve the ack","node failed");
				//send to its successor
				e.printStackTrace();
			}*/

		}

		else
		{
			//forward to successor
			System.out.println("Your code is wrong");


		}


		return ContentUris.withAppendedId(Uri.parse(contentUri), 0);




		// TODO Auto-generated method stub

	}

	private void replicate(ContentValues values) 
	{
		// TODO Auto-generated method stub
		MessageFormat message1 = new MessageFormat();
		message1.devID=mySuccessorDevID;
		message1.key=values.getAsString("key");
		message1.value=values.getAsString("value");
		message1.messageType="REPLICATE_REQ";
		int a=sendMessage(message1,myDeviceId);

		MessageFormat message2 = new MessageFormat();
		message2.devID=myPredecessorDevID;
		message2.key=values.getAsString("key");
		message2.value=values.getAsString("value");
		message2.messageType="REPLICATE_REQ";
		int b=sendMessage(message2,myDeviceId);



	}

	@Override
	public boolean onCreate() {
		// TODO Auto-generated method stub

		Context context = getContext();
		context.deleteDatabase(dbName);
		createDatabase= new simpleDynamoDBHelper(context);



		TelephonyManager tel = (TelephonyManager)getContext().getSystemService(Context.TELEPHONY_SERVICE);
		myDeviceId= tel.getLine1Number().substring(tel.getLine1Number().length()-4);

		Log.v("My deice id",myDeviceId );
		try {
			myIdentifier=genHash(myDeviceId);
			Log.v("My hash id",myIdentifier );
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}







		if(Integer.parseInt(myDeviceId) == 5554)
		{

			mySuccessorDevID="5558";
			try {
				mySuccessorHash=genHash(mySuccessorDevID);
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			myPredecessorDevID="5556";
			try {
				myPredecessorHash=genHash(myPredecessorDevID);
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			Log.v("My successor",mySuccessorDevID );
			Log.v("My predecc",myPredecessorDevID );


		}else if(Integer.parseInt(myDeviceId) == 5556)
		{

			mySuccessorDevID="5554";
			try {
				mySuccessorHash=genHash(mySuccessorDevID);
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			myPredecessorDevID="5558";
			try {
				myPredecessorHash=genHash(myPredecessorDevID);
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			Log.v("My successor",mySuccessorDevID );
			Log.v("My predecc",myPredecessorDevID );


		}
		else if(Integer.parseInt(myDeviceId) == 5558)
		{

			mySuccessorDevID="5556";
			try {
				mySuccessorHash=genHash(mySuccessorDevID);
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			myPredecessorDevID="5554";
			try {
				myPredecessorHash=genHash(myPredecessorDevID);
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			Log.v("My successor",mySuccessorDevID );
			Log.v("My predecc",myPredecessorDevID );


		}

		ServerThread serverThread = new ServerThread();
		serverThread.start();
		RecoveryThread recoveryThread = new RecoveryThread(mySuccessorDevID, myDeviceId);
		recoveryThread.start();
		return true;

	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
			String sortOrder) {
		// TODO Auto-generated method stub
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		qb.setTables(tableName);
		Log.v("In query", "searching for key"+selection);
		SQLiteDatabase readDB= createDatabase.getReadableDatabase();
		String whereclause= key +"=" + "'"+selection+"'";


		// execute the query against the database
		//Cursor resultCursor = qb.query(readDB, projection,whereclause,selectionArgs, null, null, null);

		if(selection!=null)
		{	
			Cursor resultCursor = qb.query(readDB, projection,whereclause,selectionArgs, null, null, null);
			resultCursor.moveToFirst();
			return resultCursor;

		}
		else if(sortOrder.contentEquals("LDUMP")||sortOrder.contentEquals("GET"))
		{
			Log.v("In query", "ko's query- correct");
			Cursor resultCursor = qb.query(readDB, projection,null,selectionArgs, null, null, null);
			return resultCursor;
		}
		else
		{

			Log.v("In query", "ko's query - code wrong");
			return null;
		} 



	}


	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}

	private String genHash(String input) throws NoSuchAlgorithmException {
		MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
		byte[] sha1Hash = sha1.digest(input.getBytes());
		Formatter formatter = new Formatter();
		for (byte b : sha1Hash) {
			formatter.format("%02x", b);
		}
		return formatter.toString();
	}






	public class ServerThread  extends Thread {

		static final String TAG="Received msg by Server";
		String receivedMsg;

		ObjectInputStream 	in;
		ObjectOutputStream 	out;
		MessageFormat recvData;

		public void run()
		{
			try
			{
				serverSocket=new ServerSocket(10000);

			}
			catch(Exception e)
			{

				e.printStackTrace();
			}

			while(true)
			{
				try
				{	
					Socket incomingSocket=serverSocket.accept();
					Log.v("SimpleDynamo", "Connection received");
					in = new ObjectInputStream(incomingSocket.getInputStream());
					recvData=(MessageFormat) in.readObject();
					Log.v("SimpleDynamo", "data received"+recvData.messageType);


					if(recvData.messageType.equals("INSERT_REQ"))
					{
						String tempStr[] =recvData.value.split("ver");
						int valueVersionNo= Integer.parseInt(tempStr[1]);
						SQLiteDatabase writeDB= createDatabase.getWritableDatabase();
						//						
						if(valueVersionNo>=myVersionNumber)
						{
						writeDB.delete(tableName, "key="+"'"+recvData.key+"'", null);

						ContentValues values = new ContentValues();

						values.put("key", recvData.key);
						values.put("value", recvData.value);
						long rowID=0;
						try{
							rowID = writeDB.insert(tableName, null, values);
						}
						catch(SQLException e)
						{
							e.printStackTrace();
						}
						Log.v("Inserted:","key:"+values.getAsString("key")+"value:"+values.getAsString("value"));
						if (rowID > 0)
						{
							Uri uri = ContentUris.withAppendedId(Uri.parse(contentUri), rowID);
							getContext().getContentResolver().notifyChange(uri,null);

						}
						
						myVersionNumber=valueVersionNo;	
						replicate(values);
					}else
					{Log.v("Discarding:","key:"+recvData.key+"value:"+ recvData.value);}
						//send ack to the coordinator

						MessageFormat putAckMsg = new MessageFormat();
						putAckMsg.devID=recvData.cordinatorID;
						putAckMsg.messageType="PUT_ACK";
						out = new ObjectOutputStream(incomingSocket.getOutputStream());
						out.writeObject(putAckMsg);

						//Send Data To All peers
						



					}
					else if(recvData.messageType.equals("PUT_ACK"))
					{
						Log.v("Node present","not failed");

					}
					else if(recvData.messageType.equals("REPLICATE_REQ"))
					{
						String tempStr[] =recvData.value.split("ver");
						int valueVersionNo= Integer.parseInt(tempStr[1]);
						SQLiteDatabase writeDB= createDatabase.getWritableDatabase();
						//	
						if(valueVersionNo>=myVersionNumber)
						{
						writeDB.delete(tableName, "key="+"'"+recvData.key+"'", null);

						ContentValues values = new ContentValues();

						values.put("key", recvData.key);
						values.put("value", recvData.value);
						long rowID=0;
						try{
							rowID = writeDB.insert(tableName, null, values);
						}
						catch(SQLException e)
						{
							e.printStackTrace();
						}
						Log.v("Inserted the replica:","key:"+values.getAsString("key")+"value:"+values.getAsString("value"));
						if (rowID > 0)
						{
							Uri uri = ContentUris.withAppendedId(Uri.parse(contentUri), rowID);
							getContext().getContentResolver().notifyChange(uri,null);

						}
						myVersionNumber=valueVersionNo;
						}else
						{Log.v("Discarding:","key:"+recvData.key+"value:"+ recvData.value);}
						//send ack to the coordinator
						MessageFormat putAckMsg = new MessageFormat();
						putAckMsg.devID=recvData.cordinatorID;
						putAckMsg.messageType="REPLICATE_ACK";
						out = new ObjectOutputStream(incomingSocket.getOutputStream());
						out.writeObject(putAckMsg);




					}



					else if(recvData.messageType.equals("RECOVERY_REQ"))
					{
						String kval;
						String vval;
						Uri uri = Uri.parse(contentUri);
						Cursor resultCursor = query(uri, null,null, null,"LDUMP");
						
						
						if(resultCursor.getCount()>0)
						{
						resultCursor.moveToFirst();
						ArrayList<ArrayList<String>> keyvaluePair = new ArrayList<ArrayList<String>>();
						for(int i=0;i<resultCursor.getCount();i++)
						{

							kval=resultCursor.getString(resultCursor.getColumnIndex("key"));
							vval=resultCursor.getString(resultCursor.getColumnIndex("value"));
							ArrayList<String> kvalentry = new ArrayList<String>();
							kvalentry.add(kval);
							kvalentry.add(vval);
							keyvaluePair.add(kvalentry);
							resultCursor.moveToNext();
						}

						MessageFormat recoveryMsg = new MessageFormat();
						recoveryMsg.devID=recvData.cordinatorID;
						recoveryMsg.messageType="RECOVERY_ACK";
						recoveryMsg.keyvaluePair=keyvaluePair;
						sendMessage(recoveryMsg,recoveryMsg.devID);
						Log.v("given data","to recovered node");
						}
						else
						{
							Log.v("No data ","at me yet");
						}

					}
					else if(recvData.messageType.equals("RECOVERY_ACK"))
					{
						
						if(recvData.keyvaluePair!=null)
						{for(ArrayList<String> entry : recvData.keyvaluePair)
						{

							String[] temp= entry.get(1).split("ver");
							int valueVersionNo= Integer.parseInt(temp[1]);
							SQLiteDatabase writeDB= createDatabase.getWritableDatabase();
							//						
							writeDB.delete(tableName, "key="+"'"+entry.get(0)+"'", null);

							ContentValues values = new ContentValues();

							values.put("key", entry.get(0));
							values.put("value", entry.get(1));
							
							long rowID=0;
							try{
								rowID = writeDB.insert(tableName, null, values);
							}
							catch(SQLException e)
							{
								e.printStackTrace();
							}
							Log.v("Inserted:","key:"+values.getAsString("key")+"value:"+values.getAsString("value"));
							if (rowID > 0)
							{
								Uri uri = ContentUris.withAppendedId(Uri.parse(contentUri), rowID);
								getContext().getContentResolver().notifyChange(uri,null);

							}
							if(myVersionNumber<valueVersionNo)
							{
								myVersionNumber=valueVersionNo;
								
							}



						}

						}

					}





					incomingSocket.close();	

					Log.v("Simple DHT", "My ID"+myDeviceId+"mySuccessor"+mySuccessorDevID+"myPredecessor"+myPredecessorDevID);



				}
				catch(Exception e)
				{
					e.printStackTrace();
				}



			}

		}


	}






	static int sendMessage(MessageFormat recvData, String devID) {
		// TODO Auto-generated method stub
		MessageFormat retMsg = new MessageFormat();
		Socket sendSocket=null;
		ObjectOutputStream out;
		ObjectInputStream in;
		try {

			//Log.v("Simple DHT","Sending msg of type" +recvData.messageType+" to Emulator: "+recvData.devID);
			if(recvData.devID.equals("5556"))
			{sendSocket = new Socket("10.0.2.2",11112);}
			else if(recvData.devID.equals("5558"))
			{sendSocket = new Socket("10.0.2.2",11116);}
			else if(recvData.devID.equals("5554"))
			{
				sendSocket = new Socket("10.0.2.2",11108);
				Log.v("Connection to ",": 5554");
			}

			out = new ObjectOutputStream(sendSocket.getOutputStream());
			//Send Data To All peers
			out.writeObject(recvData);
			Log.v("Simpledynamo", " Sent Data"+recvData.messageType+" to Emulator: "+recvData.devID);
			sendSocket.setSoTimeout(5000);
			Log.d("in send msg", "Socket Timeout Set to: 5000" );

			in = new ObjectInputStream(sendSocket.getInputStream());
			retMsg = (MessageFormat) in.readObject();
			Log.d("in send msg", "Received reply");

			sendSocket.close();




		}  catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.v("NOde down","didnt send msg");
			try {
				if(sendSocket!=null)
					sendSocket.close();

			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			return 0;

		}

		if(retMsg.messageType.contentEquals("PUT_ACK"))
		{
			Log.v("NOde present","Received put_ack");
			return 1;
		}
		else if (retMsg.messageType.contentEquals("REPLICATE_ACK"))
		{
			Log.v("NOde present","Received replicate_ack");
			return 1;
		}
		else
		{
			return 0;
		}	
	}






}








