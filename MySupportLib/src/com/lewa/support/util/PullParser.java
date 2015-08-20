package com.lewa.support.util;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import android.util.Xml;
import java.io.File;
import java.io.FileInputStream;
import android.util.Log;

/**
 * Information about Lewa OS, extracted from system properties.
 */


///LEWA BEGIN
public class PullParser {
	private List<User> list;
	public List<User> getUserList(String xmlFileName) {
                 FileInputStream fis = null;
		User user = null;
		try {
			File f = new File(xmlFileName);
			if (!f.exists()) {
				return null;
			}
				//	Log.i("xml pull error", "icon alias file don't exists:" + xmlFileName);
			fis = new FileInputStream(f);
			XmlPullParser parser = Xml.newPullParser();
			parser.setInput(fis, "UTF-8");
			int eventType = parser.getEventType();



if(xmlFileName=="/system/etc/res/engineering_order.xml"){
			while (eventType != XmlPullParser.END_DOCUMENT) {
				switch (eventType) {
				case XmlPullParser.START_DOCUMENT:
					list = new ArrayList<User>();
					break;
				case XmlPullParser.START_TAG:
					String name = parser.getName();
					if ("user".equals(name)) {
						user = new User();
					} else if (user != null) {
						if ("commandid".equals(name)) {
							user.setcommandId(parser.nextText());
						}else if ("packagename".equals(name)) {
							user.setPackageName(parser.nextText());
						} else if ("classname".equals(name)) {
							user.setClassName(parser.nextText());
						} else if ("actionname".equals(name)) {
							user.setActionName(parser.nextText());
						} else if ("secretcode".equals(name)) {
							user.setSecretCodeName(parser.nextText());
						} else if ("putextraname".equals(name)) {
							user.setPutExtraName(parser.nextText());
						}						
					}
					break;
				case XmlPullParser.END_TAG:
					if ("user".equals(parser.getName())) {
						list.add(user);
					}
					break;
				}
				eventType = parser.next();
			}



}
else 
{
while (eventType != XmlPullParser.END_DOCUMENT) {
				switch (eventType) {
				case XmlPullParser.START_DOCUMENT:
					list = new ArrayList<User>();
					break;
				case XmlPullParser.START_TAG:
					String name = parser.getName();
					if ("item".equals(name)) {
						user = new User();
						user.setItem(parser.nextText());
						list.add(user);		
					}
					break;
				case XmlPullParser.END_TAG:
					if ("item".equals(parser.getName())) {
					}
					break;
				}
				eventType = parser.next();
			}
}
		} catch (Exception e) {
			//Log.e("xml pull error", e.toString());
		}
		return list;	
	}

public class User {
       private String commandid;
       private String packagename;
       private String classname;
       private String actionname;
       private String secretcode;
       private String putextraname;
       public String item;

	   
       public String getcommandId() {
		return commandid;
	}
	public void setcommandId(String commandid) {
		this.commandid = commandid;
	}
	
	public String getPackageName() {
		return packagename;
	}
	public void setPackageName(String packagename) {
		this.packagename = packagename;
	}
	
	public String getClassName() {
		return classname;
	}
	public void setClassName(String classname) {
		this.classname = classname;
	}
	

       public String getActionName() {
		return actionname;
	}
	public void setActionName(String actionname) {
		this.actionname = actionname;
	}
	
       public String getSecretCodeName() {
		return secretcode;
	}
	public void setSecretCodeName(String secretcode) {
		this.secretcode = secretcode;
	}
       public String getPutExtraName() {
		return putextraname;
	}
	public void setPutExtraName(String putextraname) {
		this.putextraname = putextraname;
	}
       public String getItem() {
		return item;
	}
	public void setItem(String item) {
		this.item = item;
	}
	

}

    }
///LEWA END











