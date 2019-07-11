package test;

import config.ReadConfig;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.Peer.PeerRole;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.NetworkConfigurationException;
import org.junit.Test;
import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;

import java.io.File;
import java.io.IOException;
import java.util.Map;



public class channelTest {

    @Test
    public void channelInitTest() throws NetworkConfigurationException, IOException, InvalidArgumentException {
        ReadConfig readConfig = ReadConfig.fromYamlFile(new File("Resource/network-config.yaml"));
        Map<String, ReadConfig.Node> peers =  readConfig.getChannelPeers("mychannel");
        peers.forEach((key, value) -> {
            System.out.println(key);
            System.out.println(value.getUrl());
        });

        Channel.PeerOptions peerOptions = Channel.PeerOptions.createPeerOptions();
        for(PeerRole peerRole: PeerRole.values()){
            peerOptions.addPeerRole(peerRole);
        }
    }

    public static void main(String[] argv) throws IOException, NetworkConfigurationException, InvalidArgumentException {
        channelTest channelTest = new channelTest();
        channelTest.channelInitTest();
    }
}
