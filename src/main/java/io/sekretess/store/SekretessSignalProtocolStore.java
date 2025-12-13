package io.sekretess.store;

import io.sekretess.model.GroupSessionData;
import org.signal.libsignal.protocol.IdentityKeyPair;
import org.signal.libsignal.protocol.SignalProtocolAddress;
import org.signal.libsignal.protocol.groups.state.SenderKeyRecord;
import org.signal.libsignal.protocol.state.SessionRecord;
import org.signal.libsignal.protocol.state.impl.InMemorySignalProtocolStore;

import java.util.Base64;
import java.util.UUID;

public class SekretessSignalProtocolStore extends InMemorySignalProtocolStore {

    private final SessionStore sessionStore;
    private final GroupSessionStore groupSessionStore;

    public SekretessSignalProtocolStore(
            IdentityKeyPair identityKeyPair,
            int registrationId,
            SessionStore sessionStore,
            GroupSessionStore groupSessionStore
    ) {
        super(identityKeyPair, registrationId);
        this.sessionStore = sessionStore;
        this.groupSessionStore = groupSessionStore;
    }

    @Override
    public void storeSession(SignalProtocolAddress address, SessionRecord record) {
        super.storeSession(address, record);
        sessionStore.saveSession(address.getName(), address.getDeviceId(), Base64.getEncoder().encodeToString(record.serialize()));
    }

    @Override
    public void deleteSession(SignalProtocolAddress address) {
        super.deleteSession(address);
        sessionStore.deleteSession(address.getName());
    }

    @Override
    public void storeSenderKey(SignalProtocolAddress sender, UUID distributionId, SenderKeyRecord record) {
        super.storeSenderKey(sender, distributionId, record);
        groupSessionStore.saveGroupSession(sender.getName(), sender.getDeviceId(), distributionId.toString(), Base64.getEncoder().encodeToString(record.serialize()));
    }

    public GroupSessionStore getGroupSessionStore() {
        return groupSessionStore;
    }
}
