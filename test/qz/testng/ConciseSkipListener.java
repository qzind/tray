package qz.testng;

import org.testng.ITestResult;
import org.testng.TestListenerAdapter;

@SuppressWarnings("unused")
public class ConciseSkipListener extends TestListenerAdapter {
    @Override
    public void onTestSkipped(ITestResult tr) {
        // Remove the stack trace so only the message is printed
        if (tr.getThrowable() != null) {
            tr.getThrowable().setStackTrace(new StackTraceElement[0]);
        }
    }
}