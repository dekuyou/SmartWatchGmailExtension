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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.URL;

import jp.ddo.dekuyou.android.util.Log;
import jp.ddo.dekuyou.android.util.PaydVersionConfirm;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;

import com.sonyericsson.extras.liveware.extension.util.ExtensionUtils;
import com.sonyericsson.extras.liveware.extension.util.notification.NotificationUtil;

/**
 * The sample preference activity lets the user toggle start/stop of periodic
 * data insertion. It also allows the user to clear all events associated with
 * this extension.
 */
public class GmailPreferenceActivity extends PreferenceActivity {
	private static final String SHOW_SUMMARY = "show_summary";

	private static final String HIDE_FROM = "hide_from";

	private static final int DIALOG_CLEAR = 2;

	AccountManager mAccountManager;
	Account[] accounts;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.preferences);

		// addPreferencesFromResource(getResources().getIdentifier("preferences",
		// "xml", getPackageName()));

		PreferenceScreen ps = getPreferenceManager().createPreferenceScreen(
				this);

		mAccountManager = AccountManager.get(this);
		accounts = mAccountManager.getAccountsByType("com.google");

		for (Account account : accounts) {
			String name = account.name;
			String type = account.type;
			int describeContents = account.describeContents();
			int hashCode = account.hashCode();

			Log.d("name = " + name + "\ntype = " + type
					+ "\ndescribeContents = " + describeContents
					+ "\nhashCode = " + hashCode);

			EditTextPreference pf = new EditTextPreference(this);
			EditText ed = pf.getEditText();
			ed.setInputType(InputType.TYPE_CLASS_TEXT
					| InputType.TYPE_TEXT_VARIATION_PASSWORD);
			
			pf.setTitle(account.name);
			pf.setKey(account.name + "_");
			pf.setSummary(account.type);

			ps.addPreference(pf);

		}

		 CheckBoxPreference cp = new CheckBoxPreference(this);
		 cp.setTitle(getString(R.string.preference_option_hide_from));
		 cp.setKey(HIDE_FROM);
		 ps.addPreference(cp);
		 
			Preference pf = new Preference(this);
			pf.setTitle(getString(R.string.preference_option_summary));
			pf.setKey(SHOW_SUMMARY);

			pf.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				public boolean onPreferenceClick(Preference preference) {
					//
					return getPaydVersion();
				}
			});
			ps.addPreference(pf);

		// Handle clear all events
		Preference preference = new Preference(this);
		preference.setTitle(getString(R.string.preference_option_clear));
		preference.setKey(getString(R.string.preference_key_clear));
		preference
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {
					public boolean onPreferenceClick(Preference preference) {
						showDialog(DIALOG_CLEAR);
						return true;
					}
				});
		// Remove preferences that are not supported by the accessory
		if (!ExtensionUtils.supportsHistory(getIntent())) {
			preference = findPreference(getString(R.string.preference_key_clear));
			getPreferenceScreen().removePreference(preference);
		}
		ps.addPreference(preference);

		setPreferenceScreen(ps);

	}

	@Override
	protected void onResume() {
		super.onResume();
		getPreferenceScreen().getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(listener);
	}

	@Override
	protected void onPause() {
		super.onPause();
		getPreferenceScreen().getSharedPreferences()
				.unregisterOnSharedPreferenceChangeListener(listener);
	}

	Handler mHandler = new Handler();
	private SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {

		public void onSharedPreferenceChanged(
				SharedPreferences sharedPreferences, String key) {

			

			Log.d("onSharedPreferenceChangedExtended:" + key);
			
			if (SHOW_SUMMARY.equals(key)) {
				getPaydVersion();
				return;
			}else if(HIDE_FROM.equals(key)) {

				return;
			}else if(getString(R.string.preference_key_clear).equals(key)) {

				return;
			}else if( key.lastIndexOf("_")< 1 ){
				return;
			}
			
		    account = key.substring(0, key.lastIndexOf("_"));
			t = sharedPreferences.getString(key, "");

			if (!"".equals(t)) {

				runnable = new Runnable() {
					public void run() {
						showToast(accessFeed(t));
					}
				};
				mHandler.post(runnable);
			}
		}
	};
	
	private void showToast(String res) {
		
		mHandler.removeCallbacks(runnable);

		if ("".equals(res)) {
			Toast.makeText(this, account + ". Fail!", Toast.LENGTH_LONG).show();
		} else {
			Toast.makeText(this, account + ". SUCCESS!", Toast.LENGTH_LONG)
					.show();
		}
	}	
	
	String account = "";

	String t = "";
	private Runnable runnable;

	private String accessFeed(String t) {
		//
		HttpURLConnection c = null;
		String res = "";
		try {
			Authenticator.setDefault(new BasicAuthenticator(account, t));
			// HTTP https://mail.google.com/mail/feed/atom
			URL url = new URL("https://mail.google.com/mail/feed/atom");

			c = (HttpURLConnection) url.openConnection();
			c.setRequestMethod("GET");
			// c.setRequestProperty("Content-type", "application/atom+xml");
			c.setRequestProperty("Content-type", "text/xml; charset=UTF-8");
			// c.setRequestProperty("Authorization", "AuthSub token="+t);
			c.setInstanceFollowRedirects(true);
			c.setConnectTimeout(CONNECT_TIMEOUT);
			c.setReadTimeout(READ_TIMEOUT);
			Log.d("connect ：" + c.getURL());
			// c.setDoOutput(true);
			c.connect();

			Log.d(String.valueOf(c.getResponseCode()));
			Log.d(convertStreamToString(c.getErrorStream()));

			res = convertStreamToString(c.getInputStream());
			Log.d(res);
		} catch (Exception e) {
			e.printStackTrace();
			try {
				if (c != null)
					c.disconnect();
			} catch (Exception e2) {
				e2.printStackTrace();
			}
		} finally {
			c.disconnect();

		}
		return res;
	}

	public String convertStreamToString(InputStream is) throws IOException {
		/*
		 * To convert the InputStream to String we use the Reader.read(char[]
		 * buffer) method. We iterate until the Reader return -1 which means
		 * there's no more data to read. We use the StringWriter class to
		 * produce the string.
		 */
		if (is != null) {

			StringBuilder sb = new StringBuilder();

			try {
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(is, "UTF-8"));

				String line;
				while ((line = reader.readLine()) != null) {
					sb.append(line);
				}
			} finally {
				is.close();
			}
			return sb.toString();
		} else {
			return "";
		}
	}

	private static final int READ_TIMEOUT = 15 * 1000;

	private static final int CONNECT_TIMEOUT = 10 * 1000;



	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog = null;

		switch (id) {

		case DIALOG_CLEAR:
			dialog = createClearDialog();
			break;
		default:
			Log.w("Not a valid dialog id: " + id);
			break;
		}

		return dialog;
	}

	/**
	 * Create the Clear events dialog
	 * 
	 * @return the Dialog
	 */
	private Dialog createClearDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.preference_option_clear_txt)
				.setTitle(R.string.preference_option_clear)
				.setIcon(android.R.drawable.ic_input_delete)
				.setPositiveButton(android.R.string.yes,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								new ClearEventsTask().execute();
							}
						})
				.setNegativeButton(android.R.string.no,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
							}
						});
		return builder.create();
	}

	/**
	 * Clear all messaging events
	 */
	private class ClearEventsTask extends AsyncTask<Void, Void, Integer> {

		protected void onPreExecute() {
		}

		protected Integer doInBackground(Void... params) {
			int nbrDeleted = 0;
			nbrDeleted = NotificationUtil
					.deleteAllEvents(GmailPreferenceActivity.this);
			return nbrDeleted;
		}

		protected void onPostExecute(Integer id) {
			if (id != NotificationUtil.INVALID_ID) {
				Toast.makeText(GmailPreferenceActivity.this,
						R.string.clear_success, Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(GmailPreferenceActivity.this,
						R.string.clear_failure, Toast.LENGTH_SHORT).show();
			}
		}

	}
	private boolean getPaydVersion() {
		// PaydVersionConfirm
		Intent intent = new Intent(this, PaydVersionConfirm.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);


		return true;
	}
	
}
