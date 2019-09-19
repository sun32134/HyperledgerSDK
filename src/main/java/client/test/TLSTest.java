package client.test;

import Sample.SampleStore;
import Sample.SampleUser;
import client.ChaincodeClient;
import config.ReadConfig;
import factory.ChannelFactory;
import factory.TransactionReqFactory;
import factory.UserFactory;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.junit.Test;

import java.io.File;
import java.util.Collection;
import java.util.Properties;


public class TLSTest {
    @Test
    public void connectToPeerTest() throws Exception {
        HFClient hfClient = HFClient.createNewInstance();
        hfClient.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
        SampleStore sampleStore = new SampleStore(new File(System.getProperty("user.home"), "test.properties"));
        ReadConfig readConfig = ReadConfig.fromYamlFile(new File("Resource/network-config-raft.yaml"));
        SampleUser admin = UserFactory.getAdmin(sampleStore, readConfig);
        hfClient.setUserContext(admin);
        Channel channel = ChannelFactory.getChannelFromYaml(hfClient,"crmchannel", readConfig);

//        BlockchainInfo info = channel.queryBlockchainInfo();
//        System.out.println(info.getHeight());
        ReadConfig.ChaincodeInfo chaincodeInfo = readConfig.getChaincodeInfo("mycc");
        QueryByChaincodeRequest queryByChaincodeRequest = TransactionReqFactory.queryChaincodeReqInit(hfClient,chaincodeInfo.getChaincodeID(),"query", new String[]{"a"});
        ChaincodeClient.queryChaincode(channel,queryByChaincodeRequest);
    }
}
