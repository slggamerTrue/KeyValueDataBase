package com.logicmonitor.DemoProjects;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;


/**
 * Created by caizhou.
 * It implement a base Key Value DataBase. Key and Value can be arbitrary length. If key and value are fix length, it's
 * easy to implement.
 * There will be only one data file. No index file, No redo,undo log file.
 * When system start, it will re-created index from data file. Do not consider the case that index can't be fully
 * loaded to memory.
 */
public class KeyValueDataBaseDemo {
    static Logger log = LogManager.getLogger(KeyValueDataBaseDemo.class.getName());
    static private final String DATAFILENAME = "keyValue.data";
    private Cache<Integer/*hash*/, List<Long>/*pos in file*/> index = CacheBuilder.newBuilder().build();

    private KeyValueItem getKeyValueItem(RandomAccessFile randomAccessFile) throws IOException {
        KeyValueItem item = new KeyValueItem();
        item.setValid(randomAccessFile.readBoolean());
        item.setItemLength(randomAccessFile.readShort());
        item.setKeyLength(randomAccessFile.readShort());
        item.setValueLength(randomAccessFile.readShort());
        if (item.isValid()) {
            byte[] keyBytes = new byte[item.getKeyLength()];
            randomAccessFile.readFully(keyBytes);
            item.setKey(keyBytes);
            byte[] valueBytes = new byte[item.getValueLength()];
            randomAccessFile.readFully(valueBytes);
            item.setValue(valueBytes);
            int restLength = item.getItemLength() - item.getKeyLength() - item.getValueLength();
            if (restLength > 0) {
                randomAccessFile.skipBytes(restLength);
            }
        } else {
            randomAccessFile.skipBytes(item.getItemLength());
        }
        return item;
    }

    /**
     * recreate the index map from data file
     */
    public void init() throws ExecutionException, IOException {
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(DATAFILENAME, "r")) {
            KeyValueItem item = getKeyValueItem(randomAccessFile);
            do {
                // if it's valid, we will put it into index
                if (item.isValid()) {
                    int keyHash = HashUtil.getHash(item.getKey());
                    List<Long> posList = index.get(keyHash, new Callable<List<Long>>() {
                        @Override
                        public List<Long> call() throws Exception {
                            return new ArrayList<Long>();
                        }
                    });

                    posList.add(randomAccessFile.getFilePointer() - item.getKeyLength() - item.getValueLength() - 7);
                } else { //or we will move it forward, now we just ignore it

                }

            } while ((item = getKeyValueItem(randomAccessFile)) != null);

        } catch (FileNotFoundException e) {
            log.error("data fine not find, create it");
            File file = new File(DATAFILENAME);
            file.createNewFile();

        } catch (IOException e) {
            log.error("read to the end of the data file");
        }
    }

    private void writeItemToFile(RandomAccessFile randomAccessFile, KeyValueItem item) throws IOException {
        randomAccessFile.writeBoolean(item.isValid());
        randomAccessFile.writeShort(item.getItemLength());
        randomAccessFile.writeShort(item.getKeyLength());
        randomAccessFile.writeShort(item.getValueLength());
        randomAccessFile.write(item.getKey());
        randomAccessFile.write(item.getValue());
    }

    private String getItem(String key) throws ExecutionException {
        int keyHash = HashUtil.getHash(key);
        List<Long> list = index.get(keyHash, new Callable<List<Long>>() {
            @Override
            public List<Long> call() throws Exception {
                return new ArrayList<Long>();
            }
        });
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(DATAFILENAME, "r")) {
            for (long pos : list) {
                randomAccessFile.seek(pos);
                KeyValueItem item = getKeyValueItem(randomAccessFile);
                if (item.isValid()) {
                    if (new String(item.getKey()).equals(key)) {
                        return new String(item.getValue());
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean putItem(String key, String value) throws ExecutionException {
        int keyHash = HashUtil.getHash(key);
        List<Long> list = index.get(keyHash, new Callable<List<Long>>() {
            @Override
            public List<Long> call() throws Exception {
                return new ArrayList<Long>();
            }
        });
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(DATAFILENAME, "rw")) {
            KeyValueItem newItem = new KeyValueItem();
            newItem.setValid(true);
            newItem.setKeyLength((short) key.length());
            newItem.setValueLength((short) value.length());
            newItem.setKey(key.getBytes());
            newItem.setValue(value.getBytes());
            newItem.setItemLength((short) (key.length() + value.length()));

            for (long pos : list) {
                randomAccessFile.seek(pos);
                KeyValueItem oldItem = getKeyValueItem(randomAccessFile);
                if (oldItem.isValid()) {
                    if (new String(oldItem.getKey()).equals(key)) {
                        if (oldItem.getItemLength() >= newItem.getItemLength()) {
                            randomAccessFile.seek(pos);
                            writeItemToFile(randomAccessFile, newItem);
                            log.info("update item[" + key + "] at " + pos);
                            //no need to update index.
                            return true;
                        } else {
                            randomAccessFile.seek(pos);
                            //mark it to invalid
                            randomAccessFile.writeBoolean(false);
                            list.remove(pos);
                            log.info("mark item[" + key + "] at " + pos + " invalid");
                            break;
                        }
                    }
                }
            }
            //if not find, write it to the end of the file.
            long currentEndPos = randomAccessFile.length();
            randomAccessFile.seek(currentEndPos);
            writeItemToFile(randomAccessFile, newItem);
            list.add(currentEndPos);
            log.info("add item[" + key + "] at " + currentEndPos);
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * an very simple implement to simulate the function
     */
    private void handleConsole() throws ExecutionException {
        Scanner scanner = new Scanner(System.in);
        String line;
        while (true) {
            line = scanner.nextLine();
            if (line.equalsIgnoreCase("exit")) {
                break;
            }
            String[] cmds = line.split(" ", 2);
            if (cmds[0].equalsIgnoreCase("get")) {
                System.out.println(getItem(cmds[1]));
            } else if (cmds[0].equalsIgnoreCase("put")) {
                String[] items = cmds[1].split(" ", 2);
                putItem(items[0], items[1]);
            } else if (cmds[0].equalsIgnoreCase("delete")) {

            }
        }
    }

    public static void main(String[] args) throws ExecutionException, IOException {
        log.info("key value database starting...");
        KeyValueDataBaseDemo keyValueDataBase = new KeyValueDataBaseDemo();
        keyValueDataBase.init();
        log.info("init done, waiting cmd");
        keyValueDataBase.handleConsole();
        log.info("exit");
    }
}
