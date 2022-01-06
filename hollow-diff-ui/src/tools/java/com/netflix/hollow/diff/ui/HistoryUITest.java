package com.netflix.hollow.diff.ui;

import com.netflix.hollow.core.index.key.PrimaryKey;
import com.netflix.hollow.core.read.HollowBlobInput;
import com.netflix.hollow.core.read.engine.HollowBlobReader;
import com.netflix.hollow.core.read.engine.HollowReadStateEngine;
import com.netflix.hollow.core.read.engine.HollowTypeReadState;
import com.netflix.hollow.core.schema.HollowObjectSchema;
import com.netflix.hollow.core.schema.HollowObjectSchema.FieldType;
import com.netflix.hollow.core.schema.HollowSchema;
import com.netflix.hollow.core.util.StateEngineRoundTripper;
import com.netflix.hollow.core.write.HollowBlobWriter;
import com.netflix.hollow.core.write.HollowObjectTypeWriteState;
import com.netflix.hollow.core.write.HollowObjectWriteRecord;
import com.netflix.hollow.core.write.HollowTypeWriteState;
import com.netflix.hollow.core.write.HollowWriteStateEngine;
import com.netflix.hollow.history.ui.jetty.HollowHistoryUIServer;
import com.netflix.hollow.tools.history.HollowHistory;
import com.netflix.hollow.tools.history.keyindex.HollowHistoryKeyIndex;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.junit.Test;

public class HistoryUITest {

    @Test
    public void startServerOnPort7777() throws Exception {
        HollowHistory history = foo();

        HollowHistoryUIServer server = new HollowHistoryUIServer(history, 7777);
        server.start();
        server.join();
    }

    public HollowHistory foo() throws Exception {
        ByteArrayOutputStream baos_v2_to_v1, baos_v3_to_v2;
        HollowHistory history;
        HollowReadStateEngine readStateEngine;
        HollowBlobReader reader;
        HollowWriteStateEngine stateEngine;
        HollowObjectSchema schema;

        {
            schema = new HollowObjectSchema("TypeA", 2);

            stateEngine = new HollowWriteStateEngine();
            schema.addField("a1", FieldType.INT);
            schema.addField("a2", FieldType.INT);

            //attach schema to write state engine
            stateEngine.addTypeState(new HollowObjectTypeWriteState(schema));

            addRec(stateEngine, schema, new String[] { "a1", "a2" }, new int[] { 1, 1 });
            addRec(stateEngine, schema, new String[] { "a1", "a2" }, new int[] { 2, 2 });
            addRec(stateEngine, schema, new String[] { "a1", "a2" }, new int[] { 3, 3 });
            addRec(stateEngine, schema, new String[] { "a1", "a2" }, new int[] { 4, 4 });
            addRec(stateEngine, schema, new String[] { "a1", "a2" }, new int[] { 5, 5 });
            addRec(stateEngine, schema, new String[] { "a1", "a2" }, new int[] { 6, 6 });

            //add rec to write phase
            stateEngine.prepareForWrite();
            ByteArrayOutputStream baos_v1 = new ByteArrayOutputStream();
            HollowBlobWriter writer = new HollowBlobWriter(stateEngine);
            //write snapshot to output stream
            writer.writeSnapshot(baos_v1);

            stateEngine.prepareForNextCycle();

            //2) add new set of records
            addRec(stateEngine, schema, new String[] { "a1", "a2" }, new int[] { 1, 1 });
            addRec(stateEngine, schema, new String[] { "a1", "a2" }, new int[] { 2, 7 });
            addRec(stateEngine, schema, new String[] { "a1", "a2" }, new int[] { 3, 3 });
            addRec(stateEngine, schema, new String[] { "a1", "a2" }, new int[] { 5, 8 });
            addRec(stateEngine, schema, new String[] { "a1", "a2" }, new int[] { 6, 6 });
            addRec(stateEngine, schema, new String[] { "a1", "a2" }, new int[] { 7, 9 });
            addRec(stateEngine, schema, new String[] { "a1", "a2" }, new int[] { 8, 10 });

            //add rec to write phase
            stateEngine.prepareForWrite();

            //reinit output stream
            ByteArrayOutputStream baos_v1_to_v2 = new ByteArrayOutputStream();
            baos_v2_to_v1 = new ByteArrayOutputStream();

            //write delta based on new records
            writer.writeDelta(baos_v1_to_v2);
            writer.writeReverseDelta(baos_v2_to_v1);

            stateEngine.prepareForNextCycle();
            addRec(stateEngine, schema, new String[] { "a1", "a2" }, new int[] { 1, 1 });
            addRec(stateEngine, schema, new String[] { "a1", "a2" }, new int[] { 2, 7 });
            addRec(stateEngine, schema, new String[] { "a1", "a2" }, new int[] { 3, 11 });
            addRec(stateEngine, schema, new String[] { "a1", "a2" }, new int[] { 6, 12 });
            addRec(stateEngine, schema, new String[] { "a1", "a2" }, new int[] { 7, 13 });
            addRec(stateEngine, schema, new String[] { "a1", "a2" }, new int[] { 8, 10 });

            stateEngine.prepareForWrite();

            //reinit output stream
            ByteArrayOutputStream baos_v2_to_v3 = new ByteArrayOutputStream();
            baos_v3_to_v2 = new ByteArrayOutputStream();

            //write delta based on new records
            writer.writeDelta(baos_v2_to_v3);
            writer.writeReverseDelta(baos_v3_to_v2);

            ByteArrayOutputStream baos_v3 = new ByteArrayOutputStream();
            //write snapshot to output stream
            writer.writeSnapshot(baos_v3);

            readStateEngine = new HollowReadStateEngine();
            reader = new HollowBlobReader(readStateEngine);
            //load snapshot from output stream to read state engine
            reader.readSnapshot(HollowBlobInput.serial(baos_v3.toByteArray()));
            //>>>do not init history with the snapshot
            history = new HollowHistory(readStateEngine, 0, 10);
            history.getKeyIndex().addTypeIndex("TypeA", "a1");

            reader.applyDelta(HollowBlobInput.serial(baos_v3_to_v2.toByteArray()));
            history.reverseDeltaOccurred(20001231235959999L);

            reader.applyDelta(HollowBlobInput.serial(baos_v2_to_v1.toByteArray()));
            history.reverseDeltaOccurred(19991231235959999L);
        }

        return history;
    }
    private HollowWriteStateEngine stateEngine;
    private HollowObjectSchema schema;
    private HollowObjectSchema bSchema;

    private void addRecord(int a1, int a2) {
        HollowObjectWriteRecord rec = new HollowObjectWriteRecord(schema);
        rec.setInt("a1", a1);
        rec.setInt("a2", a2);
        stateEngine.add("TypeA", rec);
    }

    private void setupKeyIndex(HollowReadStateEngine stateEngine, HollowHistory history) {
        HollowHistoryKeyIndex keyIndex = history.getKeyIndex();
        for (String type : stateEngine.getAllTypes()) {

            HollowTypeReadState typeState = stateEngine.getTypeState(type);
            HollowSchema schema = typeState.getSchema();
            if (schema instanceof HollowObjectSchema) {
                HollowObjectSchema oSchema = (HollowObjectSchema) schema;
                PrimaryKey pKey = oSchema.getPrimaryKey();
                if (pKey == null) continue;

                keyIndex.indexTypeField(pKey, stateEngine);
                System.out.println("Setup KeyIndex: type=" + type + "\t" + pKey);
            }
        }
    }



    private static void addRec(HollowWriteStateEngine stateEngine, HollowObjectSchema schema, String[] names, int[] vals) {
        HollowObjectWriteRecord rec = new HollowObjectWriteRecord(schema);
        for (int i = 0; i < names.length; i++) {
            rec.setInt(names[i], vals[i]);
        }
        stateEngine.add(schema.getName(), rec);
    }


}
