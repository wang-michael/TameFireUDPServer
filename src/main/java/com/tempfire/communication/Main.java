package com.tempfire.communication;

/**
 * Created by michael on 2018/2/4.
 *
 */
public class Main {
    public static void main(String[] args) {
        new Thread(new ServerProducer()).start();
    }
}
