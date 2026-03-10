/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.wso2.am.integration.cucumbertests.utils.listeners;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestListener;
import org.testng.ITestResult;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.testcontainers.APIMContainer;

/**
 * TestNG listener that appends APIM server logs to the failure exception message
 * so they appear in Surefire reports and CI test summaries.
 */
public class ApimLogsOnFailureListener implements ITestListener {

    private static final Logger logger = LoggerFactory.getLogger(ApimLogsOnFailureListener.class);

    private static final int MAX_LOG_LINES = 500;

    @Override
    public void onTestFailure(ITestResult result) {
        Throwable throwable = result.getThrowable();
        if (throwable == null) {
            return;
        }
        if (!TestContext.contains("apimContainer")) {
            return;
        }
        try {
            Object container = TestContext.get("apimContainer");
            if (!(container instanceof APIMContainer)) {
                return;
            }
            APIMContainer apimContainer = (APIMContainer) container;
            if (!apimContainer.isRunning()) {
                return;
            }
            String logs = apimContainer.getLogs();
            if (logs == null || logs.isEmpty()) {
                return;
            }
            String truncated = truncateToLastLines(logs, MAX_LOG_LINES);
            String originalMessage = throwable.getMessage() != null ? throwable.getMessage() : "";
            String augmentedMessage = originalMessage
                    + "\n\n--- APIM server logs (last " + Math.min(MAX_LOG_LINES, logs.split("\n").length) + " lines) ---\n"
                    + truncated;

            Throwable wrapper = new Throwable(augmentedMessage, throwable);
            wrapper.setStackTrace(throwable.getStackTrace());
            result.setThrowable(wrapper);
            logger.info("Appended APIM server logs to failure for test: {}", result.getName());
        } catch (Exception e) {
            logger.warn("Could not append APIM server logs to failure: {}", e.getMessage());
        }
    }

    private static String truncateToLastLines(String logs, int maxLines) {
        String[] lines = logs.split("\n");
        if (lines.length <= maxLines) {
            return logs;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("... (showing last ").append(maxLines).append(" of ").append(lines.length).append(" lines)\n\n");
        int start = lines.length - maxLines;
        for (int i = start; i < lines.length; i++) {
            sb.append(lines[i]).append("\n");
        }
        return sb.toString();
    }
}
