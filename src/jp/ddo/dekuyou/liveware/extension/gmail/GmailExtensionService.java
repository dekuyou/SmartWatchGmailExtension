/*
 Copyright (c) 2011, Sony Ericsson Mobile Communications AB

 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice, this
 list of conditions and the following disclaimer.

 * Redistributions in binary form must reproduce the above copyright notice,
 this list of conditions and the following disclaimer in the documentation
 and/or other materials provided with the distribution.

 * Neither the name of the Sony Ericsson Mobile Communications AB nor the names
 of its contributors may be used to endorse or promote products derived from
 this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package jp.ddo.dekuyou.liveware.extension.gmail;

import java.util.List;
import java.util.Set;

import jp.ddo.dekuyou.android.util.Log;
import jp.ddo.dekuyou.android.util.PaydVersionConfirm;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.sonyericsson.extras.liveware.aef.notification.Notification;
import com.sonyericsson.extras.liveware.extension.util.ExtensionService;
import com.sonyericsson.extras.liveware.extension.util.notification.NotificationUtil;
import com.sonyericsson.extras.liveware.extension.util.registration.RegistrationInformation;

/**
 * The sample extension service handles extension registration and inserts data
 * into the notification database.
 */
public class GmailExtensionService extends ExtensionService {



	/**
	 * Extensions specific id for the source
	 */
	public static final String EXTENSION_SPECIFIC_ID = "EXTENSION_SPECIFIC_ID_POOR_GMAIL_NOTIFIER";

	/**
	 * Extension key
	 */
	public static final String EXTENSION_KEY = "jp.ddo.dekuyou.liveware.extension.gmail.key";

	public GmailExtensionService() {
		super(EXTENSION_KEY);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see android.app.Service#onCreate()
	 */
	@Override
	public void onCreate() {
		super.onCreate();
		Log.d("onCreate");
	}

	private String account;
	private int unreadcount;

	/**
	 * {@inheritDoc}
	 * 
	 * @see android.app.Service#onStartCommand()
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		int retVal = super.onStartCommand(intent, flags, startId);

		Log.initialize(this);

		Log.d(" GmailExtensionService onStartCommand ");

		if (intent == null) {
			Log.d("intent is null.");
			return START_NOT_STICKY; // FIXME: onStartCommand の戻り値はいったい？
		}

		Bundle extras = intent.getExtras();
		

		SharedPreferences appprefs = PreferenceManager
				.getDefaultSharedPreferences(this);


		if (extras == null) {
			Log.d("extras is null.");
			return START_NOT_STICKY; // FIXME: onStartCommand の戻り値はいったい？
		}else {

			
			if ("^^unseen-^i".equals(extras.getString("tagLabel"))){
				Editor e = appprefs.edit();
				e.putBoolean("tagLabel", true);
				e.commit();
				Log.d("tagLabel ^i isTrue ");

			}
			String extrasStr = "";
			Set<String> set = extras.keySet();
			for (String str : set) {
				Log.d( "extras:" + str
						+ ":" + extras.get(str).toString());
				extrasStr = "extras:" + str + ":" + extras.get(str).toString()
						+ "\n";

			}
			
			Log.d(extrasStr);


		}

		if (appprefs.getBoolean("tagLabel", false)
				&& "^^unseen-^iim".equals(extras.getString("tagLabel"))) {
			Log.d("tagLabel ^iim && ^i isTrue ");
			return START_NOT_STICKY; // FIXME: onStartCommand の戻り値はいったい？
		}

		// count の数から新着判定
		account = extras.getString("account");
		unreadcount = extras.getInt("count");

		SharedPreferences pref = getSharedPreferences(account, 0);
		int prev = pref.getInt("count", 0);
		Editor e = pref.edit();
		e.putInt("count", unreadcount);
		e.commit();


		if (prev < extras.getInt("count")) {

			String t = appprefs.getString(account + "_", "");

			if ("".equals(t)) {
				Log.d("sendAnnounce!");
				sendAnnounce(account, "Gmail Received. unread:" + unreadcount);
			} else {

				getFeed(t, unreadcount - prev);
			}

		}

		return retVal;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see android.app.Service#onDestroy()
	 */
	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d("onDestroy");
	}
	
	private void sendAnnounce(String title, String message){
		sendAnnounce("","",title,message);
	}

	private void sendAnnounce(String from, String email, String title, String message) {

		long time = System.currentTimeMillis();
		long sourceId = NotificationUtil.getSourceId(this,
				EXTENSION_SPECIFIC_ID);
		if (sourceId == NotificationUtil.INVALID_ID) {
			Log.e("Failed to insert data");
			return;
		}

		ContentValues eventValues = new ContentValues();
		eventValues.put(Notification.EventColumns.EVENT_READ_STATUS, false);
		
		eventValues.put(Notification.EventColumns.DISPLAY_NAME, from );
//		eventValues.put(Notification.EventColumns.CONTACTS_REFERENCE, 1);
		eventValues.put(Notification.EventColumns.TITLE,  title);
		eventValues.put(Notification.EventColumns.MESSAGE, message);
		eventValues.put(Notification.EventColumns.PERSONAL, 0);
		// eventValues.put(Notification.EventColumns.IMAGE_URI, icon);
//		eventValues.put(Notification.EventColumns.PROFILE_IMAGE_URI,
//				ExtensionUtils.getUriString(this,
//						R.drawable.widget_default_userpic_bg));
		eventValues.put(Notification.EventColumns.PUBLISHED_TIME, time);
		eventValues.put(Notification.EventColumns.SOURCE_ID, sourceId);
		

		try {
			getContentResolver().insert(Notification.Event.URI, eventValues);
		} catch (IllegalArgumentException e) {
			Log.e("Failed to insert event:¥n " + e);
		} catch (SecurityException e) {
			Log.e("Failed to insert event, is Live Ware Manager installed?:¥n "
					+ e);
		}
	}

	@Override
	protected void onViewEvent(Intent intent) {
		String action = intent
				.getStringExtra(Notification.Intents.EXTRA_ACTION);
		int eventId = intent.getIntExtra(Notification.Intents.EXTRA_EVENT_ID,
				-1);
		if (Notification.SourceColumns.ACTION_1.equals(action)) {
			doAction1(eventId);
		}
	}

	@Override
	protected void onRefreshRequest() {
		// Do nothing here, only relevant for polling extensions, this
		// extension is always up to date
	}

	/**
	 * Show toast with event information
	 * 
	 * @param eventId
	 *            The event id
	 */
	public void doAction1(int eventId) {
		Log.d("doAction1 event id: " + eventId);
		
		// PaydVersionConfirm
		Intent intent = new Intent(this, PaydVersionConfirm.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		this.startActivity(intent);
//		Cursor cursor = null;
//		try {
//			String name = "";
//			String message = "";
//			cursor = getContentResolver()
//					.query(Notification.Event.URI, null,
//							Notification.EventColumns._ID + " = " + eventId,
//							null, null);
//			if (cursor != null && cursor.moveToFirst()) {
//				int nameIndex = cursor
//						.getColumnIndex(Notification.EventColumns.DISPLAY_NAME);
//				int messageIndex = cursor
//						.getColumnIndex(Notification.EventColumns.MESSAGE);
//				name = cursor.getString(nameIndex);
//				message = cursor.getString(messageIndex);
//			}
//
//			String toastMessage = getText(R.string.action_event_1)
//					+ ", Event: " + eventId + ", Name: " + name + ", Message: "
//					+ message;
//			Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show();
//		} finally {
//			if (cursor != null) {
//				cursor.close();
//			}
//		}
	}

	/**
	 * Called when extension and sources has been successfully registered.
	 * Override this method to take action after a successful registration.
	 */
	@Override
	public void onRegisterResult(boolean result) {
		super.onRegisterResult(result);
		Log.d("onRegisterResult");

	}

	@Override
	protected RegistrationInformation getRegistrationInformation() {
		return new GmailRegistrationInformation(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sonyericsson.extras.liveware.aef.util.ExtensionService#
	 * keepRunningWhenConnected()
	 */
	@Override
	protected boolean keepRunningWhenConnected() {
		return false;
	}

	private void getFeed(String t, int count) {

		GmailFeed gf = new GmailFeed();
		gf.accessFeed(t, account);
		
		SharedPreferences pref = getSharedPreferences(account, 0);
		Editor e = pref.edit();
		unreadcount = gf.getUnreadcount();
		e.putInt("count", unreadcount);
		e.commit();

		List<GmailFeedBean> gfList = gf.getGfList();

		Log.d("count :" + count);
		if (gfList.size() > 0) {
			int j = (count > gfList.size() ? gfList.size() : count);

			for (int i = j - 1; i >= 0; i--) {
				GmailFeedBean gfs = gfList.get(i);

				SharedPreferences appprefs = PreferenceManager
						.getDefaultSharedPreferences(this);

				StringBuilder sbf = new StringBuilder();
				sbf.append((gfs.getName() == null ? gfs.getEmail() : gfs.getName()));
				
				 if ( gfs.getName() != null && !appprefs.getBoolean("hide_from", false)) {
						sbf.append((gfs.getEmail() == null ? "" : "<" + gfs.getEmail()
								+ ">"));
				 }
				
				
				StringBuilder sbs = new StringBuilder();
				sbs.append((gfs.getTitle() == null ? "" : "<"
						+ getString(R.string.subject) + ":" + gfs.getTitle() + ">")
						+ "\n");

				StringBuilder sbb = new StringBuilder();
				sbb.append((gfs.getSummary() == null ? "" : gfs.getSummary()));
				sbb.append("\nto:" + account + "\nGmail Received. unread:" + unreadcount);

				sendAnnounce(sbf.toString(), gfs.getEmail(), sbs.toString(), sbb.toString());
			}
		} else {
			sendAnnounce(account, "Gmail Received. unread:" + unreadcount + "\nRequest TimeOut.");
		}

	}




}
