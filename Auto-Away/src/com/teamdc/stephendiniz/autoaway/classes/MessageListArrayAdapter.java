package com.teamdc.stephendiniz.autoaway.classes;

import com.teamdc.stephendiniz.autoaway.R;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class MessageListArrayAdapter extends ArrayAdapter<String>
{
	private final Activity context;
	
	private final String[] title;
	private final String[] content;

	public MessageListArrayAdapter(Activity context, String[] title, String[] content)
	{
		super(context, R.layout.message_list, title);

		this.context = context;
		this.title = title;
		this.content = content;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		LayoutInflater inflater = context.getLayoutInflater();
		View rowView = inflater.inflate(R.layout.message_list, null, true);
		
		TextView textTitle = (TextView) rowView.findViewById(R.id.dl_Title);
		TextView textContent = (TextView) rowView.findViewById(R.id.dl_Content);
		
		textTitle.setText(title[position]);
		textContent.setText(content[position]);

		return rowView;
	}
}