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

public class PerformanceTestExecutor {

    private static final String PERFCAKE_COMMAND = "../perfcake-7.5/bin/perfcake.sh -s http -Dtarget_endpoint=%s";
    private static final long ASYNC_TIMEOUT = 30 * 1000;

    public static void main(String[] args) throws IOException, InterruptedException {
        Properties config = loadProperties(System.getProperty("config.file"));

//        ProcessBuilder pb = new ProcessBuilder("../perfcake-7.5/bin/perfcake.sh", "-s", "http", "-Dtarget_endpoint=http://localhost:8080/api/order");
//        pb.inheritIO();
//        pb.directory(new File("../perfcake-7.5"));
//        System.out.println(pb.directory().getAbsolutePath());
//        Process process = pb.start();
//        process.waitFor();

        if (checkAsyncResult(config)) {
            System.out.println("Performance test executed successfully");
        } else {
            System.out.println("Performance test failure");
        }

    }

    private static boolean checkAsyncResult(Properties config) {
        ResteasyClient resteasyClient = (ResteasyClient) ResteasyClientBuilder.newClient();

        WebTarget orderGetTarget = resteasyClient.target(UriBuilder.fromUri(config.getProperty("orderservice.get")));

        Timer timer = new Timer();

        timer.scheduleAtFixedRate(new TimerTask() {
            private long startTime = System.currentTimeMillis();

            @Override
            public void run() {
                if (System.currentTimeMillis() - startTime > ASYNC_TIMEOUT) {
                    cancel();
                } else {
                    int ordersCount = getObjectList(orderGetTarget).size();
                    System.out.println(ordersCount);
                }
            }
        }, 0, 1000);

        return true;
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
