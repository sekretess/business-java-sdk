# sekretess-java-sdk

Java SDK for businesses to send encrypted messages via the Sekretess platform.

This SDK provides a secure, Signal-based messaging client that allows businesses to send private and advertisement messages to consumers. All communication is end-to-end encrypted using the Signal protocol.

## Key Features

- **Signal Protocol Integration**: Uses libsignal for secure end-to-end encryption
- **Session Management**: Automatic session creation and management with consumers
- **Group Messaging**: Support for advertisement messages to groups of consumers
- **Token-based Authentication**: Mutual TLS authentication with automatic token refresh
- **Persistent Store**: Extensible storage interfaces for identity keys, sessions, and group data

## Maven Dependency

```xml
<dependency>
    <groupId>io.sekretess</groupId>
    <artifactId>business-java-sdk</artifactId>
    <version>${sekretess.version}</version>
</dependency>
```

> **Note**: Replace `${sekretess.version}` with the latest version available on [Maven Central](https://central.sonatype.com/artifact/io.sekretess/business-java-sdk).

## Requirements

- Java 21+
- Maven 3.6+

## Quick Start

### 1. Implement Required Store Interfaces

The SDK requires you to implement three interfaces for persistent storage of cryptographic material:

#### `IdentityStore`
Stores business identity keys and registration IDs:
```java
public interface IdentityStore {
    IdentityKeyData loadIdentity(String username);
    void saveIdentity(String username, byte[] serializedIdentityKeyPair, int registrationId);
}
```

#### `SessionStore`
Stores encrypted session records with consumers:
```java
public interface SessionStore {
    void saveSession(String name, int deviceId, String base64EncodedRecord);
    List<SessionData> loadAll();
    void deleteSession(String name);
}
```

#### `GroupSessionStore`
Stores group sender key data for advertisement messages:
```java
public interface GroupSessionStore {
    void saveGroupSession(String name, int deviceId, String distributionId, String sessionRecord);
    void saveSendDistributionMessage(String name, int deviceId, String distributionId, String businessDistributionMessage);
    GroupSessionData loadGroupSession(String name);
}
```

#### In-Memory Store Implementations (for testing)

Here are minimal in-memory implementations for testing:

**InMemoryIdentityStore**
```java
import io.sekretess.store.IdentityStore;
import io.sekretess.model.IdentityKeyData;

import java.util.HashMap;
import java.util.Map;

public class InMemoryIdentityStore implements IdentityStore {
    private final Map<String, IdentityKeyData> identities = new HashMap<>();

    @Override
    public IdentityKeyData loadIdentity(String username) {
        return identities.get(username);
    }

    @Override
    public void saveIdentity(String username, byte[] serializedIdentityKeyPair, int registrationId) {
        identities.put(username, new IdentityKeyData(username, serializedIdentityKeyPair, registrationId));
    }
}
```

**InMemorySessionStore**
```java
import io.sekretess.store.SessionStore;
import io.sekretess.model.SessionData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InMemorySessionStore implements SessionStore {
    private final Map<String, SessionData> store = new HashMap<>();

    @Override
    public void saveSession(String name, int deviceId, String base64EncodedRecord) {
        store.put(name, new SessionData(name, deviceId, base64EncodedRecord));
    }

    @Override
    public List<SessionData> loadAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public void deleteSession(String name) {
        store.remove(name);
    }
}
```

**InMemoryGroupSessionStore**
```java
import io.sekretess.store.GroupSessionStore;
import io.sekretess.model.GroupSessionData;

import java.util.HashMap;
import java.util.Map;

public class InMemoryGroupSessionStore implements GroupSessionStore {
    private final Map<String, GroupSessionData> store = new HashMap<>();

    @Override
    public void saveGroupSession(String name, int deviceId, String distributionId, String sessionRecord) {
        GroupSessionData existing = store.get(name);
        String existingDistMessage = existing != null ? existing.businessDistributionMessage() : null;
        store.put(name, new GroupSessionData(name, deviceId, distributionId, sessionRecord, existingDistMessage));
    }

    @Override
    public void saveSendDistributionMessage(String name, int deviceId, String distributionId, String businessDistributionMessage) {
        store.put(name, new GroupSessionData(name, deviceId, distributionId, null, businessDistributionMessage));
    }

    @Override
    public GroupSessionData loadGroupSession(String name) {
        return store.get(name);
    }
}
```

> For production use, implement these interfaces backed by a database (PostgreSQL, MySQL, etc.).

### 2. Use `SekretessManagerFactory` to Initialize

The **only supported way** to create a `SekretessManager` is through `SekretessManagerFactory.createSekretessManager()`:

```java
import io.sekretess.manager.SekretessManagerFactory;
import io.sekretess.manager.SekretessManager;

// Create your store implementations (database, cache, etc.)
IdentityStore identityStore = new YourIdentityStoreImpl();
SessionStore sessionStore = new YourSessionStoreImpl();
GroupSessionStore groupSessionStore = new YourGroupSessionStoreImpl();

try {
    // Initialize manager through factory - this sets up Signal protocol and stores
    SekretessManager manager = SekretessManagerFactory.createSekretessManager(
        identityStore,
        sessionStore,
        groupSessionStore
    );
    
    // Now you can send messages
    manager.sendMessageToConsumer("Hello, consumer!", "consumer-id-123");
    
} catch (Exception e) {
    e.printStackTrace();
}
```

## Public API

### `SekretessManagerFactory`
**Location**: `io.sekretess.manager.SekretessManagerFactory`

The only public factory for creating initialized managers.

**Method**:
```java
public static SekretessManager createSekretessManager(
    IdentityStore identityStore,
    SessionStore sessionStore,
    GroupSessionStore groupSessionStore
) throws InvalidKeyException
```

Returns a fully initialized `SekretessManager` with:
- Signal protocol store wired up
- Identity keys loaded or generated (new if first initialization)
- Sessions loaded from persistent storage
- Group sender key distribution setup

### `SekretessManager`
**Location**: `io.sekretess.manager.SekretessManager`

High-level API for sending encrypted messages.

**Public Methods**:

- `void sendMessageToConsumer(String message, String consumer)` — Send a private message to a consumer (automatically creates sessions if needed, encrypts using Signal protocol)
- `void sendAdsMessage(String message)` — Send an advertisement message to all subscribers
- `void deleteUserSession(String user)` — Delete a consumer session (used when revoking access or resetting)

## Environment Configuration

The SDK reads the following environment variables at runtime:

| Variable | Purpose |
|----------|---------|
| `SEKRETESS_BUSINESS_SERVER_URL` | Business server URL (used for sending messages) |
| `IDENTITY_PROVIDER_URL` | Identity provider token endpoint (for authentication) |
| `USER_CERTIFICATE_PATH` | Path to client X.509 certificate (PEM/DER) |
| `USER_CERTIFICATE_KEY` | Path to client private key (PEM, may be encrypted) |
| `USER_CERTIFICATE_PASSWORD` | Password for encrypted private key |
| `BUSINESS_USER_NAME` | Business identifier (used as local Signal protocol address) |

Example:
```bash
export SEKRETESS_BUSINESS_SERVER_URL=https://business.sekretess.io
export IDENTITY_PROVIDER_URL=https://idp.example.com/token
export USER_CERTIFICATE_PATH=/path/to/cert.pem
export USER_CERTIFICATE_KEY=/path/to/key.pem
export USER_CERTIFICATE_PASSWORD=my-key-password
export BUSINESS_USER_NAME=my-business-id
```



## Build

Build the project:
```bash
mvn clean package
```

Generate Javadoc:
```bash
mvn javadoc:javadoc
```

## Important Notes


✅ **DO** use:
- `SekretessManagerFactory.createSekretessManager(...)` — the only supported factory method

## License

This project is licensed under the GNU Affero General Public License v3.0 (AGPL-3.0). See the `LICENSE` file for details.

## Support

For issues, questions, or contributions, please refer to the project repository or contact the development team.
