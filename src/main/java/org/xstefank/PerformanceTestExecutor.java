package org.xstefank;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class PerformanceTestExecutor {

    private static final String PERFCAKE_COMMAND = "../perfcake-7.5/bin/perfcake.sh";
    private static final long ASYNC_TIMEOUT = 60 * 60 * 1000;
    private static final long ASYNC_DELAY = 2 * 60 * 1000;
    private static final long ASYNC_PERIOD = 5 * 1000;
    private static final long TEST_ORDER_COUNT = 10000;

    public static void main(String[] args) throws IOException, InterruptedException {
        Properties config = loadProperties(System.getProperty("config.file"));

        ProcessBuilder pb = new ProcessBuilder(PERFCAKE_COMMAND, "-s", "http",
                "-Dtarget_endpoint=" + config.getProperty("orderservice.post"));
        pb.inheritIO();
        Process process = pb.start();
        process.waitFor();

        checkAsyncResult(config);

    }

    private static void checkAsyncResult(Properties config) {
        System.out.println(String.format("Running async checks (delay, period, timeout) [%s, %s, %s]",
                getTime(ASYNC_DELAY), getTime(ASYNC_PERIOD), getTime(ASYNC_TIMEOUT)));

        ResteasyClient resteasyClient = (ResteasyClient) ResteasyClientBuilder.newClient();

        WebTarget orderGetTarget = resteasyClient.target(UriBuilder.fromUri(config.getProperty("orderservice.get")));

        Timer timer = new Timer();

        timer.scheduleAtFixedRate(new TimerTask() {
            private long startTime = System.currentTimeMillis();

            @Override
            public void run() {
                long timeMillis = System.currentTimeMillis() - startTime;
                int ordersCount = getObjectList(orderGetTarget).size();
                System.out.println(String.format("[%s] [%d completed] Checking async result...",
                        getTime(timeMillis), ordersCount));
                if (ordersCount >= TEST_ORDER_COUNT || timeMillis > ASYNC_TIMEOUT) {
                    System.out.println("Ending aynch checks...");
                    if (ordersCount >= TEST_ORDER_COUNT) {
                        System.out.println("Test executed successfully");
                    } else {
                        System.out.println("Test failure");
                    }
                    cancel();
                    System.exit(0);
                }
            }
        }, ASYNC_DELAY, ASYNC_PERIOD);
    }

    private static String getTime(long millis) {
        return String.format("%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(millis),
                TimeUnit.MILLISECONDS.toSeconds(millis) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
        );
    }

    @SuppressWarnings(value = "unchecked")
    private static List<Object> getObjectList(WebTarget orderGetTarget) {
        String resultJson = orderGetTarget.request().get(String.class);

        ObjectMapper mapper = new ObjectMapper();
        List<Object> result = null;
        try {
            result = mapper.readValue(resultJson, ArrayList.class);
        } catch (IOException e) {
            System.err.println("Cannot parse json result");
        }

        return result;
    }

    private static Properties loadProperties(String configFileName) {
        Properties properties = new Properties();

        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(configFileName);
        try {
            properties.load(is);
        } catch (IOException e) {
            System.err.println("Properties file not found - " + configFileName);
        }

        return properties;
    }
}
