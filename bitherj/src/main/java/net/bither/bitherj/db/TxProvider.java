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

package net.bither.bitherj.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import net.bither.bitherj.BitherjApplication;
import net.bither.bitherj.core.In;
import net.bither.bitherj.core.Out;
import net.bither.bitherj.core.Tx;
import net.bither.bitherj.exception.AddressFormatException;
import net.bither.bitherj.utils.Base58;
import net.bither.bitherj.utils.Sha256Hash;
import net.bither.bitherj.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TxProvider {

    private static TxProvider txProvider = new TxProvider(BitherjApplication.mDbHelper);

    public static TxProvider getInstance() {
        return txProvider;
    }

    private SQLiteOpenHelper mDb;

    public TxProvider(SQLiteOpenHelper db) {
        this.mDb = db;
    }

//    public List<Tx> getTxByAddress(String address) {
//        List<Tx> txItemList = new ArrayList<Tx>();
//        String sql = "select b.* from addresses_txs a, txs b where a.tx_hash=b.tx_hash and a.address='" +
//                address + "' order by b.block_no";
//        SQLiteDatabase db = this.mDb.getReadableDatabase();
//        Cursor c = db.rawQuery(sql, null);
//        try {
//            while (c.moveToNext()) {
//                txItemList.add(applyCursor(c));
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        c.close();
//        return txItemList;
//    }

    public List<Tx> getTxAndDetailByAddress(String address) {
        List<Tx> txItemList = new ArrayList<Tx>();
        HashMap<Sha256Hash, Tx> txDict = new HashMap<Sha256Hash, Tx>();
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        try {
            String sql = "select b.* from addresses_txs a, txs b where a.tx_hash=b.tx_hash and a.address='"
                    + address + "' order by b.block_no ";
            Cursor c = db.rawQuery(sql, null);
            while (c.moveToNext()) {
                Tx txItem = applyCursor(c);
                txItem.setIns(new ArrayList<In>());
                txItem.setOuts(new ArrayList<Out>());
                txItemList.add(txItem);
                txDict.put(new Sha256Hash(txItem.getTxHash()), txItem);
            }
            c.close();

            sql = "select b.* from addresses_txs a, ins b where a.tx_hash=b.tx_hash and a.address=? "
                    + "order by b.tx_hash ,b.in_sn";
            c = db.rawQuery(sql, new String[]{address});
            while (c.moveToNext()) {
                In inItem = applyCursorIn(c);
                Tx tx = txDict.get(new Sha256Hash(inItem.getTxHash()));
                if (tx != null)
                    tx.getIns().add(inItem);
            }
            c.close();

            sql = "select b.* from addresses_txs a, outs b where a.tx_hash=b.tx_hash and a.address=? "
                    + "order by b.tx_hash,b.out_sn";
            c = db.rawQuery(sql, new String[]{address});
            while (c.moveToNext()) {
                Out out = applyCursorOut(c);
                Tx tx = txDict.get(new Sha256Hash(out.getTxHash()));
                if (tx != null)
                    tx.getOuts().add(out);
            }
            c.close();

        } catch (AddressFormatException e) {
            e.printStackTrace();
        }
        return txItemList;
    }

    public List<Tx> getPublishedTxs() {
        List<Tx> txItemList = new ArrayList<Tx>();
        HashMap<Sha256Hash, Tx> txDict = new HashMap<Sha256Hash, Tx>();
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        String sql = "select * from txs where block_no is null or block_no = ?";
        try {
            Cursor c = db.rawQuery(sql, new String[]{Integer.toString(Tx.TX_UNCONFIRMED)});
            while (c.moveToNext()) {
                Tx txItem = applyCursor(c);
                txItem.setIns(new ArrayList<In>());
                txItem.setOuts(new ArrayList<Out>());
                txItemList.add(txItem);
                txDict.put(new Sha256Hash(txItem.getTxHash()), txItem);
            }
            c.close();

            sql = "select b.* from txs a, ins b  where a.tx_hash=b.tx_hash  and ( a.block_no is null or a.block_no = ? ) "
                    + "order by b.tx_hash ,b.in_sn";
            c = db.rawQuery(sql, new String[]{Integer.toString(Tx.TX_UNCONFIRMED)});
            while (c.moveToNext()) {
                In inItem = applyCursorIn(c);
                Tx tx = txDict.get(new Sha256Hash(inItem.getTxHash()));
                tx.getIns().add(inItem);
            }
            c.close();

            sql = "select b.* from txs a, outs b where a.tx_hash=b.tx_hash and ( a.block_no is null or a.block_no = ? ) "
                    + "order by b.tx_hash,b.out_sn";
            c = db.rawQuery(sql, new String[]{Integer.toString(Tx.TX_UNCONFIRMED)});
            while (c.moveToNext()) {
                Out out = applyCursorOut(c);
                Tx tx = txDict.get(new Sha256Hash(out.getTxHash()));
                tx.getOuts().add(out);
            }
            c.close();

        } catch (AddressFormatException e) {
            e.printStackTrace();
        } finally {

        }
        return txItemList;
    }

    public Tx getTxDetailByTxHash(byte[] txHash) {
        Tx txItem = null;
        String txHashStr = Base58.encode(txHash);
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        String sql = "select * from txs where tx_hash='" + txHashStr + "'";
        Cursor c = db.rawQuery(sql, null);
        try {
            if (c.moveToNext()) {
                txItem = applyCursor(c);
            }

            if (txItem != null) {
                addInsAndOuts(db, txItem);

            }
        } catch (AddressFormatException e) {
            e.printStackTrace();
        } finally {
            c.close();
        }
        return txItem;
    }

    private void addInsAndOuts(SQLiteDatabase db, Tx txItem) throws AddressFormatException {
        String txHashStr = Base58.encode(txItem.getTxHash());
        txItem.setOuts(new ArrayList<Out>());
        txItem.setIns(new ArrayList<In>());
        String sql = "select * from ins where tx_hash='" + txHashStr + "' order by in_sn";
        Cursor c = db.rawQuery(sql, null);
        while (c.moveToNext()) {
            In inItem = applyCursorIn(c);
            inItem.setTx(txItem);
            txItem.getIns().add(inItem);
        }
        c.close();

        sql = "select * from outs where tx_hash='" + txHashStr + "' order by out_sn";
        c = db.rawQuery(sql, null);
        while (c.moveToNext()) {
            Out outItem = applyCursorOut(c);
            outItem.setTx(txItem);
            txItem.getOuts().add(outItem);
        }
        c.close();
    }

    public boolean isExist(byte[] txHash) {
        boolean result = false;
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        String sql = "select count(0) from txs where tx_hash='" + Base58.encode(txHash) + "'";
        Cursor c = db.rawQuery(sql, null);
        if (c.moveToNext()) {
            result = c.getInt(0) > 0;
        }
        c.close();
        return result;
    }

    public void add(Tx txItem) {
        SQLiteDatabase db = this.mDb.getWritableDatabase();
        db.beginTransaction();
        addTxToDb(db, txItem);
        db.setTransactionSuccessful();
        db.endTransaction();
    }

    public void addTxs(List<Tx> txItems) {
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        List<Tx> addTxItems = new ArrayList<Tx>();
        String existSql = "select count(0) cnt from txs where tx_hash=";
        Cursor c;
        for (Tx txItem : txItems) {
            c = db.rawQuery(existSql + "'" + Base58.encode(txItem.getTxHash()) + "'", null);
            int cnt = 0;
            if (c.moveToNext()) {
                int idColumn = c.getColumnIndex("cnt");
                if (idColumn != -1) {
                    cnt = c.getInt(idColumn);
                }
            }
            if (cnt == 0) {
                addTxItems.add(txItem);
            }
            c.close();
        }
        if (addTxItems.size() > 0) {
            db = this.mDb.getWritableDatabase();
            db.beginTransaction();
            for (Tx txItem : addTxItems) {
                //LogUtil.d("txDb", Base58.encode(txItem.getTxHash()) + "," + Utils.bytesToHexString(txItem.getTxHash()));
                addTxToDb(db, txItem);
                //List<Tx> txList = getTxAndDetailByAddress("1B5XuAJNTN2Upi7AXs7tJCxvFGjhPna6Q5");
            }
            db.setTransactionSuccessful();
            db.endTransaction();
        }
    }

    private void addTxToDb(SQLiteDatabase db, Tx txItem) {
        ContentValues cv = new ContentValues();
        applyContentValues(txItem, cv);
        db.insert(BitherjDatabaseHelper.Tables.TXS, null, cv);
        Cursor c;
        String sql;
        List<Object[]> addressesTxsRels = new ArrayList<Object[]>();
        try {
            for (In inItem : txItem.getIns()) {

                sql = "select out_address from outs where tx_hash='"
                        + Base58.encode(inItem.getPrevTxHash()) + "' and out_sn=" + inItem.getPrevOutSn();
                c = db.rawQuery(sql, null);
                while (c.moveToNext()) {
                    int idColumn = c.getColumnIndex("out_address");
                    if (idColumn != -1) {
                        addressesTxsRels.add(new Object[]{c.getString(idColumn), txItem.getTxHash()});
                    }
                }
                c.close();
                cv = new ContentValues();
                applyContentValues(inItem, cv);
                db.insert(BitherjDatabaseHelper.Tables.INS, null, cv);

                sql = "update outs set out_status=" + Out.OutStatus.spent.getValue() +
                        " where tx_hash='" + Base58.encode(inItem.getPrevTxHash()) + "' and out_sn=" + inItem.getPrevOutSn();
                db.execSQL(sql);
            }
            for (Out outItem : txItem.getOuts()) {

                cv = new ContentValues();
                applyContentValues(outItem, cv);
                db.insert(BitherjDatabaseHelper.Tables.OUTS, null, cv);
                if (!Utils.isEmpty(outItem.getOutAddress())) {
                    addressesTxsRels.add(new Object[]{outItem.getOutAddress(), txItem.getTxHash()});
                }
                sql = "select tx_hash from ins where prev_tx_hash='" + Base58.encode(txItem.getTxHash())
                        + "' and prev_out_sn=" + outItem.getOutSn();
                c = db.rawQuery(sql, null);
                boolean isSpentByExistTx = false;
                if (c.moveToNext()) {
                    int idColumn = c.getColumnIndex("tx_hash");
                    if (idColumn != -1) {
                        addressesTxsRels.add(new Object[]{outItem.getOutAddress(), Base58.decode(c.getString(idColumn))});
                    }
                    isSpentByExistTx = true;
                }
                c.close();
                if (isSpentByExistTx) {
                    sql = "update outs set out_status=" + Out.OutStatus.spent.getValue() +
                            " where tx_hash='" + Base58.encode(txItem.getTxHash()) + "' and out_sn=" + outItem.getOutSn();
                    db.execSQL(sql);
                }

            }
            for (Object[] array : addressesTxsRels) {
                sql = "insert or ignore into addresses_txs(address, tx_hash) values('"
                        + array[0] + "','" + Base58.encode((byte[]) array[1]) + "')";
                db.execSQL(sql);
            }

        } catch (AddressFormatException e) {
            e.printStackTrace();
        }

    }

    public void remove(byte[] txHash) {
        String txHashStr = Base58.encode(txHash);
        List<String> txHashes = new ArrayList<String>();
        List<String> needRemoveTxHashes = new ArrayList<String>();
        txHashes.add(txHashStr);
        while (txHashes.size() > 0) {
            String thisHash = txHashes.get(0);
            txHashes.remove(0);
            needRemoveTxHashes.add(thisHash);
            List<String> temp = getRelayTx(thisHash);
            txHashes.addAll(temp);
        }
        SQLiteDatabase db = this.mDb.getWritableDatabase();
        db.beginTransaction();
        for (String str : needRemoveTxHashes) {
            removeSingleTx(db, str);
        }
        db.setTransactionSuccessful();
        db.endTransaction();
    }

    private void removeSingleTx(SQLiteDatabase db, String tx) {
        String deleteTx = "delete from txs where tx_hash='" + tx + "'";
        String deleteIn = "delete from ins where tx_hash='" + tx + "'";
        String deleteOut = "delete from outs where tx_hash='" + tx + "'";
        String deleteAddressesTx = "delete from addresses_txs where tx_hash='" + tx + "'";
        String inSql = "select prev_tx_hash,prev_out_sn from ins where tx_hash='" + tx + "'";
        String existOtherIn = "select count(0) cnt from ins where prev_tx_hash=? and prev_out_sn=?";
        String updatePrevOut = "update outs set out_status=%d where tx_hash=%s and out_sn=%d";
        Cursor c = db.rawQuery(inSql, new String[]{tx});
        List<Object[]> needUpdateOuts = new ArrayList<Object[]>();
        while (c.moveToNext()) {
            int idColumn = c.getColumnIndex(BitherjDatabaseHelper.InsColumns.PREV_TX_HASH);
            String prevTxHash = null;
            int prevOutSn = 0;
            if (idColumn != -1) {
                prevTxHash = c.getString(idColumn);
            }
            idColumn = c.getColumnIndex(BitherjDatabaseHelper.InsColumns.PREV_OUT_SN);
            if (idColumn != -1) {
                prevOutSn = c.getInt(idColumn);
            }
            needUpdateOuts.add(new Object[]{prevTxHash, prevOutSn});

        }
        c.close();
        db.execSQL(deleteAddressesTx);
        db.execSQL(deleteOut);
        db.execSQL(deleteIn);
        db.execSQL(deleteTx);
        for (Object[] array : needUpdateOuts) {
            c = db.rawQuery(existOtherIn, new String[]{array[0].toString(), array[1].toString()});
            while (c.moveToNext()) {
                if (c.getInt(0) == 0) {
                    String updateSql = Utils.format(updatePrevOut,
                            Out.OutStatus.unspent.getValue(), array[0].toString(), Integer.valueOf(array[1].toString()));
                    db.execSQL(updateSql);
                }

            }
            c.close();

        }
    }

    private List<String> getRelayTx(String txHash) {
        List<String> relayTxHashes = new ArrayList<String>();
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        String relayTx = "select distinct tx_hash from ins where prev_tx_hash='" + txHash + "'";
        Cursor c = db.rawQuery(relayTx, null);
        while (c.moveToNext()) {
            relayTxHashes.add(c.getString(0));
        }
        c.close();
        return relayTxHashes;
    }

    public boolean isAddress(String address, Tx txItem) {
        boolean result = false;
        String sql = "select count(0) from ins a, txs b where a.tx_hash=b.tx_hash and" +
                " b.block_no is not null and a.prev_tx_hash=? and a.prev_out_sn=?";
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        Cursor c;
        for (In inItem : txItem.getIns()) {
            c = db.rawQuery(sql, new String[]{Base58.encode(inItem.getPrevTxHash()), Integer.toString(inItem.getPrevOutSn())});
            if (c.moveToNext()) {
                if (c.getInt(0) > 0) {
                    c.close();
                    return false;
                }
            }
            c.close();

        }
        sql = "select count(0) from addresses_txs where tx_hash=? and address=?";
        c = db.rawQuery(sql, new String[]{
                Base58.encode(txItem.getTxHash()), address
        });
        int count = 0;
        if (c.moveToNext()) {
            count = c.getInt(0);
        }
        c.close();
        if (count > 0) {
            return true;
        }
        sql = "select count(0) from outs where tx_hash=? and out_sn=? and out_address=?";
        for (In inItem : txItem.getIns()) {
            c = db.rawQuery(sql, new String[]{Base58.encode(inItem.getPrevTxHash())
                    , Integer.toString(inItem.getPrevOutSn()), address});
            count = 0;
            if (c.moveToNext()) {
                count = c.getInt(0);
            }
            c.close();
            if (count > 0) {
                return true;
            }
        }
        return result;
    }

    public void confirmTx(int blockNo, List<byte[]> txHashes) {
        if (blockNo == Tx.TX_UNCONFIRMED || txHashes == null) {
            return;
        }
        String sql = "update txs set block_no=%d where tx_hash='%s'";
        String existSql = "select count(0) from txs where block_no=? and tx_hash=?";
        String doubleSpendSql = "select a.tx_hash from ins a, ins b where a.prev_tx_hash=b.prev_tx_hash " +
                "and a.prev_out_sn=b.prev_out_sn and a.tx_hash<>b.tx_hash and b.tx_hash=?";
        String blockTimeSql = "select block_time from blocks where block_no=?";
        String updateTxTimeThatMoreThanBlockTime = "update txs set tx_time=%d where block_no=%d and tx_time>%d";
        SQLiteDatabase db = this.mDb.getWritableDatabase();
        db.beginTransaction();
        Cursor c;
        for (byte[] txHash : txHashes) {
            c = db.rawQuery(existSql, new String[]{Integer.toString(blockNo), Base58.encode(txHash)});
            if (c.moveToNext()) {
                int cnt = c.getInt(0);
                c.close();
                if (cnt > 0) {
                    continue;
                }
            } else {
                c.close();
            }
            String updateSql = Utils.format(sql, blockNo, Base58.encode(txHash));
            db.execSQL(updateSql);
            c = db.rawQuery(doubleSpendSql, new String[]{Base58.encode(txHash)});
            List<String> txHashes1 = new ArrayList<String>();
            while (c.moveToNext()) {
                int idColumn = c.getColumnIndex("tx_hash");
                if (idColumn != -1) {
                    txHashes1.add(c.getString(idColumn));
                }
            }
            c.close();
            List<String> needRemoveTxHashes = new ArrayList<String>();
            while (txHashes1.size() > 0) {
                String thisHash = txHashes1.get(0);
                txHashes1.remove(0);
                needRemoveTxHashes.add(thisHash);
                List<String> temp = getRelayTx(thisHash);
                txHashes1.addAll(temp);
            }
            for (String each : needRemoveTxHashes) {
                removeSingleTx(db, each);
            }

        }
        c = db.rawQuery(blockTimeSql, new String[]{Integer.toString(blockNo)});
        if (c.moveToNext()) {
            int idColumn = c.getColumnIndex("block_time");
            if (idColumn != -1) {
                int blockTime = c.getInt(idColumn);
                c.close();
                String sqlTemp = Utils.format(updateTxTimeThatMoreThanBlockTime, blockTime, blockNo, blockTime);
                db.execSQL(sqlTemp);
            }
        } else {
            c.close();
        }
        db.setTransactionSuccessful();
        db.endTransaction();
    }

    public void unConfirmTxByBlockNo(int blockNo) {
        SQLiteDatabase db = this.mDb.getWritableDatabase();
        String sql = "update txs set block_no=null where block_no>=" + blockNo;
        db.execSQL(sql);
    }

    public List<Tx> getUnspendTxWithAddress(String address) {
        String unspendOutSql = "select a.*,b.tx_ver,b.tx_locktime,b.tx_time,b.block_no,b.source,ifnull(b.block_no,0)*a.out_value coin_depth " +
                "from outs a,txs b where a.tx_hash=b.tx_hash" +
                " and a.out_address=? and a.out_status=?";
        List<Tx> txItemList = new ArrayList<Tx>();
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        Cursor c = db.rawQuery(unspendOutSql, new String[]{address, Integer.toString(Out.OutStatus.unspent.getValue())});
        try {
            while (c.moveToNext()) {
                int idColumn = c.getColumnIndex("coin_depth");

                Tx txItem = applyCursor(c);
                Out outItem = applyCursorOut(c);
                if (idColumn != -1) {
                    outItem.setCoinDepth(c.getLong(idColumn));
                }
                outItem.setTx(txItem);
                txItem.setOuts(new ArrayList<Out>());
                txItem.getOuts().add(outItem);
                txItemList.add(txItem);

            }
            c.close();
        } catch (AddressFormatException e) {
            e.printStackTrace();
        }
        return txItemList;
    }

    public List<Out> getUnspendOutWithAddress(String address) {
        List<Out> outItems = new ArrayList<Out>();
        String unspendOutSql = "select a.* from outs a,txs b where a.tx_hash=b.tx_hash " +
                "and b.block_no is null and a.out_address=? and a.out_status=?";
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        Cursor c = db.rawQuery(unspendOutSql,
                new String[]{address, Integer.toString(Out.OutStatus.unspent.getValue())});
        try {
            while (c.moveToNext()) {
                outItems.add(applyCursorOut(c));
            }
            c.close();
        } catch (AddressFormatException e) {
            e.printStackTrace();
        }
        return outItems;
    }

    public List<Out> getUnSpendOutCanSpendWithAddress(String address) {
        List<Out> outItems = new ArrayList<Out>();
        String confirmedOutSql = "select a.*,b.block_no*a.out_value coin_depth from outs a,txs b" +
                " where a.tx_hash=b.tx_hash and b.block_no is not null and a.out_address=? and a.out_status=?";
        String selfOutSql = "select a.* from outs a,txs b where a.tx_hash=b.tx_hash and b.block_no" +
                " is null and a.out_address=? and a.out_status=? and b.source>=?";
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        Cursor c = db.rawQuery(confirmedOutSql,
                new String[]{address, Integer.toString(Out.OutStatus.unspent.getValue())});
        try {
            while (c.moveToNext()) {
                Out outItem = applyCursorOut(c);
                int idColumn = c.getColumnIndex("coin_depth");
                if (idColumn != -1) {
                    outItem.setCoinDepth(c.getLong(idColumn));
                }
                outItems.add(outItem);
            }
            c.close();
            c = db.rawQuery(selfOutSql, new String[]{address,
                    Integer.toString(Out.OutStatus.unspent.getValue()), "1"});
            while (c.moveToNext()) {
                outItems.add(applyCursorOut(c));
            }
            c.close();
        } catch (AddressFormatException e) {
            e.printStackTrace();
        }
        return outItems;
    }

    public List<Out> getUnSpendOutButNotConfirmWithAddress(String address) {
        List<Out> outItems = new ArrayList<Out>();
        String selfOutSql = "select a.* from outs a,txs b where a.tx_hash=b.tx_hash and b.block_no" +
                " is null and a.out_address=? and a.out_status=? and b.source=?";
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        Cursor c = db.rawQuery(selfOutSql, new String[]{address,
                Integer.toString(Out.OutStatus.unspent.getValue()), "0"});
        try {
            while (c.moveToNext()) {
                outItems.add(applyCursorOut(c));

            }
        } catch (AddressFormatException e) {
            e.printStackTrace();
        } finally {
            c.close();
        }

        return outItems;
    }

    public int txCount(String address) {
        int result = 0;
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        String sql = "select count(*) from addresses_txs  where address='" + address + "'";
        Cursor c = db.rawQuery(sql, null);
        if (c.moveToNext()) {
            result = c.getInt(0);
        }
        c.close();

        return result;
    }

    public void txSentBySelfHasSaw(byte[] txHash) {
        SQLiteDatabase db = this.mDb.getWritableDatabase();
        String sql = "update txs set source=source+1 where tx_hash='" + Base58.encode(txHash) + "' and source>=1";
        db.execSQL(sql);
    }

    public List<Out> getOuts() {
        List<Out> outItemList = new ArrayList<Out>();
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        String sql = "select * from outs ";
        Cursor c = db.rawQuery(sql, null);
        try {
            while (c.moveToNext()) {
                outItemList.add(applyCursorOut(c));
            }
        } catch (AddressFormatException e) {
            e.printStackTrace();
        } finally {
            c.close();
        }

        return outItemList;
    }

    public List<Tx> getRecentlyTxsByAddress(String address, int greateThanBlockNo, int limit) {
        List<Tx> txItemList = new ArrayList<Tx>();
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        String sql = "select b.* from addresses_txs a, txs b where a.tx_hash=b.tx_hash and a.address='%s' " +
                "and ((b.block_no is null) or (b.block_no is not null and b.block_no>%d)) " +
                "order by ifnull(b.block_no,4294967295) desc, b.tx_time desc " +
                "limit %d ";
        sql = Utils.format(sql, address, greateThanBlockNo, limit);
        Cursor c = db.rawQuery(sql, null);
        try {
            while (c.moveToNext()) {
                Tx txItem = applyCursor(c);
                txItemList.add(txItem);
            }

            for (Tx item : txItemList) {
                addInsAndOuts(db, item);
            }
        } catch (AddressFormatException e) {
            e.printStackTrace();
        } finally {
            c.close();
        }
        return txItemList;
    }

    public List<Long> txInValues(byte[] txHash) {
        List<Long> inValues = new ArrayList<Long>();
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        String sql = "select b.out_value " +
                "from ins a left outer join outs b on a.prev_tx_hash=b.tx_hash and a.prev_out_sn=b.out_sn " +
                "where a.tx_hash='" + Base58.encode(txHash) + "'";
        Cursor c = db.rawQuery(sql, null);
        while (c.moveToNext()) {
            int idColumn = c.getColumnIndex("out_value");
            if (idColumn != -1) {
                inValues.add(c.getLong(idColumn));
            } else {
                inValues.add(null);
            }
        }
        c.close();
        return inValues;
    }

    public HashMap<Sha256Hash, Tx> getTxDependencies(Tx txItem) {
        HashMap<Sha256Hash, Tx> result = new HashMap<Sha256Hash, Tx>();
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        try {


            for (In inItem : txItem.getIns()) {
                Tx tx;
                String txHashStr = Base58.encode(inItem.getTxHash());
                String sql = "select * from txs where tx_hash='" + txHashStr + "'";
                Cursor c = db.rawQuery(sql, null);
                if (c.moveToNext()) {
                    tx = applyCursor(c);
                    c.close();
                } else {
                    c.close();
                    continue;
                }
                addInsAndOuts(db, tx);
                result.put(new Sha256Hash(tx.getTxHash()), tx);

            }
        } catch (AddressFormatException e) {
            e.printStackTrace();
        }
        return result;
    }

    public void clearAllTx() {
        SQLiteDatabase db = this.mDb.getWritableDatabase();
        db.beginTransaction();
        db.delete(BitherjDatabaseHelper.Tables.TXS, "", new String[0]);
        db.delete(BitherjDatabaseHelper.Tables.OUTS, "", new String[0]);
        db.delete(BitherjDatabaseHelper.Tables.INS, "", new String[0]);
        db.delete(BitherjDatabaseHelper.Tables.ADDRESSES_TXS, "", new String[0]);
        db.setTransactionSuccessful();
        db.endTransaction();
    }

    private void applyContentValues(Tx txItem, ContentValues cv) {
        if (txItem.getBlockNo() != Tx.TX_UNCONFIRMED) {
            cv.put(BitherjDatabaseHelper.TxsColumns.BLOCK_NO, txItem.getBlockNo());
        }
        cv.put(BitherjDatabaseHelper.TxsColumns.TX_HASH, Base58.encode(txItem.getTxHash()));
        cv.put(BitherjDatabaseHelper.TxsColumns.SOURCE, txItem.getSource());
        cv.put(BitherjDatabaseHelper.TxsColumns.TX_TIME, txItem.getTxTime());
        cv.put(BitherjDatabaseHelper.TxsColumns.TX_VER, txItem.getTxVer());
        cv.put(BitherjDatabaseHelper.TxsColumns.TX_LOCKTIME, txItem.getTxLockTime());
    }

    private void applyContentValues(In inItem, ContentValues cv) {
        cv.put(BitherjDatabaseHelper.InsColumns.TX_HASH, Base58.encode(inItem.getTxHash()));
        cv.put(BitherjDatabaseHelper.InsColumns.IN_SN, inItem.getInSn());
        cv.put(BitherjDatabaseHelper.InsColumns.PREV_TX_HASH, Base58.encode(inItem.getPrevTxHash()));
        cv.put(BitherjDatabaseHelper.InsColumns.PREV_OUT_SN, inItem.getPrevOutSn());
        if (inItem.getInSignature() != null) {
            cv.put(BitherjDatabaseHelper.InsColumns.IN_SIGNATURE, Base58.encode(inItem.getInSignature()));
        }
        cv.put(BitherjDatabaseHelper.InsColumns.IN_SEQUENCE, inItem.getInSequence());
    }

    private void applyContentValues(Out outItem, ContentValues cv) {
        cv.put(BitherjDatabaseHelper.OutsColumns.TX_HASH, Base58.encode(outItem.getTxHash()));
        cv.put(BitherjDatabaseHelper.OutsColumns.OUT_SN, outItem.getOutSn());
        cv.put(BitherjDatabaseHelper.OutsColumns.OUT_SCRIPT, Base58.encode(outItem.getOutScript()));
        cv.put(BitherjDatabaseHelper.OutsColumns.OUT_VALUE, outItem.getOutValue());
        cv.put(BitherjDatabaseHelper.OutsColumns.OUT_STATUS, outItem.getOutStatus().getValue());
        if (!Utils.isEmpty(outItem.getOutAddress())) {
            cv.put(BitherjDatabaseHelper.OutsColumns.OUT_ADDRESS, outItem.getOutAddress());
        }
    }

    private Tx applyCursor(Cursor c) throws AddressFormatException {
        Tx txItem = new Tx();
        int idColumn = c.getColumnIndex(BitherjDatabaseHelper.TxsColumns.BLOCK_NO);
        if (!c.isNull(idColumn)) {
            txItem.setBlockNo(c.getInt(idColumn));
        } else {
            txItem.setBlockNo(Tx.TX_UNCONFIRMED);
        }
        idColumn = c.getColumnIndex(BitherjDatabaseHelper.TxsColumns.TX_HASH);
        if (idColumn != -1) {
            txItem.setTxHash(Base58.decode(c.getString(idColumn)));
        }
        idColumn = c.getColumnIndex(BitherjDatabaseHelper.TxsColumns.SOURCE);
        if (idColumn != -1) {
            txItem.setSource(c.getInt(idColumn));
        }
        if (txItem.getSource() >= 1) {
            txItem.setSawByPeerCnt(txItem.getSource() - 1);
            txItem.setSource(1);
        } else {
            txItem.setSawByPeerCnt(0);
            txItem.setSource(0);
        }
        idColumn = c.getColumnIndex(BitherjDatabaseHelper.TxsColumns.TX_TIME);
        if (idColumn != -1) {
            txItem.setTxTime(c.getInt(idColumn));
        }
        idColumn = c.getColumnIndex(BitherjDatabaseHelper.TxsColumns.TX_VER);
        if (idColumn != -1) {
            txItem.setTxVer(c.getInt(idColumn));
        }
        idColumn = c.getColumnIndex(BitherjDatabaseHelper.TxsColumns.TX_LOCKTIME);
        if (idColumn != -1) {
            txItem.setTxLockTime(c.getInt(idColumn));
        }
        return txItem;

    }

    private In applyCursorIn(Cursor c) throws AddressFormatException {
        In inItem = new In();
        int idColumn = c.getColumnIndex(BitherjDatabaseHelper.InsColumns.TX_HASH);
        if (idColumn != -1) {
            inItem.setTxHash(Base58.decode(c.getString(idColumn)));
        }
        idColumn = c.getColumnIndex(BitherjDatabaseHelper.InsColumns.IN_SN);
        if (idColumn != -1) {
            inItem.setInSn(c.getInt(idColumn));
        }
        idColumn = c.getColumnIndex(BitherjDatabaseHelper.InsColumns.PREV_TX_HASH);
        if (idColumn != -1) {
            inItem.setPrevTxHash(Base58.decode(c.getString(idColumn)));
        }
        idColumn = c.getColumnIndex(BitherjDatabaseHelper.InsColumns.PREV_OUT_SN);
        if (idColumn != -1) {
            inItem.setPrevOutSn(c.getInt(idColumn));
        }
        idColumn = c.getColumnIndex(BitherjDatabaseHelper.InsColumns.IN_SIGNATURE);
        if (idColumn != -1) {
            String inSignature = c.getString(idColumn);
            if (!Utils.isEmpty(inSignature)) {
                inItem.setInSignature(Base58.decode(c.getString(idColumn)));
            }
        }
        idColumn = c.getColumnIndex(BitherjDatabaseHelper.InsColumns.IN_SEQUENCE);
        if (idColumn != -1) {
            inItem.setInSequence(c.getInt(idColumn));
        }
        return inItem;
    }

    private Out applyCursorOut(Cursor c) throws AddressFormatException {
        Out outItem = new Out();
        int idColumn = c.getColumnIndex(BitherjDatabaseHelper.OutsColumns.TX_HASH);
        if (idColumn != -1) {
            outItem.setTxHash(Base58.decode(c.getString(idColumn)));
        }
        idColumn = c.getColumnIndex(BitherjDatabaseHelper.OutsColumns.OUT_SN);
        if (idColumn != -1) {
            outItem.setOutSn(c.getInt(idColumn));
        }
        idColumn = c.getColumnIndex(BitherjDatabaseHelper.OutsColumns.OUT_SCRIPT);
        if (idColumn != -1) {
            outItem.setOutScript(Base58.decode(c.getString(idColumn)));
        }
        idColumn = c.getColumnIndex(BitherjDatabaseHelper.OutsColumns.OUT_VALUE);
        if (idColumn != -1) {
            outItem.setOutValue(c.getLong(idColumn));
        }
        idColumn = c.getColumnIndex(BitherjDatabaseHelper.OutsColumns.OUT_STATUS);
        if (idColumn != -1) {
            outItem.setOutStatus(Out.getOutStatus(c.getInt(idColumn)));
        }
        idColumn = c.getColumnIndex(BitherjDatabaseHelper.OutsColumns.OUT_ADDRESS);
        if (idColumn != -1) {
            outItem.setOutAddress(c.getString(idColumn));
        }
        return outItem;
    }
}


































