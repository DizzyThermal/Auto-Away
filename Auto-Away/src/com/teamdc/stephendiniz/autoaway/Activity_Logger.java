package com.teamdc.stephendiniz.autoaway;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Calendar;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.teamdc.stephendiniz.autoaway.classes.Contact;
import com.teamdc.stephendiniz.autoaway.classes.LogEntry;

public class Activity_Logger extends ListActivity
{
	private static final String	TAG = "Logger";
	private static final String fileName = "auto_away.log";
	
	private static final int CONTEXT_MENU_REMOVE		= 0;
	private static final int CONTEXT_MENU_ADDTOFILTER	= 1;
	
	ArrayList<LogEntry> entries = new ArrayList<LogEntry>();
	String[] names;
	
	@SuppressLint("NewApi")
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		if (android.os.Build.VERSION.SDK_INT >= 11)
			getActionBar().setDisplayHomeAsUpEnabled(true);
		
		populateLogList();
		this.setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, names));
		
		registerForContextMenu(getListView());
	}
	
	public void populateLogList()
	{
		File inFile = getBaseContext().getFileStreamPath(fileName);
		int numOfLogEntries = 0;

		if (inFile.exists())
		{
			try
			{
				InputStream instream = openFileInput(fileName);
				
				InputStreamReader inputreader = new InputStreamReader(instream);
				BufferedReader buffreader = new BufferedReader(inputreader);
				
				String line;
				
				while((line = buffreader.readLine()) != null)
				{
					if (line.equals("\n"))
						break;

					LogEntry newLogEntry = new LogEntry(Integer.parseInt(line), buffreader.readLine(), buffreader.readLine(), buffreader.readLine(), buffreader.readLine());
					entries.add(newLogEntry);
					
					numOfLogEntries++;
				}

				Log.i(TAG, numOfLogEntries + " Log(s) loaded");
				
				names = new String[numOfLogEntries];
				
				for (int i = 0; i < numOfLogEntries; i++)
				{
					if (!entries.get(i).getName().equals(""))
						names[i] = entries.get(i).getName();
					else
						names[i] = entries.get(i).getNumber();
				}
				
				instream.close();
			}
			catch (java.io.FileNotFoundException exception) { Log.e(TAG, "FileNotFoundException caused by openFileInput(fileName)", exception); }
			catch (IOException exception) 					{ Log.e(TAG, "IOException caused by buffreader.readLine()", exception); 			}
		}
		else
			names = new String[0];
	}
	
	public void writeToLog(boolean clear) throws IOException
	{
		if (clear)
			entries.removeAll(entries);

		OutputStreamWriter osw = new OutputStreamWriter(openFileOutput(fileName, 0));
			
		osw.write("");
		for (int i = 0; i < entries.size(); i++)
		{
			osw.append(Integer.toString(entries.get(i).getType()) + "\n");
			osw.append(entries.get(i).getTime() + "\n");
			osw.append(entries.get(i).getDate() + "\n");
			osw.append(entries.get(i).getName() + "\n");
			osw.append(entries.get(i).getNumber() + "\n");
		}
			
		osw.flush();
		osw.close();
		
		if (clear)
			Log.i(TAG, "Logs cleared");
		else
			Log.i(TAG, entries.size() + " Log(s) saved");
		
		startActivity(getIntent()); finish();
	}
	
	public String exportToExcel() throws IOException
	{
		//Is SD Card Accessible?
    	String sdState = Environment.getExternalStorageState();

    	if(!Environment.MEDIA_MOUNTED.equals(sdState))
    		return getResources().getString(R.string.prompt_error_sdcard_mounted);
    	else if(Environment.MEDIA_MOUNTED_READ_ONLY.equals(sdState))
    		return getResources().getString(R.string.prompt_error_sdcard_readOnly);
    	
    	// Are there logs?
    	if (entries.size() == 0)
    		return getResources().getString(R.string.prompt_export_noLogs);

    	// Find Log "Time and Date"
		Calendar c = Calendar.getInstance();

		int hour = c.get(Calendar.HOUR_OF_DAY);
		int minute = c.get(Calendar.MINUTE);
		int second = c.get(Calendar.SECOND);
		
		int year = Integer.parseInt(Integer.toString(c.get(Calendar.YEAR)).substring(2,4));
		int month = c.get(Calendar.MONTH) + 1;
		int day = c.get(Calendar.DAY_OF_MONTH);
    	
		String exportFile = "Auto-Away_Log_" + pad(hour) + "." + pad(minute) + "." + pad(second) + "-" + pad(month) + "." + pad(day) + "." + pad(year) + ".csv";
		
    	//Create Preferences File
		try
		{
			File path = new File(Environment.getExternalStorageDirectory() + "/");
			path.mkdir();

			File outputFile = new File(path, exportFile);
			FileOutputStream fos = new FileOutputStream(outputFile);
			OutputStreamWriter oWriter = new OutputStreamWriter(fos);

			oWriter.write("Date,Time,Name,Number,Type\n");
			
			for (int i = 0; i < entries.size(); i++)
				oWriter.append(entries.get(i).getDate() + "," + entries.get(i).getTime() + "," + entries.get(i).getName() + "," + entries.get(i).getNumber() + "," + parseType(entries.get(i).getType()) + "\n");
			
			oWriter.flush();
			oWriter.close();
			
			Log.i(TAG, "Preferences Backup Successful");
		}
		catch (java.io.IOException exception) { Log.e(TAG, "IOException caused by trying to access \'Preferences.bak\'", exception); };
		
		return getResources().getString(R.string.prompt_export_success) + " \"/sdcard/" + exportFile + "\"";
	}
	
	private String pad(int number)
	{
		String time = Integer.toString(number);

		if(number < 10)
			time = "0" + Integer.toString(number);
		
		return time;
	}
	
	private String parseType(int type)
	{
		switch(type)
		{
			case LogEntry.PHONE_CALL:
				return getResources().getString(R.string.prompt_log_dialog_type_call);
			case LogEntry.TEXT_MESSAGE:
				return getResources().getString(R.string.prompt_log_dialog_type_text);
			default:
				return Integer.toString(type);
		}
	}
	
	public boolean onCreateOptionsMenu(Menu menu)
	{
		menu.clear();
		
		MenuItem logout = menu.add(getResources().getString(R.string.menu_clear));
		logout.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener()
		{
			public boolean onMenuItemClick(MenuItem item)
			{
				try
				{
					writeToLog(true);
				}
				catch (IOException e) { e.printStackTrace(); }

				return false;
			}
		});
		
		MenuItem export = menu.add(getResources().getString(R.string.menu_export));
		export.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener()
		{
			public boolean onMenuItemClick(MenuItem item)
			{
				try
				{
					Toast eToast = Toast.makeText(getBaseContext(), exportToExcel(), Toast.LENGTH_LONG);
					eToast.show();
				}
				catch (IOException e) { e.printStackTrace(); }
				
				return false;
			}
		});
		
		return super.onPrepareOptionsMenu(menu);
	}
	
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
	{
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
		int vId = info.position;

		if(!(entries.get(vId).getName().equals("")))
			menu.setHeaderTitle(entries.get(vId).getName());
		else
			menu.setHeaderTitle(entries.get(vId).getNumber());

		String[] menuItems = {	getResources().getString(R.string.menu_remove),
								getResources().getString(R.string.menu_addToFilter) };

		for (int i = 0; i < menuItems.length; i++)
			menu.add(Menu.NONE, i, i, menuItems[i]);
	}
	
	public boolean onContextItemSelected(MenuItem item)
	{
	  AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
	  int menuItemIndex = item.getItemId();
	  
	  switch(menuItemIndex)
	  {
	  	case CONTEXT_MENU_REMOVE:
	  		entries.remove(info.position);
	  		try
			{
				writeToLog(false);
			}
			catch (IOException e) { e.printStackTrace(); }
	  	break;
	  	case CONTEXT_MENU_ADDTOFILTER:
	  		final Dialog dialog = new Dialog(this);
	  		
	  		if(!(entries.get(info.position).getName().equals("")))
	  			dialog.setTitle(entries.get(info.position).getName());
			else
				dialog.setTitle(entries.get(info.position).getNumber());
			
			LinearLayout ll = new LinearLayout(this);
			ll.setOrientation(LinearLayout.VERTICAL);
			
			TextView body = new TextView(this);
			body.setText(getResources().getString(R.string.prompt_filter_add));
			body.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
			body.setGravity(Gravity.CENTER);
			body.setPadding(0, 20, 0, 20);
			
			LinearLayout buttonWrap = new LinearLayout(this);
			buttonWrap.setOrientation(LinearLayout.HORIZONTAL);
			buttonWrap.setGravity(Gravity.CENTER);
			buttonWrap.setPadding(0, 0, 0, 25);
			
			Button blackList = new Button(this);
			Button whiteList = new Button(this);
			Button close = new Button(this);
			
			close.setText(getResources().getString(R.string.menu_close));
			
			blackList.setText(getResources().getString(R.string.pref_filter_type_3));
			whiteList.setText(getResources().getString(R.string.pref_filter_type_4));
			
			buttonWrap.addView(blackList);
			buttonWrap.addView(whiteList);
			
			ll.addView(body);
			ll.addView(buttonWrap);
			ll.addView(close);
			
			final int id = info.position;
			
			blackList.setOnClickListener(new OnClickListener()
			{
				public void onClick(View v)
				{
					Toast eToast = Toast.makeText(getBaseContext(), inList(id, 0), Toast.LENGTH_SHORT);
					eToast.show();

					dialog.cancel();
				}
			});
			
			whiteList.setOnClickListener(new OnClickListener()
			{
				public void onClick(View v)
				{
					Toast eToast = Toast.makeText(getBaseContext(), inList(id, 1), Toast.LENGTH_SHORT);
					eToast.show();

					dialog.cancel();
				}
			});
			
			close.setOnClickListener(new OnClickListener()
			{
				public void onClick(View v)
				{
					dialog.cancel();
				}
			});
			
			dialog.setContentView(ll);
			dialog.show();
	  	break;
	  }
	  return true;
	}
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id)
	{
		final Dialog dialog = new Dialog(this);
		
		LinearLayout ll = new LinearLayout(this);

		TableLayout tl = new TableLayout(this);
		ArrayList<TableRow> tr = new ArrayList<TableRow>();

		int i = position;

		TextView number = new TextView(this);
		TextView date = new TextView(this);
		TextView time = new TextView(this);
		TextView type = new TextView(this);
		
		TextView lNumber = new TextView(this);
		TextView lDate = new TextView(this);
		TextView lTime = new TextView(this);
		TextView lType = new TextView(this);
		
		number.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
		date.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
		time.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
		type.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
		
		lNumber.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
		lDate.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
		lTime.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
		lType.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
		
		lNumber.setGravity(Gravity.RIGHT);
		lDate.setGravity(Gravity.RIGHT);
		lTime.setGravity(Gravity.RIGHT);
		lType.setGravity(Gravity.RIGHT);
		
		lNumber.setPadding(15, 2, 20, 2);
		lDate.setPadding(15, 2, 20, 2);
		lTime.setPadding(15, 2, 20, 2);
		lType.setPadding(15, 2, 20, 2);

		if (entries.get(i).getName().equals(""))
			dialog.setTitle(entries.get(i).getNumber());
		else
		{
			dialog.setTitle(entries.get(i).getName());

			TableRow newTableRow = new TableRow(this);
			
			TextView lName = new TextView(this);
			lName.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
			lName.setText(this.getResources().getString(R.string.prompt_log_dialog_name));
			lName.setGravity(Gravity.RIGHT);
			lName.setPadding(15, 2, 20, 2);
			newTableRow.addView(lName);

			TextView name = new TextView(this);
			name.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
			name.setText(entries.get(i).getName());
			newTableRow.addView(name);

			tr.add(newTableRow);
		}
		
		TableRow tRNumber = new TableRow(this);
		TableRow tRDate = new TableRow(this);
		TableRow tRTime = new TableRow(this);
		TableRow tRType = new TableRow(this);

		lNumber.setText(this.getResources().getString(R.string.prompt_log_dialog_number));
		number.setText(entries.get(i).getNumber());
		
		lDate.setText(this.getResources().getString(R.string.prompt_log_dialog_date));
		date.setText(entries.get(i).getDate());

		lTime.setText(this.getResources().getString(R.string.prompt_log_dialog_time));
		time.setText(entries.get(i).getTime());
		
		lType.setText(this.getResources().getString(R.string.prompt_log_dialog_type));

		switch(entries.get(i).getType())
		{
			case LogEntry.PHONE_CALL:
				type.setText(this.getResources().getString(R.string.prompt_log_dialog_type_call));
			break;
			case LogEntry.TEXT_MESSAGE:
				type.setText(this.getResources().getString(R.string.prompt_log_dialog_type_text));
			break;
		}
		
		tRNumber.addView(lNumber);
		tRNumber.addView(number);
		tRNumber.setBackgroundColor(Color.DKGRAY);
		
		tRDate.addView(lDate);
		tRDate.addView(date);
		
		tRTime.addView(lTime);
		tRTime.addView(time);
		tRTime.setBackgroundColor(Color.DKGRAY);

		tRType.addView(lType);
		tRType.addView(type);
		
		tr.add(tRNumber);
		tr.add(tRDate);
		tr.add(tRTime);
		tr.add(tRType);
		
		for (int j = 0; j < tr.size(); j++)
			tl.addView(tr.get(j));
		
		ll.setOrientation(LinearLayout.VERTICAL);
		ll.addView(tl);
		
		Button close = new Button(this);
		close.setText(getResources().getString(R.string.menu_close));
		close.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				dialog.cancel();
			}
		});

		ll.addView(close);
		
		dialog.setContentView(ll);
		dialog.show();
	}
	
	public String inList(int entryId, int list)
	{
		String file, listName, noun;

		if (list == 0)
		{
			file = "filtering_blacklist.txt";
			listName = getResources().getString(R.string.pref_filter_type_3);
		}
		else
		{
			file = "filtering_whitelist.txt";
			listName = getResources().getString(R.string.pref_filter_type_4);
		}

		if(!(entries.get(entryId).getName().equals("")))
  			noun = entries.get(entryId).getName();
		else
			noun = entries.get(entryId).getNumber();
		
		ArrayList<Contact> contacts = new ArrayList<Contact>();
		try
		{
			File inFile = getBaseContext().getFileStreamPath(file);
			
			if (inFile.exists())
			{
				InputStream iStream = openFileInput(file);
				InputStreamReader iReader = new InputStreamReader(iStream);
				BufferedReader bReader = new BufferedReader(iReader);
				
				String line;
				//Should be in groups of TWO!
				while((line = bReader.readLine()) != null)
				{
					if (line.equals("\n"))
						break;
					
					Contact contactFromFile = new Contact(line, bReader.readLine());
					contacts.add(contactFromFile);
				}

				iStream.close();
			}
			else
				Log.w(TAG, "\"" + file + "\" was not found!");
		}
		catch (java.io.FileNotFoundException exception) { Log.e(TAG, "FileNotFoundException caused by openFileInput(fileName)", exception); }
		catch (IOException exception) 					{ Log.e(TAG, "IOException caused by buffreader.readLine()", exception); 			}
		
		boolean isPresent = false;
		
		for (int i = 0; i < contacts.size(); i++)
		{
			if (contacts.get(i).getNumber().equals(entries.get(entryId).getNumber()))
			{
				isPresent = true;
				break;
			}
		}
		
		if (isPresent)
			return noun + " " + getResources().getString(R.string.prompt_error_filter_present) + " " + listName;

		try
		{
			OutputStreamWriter oWriter = new OutputStreamWriter(openFileOutput(file, 0));
			
			oWriter.append(noun + "\n");
			oWriter.append(entries.get(entryId).getNumber() + "\n");
		
			oWriter.flush();
			oWriter.close();
		}
		catch (java.io.IOException exception) { Log.e(TAG, "IOException caused by trying to access " + file, exception); };
		
		return noun + " " + getResources().getString(R.string.prompt_added);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId())
		{
			case android.R.id.home:
	            Intent parentActivityIntent = new Intent(this, Activity_Main.class);
	            parentActivityIntent.addFlags(
	                    Intent.FLAG_ACTIVITY_CLEAR_TOP |
	                    Intent.FLAG_ACTIVITY_NEW_TASK);
	            startActivity(parentActivityIntent);
	            finish();
	        return true;
		}
		
		return false;
	}
}
