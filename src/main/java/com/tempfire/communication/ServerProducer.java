package com.tempfire.communication;

import com.tempfire.model.SlicePictureData;
import com.tempfire.model.VoiceData;
import com.tempfire.util.CustomerException;
import com.tempfire.util.DatagramSocketFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by michael on 2017/11/4.
 *
 */
public class ServerProducer implements Runnable {

    private ExecutorService consumerThreadPool = Executors.newCachedThreadPool();
    //key为projectId，value为对应的消费者线程
    private Map<String, ServerConsumerAndThread> consumerMap = new HashMap<>();
    //用来接收数据的DatagramSocket
    private DatagramSocket receiver = DatagramSocketFactory.getInstance();
    //指定用来接收UDP数据包的buffer的大小,单位为字节，图片数据最大为多少？
    private static final int UDPBUFFER_SIZE = 1024;
    private static final byte AND_BIT = 1;
    //保存序列号最大回绕差值(序列号范围1-65535)
    private static final int MAX_CYCLE_NUM = 50000;
    //包头长度43个字节
    private static final int HEADER_LENGTH = 43;

    public void run() {
        byte buf[] = new byte[UDPBUFFER_SIZE];
        DatagramPacket datapacket = new DatagramPacket(buf, buf.length);
        while (true) {
            try {
                receiver.receive(datapacket);
                analysisData(datapacket);
            } catch (IOException | CustomerException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void analysisData(DatagramPacket datapacket) throws CustomerException {
        //头部包含37个字节，总长度肯定大于等于37
        if (datapacket.getLength() < HEADER_LENGTH) {
            throw new CustomerException("301", "收到的datapacket长度不合法！");
        }
        byte[] data = datapacket.getData();

        //获取projectId
        int projectIdLen = 0;
        for (int i = 11; i < HEADER_LENGTH; i++) {
            if (data[i] == 0) {
                projectIdLen = i - 11;
                break;
            }
        }

        String projectId = new String(data, 11, projectIdLen);
        int packageNum = (data[1] & 0x00ff) * 256 + (data[2] & 0x00ff);
        int exactDataLen = (data[3] & 0x00ff) * 256 + (data[4] & 0x00ff);
        byte[] sendTime = new byte[6];
        System.arraycopy(data, 5, sendTime, 0, 6);

//        System.out.println("------------------------------------");
//        System.out.println("data[1]: " + data[1] + " data[2]: " + data[2] + " data[3]: " + data[3] +
//            " data[4]: " + data[4]);
//        System.out.println("收到数据，data长度为： " + data.length + " datapacket len: " + datapacket.getLength() + " 包号: " + packageNum + " 有效数据长度: " + exactDataLen
//                + " projectId: " + projectId + " sendTime: " + sendTime[0] + " " + sendTime[1]);
//        System.out.println("数据类型： " + ((data[0] >>> 6) & AND_BIT));

        byte[] exactData = new byte[exactDataLen];
        System.arraycopy(data, HEADER_LENGTH, exactData, 0, exactDataLen);

        //为每个5156客户端开启一个新的consumer线程进行处理
        ServerConsumerAndThread consumerAndThread = consumerMap.get(projectId);
        ServerConsumer consumer = null;
        if (consumerAndThread == null) {
            consumer = new ServerConsumer(projectId, datapacket.getSocketAddress());
            Thread thread = new Thread(consumer);
            consumerAndThread = new ServerConsumerAndThread(consumer, thread);
            consumerMap.put(projectId, consumerAndThread);
            consumerThreadPool.execute(thread);
        }
        if (consumer == null)
            consumer = consumerAndThread.getServerConsumer();


        if (((data[0] >>> 6) & AND_BIT) == 0) {//音频数据
            int lastVoicePackageNum = consumer.getLastVoicePackageNum();
            if (packageNum > lastVoicePackageNum ||
                    (packageNum < lastVoicePackageNum && lastVoicePackageNum - packageNum > MAX_CYCLE_NUM)) {
                VoiceData voiceData = new VoiceData(sendTime, exactData);
                consumer.VOICE_RECEIVE_BUFFER.offer(voiceData);
                consumer.setLastVoicePackageNum(packageNum);
            }
        } else {//图片数据
            int lastPicturePackageNum = consumer.getLastPicturePackageNum();
            boolean finalFlag = (((data[0] >>> 4) & AND_BIT) == 1);//是否是图片的最后一包
            byte sliceNum = (byte)(data[0] & 0x0f);//图片分片后的小包包号

//            System.out.println("图片小包号： " + sliceNum + "finalFlag: " + finalFlag);

            if (packageNum == lastPicturePackageNum  && !consumer.getLastPicturePackageNumFlag()) {//属于同一大包中的另一小包图片数据
                SlicePictureData slicePicture = new SlicePictureData(sliceNum, exactData, packageNum, sendTime);
                consumer.slicePictureList.add(slicePicture);
                if (finalFlag) {
                    Collections.sort(consumer.slicePictureList);
                    consumer.PICTURE_RECEIVE_BUFFER.offer(consumer.slicePictureList);
                    consumer.slicePictureList = new ArrayList<>(16);
                    //不再接受同一大包中的其它小包的数据
                    consumer.setLastPicturePackageNumFlag(true);
                }
            } else if (packageNum > lastPicturePackageNum ||
                    (packageNum < lastPicturePackageNum && lastPicturePackageNum - packageNum > MAX_CYCLE_NUM)) {
                //开始接受另一个大包中的数据
                consumer.setLastPicturePackageNum(packageNum);
                consumer.setLastPicturePackageNumFlag(false);

                if (consumer.slicePictureList.size() > 0) //可能上次的大包中并没有接收到最后一个小包
                    consumer.slicePictureList.clear();

                SlicePictureData slicePicture = new SlicePictureData(sliceNum, exactData, packageNum, sendTime);
                consumer.slicePictureList.add(slicePicture);
                if (finalFlag) {//如果接收的第一个小包就是此大包中的最后一个小包
                    consumer.PICTURE_RECEIVE_BUFFER.offer(consumer.slicePictureList);
                    consumer.slicePictureList = new ArrayList<>(16);
                    consumer.setLastPicturePackageNumFlag(true);
                }
            } // end else if
        } // end else
    } // end method

    public void finishCalling(String projectId) {//结束指定通话
        consumerMap.get(projectId).getThread().interrupt();
        consumerMap.remove(projectId);
    }

    private static class ServerConsumerAndThread {

        private ServerConsumer serverConsumer;
        private Thread thread;

        public ServerConsumerAndThread(ServerConsumer serverConsumer, Thread thread) {
            this.serverConsumer = serverConsumer;
            this.thread = thread;
        }

        public Thread getThread() {
            return thread;
        }

        public void setThread(Thread thread) {
            this.thread = thread;
        }

        public ServerConsumer getServerConsumer() {
            return serverConsumer;
        }

        public void setServerConsumer(ServerConsumer serverConsumer) {
            this.serverConsumer = serverConsumer;
        }
    }

}
