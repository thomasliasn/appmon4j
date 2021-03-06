package de.is24.util.monitoring.state2graphite;

import de.is24.util.monitoring.InApplicationMonitor;
import de.is24.util.monitoring.Reportable;
import de.is24.util.monitoring.ReportableObserver;
import de.is24.util.monitoring.StateValueProvider;
import de.is24.util.monitoring.tools.LocalHostNameResolver;
import org.apache.log4j.Logger;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class StateValuesToGraphite implements ReportableObserver {
  private static final Logger LOGGER = Logger.getLogger(StateValuesToGraphite.class);
  private ScheduledExecutorService ex;
  private Map<String, StateValueProvider> stateValues;

  public StateValuesToGraphite(String graphiteHost, int graphitePort, String appName) {
    this(appName, new LocalHostNameResolver(), new GraphiteConnection(graphiteHost, graphitePort));
  }


  StateValuesToGraphite(String appName, LocalHostNameResolver localHostNameResolver,
                        GraphiteConnection graphiteClient) {
    String keyPrefix = appName + "." + localHostNameResolver.getLocalHostName() + ".states";
    stateValues = new ConcurrentHashMap<String, StateValueProvider>();
    InApplicationMonitor.getInstance().addReportableObserver(this);
    ex = Executors.newSingleThreadScheduledExecutor();
    ex.scheduleAtFixedRate(new ReportStateValuesJob(graphiteClient, keyPrefix), 1, 10, TimeUnit.SECONDS);
  }


  @Override
  public void addNewReportable(Reportable reportable) {
    if (reportable instanceof StateValueProvider) {
      stateValues.put(reportable.getName(), (StateValueProvider) reportable);
    }
  }


  public void shutdown() {
    InApplicationMonitor.getInstance().removeReportableObserver(this);
    ex.shutdown();
  }

  private class ReportStateValuesJob implements Runnable {
    private final GraphiteConnection graphiteClient;
    private final String keyPrefix;

    public ReportStateValuesJob(GraphiteConnection graphiteClient, String keyPrefix) {
      this.graphiteClient = graphiteClient;
      this.keyPrefix = keyPrefix;
    }

    @Override
    public void run() {
      LOGGER.debug("writing " + stateValues.size() + " state values to graphite");

      Long curTimeInSec = System.currentTimeMillis() / 1000;
      StringBuilder lines = new StringBuilder();
      for (StateValueProvider stateValueProvider : stateValues.values()) {
        lines.append(keyPrefix)
        .append(".")
        .append(stateValueProvider.getName())
        .append(" ")
        .append(stateValueProvider.getValue())
        .append(" ")
        .append(curTimeInSec)
        .append("\n");
      }
      graphiteClient.send(lines.toString());
    }

  }
}
