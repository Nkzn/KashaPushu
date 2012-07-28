package net.nkzn.android.app.kashapushu;

import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.text.Editable;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends FragmentActivity implements OnClickListener,
		KashaPushuLoaderCallbacks {

	private static final int LOADER_ID_SCREEN_NAME = "screen_name".hashCode();

	private static final int LOADER_ID_UPDATE_STATUS = "update_status"
			.hashCode();

	private static final int REQUEST_CODE_OAUTH = 0;

	private static final int REQUEST_CODE_ZXING = 1;

	/**
	 * {@link Twitter}インスタンス作成用
	 */
	private Configuration mConf;

	/**
	 * {@link OAuthActivity}用のコールバック先
	 */
	private final String CALLBACK = "http://www.nkzn.net/callbacks/kashapushu";

	/**
	 * {@link OAuthActivity}用のconsumer key
	 */
	private final String CONSUMER_KEY = KEY.CONSUMER_KEY;

	/**
	 * {@link OAuthActivity}用のconsumer secret
	 */
	private final String CONSUMER_SECRET = KEY.CONSUMER_SECRET;

	/**
	 * Twitterのアクセストークン
	 */
	private String mAccessToken;

	/**
	 * Twitterのシークレットトークン
	 */
	private String mAccessTokenSecret;

	private Twitter mTwitter;

	/**
	 * 連打(連リクエスト)防止
	 */
	private boolean isLoading = false;

	/**
	 * Twitterアカウント名読み込み用コールバック
	 */
	private ScreenNameLoaderCallbacks mScreenNameLoaderCallbacks;

	/**
	 * ツイート投稿用コールバック
	 */
	private UpdateStatusCallbacks mUpdateStatusCallbacks;

	private SoundPool sp;

	private int soundId;

	private int stream_id;

	private static final String PRODUCT_NAME = "product_name";

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		mScreenNameLoaderCallbacks = new ScreenNameLoaderCallbacks(this);
		mUpdateStatusCallbacks = new UpdateStatusCallbacks(this);

		ImageButton ibKashaPushu = (ImageButton) findViewById(R.id.ib_kashapushu);
		ibKashaPushu.setOnClickListener(this);

		Button btnLoadFromBarcode = (Button) findViewById(R.id.btn_load_from_barcode);
		btnLoadFromBarcode.setOnClickListener(this);

		if (!loadAccessToken()) {
			Toast.makeText(this, R.string.plz_auth, Toast.LENGTH_LONG).show();
			initScreenName(R.string.un_logged_in);
		} else {
			createTwitterInstance();
			initScreenName(R.string.check_account);
			getSupportLoaderManager().initLoader(LOADER_ID_SCREEN_NAME, null,
					mScreenNameLoaderCallbacks);
		}

		sp = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
		soundId = sp.load(getApplicationContext(), R.raw.kasha_pushu, 1);
		// int soundId = sp.load("", 0);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case R.id.menu_auth:
			Intent intent = new Intent(this, OAuthActivity.class);
			intent.putExtra(OAuthActivity.CALLBACK, CALLBACK);
			intent.putExtra(OAuthActivity.CONSUMER_KEY, CONSUMER_KEY);
			intent.putExtra(OAuthActivity.CONSUMER_SECRET, CONSUMER_SECRET);
			startActivityForResult(intent, REQUEST_CODE_OAUTH);
			break;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_CODE_OAUTH && resultCode == RESULT_OK) {
			// long userId = data.getLongExtra(OAuthActivity.USER_ID, 0);
			String screenName = data.getStringExtra(OAuthActivity.SCREEN_NAME);
			mAccessToken = data.getStringExtra(OAuthActivity.TOKEN);
			mAccessTokenSecret = data
					.getStringExtra(OAuthActivity.TOKEN_SECRET);

			createTwitterInstance();
			initScreenName(screenName);

			saveAccessToken(mAccessToken, mAccessTokenSecret);
		} else if (requestCode == REQUEST_CODE_ZXING && resultCode == RESULT_OK) {
			String contents = data.getStringExtra("SCAN_RESULT");
			// String format = data.getStringExtra("SCAN_RESULT_FORMAT");
			// Toast.makeText(this, "contents: " + contents + "\nformat: " +
			// format, Toast.LENGTH_LONG).show();

			String productName = loadProductNameFromBarcode(contents);
			if (productName != null) {
				tweet(productName);
			} else {
				Toast.makeText(this, R.string.product_not_found,
						Toast.LENGTH_LONG).show();
			}
		}
	}

	@Override
	public void onClick(View v) {

		switch (v.getId()) {
		case R.id.ib_kashapushu:
			doKashaPushu();
			break;
		case R.id.btn_load_from_barcode:
			loadFromBarcode();
			break;
		}
	}

	@Override
	protected void onDestroy() {
		sp.release();
		super.onDestroy();
	}

	private void createTwitterInstance() {
		ConfigurationBuilder cbuilder = new ConfigurationBuilder();
		cbuilder.setOAuthConsumerKey(CONSUMER_KEY);
		cbuilder.setOAuthConsumerSecret(CONSUMER_SECRET);
		cbuilder.setOAuthAccessToken(mAccessToken);
		cbuilder.setOAuthAccessTokenSecret(mAccessTokenSecret);
		mConf = cbuilder.build();
		TwitterFactory twitterFactory = new TwitterFactory(mConf);
		mTwitter = twitterFactory.getInstance();
	}

	private void initScreenName(String screenName) {
		TextView tvScreenName = (TextView) findViewById(R.id.tv_screenname);
		tvScreenName.setText(screenName == null ? "" : screenName);
	}

	private void initScreenName(int resId) {
		String message = getString(resId);
		initScreenName(message);
	}

	private boolean loadAccessToken() {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(this);
		String token = pref.getString(OAuthActivity.TOKEN, "default");
		String tokenSecret = pref.getString(OAuthActivity.TOKEN_SECRET,
				"default");

		if (token.equals("default") || tokenSecret.equals("default")) {
			return false;
		} else {
			mAccessToken = token;
			mAccessTokenSecret = tokenSecret;
			return true;
		}

	}

	private void saveAccessToken(String accessToken, String accessTokenSecret) {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(this);
		Editor edit = pref.edit();
		edit.putString(OAuthActivity.TOKEN, accessToken);
		edit.putString(OAuthActivity.TOKEN_SECRET, accessTokenSecret);
		edit.commit();
	}

	private void doKashaPushu() {
		if (mTwitter == null) {
			Toast.makeText(this, R.string.plz_auth, Toast.LENGTH_LONG).show();
		} else if (!isLoading) {
			EditText etWhat = (EditText) findViewById(R.id.et_what_are_you_drink);
			Editable text = etWhat.getText();

			createTwitterInstance();

			tweet(text == null ? null : text.toString());

			isLoading = true;
		}
	}

	private void tweet(String productName) {
		Bundle args = new Bundle();

		if (productName != null) {
			args.putString(PRODUCT_NAME, productName);
		}

		getSupportLoaderManager().initLoader(LOADER_ID_UPDATE_STATUS, args,
				mUpdateStatusCallbacks);
	}

	private void loadFromBarcode() {
		Intent intent = new Intent("com.google.zxing.client.android.SCAN");
		intent.setPackage("com.google.zxing.client.android");
		intent.putExtra("SCAN_MODE", "EAN_13");
		try {
			startActivityForResult(intent, REQUEST_CODE_ZXING);
		} catch (ActivityNotFoundException e) {
			showStoreDialog();
		}
	}

	private void showStoreDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.dialog_connect_to_play_store_title);
		builder.setMessage(R.string.dialog_connect_to_play_store_message);
		builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Uri uri = Uri
						.parse("market://details?id=com.google.zxing.client.android");
				Intent intent = new Intent(Intent.ACTION_VIEW, uri);
				startActivity(intent);
			}
		});

		builder.show();
	}

	private String loadProductNameFromBarcode(String barcode) {
		return null;
	}

	@Override
	public Loader<String> createScreenNameLoader(int id, Bundle args) {
		return new AsyncScreenNameLoader(this, mTwitter);
	}

	@Override
	public void onScreenNameLoadFinished(Loader<String> loader, String data) {
		initScreenName(data);

		getSupportLoaderManager().destroyLoader(loader.getId());
	}

	@Override
	public void onScreenNameLoaderReset(Loader<String> loader) {
	}

	@Override
	public Loader<Status> onCreateUpdateStatus(int id, Bundle args) {
		String productName = args.getString(PRODUCT_NAME);

		stream_id = sp.play(soundId, 1.0F, 1.0F, 0, 0, 1.0F);

		return TextUtils.isEmpty(productName) ? new AsyncUpdateStatus(this,
				mTwitter) : new AsyncUpdateStatus(this, mTwitter, productName);
	}

	@Override
	public void onUpdateStatusFinished(Loader<Status> loader, Status data) {
		int message = 0;

		if (data == null) {
			message = R.string.post_failed;
		} else {
			message = R.string.post_succeed;

			sp.stop(stream_id);
		}
		Toast.makeText(this, message, Toast.LENGTH_LONG).show();

		isLoading = false;

		((EditText) findViewById(R.id.et_what_are_you_drink)).setText("");

		getSupportLoaderManager().destroyLoader(loader.getId());
	}

	@Override
	public void onUpdateStatusReset(Loader<Status> loader) {
	}

	class ScreenNameLoaderCallbacks implements LoaderCallbacks<String> {

		KashaPushuLoaderCallbacks mCallbacks;

		public ScreenNameLoaderCallbacks(KashaPushuLoaderCallbacks callbacks) {
			mCallbacks = callbacks;
		}

		@Override
		public Loader<String> onCreateLoader(int id, Bundle args) {
			return mCallbacks.createScreenNameLoader(id, args);
		}

		@Override
		public void onLoadFinished(Loader<String> loader, String data) {
			mCallbacks.onScreenNameLoadFinished(loader, data);
		}

		@Override
		public void onLoaderReset(Loader<String> loader) {
			mCallbacks.onScreenNameLoaderReset(loader);
		}

	}

	class UpdateStatusCallbacks implements LoaderCallbacks<Status> {

		KashaPushuLoaderCallbacks mCallbacks;

		public UpdateStatusCallbacks(KashaPushuLoaderCallbacks callbacks) {
			mCallbacks = callbacks;
		}

		@Override
		public Loader<Status> onCreateLoader(int id, Bundle args) {
			return mCallbacks.onCreateUpdateStatus(id, args);
		}

		@Override
		public void onLoadFinished(Loader<Status> loader, Status data) {
			mCallbacks.onUpdateStatusFinished(loader, data);
		}

		@Override
		public void onLoaderReset(Loader<Status> loader) {
			mCallbacks.onUpdateStatusReset(loader);
		}

	}
}