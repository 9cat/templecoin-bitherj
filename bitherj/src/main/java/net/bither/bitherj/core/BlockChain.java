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

import net.bither.bitherj.db.BlockProvider;
import net.bither.bitherj.db.TxProvider;
import net.bither.bitherj.exception.VerificationException;
import net.bither.bitherj.utils.LogUtil;
import net.bither.bitherj.utils.Utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class BlockChain {
    private static BlockChain uniqueInstance = new BlockChain();
    private static final Logger log = LoggerFactory.getLogger(BlockChain.class);
    protected HashMap<byte[], Block> singleBlocks;
    protected Block lastBlock;
    protected Block lastOrphanBlock;

    BlockChain() {
        BlockProvider.getInstance().cleanOldBlock();

        this.singleBlocks = new HashMap<byte[], Block>();
        this.lastBlock = BlockProvider.getInstance().getLastBlock();
        this.lastOrphanBlock = BlockProvider.getInstance().getLastOrphanBlock();
    }

    public static BlockChain getInstance() {

        return uniqueInstance;
    }

    public void addSPVBlock(Block block) {
        // only none block need add spv block
        if (this.getBlockCount() == 0) {
            block.setMain(true);
            this.addBlock(block);
            this.lastBlock = block;
        }
    }

    public void addBlocks(List<Block> blocks) {
        BlockProvider.getInstance().addBlocks(blocks);
    }

    public Block getLastBlock() {
        return this.lastBlock;
    }

    public Block getBlock(byte[] blockHash) {
        return BlockProvider.getInstance().getBlock(blockHash);
    }

    public int getBlockCount() {
        return BlockProvider.getInstance().getBlockCount();
    }

    public List<byte[]> getBlockLocatorArray() {
        // append 10 most recent block hashes, descending, then continue appending, doubling the step back each time,
        // finishing with the genesis block (top, -1, -2, -3, -4, -5, -6, -7, -8, -9, -11, -15, -23, -39, -71, -135, ..., 0)
        ArrayList<byte[]> locators = new ArrayList<byte[]>();
        int step = 1, start = 0;
        Block b = this.lastBlock;

        while (b != null && b.getBlockNo() > 0) {
            locators.add(b.getBlockHash());
            if (++start >= 10) step *= 2;

            for (int i = 0; b != null && i < step; i++) {
                b = BlockProvider.getInstance().getMainChainBlock(b.getBlockPrev());
            }
        }
        locators.add(BitherjSettings.GENESIS_BLOCK_HASH);

        return locators;
    }

    public boolean rollbackBlock(int blockNo) {
        log.warn("block chain roll back to " + blockNo);
        if (blockNo > this.lastBlock.getBlockNo())
            return false;
        int delta = this.lastBlock.getBlockNo() - blockNo;
        if (delta >= BitherjSettings.BLOCK_DIFFICULTY_INTERVAL || delta >= this.getBlockCount())
            return false;

        List<Block> blocks = BlockProvider.getInstance().getBlocksFrom(blockNo);
        // DDLogWarn(@"roll back block from %d to %d", self.lastBlock.height, blockNo);

        for (Block block : blocks) {
            BlockProvider.getInstance().removeBlock(block.getBlockHash());

            if (block.isMain()) {
                TxProvider.getInstance().unConfirmTxByBlockNo(block.getBlockNo());
            }
        }
        this.lastBlock = BlockProvider.getInstance().getLastBlock();
        return true;
    }

    public int relayedBlockHeadersForMainChain(List<Block> blocks){
        if(blocks == null || blocks.size() == 0){
            return 0;
        }
        ArrayList<Block> blocksToAdd = new ArrayList<Block>();
        Block prev = getLastBlock();
        if(prev == null){
            LogUtil.w(BlockChain.class.getSimpleName(), "pre block is null");
            return 0;
        }
        for(int i = 0; i < blocks.size(); i ++){
            Block block = blocks.get(i);
            if(!Arrays.equals(prev.getBlockHash(), block.getBlockPrev())){
                Block alreadyIn = getBlock(block.getBlockHash());
                if(alreadyIn != null){
                    LogUtil.d(BlockChain.class.getSimpleName(), "Block is already in, No." + alreadyIn.getBlockNo());
                    continue;
                }else{
                    this.singleBlocks.put(block.getBlockHash(), block);
                    break;
                }
            }
            block.setBlockNo(prev.getBlockNo() + 1);
            try {
                block.verifyDifficultyFromPreviousBlock(prev);
            }catch (Exception e){
                e.printStackTrace();
                break;
            }

            block.setMain(true);
            blocksToAdd.add(block);
            prev = block;
        }
        if(blocksToAdd.size() > 0){
            addBlocks(blocksToAdd);
            lastBlock = blocksToAdd.get(blocksToAdd.size() - 1);
        }
        return blocksToAdd.size();
    }

    /*
     * if result is true, means the block is in main chain, if result is false, means the block is single
     * or orphan.
     * */
    public boolean relayedBlock(Block block) throws VerificationException {
        Block prev = BlockProvider.getInstance().getBlock(block.getBlockPrev());

        if (prev == null) {

            LogUtil.d(this.getClass().getSimpleName(), "prev block is null, prev hash is : " + Utils.hashToString(block.getBlockPrev()));
//            DDLogDebug(@"%@:%d relayed orphan block %@, previous %@, last block is %@, height %d", peer.host, peer.port,
//                    block.blockHash, block.prevBlock, self.lastBlock.blockHash, self.lastBlock.height);

            // ignore orphans older than one week ago
//            if (block.blockTime - NSTimeIntervalSince1970 < [NSDate timeIntervalSinceReferenceDate] - ONE_WEEK) return;

            this.singleBlocks.put(block.getBlockPrev(), block);
            return false;
//            // call get blocks, unless we already did with the previous block, or we're still downloading the chain
//            if (self.lastBlock.height >= peer.lastBlock && ![self.lastOrphan.blockHash isEqual:block.prevBlock]) {
//                DDLogDebug(@"%@:%d calling getblocks", peer.host, peer.port);
//                [peer sendGetBlocksMessageWithLocators:[self blockLocatorArray] andHashStop:nil];
//            }
        }

        block.setBlockNo(prev.getBlockNo() + 1);
        //TODO
//        int transitionTime = 0;
//        // hit a difficulty transition, find previous transition time
//        if ((block.getBlockNo() % BitherjSettings.BLOCK_DIFFICULTY_INTERVAL) == 0) {
//            Block b = block;
//            for (int i = 0; b != null && i < BitherjSettings.BLOCK_DIFFICULTY_INTERVAL; i++) {
//                b = BlockProvider.getInstance().getBlock(b.getBlockPrev());
//            }
//            transitionTime = b.getBlockTime();
//        }


        // verify block difficulty
        block.verifyDifficultyFromPreviousBlock(prev);
//        if (!block.verifyDifficultyFromPreviousBlock(prev)) {
//            callback(block, NO);
//            return;
//        }

        boolean result = false;
        if (Arrays.equals(block.getBlockPrev(), this.lastBlock.getBlockHash())) {
            this.extendMainChain(block);
            result = true;
        } else if (this.inMainChain(block)) {
            result = true;
        } else {
            if (block.getBlockNo() <= BitherjSettings.BITCOIN_REFERENCE_BLOCK_HEIGHT) {
                LogUtil.d(this.getClass().getSimpleName(), "block is too old");
                return false;
            }
            if (block.getBlockNo() <= this.lastBlock.getBlockNo()) {
                this.addOrphan(block);
                LogUtil.d(this.getClass().getSimpleName(), "block is orphan");
                return false;
            }

            if (block.getBlockNo() > this.lastBlock.getBlockNo()) {
                Block b = this.getSameParent(block, this.lastBlock);
                this.rollbackBlock(b.getBlockNo());
                LogUtil.d(this.getClass().getSimpleName(), "roll back block from" + b.getBlockNo());
            }
        }
        if (!result)
            LogUtil.d(this.getClass().getSimpleName(), "block is not in main chain");
        return result;
    }

    private void extendMainChain(Block block) {
        if (Arrays.equals(block.getBlockPrev(), this.lastBlock.getBlockHash())) {
            block.setMain(true);
            this.addBlock(block);
            this.lastBlock = block;
        }
    }

    private boolean inMainChain(Block block) {
        Block b = this.lastBlock;
        while (b != null && b.getBlockNo() > block.getBlockNo()) {
            b = BlockProvider.getInstance().getBlock(b.getBlockPrev());
        }
        return b != null && Arrays.equals(b.getBlockHash(), block.getBlockHash());
    }

    private void addBlock(Block block) {
        BlockProvider.getInstance().addBlock(block);
    }

    private void addOrphan(Block block) {
        block.setMain(false);
        this.addBlock(block);
        this.lastOrphanBlock = block;
    }

    private Block getSameParent(Block block1, Block block2) {
        Block b1 = block1;
        Block b2 = block2;

        while (b1 != null && b2 != null && !Arrays.equals(b1.getBlockHash(), b2.getBlockHash())) {
            b1 = BlockProvider.getInstance().getBlock(b1.getBlockPrev());
            if (b1.getBlockNo() < b2.getBlockNo()) {
                b2 = BlockProvider.getInstance().getBlock(b2.getBlockPrev());
            }
        }
        return b1;
    }

    private void forkMainChain(Block forkStartBlock, Block lastBlock) {
        Block b = this.lastBlock;
        Block next = lastBlock;
        while (!Arrays.equals(b.getBlockHash(), forkStartBlock.getBlockHash())) {
            next = BlockProvider.getInstance().getOrphanBlockByPrevHash(b.getBlockPrev());
            BlockProvider.getInstance().updateBlock(b.getBlockHash(), false);
            b = BlockProvider.getInstance().getMainChainBlock(b.getBlockPrev());
            this.lastBlock = b;
        }
        b = next;
        BlockProvider.getInstance().updateBlock(next.getBlockHash(), true);
        this.lastBlock = next;
        while (!Arrays.equals(b.getBlockHash(), lastBlock.getBlockPrev())) {
            BlockProvider.getInstance().updateBlock(b.getBlockHash(), true);
            this.lastBlock = b;
            b = BlockProvider.getInstance().getOrphanBlockByPrevHash(b.getBlockHash());
        }
        lastBlock.setMain(true);
        this.addBlock(lastBlock);
        this.lastBlock = lastBlock;
    }
}
