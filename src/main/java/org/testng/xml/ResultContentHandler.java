package org.testng.xml;

import static org.testng.reporters.XMLReporterConfig.ATTR_DESC;
import static org.testng.reporters.XMLReporterConfig.ATTR_FINISHED_AT;
import static org.testng.reporters.XMLReporterConfig.ATTR_METHOD_SIG;
import static org.testng.reporters.XMLReporterConfig.ATTR_NAME;
import static org.testng.reporters.XMLReporterConfig.ATTR_STARTED_AT;
import static org.testng.reporters.XMLReporterConfig.ATTR_STATUS;
import static org.testng.reporters.XMLReporterConfig.TAG_CLASS;
import static org.testng.reporters.XMLReporterConfig.TAG_SUITE;
import static org.testng.reporters.XMLReporterConfig.TAG_TEST;
import static org.testng.reporters.XMLReporterConfig.TAG_TEST_METHOD;

import org.testng.ITestResult;
import org.testng.remote.strprotocol.GenericMessage;
import org.testng.remote.strprotocol.IRemoteSuiteListener;
import org.testng.remote.strprotocol.IRemoteTestListener;
import org.testng.remote.strprotocol.MessageHelper;
import org.testng.remote.strprotocol.SuiteMessage;
import org.testng.remote.strprotocol.TestMessage;
import org.testng.remote.strprotocol.TestResultMessage;
import org.testng.reporters.XMLReporterConfig;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

public class ResultContentHandler extends DefaultHandler {
  private int m_suiteMethodCount = 0;
  private int m_testMethodCount = 0;
  private SuiteMessage m_currentSuite;
  private TestMessage m_currentTest;
  private String m_className;
  private int m_passed;
  private int m_failed;
  private int m_skipped;
  private int m_invocationCount;
  private int m_currentInvocationCount;
  private TestResultMessage m_currentTestResult;
  private IRemoteSuiteListener m_suiteListener;
  private IRemoteTestListener m_testListener;

  public ResultContentHandler(IRemoteSuiteListener suiteListener,
      IRemoteTestListener testListener) {
    m_suiteListener = suiteListener;
    m_testListener = testListener;
  }

  @Override
  public void startElement (String uri, String localName,
      String qName, Attributes attributes) {
    p("Start " + qName);
    if (TAG_SUITE.equals(qName)) {
      m_suiteListener.onInitialization(new GenericMessage(MessageHelper.GENERIC_SUITE_COUNT));
      m_suiteMethodCount = 0;
      m_currentSuite = new SuiteMessage(attributes.getValue(ATTR_NAME),
          true /* start */, m_suiteMethodCount);
      m_suiteListener.onStart(m_currentSuite);
    } else if (TAG_TEST.equals(qName)) {
      m_passed = m_failed = m_skipped = 0;
      m_currentTest = new TestMessage(true /* start */, m_currentSuite.getSuiteName(),
          attributes.getValue(ATTR_NAME), m_testMethodCount,
          m_passed, m_failed, m_skipped, 0);
      m_testListener.onStart(m_currentTest);
    } else if (TAG_CLASS.equals(qName)) {
      m_className = attributes.getValue(ATTR_NAME);
    } else if (TAG_TEST_METHOD.equals(qName)) {
      Integer status = XMLReporterConfig.getStatus(attributes.getValue(ATTR_STATUS));
      m_currentTestResult = new TestResultMessage(status, m_currentSuite.getSuiteName(),
          m_currentTest.getTestName(), m_className, attributes.getValue(ATTR_NAME),
          attributes.getValue(ATTR_DESC), parseParameters(attributes.getValue(ATTR_METHOD_SIG)),
          XMLReporterConfig.convertDate(attributes.getValue(ATTR_STARTED_AT)),
          XMLReporterConfig.convertDate(attributes.getValue(ATTR_FINISHED_AT)),
          null /* stack trace, filled later */,
          m_invocationCount, m_currentInvocationCount);
      m_suiteMethodCount++;
      m_testMethodCount++;
      if (status == ITestResult.SUCCESS) m_passed++;
      else if (status == ITestResult.FAILURE) m_failed++;
      else if (status == ITestResult.SKIP) m_skipped++;
    }
  }

  private String[] parseParameters(String value) {
    return new String[] { "n:42" };
  }

  @Override
  public void endElement (String uri, String localName, String qName) {
    if (TAG_SUITE.equals(qName)) {
      m_suiteListener.onFinish(new SuiteMessage(null, false /* end */, m_suiteMethodCount));
      m_currentSuite = null;
    } else if (TAG_TEST.equals(qName)) {
      m_currentTest = new TestMessage(false /* start */, m_currentSuite.getSuiteName(),
          null, m_testMethodCount,
          m_passed, m_failed, m_skipped, 0);
      m_testMethodCount = 0;
      m_testListener.onFinish(m_currentTest);
    } else if (TAG_CLASS.equals(qName)) {
      m_className = null;
    } else if (TAG_TEST_METHOD.equals(qName)) {
      switch(m_currentTestResult.getResult()) {
      case ITestResult.SUCCESS:
        m_testListener.onTestSuccess(m_currentTestResult);
        break;
      case ITestResult.FAILURE:
        m_testListener.onTestFailure(m_currentTestResult);
        break;
      case ITestResult.SKIP:
        m_testListener.onTestSkipped(m_currentTestResult);
        break;
      default:
       p("Ignoring test status:" + m_currentTestResult.getResult());
      }
      
    }
  }

  private static void p(String string) {
    if (false) {
      System.out.println("[ResultContentHandler] " + string);
    }
  }
}
