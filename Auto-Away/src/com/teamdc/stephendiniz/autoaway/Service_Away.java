package com.teamdc.stephendiniz.autoaway;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Calendar;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.ContactsContract.PhoneLookup;
import android.telephony.PhoneStateListener;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.teamdc.stephendiniz.autoaway.classes.LogEntry;

public class Service_Away extends Service
{
	private static final String	TAG = "AwayService";
	private static final String logFile	= "auto_away.log";

	private static final int NOTIFICATION_ID	 = 1;

	private static final int FILTER_JUSTCONTACTS = 1;
	private static final int FILTER_BLACKLIST    = 2;
	private static final int FILTER_WHITELIST    = 3;
	
	private static final int CALL_AND_TEXT		 = 0;
	private static final int CALL_ONLY			 = 1;
	private static final int TEXT_ONLY			 = 2;

	private boolean	silentStatus;
	private int		callText;

	private boolean logStatus;

	private String 	messageContent;
	private boolean informStatus;
	private int 	delayDuration;
	private boolean repeatStatus;

	private boolean	phoneCall;

	private int 	notifyCount;

	private String 	returnAddress;

	private String 	filterFile = "";
	private int 	filterStatus;

	private String 	name;
	
	private int noServiceResendAttempts = 0;
	
	// Global Objects
	private PowerManager pm;
	private PowerManager.WakeLock wakeLock;
	
	private PendingIntent sentPI;
	
	private AudioManager aManager;
	
	private BroadcastReceiver smsReceiver;
	private BroadcastReceiver resend;
	
	// Global Data Objects
	private ArrayList<LogEntry> entries = new ArrayList<LogEntry>();
	
	private ArrayList<String> pNumbers  = new ArrayList<String>();
	private ArrayList<String> addresses = new ArrayList<String>();

	private int startId;
	
	public void onStart(Intent intent, int startId)
	{
		super.onStart(intent, startId);

		this.startId = startId;
		
		// Only load extras if intent exists (wasn't recycled)
		if (intent != null)
		{
			Bundle infoBundle = intent.getExtras();
	
			setSilentStatus(infoBundle.getBoolean("extraSilentStatus"));
			setCallText(infoBundle.getInt("extraCallText"));
			setLogStatus(infoBundle.getBoolean("extraLogStatus"));
			setMessageContent(infoBundle.getString("extraMessage"));
			setInformStatus(infoBundle.getBoolean("extraInformStatus"));
			setDelayDuration(infoBundle.getInt("extraDelay"));
			setRepeatStatus(infoBundle.getBoolean("extraRepeatStatus"));
			setFilterStatus(infoBundle.getInt("extraFilterStatus"));
		}

		// Initializations
		pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Auto-Away");

        sentPI = PendingIntent.getBroadcast(this, 0, new Intent("android.provider.Telephony.SMS_SENT"), 0);
		aManager = (AudioManager)getBaseContext().getSystemService(Context.AUDIO_SERVICE);
		
		if(getSilentStatus())
			aManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);

		setPhoneCall(false);
		setFile(getFilterStatus());
		setNotifyCount(0);
		createNotification();
		
		if (getFilterStatus() != 0)
			grabNumbers(getFile());

		if(getLogStatus())
			loadLog(logFile);
		
		// Create SMS Broadcast Receiver if Texting is Enabled
		if(getCallText() != CALL_ONLY)
		{
			smsReceiver = new BroadcastReceiver()
			{
				@SuppressLint({ "NewApi", "NewApi" })
				@Override
				public void onReceive(Context context, Intent intent)
				{
					Bundle bundle = intent.getExtras();
					SmsMessage[] msgs = null;
	
					if(null != bundle)
					{
						setReturnAddress(null);
						Object[] pdus = (Object[]) bundle.get("pdus");
						msgs = new SmsMessage[pdus.length];
	
						for (int i = 0; i < msgs.length; i++)
						{
							msgs[i] = SmsMessage.createFromPdu((byte[])pdus[i]);
							setReturnAddress(msgs[i].getOriginatingAddress());
						}
						
						Log.i(TAG, "Text Message has been received from: " + hyphenate(getReturnAddress()));
						initTextSend();
					}
				}
			};
			registerReceiver(smsReceiver, new IntentFilter("android.provider.Telephony.SMS_RECEIVED"));
		}
		
        resend = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent) 
            {
            	// Resending here WILL DRAIN battery if error is persistent (NO DATA in a Gym Locker etc)
                switch (getResultCode())
                {
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                    	Log.e(TAG, "Text did NOT send (GENERIC_FAILURE)");
                    	Log.i(TAG, "Attempting to resend");
                    	sendSms();
                    break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                    	Log.e(TAG, "Text did NOT send (NO_SERVICE)");
                    	if(noServiceResendAttempts > 5)
                    	{
                    		Log.i(TAG, "Attempting resend: " + (noServiceResendAttempts++));
                    		sendSms();
                    	}
                    	else
                    	{
                    		createNotificationFromString(getResources().getString(R.string.error_no_service));
                    		noServiceResendAttempts = 0;
                    	}
                    break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                    	Log.e(TAG, "Text did NOT send (RADIO_OFF)");
                    	createNotificationFromString(getResources().getString(R.string.error_radio_off));
                    break;
                }
            }
        };
        registerReceiver(resend, new IntentFilter("android.provider.Telephony.SMS_SENT"));
        
        // Create PhoneStateListener (If Calls are Selected)
        if(getCallText() != TEXT_ONLY)
		{
	        TelephonyManager tManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
	        PhoneStateListener listener = new PhoneStateListener()
	        {
	        	@Override
	        	public void onCallStateChanged(int state, String incomingNumber)
	        	{
	        		switch(state)
	        		{
		        		case TelephonyManager.CALL_STATE_IDLE:
		        			Log.d(TAG, "Phone: Idle");
		        		break;
		        		case TelephonyManager.CALL_STATE_RINGING:
		        			Log.d(TAG, "Phone: Ringing.");
		        			
		        			setReturnAddress(incomingNumber);
		        			setPhoneCall(true);

		        			Handler handler = new Handler();
		        			Runnable runnable = new Runnable()
		        			{
		        				public void run()
		        				{
			        				initTextSend();
		        				}
		        			};
		        			
		        			handler.removeCallbacks(runnable);
		        			Log.i(TAG, "Incoming call from: " + getReturnAddress());
		        			handler.postDelayed(runnable, (long)1000);
		        		break;
		        		case TelephonyManager.CALL_STATE_OFFHOOK:
		        			Log.d(TAG, "Phone: Off Hook");
		        		break;
	        		}
	        	}
	        };
	        tManager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE);
		}
	}

	public void onDestroy()
	{
		super.onDestroy();
		
		// Return back to Normal Ringer state (if it was changed)
		if(getSilentStatus())
			aManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
		
		// Destroy Notification
		NotificationManager nManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		nManager.cancel(NOTIFICATION_ID);
		
		// Unregister Broadcast Receivers
		unregisterReceiver(smsReceiver);
		unregisterReceiver(resend);
		
		stopSelf(startId);
	}
	
	@SuppressLint({ "NewApi", "NewApi" })
	private void initTextSend()
	{
		if (noRepetition() && notListed())
		{
			// Add address to list of "Already Notified"
			addresses.add(getReturnAddress());
			
			int phoneOffset = 0;
			
			// Add 30 Second Call Padding
			if (getPhoneCall() && (getDelayDuration() < 30))
				phoneOffset = 30;

			Handler tHandler = new Handler();
			Runnable tGo = new Runnable()
			{
				public void run()
				{
					// If logging is enabled, log address
					if(getLogStatus())
						logAddress();
					
					// Create Notification
					createNotification();
					
					// No longer important
					setPhoneCall(false);
					
					// Send Text Message
					sendSms();
				}
			};
			tHandler.removeCallbacks(tGo);
			Log.i(TAG, "Timer set, waiting " + (getDelayDuration() + phoneOffset) + "s");
			tHandler.postDelayed(tGo, (long)(1000 * (getDelayDuration() + phoneOffset)));
		}
	}
	
	@SuppressLint({ "NewApi", "NewApi", "NewApi" })
	private void sendSms()
	{
		SmsManager manager = SmsManager.getDefault();

		wakeLock.acquire();
		Log.d(TAG, "Wake Lock Acquired!");

		if (getMessageContent(getInformStatus()).length() > 160)
		{
			ArrayList<String> messagelist = manager.divideMessage(getMessageContent(getInformStatus()));

			manager.sendMultipartTextMessage(getReturnAddress(), null, messagelist, null, null);
			Log.i(TAG, "Multipart Text Message Sent!");
		}
		else
		{
			manager.sendTextMessage(getReturnAddress(), null, getMessageContent(getInformStatus()), sentPI, null);
			Log.i(TAG, "Text Message Sent!");
		}
		
		wakeLock.release();
		Log.d(TAG, "Wake Lock Released!");
	}
	
	// Supplement Methods
	
	private boolean noRepetition()
	{
		if (addresses.contains(getReturnAddress()))
		{
			if (getRepeatStatus())
				return true;
			else
			{
				Log.w(TAG, hyphenate(getReturnAddress()) + " was already notified!");
				return false;
			}
		}
		else
			return true;
	}
	
	private boolean notListed()
	{
		if (getFilterStatus() == 0)
			return true;
		
		switch(getFilterStatus())
		{
			case FILTER_WHITELIST:
				if (getList())
					return true;
				else
					Log.w(TAG, hyphenate(getReturnAddress()) + " is NOT on the Whitelist!");
			break;
			case FILTER_BLACKLIST:
				if (!getList())
					return true;
				else
					Log.w(TAG, hyphenate(getReturnAddress()) + " is ON the Blacklist!");
			break;
			case FILTER_JUSTCONTACTS:
				if (numberInContacts())
					return true;
				else
					Log.w(TAG, hyphenate(getReturnAddress()) + " is NOT in Contacts!");
			break;	
		}
		
		return false;
	}
	
	private void loadLog(String file)
	{
		File inFile = getBaseContext().getFileStreamPath(file);
		int numOfLogEntries = 0;

		if (inFile.exists())
		{
			try
			{
				InputStream iStream = openFileInput(file);
				InputStreamReader iReader = new InputStreamReader(iStream);
				BufferedReader buffreader = new BufferedReader(iReader);
				
				String line;
				
				while(((line = buffreader.readLine()) != null))
				{
					if (line.equals("\n"))
						break;
					
					LogEntry newLogEntry = new LogEntry(Integer.parseInt(line), buffreader.readLine(), buffreader.readLine(), buffreader.readLine(), buffreader.readLine());
					entries.add(newLogEntry);

					numOfLogEntries++;
				}
				
				iStream.close();
			}
			catch (java.io.FileNotFoundException exception)	{ Log.e(TAG, "\"" + file + "\" not found!", exception); 	}
			catch (java.io.IOException exception)			{ Log.e(TAG, "IOException on \"" + file + "\"", exception); };
		}
		
		Log.i(TAG, "Logs loaded: " + numOfLogEntries);
	}
	
	private void grabNumbers(String file)
	{
		// Clear list, just in case
		pNumbers.removeAll(pNumbers);

		File inFile = getBaseContext().getFileStreamPath(file);
		int numOfFilters = 0;
		
		if (inFile.exists())
		{
			try
			{
				InputStream iStream = openFileInput(file);
				InputStreamReader iReader = new InputStreamReader(iStream);
				BufferedReader bReader = new BufferedReader(iReader);
				
				String line;
				
				while((line = bReader.readLine()) != null)
				{
					line = bReader.readLine();
					pNumbers.add(dehyphenate(line));
					
					numOfFilters++;
				}
					
				iStream.close();
			}
			catch (java.io.FileNotFoundException exception) { Log.e(TAG, "FileNotFoundException caused by openFileInput(fileName)", exception); }
			catch (IOException exception) 					{ Log.e(TAG, "IOException caused by buffreader.readLine()", exception); 			}

			if (numOfFilters == 0)
			{
				Toast eToast = Toast.makeText(this, getResources().getString(R.string.prompt_filtering_empty), Toast.LENGTH_LONG);
				eToast.show();
			}
			else
				Log.i(TAG, "Filters Loaded: " + numOfFilters);
		}
		else
			Log.w(TAG, getFile() + " doesn't exist, no filters loaded!");
	}
	
	private void logAddress()
	{
		LogEntry logEntry = new LogEntry();

		// Find Log "Type"
		if (getPhoneCall())
			logEntry.setType(LogEntry.PHONE_CALL);
		else
			logEntry.setType(LogEntry.TEXT_MESSAGE);

		// Find Log "Time and Date"
		Calendar c = Calendar.getInstance();

		String timeOfDay = "AM";
		if(c.get(Calendar.AM_PM) == Calendar.PM)
			timeOfDay = "PM";
		
		int hour = c.get(Calendar.HOUR);
		if (hour == 0)
			hour = 12;

		int minute = c.get(Calendar.MINUTE);
		int second = c.get(Calendar.SECOND);
		
		int year = Integer.parseInt(Integer.toString(c.get(Calendar.YEAR)).substring(2,4));
		int month = c.get(Calendar.MONTH) + 1;
		int day = c.get(Calendar.DAY_OF_MONTH);
		
		logEntry.setTime(pad(hour) + ":" + pad(minute) + ":" + pad(second) + " " + timeOfDay);
		logEntry.setDate(pad(month) + "/" + pad(day) + "/" + year);
		
		// Find Log "Name" in contacts (If it exists)
		getContactInfo();
		logEntry.setName(getName());
		logEntry.setNumber(hyphenate(getReturnAddress()));
		
		entries.add(logEntry);
		saveLog(logFile);
	}

	private String pad(int number)
	{
		String time = Integer.toString(number);

		if(number < 10)
			time = "0" + Integer.toString(number);
		
		return time;
	}
	
	private void saveLog(String file)
	{
		try
		{
			OutputStreamWriter oWriter = new OutputStreamWriter(openFileOutput(file, 0));
			
			oWriter.write("");
			for (int i = 0; i < entries.size(); i++)
			{
				oWriter.append(Integer.toString(entries.get(entries.size() - 1 - i).getType()) + "\n");
				oWriter.append(entries.get(entries.size() - 1 - i).getTime() + "\n");
				oWriter.append(entries.get(entries.size() - 1 - i).getDate() + "\n");
				oWriter.append(entries.get(entries.size() - 1 - i).getName() + "\n");
				oWriter.append(entries.get(entries.size() - 1 - i).getNumber() + "\n");
			}
			
			oWriter.flush();
			oWriter.close();
			
			Log.i(TAG, "Saved " + hyphenate(getReturnAddress()) + " to log");
		}
		catch (java.io.IOException exception) { Log.e(TAG, "IOException on \"" + file + "\"", exception); };
	}	

	@SuppressLint("NewApi")
	private void createNotification()
	{
		NotificationManager nManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		Notification notification;

		if(getNotifyCount() > 0)
		{
			nManager.cancel(NOTIFICATION_ID);

			notification = new Notification(R.drawable.notification_icon, getResources().getString(R.string.notification_ticker_start), System.currentTimeMillis());
			notification.setLatestEventInfo(this, getResources().getString(R.string.app_name), getMessageContent(false), PendingIntent.getActivity(this, 0, new Intent(this, Activity_Main.class), 0));
			
			if (!getLogStatus())
				getContactInfo();

			if (getName().equals(""))
				notification = new Notification(R.drawable.notification_icon, getResources().getString(R.string.notification_ticker_replied) + " " + hyphenate(getReturnAddress()), System.currentTimeMillis());
			else
				notification = new Notification(R.drawable.notification_icon, getResources().getString(R.string.notification_ticker_replied) + " " + getName() + " (" + hyphenate(getReturnAddress()) + ")", System.currentTimeMillis());
			
			notification.setLatestEventInfo(this, getResources().getString(R.string.app_name) + " (" + getNotifyCount() + ")", getMessageContent(false), PendingIntent.getActivity(this, 0, new Intent(this, Activity_Main.class), 0));
			
			Log.i(TAG, "Notification Updated: " + getNotifyCount() + " responses this lifecycle");
		}
		else
		{
			// Service started, first notification
			notification = new Notification(R.drawable.notification_icon, getResources().getString(R.string.notification_ticker_start), System.currentTimeMillis());
			notification.setLatestEventInfo(this, getResources().getString(R.string.app_name), getMessageContent(false), PendingIntent.getActivity(this, 0, new Intent(this, Activity_Main.class), 0));

			Log.i(TAG, "Notification Created");
		}

		notification.flags |= Notification.FLAG_ONGOING_EVENT;
		notification.flags |= Notification.FLAG_NO_CLEAR;
		startForeground(NOTIFICATION_ID, notification);
		nManager.notify(NOTIFICATION_ID, notification);

		setNotifyCount(getNotifyCount() + 1);
	}
	
	@SuppressLint("NewApi")
	private void createNotificationFromString(String message)
	{
		NotificationManager nManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		Notification notification;

		// Service started, first notification
		notification = new Notification(R.drawable.notification_icon, message, System.currentTimeMillis());
		notification.setLatestEventInfo(this, getResources().getString(R.string.app_name), getMessageContent(false), PendingIntent.getActivity(this, 0, new Intent(this, Activity_Main.class), 0));

		Log.i(TAG, "Notification Created");

		notification.flags |= Notification.FLAG_ONGOING_EVENT;
		notification.flags |= Notification.FLAG_NO_CLEAR;
		startForeground(NOTIFICATION_ID + 1, notification);
		nManager.notify(NOTIFICATION_ID + 1, notification);
	}
	
	private String hyphenate(String number)
	{
		if (number.length() == 10)
			return number.substring(0,3) + "-" + number.substring(3,6) + "-" + number.substring(6,10);
		
		if (number.length() == 11)
			return number.substring(0,1) + "-" + number.substring(1,4) + "-" + number.substring(4,7) + "-" + number.substring(7,11);

		return number;
	}
	
	@SuppressLint("NewApi")
	private String dehyphenate(String number)
	{
		if (number.length() == 12)
			return number.substring(0,3) + number.substring(4,7) +  number.substring(8,12);
		else if (number.length() == 14)
			return number.substring(0,1) + number.substring(2,5) +  number.substring(6,9) + number.substring(10,14);

		return number;
	}
	
	@SuppressLint("NewApi")
	private boolean numberInContacts()
	{
		Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(getReturnAddress()));
		Cursor cursor = getContentResolver().query(uri, null, null, null, null);
		
		if(cursor.moveToNext())
		{
			cursor.close();

			return true;
		}
		
		cursor.close();

		return false;
	}
	
	@SuppressLint("NewApi")
	private void getContactInfo()				
	{
		String number = hyphenate(getReturnAddress());
		Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
		Cursor cursor = getContentResolver().query(uri, new String[]{PhoneLookup.DISPLAY_NAME}, null, null, null);

		if(cursor.moveToNext())
		{
			String name = cursor.getString(cursor.getColumnIndexOrThrow(PhoneLookup.DISPLAY_NAME));
			
			setName(name);
		}
		else
			setName("");

		cursor.close();
	}

	// Get and Set Methods
	
	public boolean getSilentStatus()						{ return silentStatus;												}
	public void setSilentStatus(boolean silentStatus)		{ this.silentStatus = silentStatus;									}
	
	public int getCallText()								{ return callText;													}
	public void setCallText(int callText)					{ this.callText = callText;											}
	
	public String getMessageContent(boolean status) 		{ if(status)
																return "[Auto-Away]: " + messageContent;

															  return messageContent;											}
	public void setMessageContent(String messageContent)	{ this.messageContent = messageContent;								}

	public boolean getInformStatus()						{ return informStatus;												}
	public void setInformStatus(boolean informStatus)		{ this.informStatus = informStatus;									}
	
	public int getDelayDuration()							{ return delayDuration;												}
	public void setDelayDuration(int delayDuration)			{ this.delayDuration = delayDuration;								}
	
	public String getReturnAddress()						{ return returnAddress;												}
	public void setReturnAddress(String returnAddress)		{ this.returnAddress = returnAddress;								}
	
	public boolean getLogStatus()							{ return logStatus;													}
	public void setLogStatus(boolean logStatus)				{ this.logStatus = logStatus;										}
	
	public boolean getRepeatStatus()						{ return repeatStatus;												}
	public void setRepeatStatus(boolean repeatStatus)		{ this.repeatStatus = repeatStatus;									}
	
	public int getNotifyCount()								{ return notifyCount;												}
	public void setNotifyCount(int notifyCount)				{ this.notifyCount = notifyCount;									}
	
	public String getFile()									{ return filterFile;												}
	public void setFile(int id)								{ if(id == FILTER_WHITELIST) filterFile = "filtering_whitelist.txt";
															  if(id == FILTER_BLACKLIST) filterFile = "filtering_blacklist.txt";}
	
	public int getFilterStatus()							{ return filterStatus;												}
	public void setFilterStatus(int filterStatus)			{ this.filterStatus = filterStatus;									}
	public boolean getList()								{ if (pNumbers.contains(getReturnAddress())) 
																 return true;													   
															  else	
																 return false;													}

	public boolean getPhoneCall()							{ return phoneCall;													}
	public void setPhoneCall(boolean phoneCall)				{ this.phoneCall = phoneCall;										}

	public String getName()									{ return name;														}
	public void setName(String name)						{ this.name = name;													}
	
	public IBinder onBind(Intent i) 						{ return null; 														}
}