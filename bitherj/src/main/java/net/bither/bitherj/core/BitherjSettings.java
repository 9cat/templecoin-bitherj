/*
* Copyright 2014 http://Bither.net
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package net.bither.bitherj.core;

import net.bither.bitherj.utils.Utils;

import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;

public class BitherjSettings {

    public static final boolean LOG_DEBUG = true;

    public static final boolean DEV_DEBUG = false;
    public static final int PROTOCOL_VERSION = 70001;
    public static final int MIN_PROTO_VERSION = 70001;

    public static final int MAX_TX_SIZE = 100000;

    /**
     * The alert signing key originally owned by Satoshi, and now passed on to Gavin along with a few others.
     */
    public static final byte[] SATOSHI_KEY = Hex.decode("04fc9702847840aaf195de8442ebecedf5b095cdbb9bc716bda9110971b28a49e0ead8564ff0db22209e0374782c093bb899692d524e9d6a6956e7c5ecbcd68284");


    /**
     * The string returned by getId() for the main, production network where people trade things.
     */
    public static final String ID_MAINNET = "org.bitcoin.production";
    /**
     * The string returned by getId() for the testnet.
     */
    public static final String ID_TESTNET = "org.bitcoin.test";
    /**
     * Unit test network.
     */
    public static final String ID_UNITTESTNET = "net.bither.bitherj.unittest";

    /**
     * The string used by the payment protocol to represent the main net.
     */
    public static final String PAYMENT_PROTOCOL_ID_MAINNET = "main";
    /**
     * The string used by the payment protocol to represent the test net.
     */
    public static final String PAYMENT_PROTOCOL_ID_TESTNET = "test";


    public static final BigInteger proofOfWorkLimit = Utils.decodeCompactBits(0x1d00ffffL);
    public static final int port = 8333;
    public static final long packetMagic = 0xf9beb4d9L;
    public static final int addressHeader = 0;
    public static final int p2shHeader = 5;
    public static final int dumpedPrivateKeyHeader = 128;
    public static final int TARGET_TIMESPAN = 14 * 24 * 60 * 60;  // 2 weeks per difficulty cycle, on average.
    public static final int TARGET_SPACING = 10 * 60;  // 10 minutes per block.
    public static final int INTERVAL = TARGET_TIMESPAN / TARGET_SPACING;
    public static final int interval = INTERVAL;
    public static final int targetTimespan = TARGET_TIMESPAN;
    public static final byte[] alertSigningKey = SATOSHI_KEY;

    public static final long TX_UNCONFIRMED = Long.MAX_VALUE;

    public static final int PROTOCOL_TIMEOUT = 30000;

    public static final String id = ID_MAINNET;

    /**
     * The depth of blocks required for a coinbase transaction to be spendable.
     */
    public static final int spendableCoinbaseDepth = 100;
    public static final int subsidyDecreaseBlockCount = 210000;

    public static final int[] acceptableAddressCodes = new int[]{addressHeader, p2shHeader};
    public static final String[] dnsSeeds = new String[]{
            "seed.bitcoin.sipa.be",        // Pieter Wuille
            "dnsseed.bluematt.me",         // Matt Corallo
            "seed.bitcoinstats.com",       // Chris Decker
            "bitseed.xf2.org",
            "seed.bitcoinstats.com",
            "seed.bitnodes.io"
    };

    public static final long MAX_MONEY = 21000000l * 100000000l;

    public static final byte[] GENESIS_BLOCK_HASH = Utils.reverseBytes(Hex.decode("000000000019d6689c085ae165831e934ff763ae46a2a6c172b3f1b60a8ce26f"));
    public static final int BLOCK_DIFFICULTY_INTERVAL = 2016;
    public static final int BITCOIN_REFERENCE_BLOCK_HEIGHT = 250000;
    public static final int MaxPeerConnections = 6;
    public static final int MaxPeerBackgroundConnections = 2;

    public static enum AppMode {
        COLD, HOT
    }

    public static final String PRIVATE_KEY_FILE_NAME = "%s/%s.key";
    public static final String WATCH_ONLY_FILE_NAME = "%s/%s.pub";

    public static final boolean ensureMinRequiredFee = true;

    public enum TransactionFeeMode {
        Normal(10000), Low(1000);

        private int satoshi;

        TransactionFeeMode(int satoshi) {
            this.satoshi = satoshi;
        }

        public int getMinFeeSatoshi() {
            return satoshi;
        }
    }
}
