package io.jaegertracing.qe.spansreporter;

import java.net.InetAddress;
import java.net.UnknownHostException;

import lombok.AccessLevel;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Utils {

    public static String getHostname() {
        InetAddress inetAddress;
        try {
            inetAddress = InetAddress.getLocalHost();
            return inetAddress.getHostName();
        } catch (UnknownHostException ex) {
            logger.error("Exception,", ex);
        }
        return null;
    }
}
