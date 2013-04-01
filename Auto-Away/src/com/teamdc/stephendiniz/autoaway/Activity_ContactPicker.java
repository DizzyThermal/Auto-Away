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
import android.database.Cursor;
import android.os.Bundle;
import android.provider.Contacts;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.teamdc.stephendiniz.autoaway.classes.Contact;
import com.teamdc.stephendiniz.autoaway.classes.PhoneContact;

public class Activity_ContactPicker extends ListActivity 
{	
	private static final String	TAG = "ContactPicker";		

	Resources r;
	Dialog dialog;
	private Bundle infoBundle;

	private final int FILTER_BLACKLIST = 2;
	private final int FILTER_WHITELIST = 3;

	static final int FILTERING_ERROR_EXISTS	= 0;
	static final int FILTERING_ERROR_NUMBER	= 1;
	static final int FILTERING_ADDED		= 2;
	static final int FILTERING_SAVED		= 3;
	static final int FILTERING_BLANK		= 4;

	ArrayList<Contact> contacts = new ArrayList<Contact>();
	ArrayList<PhoneContact> pContacts = new ArrayList<PhoneContact>();

	private int filterStatus;
	private String file;
	
	@SuppressLint("NewApi")
	public void onCreate(Bundle SavedInstanceState)
	{
		super.onCreate(SavedInstanceState);

		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		r = getResources();
		Cursor cursor = getContentResolver().query(Contacts.CONTENT_URI, null, null, null, null);

		infoBundle = getIntent().getExtras();
		
		setFilterStatus(infoBundle.getInt("extraFilterStatus"));

		switch(getFilterStatus())
		{
			case FILTER_BLACKLIST:
				setFile("filtering_blacklist.txt");
				setTitle(r.getString(R.string.pref_filter_type_3));
			break;
			
			case FILTER_WHITELIST:
				setFile("filtering_whitelist.txt");
				setTitle(r.getString(R.string.pref_filter_type_4));
			break;
		}

		grabNumbers(getFile());

		ArrayList<String> random = new ArrayList<String>();

		//Checks contacts for the number passed (returnAddress)
		while (cursor.moveToNext())
		{
			int num = Integer.parseInt(cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.HAS_PHONE_NUMBER)));

			if (num > 0)
			{
				PhoneContact newPContact = new PhoneContact(cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME)), cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID)));
				pContacts.add(newPContact);
			}
			else
				random.add(cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME)));
		}

		cursor.close();
		sortContacts();

		String[] names = new String[pContacts.size()];
		for (int i = 0; i < pContacts.size(); i++)
			names[i] = pContacts.get(i).getName();
		
		this.setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, names));
	}
	
	@SuppressLint("NewApi")
	public void onListItemClick(ListView l, View v, int position, long id)
	{
		super.onListItemClick(l, v, position, id);
		Object o = this.getListAdapter().getItem(position);
		int count = 0;
		String number = null;
		String added = null;
		String keyword = o.toString();
		String name = keyword;
		String search = pContacts.get(phoneContactSearch(pContacts, keyword)).getId();
		Cursor phone = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID +" = ?", new String[]{search}, null);
		for (phone.moveToFirst(); !phone.isAfterLast(); phone.moveToNext())
		{
			number = hyphenate(phone.getString(phone.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)));
			if (!numberExists(number))
			{
				Contact newContact = new Contact(name, number);
				contacts.add(newContact);
				count += 1;
				added = number;
			}	
		}
		if (count == 0 && numberExists(number))
				showTheMessage(FILTERING_ERROR_EXISTS, number);
		else if (count == 0)
			showTheMessage(FILTERING_ERROR_NUMBER,null);
		else if (count > 1)
			showTheMessage(FILTERING_ADDED, name);
		else
			showTheMessage(FILTERING_ADDED, name + " (" + added + ")");
		phone.close();
		sortNames();
		saveNumbers(getFile());	
		finish();
	}
	
	public void sortNames()
	{
		// Bubble Sort with Object Specialized Data Swap
		for (int i = 0; i < contacts.size(); i++)
			for (int j = 0; j < contacts.size()-1-i; j++)
				if(contacts.get(j).getName().compareTo(contacts.get(j+1).getName()) > 0)
				{
					String tmpName = contacts.get(j).getName();
					String tmpNumber = contacts.get(j).getNumber();

					contacts.get(j).setInfo(contacts.get(j+1).getName(), contacts.get(j+1).getNumber());
					contacts.get(j+1).setInfo(tmpName, tmpNumber);
				}
	}
	
	public void sortContacts()
	{
		// Bubble Sort with Object Specialized Data Swap
		for (int i = 0; i < pContacts.size(); i++)
			for (int j = 0; j < pContacts.size()-1-i; j++)
				if(pContacts.get(j).getName().compareTo(pContacts.get(j+1).getName()) > 0)
				{
					String tmpName = pContacts.get(j).getName();
					String tmpId = pContacts.get(j).getId();

					pContacts.get(j).setInfo(pContacts.get(j+1).getName(), pContacts.get(j+1).getId());
					pContacts.get(j+1).setInfo(tmpName, tmpId);
				}
	}
	
	public boolean grabNumbers(String file)
	{
		int numOfContacts = 0;

		contacts.removeAll(contacts);
		
		File inFile = getBaseContext().getFileStreamPath(getFile());

		if (inFile.exists())
		{
			try
			{
				InputStream iStream = openFileInput(file);
				InputStreamReader iReader = new InputStreamReader(iStream);
				BufferedReader bReader = new BufferedReader(iReader);
				
				String line;
				//Should be in groups of TWO!
				while((line = bReader.readLine()) != null)
				{
					Contact contactFromFile = new Contact(line, bReader.readLine());
					contacts.add(contactFromFile);
					numOfContacts++;
				}
				
				iStream.close();
				
				Log.i(TAG, numOfContacts + " contact(s) read from file");
			}
			catch (java.io.FileNotFoundException exception) { Log.e(TAG, "FileNotFoundException caused by " + getFile(), exception);	}
			catch (IOException exception) 					{ Log.e(TAG, "IOException caused by buffreader.readLine()", exception); 	}
					
			if(contacts.isEmpty())
				return false;
		}
		
		return true;
	}
	
	public void saveNumbers(String file)
	{
		try
		{
			OutputStreamWriter oWriter = new OutputStreamWriter(openFileOutput(file, 0));
			
			for(int i = 0; i < contacts.size(); i++)
			{
				oWriter.append(contacts.get(i).getName() + "\n");
				oWriter.append(contacts.get(i).getNumber() + "\n");
			}
		
		oWriter.flush();
		oWriter.close();
		}
		catch (java.io.IOException exception) { Log.e(TAG, "IOException caused by trying to access " + getFile(), exception); };
	}
	
	public void showTheMessage(int id, String extra)
	{
		String message = "";

		switch(id)
		{
			case FILTERING_ERROR_NUMBER:
				message = r.getString(R.string.prompt_error_filter_number);
			break;
			
			case FILTERING_ERROR_EXISTS:
				message = r.getString(R.string.prompt_error_filter_exists);
			break;
			
			case FILTERING_ADDED:
				message = "\'" + extra + "\'" + " " + r.getString(R.string.prompt_added);
			break;
			
			case FILTERING_BLANK:
				message = r.getString(R.string.prompt_error_filter_blank);
			break;
		}
		
		Toast eToast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
		eToast.show();
	}
	
	public String hyphenate(String number)
	{
		if (number.length() == 10)
			return number.substring(0,3) + "-" + number.substring(3,6) + "-" + number.substring(6,10);
		
		if (number.length() == 11)
			return number.substring(0,1) + "-" + number.substring(1,4) + "-" + number.substring(4,7) + "-" + number.substring(7,11);
		
		//Not 10 digits long - Unable to hyphenate
		return number;
	}
	
	public boolean numberExists(String name)
	{
		boolean exists = false;

		for (int i = 0; i < contacts.size(); i++)
		{
			if (contacts.get(i).getName().equals(name))
			{
				exists = true;
				break;
			}
		}

		return exists;
	}
	
	public int phoneContactSearch(ArrayList<PhoneContact> pCon, String keyword)
	{
		for (int i = 0; i < pCon.size(); i++)
			if (pCon.get(i).getName().equals(keyword))
				return i;
		
		return -1;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId())
		{
			case android.R.id.home:
	            Intent parentActivityIntent = new Intent(this, Activity_Filtering.class);
	            parentActivityIntent.addFlags(
	                    Intent.FLAG_ACTIVITY_CLEAR_TOP |
	                    Intent.FLAG_ACTIVITY_NEW_TASK);
	            startActivity(parentActivityIntent);
	            finish();
	        return true;
		}
		
		return false;
	}
	
	public int getFilterStatus()						{ return filterStatus; 					}
	public void setFilterStatus(int filterStatus)		{ this.filterStatus = filterStatus;		}
	
	public String getFile()								{ return file;							}
	public void setFile(String file)					{ this.file = file;						}
}