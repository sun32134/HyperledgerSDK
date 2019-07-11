package config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.Channel.PeerOptions;
import org.hyperledger.fabric.sdk.Peer.PeerRole;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.NetworkConfigurationException;
import org.hyperledger.fabric.sdk.helper.Utils;
import org.hyperledger.fabric.sdk.identity.X509Enrollment;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric.sdk.security.CryptoSuite.Factory;
import org.yaml.snakeyaml.Yaml;

public class ReadConfig {
    private final JsonObject jsonConfig;
    private ReadConfig.OrgInfo clientOrganization;
    private Map<String, ReadConfig.Node> orderers;
    private Map<String, ReadConfig.Node> peers;
    private Map<String, ReadConfig.Node> eventHubs;
    private Map<String, ReadConfig.OrgInfo> organizations;
    private static final Log logger = LogFactory.getLog(ReadConfig.class);
    private static Map<PeerRole, String> roleNameRemapHash = new HashMap<PeerRole, String>() {
        {
            this.put(PeerRole.SERVICE_DISCOVERY, "discover");
        }
    };

    public Collection<String> getPeerNames() {
        return (Collection)(this.peers == null ? Collections.EMPTY_SET : new HashSet(this.peers.keySet()));
    }

    public Collection<String> getOrdererNames() {
        return (Collection)(this.orderers == null ? Collections.EMPTY_SET : new HashSet(this.orderers.keySet()));
    }

    public Collection<String> getEventHubNames() {
        return (Collection)(this.eventHubs == null ? Collections.EMPTY_SET : new HashSet(this.eventHubs.keySet()));
    }

    private Properties getNodeProperties(String type, String name, Map<String, ReadConfig.Node> nodes) throws InvalidArgumentException {
        if (Utils.isNullOrEmpty(name)) {
            throw new InvalidArgumentException("Parameter name is null or empty.");
        } else {
            ReadConfig.Node node = (ReadConfig.Node)nodes.get(name);
            if (node == null) {
                throw new InvalidArgumentException(String.format("%s %s not found.", type, name));
            } else {
                return null == node.properties ? new Properties() : (Properties)node.properties.clone();
            }
        }
    }

    private void setNodeProperties(String type, String name, Map<String, ReadConfig.Node> nodes, Properties properties) throws InvalidArgumentException {
        if (Utils.isNullOrEmpty(name)) {
            throw new InvalidArgumentException("Parameter name is null or empty.");
        } else if (properties == null) {
            throw new InvalidArgumentException("Parameter properties is null.");
        } else {
            ReadConfig.Node node = (ReadConfig.Node)nodes.get(name);
            if (node == null) {
                throw new InvalidArgumentException(String.format("%S %s not found.", type, name));
            } else {
                Properties ourCopyProps = new Properties();
                ourCopyProps.putAll(properties);
                node.properties = ourCopyProps;
            }
        }
    }

    public Properties getPeerProperties(String name) throws InvalidArgumentException {
        return this.getNodeProperties("Peer", name, this.peers);
    }

    public Properties getOrdererProperties(String name) throws InvalidArgumentException {
        return this.getNodeProperties("Orderer", name, this.orderers);
    }

    public Properties getEventHubsProperties(String name) throws InvalidArgumentException {
        return this.getNodeProperties("EventHub", name, this.eventHubs);
    }

    public void setPeerProperties(String name, Properties properties) throws InvalidArgumentException {
        this.setNodeProperties("Peer", name, this.peers, properties);
    }

    public void setOrdererProperties(String name, Properties properties) throws InvalidArgumentException {
        this.setNodeProperties("Orderer", name, this.orderers, properties);
    }

    public void setEventHubProperties(String name, Properties properties) throws InvalidArgumentException {
        this.setNodeProperties("EventHub", name, this.eventHubs, properties);
    }


    private ReadConfig(JsonObject jsonConfig) throws InvalidArgumentException, NetworkConfigurationException {
        this.jsonConfig = jsonConfig;
        String configName = getJsonValueAsString(jsonConfig.get("name"));
        if (configName != null && !configName.isEmpty()) {
            String configVersion = getJsonValueAsString(jsonConfig.get("version"));
            if (configVersion != null && !configVersion.isEmpty()) {
                this.createAllPeers();
                this.createAllOrderers();
                Map<String, JsonObject> foundCertificateAuthorities = this.findCertificateAuthorities();
                this.createAllOrganizations(foundCertificateAuthorities);
                JsonObject jsonClient = getJsonObject(jsonConfig, "client");
                String orgName = jsonClient == null ? null : getJsonValueAsString((JsonValue)jsonClient.get("organization"));
                if (orgName != null && !orgName.isEmpty()) {
                    this.clientOrganization = this.getOrganizationInfo(orgName);
                    if (this.clientOrganization == null) {
                        throw new InvalidArgumentException("Client organization " + orgName + " is not defined");
                    }
                } else {
                    throw new InvalidArgumentException("A client organization must be specified");
                }
            } else {
                throw new InvalidArgumentException("Network config must have a version");
            }
        } else {
            throw new InvalidArgumentException("Network config must have a name");
        }
    }

    public static ReadConfig fromYamlFile(File configFile) throws InvalidArgumentException, IOException, NetworkConfigurationException {
        return fromFile(configFile, false);
    }

    public static ReadConfig fromJsonFile(File configFile) throws InvalidArgumentException, IOException, NetworkConfigurationException {
        return fromFile(configFile, true);
    }

    public static ReadConfig fromYamlStream(InputStream configStream) throws InvalidArgumentException, NetworkConfigurationException {
        logger.trace("ReadConfig.fromYamlStream...");
        if (configStream == null) {
            throw new InvalidArgumentException("configStream must be specified");
        } else {
            Yaml yaml = new Yaml();
            Map<String, Object> map = (Map)yaml.load(configStream);
            JsonObjectBuilder builder = Json.createObjectBuilder(map);
            JsonObject jsonConfig = builder.build();
            return fromJsonObject(jsonConfig);
        }
    }

    public static ReadConfig fromJsonStream(InputStream configStream) throws InvalidArgumentException, NetworkConfigurationException {
        logger.trace("ReadConfig.fromJsonStream...");
        if (configStream == null) {
            throw new InvalidArgumentException("configStream must be specified");
        } else {
            JsonReader reader = Json.createReader(configStream);
            Throwable var2 = null;

            ReadConfig var4;
            try {
                JsonObject jsonConfig = (JsonObject)reader.read();
                var4 = fromJsonObject(jsonConfig);
            } catch (Throwable var13) {
                var2 = var13;
                throw var13;
            } finally {
                if (reader != null) {
                    if (var2 != null) {
                        try {
                            reader.close();
                        } catch (Throwable var12) {
                            var2.addSuppressed(var12);
                        }
                    } else {
                        reader.close();
                    }
                }

            }

            return var4;
        }
    }

    public static ReadConfig fromJsonObject(JsonObject jsonConfig) throws InvalidArgumentException, NetworkConfigurationException {
        if (jsonConfig == null) {
            throw new InvalidArgumentException("jsonConfig must be specified");
        } else {
            if (logger.isTraceEnabled()) {
                logger.trace(String.format("ReadConfig.fromJsonObject: %s", jsonConfig.toString()));
            }

            return load(jsonConfig);
        }
    }

    private static ReadConfig fromFile(File configFile, boolean isJson) throws InvalidArgumentException, IOException, NetworkConfigurationException {
        if (configFile == null) {
            throw new InvalidArgumentException("configFile must be specified");
        } else {
            if (logger.isTraceEnabled()) {
                logger.trace(String.format("ReadConfig.fromFile: %s  isJson = %b", configFile.getAbsolutePath(), isJson));
            }

            InputStream stream = new FileInputStream(configFile);
            Throwable var4 = null;

            ReadConfig config;
            try {
                config = isJson ? fromJsonStream(stream) : fromYamlStream(stream);
            } catch (Throwable var13) {
                var4 = var13;
                throw var13;
            } finally {
                if (stream != null) {
                    if (var4 != null) {
                        try {
                            stream.close();
                        } catch (Throwable var12) {
                            var4.addSuppressed(var12);
                        }
                    } else {
                        stream.close();
                    }
                }

            }

            return config;
        }
    }

    private static ReadConfig load(JsonObject jsonConfig) throws InvalidArgumentException, NetworkConfigurationException {
        if (jsonConfig == null) {
            throw new InvalidArgumentException("config must be specified");
        } else {
            return new ReadConfig(jsonConfig);
        }
    }

    public ReadConfig.OrgInfo getClientOrganization() {
        return this.clientOrganization;
    }

    public ReadConfig.OrgInfo getOrganizationInfo(String orgName) {
        return (ReadConfig.OrgInfo)this.organizations.get(orgName);
    }

    public Collection<ReadConfig.OrgInfo> getOrganizationInfos() {
        return Collections.unmodifiableCollection(this.organizations.values());
    }

    public ReadConfig.UserInfo getPeerAdmin() throws NetworkConfigurationException {
        return this.getPeerAdmin(this.clientOrganization.getName());
    }

    public ReadConfig.UserInfo getPeerAdmin(String orgName) throws NetworkConfigurationException {
        ReadConfig.OrgInfo org = this.getOrganizationInfo(orgName);
        if (org == null) {
            throw new NetworkConfigurationException(String.format("Organization %s is not defined", orgName));
        } else {
            return org.getPeerAdmin();
        }
    }

    public Map<String, Node> getChannelPeers(String channelName) throws NetworkConfigurationException {
        JsonObject channels = getJsonObject(this.jsonConfig, "channels");
        Map<String, Node> channelPeers = new HashMap<>();
        if(channels != null){
            JsonObject jsonChannel = getJsonObject(channels, channelName);
            // TODO: 从json对象中拿到peer值
            System.out.println(jsonChannel.toString());
            JsonObject jsonPeers = jsonChannel.getJsonObject("peers");
            assert jsonPeers != null;
            Iterator iterator = jsonPeers.entrySet().iterator();

            while (iterator.hasNext()){
                Entry<String, JsonValue> entry = (Entry)iterator.next();
                String peerName = entry.getKey();
                JsonObject jsonPeer = getJsonValueAsObject((JsonValue)entry.getValue());
                if (jsonPeer == null) {
                    throw new NetworkConfigurationException(String.format("Error loading config. Invalid peer entry: %s", peerName));
                }
                ReadConfig.Node peer = this.peers.get(peerName);

                if (peer == null) {
                    throw new NetworkConfigurationException(String.format("Error loading config. Invalid peer entry: %s", peerName));
                }
                else {
                    channelPeers.put(peerName, peer);
                }
            }
        }
        return channelPeers;
    }

    public Map<String, Node> getChannelOrderer(String channelName) throws NetworkConfigurationException {
        JsonObject channels = getJsonObject(this.jsonConfig, "channels");
        Map<String, Node> channelOrderers = new HashMap<>();
        if(channels != null){
            JsonObject jsonChannel = getJsonObject(channels, channelName);
            // TODO: 从json对象中拿到Orderer值
            System.out.println(jsonChannel.toString());
            JsonArray jsonOrderers = getJsonValueAsArray(jsonChannel.get("orderers"));
            assert jsonOrderers != null;
            Iterator iterator = jsonOrderers.iterator();

            while (iterator.hasNext()){
                JsonValue jsonValue = (JsonValue) iterator.next();
                String ordererName = getJsonValueAsString(jsonValue);
                ReadConfig.Node orderer = this.orderers.get(ordererName);
                if (orderer == null) {
                    throw new NetworkConfigurationException(String.format("Error loading config. Invalid orderer entry: %s", ordererName));
                }
                else {
                    channelOrderers.put(ordererName, orderer);
                }
            }
        }
        return channelOrderers;
    }

    public Channel loadChannel(HFClient client, String channelName) throws NetworkConfigurationException {
        if (logger.isTraceEnabled()) {
            logger.trace(String.format("ReadConfig.loadChannel: %s", channelName));
        }

        Channel channel = null;
        JsonObject channels = getJsonObject(this.jsonConfig, "channels");
        if (channels != null) {
            JsonObject jsonChannel = getJsonObject(channels, channelName);
            if (jsonChannel != null) {
                channel = client.getChannel(channelName);
                if (channel != null) {
                    throw new NetworkConfigurationException(String.format("Channel %s is already configured in the client!", channelName));
                } else {
                    channel = this.reconstructChannel(client, channelName, jsonChannel);
                    return channel;
                }
            } else {
                Set<String> channelNames = this.getChannelNames();
                if (channelNames.isEmpty()) {
                    throw new NetworkConfigurationException("Channel configuration has no channels defined.");
                } else {
                    StringBuilder sb = new StringBuilder(1000);
                    channelNames.forEach((s) -> {
                        if (sb.length() != 0) {
                            sb.append(", ");
                        }

                        sb.append(s);
                    });
                    throw new NetworkConfigurationException(String.format("Channel %s not found in configuration file. Found channel names: %s ", channelName, sb.toString()));
                }
            }
        } else {
            throw new NetworkConfigurationException("Channel configuration has no channels defined.");
        }
    }

    private void createAllOrderers() throws NetworkConfigurationException {
        if (this.orderers != null) {
            throw new NetworkConfigurationException("INTERNAL ERROR: orderers has already been initialized!");
        } else {
            this.orderers = new HashMap();
            JsonObject jsonOrderers = getJsonObject(this.jsonConfig, "orderers");
            if (jsonOrderers != null) {
                Iterator var2 = jsonOrderers.entrySet().iterator();

                while(var2.hasNext()) {
                    Entry<String, JsonValue> entry = (Entry)var2.next();
                    String ordererName = (String)entry.getKey();
                    JsonObject jsonOrderer = getJsonValueAsObject((JsonValue)entry.getValue());
                    if (jsonOrderer == null) {
                        throw new NetworkConfigurationException(String.format("Error loading config. Invalid orderer entry: %s", ordererName));
                    }

                    ReadConfig.Node orderer = this.createNode(ordererName, jsonOrderer, "url");
                    if (orderer == null) {
                        throw new NetworkConfigurationException(String.format("Error loading config. Invalid orderer entry: %s", ordererName));
                    }

                    this.orderers.put(ordererName, orderer);
                }
            }

        }
    }

    private void createAllPeers() throws NetworkConfigurationException {
        if (this.peers != null) {
            throw new NetworkConfigurationException("INTERNAL ERROR: peers has already been initialized!");
        } else if (this.eventHubs != null) {
            throw new NetworkConfigurationException("INTERNAL ERROR: eventHubs has already been initialized!");
        } else {
            this.peers = new HashMap();
            this.eventHubs = new HashMap();
            JsonObject jsonPeers = getJsonObject(this.jsonConfig, "peers");
            if (jsonPeers != null) {
                Iterator var2 = jsonPeers.entrySet().iterator();

                while(var2.hasNext()) {
                    Entry<String, JsonValue> entry = (Entry)var2.next();
                    String peerName = (String)entry.getKey();
                    JsonObject jsonPeer = getJsonValueAsObject((JsonValue)entry.getValue());
                    if (jsonPeer == null) {
                        throw new NetworkConfigurationException(String.format("Error loading config. Invalid peer entry: %s", peerName));
                    }

                    ReadConfig.Node peer = this.createNode(peerName, jsonPeer, "url");
                    if (peer == null) {
                        throw new NetworkConfigurationException(String.format("Error loading config. Invalid peer entry: %s", peerName));
                    }

                    this.peers.put(peerName, peer);
                    ReadConfig.Node eventHub = this.createNode(peerName, jsonPeer, "eventUrl");
                    if (null != eventHub) {
                        this.eventHubs.put(peerName, eventHub);
                    }
                }
            }

        }
    }

    private Map<String, JsonObject> findCertificateAuthorities() throws NetworkConfigurationException {
        Map<String, JsonObject> ret = new HashMap();
        JsonObject jsonCertificateAuthorities = getJsonObject(this.jsonConfig, "certificateAuthorities");
        if (null != jsonCertificateAuthorities) {
            Iterator var3 = jsonCertificateAuthorities.entrySet().iterator();

            while(var3.hasNext()) {
                Entry<String, JsonValue> entry = (Entry)var3.next();
                String name = (String)entry.getKey();
                JsonObject jsonCA = getJsonValueAsObject((JsonValue)entry.getValue());
                if (jsonCA == null) {
                    throw new NetworkConfigurationException(String.format("Error loading config. Invalid CA entry: %s", name));
                }

                ret.put(name, jsonCA);
            }
        }

        return ret;
    }

    private void createAllOrganizations(Map<String, JsonObject> foundCertificateAuthorities) throws NetworkConfigurationException {
        if (this.organizations != null) {
            throw new NetworkConfigurationException("INTERNAL ERROR: organizations has already been initialized!");
        } else {
            this.organizations = new HashMap();
            JsonObject jsonOrganizations = getJsonObject(this.jsonConfig, "organizations");
            if (jsonOrganizations != null) {
                Iterator var3 = jsonOrganizations.entrySet().iterator();

                while(var3.hasNext()) {
                    Entry<String, JsonValue> entry = (Entry)var3.next();
                    String orgName = (String)entry.getKey();
                    JsonObject jsonOrg = getJsonValueAsObject((JsonValue)entry.getValue());
                    if (jsonOrg == null) {
                        throw new NetworkConfigurationException(String.format("Error loading config. Invalid Organization entry: %s", orgName));
                    }

                    ReadConfig.OrgInfo org = this.createOrg(orgName, jsonOrg, foundCertificateAuthorities);
                    this.organizations.put(orgName, org);
                }
            }

        }
    }

    private Channel reconstructChannel(HFClient client, String channelName, JsonObject jsonChannel) throws NetworkConfigurationException {
        Channel channel = null;

        try {
            channel = client.newChannel(channelName);
            JsonArray ordererNames = getJsonValueAsArray((JsonValue)jsonChannel.get("orderers"));
            boolean foundOrderer = false;
            if (ordererNames != null) {
                for(Iterator var7 = ordererNames.iterator(); var7.hasNext(); foundOrderer = true) {
                    JsonValue jsonVal = (JsonValue)var7.next();
                    String ordererName = getJsonValueAsString(jsonVal);
                    Orderer orderer = this.getOrderer(client, ordererName);
                    if (orderer == null) {
                        throw new NetworkConfigurationException(String.format("Error constructing channel %s. Orderer %s not defined in configuration", channelName, ordererName));
                    }

                    channel.addOrderer(orderer);
                }
            }

            JsonObject jsonPeers = getJsonObject(jsonChannel, "peers");
            boolean foundPeer = false;
            Peer peer;
            PeerOptions peerOptions;
            if (jsonPeers != null) {
                for(Iterator var22 = jsonPeers.entrySet().iterator(); var22.hasNext(); channel.addPeer(peer, peerOptions)) {
                    Entry<String, JsonValue> entry = (Entry)var22.next();
                    String peerName = (String)entry.getKey();
                    if (logger.isTraceEnabled()) {
                        logger.trace(String.format("ReadConfig.reconstructChannel: Processing peer %s", peerName));
                    }

                    JsonObject jsonPeer = getJsonValueAsObject((JsonValue)entry.getValue());
                    if (jsonPeer == null) {
                        throw new NetworkConfigurationException(String.format("Error constructing channel %s. Invalid peer entry: %s", channelName, peerName));
                    }

                    peer = this.getPeer(client, peerName);
                    if (peer == null) {
                        throw new NetworkConfigurationException(String.format("Error constructing channel %s. Peer %s not defined in configuration", channelName, peerName));
                    }

                    peerOptions = PeerOptions.createPeerOptions();
                    PeerRole[] var15 = PeerRole.values();
                    int var16 = var15.length;

                    for(int var17 = 0; var17 < var16; ++var17) {
                        PeerRole peerRole = var15[var17];
                        setPeerRole(channelName, peerOptions, jsonPeer, peerRole);
                    }

                    foundPeer = true;
                    EventHub eventHub = this.getEventHub(client, peerName);
                    if (eventHub != null) {
                        channel.addEventHub(eventHub);
                        if (peerOptions.getPeerRoles() == null) {
                            peerOptions.setPeerRoles(EnumSet.of(PeerRole.ENDORSING_PEER, PeerRole.CHAINCODE_QUERY, PeerRole.LEDGER_QUERY));
                        }
                    }
                }
            }

            if (!foundPeer) {
                throw new NetworkConfigurationException(String.format("Error constructing channel %s. At least one peer must be specified", channelName));
            } else {
                return channel;
            }
        } catch (InvalidArgumentException var19) {
            throw new IllegalArgumentException(var19);
        }
    }

    private static void setPeerRole(String channelName, PeerOptions peerOptions, JsonObject jsonPeer, PeerRole role) throws NetworkConfigurationException {
        String propName = roleNameRemap(role);
        JsonValue val = (JsonValue)jsonPeer.get(propName);
        if (val != null) {
            Boolean isSet = getJsonValueAsBoolean(val);
            if (isSet == null) {
                throw new NetworkConfigurationException(String.format("Error constructing channel %s. Role %s has invalid boolean value: %s", channelName, propName, val.toString()));
            }

            if (isSet) {
                peerOptions.addPeerRole(role);
            }
        }

    }

    private static String roleNameRemap(PeerRole peerRole) {
        String remap = roleNameRemapHash.get(peerRole);
        return remap == null ? peerRole.getPropertyName() : remap;
    }

    private Orderer getOrderer(HFClient client, String ordererName) throws InvalidArgumentException {
        Orderer orderer = null;
        ReadConfig.Node o = this.orderers.get(ordererName);
        if (o != null) {
            orderer = client.newOrderer(o.getName(), o.getUrl(), o.getProperties());
        }

        return orderer;
    }

    private ReadConfig.Node createNode(String nodeName, JsonObject jsonNode, String urlPropName) throws NetworkConfigurationException {
        String url = jsonNode.getString(urlPropName, (String)null);
        if (url == null) {
            return null;
        } else {
            Properties props = extractProperties(jsonNode, "grpcOptions");
            if (null != props) {
                String value = props.getProperty("grpc.keepalive_time_ms");
                if (null != value) {
                    props.remove("grpc.keepalive_time_ms");
                    props.put("grpc.NettyChannelBuilderOption.keepAliveTime", new Object[]{new Long(value), TimeUnit.MILLISECONDS});
                }

                value = props.getProperty("grpc.keepalive_timeout_ms");
                if (null != value) {
                    props.remove("grpc.keepalive_timeout_ms");
                    props.put("grpc.NettyChannelBuilderOption.keepAliveTimeout", new Object[]{new Long(value), TimeUnit.MILLISECONDS});
                }
            }

            this.getTLSCerts(nodeName, jsonNode, props);
            return new ReadConfig.Node(nodeName, url, props);
        }
    }

    private void getTLSCerts(String nodeName, JsonObject jsonOrderer, Properties props) {
        JsonObject jsonTlsCaCerts = getJsonObject(jsonOrderer, "tlsCACerts");
        if (jsonTlsCaCerts != null) {
            String pemFilename = getJsonValueAsString((JsonValue)jsonTlsCaCerts.get("path"));
            String pemBytes = getJsonValueAsString((JsonValue)jsonTlsCaCerts.get("pem"));
            if (pemFilename != null) {
                props.put("pemFile", pemFilename);
            }

            if (pemBytes != null) {
                props.put("pemBytes", pemBytes.getBytes());
            }
        }

    }

    private ReadConfig.OrgInfo createOrg(String orgName, JsonObject jsonOrg, Map<String, JsonObject> foundCertificateAuthorities) throws NetworkConfigurationException {
        String msgPrefix = String.format("Organization %s", orgName);
        String mspId = getJsonValueAsString((JsonValue)jsonOrg.get("mspid"));
        ReadConfig.OrgInfo org = new ReadConfig.OrgInfo(orgName, mspId);
        JsonArray jsonPeers = getJsonValueAsArray((JsonValue)jsonOrg.get("peers"));
        String signedCert;
        if (jsonPeers != null) {
            Iterator var8 = jsonPeers.iterator();

            while(var8.hasNext()) {
                JsonValue peer = (JsonValue)var8.next();
                signedCert = getJsonValueAsString(peer);
                if (signedCert != null) {
                    org.addPeerName(signedCert);
                }
            }
        }

        JsonArray jsonCertificateAuthorities = getJsonValueAsArray((JsonValue)jsonOrg.get("certificateAuthorities"));
        String caName;
        if (jsonCertificateAuthorities != null) {
            Iterator var17 = jsonCertificateAuthorities.iterator();

            while(var17.hasNext()) {
                JsonValue jsonCA = (JsonValue)var17.next();
                caName = getJsonValueAsString(jsonCA);
                if (caName != null) {
                    JsonObject jsonObject = (JsonObject)foundCertificateAuthorities.get(caName);
                    if (jsonObject == null) {
                        throw new NetworkConfigurationException(String.format("%s: Certificate Authority %s is not defined", msgPrefix, caName));
                    }

                    org.addCertificateAuthority(this.createCA(caName, jsonObject, org));
                }
            }
        }

        String adminPrivateKeyString = extractPemString(jsonOrg, "adminPrivateKey", msgPrefix);
        signedCert = extractPemString(jsonOrg, "signedCert", msgPrefix);
        if (!Utils.isNullOrEmpty(adminPrivateKeyString) && !Utils.isNullOrEmpty(signedCert)) {
            caName = null;

            PrivateKey privateKey;
            try {
                privateKey = getPrivateKeyFromString(adminPrivateKeyString);
            } catch (IOException var15) {
                throw new NetworkConfigurationException(String.format("%s: Invalid private key", msgPrefix), var15);
            }

            try {
                org.peerAdmin = new ReadConfig.UserInfo(Factory.getCryptoSuite(), mspId, "PeerAdmin_" + mspId + "_" + orgName, (String)null);
            } catch (Exception var14) {
                throw new NetworkConfigurationException(var14.getMessage(), var14);
            }

            org.peerAdmin.setEnrollment(new X509Enrollment(privateKey, signedCert));
        }

        return org;
    }

    private static PrivateKey getPrivateKeyFromString(String data) throws IOException {
        Reader pemReader = new StringReader(data);
        PEMParser pemParser = new PEMParser(pemReader);
        Throwable var4 = null;

        PrivateKeyInfo pemPair;
        try {
            pemPair = (PrivateKeyInfo)pemParser.readObject();
        } catch (Throwable var13) {
            var4 = var13;
            throw var13;
        } finally {
            if (pemParser != null) {
                if (var4 != null) {
                    try {
                        pemParser.close();
                    } catch (Throwable var12) {
                        var4.addSuppressed(var12);
                    }
                } else {
                    pemParser.close();
                }
            }

        }

        return (new JcaPEMKeyConverter()).getPrivateKey(pemPair);
    }

    private static String extractPemString(JsonObject json, String fieldName, String msgPrefix) throws NetworkConfigurationException {
        String path = null;
        String pemString = null;
        JsonObject jsonField = getJsonValueAsObject((JsonValue)json.get(fieldName));
        if (jsonField != null) {
            path = getJsonValueAsString((JsonValue)jsonField.get("path"));
            pemString = getJsonValueAsString((JsonValue)jsonField.get("pem"));
        }

        if (path != null && pemString != null) {
            throw new NetworkConfigurationException(String.format("%s should not specify both %s path and pem", msgPrefix, fieldName));
        } else {
            if (path != null) {
                File pemFile = new File(path);
                String fullPathname = pemFile.getAbsolutePath();
                if (!pemFile.exists()) {
                    throw new NetworkConfigurationException(String.format("%s: %s file %s does not exist", msgPrefix, fieldName, fullPathname));
                }

                try {
                    FileInputStream stream = new FileInputStream(pemFile);
                    Throwable var9 = null;

                    try {
                        pemString = IOUtils.toString(stream, "UTF-8");
                    } catch (Throwable var19) {
                        var9 = var19;
                        throw var19;
                    } finally {
                        if (stream != null) {
                            if (var9 != null) {
                                try {
                                    stream.close();
                                } catch (Throwable var18) {
                                    var9.addSuppressed(var18);
                                }
                            } else {
                                stream.close();
                            }
                        }

                    }
                } catch (IOException var21) {
                    throw new NetworkConfigurationException(String.format("Failed to read file: %s", fullPathname), var21);
                }
            }

            return pemString;
        }
    }

    private ReadConfig.CAInfo createCA(String name, JsonObject jsonCA, ReadConfig.OrgInfo org) throws NetworkConfigurationException {
        String url = getJsonValueAsString((JsonValue)jsonCA.get("url"));
        Properties httpOptions = extractProperties(jsonCA, "httpOptions");
        String enrollId = null;
        String enrollSecret = null;
        List<JsonObject> registrars = getJsonValueAsList((JsonValue)jsonCA.get("registrar"));
        List<ReadConfig.UserInfo> regUsers = new LinkedList();
        if (registrars != null) {
            Iterator var10 = registrars.iterator();

            while(var10.hasNext()) {
                JsonObject reg = (JsonObject)var10.next();
                enrollId = getJsonValueAsString((JsonValue)reg.get("enrollId"));
                enrollSecret = getJsonValueAsString((JsonValue)reg.get("enrollSecret"));

                try {
                    regUsers.add(new ReadConfig.UserInfo(Factory.getCryptoSuite(), org.mspId, enrollId, enrollSecret));
                } catch (Exception var13) {
                    throw new NetworkConfigurationException(var13.getMessage(), var13);
                }
            }
        }

        ReadConfig.CAInfo caInfo = new ReadConfig.CAInfo(name, org.mspId, url, regUsers, httpOptions);
        String caName = getJsonValueAsString((JsonValue)jsonCA.get("caName"));
        if (caName != null) {
            caInfo.setCaName(caName);
        }

        Properties properties = new Properties();
        if (null != httpOptions && "false".equals(httpOptions.getProperty("verify"))) {
            properties.setProperty("allowAllHostNames", "true");
        }

        this.getTLSCerts(name, jsonCA, properties);
        caInfo.setProperties(properties);
        return caInfo;
    }

    private static Properties extractProperties(JsonObject json, String fieldName) {
        Properties props = new Properties();
        JsonObject options = getJsonObject(json, fieldName);
        if (options != null) {
            Iterator var4 = options.entrySet().iterator();

            while(var4.hasNext()) {
                Entry<String, JsonValue> entry = (Entry)var4.next();
                String key = (String)entry.getKey();
                JsonValue value = (JsonValue)entry.getValue();
                props.setProperty(key, getJsonValue(value));
            }
        }

        return props;
    }

    private Peer getPeer(HFClient client, String peerName) throws InvalidArgumentException {
        Peer peer = null;
        ReadConfig.Node p = (ReadConfig.Node)this.peers.get(peerName);
        if (p != null) {
            peer = client.newPeer(p.getName(), p.getUrl(), p.getProperties());
        }

        return peer;
    }

    private EventHub getEventHub(HFClient client, String name) throws InvalidArgumentException {
        EventHub ehub = null;
        ReadConfig.Node e = (ReadConfig.Node)this.eventHubs.get(name);
        if (e != null) {
            ehub = client.newEventHub(e.getName(), e.getUrl(), e.getProperties());
        }

        return ehub;
    }

    private static String getJsonValue(JsonValue value) {
        String s = null;
        if (value != null) {
            s = getJsonValueAsString(value);
            if (s == null) {
                s = getJsonValueAsNumberString(value);
            }

            if (s == null) {
                Boolean b = getJsonValueAsBoolean(value);
                if (b != null) {
                    s = b ? "true" : "false";
                }
            }
        }

        return s;
    }

    private static JsonObject getJsonValueAsObject(JsonValue value) {
        return value != null && value.getValueType() == ValueType.OBJECT ? value.asJsonObject() : null;
    }

    private static JsonArray getJsonValueAsArray(JsonValue value) {
        return value != null && value.getValueType() == ValueType.ARRAY ? value.asJsonArray() : null;
    }

    private static List<JsonObject> getJsonValueAsList(JsonValue value) {
        if (value != null) {
            if (value.getValueType() == ValueType.ARRAY) {
                return value.asJsonArray().getValuesAs(JsonObject.class);
            }

            if (value.getValueType() == ValueType.OBJECT) {
                List<JsonObject> ret = new ArrayList();
                ret.add(value.asJsonObject());
                return ret;
            }
        }

        return null;
    }

    private static String getJsonValueAsString(JsonValue value) {
        return value != null && value.getValueType() == ValueType.STRING ? ((JsonString)value).getString() : null;
    }

    private static String getJsonValueAsNumberString(JsonValue value) {
        return value != null && value.getValueType() == ValueType.NUMBER ? value.toString() : null;
    }

    private static Boolean getJsonValueAsBoolean(JsonValue value) {
        if (value != null) {
            if (value.getValueType() == ValueType.TRUE) {
                return true;
            }

            if (value.getValueType() == ValueType.FALSE) {
                return false;
            }
        }

        return null;
    }

    private static JsonObject getJsonObject(JsonObject object, String propName) {
        JsonObject obj = null;
        JsonValue val = (JsonValue)object.get(propName);
        if (val != null && val.getValueType() == ValueType.OBJECT) {
            obj = val.asJsonObject();
        }

        return obj;
    }

    public Set<String> getChannelNames() {
        Set<String> ret = Collections.EMPTY_SET;
        JsonObject channels = getJsonObject(this.jsonConfig, "channels");
        if (channels != null) {
            Set<String> channelNames = channels.keySet();
            if (channelNames != null && !channelNames.isEmpty()) {
                ret = new HashSet(channelNames);
            }
        }

        return (Set)ret;
    }

    public static class CAInfo {
        private final String name;
        private final String url;
        private final Properties httpOptions;
        private final String mspid;
        private String caName;
        private Properties properties;
        private final List<ReadConfig.UserInfo> registrars;

        CAInfo(String name, String mspid, String url, List<ReadConfig.UserInfo> registrars, Properties httpOptions) {
            this.name = name;
            this.url = url;
            this.httpOptions = httpOptions;
            this.registrars = registrars;
            this.mspid = mspid;
        }

        private void setCaName(String caName) {
            this.caName = caName;
        }

        public String getName() {
            return this.name;
        }

        public String getCAName() {
            return this.caName;
        }

        public String getUrl() {
            return this.url;
        }

        public Properties getHttpOptions() {
            return this.httpOptions;
        }

        void setProperties(Properties properties) {
            this.properties = properties;
        }

        public Properties getProperties() {
            return this.properties;
        }

        public Collection<ReadConfig.UserInfo> getRegistrars() {
            return new LinkedList(this.registrars);
        }
    }

    public static class OrgInfo {
        private final String name;
        private final String mspId;
        private final List<String> peerNames = new ArrayList();
        private final List<ReadConfig.CAInfo> certificateAuthorities = new ArrayList();
        private ReadConfig.UserInfo peerAdmin;

        OrgInfo(String orgName, String mspId) {
            this.name = orgName;
            this.mspId = mspId;
        }

        private void addPeerName(String peerName) {
            this.peerNames.add(peerName);
        }

        private void addCertificateAuthority(ReadConfig.CAInfo ca) {
            this.certificateAuthorities.add(ca);
        }

        public String getName() {
            return this.name;
        }

        public String getMspId() {
            return this.mspId;
        }

        public List<String> getPeerNames() {
            return this.peerNames;
        }

        public List<ReadConfig.CAInfo> getCertificateAuthorities() {
            return this.certificateAuthorities;
        }

        public ReadConfig.UserInfo getPeerAdmin() {
            return this.peerAdmin;
        }
    }

    public static class UserInfo implements User {
        protected String name;
        protected String enrollSecret;
        protected String mspid;
        private Set<String> roles;
        private String account;
        private String affiliation;
        private Enrollment enrollment;
        private CryptoSuite suite;

        public void setName(String name) {
            this.name = name;
        }

        public void setEnrollSecret(String enrollSecret) {
            this.enrollSecret = enrollSecret;
        }

        public String getMspid() {
            return this.mspid;
        }

        public void setMspid(String mspid) {
            this.mspid = mspid;
        }

        public void setRoles(Set<String> roles) {
            this.roles = roles;
        }

        public void setAccount(String account) {
            this.account = account;
        }

        public void setAffiliation(String affiliation) {
            this.affiliation = affiliation;
        }

        public void setEnrollment(Enrollment enrollment) {
            this.enrollment = enrollment;
        }

        UserInfo(CryptoSuite suite, String mspid, String name, String enrollSecret) {
            this.suite = suite;
            this.name = name;
            this.enrollSecret = enrollSecret;
            this.mspid = mspid;
        }

        public String getEnrollSecret() {
            return this.enrollSecret;
        }

        public String getName() {
            return this.name;
        }

        public Set<String> getRoles() {
            return this.roles;
        }

        public String getAccount() {
            return this.account;
        }

        public String getAffiliation() {
            return this.affiliation;
        }

        public Enrollment getEnrollment() {
            return this.enrollment;
        }

        public String getMspId() {
            return this.mspid;
        }
    }

    public class Node {
        private final String name;
        private final String url;
        private Properties properties;

        Node(String name, String url, Properties properties) {
            this.url = url;
            this.name = name;
            this.properties = properties;
        }

        public String getName() {
            return this.name;
        }

        public String getUrl() {
            return this.url;
        }

        public Properties getProperties() {
            return this.properties;
        }
    }
}
