package io.sekretess.manager;

import io.sekretess.client.SekretessServerClient;
import io.sekretess.client.response.ConsumerKeysResponse;
import io.sekretess.client.response.SendAdsMessageResponse;
import io.sekretess.client.response.SendMessageResponse;
import io.sekretess.exception.MessageSendException;
import io.sekretess.exception.PrekeyBundleException;
import io.sekretess.exception.SessionCreationException;
import io.sekretess.model.GroupSessionData;
import io.sekretess.store.SekretessSignalProtocolStore;
import io.sekretess.util.MessageType;
import org.signal.libsignal.protocol.*;
import org.signal.libsignal.protocol.ecc.ECPublicKey;
import org.signal.libsignal.protocol.groups.GroupCipher;
import org.signal.libsignal.protocol.kem.KEMPublicKey;
import org.signal.libsignal.protocol.message.CiphertextMessage;
import org.signal.libsignal.protocol.message.PreKeySignalMessage;
import org.signal.libsignal.protocol.message.SenderKeyDistributionMessage;
import org.signal.libsignal.protocol.state.PreKeyBundle;
import org.signal.libsignal.protocol.state.SessionRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class SekretessManager {

    private static final Logger logger = LoggerFactory.getLogger(SekretessManager.class);

    private final SekretessSignalProtocolStore signalProtocolStore;
    private final SekretessServerClient sekretessServerClient;
    private final String userName = System.getenv("BUSINESS_USER_NAME");

    public SekretessManager(SekretessSignalProtocolStore signalProtocolStore) {
        this(signalProtocolStore, new SekretessServerClient());
    }


    SekretessManager(SekretessSignalProtocolStore signalProtocolStore,
                    SekretessServerClient serverClient) {
        this.signalProtocolStore = signalProtocolStore;
        this.sekretessServerClient = serverClient;
    }

    private void sendMessage(String message, String consumer, MessageType messageType) throws SessionCreationException, MessageSendException, PrekeyBundleException {
        SignalProtocolAddress consumerAddress = new SignalProtocolAddress(consumer, 123);
        SessionRecord sessionRecord = signalProtocolStore.loadSession(consumerAddress);
        if (sessionRecord == null) {
            logger.info("No session available for consumer: {}", consumer);
            SessionBuilder sessionBuilder = new SessionBuilder(signalProtocolStore, consumerAddress);
            PreKeyBundle consumerPrekeyBundle = getConsumerPrekeyBundle(consumer);
            try {
                sessionBuilder.process(consumerPrekeyBundle);
                sessionRecord = signalProtocolStore.loadSession(consumerAddress);
            } catch (InvalidKeyException | UntrustedIdentityException e) {
                throw new SessionCreationException("Exception happened when trying to create session with consumer: " + consumer + " , " + e.getMessage());
            }
        }

        SessionCipher sessionCipher = new SessionCipher(signalProtocolStore, consumerAddress);
        try {
            CiphertextMessage ciphertextMessage = sessionCipher.encrypt(message.getBytes());
            PreKeySignalMessage signalMessage = new PreKeySignalMessage(ciphertextMessage.serialize());
            SendMessageResponse sendMessageResponse = sekretessServerClient.sendMessage(Base64.getEncoder().encodeToString(signalMessage.serialize()), consumer, messageType.name());
            IdentityKey idenKey = new IdentityKey(Base64.getDecoder().decode(sendMessageResponse.userIK()));
            if (!Arrays.equals(sessionRecord.getRemoteIdentityKey().getPublicKey().serialize(), idenKey.getPublicKey().serialize())) {
                signalProtocolStore.deleteSession(consumerAddress);
                handleRetrySendMessage(message, consumer, sendMessageResponse.subscribedToAdMessages(), messageType);
            }
        } catch (Exception e) {
            logger.error("Exception happened when trying to send message! {}", e.getMessage(), e);
            throw new MessageSendException("Exception happened when trying to send message! " + e.getMessage());
        }
    }

    public void sendMessageToConsumer(String message, String consumer) throws SessionCreationException, MessageSendException, PrekeyBundleException {
        this.sendMessage(message, consumer, MessageType.PRIVATE);
    }

    private void handleRetrySendMessage(String message, String consumer, boolean isSubscribedToAdMessages, MessageType messageType) throws PrekeyBundleException {
        logger.info("Received to retry message to consumer: {}", consumer);
        SignalProtocolAddress consumerAddress = new SignalProtocolAddress(consumer, 123);
        SessionBuilder sessionBuilder = new SessionBuilder(signalProtocolStore, consumerAddress);
        PreKeyBundle consumerPrekeyBundle = getConsumerPrekeyBundle(consumer);
        signalProtocolStore.saveIdentity(consumerAddress, consumerPrekeyBundle.getIdentityKey());
        try {
            sessionBuilder.process(consumerPrekeyBundle);
            signalProtocolStore.loadSession(consumerAddress);
        } catch (InvalidKeyException | UntrustedIdentityException e) {
            logger.error("Exception happened when trying to create session with consumer: {} , {}", consumer, e.getMessage(), e);
            throw new RuntimeException(e);
        }

        SessionCipher sessionCipher = new SessionCipher(signalProtocolStore, consumerAddress);
        try {
            CiphertextMessage ciphertextMessage = sessionCipher.encrypt(message.getBytes());
            PreKeySignalMessage signalMessage = new PreKeySignalMessage(ciphertextMessage.serialize());
            sekretessServerClient.sendMessage(Base64.getEncoder().encodeToString(signalMessage.serialize()), consumer, messageType.name());
        } catch (Exception e) {
            logger.error("Exception happened when trying to send message! {}", e.getMessage(), e);
        }

        if (isSubscribedToAdMessages) {
            sendSenderKeyDistributionMessage(consumer);
        }
    }

    private void sendSenderKeyDistributionMessage(String consumer) {
        logger.info("Request received to subscribe ads messages from consumer: {}", consumer);
        try {
            SenderKeyDistributionMessage sentBusinessDistributionMessage = null;
            GroupSessionData groupSessionModel = Optional.ofNullable(signalProtocolStore.getGroupSessionStore().loadGroupSession(userName)).orElseThrow();
            if (groupSessionModel.sessionRecord() != null) {
                sentBusinessDistributionMessage =
                        new SenderKeyDistributionMessage(Base64.getDecoder().decode(groupSessionModel.businessDistributionMessage()));
            } else {
                throw new RuntimeException("Group session not found for business!");
            }
            sendMessage(Base64.getEncoder().encodeToString(sentBusinessDistributionMessage.serialize()), consumer, MessageType.KEY_DIST);

        } catch (Exception e) {
            logger.error("Exception happened when trying to send senderkeydistribution message! {}", e.getMessage(), e);
        }
    }

    public void deleteUserSession(String user) {
        try {
            SignalProtocolAddress userAddress = new SignalProtocolAddress(user, 123);
            signalProtocolStore.deleteSession(userAddress);
        } catch (Exception e) {
            logger.error("Exception happened when deleting user session! {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }


    public void sendAdsMessage(String message) throws MessageSendException {
        try {
            GroupSessionData groupSessionModel = Optional.ofNullable(signalProtocolStore.getGroupSessionStore().loadGroupSession(userName)).orElseThrow();
            GroupCipher groupCipher = new GroupCipher(this.signalProtocolStore, new SignalProtocolAddress(userName, 1));
            CiphertextMessage ciphertextMessage =
                    groupCipher.encrypt(UUID.fromString(groupSessionModel.distributionId()), message.getBytes());
            List<SendAdsMessageResponse> sendAdsMessageResponses = sekretessServerClient.sendAdsMessage(Base64.getEncoder().encodeToString(ciphertextMessage.serialize()), userName);
            if (!sendAdsMessageResponses.isEmpty()) {
                sendAdsMessageResponses.forEach(sendAdsMessageResponse -> {
                    sendSenderKeyDistributionMessage(sendAdsMessageResponse.consumerName());
                });
            }

        } catch (Exception e) {
            logger.error("Exception happened when sending ads message! {}", e.getMessage(), e);
            throw new MessageSendException("Exception happened when sending ads message! " + e.getMessage());
        }
    }


    private PreKeyBundle getConsumerPrekeyBundle(String consumer) throws PrekeyBundleException {
        try {
            ConsumerKeysResponse consumerKeysResponse = sekretessServerClient.getConsumerKeys(consumer);
            String signedPreKey = consumerKeysResponse.spk();
            String[] preKeyRecords = consumerKeysResponse.opk().split(":");
            String preKeyRecordValue = preKeyRecords[1];
            int preKeyId = Integer.parseInt(preKeyRecords[0]);
            int regId = consumerKeysResponse.regID();
            String identityKey = consumerKeysResponse.ik();
            int signedPreKeyId = Integer.parseInt(consumerKeysResponse.spkID());
            byte[] signedPreKeySignature = Base64.getDecoder().decode(consumerKeysResponse.spkSignature());
            String pqSignedPrekey = consumerKeysResponse.pqSpk();
            int pqSignedPrekeyId = Integer.parseInt(consumerKeysResponse.pqSpkID());
            byte[] pqSignedPrekeySignature = Base64.getDecoder().decode(consumerKeysResponse.pqSpkSignature());

            ECPublicKey signPrekey = new ECPublicKey(Base64.getDecoder().decode(signedPreKey));
            ECPublicKey preKeyRecord = new ECPublicKey(Base64.getDecoder().decode(preKeyRecordValue));
            IdentityKey idenKey = new IdentityKey(Base64.getDecoder().decode(identityKey));
            KEMPublicKey kemPublicKey = new KEMPublicKey(Base64.getDecoder().decode(pqSignedPrekey));
            return new PreKeyBundle(
                    regId,
                    1,
                    preKeyId,
                    preKeyRecord,
                    signedPreKeyId,
                    signPrekey,
                    signedPreKeySignature,
                    idenKey,
                    pqSignedPrekeyId,
                    kemPublicKey,
                    pqSignedPrekeySignature);

        } catch (Exception e) {
            throw new PrekeyBundleException("Exception happened when trying to get consumer prekey bundle: " + consumer + " , " + e.getMessage());
        }
    }

}
