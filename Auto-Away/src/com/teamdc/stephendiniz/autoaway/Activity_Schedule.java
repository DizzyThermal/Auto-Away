package com.teamdc.stephendiniz.autoaway;

import java.util.ArrayList;

import android.app.Activity;
import android.app.Dialog;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.teamdc.stephendiniz.autoaway.classes.Schedule;

public class Activity_Schedule extends Activity
{
	private static final String	TAG = "Schedules";

	private final String schedulesFile	= "schedules.txt";

	private ArrayList<Schedule> schedules = new ArrayList<Schedule>();
	
	private String[] sTitle;
	private String[] sStart;
	private String[] sStop;
	private String[] sDays;
	private String[] sMessageTitle;
	
	Resources r;
	Dialog dialog;

	//static final int MESSAGE_ERROR_EXISTS	= 0;
	//static final int MESSAGE_ERROR_BLANK	= 1;
	//static final int MESSAGE_ADDED			= 2;
	//static final int MESSAGE_SAVED			= 3;
	
	static final int CONTEXT_MENU_EDIT		= 0;
	static final int CONTEXT_MENU_REMOVE	= 1;
	
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
//		messagesExist(schedulesFile);
		TextView newText = new TextView(this);
		newText.setText("Hi");
		setContentView(newText);
		//ScheduleListArrayAdapter adapter = new ScheduleListArrayAdapter(this, sTitle, sStart, sStop, sDays, sMessageTitle);
		//setListAdapter(adapter);
		
		//registerForContextMenu(getListView());
	}

	public void onResume()
	{
		super.onResume();
		
		r = getResources();
	}

	public void onPause()
	{
		super.onPause();
		
		finish();
	}

	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_schedules, menu);
		
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) 
	{
		switch(item.getItemId())
		{
			case R.id.menu_schedules_add:
				
				dialog = new Dialog(this);
				
				dialog.setContentView(R.layout.schedule_add);
				dialog.setTitle(r.getString(R.string.prompt_message_title));
				
				Button pButton = (Button)dialog.findViewById(R.id.dialog_messagesButtonPositive_add);
				pButton.setOnClickListener(new View.OnClickListener()
				{
					public void onClick(View v)
					{
						// Add
					}
				});
				
				Button nButton = (Button)dialog.findViewById(R.id.dialog_messagesButtonNegative_add);
				nButton.setOnClickListener(new View.OnClickListener()
				{
					public void onClick(View v)
					{
						dialog.cancel();
					}
				});
				
				dialog.show();
			break;
		}
		
		return true;
	}

//	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
//	{
//		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
//		menu.setHeaderTitle(schedules.get(info.position).getTitle());
//		String[] menuItems = {r.getString(R.string.menu_edit), r.getString(R.string.menu_remove)};
//
//		for (int i = 0; i < menuItems.length; i++)
//			menu.add(Menu.NONE, i, i, menuItems[i]);
//	}
//	
//	public boolean onContextItemSelected(MenuItem item)
//	{
//	  AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
//	  int menuItemIndex = item.getItemId();
//
//	  final int iId = info.position;
//	  switch(menuItemIndex)
//	  {
//	  	case CONTEXT_MENU_EDIT:
//	  		dialog = new Dialog(this);
//			
//			dialog.setContentView(R.layout.messages_edit);
//			dialog.setTitle(r.getString(R.string.menu_edit) + " " + schedules.get(iId).getTitle());
//			
//			Button pButton = (Button)dialog.findViewById(R.id.dialog_messagesButtonPositive_edit);
//			pButton.setOnClickListener(new View.OnClickListener()
//			{
//				public void onClick(View v)
//				{
//					// Save Edit
//				}
//			});
//			
//			Button nButton = (Button)dialog.findViewById(R.id.dialog_messagesButtonNegative_edit);
//			nButton.setOnClickListener(new View.OnClickListener()
//			{
//				public void onClick(View v)
//				{
//					dialog.cancel();
//				}
//			});
//			
//			dialog.show();
//			EditText eTitle = (EditText)dialog.findViewById(R.id.dialog_messagesTitleEdit_edit);
//			eTitle.setText(schedules.get(iId).getTitle());
//	  	break;
//	  		
//	  	case CONTEXT_MENU_REMOVE:
//	  		Log.i(TAG, "\"" + schedules.get(info.position).getTitle() + "\" removed");
//	  		schedules.remove(info.position);
//	  		saveMessages(schedulesFile);
//
//			startActivity(getIntent()); finish();
//	  	break;
//	  }
//	  return true;
//	}

//	public boolean messagesExist(String file)
//	{
//		schedules.removeAll(schedules);
//
//		try
//		{
//			File inFile = getBaseContext().getFileStreamPath(schedulesFile);
//			
//			if (inFile.exists())
//			{
//				int numOfMessages = 0;
//				InputStream iStream = openFileInput(file);
//				InputStreamReader iReader = new InputStreamReader(iStream);
//				BufferedReader bReader = new BufferedReader(iReader);
//				
//				String line;
//				
//				//Should ALWAYS be in groups of two
//				while((line = bReader.readLine()) != null)
//				{
//					Message messageFromFile = new Message(line, bReader.readLine());
//					schedules.add(messageFromFile);
//					numOfMessages++;
//				}
//				
//				iStream.close();
//				
//				Log.i(TAG, numOfMessages + " message(s) read from file");
//			}
//			else
//				Log.w(TAG, "\"" + schedulesFile + "\" was not found!");
//		}
//		catch (java.io.FileNotFoundException exception) { Log.e(TAG, "FileNotFoundException caused by openFileInput(fileName)", exception); }
//		catch (IOException exception) 					{ Log.e(TAG, "IOException caused by buffreader.readLine()", exception); 			}
//		
//		sTitle = new String[schedules.size()];
//		sContent = new String[schedules.size()];
//		
//		for (int i = 0; i < schedules.size(); i++)
//		{
//			sTitle[i] = messages.get(i).getTitle();
//			sContent[i] = messages.get(i).getContent();
//		}
//		
//		if(messages.isEmpty())
//			return false;
//		
//		return true;
//	}
//	
//	public void saveMessages(String file)
//	{
//		try
//		{
//			OutputStreamWriter oWriter = new OutputStreamWriter(openFileOutput(file, 0));
//			
//			for(int i = 0; i < schedules.size(); i++)
//			{
//				oWriter.append(schedules.get(i).getTitle() + "\n");
//				oWriter.append(schedules.get(i).getContent() + "\n");
//			}
//		
//		oWriter.flush();
//		oWriter.close();
//		}
//		catch (java.io.IOException exception) { Log.e(TAG, "IOException caused by trying to access " + file, exception); };
//	}
//	public void showTheMessage(int id, String extra)
//	{
//		String message = "";
//
//		switch(id)
//		{
//			case MESSAGE_ERROR_BLANK:
//				message = r.getString(R.string.prompt_error_message_blank);
//			break;
//			
//			case MESSAGE_ERROR_EXISTS:
//				message = r.getString(R.string.prompt_error_message_exists);
//			break;
//			
//			case MESSAGE_ADDED:
//				message = "\'" + extra + "\'" + " " + r.getString(R.string.prompt_added);
//			break;
//			
//			case MESSAGE_SAVED:
//				message = "\'" + extra + "\'" + " " + r.getString(R.string.prompt_message_saved);
//		}
//		
//		Toast eToast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
//		eToast.show();
//	}
//	
//	public boolean titleExists(String title)
//	{
//		boolean exists = false;
//
//		for (int i = 0; i < messages.size(); i++)
//		{
//			if (messages.get(i).getTitle().equals(title))
//			{
//				exists = true;
//				break;
//			}
//		}
//
//		return exists;
//	}
}
