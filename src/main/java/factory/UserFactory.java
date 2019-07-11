package factory;

import Sample.SampleStore;
import Sample.SampleUser;
import Sample.Util;
import config.ReadConfig;
import org.hyperledger.fabric.sdk.exception.NetworkConfigurationException;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;

public class UserFactory {
    public static SampleUser getAdmin(SampleStore sampleStore, ReadConfig readConfig) throws NetworkConfigurationException {
        ReadConfig.OrgInfo orgInfo = readConfig.getClientOrganization();
        ReadConfig.UserInfo userInfo = readConfig.getPeerAdmin();
        if(sampleStore.hasMember(userInfo.getName(), orgInfo.getName())){
            return sampleStore.getMember(userInfo.getName(), orgInfo.getName());
        }
        else{
            return sampleStore.getMember(userInfo.getName(), orgInfo.getName(), orgInfo.getMspId(), userInfo.getEnrollment());
        }
    }

    public static SampleUser getUser(SampleStore sampleStore, String userName, String orgName){
        if(sampleStore.hasMember(userName, orgName)){
            return sampleStore.getMember(userName, orgName);
        }
        else{
            throw new NullPointerException("Sample store do not has member user " + userName);
        }
    }

    public static SampleUser getUser(SampleStore sampleStore, String userName, ReadConfig.OrgInfo orgInfo, String skFilePath, String pkFilePath) throws InvalidKeySpecException, NoSuchAlgorithmException, NoSuchProviderException, IOException {
        return sampleStore.getMember(userName, orgInfo.getName(), orgInfo.getMspId(), Util.findFileSk(new File(skFilePath)), new File(pkFilePath));
    }
}
