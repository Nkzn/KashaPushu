package net.nkzn.android.app.kashapushu;

import java.io.FileDescriptor;
import java.io.PrintWriter;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

public class AsyncScreenNameLoader extends AsyncTaskLoader<String> {

	Twitter mTwitter;
	
	String result;
	
	public AsyncScreenNameLoader(Context context, Twitter twitter) {
		super(context);
		mTwitter = twitter;
	}

	@Override
	public String loadInBackground() {
		try {
			return mTwitter.getScreenName();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (TwitterException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public void deliverResult(String data) {
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
