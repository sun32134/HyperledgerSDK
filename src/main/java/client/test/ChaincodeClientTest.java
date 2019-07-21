package client.test;

import Sample.SampleStore;
import Sample.SampleUser;
import client.ChaincodeClient;
import client.ChaincodeEventCapture;
import config.ReadConfig;
import factory.ChannelFactory;
import factory.TransactionReqFactory;
import factory.UserFactory;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.junit.Test;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;

public class ChaincodeClientTest {
    @Test
    public void installChainCodeTest() throws Exception {
        HFClient hfClient = HFClient.createNewInstance();
        hfClient.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
        SampleStore sampleStore = new SampleStore(new File(System.getProperty("user.home"), "test.properties"));
        ReadConfig readConfig = ReadConfig.fromYamlFile(new File("Resource/network-config.yaml"));
        SampleUser admin = UserFactory.getAdmin(sampleStore, readConfig);
        hfClient.setUserContext(admin);

        // 安装chaincode时需要注意将network-config中的client org部分修改成与该peer相同的Org，否则使用的私钥和证书会不对
        Peer peer1 = hfClient.newPeer("peer0.org1.example.com", "grpc://192.168.1.164:7051");
        Peer peer2 = hfClient.newPeer("peer1.org1.example.com", "grpc://192.168.1.164:7056");
        Collection<Peer> peers = new LinkedList<>();
        peers.add(peer1);
        peers.add(peer2);

        ReadConfig.ChaincodeInfo chaincodeInfo = readConfig.getChaincodeInfo("crm");
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

        ReadConfig.ChaincodeInfo chaincodeInfo = readConfig.getChaincodeInfo("crm");
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

        ReadConfig.ChaincodeInfo chaincodeInfo = readConfig.getChaincodeInfo("crm");
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
        ReadConfig.ChaincodeInfo chaincodeInfo = readConfig.getChaincodeInfo("crm");
        TransactionProposalRequest transactionProposalRequest = TransactionReqFactory.invodeChaincodeReqInit(hfClient, chaincodeInfo, "uploadRegister",
                new String[]{"1", "asdfasdf", "asdf", "wer", "asdf", "asdf", "wer", "asdgawe", "adfaw", "asdfaw", "asdfwev"});
//        TransactionProposalRequest transactionProposalRequest = TransactionReqFactory.invodeChaincodeReqInit(hfClient, chaincodeInfo, "uploadDCI",
//                new String[]{"2", "asdfasdf", "asdf", "wer", "asdf"});
//        TransactionProposalRequest transactionProposalRequest =  TransactionReqFactory.invodeChaincodeReqInit(hfClient, chaincodeInfo, "expiredDCI",
//                new String[]{"2", "asdfasdf"});
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
        ReadConfig.ChaincodeInfo chaincodeInfo = readConfig.getChaincodeInfo("crm");
        QueryByChaincodeRequest queryByChaincodeRequest = TransactionReqFactory.queryChaincodeReqInit(hfClient,chaincodeInfo.getChaincodeID(),"queryRegister", new String[]{"2"});
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
        String EXPECTED_EVENT_NAME = "upload success";
        CountDownLatch latch = new CountDownLatch(1);
        String eventListenerHandle = ChaincodeClient.setChaincodeEventListener(channel,EXPECTED_EVENT_NAME, latch);
        System.out.println("waiting for event");
        latch.await();
    }

    @Test
    public void testJson(){
        byte[] bytes = new byte[]{123, 34, 84, 120, 105, 100, 34, 58, 34, 55, 51, 101, 55, 56, 100, 100, 54, 97, 55, 100, 98, 101, 51, 101, 98, 102, 53, 56, 97, 56, 52, 100, 97, 57, 57, 49, 52, 53, 101, 99, 51, 50, 48, 53, 100, 97, 48, 54, 49, 101, 97, 49, 49, 97, 98, 48, 48, 102, 48, 55, 101, 50, 97, 49, 52, 53, 49, 52, 54, 101, 97, 97, 50, 34, 44, 34, 72, 97, 115, 104, 34, 58, 34, 50, 34, 125};
        String s = new String(bytes);
        System.out.println(s);
    }

}
