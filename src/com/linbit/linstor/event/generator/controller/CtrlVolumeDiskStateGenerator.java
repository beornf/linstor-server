package com.linbit.linstor.event.generator.controller;

import com.linbit.linstor.Node;
import com.linbit.linstor.ResourceName;
import com.linbit.linstor.annotation.ApiContext;
import com.linbit.linstor.api.pojo.ResourceState;
import com.linbit.linstor.api.pojo.VolumeState;
import com.linbit.linstor.core.CoreModule;
import com.linbit.linstor.event.ObjectIdentifier;
import com.linbit.linstor.event.generator.VolumeDiskStateGenerator;
import com.linbit.linstor.netcom.Peer;
import com.linbit.linstor.satellitestate.SatelliteVolumeState;
import com.linbit.linstor.security.AccessContext;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;

@Singleton
public class CtrlVolumeDiskStateGenerator implements VolumeDiskStateGenerator
{
    private final AccessContext accCtx;
    private final CoreModule.NodesMap nodesMap;
    private final ReadWriteLock nodesMapLock;

    @Inject
    public CtrlVolumeDiskStateGenerator(
        @ApiContext AccessContext accCtxRef,
        CoreModule.NodesMap nodesMapRef,
        @Named(CoreModule.NODES_MAP_LOCK) ReadWriteLock nodesMapLockRef
    )
    {
        accCtx = accCtxRef;
        nodesMap = nodesMapRef;
        nodesMapLock = nodesMapLockRef;
    }

    @Override
    public String generate(ObjectIdentifier objectIdentifier)
        throws Exception
    {
        String diskState;

        nodesMapLock.readLock().lock();
        try
        {
            Node node = nodesMap.get(objectIdentifier.getNodeName());

            diskState = "NoSatelliteState";
            if (node != null)
            {
                Peer peer = node.getPeer(accCtx);

                if (peer != null)
                {
                    diskState = peer.getSatelliteState().getFromVolume(
                        objectIdentifier.getResourceName(),
                        objectIdentifier.getVolumeNumber(),
                        SatelliteVolumeState::getDiskState
                    );
                }
            }
        }
        finally
        {
            nodesMapLock.readLock().unlock();
        }

        return diskState;
    }
}