package io.sekretess.store;

import io.sekretess.model.GroupSessionData;
import io.sekretess.model.IdentityKeyData;
import io.sekretess.model.SessionData;
import org.signal.libsignal.protocol.*;
import org.signal.libsignal.protocol.ecc.ECKeyPair;
import org.signal.libsignal.protocol.groups.GroupSessionBuilder;
import org.signal.libsignal.protocol.groups.state.SenderKeyRecord;
import org.signal.libsignal.protocol.message.SenderKeyDistributionMessage;
import org.signal.libsignal.protocol.state.SessionRecord;
import org.signal.libsignal.protocol.util.KeyHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.List;
import java.util.UUID;

public class SekretessStoreFactory {

    private static final Logger logger = LoggerFactory.getLogger(SekretessStoreFactory.class);
    private static final String username = System.getenv("BUSINESS_USER_NAME");

    public static SekretessSignalProtocolStore initialize(
            IdentityStore identityStore,
            SessionStore sessionStore,
            GroupSessionStore groupSessionStore
    ) throws InvalidKeyException {

        IdentityKeyData identityData = identityStore.loadIdentity(username);
        SekretessSignalProtocolStore sekretessSignalProtocolStore;

        if (identityData == null) {
            logger.info("No identityKeys saved for the user: {}. Creating new one!", username);
            ECKeyPair ecKeyPair = ECKeyPair.generate();
            IdentityKeyPair identityKeyPair = new IdentityKeyPair(new IdentityKey(ecKeyPair.getPublicKey()), ecKeyPair.getPrivateKey());
            int registrationId = KeyHelper.generateRegistrationId(false);

            identityStore.saveIdentity(username, identityKeyPair.serialize(), registrationId);

            sekretessSignalProtocolStore = new SekretessSignalProtocolStore(identityKeyPair, registrationId, sessionStore, groupSessionStore);
            GroupSessionBuilder businessSessionBuilder = new GroupSessionBuilder(sekretessSignalProtocolStore);
            SignalProtocolAddress businessAddress = new SignalProtocolAddress(username, 1);
            String distributionId = UUID.randomUUID().toString();
            SenderKeyDistributionMessage sentBusinessDistributionMessage = businessSessionBuilder.create(businessAddress, UUID.fromString(distributionId));
            groupSessionStore.saveSendDistributionMessage(username, 1, distributionId, Base64.getEncoder().encodeToString(sentBusinessDistributionMessage.serialize()));
        } else {
            logger.info("Found identityKeys for the user: {}. Will re-use it", username);
            IdentityKeyPair identityKeyPair = new IdentityKeyPair(identityData.serializedIdentityKeyPair());
            int registrationId = identityData.registrationId();
            sekretessSignalProtocolStore = new SekretessSignalProtocolStore(identityKeyPair, registrationId, sessionStore, groupSessionStore);
            List<SessionData> sessions = sessionStore.loadAll();
            sessions.forEach(sessionData -> {
                SignalProtocolAddress signalProtocolAddress = new SignalProtocolAddress(sessionData.name(), sessionData.deviceId());
                SessionRecord sessionRecord = null;
                try {
                    sessionRecord = new SessionRecord(Base64.getDecoder().decode(sessionData.base64SessionRecord()));
                    sekretessSignalProtocolStore.storeSession(signalProtocolAddress, sessionRecord);
                } catch (InvalidMessageException e) {
                    logger.error("Exception happened when to create session record from DB! {}", e.getMessage(), e);
                }
            });
            GroupSessionData groupSessionData = groupSessionStore.loadGroupSession(username);
            if (groupSessionData == null) {
                GroupSessionBuilder businessSessionBuilder = new GroupSessionBuilder(sekretessSignalProtocolStore);
                SignalProtocolAddress businessAddress = new SignalProtocolAddress(username, 1);
                String distributionId = UUID.randomUUID().toString();
                SenderKeyDistributionMessage sentBusinessDistributionMessage = businessSessionBuilder.create(businessAddress, UUID.fromString(distributionId));
                groupSessionStore.saveSendDistributionMessage(username, 1, distributionId, Base64.getEncoder().encodeToString(sentBusinessDistributionMessage.serialize()));
                groupSessionData = new GroupSessionData(username, 1, distributionId, Base64.getEncoder().encodeToString(sentBusinessDistributionMessage.serialize()));
            }
            SignalProtocolAddress signalProtocolAddress = new SignalProtocolAddress(groupSessionData.name(), groupSessionData.deviceId());
            SenderKeyRecord senderKeyRecord = null;
            try {
                senderKeyRecord = new SenderKeyRecord(Base64.getDecoder().decode(groupSessionData.sessionRecord()));
                sekretessSignalProtocolStore.storeSenderKey(signalProtocolAddress, UUID.fromString(groupSessionData.distributionId()), senderKeyRecord);
            } catch (Exception e) {
                logger.error("Exception happened when creating senderKeyRecord! {}", e.getMessage(), e);
            }
        }
        return sekretessSignalProtocolStore;
    }
}
