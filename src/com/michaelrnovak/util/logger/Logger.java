/*
 * Copyright (C) 2009  Michael Novak <mike@androidnerds.org>
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.michaelrnovak.util.logger;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.michaelrnovak.util.logger.service.ILogProcessor;
import com.michaelrnovak.util.logger.service.LogProcessor;

import java.io.File;
import java.util.HashMap;

public class Logger extends Activity {
	private ILogProcessor mService;
	private ScrollView mScrollView;
	private LinearLayout mLines;
	private AlertDialog mDialog;
	private ProgressDialog mProgressDialog;
	private int mFilter = -1;
	private int mBuffer = 0;
	private int mLogType = 0;
	private String mFilterTag = "";
	private boolean mServiceRunning = false;
	public int MAX_LINES = 250;
	public static final int DIALOG_FILTER_ID = 1;
	public static final int DIALOG_SAVE_ID = 2;
	public static final int DIALOG_SAVE_PROGRESS_ID = 3;
	public static final int DIALOG_EMAIL_ID = 4;
	public static final int DIALOG_BUFFER_ID = 5;
	public static final int DIALOG_TYPE_ID = 6;
	public static final int DIALOG_TAG_ID = 7;
	public static final int FILTER_OPTION = Menu.FIRST;
	public static final int EMAIL_OPTION = Menu.FIRST + 1;
	public static final int SAVE_OPTION = Menu.FIRST + 2;
	public static final int BUFFER_OPTION = Menu.FIRST + 3;
	public static final int TYPE_OPTION = Menu.FIRST + 4;
	public static final int TAG_OPTION = Menu.FIRST + 5;
	final CharSequence[] items = {"Debug", "Error", "Info", "Verbose", "Warn", "All"};
	final char[] mFilters = {'D', 'E', 'I', 'V', 'W'};
	final CharSequence[] buffers = {"Main", "Radio", "Events"};
	final CharSequence[] types = {"Logcat", "Dmesg"};
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        mScrollView = (ScrollView) findViewById(R.id.scrollView);
        mLines = (LinearLayout) findViewById(R.id.lines);
        
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	bindService(new Intent(this, LogProcessor.class), mConnection, Context.BIND_AUTO_CREATE);
    	
    	//little housekeeping.
    	File f = new File("/sdcard/tmp.log");
    	if (f.exists()) {
    		f.delete();
    	}
    	
    }
    
    @Override
    public void onPause() {
    	super.onPause();
    	unbindService(mConnection);
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	
    	MenuItem item = menu.getItem(0);
    	
    	if (mBuffer != 0) {
    		item.setEnabled(false);
    	} else {
    		item.setEnabled(true);
    	}
    	
    	return super.onPrepareOptionsMenu(menu);
    }
    
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	
    	if (mBuffer == 0) {
    		menu.add(0, FILTER_OPTION, 1, "Filter Log").setIcon(android.R.drawable.ic_menu_view);
    	} else {
    		menu.add(0, FILTER_OPTION, 1, "Filter Log").setIcon(android.R.drawable.ic_menu_view).setEnabled(false);
    	}
    	
    	menu.add(0, TAG_OPTION, 2, "Tag Filter").setIcon(android.R.drawable.ic_menu_sort_alphabetically);
    	menu.add(0, BUFFER_OPTION, 3, "Choose Buffer").setIcon(android.R.drawable.ic_menu_manage);
    	menu.add(0, EMAIL_OPTION, 4, "Email Log").setIcon(android.R.drawable.ic_menu_send);
    	menu.add(0, SAVE_OPTION, 5, "Save Log").setIcon(android.R.drawable.ic_menu_save);
    	menu.add(0, TYPE_OPTION, 6, "Log Type").setIcon(android.R.drawable.ic_menu_set_as);
    	
    	return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    	case FILTER_OPTION:
    		onCreateDialog(DIALOG_FILTER_ID);
    		break;
    	case EMAIL_OPTION:
    		generateEmailMessage();
    		break;
    	case SAVE_OPTION:
    		onCreateDialog(DIALOG_SAVE_ID);
    		break;
    	case BUFFER_OPTION:
    		onCreateDialog(DIALOG_BUFFER_ID);
    		break;
    	case TYPE_OPTION:
    		onCreateDialog(DIALOG_TYPE_ID);
    		break;
    	case TAG_OPTION:
    		onCreateDialog(DIALOG_TAG_ID);
    		break;
    	default:
    		break;
    	}
    	
    	return false;
    }
    
    protected Dialog onCreateDialog(int id) {
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	
    	switch (id) {
    	case DIALOG_FILTER_ID:
    		builder.setTitle("Select a filter level");
    		builder.setSingleChoiceItems(items, mFilter, mClickListener);
    		mDialog = builder.create();
    		break;
    	case DIALOG_SAVE_ID:
    		builder.setTitle("Enter filename:");
    		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
    		View v = inflater.inflate(R.layout.file_save, (ViewGroup) findViewById(R.id.layout_root));
    		builder.setView(v);
    		builder.setNegativeButton("Cancel", mButtonListener);
    		builder.setPositiveButton("Save", mButtonListener);
    		mDialog = builder.create();
    		break;
    	case DIALOG_SAVE_PROGRESS_ID:
    		mProgressDialog = ProgressDialog.show(this, "", "Saving...", true);
    		return mProgressDialog;
    	case DIALOG_EMAIL_ID:
    		mProgressDialog = ProgressDialog.show(this, "", "Generating attachment...", true);
    		return mProgressDialog;
    	case DIALOG_BUFFER_ID:
    		builder.setTitle("Select a buffer");
    		builder.setSingleChoiceItems(buffers, mBuffer, mBufferListener);
    		mDialog = builder.create();
    		break;
    	case DIALOG_TYPE_ID:
    		builder.setTitle("Select a log");
    		builder.setSingleChoiceItems(types, mLogType, mTypeListener);
    		mDialog = builder.create();
    		break;
    	case DIALOG_TAG_ID:
    		builder.setTitle("Enter tag name");
    		LayoutInflater inflate = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
    		View t = inflate.inflate(R.layout.file_save, (ViewGroup) findViewById(R.id.layout_root));
    		EditText et = (EditText) t.findViewById(R.id.filename);
    		et.setText(mFilterTag);
    		builder.setView(t);
    		builder.setNegativeButton("Cancel", mTagListener);
    		builder.setPositiveButton("Filter", mTagListener);
    		mDialog = builder.create();
    		break;
    	default:
    		break;
    	}
    	
    	mDialog.show();
    	return mDialog;
    }
    
    DialogInterface.OnClickListener mClickListener = new DialogInterface.OnClickListener() {
		
		@Override
		public void onClick(DialogInterface dialog, int which) {
			if (which == 5) {
				mFilter = -1;
			} else {
				mFilter = which;
			}
			
			updateFilter();
		}
	};
	
	DialogInterface.OnClickListener mBufferListener = new DialogInterface.OnClickListener() {
		
		@Override
		public void onClick(DialogInterface dialog, int which) {
			mBuffer = which;
			
			updateBuffer();
		}
	};
	
	DialogInterface.OnClickListener mTypeListener = new DialogInterface.OnClickListener() {
		
		@Override
		public void onClick(DialogInterface dialog, int which) {
			mLogType = which;
			updateLog();
		}
	};
	
	DialogInterface.OnClickListener mButtonListener = new DialogInterface.OnClickListener() {
		
		@Override
		public void onClick(DialogInterface dialog, int which) {
			if (which == -1) {
				EditText et = (EditText) mDialog.findViewById(R.id.filename);
	    		onCreateDialog(DIALOG_SAVE_PROGRESS_ID);
	    		Log.d("Logger", "Filename: " + et.getText().toString());
	    		
				try {
					mService.write(et.getText().toString());
				} catch (RemoteException e) {
					Log.e("Logger", "Trouble writing the log to a file");
				}
			}
		}
	};

	DialogInterface.OnClickListener mTagListener = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			if (which == -1) {
				EditText et = (EditText) mDialog.findViewById(R.id.filename);
				
				mFilterTag = et.getText().toString().trim();
				updateFilterTag();
			}
		}
	};
	
    public void stopLogging() {
    	unbindService(mConnection);
    	mServiceRunning = false;
    	
    	if (mServiceRunning) {
    		Log.d("Logger", "mServiceRunning is still TRUE");
    	}
    }
    
    public void startLogging() {
    	bindService(new Intent(this, LogProcessor.class), mConnection, Context.BIND_AUTO_CREATE);
    	
    	try {
    		mService.run(mLogType);
    		mServiceRunning = true;
    	} catch (RemoteException e) {
    		Log.e("Logger", "Could not start logging");
    	}
    }
    
    private void handleLogMessage(String line) {
    	if (mFilter != -1 && line.charAt(0) != mFilters[mFilter]) {
    		return;
    	}
    	
    	if (!mFilterTag.equals("")) {
    		String tag = line.substring(2, line.indexOf("("));
    		
    		if (!mFilterTag.toLowerCase().equals(tag.toLowerCase().trim())) {
    			return;
    		}
    	}
    	
    	TextView lineView = new TextView(this);
    	lineView.setTypeface(Typeface.MONOSPACE);
    	
    	if (mLogType == 0) {
    		lineView.setText(new LogFormattedString(line));
    	} else {
    		lineView.setText(line);
    	}
    	
    	final boolean autoscroll = 
            (mScrollView.getScrollY() + mScrollView.getHeight() >= mLines.getBottom()) ? true : false;
    	
    	mLines.addView(lineView, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
    	
    	if (mLines.getChildCount() > MAX_LINES) {
    		mLines.removeViewAt(0);
    	}
    	
    	mScrollView.post(new Runnable() {
    		public void run() {
    			if (autoscroll == true) {
    				mScrollView.scrollTo(0, mLines.getBottom() - mScrollView.getHeight());
    			}
    		}
    	});
    }
    
    private void updateFilter() {
    	mLines.removeAllViews();
    	
    	try {
    		mService.reset(buffers[mBuffer].toString());
    	} catch (RemoteException e) {
    		Log.e("Logger", "Service is gone...");
    	}
    	
    	mDialog.dismiss();
    }
    
    private void updateBuffer() {
    	mLines.removeAllViews();
    	
    	try {
    		mService.reset(buffers[mBuffer].toString());
    	} catch (RemoteException e) {
    		Log.e("Logger", "Service is gone...");
    	}
    	
    	mDialog.dismiss();
    }
    
    private void updateLog() {
    	mLines.removeAllViews();
    	
    	try {
    		mService.restart(mLogType);
    	} catch (RemoteException e) {
    		Log.e("Logger", "Service is gone...");
    	}
    	
    	mDialog.dismiss();
    }
    
    private void updateFilterTag() {
    	mLines.removeAllViews();
    	
    	try {
    		mService.reset(buffers[mBuffer].toString());
    	} catch (RemoteException e) {
    		Log.e("Logger", "Service is gone...");
    	}
    	
    	mDialog.dismiss();
    }
    
    private void saveResult(String msg) {
    	mProgressDialog.dismiss();
    	
    	if (msg.equals("error")) {
    		Toast.makeText(this, "Error while saving the log to file!", Toast.LENGTH_LONG).show();
    	} else if (msg.equals("saved")) {
    		Toast.makeText(this, "Log has been saved to file.", Toast.LENGTH_LONG).show();
    	} else if (msg.equals("attachment")) {
    		Intent mail = new Intent(Intent.ACTION_SEND);
    		mail.setType("text/plain");
    		mail.putExtra(Intent.EXTRA_SUBJECT, "Logger Debug Output");
    		mail.putExtra(Intent.EXTRA_STREAM, Uri.parse("file:///sdcard/tmp.log"));
    		mail.putExtra(Intent.EXTRA_TEXT, "Here's the output from my log file. Thanks!");
    		startActivity(Intent.createChooser(mail, "Email:"));
    	}
    }
    
    private void generateEmailMessage() {
    	onCreateDialog(DIALOG_EMAIL_ID);
    	
    	try {
    		mService.write("tmp.log");
    	} catch (RemoteException e) {
    		Log.e("Logger", "Error generating email attachment.");
    	}
    }
    
    public Handler mHandler = new Handler() {
    	public void handleMessage(Message msg) {
    		switch (msg.what) {
    		case LogProcessor.MSG_READ_FAIL:
    			Log.d("Logger", "MSG_READ_FAIL");
    			break;
    		case LogProcessor.MSG_LOG_FAIL:
    			Log.d("Logger", "MSG_LOG_FAIL");
    			break;
    		case LogProcessor.MSG_NEW_LINE:
    			handleLogMessage((String) msg.obj);
    			break;
    		case LogProcessor.MSG_LOG_SAVE:
    			saveResult((String) msg.obj);
    			break;
    		default:
    			super.handleMessage(msg);
    		}
    	}
    };
    
    private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			mService = ILogProcessor.Stub.asInterface((IBinder)service);
			LogProcessor.setHandler(mHandler);
			
			try {
				mService.run(mLogType);
				mServiceRunning = true;
			} catch (RemoteException e) {
				Log.e("Logger", "Could not start logging");
			}
		}
		
		public void onServiceDisconnected(ComponentName className) {
			Log.i("Logger", "onServiceDisconnected has been called");
			mService = null;
		}
	};
    
    private static class LogFormattedString extends SpannableString {
    	public static final HashMap<Character, Integer> LABEL_COLOR_MAP;
    	
    	public LogFormattedString(String line) {
    		super(line);
    		
    		try {
    			
    			if (line.length() < 4) {
    				throw new RuntimeException();
    			}
    			
    			if (line.charAt(1) != '/') {
    				throw new RuntimeException();
    			}
    			
    			Integer labelColor = LABEL_COLOR_MAP.get(line.charAt(0));
    			
    			if (labelColor == null) {
    				labelColor = LABEL_COLOR_MAP.get('E');
    			}
    			
    			setSpan(new ForegroundColorSpan(labelColor), 0, 1, 0);
    			setSpan(new StyleSpan(Typeface.BOLD), 0, 1, 0);
    			
    			int leftIdx;
    			
    			if ((leftIdx = line.indexOf(':', 2)) >= 0) {
    				setSpan(new ForegroundColorSpan(labelColor), 2, leftIdx, 0);
    				setSpan(new StyleSpan(Typeface.ITALIC), 2, leftIdx, 0);
    			}
    			
    		} catch (Exception e) {
    			setSpan(new ForegroundColorSpan(0xffddaacc), 0, length(), 0);
    		}
    	}
    	
    	static {
    		LABEL_COLOR_MAP = new HashMap<Character, Integer>();
    		LABEL_COLOR_MAP.put('D', 0xff9999ff);
    		LABEL_COLOR_MAP.put('V', 0xffcccccc);
    		LABEL_COLOR_MAP.put('I', 0xffeeeeee);
    		LABEL_COLOR_MAP.put('E', 0xffff9999);
    		LABEL_COLOR_MAP.put('W', 0xffffff99);
    	}
    }
}