package com.teamdc.stephendiniz.autoaway.classes;

public class PhoneContact
{
	private String name;
	private String id;
	
	public PhoneContact()
	{
		name = "";
		id = "";
	}
	
	public PhoneContact(String name, String id)
	{
		this.name = name;
		this.id = id;
	}

	public String getName()						{ return name;		}
	public String getId()						{ return id;		}
	
	public void setName(String name)			{ this.name = name; }
	public void setId(String id)				{ this.id = id;		}

	public void setInfo(String name, String id)	{ this.name = name;
												  this.id = id;		}
}
