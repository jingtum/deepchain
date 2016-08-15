package jingtum.deepchain.source;

/**
 * Created by nichunen on 16/8/9.
 */


import exception.ConfigurationException;
import exception.BitcoinBlockReadException;

import java.io.IOException;
import java.io.InputStream;

import org.apache.hadoop.io.BytesWritable;

import org.apache.hadoop.mapred.FileSplit;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.Reporter;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * Reads records as blocks of the bitcoin blockchain. Note that it can be tricky to find the start of a block in a split. The BitcoinBlockReader provides a method (seekBlockStart) for this.
 */

public class BitcoinBlockRecordReader extends AbstractBitcoinRecordReader<BytesWritable, BitcoinBlock> {
    private static final Log LOG = LogFactory.getLog(BitcoinBlockRecordReader.class.getName());


    public BitcoinBlockRecordReader(FileSplit split, JobConf job, Reporter reporter) throws IOException, ConfigurationException, BitcoinBlockReadException {
        super(split, job, reporter);
    }

    /**
     * Create an empty key
     *
     * @return key
     */
    public BytesWritable createKey() {
        return new BytesWritable();
    }

    /**
     * Create an empty value
     *
     * @return value
     */
    public BitcoinBlock createValue() {
        return new BitcoinBlock();
    }


    /**
     * Read a next block.
     *
     * @param key   is a 64 byte array (hashMerkleRoot and prevHashBlock)
     * @param value is a deserialized Java object of class BitcoinBlock
     * @return true if next block is available, false if not
     */
    public boolean next(BytesWritable key, BitcoinBlock value) throws IOException {
        // read all the blocks, if necessary a block overlapping a split
        while (getFilePosition() <= getEnd()) { // did we already went beyond the split (remote) or do we have no further data left?
            BitcoinBlock dataBlock = null;
            try {
                dataBlock = getBbr().readBlock();

            } catch (BitcoinBlockReadException e) {
                // log
                LOG.error(e);
            }
            if (dataBlock == null) return false;
            byte[] hashMerkleRoot = dataBlock.getHashMerkleRoot();
            byte[] hashPrevBlock = dataBlock.getHashPrevBlock();
            byte[] newKey = new byte[hashMerkleRoot.length + hashPrevBlock.length];
            for (int i = 0; i < hashMerkleRoot.length; i++) {
                newKey[i] = hashMerkleRoot[i];
            }
            for (int j = 0; j < hashPrevBlock.length; j++) {
                newKey[j + hashMerkleRoot.length] = hashPrevBlock[j];
            }
            key.set(newKey, 0, newKey.length);
            value.set(dataBlock);
            return true;
        }
        return false;
    }


}