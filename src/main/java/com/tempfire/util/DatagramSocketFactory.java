package com.tempfire.util;

import java.net.DatagramSocket;
import java.net.SocketException;

/**
 * Created by michael on 2018/2/4.
 *
 */
public class DatagramSocketFactory {

    private static final int PORT = 12345;//指定用来接收数据的端口

    private static class DatagramSocketHolder {

        private static DatagramSocket DATAGRAM_SOCKET = null;

        static {
            try {
                DATAGRAM_SOCKET = new DatagramSocket(PORT);
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }
    }

    public static DatagramSocket getInstance() {
        return DatagramSocketHolder.DATAGRAM_SOCKET;
    }
}
