package edu.buffalo.cse.cse486586.simpledynamo;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;


public class RecoveryThread extends Thread 
{String mySuccessorDevID;
String myDeviceId;

public RecoveryThread(String mySuccessorDevID,String myDeviceId )
{
	this.myDeviceId=myDeviceId;
	this.mySuccessorDevID=mySuccessorDevID;
}
public void run()

{
	MessageFormat recoveryMsg= new MessageFormat();
	recoveryMsg.devID=mySuccessorDevID;
	recoveryMsg.cordinatorID=myDeviceId;
	recoveryMsg.messageType="RECOVERY_REQ";
	SimpleDynamoProvider.sendMessage(recoveryMsg,recoveryMsg.devID);
	Log.v("Asked for recovery data ","from:"+recoveryMsg.devID);
}
}
