package jp.ddo.dekuyou.liveware.extension.gmail;

import java.net.Authenticator;
import java.net.PasswordAuthentication;

import jp.ddo.dekuyou.android.util.Log;

public class BasicAuthenticator extends Authenticator {

	private String username;
	private String password;
	private int count = 0;

	public BasicAuthenticator(String username, String password) {
		this.username = username;
		this.password = password;
	}

	protected PasswordAuthentication getPasswordAuthentication() {
		Log.d("BasicAuthenticator : " + count++);
		if(count > 1){
			username = "";
		}

		return new PasswordAuthentication(username, password.toCharArray());
	}
}