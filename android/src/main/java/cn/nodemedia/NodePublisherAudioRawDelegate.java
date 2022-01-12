package cn.nodemedia;
import cn.nodemedia.NodePublisher;

public interface NodePublisherAudioRawDelegate {
    void onAudioRawCallback(NodePublisher streamer, int channels, int samplerate, byte[] data, int size);
}
