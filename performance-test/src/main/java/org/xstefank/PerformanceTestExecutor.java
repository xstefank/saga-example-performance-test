package org.xstefank;

import java.io.File;
import java.io.IOException;

public class PerformanceTestExecutor {

    private static String command = "../perfcake-7.5/bin/perfcake.sh -s http -Dtarget_endpoint=http://localhost:8080/api/order";

    public static void main(String[] args) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("../perfcake-7.5/bin/perfcake.sh", "-s", "http", "-Dtarget_endpoint=http://localhost:8080/api/order");
        pb.inheritIO();
        pb.directory(new File("../perfcake-7.5"));
        System.out.println(pb.directory().getAbsolutePath());
        Process process = pb.start();
        process.waitFor();

    }
}
