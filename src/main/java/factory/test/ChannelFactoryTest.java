package factory.test;

import Sample.SampleStore;
import Sample.SampleUser;
import client.ChannelClient;
import config.ReadConfig;
import factory.ChannelFactory;
import factory.UserFactory;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.exception.*;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class ChannelFactoryTest {

    @Test
    public void createNewChannelTest() throws IllegalAccessException, InvocationTargetException, InvalidArgumentException, InstantiationException, NoSuchMethodException, CryptoException, ClassNotFoundException, IOException, NetworkConfigurationException, TransactionException, ProposalException {
        HFClient hfClient = HFClient.createNewInstance();
        hfClient.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
        SampleStore sampleStore = new SampleStore(new File(System.getProperty("user.home"), "test.properties"));
        ReadConfig readConfig = ReadConfig.fromYamlFile(new File("Resource/network-config-kafka.yaml"));
        SampleUser admin = UserFactory.getAdmin(sampleStore, readConfig);
        hfClient.setUserContext(admin);

        Channel channel = ChannelFactory.createNewChannel(hfClient, "mychannel", new File("Resource/channel.tx"), readConfig, sampleStore);
        System.out.println(channel.getName());
        System.out.println(channel.getOrderers());
    }

    @Test
    public void joinChannelFromYamlTest() throws IllegalAccessException, InvocationTargetException, InvalidArgumentException, InstantiationException, NoSuchMethodException, CryptoException, ClassNotFoundException, IOException, NetworkConfigurationException, TransactionException, ProposalException {
        HFClient hfClient = HFClient.createNewInstance();
        hfClient.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
        SampleStore sampleStore = new SampleStore(new File(System.getProperty("user.home"), "test.properties"));
        ReadConfig readConfig = ReadConfig.fromYamlFile(new File("Resource/network-config-kafka.yaml"));
        SampleUser admin = UserFactory.getAdmin(sampleStore, readConfig);
        hfClient.setUserContext(admin);

        Channel channel = ChannelFactory.getChannelFromYaml(hfClient, "mychannel", readConfig);

        String peerName = "peer2.org1.beijinca.com";
        String peerUrl = "grpc://192.168.1.164:7060";
        Peer peer = hfClient.newPeer(peerName, peerUrl, readConfig.getPeerProperties(peerName));
        ChannelClient.joinChannel(channel, peer, Channel.PeerOptions.createPeerOptions().setPeerRoles(Peer.PeerRole.ALL),sampleStore);
    }
}
