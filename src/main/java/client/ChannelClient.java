package client;

import Sample.SampleStore;
import Sample.SampleUser;
import config.ChannelInfo;
import config.ReadConfig;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.exception.*;
import org.hyperledger.fabric.sdk.security.CryptoSuite;

import java.io.File;
import java.io.IOException;
import java.util.List;

/*
* 处理所有与Channel与联盟链相关的操作
* */
public class ChannelClient {

    /*
    * 初始化新channel
    * */
    public static Channel newChannel(HFClient hfClient, ChannelInfo channelInfo, SampleStore sampleStore) throws InvalidArgumentException, TransactionException, ProposalException, IOException {
        List<Orderer> orderers = channelInfo.getOrderers();

        Orderer anOrderer = orderers.iterator().next();
        orderers.remove(anOrderer);
        Channel channel = hfClient.newChannel(channelInfo.getChannelName(), anOrderer, channelInfo.getChannelConfiguration(), channelInfo.getSignature());
        for(Orderer orderer: orderers){
            channel.addOrderer(orderer);
        }
        channel.initialize();
        sampleStore.saveChannel(channel);
        return channel;
    }

    /*
    * 对Channel中添加新的Peer
    * */
    public static void joinChannel(Channel channel, Peer peer, Channel.PeerOptions peerOptions, SampleStore sampleStore) throws ProposalException, InvalidArgumentException, IOException {
        channel.joinPeer(peer, peerOptions);
        sampleStore.saveChannel(channel);
    }
}
