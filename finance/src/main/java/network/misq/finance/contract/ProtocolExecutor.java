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

package network.misq.finance.contract;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Executes the given protocol.
 */
@Slf4j
public class ProtocolExecutor implements Protocol.Listener {
    @Getter
    protected final Protocol protocol;

    public ProtocolExecutor(Protocol protocol) {
        this.protocol = protocol;

        protocol.addListener(this);
    }

    public void start() {
        protocol.start();
    }

    @Override
    public void onStateChange(Protocol.State state) {
        log.info("{}: {}", protocol.getContract().getMyRole().name(), state);
    }
}