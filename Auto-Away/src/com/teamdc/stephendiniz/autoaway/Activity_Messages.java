package com.teamdc.stephendiniz.autoaway;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.teamdc.stephendiniz.autoaway.classes.Message;
import com.teamdc.stephendiniz.autoaway.classes.MessageListArrayAdapter;

public class Activity_Messages extends ListActivity
{
	private static final String	TAG = "Messages";

	private final String messagesFile	= "awayMessages.txt";

	private ArrayList<Message> messages = new ArrayList<Message>();
	
	private String[] sTitle;
	private String[] sContent;
	
	Resources r;
	Dialog dialog;

	static final int MESSAGE_ERROR_EXISTS	= 0;
	static final int MESSAGE_ERROR_BLANK	= 1;
	static final int MESSAGE_ADDED			= 2;
	static final int MESSAGE_SAVED			= 3;
	
	static final int CONTEXT_MENU_EDIT		= 0;
	static final int CONTEXT_MENU_REMOVE	= 1;
	
	@SuppressLint("NewApi")
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		if (android.os.Build.VERSION.SDK_INT >= 11)
			getActionBar().setDisplayHomeAsUpEnabled(true);
		
		messagesExist(messagesFile);
		
		MessageListArrayAdapter adapter = new MessageListArrayAdapter(this, sTitle, sContent);
		setListAdapter(adapter);
		
		registerForContextMenu(getListView());
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
		inflater.inflate(R.menu.menu_messages, menu);
		
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) 
	{
		switch(item.getItemId())
		{
			case R.id.menu_messages_add:
				
				dialog = new Dialog(this);
				
				dialog.setContentView(R.layout.messages_add);
				dialog.setTitle(r.getString(R.string.prompt_message_title));
				
				Button pButton = (Button)dialog.findViewById(R.id.dialog_messagesButtonPositive_add);
				pButton.setOnClickListener(new View.OnClickListener()
				{
					public void onClick(View v)
					{
						EditText eTitle = (EditText)dialog.findViewById(R.id.dialog_messagesTitleEdit_add);
						EditText eContent = (EditText)dialog.findViewById(R.id.dialog_messagesContentEdit_add);
						
						if(eTitle.getText().toString().equals("") ||eTitle.getText().toString().equals(null)||eContent.getText().toString().equals("")||eContent.getText().toString().equals(null))
							showTheMessage(MESSAGE_ERROR_BLANK, null);

						else
						{
							if (titleExists(eTitle.getText().toString()))
								showTheMessage(MESSAGE_ERROR_EXISTS, null);
							
							else
							{
								Message newMessage = new Message(eTitle.getText().toString().trim(),eContent.getText().toString().trim());
								messages.add(newMessage);
								showTheMessage(MESSAGE_ADDED, eTitle.getText().toString().trim());
								dialog.cancel();
								saveMessages(messagesFile);
								startActivity(getIntent()); finish();
							}
						}
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
			case android.R.id.home:
	            Intent parentActivityIntent = new Intent(this, Activity_Main.class);
	            parentActivityIntent.addFlags(
	                    Intent.FLAG_ACTIVITY_CLEAR_TOP |
	                    Intent.FLAG_ACTIVITY_NEW_TASK);
	            startActivity(parentActivityIntent);
	            finish();
	       return true;
		}
		
		return true;
	}

	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
	{
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
		menu.setHeaderTitle(messages.get(info.position).getTitle());
		String[] menuItems = {r.getString(R.string.menu_edit), r.getString(R.string.menu_remove)};

		for (int i = 0; i < menuItems.length; i++)
			menu.add(Menu.NONE, i, i, menuItems[i]);
	}
	
	public boolean onContextItemSelected(MenuItem item)
	{
	  AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
	  int menuItemIndex = item.getItemId();

	  final int iId = info.position;
	  switch(menuItemIndex)
	  {
	  	case CONTEXT_MENU_EDIT:
	  		dialog = new Dialog(this);
			
			dialog.setContentView(R.layout.messages_edit);
			dialog.setTitle(r.getString(R.string.menu_edit) + " " + messages.get(iId).getTitle());
			
			Button pButton = (Button)dialog.findViewById(R.id.dialog_messagesButtonPositive_edit);
			pButton.setOnClickListener(new View.OnClickListener()
			{
				public void onClick(View v)
				{
					EditText eTitle = (EditText)dialog.findViewById(R.id.dialog_messagesTitleEdit_edit);
					EditText eContent = (EditText)dialog.findViewById(R.id.dialog_messagesContentEdit_edit);
					
					if(eTitle.getText().toString().equals("") ||eTitle.getText().toString().equals(null)||eContent.getText().toString().equals("")||eContent.getText().toString().equals(null))
						showTheMessage(MESSAGE_ERROR_BLANK, null);

					else
					{
						if((messages.get(iId).getTitle().equals(eTitle.getText().toString())) && (messages.get(iId).getContent().equals(eContent.getText().toString())))
							dialog.cancel();

						else
						{
							messages.get(iId).setInfo(eTitle.getText().toString().trim(), eContent.getText().toString().trim());
							showTheMessage(MESSAGE_SAVED, eTitle.getText().toString().trim());
							dialog.cancel();
							saveMessages(messagesFile);
							
							Log.i(TAG, "\"" + messages.get(iId).getTitle() + "\" edited successfully");
							startActivity(getIntent()); finish();
						}
					}
				}
			});
			
			Button nButton = (Button)dialog.findViewById(R.id.dialog_messagesButtonNegative_edit);
			nButton.setOnClickListener(new View.OnClickListener()
			{
				public void onClick(View v)
				{
					dialog.cancel();
				}
			});
			
			dialog.show();
			EditText eTitle = (EditText)dialog.findViewById(R.id.dialog_messagesTitleEdit_edit);
			eTitle.setText(messages.get(iId).getTitle());
			EditText eContent = (EditText)dialog.findViewById(R.id.dialog_messagesContentEdit_edit);
			eContent.setText(messages.get(iId).getContent());
	  	break;
	  		
	  	case CONTEXT_MENU_REMOVE:
	  		Log.i(TAG, "\"" + messages.get(info.position).getTitle() + "\" removed");
	  		messages.remove(info.position);
	  		saveMessages(messagesFile);

			startActivity(getIntent()); finish();
	  	break;
	  }
	  return true;
	}

	public boolean messagesExist(String file)
	{
		messages.removeAll(messages);

		try
		{
			File inFile = getBaseContext().getFileStreamPath(messagesFile);
			
			if (inFile.exists())
			{
				int numOfMessages = 0;
				InputStream iStream = openFileInput(file);
				InputStreamReader iReader = new InputStreamReader(iStream);
				BufferedReader bReader = new BufferedReader(iReader);
				
				String line;
				
				//Should ALWAYS be in groups of two
				while((line = bReader.readLine()) != null)
				{
					Message messageFromFile = new Message(line, bReader.readLine());
					messages.add(messageFromFile);
					numOfMessages++;
				}
				
				iStream.close();
				
				Log.i(TAG, numOfMessages + " message(s) read from file");
			}
			else
				Log.w(TAG, "\"" + messagesFile + "\" was not found!");
		}
		catch (java.io.FileNotFoundException exception) { Log.e(TAG, "FileNotFoundException caused by openFileInput(fileName)", exception); }
		catch (IOException exception) 					{ Log.e(TAG, "IOException caused by buffreader.readLine()", exception); 			}
		
		sTitle = new String[messages.size()];
		sContent = new String[messages.size()];
		
		for (int i = 0; i < messages.size(); i++)
		{
			sTitle[i] = messages.get(i).getTitle();
			sContent[i] = messages.get(i).getContent();
		}
		
		if(messages.isEmpty())
			return false;
		
		return true;
	}
	
	public void saveMessages(String file)
	{
		try
		{
			OutputStreamWriter oWriter = new OutputStreamWriter(openFileOutput(file, 0));
			
			for(int i = 0; i < messages.size(); i++)
			{
				oWriter.append(messages.get(i).getTitle() + "\n");
				oWriter.append(messages.get(i).getContent() + "\n");
			}
		
		oWriter.flush();
		oWriter.close();
		}
		catch (java.io.IOException exception) { Log.e(TAG, "IOException caused by trying to access " + file, exception); };
	}
	public void showTheMessage(int id, String extra)
	{
		String message = "";

		switch(id)
		{
			case MESSAGE_ERROR_BLANK:
				message = r.getString(R.string.prompt_error_message_blank);
			break;
			
			case MESSAGE_ERROR_EXISTS:
				message = r.getString(R.string.prompt_error_message_exists);
			break;
			
			case MESSAGE_ADDED:
				message = "\'" + extra + "\'" + " " + r.getString(R.string.prompt_added);
			break;
			
			case MESSAGE_SAVED:
				message = "\'" + extra + "\'" + " " + r.getString(R.string.prompt_message_saved);
		}
		
		Toast eToast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
		eToast.show();
	}
	
	public boolean titleExists(String title)
	{
		boolean exists = false;

		for (int i = 0; i < messages.size(); i++)
		{
			if (messages.get(i).getTitle().equals(title))
			{
				exists = true;
				break;
			}
		}

		return exists;
	}
}
