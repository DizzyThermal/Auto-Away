package com.teamdc.stephendiniz.autoaway.classes;

public class LogEntry
{
	public static final int PHONE_CALL		= 0;
	public static final int TEXT_MESSAGE	= 1;
	
	private int type;
	private String time;
	private String date;
	private String name;
	private String number;
	
	public LogEntry()
	{
		type	= PHONE_CALL;
		time	= "";
		date	= "";
		name	= "";
		number	= "";
	}
	
	public LogEntry(int type, String time, String date, String name, String number)
	{
		this.type	= type;
		this.time	= time;
		this.date	= date;
		this.name	= name;
		this.number	= number;
	}
	
	public int getType()		{ return type;		}
	public String getTime()		{ return time;		}
	public String getDate()		{ return date;		}
	public String getName()		{ return name;		}
	public String getNumber()	{ return number;	}
	
	public void setType(int type)			{ this.type = type;		}
	public void setTime(String time)		{ this.time = time;		}
	public void setDate(String date)		{ this.date = date;		}
	public void setName(String name)		{ this.name = name;		}
	public void setNumber(String number)	{ this.number = number;	}
}
