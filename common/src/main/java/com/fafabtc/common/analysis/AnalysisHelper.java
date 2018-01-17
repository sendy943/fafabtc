package com.fafabtc.common.analysis;

import android.content.Context;

import com.avos.avoscloud.feedback.FeedbackAgent;
import com.fafabtc.analysis.bugly.BuglyHelper;
import com.fafabtc.analysis.leancloud.LeanCloudHelper;

/**
 * Created by jastrelax on 2018/1/15.
 */

public class AnalysisHelper {
    public static void init(Context context) {
        BuglyHelper.init(context);
        LeanCloudHelper.init(context);
    }

    public static void startFeedBackActivity(Context context) {
        FeedbackAgent agent = new FeedbackAgent(context);
        agent.isContactEnabled(false);
        agent.startDefaultThreadActivity();
    }

    public static void sendFeedback(Context context, String feedback, final Runnable callback) {
        LeanCloudHelper.sendFeedback(context, feedback, callback);
    }

    public static void test() {
        LeanCloudHelper.testLeancloud();
    }
}
