package factory;

import Sample.SampleStore;
import client.ChannelClient;
import config.ChannelInfo;
import config.ReadConfig;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.NetworkConfigurationException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.hyperledger.fabric.sdk.exception.TransactionException;

import java.io.File;
import java.io.IOException;

public class ChannelFactory {
    public static Channel createNewChannel(HFClient client, String channelName, File channelConfig, ReadConfig readConfig, SampleStore sampleStore) throws IOException, NetworkConfigurationException, InvalidArgumentException, ProposalException, TransactionException {
        ChannelInfo channelInfo = new ChannelInfo(channelName);
        channelInfo.initFromReadConfig(client, channelConfig, readConfig);
        return ChannelClient.newChannel(client, channelInfo, sampleStore);
    }

    public static Channel getChannelFromYaml(HFClient client, String channelName, ReadConfig readConfig) throws NetworkConfigurationException, TransactionException, InvalidArgumentException {
        Channel channel =  readConfig.loadChannel(client, channelName);
        channel.initialize();
        return channel;
    }

    public static Channel getChannelFromStore(HFClient client, String channelName, SampleStore sampleStore) throws InvalidArgumentException, IOException, ClassNotFoundException, TransactionException {
        Channel channel =  sampleStore.getChannel(client, channelName);
        channel.initialize();
        return channel;
    }
}
