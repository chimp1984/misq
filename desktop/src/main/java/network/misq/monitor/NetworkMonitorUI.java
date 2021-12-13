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

package network.misq.monitor;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import network.misq.common.util.CompletableFutureUtils;
import network.misq.desktop.common.threading.UIThread;
import network.misq.network.NetworkService;
import network.misq.network.p2p.ServiceNode;
import network.misq.network.p2p.node.Address;
import network.misq.network.p2p.node.Connection;
import network.misq.network.p2p.node.ConnectionListener;
import network.misq.network.p2p.node.Node;
import network.misq.network.p2p.services.mesh.MeshService;
import network.misq.network.p2p.services.mesh.monitor.NetworkMonitor;
import network.misq.network.p2p.services.mesh.peers.PeerGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
public class NetworkMonitorUI extends Application {
    private NetworkMonitor networkMonitor;
    private FlowPane seedsPane, nodesPane;
    private TextArea nodeInfoTextArea, eventTextArea;
    private final List<NetworkService> seedNetworkServices = new ArrayList<>();
    private final List<NetworkService> nodeNetworkServices = new ArrayList<>();
    private final Map<Address, String> connectionInfoByAddress = new HashMap<>();

    @Override
    public void start(Stage primaryStage) {
        networkMonitor = new NetworkMonitor();

        String bgStyle = "-fx-background-color: #dadada";
        String seedStyle = "-fx-background-color: #80afa1";

        Insets bgPadding = new Insets(10, 10, 10, 10);
        Insets labelPadding = new Insets(4, -20, 0, 0);
        int nodeWidth = 50;
        double stageWidth = 2000;
        double availableWidth = stageWidth - 2 * bgPadding.getLeft() - 2 * bgPadding.getRight();
        int nodesPerRow = (int) (availableWidth / nodeWidth);
        stageWidth = nodesPerRow * nodeWidth + 2 * bgPadding.getLeft() + 2 * bgPadding.getRight();

        seedsPane = new FlowPane();
        seedsPane.setStyle(seedStyle);
        seedsPane.setPadding(bgPadding);

        nodesPane = new FlowPane();
        nodesPane.setStyle(bgStyle);
        nodesPane.setPadding(bgPadding);

        HBox modesBox = new HBox(20);
        modesBox.setStyle(bgStyle);
        modesBox.setPadding(bgPadding);

        Label modeLabel = new Label("Mode: ");
        modeLabel.setPadding(labelPadding);
        modesBox.getChildren().add(modeLabel);

        HBox actionBox = new HBox(20);
        actionBox.setStyle(bgStyle);
        actionBox.setPadding(bgPadding);

        Label actionLabel = new Label("Info: ");
        actionLabel.setPadding(labelPadding);
        actionBox.getChildren().add(actionLabel);


        HBox infoBox = new HBox(20);
        infoBox.setStyle(bgStyle);
        infoBox.setPadding(bgPadding);

        Label infoLabel = new Label("Node info: ");
        infoLabel.setPadding(labelPadding);
        infoBox.getChildren().add(actionLabel);

        nodeInfoTextArea = new TextArea();
        nodeInfoTextArea.setEditable(false);
        nodeInfoTextArea.setMinHeight(900);
        nodeInfoTextArea.setMinWidth(900);
        nodeInfoTextArea.setFont(Font.font("Courier", FontWeight.NORMAL, FontPosture.REGULAR, 13));
        nodeInfoTextArea.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.DELETE) {
                nodeInfoTextArea.clear();
            }
        });

        eventTextArea = new TextArea();
        eventTextArea.setEditable(false);
        eventTextArea.setMinHeight(nodeInfoTextArea.getMinHeight());
        eventTextArea.setMinWidth(nodeInfoTextArea.getMinWidth());
        eventTextArea.setFont(Font.font("Courier", FontWeight.NORMAL, FontPosture.REGULAR, 13));

        ContextMenu contextMenu = new ContextMenu();
        MenuItem clear = new MenuItem("Clear");
        clear.setOnAction((event) -> {
            eventTextArea.clear();
        });
        contextMenu.getItems().add(clear);
        eventTextArea.setContextMenu(contextMenu);

        infoBox.getChildren().addAll(nodeInfoTextArea, eventTextArea);

        VBox vBox = new VBox(20);
        vBox.setPadding(bgPadding);
        vBox.getChildren().addAll(seedsPane, nodesPane, infoBox);

        ScrollPane scrollPane = new ScrollPane(vBox);
        scrollPane.setFitToWidth(true);

        Scene scene = new Scene(scrollPane, stageWidth, 1400);
        primaryStage.setTitle("Network simulator");
        primaryStage.setScene(scene);
        primaryStage.show();

        start();
    }


    private void start() {
        bootstrap(networkMonitor.getSeedAddresses(networkMonitor.getTransportType()), "Seed ", seedNetworkServices, seedsPane)
                .thenCompose(res1 -> {
                    return bootstrap(networkMonitor.getNodeAddresses(), "Node ", nodeNetworkServices, nodesPane);
                }).whenComplete((r, t) -> {
                    log.info("All nodes bootstrapped");
                });
    }

    private void setupConnectionListener(NetworkService networkService) {
        networkService.findDefaultNode(networkMonitor.getTransportType()).ifPresent(node -> {
            node.addConnectionListener(new ConnectionListener() {
                @Override
                public void onConnection(Connection connection) {
                    UIThread.run(() -> {
                        String dir = connection.isOutboundConnection() ? " -> " : " <- ";
                        String info = "\n+ onConnection " + node + dir + connection.getPeerAddress();
                        eventTextArea.appendText(info);

                        Address key = node.findMyAddress().get();
                        String prev = connectionInfoByAddress.get(key);
                        if (prev == null) {
                            prev = "";
                        }
                        connectionInfoByAddress.put(key, prev + info);
                    });
                }

                @Override
                public void onDisconnect(Connection connection) {
                    UIThread.run(() -> {
                        String dir = connection.isOutboundConnection() ? " -> " : " <- ";
                        String info = "\n- onDisconnect " + node + dir + connection.getPeerAddress();
                        eventTextArea.appendText(info);

                        Address key = node.findMyAddress().get();
                        String prev = connectionInfoByAddress.get(key);
                        if (prev == null) {
                            prev = "";
                        }
                        connectionInfoByAddress.put(key, prev + info);
                    });
                }
            });
        });
    }

    private CompletableFuture<Boolean> bootstrap(List<Address> addresses,
                                                 String name,
                                                 List<NetworkService> sink,
                                                 Pane pane) {
        UIThread.run(() -> stopNodes(sink, pane));

        List<CompletableFuture<Boolean>> allFutures = new ArrayList<>();
        addresses.forEach(address -> {
            int port = address.getPort();
            NetworkService networkService = networkMonitor.createNetworkService(port);
            setupConnectionListener(networkService);
            allFutures.add(networkService.bootstrap(port));

            UIThread.run(() -> {
                Button button = new Button(name + port);
                button.setMinWidth(100);
                button.setMaxWidth(button.getMinWidth());
                pane.getChildren().add(button);
                button.setOnAction(e -> onNodeInfo(networkService, port));
                sink.add(networkService);
            });
        });

        return CompletableFutureUtils.allOf(allFutures)
                .thenApply(success -> success.stream().allMatch(e -> e))
                .orTimeout(120, TimeUnit.SECONDS)
                .thenCompose(CompletableFuture::completedFuture);
    }

    private void stopNodes(List<NetworkService> sink, Pane seedsPane) {
        seedsPane.getChildren().clear();
        sink.forEach(e -> {
            CompletableFuture<Void> shutdown = e.shutdown();
            try {
                shutdown.get();
            } catch (InterruptedException | ExecutionException ignore) {
            }
        });
        sink.clear();
    }


    private void onNodeInfo(NetworkService networkService, int port) {
        String connectionMatrix = networkService.findServiceNode(networkMonitor.getTransportType())
                .flatMap(ServiceNode::getMeshService)
                .map(MeshService::getPeerGroup).stream()
                .map(PeerGroup::getConnectionMatrix)
                .findAny()
                .orElse("null");
        nodeInfoTextArea.setText(connectionMatrix);
        ContextMenu contextMenu = new ContextMenu();
        MenuItem stop = new MenuItem("Stop");
        stop.setOnAction((event) -> {
            networkService.shutdown();
            //networkService.findServiceNode(networkMonitor.getTransportType()).ifPresent(e->e.shutdown());
        });
        MenuItem start = new MenuItem("Start");
        start.setOnAction((event) -> {
            // networkService.findServiceNode(networkMonitor.getTransportType()).ifPresent(e->e.bootstrap(port));
            networkService.init();
            networkService.bootstrap(port);
        });
        contextMenu.getItems().addAll(stop, start);
        nodeInfoTextArea.setContextMenu(contextMenu);

        networkService.findDefaultNode(networkMonitor.getTransportType())
                .flatMap(Node::findMyAddress)
                .ifPresent(address -> nodeInfoTextArea.appendText("\nConnections:" + connectionInfoByAddress.get(address)));
    }
}
