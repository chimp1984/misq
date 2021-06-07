/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package network.misq.network;

import lombok.extern.slf4j.Slf4j;
import network.misq.network.p2p.Address;
import network.misq.network.p2p.NetworkConfig;
import network.misq.network.p2p.NetworkId;
import network.misq.network.p2p.P2pService;
import network.misq.network.p2p.data.storage.Storage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class AllNetworksIntegrationTest {
    private P2pService alice1, alice2, bob1, bob2;
    protected final Storage storage = new Storage("");

    private Set<NetworkConfig> getNetNetworkConfigs(Config.Role role, String id, int serverPort) {
        return Set.of(Config.getClearNetNetworkConfig(role, id, serverPort),
                Config.getTorNetworkConfig(role, id, serverPort),
                Config.getI2pNetworkConfig(role, id));
    }

    @Test
    public void testInitializeServer() throws InterruptedException {
        try {
            initializeServer();
        } finally {
            if (alice1 != null) alice1.shutdown();
            if (alice2 != null) alice2.shutdown();
            if (bob1 != null) bob1.shutdown();
            if (bob2 != null) bob2.shutdown();
        }
    }

    public void initializeServer() throws InterruptedException {
        Set<NetworkConfig> netNetworkConfigsAlice1 = getNetNetworkConfigs(Config.Role.Alice, "alice1", 1111);
        Set<NetworkConfig> netNetworkConfigsAlice2 = getNetNetworkConfigs(Config.Role.Alice, "alice2", 1112);
        Set<NetworkConfig> netNetworkConfigsBob1 = getNetNetworkConfigs(Config.Role.Bob, "bob1", 2222);
        Set<NetworkConfig> netNetworkConfigsBob2 = getNetNetworkConfigs(Config.Role.Bob, "bob2", 2223);

        alice1 = new P2pService(netNetworkConfigsAlice1, Config.aliceKeyPairSupplier1);
        alice2 = new P2pService(netNetworkConfigsAlice2, Config.aliceKeyPairSupplier2);
        bob1 = new P2pService(netNetworkConfigsBob1, Config.bobKeyPairSupplier1);
        bob2 = new P2pService(netNetworkConfigsBob2, Config.bobKeyPairSupplier2);

        CountDownLatch serversReadyLatch = new CountDownLatch(4);
        alice1.initializeServer((res, error) -> {
            if (res != null)
                log.error("initializeServer completed: {}", res);
        }).whenComplete((result, throwable) -> {
            assertNotNull(result);
            Assertions.assertTrue(result);
            serversReadyLatch.countDown();
        });
        alice2.initializeServer((res, error) -> {
            if (res != null)
                log.error("initializeServer completed: {}", res);
        }).whenComplete((result, throwable) -> {
            assertNotNull(result);
            Assertions.assertTrue(result);
            serversReadyLatch.countDown();
        });
        bob1.initializeServer((res, error) -> {
            if (res != null)
                log.error("initializeServer completed: {}", res);
        }).whenComplete((result, throwable) -> {
            assertNotNull(result);
            Assertions.assertTrue(result);
            serversReadyLatch.countDown();
        });
        bob2.initializeServer((res, error) -> {
            if (res != null)
                log.error("initializeServer completed: {}", res);
        }).whenComplete((result, throwable) -> {
            assertNotNull(result);
            Assertions.assertTrue(result);
            serversReadyLatch.countDown();
        });

        boolean serversReady = serversReadyLatch.await(2, TimeUnit.MINUTES);
        assertTrue(serversReady);
    }

    @Test
    public void sendMsgWithMultipleIdsOnMultiNetworks() throws InterruptedException {
        initializeServer();

        Set<Address> alice1Addresses = alice1.findMyAddresses();
        Set<Address> alice2Addresses = alice2.findMyAddresses();
        Set<Address> bob1Addresses = bob1.findMyAddresses();
        Set<Address> bob2Addresses = bob2.findMyAddresses();

        String alice1ToBob1Msg = "alice1ToBob1Msg";
        String alice1ToBob2Msg = "alice1ToBob2Msg";
        String alice2ToBob1Msg = "alice2ToBob1Msg";
        String alice2ToBob2Msg = "alice2ToBob2Msg";
        String bob1ToAlice1Msg = "bob1ToAlice1Msg";
        String bob1ToAlice2Msg = "bob1ToAlice2Msg";
        String bob2ToAlice1Msg = "bob2ToAlice1Msg";
        String bob2ToAlice2Msg = "bob2ToAlice2Msg";

        // We get 3 msg for 3 networks and 2 msg per node. With 4 nodes it is in total 4*2*3=24
        CountDownLatch receivedLatch = new CountDownLatch(24);
        alice1.addMessageListener((message, connection) -> {
            assertTrue(message instanceof MockMessage);
            log.error("onMessage alice1 {} {}", message, connection);
            if (bob1Addresses.contains(connection.getPeerAddress())) {
                assertEquals(((MockMessage) message).getMsg(), bob1ToAlice1Msg);
            } else if (bob2Addresses.contains(connection.getPeerAddress())) {
                assertEquals(((MockMessage) message).getMsg(), bob2ToAlice1Msg);
            } else {
                fail();
            }
            receivedLatch.countDown();
        });
        alice2.addMessageListener((message, connection) -> {
            assertTrue(message instanceof MockMessage);
            log.error("onMessage alice2 {} {}", message, connection);
            if (bob1Addresses.contains(connection.getPeerAddress())) {
                assertEquals(((MockMessage) message).getMsg(), bob1ToAlice2Msg);
            } else if (bob2Addresses.contains(connection.getPeerAddress())) {
                assertEquals(((MockMessage) message).getMsg(), bob2ToAlice2Msg);
            } else {
                fail();
            }
            receivedLatch.countDown();
        });

        bob1.addMessageListener((message, connection) -> {
            assertTrue(message instanceof MockMessage);
            log.error("onMessage bob1 {} {}", message, connection);
            if (alice1Addresses.contains(connection.getPeerAddress())) {
                assertEquals(((MockMessage) message).getMsg(), alice1ToBob1Msg);
            } else if (alice2Addresses.contains(connection.getPeerAddress())) {
                assertEquals(((MockMessage) message).getMsg(), alice2ToBob1Msg);
            } else {
                fail();
            }
            receivedLatch.countDown();
        });
        bob2.addMessageListener((message, connection) -> {
            assertTrue(message instanceof MockMessage);
            log.error("onMessage bob2 {} {}", message, connection);
            if (alice1Addresses.contains(connection.getPeerAddress())) {
                assertEquals(((MockMessage) message).getMsg(), alice1ToBob2Msg);
            } else if (alice2Addresses.contains(connection.getPeerAddress())) {
                assertEquals(((MockMessage) message).getMsg(), alice2ToBob2Msg);
            } else {
                fail();
            }
            receivedLatch.countDown();
        });

        CountDownLatch sentLatch = new CountDownLatch(8);
        NetworkId bob1NetworkId = new NetworkId(bob1Addresses, Config.keyPairBob1.getPublic(), "default");
        alice1.confidentialSend(new MockMessage(alice1ToBob1Msg), bob1NetworkId, Config.keyPairAlice1)
                .whenComplete((connection, throwable) -> {
                    if (connection != null) {
                        sentLatch.countDown();
                    } else {
                        fail();
                    }
                });
        NetworkId bob2NetworkId = new NetworkId(bob2Addresses, Config.keyPairBob2.getPublic(), "default");
        alice1.confidentialSend(new MockMessage(alice1ToBob2Msg), bob2NetworkId, Config.keyPairAlice1)
                .whenComplete((connection, throwable) -> {
                    if (connection != null) {
                        sentLatch.countDown();
                    } else {
                        fail();
                    }
                });
        alice2.confidentialSend(new MockMessage(alice2ToBob1Msg), bob1NetworkId, Config.keyPairAlice2)
                .whenComplete((connection, throwable) -> {
                    if (connection != null) {
                        sentLatch.countDown();
                    } else {
                        fail();
                    }
                });
        alice2.confidentialSend(new MockMessage(alice2ToBob2Msg), bob2NetworkId, Config.keyPairAlice2)
                .whenComplete((connection, throwable) -> {
                    if (connection != null) {
                        sentLatch.countDown();
                    } else {
                        fail();
                    }
                });

        NetworkId alice1NetworkId = new NetworkId(alice1Addresses, Config.keyPairAlice1.getPublic(), "default");
        bob1.confidentialSend(new MockMessage(bob1ToAlice1Msg), alice1NetworkId, Config.keyPairBob1)
                .whenComplete((connection, throwable) -> {
                    if (connection != null) {
                        sentLatch.countDown();
                    } else {
                        fail();
                    }
                });
        NetworkId alice2NetworkId = new NetworkId(alice2Addresses, Config.keyPairAlice2.getPublic(), "default");
        bob1.confidentialSend(new MockMessage(bob1ToAlice2Msg), alice2NetworkId, Config.keyPairBob1)
                .whenComplete((connection, throwable) -> {
                    if (connection != null) {
                        sentLatch.countDown();
                    } else {
                        fail();
                    }
                });
        bob2.confidentialSend(new MockMessage(bob2ToAlice1Msg), alice1NetworkId, Config.keyPairBob2)
                .whenComplete((connection, throwable) -> {
                    if (connection != null) {
                        sentLatch.countDown();
                    } else {
                        fail();
                    }
                });
        bob2.confidentialSend(new MockMessage(bob2ToAlice2Msg), alice2NetworkId, Config.keyPairBob2)
                .whenComplete((connection, throwable) -> {
                    if (connection != null) {
                        sentLatch.countDown();
                    } else {
                        fail();
                    }
                });

        boolean allSent = sentLatch.await(2, TimeUnit.MINUTES);
        assertTrue(allSent);
        boolean allReceived = receivedLatch.await(2, TimeUnit.MINUTES);
        assertTrue(allReceived);

        alice1.shutdown();
        alice2.shutdown();
        bob1.shutdown();
        bob2.shutdown();
    }
}
