/****************************************************************************
**
** Copyright (C) 2010-2012 UC Mobile Ltd. All Rights Reserved
** File        : WebConfiguration.java
**
** Description : Used to read configuration for debugging
**
** Creation    : 2012/12/24
** Author      : Roger (yixx@ucweb.com)
** History     :
**               Creation, 2012/12/24, Roger, Create the file
**
****************************************************************************/
package com.uc.webkit.utils;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import android.os.Environment;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.widget.Toast;

/**
 * Used to read configuration for debugging.
 *
 */
public final class WebConfiguration {
	
	public static final int TEST_URLS_CONFIG = 0;
	public static final int SCROLL_CONFIG = 1;
	
	private static final String[] CONFIG_FILES = new String[] {
		Environment.getExternalStorageDirectory().toString() + "/test_urls.config",
		Environment.getExternalStorageDirectory().toString() + "/scroll.config",
	};
	
	
	private final List<String> mLines = new ArrayList<String>();
	private final int mConfigName;
	private boolean mValid = false;

	
	public WebConfiguration(int configName) {
    	mConfigName = configName;		
        try {
        	FileInputStream fis = new FileInputStream(CONFIG_FILES[configName]);
        	DataInputStream dis = new DataInputStream(fis);
        	String line = dis.readLine();
        	while (line != null) {
        		mLines.add(line);
            	line = dis.readLine();
            }
    		dis.close();
    		fis.close();
    		mValid = true;
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
		}
	}
	
	public boolean isValid() {
		return mValid;
	}
	
	public void toast(Context context) {
		if (isValid()) {
			String msg = "Read " + CONFIG_FILES[mConfigName] + " - \n";
			for (String line : mLines) {
				msg += line + "\n";
			}
			Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
		} else {
			Toast.makeText(context, "Read " + CONFIG_FILES[mConfigName] + " failed.",
					Toast.LENGTH_SHORT).show();
		}
	}
	
	public String[] getLines() {
		return mLines.toArray(new String[mLines.size()]);
	}
}
