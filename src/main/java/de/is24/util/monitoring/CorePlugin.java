package de.is24.util.monitoring;

import org.apache.log4j.Logger;
import java.util.Vector;


/**
 * This plugin represents the former core functionality of InApplicationMonitor, on a way to a more
 * flexible implementation by plugins, to simplify testing and the first step on the way fo a more
 * dependency injection friendly implementation.
 *
 * This plugin will take over some functionality that only makes sense in the context of a plugin that stores
 * data locally in the JVM. Other plugins (namely the statsd plugin) move data aggregation out of the JVM.
 * And thus it makes no sense to let them implement some of the patterns like reportableObserver etc.
 */
public class CorePlugin extends AbstractMonitorPlugin {
  private static Logger LOGGER = Logger.getLogger(CorePlugin.class);
  protected volatile boolean monitorActive = true;
  private volatile int maxHistoryEntriesToKeep = 5;
  private final Vector<ReportableObserver> reportableObservers = new Vector<ReportableObserver>();
  private final Monitors<Counter> countersTimers = new Monitors<Counter>(reportableObservers);
  private final Monitors<StateValueProvider> stateValues = new Monitors<StateValueProvider>(reportableObservers);
  private final Monitors<Version> versions = new Monitors<Version>(reportableObservers);
  private final Monitors<HistorizableList> historizableLists = new Monitors<HistorizableList>(reportableObservers);

  @Override
  public String getUniqueName() {
    return "CorePlugin";
  }


  /**
  * @return Number of entries to keep for each Historizable list.
  */
  public int getMaxHistoryEntriesToKeep() {
    return maxHistoryEntriesToKeep;
  }

  /**
   * Set the Number of entries to keep for each Historizable list.
   * Default is 5.
   *
   * @param aMaxHistoryEntriesToKeep Number of entries to keep
   */
  public void setMaxHistoryEntriesToKeep(int aMaxHistoryEntriesToKeep) {
    maxHistoryEntriesToKeep = aMaxHistoryEntriesToKeep;
  }

  /**
   * adds a new ReportableObserver that wants to be notified about new Reportables that are
   * registered on the InApplicationMonitor
   * @param reportableObserver the class that wants to be notified
   */
  public void addReportableObserver(final ReportableObserver reportableObserver) {
    reportableObservers.add(reportableObserver);

    LOGGER.info("registering new ReportableObserver (" + reportableObserver.getClass().getName() + ")");

    // iterate over all reportables that are registered already and call the observer for each one of them
    reportInto(new ReportVisitor() {
        public void notifyReportableObserver(Reportable reportable) {
          reportableObserver.addNewReportable(reportable);

        }

        @Override
        public void reportCounter(Counter counter) {
          notifyReportableObserver(counter);
        }

        @Override
        public void reportTimer(Timer timer) {
          notifyReportableObserver(timer);
        }

        @Override
        public void reportStateValue(StateValueProvider stateValueProvider) {
          notifyReportableObserver(stateValueProvider);
        }

        @Override
        public void reportHistorizableList(HistorizableList historizableList) {
          notifyReportableObserver(historizableList);
        }

        @Override
        public void reportVersion(Version version) {
          notifyReportableObserver(version);
        }

      });
  }

  private void notifyReportableObservers(Reportable reportable) {
    for (ReportableObserver reportableObserver : reportableObservers) {
      reportableObserver.addNewReportable(reportable);
    }
  }

  /**
   * Allow disconnection of observers, mainly for testing
   *
   * @param reportableObserver
   */
  public void removeReportableObserver(final ReportableObserver reportableObserver) {
    reportableObservers.remove(reportableObserver);
  }

  /**
   * Implements the {@link de.is24.util.monitoring.InApplicationMonitor} side of the Visitor pattern.
   * Iterates through all registered {@link de.is24.util.monitoring.Reportable} instances and calls
   * the corresponding method on the {@link de.is24.util.monitoring.ReportVisitor} implementation.
   * @param reportVisitor The {@link de.is24.util.monitoring.ReportVisitor} instance that shall be visited
   * by all regieteres {@link de.is24.util.monitoring.Reportable} instances.
   */
  public void reportInto(ReportVisitor reportVisitor) {
    countersTimers.accept(reportVisitor);
    stateValues.accept(reportVisitor);
    versions.accept(reportVisitor);
    historizableLists.accept(reportVisitor);
  }

  /**
  * <p>Increase the specified counter by a variable amount.</p>
  *
  * @param   name
  *          the name of the {@code Counter} to increase
  * @param   increment
  *          the added to add
  */
  public void incrementCounter(String name, int increment) {
    incrementInternalCounter(increment, name);
  }

  @Override
  public void incrementHighRateCounter(String name, int increment) {
    incrementInternalCounter(increment, name);
  }

  private void incrementInternalCounter(int increment, String name) {
    getCounter(name).increment(increment);
  }

  /**
  * Initialization of a counter.
  * @param name the name of the counter to be initialized
  */
  public void initializeCounter(String name) {
    getCounter(name).initialize();
  }

  /**
   * Add a timer measurement for the given name.
   * {@link de.is24.util.monitoring.Timer}s allow adding timer measurements, implicitly incrementing the count
   * Timers count and measure timed events.
   * The application decides which unit to use for timing.
   * Miliseconds are suggested and some {@link de.is24.util.monitoring.ReportVisitor} implementations
   * may imply this.
   *
   * @param name name of the {@link de.is24.util.monitoring.Timer}
   * @param timing number of elapsed time units for a single measurement
   */
  public void addTimerMeasurement(String name, long timing) {
    getTimer(name).addMeasurement(timing);
  }

  /**
   * Add a timer measurement for a rarely occuring event with given name.
   * This allows Plugins to to react on the estimated rate of the event.
   * Namely the statsd plugin will not sent these , as the requires storage
   * is in no relation to the value of the data.
   * {@link de.is24.util.monitoring.Timer}s allow adding timer measurements, implicitly incrementing the count
   * Timers count and measure timed events.
   * The application decides which unit to use for timing.
   * Miliseconds are suggested and some {@link de.is24.util.monitoring.ReportVisitor} implementations
   * may imply this.
   *
   * @param name name of the {@link de.is24.util.monitoring.Timer}
   * @param timing number of elapsed time units for a single measurement
   */
  public void addSingleEventTimerMeasurement(String name, long timing) {
    addTimerMeasurement(name, timing);
  }

  /**
   * Add a timer measurement for a rarely occuring event with given name.
   * This allows Plugins to to react on the estimated rate of the event.
   * Namely the statsd plugin will not sent these , as the requires storage
   * is in no relation to the value of the data.
   * {@link de.is24.util.monitoring.Timer}s allow adding timer measurements, implicitly incrementing the count
   * Timers count and measure timed events.
   * The application decides which unit to use for timing.
   * Miliseconds are suggested and some {@link de.is24.util.monitoring.ReportVisitor} implementations
   * may imply this.
   *
   * @param name name of the {@link de.is24.util.monitoring.Timer}
   * @param timing number of elapsed time units for a single measurement
   */
  public void addHighRateTimerMeasurement(String name, long timing) {
    addTimerMeasurement(name, timing);
  }


  /**
   * Initialization of a TimerMeasurement
   * @param name the name of the timer to be initialized
   */
  public void initializeTimerMeasurement(String name) {
    getTimer(name).initializeMeasurement();
  }

  /**
   * Add a state value provider to this appmon4j instance.
   * {@link de.is24.util.monitoring.StateValueProvider} instances allow access to a numeric
   * value (long), that is already available in the application.
   *
   * @param stateValueProvider the StateValueProvider instance to add
   */
  public void registerStateValue(String name, StateValueProvider stateValueProvider) {
    StateValueProvider oldProvider = stateValues.put(name, stateValueProvider);
    if (oldProvider != null) {
      LOGGER.warn("StateValueProvider [" + oldProvider + "] @" + stateValueProvider.getName() +
        " has been replaced by [" + stateValueProvider + "]!");
    }
    notifyReportableObservers(stateValueProvider);
  }

  /**
   * This method was intended to register module names with their
   * current version identifier.
   * This could / should actually be generalized into an non numeric
   * state value
   *
   * @param versionToAdd The Version Object to add
   */
  public void registerVersion(Version versionToAdd) {
    versions.put(versionToAdd.getName(), versionToAdd);
    notifyReportableObservers(versionToAdd);
  }

  /**
   * add a {@link de.is24.util.monitoring.Historizable} instance to the list identified by historizable.getName()
   *
   * @param historizable the historizable to add
   */
  public void addHistorizable(String name, Historizable historizable) {
    HistorizableList listToAddTo = getHistorizableList(name);
    listToAddTo.add(historizable);
  }

  /**
   * @param name the name of the StatsValueProvider
   * @return the StatsValueProvider
   */
  StateValueProvider getStateValue(String name) {
    return stateValues.get(name);
  }

  /**
   * internally used method to retrieve or create and register a named {@link de.is24.util.monitoring.Counter}.
   * @param name of the required {@link de.is24.util.monitoring.Counter}
   * @return {@link de.is24.util.monitoring.Counter} instance registered for the given name
   */
  Counter getCounter(final String name) {
    return countersTimers.get("counter." + name, new Monitors.Factory<Counter>() {
        @Override
        public Counter createMonitor() {
          return new Counter(name);
        }
      });
  }

  /**
   * internaly used method to retrieve or create and register a named {@link de.is24.util.monitoring.Timer}.
   * @param name of the required {@link de.is24.util.monitoring.Timer}
   * @return {@link de.is24.util.monitoring.Timer} instance registered for the given name
   */
  Timer getTimer(final String name) {
    return (Timer) countersTimers.get("timer." + name, new Monitors.Factory<Counter>() {
        @Override
        public Counter createMonitor() {
          return new Timer(name);
        }
      });
  }

  /**
   * internally used method to retrieve or create and register a named HistorizableList.
   * @param name of the required {@link de.is24.util.monitoring.HistorizableList}
   * @return {@link de.is24.util.monitoring.HistorizableList} instance registered for the given name
   */
  HistorizableList getHistorizableList(final String name) {
    return historizableLists.get(name, new Monitors.Factory<HistorizableList>() {
        @Override
        public HistorizableList createMonitor() {
          return new HistorizableList(name, maxHistoryEntriesToKeep);
        }
      });
  }
}
