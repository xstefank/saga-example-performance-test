package org.xstefank;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.UriBuilder;
import java.io.File;
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
    private static final long ASYNC_DELAY = 30 * 1000;
    private static final long ASYNC_PERIOD = 5 * 1000;
    private static final long TEST_ORDER_COUNT = 1000;

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
        System.out.println(String.format("Running async checks (delay, period, timeout) [%d, %d, %d]", ASYNC_DELAY, ASYNC_PERIOD, ASYNC_TIMEOUT));
        ResteasyClient resteasyClient = (ResteasyClient) ResteasyClientBuilder.newClient();

        WebTarget orderGetTarget = resteasyClient.target(UriBuilder.fromUri(config.getProperty("orderservice.get")));

        Timer timer = new Timer();

        timer.scheduleAtFixedRate(new TimerTask() {
            private long startTime = System.currentTimeMillis();

            @Override
            public void run() {
                long timeMillis = System.currentTimeMillis() - startTime;
                System.out.println(String.format("[%s] Checking async result...", getTime(timeMillis)));
                if (checkObjectCount(orderGetTarget) || timeMillis > ASYNC_TIMEOUT) {
                    System.out.println("Ending aynch checks...");
                    if (checkObjectCount(orderGetTarget)) {
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
        return String.format("%02d min, %02d sec",
                TimeUnit.MILLISECONDS.toMinutes(millis),
                TimeUnit.MILLISECONDS.toSeconds(millis) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
        );
    }

    private static boolean checkObjectCount(WebTarget target) {
        int ordersCount = getObjectList(target).size();

        return ordersCount >= TEST_ORDER_COUNT;
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
