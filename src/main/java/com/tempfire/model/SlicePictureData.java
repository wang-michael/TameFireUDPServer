package com.tempfire.model;

/**
 * Created by michael on 2018/2/4.
 * 存储分片后的图片数据
 */
public class SlicePictureData implements Comparable<SlicePictureData> {

    //存储图片大包的包号
    private int packageNum;

    //存储图片小包的包号
    private byte sliceNum;

    //存储具体的图片数据
    private byte[] data;

    //时间戳，存储数据的发送时间
    private byte[] sendTime;

    public SlicePictureData(byte sliceNum, byte[] data, int packageNum, byte[] sendTime) {
        this.sliceNum = sliceNum;
        this.data = data;
        this.packageNum = packageNum;
        this.sendTime = sendTime;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public byte getSliceNum() {
        return sliceNum;
    }

    public void setSliceNum(byte sliceNum) {
        this.sliceNum = sliceNum;
    }

    public int getPackageNum() {
        return packageNum;
    }

    public void setPackageNum(int packageNum) {
        this.packageNum = packageNum;
    }

    public byte[] getSendTime() {
        return sendTime;
    }

    public void setSendTime(byte[] sendTime) {
        this.sendTime = sendTime;
    }

    @Override
    public int compareTo(SlicePictureData o) {
        return this.sliceNum <= o.sliceNum ? -1 : 1;
    }

}
