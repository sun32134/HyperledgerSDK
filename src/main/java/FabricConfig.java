import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

public class FabricConfig {
    private static final Logger logger = LoggerFactory.getLogger(FabricConfig.class);

    public static SampleOrg getSampleOrg(){
        SampleOrg org1 = new SampleOrg("peerOrg1", "Org1MSP");
        org1.setDomainName("org1.example.com");

        org1.setCAName("ca_peerOrg1");
        org1.setCALocation("http://192.168.1.164:7054");

        org1.addPeerLocation("peer0.org1.example.com", "grpc://192.168.1.164:7051");
        org1.addPeerLocation("peer1.org1.example.com", "grpc://192.168.1.164:7056");
        org1.addOrdererLocation("orderer.example.com", "grpc://192.168.1.164:7050");
        return org1;
    }

    public static SampleUser getSampleUser(HFClient hfclient) throws IllegalAccessException, InvocationTargetException, InvalidArgumentException, InstantiationException, NoSuchMethodException, CryptoException, ClassNotFoundException, InvalidKeySpecException, NoSuchAlgorithmException, NoSuchProviderException, IOException {
        SampleOrg org1 = FabricConfig.getSampleOrg();
        File sampleStoreFile = new File(System.getProperty("user.home") + "/test.properties");
        if (sampleStoreFile.exists()) { //For testing start fresh
            sampleStoreFile.delete();
        }

        final SampleStore sampleStore = new SampleStore(sampleStoreFile);

        SampleUser admin = sampleStore.getMember("admin", org1.getName(), org1.getMSPID(),
                findFileSk("Resource/Org1User/Admin@org1.example.com/msp/keystore"),
                new File("Resource/Org1User/Admin@org1.example.com/msp/signcerts/Admin@org1.example.com-cert.pem"));
        hfclient.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
        hfclient.setUserContext(admin);

        return admin;
    }

    public static Channel getChannel(HFClient hfclient, String channelName, SampleOrg org) throws InvalidArgumentException {

        return hfclient.newChannel(channelName);
    }

    private static File findFileSk(String directorys) {

        File directory = new File(directorys);
        System.out.printf(String.valueOf(directory.exists()));

        File[] matches = directory.listFiles((dir, name) -> name.endsWith("_sk"));

        if (null == matches) {
            throw new RuntimeException(format("Matches returned null does %s directory exist?", directory.getAbsoluteFile().getName()));
        }

        if (matches.length != 1) {
            throw new RuntimeException(format("Expected in %s only 1 sk file but found %d", directory.getAbsoluteFile().getName(), matches.length));
        }

        return matches[0];

    }
}
