package com.teamdc.stephendiniz.autoaway.classes;

public class Message
{
	private String title;
	private String content;
	
	public Message()
	{
		title = "";
		content = "";
	}
	
	public Message(String title, String content)
	{
		this.title = title;
		this.content = content;
	}

	public String getTitle()							{ return title;				}
	public String getContent()							{ return content;			}
	
	public void setTitle(String title)					{ this.title = title; 		}
	public void setContent(String content)				{ this.content = content;	}
	
	public void setInfo(String title, String content)	{ this.title = title;
														  this.content = content;	}
}
