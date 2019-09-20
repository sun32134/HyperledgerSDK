package client.test;

import Sample.SampleOrg;
import Sample.SampleStore;
import Sample.SampleUser;
import client.ChaincodeClient;
import config.ReadConfig;
import factory.ChannelFactory;
import factory.TransactionReqFactory;
import factory.UserFactory;
import org.bouncycastle.openssl.PEMWriter;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric_ca.sdk.EnrollmentRequest;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.HFCAInfo;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.security.PrivateKey;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.Properties;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ChaincodeClientRaftTest {
    @Test
    public void installChainCodeRaftTest() throws Exception {
        HFClient hfClient = HFClient.createNewInstance();
        hfClient.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
        SampleStore sampleStore = new SampleStore(new File(System.getProperty("user.home"), "test.properties"));
        ReadConfig readConfig = ReadConfig.fromYamlFile(new File("Resource/network-config-raft.yaml"));
        SampleUser admin = UserFactory.getAdmin(sampleStore, readConfig);
        hfClient.setUserContext(admin);

        Channel channel = ChannelFactory.getChannelFromYaml(hfClient,"crmchannel", readConfig);
        Collection<Peer> peers = channel.getPeers(EnumSet.of(Peer.PeerRole.ENDORSING_PEER));

        ReadConfig.ChaincodeInfo chaincodeInfo = readConfig.getChaincodeInfo("crm");
        InstallProposalRequest installProposalRequest = TransactionReqFactory.installChaincodeReqInit(hfClient, chaincodeInfo);
        ChaincodeClient.installChaincode(hfClient, peers,installProposalRequest);
    }

    @Test
    public void instantiateChaincodeRaftTest() throws Exception {
        HFClient hfClient = HFClient.createNewInstance();
        hfClient.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
        SampleStore sampleStore = new SampleStore(new File(System.getProperty("user.home"), "test.properties"));
        ReadConfig readConfig = ReadConfig.fromYamlFile(new File("Resource/network-config-raft.yaml"));
        SampleUser admin = UserFactory.getAdmin(sampleStore, readConfig);
        hfClient.setUserContext(admin);

        Channel channel = ChannelFactory.getChannelFromYaml(hfClient,"crmchannel", readConfig);
        Collection<Peer> peers = channel.getPeers();

        ReadConfig.ChaincodeInfo chaincodeInfo = readConfig.getChaincodeInfo("crm");
        InstantiateProposalRequest instantiateProposalRequest = TransactionReqFactory.instantiateChaincodeReqInit(hfClient, chaincodeInfo,"Init", new String[]{""});
        ChaincodeClient.instantiateChaincode(channel, peers,instantiateProposalRequest);
    }

    @Test
    public void upgradeChaincodeRaftTest() throws Exception {
        HFClient hfClient = HFClient.createNewInstance();
        hfClient.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
        SampleStore sampleStore = new SampleStore(new File(System.getProperty("user.home"), "test.properties"));
        ReadConfig readConfig = ReadConfig.fromYamlFile(new File("Resource/network-config-raft.yaml"));
        SampleUser admin = UserFactory.getAdmin(sampleStore, readConfig);
        hfClient.setUserContext(admin);

        Channel channel = ChannelFactory.getChannelFromYaml(hfClient,"crmchannel", readConfig);
        Collection<Peer> peers = channel.getPeers(EnumSet.of(Peer.PeerRole.ENDORSING_PEER));

        ReadConfig.ChaincodeInfo chaincodeInfo = readConfig.getChaincodeInfo("crm");
        UpgradeProposalRequest instantiateProposalRequest = TransactionReqFactory.upgradeChaincodeReqInit(hfClient,chaincodeInfo,"Init", new String[]{""});
        ChaincodeClient.upgradeChaincode(channel, peers, instantiateProposalRequest);
    }

    @Test
    public void invokeChaincodeRaftTest() throws Exception {
        HFClient hfClient = HFClient.createNewInstance();
        hfClient.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
        SampleStore sampleStore = new SampleStore(new File(System.getProperty("user.home"), "test.properties"));
        ReadConfig readConfig = ReadConfig.fromYamlFile(new File("Resource/network-config-raft.yaml"));
        SampleUser admin = UserFactory.getAdmin(sampleStore, readConfig);
        hfClient.setUserContext(admin);

        Channel channel = ChannelFactory.getChannelFromYaml(hfClient,"crmchannel", readConfig);
        ReadConfig.ChaincodeInfo chaincodeInfo = readConfig.getChaincodeInfo("crm");
//      TransactionProposalRequest transactionProposalRequest = TransactionReqFactory.invodeChaincodeReqInit(hfClient, chaincodeInfo, "invoke",
//                new String[]{"a", "b", "10"});
        TransactionProposalRequest transactionProposalRequest = TransactionReqFactory.invodeChaincodeReqInit(hfClient, chaincodeInfo, "uploadRegister",
                new String[]{"1", "adfawe", "asdf", "wer", "asdf", "asdf", "wer", "asdgawe", "adfaw", "asdfaw", "asdfwev"});
        ChaincodeClient.invodeChaincode(channel, transactionProposalRequest);
    }

    @Test
    public void queryChaincodeRaftTest() throws Exception{
        HFClient hfClient = HFClient.createNewInstance();
        hfClient.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
        SampleStore sampleStore = new SampleStore(new File(System.getProperty("user.home"), "test.properties"));
        ReadConfig readConfig = ReadConfig.fromYamlFile(new File("Resource/network-config-raft.yaml"));
        SampleUser admin = UserFactory.getAdmin(sampleStore, readConfig);
        hfClient.setUserContext(admin);
        System.out.println(admin.isEnrolled());

        Channel channel = ChannelFactory.getChannelFromYaml(hfClient,"crmchannel", readConfig);
        ReadConfig.ChaincodeInfo chaincodeInfo = readConfig.getChaincodeInfo("crm");
        QueryByChaincodeRequest queryByChaincodeRequest = TransactionReqFactory.queryChaincodeReqInit(hfClient,chaincodeInfo.getChaincodeID(),"queryRegister", new String[]{"1"});
        ChaincodeClient.queryChaincode(channel, queryByChaincodeRequest);
    }
}
