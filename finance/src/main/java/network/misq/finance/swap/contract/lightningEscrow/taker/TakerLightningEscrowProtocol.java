package network.misq.finance.swap.contract.lightningEscrow.taker;

import network.misq.finance.contract.AssetTransfer;
import network.misq.finance.contract.ManyPartyContract;
import network.misq.finance.swap.contract.lightningEscrow.LightningEscrow;
import network.misq.finance.swap.contract.lightningEscrow.LightningEscrowProtocol;
import network.misq.p2p.P2pService;
import network.misq.p2p.message.Message;
import network.misq.p2p.node.Connection;

import java.util.concurrent.CompletableFuture;

public class TakerLightningEscrowProtocol extends LightningEscrowProtocol {
    public TakerLightningEscrowProtocol(ManyPartyContract contract, P2pService p2pService) {
        super(contract, p2pService, new AssetTransfer.Automatic(), new LightningEscrow());
    }

    @Override
    public CompletableFuture<Boolean> start() {
        return null;
    }

    @Override
    public void onMessage(Message message, Connection connection) {
    }
}