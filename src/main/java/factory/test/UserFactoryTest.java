package factory.test;

import Sample.SampleStore;
import Sample.SampleUser;
import config.ReadConfig;
import factory.UserFactory;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.NetworkConfigurationException;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;

public class UserFactoryTest {
    @Test
    public void getAdminTest() throws NetworkConfigurationException, IOException, InvalidArgumentException {
        ReadConfig readConfig = ReadConfig.fromYamlFile(new File("Resource/network-config.yaml"));
        SampleStore sampleStore = new SampleStore(new File(System.getProperty("user.home"), "test.properties"));
        SampleUser sampleUser = UserFactory.getAdmin(sampleStore, readConfig);
        Assert.assertEquals("Org2MSP", sampleUser.getMspId());
    }

    @Test
    public void getUserTest() throws NetworkConfigurationException, IOException, InvalidArgumentException {
        ReadConfig readConfig = ReadConfig.fromYamlFile(new File("Resource/network-config.yaml"));
        SampleStore sampleStore = new SampleStore(new File(System.getProperty("user.home"), "test.properties"));
        SampleUser sampleUser = UserFactory.getUser(sampleStore, "admin1", "Org1");
        Assert.assertEquals("Org1MSP", sampleUser.getMspId());
    }

    @Test
    public void getNewUserTest() throws NetworkConfigurationException, IOException, InvalidArgumentException, InvalidKeySpecException, NoSuchAlgorithmException, NoSuchProviderException {
        ReadConfig readConfig = ReadConfig.fromYamlFile(new File("Resource/network-config.yaml"));
        SampleStore sampleStore = new SampleStore(new File(System.getProperty("user.home"), "test.properties"));
        ReadConfig.OrgInfo orgInfo = readConfig.getOrganizationInfo("Org2");
        SampleUser sampleUser = UserFactory.getUser(sampleStore, "user1", orgInfo, "Resource/Org2User/User1@org2.example.com/msp/keystore",
                "Resource/Org2User/User1@org2.example.com/msp/signcerts/User1@org2.example.com-cert.pem");
        Assert.assertEquals("Org2MSP", sampleUser.getMspId());
    }
}
