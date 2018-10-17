package com.linbit.linstor.storage.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.linbit.extproc.ExtCmd;
import com.linbit.extproc.ExtCmd.OutputData;
import com.linbit.linstor.storage.StorageException;
import com.linbit.linstor.storage.StorageUtils;
import com.linbit.linstor.storage.layer.provider.lvm.LvmCommands;

import static com.linbit.linstor.storage.layer.provider.lvm.LvmCommands.LVS_COL_IDENTIFIER;
import static com.linbit.linstor.storage.layer.provider.lvm.LvmCommands.LVS_COL_PATH;
import static com.linbit.linstor.storage.layer.provider.lvm.LvmCommands.LVS_COL_POOL_LV;
import static com.linbit.linstor.storage.layer.provider.lvm.LvmCommands.LVS_COL_SIZE;
import static com.linbit.linstor.storage.layer.provider.lvm.LvmCommands.LVS_COL_VG;

/**
 * @author Gabor Hernadi &lt;gabor.hernadi@linbit.com&gt;
 */
public class LvmUtils
{
    private LvmUtils()
    {
    }

    public static class LvsInfo
    {
        public final String volumeGroup;
        public final String thinPool;
        public final String identifier;
        public final String path;
        public final long size;

        LvsInfo(String volumeGroupRef, String thinPoolRef, String identifierRef, String pathRef, long sizeRef)
        {
            volumeGroup = volumeGroupRef;
            thinPool = thinPoolRef;
            identifier = identifierRef;
            path = pathRef;
            size = sizeRef;
        }
    }

    public static HashMap<String, LvsInfo> getLvsInfo(
        final ExtCmd ec,
        final Set<String> volumeGroups
    )
        throws StorageException
    {
        final OutputData output = LvmCommands.lvs(ec, volumeGroups);
        final String stdOut = new String(output.stdoutData);

        final HashMap<String, LvsInfo> infoByIdentifier = new HashMap<>();

        final String[] lines = stdOut.split("\n");
        for (final String line : lines)
        {
            final String[] data = line.trim().split(StorageUtils.DELIMITER);
            final int expectedColCount = 4;
            if (data.length == expectedColCount)
            {
                final String identifier = data[LVS_COL_IDENTIFIER];
                final String path = data[LVS_COL_PATH];
                final String sizeStr = data[LVS_COL_SIZE];
                final String vgStr = data[LVS_COL_VG];
                final String thinPoolStr;
                if (data.length <= LVS_COL_POOL_LV)
                {
                    thinPoolStr = null;
                }
                else
                {
                    thinPoolStr = data[LVS_COL_POOL_LV];
                }

                long size;
                try
                {
                    size = StorageUtils.parseDecimalAsLong(sizeStr.trim());
                }
                catch (NumberFormatException nfExc)
                {
                    throw new StorageException(
                        "Unable to parse logical volume size",
                        "Size to parse: '" + sizeStr + "'",
                        null,
                        null,
                        "External command used to query logical volume info: " +
                            String.join(" ", output.executedCommand),
                        nfExc
                    );
                }

                final LvsInfo state = new LvsInfo(
                    vgStr,
                    thinPoolStr,
                    identifier,
                    path,
                    size
                );
                infoByIdentifier.put(identifier, state);
            }
        }
        return infoByIdentifier;
    }


    public static Map<String, Long> getExtentSize(ExtCmd extCmd, Set<String> volumeGroups) throws StorageException
    {
        return parseSimpleVolumeGroupTable(
            LvmCommands.getExtentSize(extCmd, volumeGroups),
            "extent size"
        );
    }

    public static Map<String, Long> getVGTotalSize(ExtCmd extCmd, Set<String> volumeGroups) throws StorageException
    {
        return parseSimpleVolumeGroupTable(
            LvmCommands.getVgTotalSize(extCmd, volumeGroups),
            "total size"
        );
    }

    public static Map<String, Long> getVGFreeSize(ExtCmd extCmd, Set<String> volumeGroups) throws StorageException
    {
        return parseSimpleVolumeGroupTable(
            LvmCommands.getVgFreeSize(extCmd, volumeGroups),
            "free size"
        );
    }
    private static Map<String, Long> parseSimpleVolumeGroupTable(OutputData output, String descr)
        throws StorageException
    {
        final int expectedColums = 2;

        final Map<String, Long> result = new HashMap<>();

        final String stdOut = new String(output.stdoutData);
        final String[] lines = stdOut.split("\n");

        for (final String line : lines)
        {
            final String[] data = line.trim().split(StorageUtils.DELIMITER);
            if (data.length == expectedColums)
            {
                try
                {
                    result.put(
                        data[0],
                        StorageUtils.parseDecimalAsLong(data[1])
                    );
                }
                catch (NumberFormatException nfExc)
                {
                    throw new StorageException(
                        "Unable to parse " + descr,
                        "Numerc value to parse: '" + data[1] + "'",
                        null,
                        null,
                        "External command: " + String.join(" ", output.executedCommand),
                        nfExc
                    );
                }
            }
            else
            {
                throw new StorageException(
                    "Unable to parse " + descr,
                    "Expected " + expectedColums + " columns, but got " + data.length,
                    "Failed to parse line: " + line,
                    null,
                    "External command: " + String.join(" ", output.executedCommand)
                );
            }
        }
        return result;
    }
}
