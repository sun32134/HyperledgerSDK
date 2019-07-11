package config;

import Sample.SampleUser;
import config.ReadConfig;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.NetworkConfigurationException;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ChannelInfo {
    private String channelName;
    private List<Orderer> orderers;
    private List<Peer> peers;
    private ChannelConfiguration channelConfiguration;
    private byte[] signature;

    public ChannelInfo(String channelName){
        this.channelName = channelName;
        this.orderers = new LinkedList<>();
        this.peers = new LinkedList<>();
    }

    private void calcuateSignature(HFClient hfClient, SampleUser admin) throws InvalidArgumentException {
        this.signature = hfClient.getChannelConfigurationSignature(channelConfiguration, admin);
    }

    public void initFromReadConfig(HFClient hfClient, File channelConfig, ReadConfig readConfig) throws NetworkConfigurationException, IOException, InvalidArgumentException {
        Map<String, ReadConfig.Node> peers = readConfig.getChannelPeers(channelName);
        peers.forEach((peerName, peerNode) -> {
            try {
                Peer peer = hfClient.newPeer(peerName, peerNode.getUrl(), peerNode.getProperties());
                this.peers.add(peer);
            } catch (InvalidArgumentException e) {
                e.printStackTrace();
            }
        });

        Map<String, ReadConfig.Node> orderers = readConfig.getChannelOrderer(channelName);
        orderers.forEach((ordererName, ordererNode)->{
            try {
                Orderer orderer = hfClient.newOrderer(ordererName, ordererNode.getUrl());
                this.orderers.add(orderer);
            } catch (InvalidArgumentException e) {
                e.printStackTrace();
            }
        });
        this.setChannelConfiguration(channelConfig);
        this.calcuateSignature(hfClient, (SampleUser) hfClient.getUserContext());
    }

    public String getChannelName() {
        return channelName;
    }

    public List<Orderer> getOrderers() {
        return orderers;
    }

    public ChannelConfiguration getChannelConfiguration() {
        return channelConfiguration;
    }

    private void setChannelConfiguration(File channelConfig) throws IOException, InvalidArgumentException {
        this.channelConfiguration = new ChannelConfiguration(channelConfig);
    }

    public byte[] getSignature() {
        return signature;
    }

    public List<Peer> getPeers() {
        return this.peers;
    }
}
