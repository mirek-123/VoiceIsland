package com.mirecek.siriisland;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;

/** Periodic watchdog that re-enables the accessibility service if HyperOS turned it off. */
public final class KeepAliveJob extends JobService {
    private static final int JOB_ID = 2701;
    private static final long PERIOD_MS = 15 * 60 * 1000L;

    static void schedule(Context c) {
        JobScheduler js = c.getSystemService(JobScheduler.class);
        if (js.getPendingJob(JOB_ID) != null) return;
        js.schedule(new JobInfo.Builder(JOB_ID, new ComponentName(c, KeepAliveJob.class))
                .setPeriodic(PERIOD_MS)
                .setPersisted(true)
                .build());
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        AccessibilityToggle.ensureRunning(this);
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }
}
