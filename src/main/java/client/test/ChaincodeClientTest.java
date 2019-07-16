package client.test;

import Sample.SampleStore;
import Sample.SampleUser;
import client.ChaincodeClient;
import client.ChaincodeEventCapture;
import client.ChannelClient;
import config.ChannelInfo;
import config.ReadConfig;
import factory.ChannelFactory;
import factory.TransactionReqFactory;
import factory.UserFactory;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.NetworkConfigurationException;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.junit.Test;
import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;
import sun.awt.windows.WToolkit;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

public class ChaincodeClientTest {
    @Test
    public void installChainCodeTest() throws Exception {
        HFClient hfClient = HFClient.createNewInstance();
        hfClient.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
        SampleStore sampleStore = new SampleStore(new File(System.getProperty("user.home"), "test.properties"));
        ReadConfig readConfig = ReadConfig.fromYamlFile(new File("Resource/network-config.yaml"));
        SampleUser admin = UserFactory.getAdmin(sampleStore, readConfig);
        hfClient.setUserContext(admin);

        Peer peer1 = hfClient.newPeer("peer0.org2.example.com", "grpc://192.168.1.164:8051");
        Peer peer2 = hfClient.newPeer("peer1.org2.example.com", "grpc://192.168.1.164:8056");
        Collection<Peer> peers = new LinkedList<>();
        peers.add(peer1);
        peers.add(peer2);

        ReadConfig.ChaincodeInfo chaincodeInfo = readConfig.getChaincodeInfo("fabcar");
        InstallProposalRequest installProposalRequest = TransactionReqFactory.installChaincodeReqInit(hfClient, chaincodeInfo);
        ChaincodeClient.installChaincode(hfClient,peers,installProposalRequest);
    }

    @Test
    public void instantiateChaincodeTest() throws Exception {
        HFClient hfClient = HFClient.createNewInstance();
        hfClient.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
        SampleStore sampleStore = new SampleStore(new File(System.getProperty("user.home"), "test.properties"));
        ReadConfig readConfig = ReadConfig.fromYamlFile(new File("Resource/network-config.yaml"));
        SampleUser admin = UserFactory.getAdmin(sampleStore, readConfig);
        hfClient.setUserContext(admin);

        Channel channel = ChannelFactory.getChannelFromYaml(hfClient,"mychannel", readConfig);
        Collection<Peer> peers = channel.getPeers();

        ReadConfig.ChaincodeInfo chaincodeInfo = readConfig.getChaincodeInfo("fabcar");
        InstantiateProposalRequest instantiateProposalRequest = TransactionReqFactory.instantiateChaincodeReqInit(hfClient,chaincodeInfo,"Init", new String[]{""});
        ChaincodeClient.instantiateChaincode(channel, peers,instantiateProposalRequest);
    }

    @Test
    public void upgradeChaincodeTest() throws Exception {
        HFClient hfClient = HFClient.createNewInstance();
        hfClient.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
        SampleStore sampleStore = new SampleStore(new File(System.getProperty("user.home"), "test.properties"));
        ReadConfig readConfig = ReadConfig.fromYamlFile(new File("Resource/network-config.yaml"));
        SampleUser admin = UserFactory.getAdmin(sampleStore, readConfig);
        hfClient.setUserContext(admin);

        Channel channel = ChannelFactory.getChannelFromYaml(hfClient,"mychannel", readConfig);
        Collection<Peer> peers = channel.getPeers();

        ReadConfig.ChaincodeInfo chaincodeInfo = readConfig.getChaincodeInfo("fabcar");
        UpgradeProposalRequest instantiateProposalRequest = TransactionReqFactory.upgradeChaincodeReqInit(hfClient,chaincodeInfo,"Init", new String[]{""});
        ChaincodeClient.upgradeChaincode(channel, peers,instantiateProposalRequest);
    }

    @Test
    public void invokeChaincodeTest() throws Exception {
        HFClient hfClient = HFClient.createNewInstance();
        hfClient.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
        SampleStore sampleStore = new SampleStore(new File(System.getProperty("user.home"), "test.properties"));
        ReadConfig readConfig = ReadConfig.fromYamlFile(new File("Resource/network-config.yaml"));
        SampleUser admin = UserFactory.getAdmin(sampleStore, readConfig);
        hfClient.setUserContext(admin);

        Channel channel = ChannelFactory.getChannelFromYaml(hfClient,"mychannel", readConfig);
        ReadConfig.ChaincodeInfo chaincodeInfo = readConfig.getChaincodeInfo("fabcar");
        TransactionProposalRequest transactionProposalRequest = TransactionReqFactory.invodeChaincodeReqInit(hfClient, chaincodeInfo, "createCar",
                new String[]{"sdfadf", "asdfasdf", "asdf", "wer", "asdf"});
        ChaincodeClient.invodeChaincode(channel,transactionProposalRequest);
    }

    @Test
    public void queryChaincodeTest() throws Exception{
        HFClient hfClient = HFClient.createNewInstance();
        hfClient.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
        SampleStore sampleStore = new SampleStore(new File(System.getProperty("user.home"), "test.properties"));
        ReadConfig readConfig = ReadConfig.fromYamlFile(new File("Resource/network-config.yaml"));
        SampleUser admin = UserFactory.getAdmin(sampleStore, readConfig);
        hfClient.setUserContext(admin);

        Channel channel = ChannelFactory.getChannelFromYaml(hfClient,"mychannel", readConfig);
        ReadConfig.ChaincodeInfo chaincodeInfo = readConfig.getChaincodeInfo("fabcar");
        QueryByChaincodeRequest queryByChaincodeRequest = TransactionReqFactory.queryChaincodeReqInit(hfClient,chaincodeInfo.getChaincodeID(),"queryAllCars", new String[]{""});
        ChaincodeClient.queryChaincode(channel,queryByChaincodeRequest);
    }

    @Test
    public void chaincodeEventTest() throws Exception{
        HFClient hfClient = HFClient.createNewInstance();
        hfClient.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
        SampleStore sampleStore = new SampleStore(new File(System.getProperty("user.home"), "test.properties"));
        ReadConfig readConfig = ReadConfig.fromYamlFile(new File("Resource/network-config.yaml"));
        SampleUser admin = UserFactory.getAdmin(sampleStore, readConfig);
        hfClient.setUserContext(admin);

        Channel channel = ChannelFactory.getChannelFromYaml(hfClient,"mychannel", readConfig);

        String peerName = "peer0.org1.example.com";
        String peerUrl = "grpc://192.168.1.164:7051";
        EventHub eventHub = hfClient.newEventHub(peerName,peerUrl,readConfig.getPeerProperties(peerName));
        channel.addEventHub(eventHub);
        Vector<ChaincodeEventCapture> chaincodeEvents = new Vector<>();
        String EXPECTED_EVENT_NAME = "event";
        CountDownLatch latch = new CountDownLatch(1);
        String eventListenerHandle = ChaincodeClient.setChaincodeEventListener(channel,EXPECTED_EVENT_NAME, latch);
        System.out.println("waiting for event");
        latch.await();
    }

}
