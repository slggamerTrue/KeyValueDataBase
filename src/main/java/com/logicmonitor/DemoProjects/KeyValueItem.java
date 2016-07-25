package com.logicmonitor.DemoProjects;

/**
 * Created by caizhou on 16/7/24.
 */
public class KeyValueItem {
    private boolean valid; //is deleted?
    private short itemLength; //total length of the item
    //item length could not equal key length + value length
    private short keyLength; //key length
    private short valueLength; //value length
    private byte[] key;
    private byte[] value;

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public short getItemLength() {
        return itemLength;
    }

    public void setItemLength(short itemLength) {
        this.itemLength = itemLength;
    }

    public short getKeyLength() {
        return keyLength;
    }

    public void setKeyLength(short keyLength) {
        this.keyLength = keyLength;
    }

    public short getValueLength() {
        return valueLength;
    }

    public void setValueLength(short valueLength) {
        this.valueLength = valueLength;
    }

    public byte[] getKey() {
        return key;
    }

    public void setKey(byte[] key) {
        this.key = key;
    }

    public byte[] getValue() {
        return value;
    }

    public void setValue(byte[] value) {
        this.value = value;
    }
}
