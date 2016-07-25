# KeyValueDataBase
a demo for key value data base
 * It implement a base Key Value DataBase. Key and Value can be arbitrary length. If key and value are fix length, it's
 * easy to implement.
 * There will be only one data file. No index file, No redo,undo log file.
 * When system start, it will re-created index from data file. Do not consider the case that index can't be fully
 * loaded to memory. 1G memory can handle the index for about 90,000,000 items.
 * Only one thread, do not consider conflict.
  
 For the item, the formation in data file should be:
bytes   1        2          2         2
      valid|totalLength|keyLength|valueLength
      keybytes....
      valuebytes....

2 bytes can support at most 65535 length for key and value.

 For the index, it's a map<Integer, List<Long>>. the key is the hashcode of the key of item. The value is list about 
 the start position of item in the file. Of course we can store the index as a file too. Re-create it each time when
 we start just show the function we create the index base on the data file.
 
 