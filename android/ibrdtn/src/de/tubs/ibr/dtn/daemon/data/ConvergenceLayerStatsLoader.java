package de.tubs.ibr.dtn.daemon.data;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;
import de.tubs.ibr.dtn.stats.ConvergenceLayerStatsEntry;
import de.tubs.ibr.dtn.stats.StatsContentProvider;

@SuppressLint("SimpleDateFormat")
public class ConvergenceLayerStatsLoader extends AsyncTaskLoader<Cursor> {
    
    private static final String TAG = "ConvergenceLayerStatsLoader";
    
    private Boolean mStarted = false;
    private Cursor mData = null;
    private String mConvergenceLayer = null;

    public ConvergenceLayerStatsLoader(Context context, String convergencecayer) {
        super(context);
        mConvergenceLayer = convergencecayer;
        setUpdateThrottle(250);
    }
    
    @Override
    public void deliverResult(Cursor data) {
        if (isReset()) {
        	onReleaseResources(data);
        	data = null;
        }
        
        Cursor oldData = mData;
        mData = data;
        
        if (isStarted()) {
            super.deliverResult(data);
        }
        
        if (oldData != null && oldData != data) {
        	onReleaseResources(oldData);
        }
    }
    
    @Override
	public void onCanceled(Cursor data) {
		super.onCanceled(data);
		onReleaseResources(data);
	}

    @Override
    protected void onReset() {
    	super.onReset();
    	
        onStopLoading();
        
        onReleaseResources(mData);
        mData = null;
        
        if (mStarted) {
            // unregister from intent receiver
            getContext().unregisterReceiver(_receiver);
            mStarted = false;
        }
    }

    @Override
    protected void onStartLoading() {
        if (mData != null) {
            this.deliverResult(mData);
        }
        
        IntentFilter filter = new IntentFilter(StatsContentProvider.NOTIFY_DATABASE_UPDATED);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        getContext().registerReceiver(_receiver, filter);
        mStarted = true;
        
        if (this.takeContentChanged() || mData == null) {
            forceLoad();
        }
    }

    @Override
    protected void onStopLoading() {
        cancelLoad();
    }

    @Override
    public Cursor loadInBackground() {
        final DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        // generate a time limit (24 hours)
        Calendar cal = Calendar.getInstance();
        cal.roll(Calendar.DATE, -1);
        String timestamp_limit = formatter.format(cal.getTime());

        try {
            if (mConvergenceLayer == null) {
                // limit to specific download
            	
                // limit to specific download
            	return getContext().getContentResolver().query(
            			StatsContentProvider.CL_STATS_URI,
                        ConvergenceLayerStatsEntry.PROJECTION,
                        ConvergenceLayerStatsEntry.TIMESTAMP + " >= ?",
                        new String[] { timestamp_limit },
                        ConvergenceLayerStatsEntry.TIMESTAMP + " ASC");
            } else {
                // limit to specific download
            	return getContext().getContentResolver().query(
            			StatsContentProvider.CL_STATS_URI,
                        ConvergenceLayerStatsEntry.PROJECTION,
                        ConvergenceLayerStatsEntry.TIMESTAMP + " >= ? AND " + ConvergenceLayerStatsEntry.CONVERGENCE_LAYER + " = ?",
                        new String[] { timestamp_limit, mConvergenceLayer },
                        ConvergenceLayerStatsEntry.TIMESTAMP + " ASC");
            }
        } catch (Exception e) {
            Log.e(TAG, "loadInBackground() failed", e);
        }
        
        return null;
    }
    
    /**
     * Helper function to take care of releasing resources associated
     * with an actively loaded data set.
     */
    protected void onReleaseResources(Cursor data) {
    	if (data == null) return;
    	
        // For a simple List<> there is nothing to do.  For something
        // like a Cursor, we would close it here.
    	data.close();
    }

    private BroadcastReceiver _receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            onContentChanged();
        }
    };
}
