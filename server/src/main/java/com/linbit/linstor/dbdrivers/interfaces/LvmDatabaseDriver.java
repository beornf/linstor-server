package com.linbit.linstor.dbdrivers.interfaces;

import com.linbit.SingleColumnDatabaseDriver;
import com.linbit.linstor.storage2.layer.data.LvmLayerData;

public interface LvmDatabaseDriver
{
    SingleColumnDatabaseDriver<LvmLayerData, String> getVolumeGroupDriver();

    SingleColumnDatabaseDriver<LvmLayerData, Double> getToleranceFactorDriver();
}
