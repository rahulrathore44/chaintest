package com.aventstack.chaintest.plugins;

import com.aventstack.chaintest.service.ChainPluginService;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;

public class ChainTestExecutionListener implements TestExecutionListener {

    public void testPlanExecutionFinished(final TestPlan testPlan) {
        ChainPluginService.INSTANCE.executionFinished();
    }

}
