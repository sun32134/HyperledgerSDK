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
* ����������Channel����������صĲ���
* */
public class ChannelClient {

    /*
    * ��ʼ����channel
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
    * ��Channel������µ�Peer
    * */
    public static void joinChannel(Channel channel, Peer peer, Channel.PeerOptions peerOptions, SampleStore sampleStore) throws ProposalException, InvalidArgumentException, IOException {
        channel.joinPeer(peer, peerOptions);
        sampleStore.saveChannel(channel);
    }
}
