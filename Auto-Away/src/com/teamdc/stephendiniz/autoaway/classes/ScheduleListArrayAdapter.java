package com.teamdc.stephendiniz.autoaway.classes;

import com.teamdc.stephendiniz.autoaway.R;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class ScheduleListArrayAdapter extends ArrayAdapter<String>
{
	private final Activity context;
	
	private String[] title;
	private String[] start;
	private String[] stop;
	private String[] days;
	private String[] message;

	public ScheduleListArrayAdapter(Activity context, String[] title, String[] start, String[] stop, String[] days, String[] message)
	{
		super(context, R.layout.schedule_list, title);

		this.context = context;
		this.title = title;
		this.start = start;
		this.stop = stop;
		this.days = days;
		this.message = message;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		LayoutInflater inflater = context.getLayoutInflater();
		View rowView = inflater.inflate(R.layout.schedule_list, null, true);
		
		TextView textTitle = (TextView) rowView.findViewById(R.id.sch_Title);
		TextView textStartStop = (TextView) rowView.findViewById(R.id.sch_StartStop);
		TextView textDays = (TextView) rowView.findViewById(R.id.sch_Days);
		TextView textMessage = (TextView) rowView.findViewById(R.id.sch_Message);
		
		textTitle.setText(title[position]);
		textStartStop.setText(start[position] + " - " + stop[position]);
		textDays.setText(days[position]);
		textMessage.setText(message[position]);
		
		return rowView;
	}
}