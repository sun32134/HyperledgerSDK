package client;

import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.exception.ChaincodeEndorsementPolicyParseException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Pattern;


/*
* 处理Chaincode所有与联盟链相关的操作
* */
public class ChaincodeClient {

    /*
    *  在背书节点中安装chaincode
    * */
    public static void installChaincode(HFClient hfClient, Collection<Peer> peers, InstallProposalRequest installProposalRequest) throws InvalidArgumentException, ProposalException {
        Collection<ProposalResponse> responses = hfClient.sendInstallProposal(installProposalRequest, peers);
        checkProposalResponse(responses);
    }

    /*
     * 在背书节点中实例化chaincode
     * */
    public static void instantiateChaincode(Channel channel, Collection<Peer> peers, InstantiateProposalRequest instantiateProposalRequest) throws ChaincodeEndorsementPolicyParseException, IOException, InvalidArgumentException, ProposalException {
        Collection<ProposalResponse> responses = channel.sendInstantiationProposal(instantiateProposalRequest, peers);
        System.out.print("Sending instantiateProposalRequest to all peers with arguments");
        CompletableFuture<BlockEvent.TransactionEvent> cf = channel.sendTransaction(responses);
        System.out.println("Chaincode " + instantiateProposalRequest.getChaincodeName() + " on channel " + channel.getName() + " instantiation " + cf);
        checkProposalResponse(responses);
    }

    /*
    * 调用Chaincode
    * */
    public static void invodeChaincode(Channel channel, TransactionProposalRequest transactionProposalRequest) throws InvalidArgumentException, ProposalException {
        Collection<ProposalResponse> responses = channel.sendTransactionProposal(transactionProposalRequest, channel.getPeers());
        checkProposalResponse(responses);
        CompletableFuture<BlockEvent.TransactionEvent> cf = channel.sendTransaction(responses);
    }

    /*
    * 查询chaincode数据
    * */
    public static void  queryChaincode(Channel channel, QueryByChaincodeRequest queryByChaincodeRequest) throws ProposalException, InvalidArgumentException {
        Collection<ProposalResponse> response = channel.queryByChaincode(queryByChaincodeRequest);
        for (ProposalResponse pres : response) {
            String stringResponse = new String(pres.getChaincodeActionResponsePayload());
            System.out.println(stringResponse);
        }
    }

    private static void checkProposalResponse(Collection<ProposalResponse> responses){
        for (ProposalResponse response : responses) {
            if (response.getStatus() == ProposalResponse.Status.SUCCESS) {
                System.out.printf("Successful proposal response Txid: %s from peer %s", response.getTransactionID(), response.getPeer().getName());
            } else {
                System.out.printf("Failed proposal response Txid: %s from peer %s", response.getTransactionID(), response.getPeer().getName());
            }
        }
    }

    public static String setChaincodeEventListener(Channel channel, String expetedEventName, CountDownLatch latch) throws InvalidArgumentException {
        ChaincodeEventListener chaincodeEventListener = (s, blockEvent, chaincodeEvent) -> {
            // TODO: event处理函数
            System.out.println(chaincodeEvent.getEventName());
            System.out.println(new String(chaincodeEvent.getPayload()));
            latch.countDown();
        };
        String eventListenerHandle = channel.registerChaincodeEventListener(Pattern.compile(".*"),
                Pattern.compile(Pattern.quote(expetedEventName)),
                chaincodeEventListener);
        return eventListenerHandle;
    }

    public static void upgradeChaincode(Channel channel, Collection<Peer> peers, UpgradeProposalRequest upgradeProposalRequest) throws ProposalException, InvalidArgumentException {
        Collection<ProposalResponse> responses = channel.sendUpgradeProposal(upgradeProposalRequest, peers);
        CompletableFuture<BlockEvent.TransactionEvent> cf = channel.sendTransaction(responses);
        System.out.println("Chaincode " + upgradeProposalRequest.getChaincodeName() + " on channel " + channel.getName() + " instantiation " + cf);
        checkProposalResponse(responses);
    }
}
