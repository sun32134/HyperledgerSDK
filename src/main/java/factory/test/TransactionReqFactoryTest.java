package factory.test;

import Sample.SampleStore;
import Sample.SampleUser;
import config.ReadConfig;
import factory.TransactionReqFactory;
import factory.UserFactory;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.exception.ChaincodeEndorsementPolicyParseException;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.NetworkConfigurationException;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class TransactionReqFactoryTest {
    @Test
    public void installTest() throws NetworkConfigurationException, IOException, InvalidArgumentException, IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchMethodException, CryptoException, ClassNotFoundException {
        HFClient hfClient = HFClient.createNewInstance();
        hfClient.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
        SampleStore sampleStore = new SampleStore(new File(System.getProperty("user.home"), "test.properties"));
        ReadConfig readConfig = ReadConfig.fromYamlFile(new File("Resource/network-config.yaml"));
        SampleUser admin = UserFactory.getAdmin(sampleStore, readConfig);
        hfClient.setUserContext(admin);

        ReadConfig.ChaincodeInfo chaincodeInfo = readConfig.getChaincodeInfo("fabcar");
        TransactionReqFactory.installChaincodeReqInit(hfClient, chaincodeInfo);
    }

    @Test
    public void instantiateTest() throws IllegalAccessException, InvocationTargetException, InvalidArgumentException, InstantiationException, NoSuchMethodException, CryptoException, ClassNotFoundException, IOException, NetworkConfigurationException, ChaincodeEndorsementPolicyParseException {
        HFClient hfClient = HFClient.createNewInstance();
        hfClient.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
        SampleStore sampleStore = new SampleStore(new File(System.getProperty("user.home"), "test.properties"));
        ReadConfig readConfig = ReadConfig.fromYamlFile(new File("Resource/network-config.yaml"));
        SampleUser admin = UserFactory.getAdmin(sampleStore, readConfig);
        hfClient.setUserContext(admin);

        ReadConfig.ChaincodeInfo chaincodeInfo = readConfig.getChaincodeInfo("fabcar");
        TransactionReqFactory.instantiateChaincodeReqInit(hfClient,chaincodeInfo, "Init", new String[]{""});
    }

    @Test
    public void invodeTest() throws IllegalAccessException, InvocationTargetException, InvalidArgumentException, InstantiationException, NoSuchMethodException, CryptoException, ClassNotFoundException, IOException, NetworkConfigurationException {
        HFClient hfClient = HFClient.createNewInstance();
        hfClient.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
        SampleStore sampleStore = new SampleStore(new File(System.getProperty("user.home"), "test.properties"));
        ReadConfig readConfig = ReadConfig.fromYamlFile(new File("Resource/network-config.yaml"));
        SampleUser admin = UserFactory.getAdmin(sampleStore, readConfig);
        hfClient.setUserContext(admin);

        ReadConfig.ChaincodeInfo chaincodeInfo = readConfig.getChaincodeInfo("fabcar");
        TransactionReqFactory.invodeChaincodeReqInit(hfClient, chaincodeInfo, "initLedger", new String[]{""});
    }

    @Test
    public void queryTest() throws IllegalAccessException, InvocationTargetException, InvalidArgumentException, InstantiationException, NoSuchMethodException, CryptoException, ClassNotFoundException, IOException, NetworkConfigurationException {
        HFClient hfClient = HFClient.createNewInstance();
        hfClient.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
        SampleStore sampleStore = new SampleStore(new File(System.getProperty("user.home"), "test.properties"));
        ReadConfig readConfig = ReadConfig.fromYamlFile(new File("Resource/network-config.yaml"));
        SampleUser admin = UserFactory.getAdmin(sampleStore, readConfig);
        hfClient.setUserContext(admin);

        ReadConfig.ChaincodeInfo chaincodeInfo = readConfig.getChaincodeInfo("fabcar");
        TransactionReqFactory.queryChaincodeReqInit(hfClient, chaincodeInfo.getChaincodeID(),"queryAllCars",new String[]{""});
    }
}
