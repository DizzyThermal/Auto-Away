package com.teamdc.stephendiniz.autoaway.classes;

public class Schedule
{
	private String		title;
	private String		start;
	private String		stop;
	private boolean[]	days = new boolean[7];
	private Message		message;
	
	public Schedule(String title, String start, String stop, boolean[] days, Message message)
	{
		this.title = title;
		this.start = start;
		this.stop = stop;
		for (int i = 0; i < 7; i++)
			this.days[i] = days[i];
		this.message.setInfo(message.getTitle(), message.getContent());
	}

	public String getTitle()				{ return title;														}
	public String getStart()				{ return start;														}
	public String getStop()					{ return stop;														}
	public boolean[] getDays()				{ return days;														}
	public Message getMessage()				{ return message;													}
	
	public void setTitle(String title)		{ this.title = title;												}
	public void setStart(String start)		{ this.start = start;												}
	public void setStop(String stop)		{ this.stop = stop;													}
	public void setDays(boolean[] days)		{ for(int i = 0; i < 7; i++)
												this.days[i] = days[i];											}
	public void setMessage(Message message)	{ this.message.setInfo(message.getTitle(), message.getContent());	}
}
