package com.teamdc.stephendiniz.autoaway;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.teamdc.stephendiniz.autoaway.classes.Message;

public class Activity_Main extends PreferenceActivity implements OnPreferenceChangeListener, OnPreferenceClickListener
{

	private static final String	TAG = "Main";
	private static final String messagesFile = "awayMessages.txt";
	
	final String SERVICE_PREF	= "serviceCheckBox";
	final String SILENT_PREF	= "silentCheckBox";
	final String CALLTEXT_PREF	= "callTextListPref";

	final String LOG_ACT_PREF	= "logPreference";
	final String LOG_PREF		= "logCheckBox";

	final String MESSAGE_PREF	= "messageListPref";
	final String FILTER_PREF	= "filteringListPref";
	final String INFORM_PREF	= "informCheckBox";
	final String DELAY_PREF		= "delayListPref";
	final String REPEAT_PREF	= "repeatCheckBox";

	final String BACKUP_PREF	= "backupPreference";
	final String RESTORE_PREF	= "restorePreference";

	final String ABOUT_PREF		= "aboutPreference";
	
	final String BUCHECK_PREF	= "bUCheckPreference";
	
	final String THEME_PREF		= "themePreference";

	CheckBoxPreference 	serviceCheckBox;
	CheckBoxPreference 	silentCheckBox;
	ListPreference		callTextListPref;

	Preference 			logPreference;
	CheckBoxPreference 	logCheckBox;

	ListPreference 		messageListPref;
	ListPreference		filteringListPref;
	CheckBoxPreference 	informCheckBox;
	ListPreference 		delayListPref;
	CheckBoxPreference 	repeatCheckBox;

	Preference	backupPreference;
	Preference	restorePreference;
	Preference	aboutPreference;
	
	Preference	bUCheckPreference;
	
	Dialog dialog;
	
	Resources r;

	SharedPreferences prefs;
	SharedPreferences.Editor editor;

	static final int LOG_DIALOG_ID = 0;
	static final int FILTERING_DIALOG_ID = 1;
	
	static final int FILTER_MENU_OFF		= 0;
	static final int FILTER_MENU_CONTACTS   = 1;
	static final int FILTER_MENU_BLACKLIST	= 2;
	static final int FILTER_MENU_WHITELIST	= 3;

	static final int MENU_ITEM_MESSAGES	= 0;
	static final int MENU_ITEM_FILTER	= 1;
	static final int MENU_ITEM_SCHEDULE	= 2;
	static final int MENU_ITEM_THEME	= 3;
	
	static final int CALLTEXT_BOTH	= 0;
	static final int CALLTEXT_CALL	= 1;
	static final int CALLTEXT_TEXT	= 2;

	private ArrayList<Message> messages = new ArrayList<Message>();

	@SuppressLint("NewApi")
	public void onCreate (Bundle savedInstanceState)
	{
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		editor = prefs.edit();

		if(android.os.Build.VERSION.SDK_INT >= 14)
		{
			if(prefs.getString(THEME_PREF, "LIGHT").equals("LIGHT"))
				setTheme(R.style.HoloLight);
			else
				setTheme(R.style.HoloDark);
		}

		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.preferences);

		// Create XML to Android Object Binds and register Change/Click Listeners
		serviceCheckBox 	= (CheckBoxPreference)findPreference(SERVICE_PREF);
			serviceCheckBox.setOnPreferenceChangeListener(this);
		silentCheckBox 		= (CheckBoxPreference)findPreference(SILENT_PREF);
		callTextListPref	= (ListPreference)findPreference(CALLTEXT_PREF);
		
		logPreference 		= (Preference)findPreference(LOG_ACT_PREF);
			logPreference.setOnPreferenceClickListener(this);
		logCheckBox 		= (CheckBoxPreference)findPreference(LOG_PREF);
			logCheckBox.setOnPreferenceClickListener(this);
		
		messageListPref 	= (ListPreference)findPreference(MESSAGE_PREF);
		filteringListPref	= (ListPreference)findPreference(FILTER_PREF);
		informCheckBox 		= (CheckBoxPreference)findPreference(INFORM_PREF);
		delayListPref 		= (ListPreference)findPreference(DELAY_PREF);
		repeatCheckBox 		= (CheckBoxPreference)findPreference(REPEAT_PREF);
		
		backupPreference 	= (Preference)findPreference(BACKUP_PREF);
			backupPreference.setOnPreferenceClickListener(this);
			
		restorePreference 	= (Preference)findPreference(RESTORE_PREF);
			restorePreference.setOnPreferenceClickListener(this);
		
		aboutPreference		= (Preference)findPreference(ABOUT_PREF);
		
		bUCheckPreference	= new Preference(this);

		// Load Custom Messages and Generate Drop Down Lists
		messagesExist(messagesFile);
		createListPreferences(false);
	}
	
	@SuppressLint("NewApi")
	public void onResume()
	{
		super.onResume();
		
		editor.putBoolean(BUCHECK_PREF, checkForBackups());
		editor.commit();

		if(getServiceStatus())
			setPreferenceStatus(false);	// Disable Preference Changing if Service is Running

		logPreference.setEnabled(getLogStatus());
		aboutPreference.setSummary(" v" + r.getString(R.string.app_version));
		
		if(!prefs.getBoolean(BUCHECK_PREF, false))
			restorePreference.setEnabled(false);
		
		messagesExist(messagesFile);
		createListPreferences(true);
	}

	public boolean onPreferenceClick(Preference p)
	{
		if(p.getKey().equals(LOG_ACT_PREF))
		{
			Intent log = new Intent(this, Activity_Logger.class);
			startActivity(log);

			return true;
		}
		else if(p.getKey().equals(BACKUP_PREF))
		{
			Toast eToast = Toast.makeText(this, backupInformation(), Toast.LENGTH_SHORT);
			eToast.show();
			
			return true;
		}
		else if(p.getKey().equals(RESTORE_PREF))
		{
			Toast eToast = Toast.makeText(this, restoreInformation(), Toast.LENGTH_SHORT);
			eToast.show();
			finish();startActivity(getIntent());
			
			return true;
		}
		else if(p.getKey().equals(LOG_PREF))
		{
			if(getLogStatus())
			{
				logPreference.setEnabled(true);
				setLogStatus(true);
			}
			else
			{
				logPreference.setEnabled(false);
				setLogStatus(false);
			}
			
			return true;
		}
		
		return false;
	}
	public boolean onPreferenceChange(Preference p, Object o)
	{
		if(p.getKey().equals(SERVICE_PREF))
		{
			final Intent awayService = new Intent(this, Service_Away.class);

			if(prefs.getBoolean(SERVICE_PREF, false))
			{
				setServiceStatus(false);
				setPreferenceStatus(true);
				stopService(awayService);
				
				return true;
			}
			
			else
			{
				setServiceStatus(true);
				setPreferenceStatus(false);

				//Set Intent Extras
				awayService.putExtra("extraSilentStatus", getSilentStatus());
				awayService.putExtra("extraLogStatus", getLogStatus());
				awayService.putExtra("extraCallText", getCallText());
				awayService.putExtra("extraMessage", getMessage());
				awayService.putExtra("extraInformStatus", getInformStatus());
				awayService.putExtra("extraDelay", getDelay());
				awayService.putExtra("extraRepeatStatus", getRepeatStatus());
				awayService.putExtra("extraFilterStatus", getFilterStatus());

				//Start service and terminate activity
				startService(awayService);
				finish();
				
				return true;
			}
		}
			
		return false;
	}
	
	@SuppressLint("NewApi")
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_main, menu);

		if(android.os.Build.VERSION.SDK_INT >= 14)
		{
			if(prefs.getString(THEME_PREF, "LIGHT").equals("LIGHT"))
				menu.findItem(R.id.menu_theme).setTitle(r.getString(R.string.menu_main_theme_dark));
			else
				menu.findItem(R.id.menu_theme).setTitle(r.getString(R.string.menu_main_theme_light));
		}
		else
			menu.removeItem(R.id.menu_theme);
		
		return true;
	}
	
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		if(getServiceStatus())
		{
			menu.getItem(MENU_ITEM_MESSAGES).setEnabled(false);
			menu.getItem(MENU_ITEM_FILTER).setEnabled(false);
			//menu.getItem(MENU_ITEM_SCHEDULE).setEnabled(false);
		}
		else if(!getServiceStatus())
		{
			menu.getItem(MENU_ITEM_MESSAGES).setEnabled(true);
			
			if(getFilterStatus() == FILTER_MENU_CONTACTS || getFilterStatus() == FILTER_MENU_OFF)
				menu.getItem(MENU_ITEM_FILTER).setEnabled(false);
			else
				menu.getItem(MENU_ITEM_FILTER).setEnabled(true);
			
			//menu.getItem(MENU_ITEM_SCHEDULE).setEnabled(true);
		}
		
		return true;
	}
	
	public boolean onOptionsItemSelected(MenuItem item) 
	{
		switch(item.getItemId())
		{
			case R.id.menu_edit_messages:
				Intent message = new Intent(this, Activity_Messages.class);
				startActivity(message);
			break;
			
			case R.id.menu_filtering:
				switch(getFilterStatus())
				{
					case FILTER_MENU_OFF:
						 showDialog(FILTERING_DIALOG_ID);
					break;
					
					case FILTER_MENU_CONTACTS:
					break;
					
					default:
						Intent filtering = new Intent(this, Activity_Filtering.class);
						filtering.putExtra("extraFilterStatus", getFilterStatus());
						
						startActivity(filtering);
					break;
				}
			break;
			//case R.id.menu_scheduling:
			//	Intent schedule = new Intent(this, Activity_Schedule.class);
			//	startActivity(schedule);
			//break;
			case R.id.menu_theme:
				if(prefs.getString(THEME_PREF, "LIGHT").equals("LIGHT"))
					editor.putString(THEME_PREF, "DARK");
				else
					editor.putString(THEME_PREF, "LIGHT");

				editor.commit();
				finish();startActivity(getIntent());
			break;
		}
		
		return true;
	}

	public boolean messagesExist(String file)
	{
		r = getResources();
		messages.removeAll(messages);
		
		Message defaultMessage = new Message(r.getString(R.string.default_message_title), r.getString(R.string.default_message_content));
		messages.add(defaultMessage);
		
		try
		{
			File inFile = getBaseContext().getFileStreamPath(file);
			
			if (inFile.exists())
			{
				InputStream iStream = openFileInput(file);
				InputStreamReader iReader = new InputStreamReader(iStream);
				BufferedReader bReader = new BufferedReader(iReader);
				
				String line;
				
				//Should ALWAYS be in groups of two
				while((line = bReader.readLine()) != null)
				{
					Message messageFromFile = new Message(line, bReader.readLine());
					messages.add(messageFromFile);
				}
				
				iStream.close();
			}
		}
		catch (java.io.FileNotFoundException exception) { Log.e(TAG, "FileNotFoundException caused by openFileInput(fileName)", exception); }
		catch (IOException exception) 					{ Log.e(TAG, "IOException caused by buffreader.readLine()", exception); 			}
		
		if(messages.isEmpty())
			return false;
		
		return true;
	}
	public void createListPreferences(boolean justMessages)
	{
		if(!justMessages)
		{
			CharSequence[] callTextNames = { r.getString(R.string.pref_call_text_type_1), r.getString(R.string.pref_call_text_type_2),
											 r.getString(R.string.pref_call_text_type_3) };
			CharSequence[] callTextValues = { "0", "1", "2" };
			
			callTextListPref.setEntries(callTextNames);
			callTextListPref.setEntryValues(callTextValues);
			
			CharSequence[] delayNames = {	r.getString(R.string.pref_delay_type_1), r.getString(R.string.pref_delay_type_2),
											r.getString(R.string.pref_delay_type_3), r.getString(R.string.pref_delay_type_4),
											r.getString(R.string.pref_delay_type_5), r.getString(R.string.pref_delay_type_6) };
			
			CharSequence[] delayValues = {	"0", "15", "30", "60", "120", "300" };
			
			delayListPref.setEntries(delayNames);
			delayListPref.setEntryValues(delayValues);

			CharSequence[] filteringNames = {	r.getString(R.string.pref_filter_type_1), r.getString(R.string.pref_filter_type_2),
												r.getString(R.string.pref_filter_type_3), r.getString(R.string.pref_filter_type_4)};
			
			CharSequence[] filteringValues = { Integer.toString(FILTER_MENU_OFF), Integer.toString(FILTER_MENU_CONTACTS), Integer.toString(FILTER_MENU_BLACKLIST), Integer.toString(FILTER_MENU_WHITELIST) };			
			filteringListPref.setEntries(filteringNames);
			filteringListPref.setEntryValues(filteringValues);
		}

		CharSequence[] messageTitle = new CharSequence[messages.size()];
		CharSequence[] messageContent = new CharSequence[messages.size()];
		
		for (int i = 0; i < messages.size(); i++)
		{
			messageTitle[i]		= messages.get(i).getTitle();
			messageContent[i]	= messages.get(i).getContent();
		}
		
		messageListPref.setEntries(messageTitle);
		messageListPref.setEntryValues(messageContent);
	}

	private void setPreferenceStatus(boolean status)
	{
		silentCheckBox.setEnabled(status);
		callTextListPref.setEnabled(status);
		
		logPreference.setEnabled(status);
		logCheckBox.setEnabled(status);
		
		messageListPref.setEnabled(status);
		filteringListPref.setEnabled(status);
		informCheckBox.setEnabled(status);
		delayListPref.setEnabled(status);
		repeatCheckBox.setEnabled(status);
		
		backupPreference.setEnabled(status);
		
		if ((status == false) || (status == true && prefs.getBoolean(BUCHECK_PREF, false)))
			restorePreference.setEnabled(status);
		
		aboutPreference.setEnabled(status);
	}
    protected Dialog onCreateDialog(int id)
    {
    	AlertDialog dialog = null;
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);

    	switch(id)
    	{
    		case LOG_DIALOG_ID:
    			builder.setTitle(r.getString(R.string.prompt_log_title)).setMessage(r.getString(R.string.prompt_log_content))
    			.setPositiveButton(r.getString(R.string.menu_yes), new DialogInterface.OnClickListener()
    			{
    				public void onClick(DialogInterface dialog, int id)
    				{
    					setLogStatus(true);
    					logCheckBox.setChecked(true);
    				}
    			})
    			.setNegativeButton(r.getString(R.string.menu_no), new DialogInterface.OnClickListener()
    			{
    				public void onClick(DialogInterface dialog, int id)
    				{
    					dialog.cancel();
    				}
    			});

    			dialog = builder.create();
    			break;
    		case FILTERING_DIALOG_ID:
    			builder.setTitle(r.getString(R.string.prompt_filtering_title))
    			.setNegativeButton(r.getString(R.string.menu_close), new DialogInterface.OnClickListener()
    			{
    				public void onClick(DialogInterface dialog, int id)
    				{
    					dialog.cancel();
    				}
    			});
    			dialog = builder.create();
    			break;
    	}

    	return dialog;
    }

    public String backupInformation()
    {
    	long startTime = 0;
    	long stopTime = 0;
    	
    	startTime = System.currentTimeMillis();

    	//Is SD Card Accessible?
    	String sdState = Environment.getExternalStorageState();

    	if(!Environment.MEDIA_MOUNTED.equals(sdState))
    		return r.getString(R.string.prompt_error_sdcard_mounted);
    	else if(Environment.MEDIA_MOUNTED_READ_ONLY.equals(sdState))
    		return r.getString(R.string.prompt_error_sdcard_readOnly);
    	
    	//Create Preferences File
		try
		{
			File path = new File(Environment.getExternalStorageDirectory() + "/Auto-Away/");
			path.mkdir();
			File outputFile = new File(path, "Preferences.bak");
			FileOutputStream fos = new FileOutputStream(outputFile);
			OutputStreamWriter oWriter = new OutputStreamWriter(fos);

			Log.d(TAG, "Preference Integrity Check:");
			Log.d(TAG, "Silence Status: " + new Boolean(getSilentStatus()).toString());
			Log.d(TAG, "Call/Text Status: " + Integer.toString(getCallText()));
			Log.d(TAG, "Log Status: " + new Boolean(getLogStatus()).toString());
			Log.d(TAG, "Message: " + getMessage());
			Log.d(TAG, "Filter Status: " + new Boolean(getLogStatus()).toString());
			Log.d(TAG, "Inform Status: " + new Boolean(prefs.getBoolean(INFORM_PREF, true)).toString());
			Log.d(TAG, "Delay: " + Integer.toString(getDelay()) + "s");
			Log.d(TAG, "Repeat Status: " + new Boolean(prefs.getBoolean(REPEAT_PREF, false)).toString());

			oWriter.write(new Boolean(getSilentStatus()).toString() + "\n");
			oWriter.append(Integer.toString(getCallText()) + "\n");
			oWriter.append(new Boolean(getLogStatus()).toString() + "\n");
			oWriter.append(getMessage() + "\n");
			oWriter.append(Integer.toString(getFilterStatus()) + "\n");
			oWriter.append(new Boolean(prefs.getBoolean(INFORM_PREF, true)).toString() + "\n");
			oWriter.append(Integer.toString(getDelay()) + "\n");
			oWriter.append(new Boolean(prefs.getBoolean(REPEAT_PREF, false)).toString() + "\n");
			oWriter.append(prefs.getString(THEME_PREF, "LIGHT") + "\n");
			
			oWriter.flush();
			oWriter.close();
			
			Log.i(TAG, "Preferences Backup Successful");
		}
		catch (java.io.IOException exception) { Log.e(TAG, "IOException caused by trying to access \'Preferences.bak\'", exception); };
		
    	//Write Log to SD Card
		try
		{
			File path = new File(Environment.getExternalStorageDirectory() + "/Auto-Away/");
			OutputStreamWriter oWriter = new OutputStreamWriter(new FileOutputStream(new File(path, "Log.bak")));
			InputStream iStream = openFileInput("auto_away.log");
			InputStreamReader iReader = new InputStreamReader(iStream);
			BufferedReader bReader = new BufferedReader(iReader);

			String line = null;
			if((line = bReader.readLine()) != null)
				oWriter.write(line + "\n");
			
			while((line = bReader.readLine()) != null)
				oWriter.append(line + "\n");
			
			oWriter.flush();
			oWriter.close();
			iStream.close();
			Log.i(TAG, "Log Backup Successful");
		}
		catch (java.io.IOException exception) { Log.e(TAG, "IOException caused by trying to access \'auto_away.log\'", exception); };
		
		//Write Messages to SD Card
		try
		{
			File path = new File(Environment.getExternalStorageDirectory() + "/Auto-Away/");
			OutputStreamWriter oWriter = new OutputStreamWriter(new FileOutputStream(new File(path,"Messages.bak")));
			InputStream iStream = openFileInput("awayMessages.txt");
			InputStreamReader iReader = new InputStreamReader(iStream);
			BufferedReader bReader = new BufferedReader(iReader);

			String line = null;
			if((line = bReader.readLine()) != null)
				oWriter.write(line + "\n");
			
			while((line = bReader.readLine()) != null)
				oWriter.append(line + "\n");
			
			oWriter.flush();
			oWriter.close();
			iStream.close();
			Log.i(TAG, "Messages Backup Successful");
		}
		catch (java.io.IOException exception) { Log.e(TAG, "IOException caused by trying to access \'awayMessages.txt\'", exception); };
		
		//Write Filters to SD Card
		try
		{
			File path = new File(Environment.getExternalStorageDirectory() + "/Auto-Away/");
			OutputStreamWriter oWriter = new OutputStreamWriter(new FileOutputStream(new File(path,"Blacklist.bak")));
			InputStream iStream = openFileInput("filtering_blacklist.txt");
			InputStreamReader iReader = new InputStreamReader(iStream);
			BufferedReader bReader = new BufferedReader(iReader);

			String line = null;
			if((line = bReader.readLine()) != null)
				oWriter.write(line + "\n");
			
			while((line = bReader.readLine()) != null)
				oWriter.append(line + "\n");

			oWriter.flush();
			oWriter.close();
			iStream.close();
			Log.i(TAG, "Blacklist Filter Backup Successful");
		}
		catch (java.io.IOException exception) { Log.e(TAG, "IOException caused by trying to access \'filtering_blacklist.txt\'", exception); };
		
		try
		{
			File path = new File(Environment.getExternalStorageDirectory() + "/Auto-Away/");
			OutputStreamWriter oWriter = new OutputStreamWriter(new FileOutputStream(new File(path,"Whitelist.bak")));
			InputStream iStream = openFileInput("filtering_whitelist.txt");
			InputStreamReader iReader = new InputStreamReader(iStream);
			BufferedReader bReader = new BufferedReader(iReader);

			String line = null;
			if((line = bReader.readLine()) != null)
				oWriter.write(line + "\n");
			
			while((line = bReader.readLine()) != null)
				oWriter.append(line + "\n");
			
			oWriter.flush();
			oWriter.close();
			iStream.close();
			Log.i(TAG, "Whitelist Filter Backup Successful");
		}
		catch (java.io.IOException exception) { Log.e(TAG, "IOException caused by trying to access \'filtering_whitelist.txt\'", exception); };
		
		Log.i(TAG, "Backup Completed Successfully");
		
		editor.putBoolean(BUCHECK_PREF, true);
		editor.commit();
		
		setPreferenceStatus(true);
		stopTime = System.currentTimeMillis();
		
		return (r.getString(R.string.prompt_sdcard_backup_successful) + " (" + (stopTime-startTime) + " ms)");
    }
    
    public String restoreInformation()
    {
    	long startTime = 0;
    	long stopTime = 0;
    	
    	startTime = System.currentTimeMillis();
    	//Is SD Card Accessible?
    	String sdState = Environment.getExternalStorageState();
    	
    	if(!Environment.MEDIA_MOUNTED.equals(sdState))
    		return r.getString(R.string.prompt_error_sdcard_mounted);
    	else if(Environment.MEDIA_MOUNTED_READ_ONLY.equals(sdState))
    		return r.getString(R.string.prompt_error_sdcard_readOnly);
    	
    	//Create Preferences File
		try
		{
			File path = new File(Environment.getExternalStorageDirectory() + "/Auto-Away/");
			path.mkdir();
			File inputFile = new File(path, "Preferences.bak");
			FileInputStream fos = new FileInputStream(inputFile);
			InputStreamReader iReader = new InputStreamReader(fos);
			BufferedReader bReader = new BufferedReader(iReader);

			String line = null;
			if((line = bReader.readLine()) == null)
				return r.getString(R.string.prompt_error_sdcard_restore);
			
			editor.putBoolean(SILENT_PREF, Boolean.parseBoolean(line));
			editor.putString(CALLTEXT_PREF, bReader.readLine());
			editor.putBoolean(LOG_PREF, Boolean.parseBoolean(bReader.readLine()));
			editor.putString(MESSAGE_PREF, bReader.readLine());
			editor.putString(FILTER_PREF, bReader.readLine());
			editor.putBoolean(INFORM_PREF, Boolean.parseBoolean(bReader.readLine()));
			editor.putString(DELAY_PREF, bReader.readLine());
			editor.putBoolean(REPEAT_PREF, Boolean.parseBoolean(bReader.readLine()));
			editor.putString(THEME_PREF, bReader.readLine());
			editor.commit();
			
			iReader.close();
		}
		catch (java.io.IOException exception) { Log.e(TAG, "IOException caused by trying to access \'Preferences.bak\'", exception); };
		
    	//Write Log to SD Card
		try
		{
			File path = new File(Environment.getExternalStorageDirectory() + "/Auto-Away/");
			OutputStreamWriter oWriter = new OutputStreamWriter(openFileOutput("auto_away.log",0));
			InputStreamReader iReader = new InputStreamReader(new FileInputStream(new File(path,"Log.bak")));
			BufferedReader bReader = new BufferedReader(iReader);

			String line = null;
			if((line = bReader.readLine()) != null)
				oWriter.write(line + "\n");
			
			while((line = bReader.readLine()) != null)
				oWriter.append(line + "\n");
			
			oWriter.flush();
			oWriter.close();
			iReader.close();
		}
		catch (java.io.IOException exception) { Log.e(TAG, "IOException caused by trying to access \'Log.bak\'", exception); };
		
		//Write Messages to SD Card
		try
		{
			File path = new File(Environment.getExternalStorageDirectory() + "/Auto-Away/");
			OutputStreamWriter oWriter = new OutputStreamWriter(openFileOutput("awayMessages.txt",0));
			InputStreamReader iReader = new InputStreamReader(new FileInputStream(new File(path,"Messages.bak")));
			BufferedReader bReader = new BufferedReader(iReader);

			String line = null;
			if((line = bReader.readLine()) != null)
				oWriter.write(line + "\n");
			
			while((line = bReader.readLine()) != null)
				oWriter.append(line + "\n");
			
			oWriter.flush();
			oWriter.close();
			iReader.close();
		}
		catch (java.io.IOException exception) { Log.e(TAG, "IOException caused by trying to access \'Messages.bak\'", exception); };
		
		//Write Filters to SD Card
		try
		{
			File path = new File(Environment.getExternalStorageDirectory() + "/Auto-Away/");
			OutputStreamWriter oWriter = new OutputStreamWriter(openFileOutput("filtering_blacklist.txt",0));
			InputStreamReader iReader = new InputStreamReader(new FileInputStream(new File(path,"Blacklist.bak")));
			BufferedReader bReader = new BufferedReader(iReader);

			String line = bReader.readLine();
			if((line = bReader.readLine()) != null)
				oWriter.write(line + "\n");
			
			while((line = bReader.readLine()) != null)
				oWriter.append(line + "\n");
			
			oWriter.flush();
			oWriter.close();
			iReader.close();
		}
		catch (java.io.IOException exception) { Log.e(TAG, "IOException caused by trying to access \'Blacklist.bak\'", exception); };
		
		try
		{
			File path = new File(Environment.getExternalStorageDirectory() + "/Auto-Away/");
			OutputStreamWriter oWriter = new OutputStreamWriter(openFileOutput("filtering_whitelist.txt",0));
			InputStreamReader iReader = new InputStreamReader(new FileInputStream(new File(path,"Whitelist.bak")));
			BufferedReader bReader = new BufferedReader(iReader);

			String line = bReader.readLine();
			if((line = bReader.readLine()) != null)
				oWriter.write(line + "\n");
			
			while((line = bReader.readLine()) != null)
				oWriter.append(line + "\n");
			
			oWriter.flush();
			oWriter.close();
			iReader.close();
		}
		catch (java.io.IOException exception) { Log.e(TAG, "IOException caused by trying to access \'Whitelist.bak\'", exception); };

		stopTime = System.currentTimeMillis();
		
		return (r.getString(R.string.prompt_sdcard_restore_successful) + " (" + (stopTime-startTime) + " ms)");
    }

    public boolean checkForBackups()
    {
    	File pFile = new File(Environment.getExternalStorageDirectory() + "/Auto-Away/Preferences.bak");
    	File lFile = new File(Environment.getExternalStorageDirectory() + "/Auto-Away/Log.bak");
    	File mFile = new File(Environment.getExternalStorageDirectory() + "/Auto-Away/Messages.bak");
    	File bFile = new File(Environment.getExternalStorageDirectory() + "/Auto-Away/Blacklist.bak");
    	File wFile = new File(Environment.getExternalStorageDirectory() + "/Auto-Away/Whitelist.bak");
    	
    	if (!pFile.exists() || !lFile.exists() || !mFile.exists() || !bFile.exists() || !wFile.exists())
    		return false;
    		
    	return true;
    }
    
    private void goToLink(String url)
    {
		Intent i = new Intent(Intent.ACTION_VIEW);
		i.setData(Uri.parse(url));
		startActivity(i);
    }
    
	public boolean getServiceStatus()						{ return prefs.getBoolean(SERVICE_PREF, false);												}
	public void setServiceStatus(boolean serviceRunning)	{ editor.putBoolean(SERVICE_PREF, serviceRunning);	
															  editor.commit();																			}
    
	public boolean getSilentStatus()						{ return prefs.getBoolean(SILENT_PREF, true);												}
	public void setSilentStatus(boolean isSilent)			{ editor.putBoolean(SILENT_PREF, isSilent);	
															  editor.commit();																			}
	
	public int getCallText()								{ return Integer.parseInt(prefs.getString(CALLTEXT_PREF, "0"));								}
	public void setCallText(String callText)				{ editor.putString(CALLTEXT_PREF, callText);
															  editor.commit();																			}
	
	public boolean getLogStatus()							{ return prefs.getBoolean(LOG_PREF, true);													}
	public void setLogStatus(boolean logStatus)				{ editor.putBoolean(LOG_PREF, logStatus);
															  editor.commit();																			}
	
	public String getMessage() 								{ return prefs.getString(MESSAGE_PREF, r.getString(R.string.default_message_content));		}
	
	public int getFilterStatus()							{ return Integer.parseInt(prefs.getString(FILTER_PREF, "0")); 								}
	public void setFilterStatus(int id)						{ editor.putString(FILTER_PREF, Integer.toString(id));
															  editor.commit();																			}
	
    public int getDelay()									{ return Integer.parseInt(prefs.getString(DELAY_PREF, "0"));								}
	
	public boolean getInformStatus()						{ return prefs.getBoolean(INFORM_PREF, true);												}
	public void setInformStatus(boolean informStatus)		{ editor.putBoolean(INFORM_PREF, informStatus);
															  editor.commit();																			}
	
	public boolean getRepeatStatus()						{ return prefs.getBoolean(REPEAT_PREF, false);												}
	public void setRepeatStatus(boolean repeatStatus)		{ editor.putBoolean(REPEAT_PREF, repeatStatus);
															  editor.commit();																			}
}