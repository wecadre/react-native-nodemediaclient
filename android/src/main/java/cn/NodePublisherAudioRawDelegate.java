package cn.nodemedia;

public interface NodePublisherAudioRawDelegate {
    void onAudioRawCallback(NodePublisher streamer, int channels, int samplerate, byte[] data, int size);
}
