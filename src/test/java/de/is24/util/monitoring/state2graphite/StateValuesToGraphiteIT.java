package de.is24.util.monitoring.state2graphite;

import de.is24.util.monitoring.InApplicationMonitor;
import de.is24.util.monitoring.SimpleStateValueProvider;
import de.is24.util.monitoring.tools.LocalHostNameResolver;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class StateValuesToGraphiteIT {
  private GraphiteConnection graphiteConnection;
  private StateValuesToGraphite target;

  @Before
  public void setUp() throws Exception {
    LocalHostNameResolver localHostNameResolver = mock(LocalHostNameResolver.class);
    when(localHostNameResolver.getLocalHostName()).thenReturn("testHost");

    graphiteConnection = mock(GraphiteConnection.class);
    target = new StateValuesToGraphite("testAppName", localHostNameResolver, graphiteConnection);
  }

  @After
  public void tearDown() {
    target.shutdown();
  }

  @Test
  public void useAppNameHostNameAndStateAsPrefix() throws Exception {
    // wait for 2 seconds
    Thread.sleep(2000);
    verify(graphiteConnection, times(1)).send(startsWith("testAppName.testHost.states."));
  }

  @Test
  public void useGraphiteFormatting() throws Exception {
    InApplicationMonitor.getInstance().registerStateValue(new SimpleStateValueProvider("StateTest", 4711));

    // wait for 2 seconds
    Thread.sleep(2000);
    verify(graphiteConnection, times(1)).send(contains("testAppName.testHost.states.StateTest 4711 "));
  }

}
