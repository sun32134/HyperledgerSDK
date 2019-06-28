import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FabricConfig {
    private static final Logger logger = LoggerFactory.getLogger(FabricConfig.class);
    private final HashMap<String, SampleOrg> sampleOrgs = new HashMap<>();
    private static FabricConfig config;
    private static final Properties sdkProperties = new Properties();

    private static final String PROPBASE = "network.";
    private static final String INVOKEWAITTIME = PROPBASE + "InvokeWaitTime";
    private static final String DEPLOYWAITTIME = PROPBASE + "DeployWaitTime";
    private static final String PROPOSALWAITTIME = PROPBASE + "ProposalWaitTime";
    private static final String RUNIDEMIXMTTEST = PROPBASE + "RunIdemixMTTest";  // org.hyperledger.fabric.sdktest.RunIdemixMTTest ORG_HYPERLEDGER_FABRIC_SDKTEST_RUNIDEMIXMTTEST
    private static final String RUNSERVICEDISCOVERYIT = PROPBASE + "runServiceDiscoveryIT";  // org.hyperledger.fabric.sdktest.RunIdemixMTTest ORG_HYPERLEDGER_FABRIC_SDKTEST_RUNIDEMIXMTTEST

    private static final Pattern orgPat = Pattern.compile("^" + Pattern.quote("org") + "([^\\.]+)\\.mspid$");

    private FabricConfig(){
        InputStream configProps;
        try{
            configProps = ClassLoader.getSystemResourceAsStream("network.properties");
            sdkProperties.load(configProps);
        }catch (IOException e){
            logger.error("config file not exist");
        }finally {
            defaultProperty(INVOKEWAITTIME, "32000");
            defaultProperty(DEPLOYWAITTIME, "120000");
            defaultProperty(PROPOSALWAITTIME, "120000");
            defaultProperty(RUNIDEMIXMTTEST, "false");
            defaultProperty(RUNSERVICEDISCOVERYIT, "false");

            for (Map.Entry<Object, Object> x: sdkProperties.entrySet()){
                final String key = x.getKey() + "";
                final String val = x.getValue() + "";


            }
        }
    }

    private static void defaultProperty(String key, String value) {
        String ret = System.getProperty(key);
        if (ret != null) {
            sdkProperties.put(key, ret);
        } else {
            String envKey = key.toUpperCase().replaceAll("\\.", "_");
            ret = System.getenv(envKey);
            if (null != ret) {
                sdkProperties.put(key, ret);
            } else {
                if (null == sdkProperties.getProperty(key) && value != null) {
                    sdkProperties.put(key, value);
                }

            }

        }
    }
}
