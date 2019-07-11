package test;

import Sample.SampleOrg;
import Sample.SampleStore;
import Sample.SampleUser;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.hyperledger.fabric.sdk.exception.TransactionException;

import java.io.File;
import java.io.IOException;
import java.util.*;


import static org.hyperledger.fabric.sdk.Channel.PeerOptions.createPeerOptions;

public class ChannelFactory {
    public static Channel constructChannel(String channelName, SampleUser admin, HFClient hfClient, Collection<Orderer> orderers, Collection<Peer> peers) throws Exception {

        Orderer anOrderer = orderers.iterator().next();
        orderers.remove(anOrderer);

        String channelConfig = "Resource/channel.tx";
        ChannelConfiguration channelConfiguration = new ChannelConfiguration(new File(channelConfig));
        Channel newChannel = hfClient.newChannel(channelName, anOrderer, channelConfiguration, hfClient.getChannelConfigurationSignature(channelConfiguration, admin));
        for(Peer peer: peers){
            newChannel.joinPeer(peer, createPeerOptions().setPeerRoles(EnumSet.of(Peer.PeerRole.ENDORSING_PEER, Peer.PeerRole.LEDGER_QUERY, Peer.PeerRole.CHAINCODE_QUERY, Peer.PeerRole.EVENT_SOURCE)));
        }

        for(Orderer orderer: orderers){
            newChannel.addOrderer(orderer);
        }
        newChannel.initialize();
        SampleStore sampleStore = new SampleStore(new File(System.getProperty("user.home"), "test.properties"));
        sampleStore.saveChannel(newChannel);
        return newChannel;
    }

    public static Channel fromSampleStore(HFClient hfClient) throws InvalidArgumentException, IOException, ClassNotFoundException, TransactionException {
        SampleStore sampleStore = new SampleStore(new File(System.getProperty("user.home"), "test.properties"));
        Channel channel = sampleStore.getChannel(hfClient, "mychannel");
        channel.initialize();
        return channel;
    }

    public static void joinChannel(Channel channel, Collection<Peer> peers) throws ProposalException, TransactionException, InvalidArgumentException, IOException {
        for(Peer peer: peers){
            channel.joinPeer(peer, createPeerOptions().setPeerRoles(EnumSet.of(Peer.PeerRole.ENDORSING_PEER, Peer.PeerRole.LEDGER_QUERY, Peer.PeerRole.CHAINCODE_QUERY, Peer.PeerRole.EVENT_SOURCE)));
        }
        channel.initialize();

        SampleStore sampleStore = new SampleStore(new File(System.getProperty("user.home"), "test.properties"));
        sampleStore.saveChannel(channel);
    }
}
