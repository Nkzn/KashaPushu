package net.nkzn.android.app.kashapushu;

import twitter4j.Status;
import android.os.Bundle;
import android.support.v4.content.Loader;

public interface KashaPushuLoaderCallbacks {
	Loader<String> createScreenNameLoader(int id, Bundle args);
	void onScreenNameLoadFinished(Loader<String> loader, String data);
	void onScreenNameLoaderReset(Loader<String> loader);
	Loader<Status> onCreateUpdateStatus(int id, Bundle args);
	void onUpdateStatusFinished(Loader<Status> loader, Status data);
	void onUpdateStatusReset(Loader<Status> loader);
}
