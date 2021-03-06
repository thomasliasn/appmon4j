package de.is24.util.monitoring.visitors;

import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import org.apache.log4j.Logger;
import de.is24.util.monitoring.Counter;
import de.is24.util.monitoring.Historizable;
import de.is24.util.monitoring.HistorizableList;
import de.is24.util.monitoring.ReportVisitor;
import de.is24.util.monitoring.StateValueProvider;
import de.is24.util.monitoring.Timer;
import de.is24.util.monitoring.Version;


/**
 * @author oschmitz
 */
public abstract class UnsortedWriterReportVisitor implements ReportVisitor {
  private static final Logger LOGGER = Logger.getLogger(UnsortedWriterReportVisitor.class);
  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

  protected Writer writer;

  public UnsortedWriterReportVisitor(Writer writer) {
    this.writer = writer;
  }

  protected void writeStringToWriter(String toWrite) {
    try {
      writer.write(toWrite);
      writer.write("\n");
    } catch (IOException e) {
      LOGGER.error("hoppla", e);
    }
  }

  public void reportCounter(Counter counter) {
    LOGGER.debug("+++ entering UnsortedWriterReportVisitor.reportCounter +++");

    String result = counter.getName() + " counter :" + counter.getCount();
    writeStringToWriter(result);
  }

  /* (non-Javadoc)
   * @see de.is24.util.monitoring.ReportVisitor#reportTimer(de.is24.util.monitoring.Timer)
   */
  public void reportTimer(Timer timer) {
    LOGGER.debug("+++ entering UnsortedWriterReportVisitor.reportTimer +++");

    long count = timer.getCount();
    long timersum = timer.getTimerSum();
    String result = timer.getName() + " timer : count = " + count + ", timerSum = " + timersum + ", mean = " +
      ((double) timersum / count);
    writeStringToWriter(result);
  }

  /* (non-Javadoc)
   * @see de.is24.util.monitoring.ReportVisitor#reportStateValue(de.is24.util.monitoring.StateValueProvider)
   */
  public void reportStateValue(StateValueProvider stateValueProvider) {
    LOGGER.debug("+++ entering UnsortedWriterReportVisitor.reportStateValue +++");

    String result = stateValueProvider.getName() + " state : " + stateValueProvider.getValue();
    writeStringToWriter(result);
  }

  /* (non-Javadoc)
   * @see de.is24.util.monitoring.ReportVisitor#reportVersion(de.is24.util.monitoring.Version)
   */
  public void reportVersion(Version aVersion) {
    LOGGER.debug("+++ enter UnsortedWriterReportVisitor.reportVersion+++");

    String result = aVersion.getName() + " version : " + aVersion.getValue();
    writeStringToWriter(result);
  }

  /**
   *
   */
  public void reportHistorizableList(HistorizableList aHistorizableList) {
    LOGGER.debug("+++ entering UnsortedWriterReportVisitor.reportHistorizableList +++");

    StringBuffer result = new StringBuffer();
    result.append(aHistorizableList.getName()).append(" historizable :\n");
    for (Historizable historizable : aHistorizableList) {
      result.append(DATE_FORMAT.format(historizable.getTimestamp())).append(" ");
      result.append(historizable.getValue()).append("\n");
    }
    writeStringToWriter(result.toString());
  }
}
