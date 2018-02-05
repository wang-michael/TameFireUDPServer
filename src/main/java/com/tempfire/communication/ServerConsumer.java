package com.tempfire.communication;

import com.tempfire.model.SlicePictureData;
import com.tempfire.model.VoiceData;
import com.tempfire.util.TimeUtil;

import java.io.*;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by michael on 2018/2/4.
 * 每个客户端每次通话会产生一个音频文件和多个视频文件
 */
public class ServerConsumer implements Runnable  {

    private static final String SEPARATOR = File.separator;
    private static final String PROJECT_ROOTPATH = System.getProperty("user.dir");
    //保存客户端的SocketAddress
    private SocketAddress clientSocketAddress;
    private String projectId;
    //保存上次接收到的音频序号
    private int lastVoicePackageNum = -1;
    //设置队列最大长度防止内存溢出？？？
    public Queue<VoiceData> VOICE_RECEIVE_BUFFER = new ConcurrentLinkedQueue<>();

    //保存上次接收到的视频大包的序号
    private int lastPicturePackageNum = -1;
    //判断上次接收到的视频大包序号中的最后一个小包是否已经到达，如果到了将lastPicturePackageNumFlag置为true
    //之后不再接受当前大包对应的其它小包的数据
    private boolean lastPicturePackageNumFlag = false;
    public List<SlicePictureData> slicePictureList = new ArrayList<>(16);
    public Queue<List<SlicePictureData>> PICTURE_RECEIVE_BUFFER = new ConcurrentLinkedQueue<>();

    //projectId过长会不会导致文件夹名字太长不能生成？
    private String voiceDirPath;
    private String imgDirPath;

    public ServerConsumer(String projectId, SocketAddress clientSocketAddress) {
        super();
        this.projectId = projectId;
        this.clientSocketAddress = clientSocketAddress;
    }

    //只需要负责从VOICE_RECEIVE_BUFFER和PICTURE_RECEIVE_BUFFER中取数据存储到文件中即可
    public void run() {
        mkdirs();
        String voiceFilePath = voiceDirPath + TimeUtil.getNowTime();
        File voiceFile = new File(voiceFilePath);
        try (BufferedOutputStream voiceBos = new BufferedOutputStream(new FileOutputStream(voiceFile))) {
            VoiceData voiceData;
            List<SlicePictureData> slicePictureList;
            while (!Thread.interrupted()) {
                if ((voiceData = VOICE_RECEIVE_BUFFER.poll()) != null) {
                    voiceBos.write(voiceData.getData());
                }
                if ((slicePictureList = PICTURE_RECEIVE_BUFFER.poll()) != null) {
                    File imgFile = new File(imgDirPath + slicePictureList.get(0).getPackageNum()
                            + "-" + TimeUtil.getNowTime() + ".jpg");
                    try(BufferedOutputStream imgBos = new BufferedOutputStream(new FileOutputStream(imgFile))) {
                        for (SlicePictureData pictureData : slicePictureList) {
                            imgBos.write(pictureData.getData());
                        }
                        imgBos.flush();
                    }
                }
                //实际使用时不用手动刷新，线程结束时会自动关闭输出流
                voiceBos.flush();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getLastVoicePackageNum() {
        return lastVoicePackageNum;
    }

    public void setLastVoicePackageNum(int lastVoicePackageNum) {
        this.lastVoicePackageNum = lastVoicePackageNum;
    }

    public int getLastPicturePackageNum() {
        return lastPicturePackageNum;
    }

    public void setLastPicturePackageNum(int lastPicturePackageNum) {
        this.lastPicturePackageNum = lastPicturePackageNum;
    }

    public boolean getLastPicturePackageNumFlag() {
        return lastPicturePackageNumFlag;
    }

    public void setLastPicturePackageNumFlag(boolean lastPicturePackageNumFlag) {
        this.lastPicturePackageNumFlag = lastPicturePackageNumFlag;
    }

    private void mkdirs() {
        voiceDirPath = PROJECT_ROOTPATH + SEPARATOR + projectId + SEPARATOR +TimeUtil.GetTodayTime() + SEPARATOR + "voice" + SEPARATOR;
        imgDirPath = PROJECT_ROOTPATH + SEPARATOR + projectId + SEPARATOR + TimeUtil.GetTodayTime() + SEPARATOR + "img" + SEPARATOR;
        File voiceDir = new File(voiceDirPath);
        File imgDir = new File(imgDirPath);
        if (!voiceDir.exists())
            voiceDir.mkdirs();
        if (!imgDir.exists())
            imgDir.mkdirs();
    }
}
