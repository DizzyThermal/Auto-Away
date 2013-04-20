package com.teamdc.stephendiniz.autoaway.classes;

public class PhoneContact
{
	private String name;
	private String number;
	private String id;
	
	public PhoneContact()
	{
		name = "";
		number = "";
		id = "";
	}
	
	public PhoneContact(String name, String number, String id)
	{
		this.name	= name;
		this.number	= number;
		this.id		= id;
	}

	public String getName()										{ return name;			}
	public String getNumber()									{ return number;		}
	public String getId()										{ return id;			}
	
	public void setName(String name)							{ this.name = name;		}
	public void setNumber(String number)						{ this.number = number;	}
	public void setId(String id)								{ this.id = id;			}

	public void setInfo(String name, String number, String id)	{ this.name = name;
																  this.number = number;
												  				  this.id = id;			}
}
