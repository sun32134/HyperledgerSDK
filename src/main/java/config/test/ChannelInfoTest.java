package config.test;

import Sample.SampleStore;
import Sample.SampleUser;
import config.ChannelInfo;
import config.ReadConfig;
import factory.UserFactory;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.NetworkConfigurationException;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

public class ChannelInfoTest {
    @Test
    public void initFromReadConfigTest() throws IllegalAccessException, InvocationTargetException, InvalidArgumentException, InstantiationException, NoSuchMethodException, CryptoException, ClassNotFoundException, IOException, NetworkConfigurationException {
        HFClient hfClient = HFClient.createNewInstance();
        hfClient.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
        SampleStore sampleStore = new SampleStore(new File(System.getProperty("user.home"), "test.properties"));
        ReadConfig readConfig = ReadConfig.fromYamlFile(new File("Resource/network-config.yaml"));
        SampleUser admin = UserFactory.getAdmin(sampleStore, readConfig);
        hfClient.setUserContext(admin);

        ChannelInfo channelInfo = new ChannelInfo("mychannel");
        channelInfo.initFromReadConfig(hfClient, new File("Resource/channel.tx"), readConfig);

        Assert.assertEquals("mychannel", channelInfo.getChannelName());
        System.out.println(channelInfo.getPeers());
        System.out.println(channelInfo.getOrderers());

        System.out.println(Arrays.toString(channelInfo.getSignature()));
    }
}
