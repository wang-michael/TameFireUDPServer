package com.tempfire.model;

/**
 * Created by michael on 2018/2/5.
 *
 */
public class VoiceData {

    //存储数据包中的发送时间
    private byte[] sendTime;

    private byte[] data;

    public VoiceData(byte[] sendTime, byte[] data) {
        this.sendTime = sendTime;
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public byte[] getSendTime() {
        return sendTime;
    }

    public void setSendTime(byte[] sendTime) {
        this.sendTime = sendTime;
    }
}
