package com.tempfire.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by michael on 2018/2/5.
 *
 */
public class TimeUtil {

    public static String GetTodayTime() {
        Date date = new Date();//获取当前时间
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        String time = format.format(calendar.getTime());
        return time;
    }
    public static String addTime(int time) {
        Calendar ca = Calendar.getInstance();
        ca.add(Calendar.DATE,time);
        int year = ca.get(Calendar.YEAR);
        int month=ca.get(Calendar.MONTH)+1;
        int day=ca.get(Calendar.DATE);
        return new String(year+"-"+month+"-"+day);
    }
    public static int getDay(){
        Calendar ca = Calendar.getInstance();
        int day=ca.get(Calendar.DAY_OF_YEAR)+1;
        return day;
    }
    public static int getYear(){
        Calendar ca = Calendar.getInstance();
        int year = ca.get(Calendar.YEAR);
        return year;
    }
    public static int getTenDay(){
        Calendar ca = Calendar.getInstance();
        int day = ca.get(Calendar.DAY_OF_YEAR) + 1;
        return day/10;
    }
    public static String getNowTime(){
        Date date = new Date();
        DateFormat format=new SimpleDateFormat("HH-mm-ss");
        String time=format.format(date);
        return time;
    }

    public static void main(String[] args) {
        System.out.println(getNowTime());
    }
}
