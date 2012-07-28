package net.nkzn.android.app.kashapushu;

import java.io.FileDescriptor;
import java.io.PrintWriter;

import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import android.text.TextUtils;
import android.util.Log;

public class AsyncUpdateStatus extends AsyncTaskLoader<Status> {

//	private static final String tag = AsyncUpdateStatus.class.getSimpleName();
	
	Twitter mTwitter;
	
	String mDrinkName;
	
	Status result;
	
	public AsyncUpdateStatus(Context context, Twitter twitter) {
		super(context);
		mTwitter = twitter;
	}
	
	public AsyncUpdateStatus(Context context, Twitter twitter, String drinkName) {
		this(context, twitter);
		mDrinkName = drinkName;
	}

	@Override
	public Status loadInBackground() {
		
		StringBuilder message = new StringBuilder();
		message.append(getContext().getString(R.string.fizz));
		
		if(!TextUtils.isEmpty(mDrinkName)) {
			message.append(" (" + mDrinkName + ")");
		}
		
		StatusUpdate statusUpdate = new StatusUpdate(message.toString());
		try {
			return mTwitter.updateStatus(statusUpdate);
		} catch (TwitterException e) {
			e.printStackTrace();
			Log.w("", e);
		}
		return null;
	}

	@Override
	public void deliverResult(Status data) {
		if (isReset()) {
			if (this.result != null) {
				this.result = null;
			}
			return;
		}

		this.result = data;

		if (isStarted()) {
			super.deliverResult(data);
		}
	}
	
	@Override
	protected void onStartLoading() {
		if (this.result != null) {
			deliverResult(this.result);
		}
		if (takeContentChanged() || this.result == null) {
			forceLoad();
		}
	}

	@Override
	protected void onStopLoading() {
		super.onStopLoading();
		cancelLoad();
	}

	@Override
	protected void onReset() {
		super.onReset();
		onStopLoading();
	}
	
	@Override
	public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
	    super.dump(prefix, fd, writer, args);
	    writer.print(prefix); writer.print("result="); writer.println(this.result);
	}
}
