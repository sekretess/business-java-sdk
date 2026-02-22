package io.sekretess.demobusinesssdk;

import io.sekretess.manager.SekretessManagerFactory;
import io.sekretess.manager.SekretessManager;
import io.sekretess.store.GroupSessionStore;
import io.sekretess.store.IdentityStore;
import io.sekretess.store.SessionStore;

import java.util.Scanner;

public class MyClass {

    public static void main(String[] args) {
        IdentityStore identityStore = new InMemoryIdentityStore();
        SessionStore sessionStore = new InMemorySessionStore();
        GroupSessionStore groupSessionStore = new InMemoryGroupSessionStore();

        try {
            SekretessManager manager = SekretessManagerFactory.createSekretessManager(
                    identityStore,
                    sessionStore,
                    groupSessionStore
            );
            manager.sendMessageToConsumer("Hello, newbusiness!", "some-user");
            // For broadcasting
//            while(true){
//                Scanner scanner = new Scanner(System.in);
//                scanner.nextLine();
//                manager.sendAdsMessage("Hello, newbusiness!");
//
//            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
