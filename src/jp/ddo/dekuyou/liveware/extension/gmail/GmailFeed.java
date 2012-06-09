package jp.ddo.dekuyou.liveware.extension.gmail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import jp.ddo.dekuyou.android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.util.Xml;

public class GmailFeed {

	private static final int READ_TIMEOUT = 15 * 1000;

	private static final int CONNECT_TIMEOUT = 10 * 1000;
	
	private Integer unreadcount = 0;
	private String feed = "";
	private List<GmailFeedBean> gfList = new ArrayList<GmailFeedBean>(); 
	
	public List<GmailFeedBean> getGfList() {
		return gfList;
	}

	public void setGfList(List<GmailFeedBean> gfList) {
		this.gfList = gfList;
	}

	public Integer getUnreadcount() {
		return unreadcount;
	}

	public void setUnreadcount(Integer unreadcount) {
		this.unreadcount = unreadcount;
	}

	public String accessFeed(String t, String account) {
		//
		HttpURLConnection c = null;

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

			feed = convertStreamToString(c.getInputStream());
			Log.d(feed);
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
		
		gmailFeed2List(feed);


		return feed;
	}

	private String convertStreamToString(InputStream is) throws IOException {
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
	
	
	private List<GmailFeedBean> gmailFeed2List(String res) {

		GmailFeedBean gf = null;
		String pnameB = "";
		XmlPullParser xmlpp = Xml.newPullParser();
		try {
			xmlpp.setInput(new StringReader(res));
			while (xmlpp.next() != XmlPullParser.END_DOCUMENT) {
				Log.d("depth:" + xmlpp.getDepth() + ", eventType:"
						+ xmlpp.getEventType() + ", name:" + xmlpp.getName()
						+ ", text:" + xmlpp.getText());

				if (xmlpp.getEventType() == 4 && "fullcount".equals(pnameB)) {
					// 未読数の更新

					unreadcount = Integer.parseInt(xmlpp.getText());


				}

				if ("entry".equals(xmlpp.getName())) {
					if (xmlpp.getEventType() == 2) {
						gf = new GmailFeedBean();
					} else if (xmlpp.getEventType() == 3) {
						gfList.add(gf);
					}
				}
				if (xmlpp.getEventType() == 2) {
					pnameB = xmlpp.getName();
				}
				if (xmlpp.getEventType() == 4 && gf != null) {
					char lead = pnameB.charAt(0);
					String pname = Character.toUpperCase(lead)
							+ pnameB.substring(1);

					Method method;
					try {
						method = gf.getClass().getMethod("set" + pname,
								new Class[] { String.class });
					} catch (SecurityException e) {
						throw new RuntimeException(e);
					} catch (NoSuchMethodException e) {
						throw new RuntimeException(e);
					}

					try {
						method.invoke(gf, new Object[] { xmlpp.getText() });

					} catch (IllegalArgumentException e) {
						throw new RuntimeException(e);
					} catch (IllegalAccessException e) {
						throw new RuntimeException(e);
					} catch (InvocationTargetException e) {
						throw new RuntimeException(e);
					}

				}

			}
		} catch (XmlPullParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return gfList;
	}


}
