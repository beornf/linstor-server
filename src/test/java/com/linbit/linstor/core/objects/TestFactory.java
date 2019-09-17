package com.linbit.linstor.core.objects;

import com.linbit.drbd.md.MdException;
import com.linbit.linstor.core.identifier.NetInterfaceName;
import com.linbit.linstor.core.identifier.NodeName;
import com.linbit.linstor.core.identifier.ResourceName;
import com.linbit.linstor.core.identifier.SnapshotName;
import com.linbit.linstor.core.identifier.StorPoolName;
import com.linbit.linstor.core.identifier.VolumeNumber;
import com.linbit.linstor.core.objects.NetInterface.EncryptionType;
import com.linbit.linstor.core.types.LsIpAddress;
import com.linbit.linstor.core.types.TcpPortNumber;
import com.linbit.linstor.dbdrivers.DatabaseException;
import com.linbit.linstor.numberpool.DynamicNumberPool;
import com.linbit.linstor.propscon.PropsContainerFactory;
import com.linbit.linstor.security.ObjectProtection;
import com.linbit.linstor.storage.interfaces.categories.resource.RscDfnLayerObject;
import com.linbit.linstor.storage.interfaces.categories.resource.VlmDfnLayerObject;
import com.linbit.linstor.storage.interfaces.categories.resource.VlmProviderObject;
import com.linbit.linstor.storage.kinds.DeviceLayerKind;
import com.linbit.linstor.storage.kinds.DeviceProviderKind;
import com.linbit.linstor.transaction.TransactionMgr;
import com.linbit.linstor.transaction.TransactionObjectFactory;
import com.linbit.utils.Pair;

import javax.inject.Provider;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TestFactory
{
    /**
     * Creates a new {@link NetInterfaceData} without persisting it to the database
     */
    public static NetInterfaceData createNetInterfaceData(
        UUID niUuidRef,
        NetInterfaceName niNameRef,
        Node nodeRef,
        LsIpAddress niAddrRef,
        TcpPortNumber niStltConnPortRef,
        EncryptionType niStltConnEncrTypeRef,
        NetInterfaceDataGenericDbDriver dbDriverRef,
        TransactionObjectFactory transObjFactoryRef,
        Provider<? extends TransactionMgr> transMgrProviderRef
    )
    {
        return new NetInterfaceData(
            niUuidRef,
            niNameRef,
            nodeRef,
            niAddrRef,
            niStltConnPortRef,
            niStltConnEncrTypeRef,
            dbDriverRef,
            transObjFactoryRef,
            transMgrProviderRef
        );
    }

    /**
     * Creates a new {@link NodeConnectionData} without persisting it to the database
     * @throws DatabaseException
     */
    public static NodeConnectionData createNodeConnectionData(
        UUID uuidRef,
        Node nodeSrcRef,
        Node nodeDstRef,
        NodeConnectionDataGenericDbDriver driverRef,
        PropsContainerFactory propsContainerFactoryRef,
        TransactionObjectFactory transObjFactoryRef,
        Provider<? extends TransactionMgr> transMgrProviderRef
    )
        throws DatabaseException
    {
        return new NodeConnectionData(
            uuidRef,
            nodeSrcRef,
            nodeDstRef,
            driverRef,
            propsContainerFactoryRef,
            transObjFactoryRef,
            transMgrProviderRef
        );
    }

    /**
     * Creates a new {@link Node} without persisting it to the database
     * @throws DatabaseException
     */
    public static Node createNode(
        UUID uuidRef,
        ObjectProtection objProtRef,
        NodeName nodeNameRef,
        Node.Type initialTypeRef,
        long initialFlagsRef,
        NodeGenericDbDriver dbDriverRef,
        PropsContainerFactory propsContainerFactoryRef,
        TransactionObjectFactory transObjFactoryRef,
        Provider<? extends TransactionMgr> transMgrProviderRef
    )
        throws DatabaseException
    {
        return new Node(
            uuidRef,
            objProtRef,
            nodeNameRef,
            initialTypeRef,
            initialFlagsRef,
            dbDriverRef,
            propsContainerFactoryRef,
            transObjFactoryRef,
            transMgrProviderRef
        );
    }

    /**
     * Creates a new {@link ResourceConnectionData} without persisting it to the database
     * @throws DatabaseException
     */
    public static ResourceConnectionData createResourceConnectionData(
        UUID uuidRef,
        ResourceData resSrcRef,
        ResourceData resDstRef,
        TcpPortNumber portRef,
        DynamicNumberPool tcpPortPoolRef,
        ResourceConnectionDataGenericDbDriver driverRef,
        PropsContainerFactory propsContainerFactoryRef,
        TransactionObjectFactory transObjFactoryRef,
        Provider<? extends TransactionMgr> transMgrProviderRef,
        long initFlags
    )
        throws DatabaseException
    {
        return new ResourceConnectionData(
            uuidRef,
            resSrcRef,
            resDstRef,
            portRef,
            tcpPortPoolRef,
            driverRef,
            propsContainerFactoryRef,
            transObjFactoryRef,
            transMgrProviderRef,
            initFlags
        );
    }

    /**
     * Creates a new {@link ResourceData} without persisting it to the database
     * @throws DatabaseException
     */
    public static ResourceData createResourceData(
        UUID resUuidRef,
        ObjectProtection objProtRef,
        ResourceDefinitionData resDfnRef,
        Node nodeRef,
        long initFlagsRef,
        ResourceDataGenericDbDriver driverRef,
        PropsContainerFactory propsContainerFactoryRef,
        TransactionObjectFactory transObjFactoryRef,
        Provider<? extends TransactionMgr> transMgrProviderRef,
        Map<Resource.Key, ResourceConnection> rscConnMapRef,
        Map<VolumeNumber, Volume> vlmMapRef
    )
        throws DatabaseException
    {
        return new ResourceData(
            resUuidRef,
            objProtRef,
            resDfnRef,
            nodeRef,
            initFlagsRef,
            driverRef,
            propsContainerFactoryRef,
            transObjFactoryRef,
            transMgrProviderRef,
            rscConnMapRef,
            vlmMapRef
        );
    }

    /**
     * Creates a new {@link ResourceDefinitionData} without persisting it to the database
     * @throws DatabaseException
     */
    public static ResourceDefinitionData createResourceDefinitionData(
        UUID resDfnUuidRef,
        ObjectProtection resDfnObjProtRef,
        ResourceName resNameRef,
        byte[] extName,
        long flagValueRef,
        List<DeviceLayerKind> layerStackRef,
        ResourceDefinitionDataGenericDbDriver driverRef,
        PropsContainerFactory propsContainerFactoryRef,
        TransactionObjectFactory transObjFactoryRef,
        Provider<? extends TransactionMgr> transMgrProviderRef,
        Map<VolumeNumber, VolumeDefinition> vlmDfnMapRef,
        Map<NodeName, Resource> rscMapRef,
        Map<SnapshotName, SnapshotDefinition> snapshotDfnMapRef,
        Map<Pair<DeviceLayerKind, String>, RscDfnLayerObject> layerDataMapRef,
        ResourceGroup rscGrpRef
    )
        throws DatabaseException
    {
        return new ResourceDefinitionData(
            resDfnUuidRef,
            resDfnObjProtRef,
            resNameRef,
            extName,
            flagValueRef,
            layerStackRef,
            driverRef,
            propsContainerFactoryRef,
            transObjFactoryRef,
            transMgrProviderRef,
            vlmDfnMapRef,
            rscMapRef,
            snapshotDfnMapRef,
            layerDataMapRef,
            rscGrpRef
        );
    }

    /**
     * Creates a new {@link StorPoolData} without persisting it to the database
     * @throws DatabaseException
     */
    public static StorPoolData createStorPoolData(
        UUID uuidRef,
        Node nodeRef,
        StorPoolDefinitionData spddRef,
        DeviceProviderKind lvmRef,
        FreeSpaceMgr fsmRef,
        StorPoolDataGenericDbDriver driverRef,
        PropsContainerFactory propsContainerFactoryRef,
        TransactionObjectFactory transObjFactoryRef,
        Provider<? extends TransactionMgr> transMgrProviderRef,
        Map<String, VlmProviderObject> volumeMapRef
    )
        throws DatabaseException
    {
        return new StorPoolData(
            uuidRef,
            nodeRef,
            spddRef,
            lvmRef,
            fsmRef,
            driverRef,
            propsContainerFactoryRef,
            transObjFactoryRef,
            transMgrProviderRef,
            volumeMapRef
        );
    }

    /**
     * Creates a new {@link StorPoolDefinitionData} without persisting it to the database
     * @throws DatabaseException
     */
    public static StorPoolDefinitionData createStorPoolDefinitionData(
        UUID uuidRef,
        ObjectProtection objProtRef,
        StorPoolName spNameRef,
        StorPoolDefinitionDataGenericDbDriver driverRef,
        PropsContainerFactory propsContainerFactoryRef,
        TransactionObjectFactory transObjFactoryRef,
        Provider<? extends TransactionMgr> transMgrProviderRef,
        Map<NodeName, StorPool> storPoolsMapRef
    )
        throws DatabaseException
    {
        return new StorPoolDefinitionData(
            uuidRef,
            objProtRef,
            spNameRef,
            driverRef,
            propsContainerFactoryRef,
            transObjFactoryRef,
            transMgrProviderRef,
            storPoolsMapRef
        );
    }

    /**
     * Creates a new {@link VolumeConnectionData} without persisting it to the database
     * @throws DatabaseException
     */
    public static VolumeConnectionData createVolumeConnectionData(
        UUID uuidRef,
        VolumeData volSrcRef,
        VolumeData volDstRef,
        VolumeConnectionDataGenericDbDriver driverRef,
        PropsContainerFactory propsContainerFactoryRef,
        TransactionObjectFactory transObjFactoryRef,
        Provider<? extends TransactionMgr> transMgrProviderRef
    )
        throws DatabaseException
    {
        return new VolumeConnectionData(
            uuidRef,
            volSrcRef,
            volDstRef,
            driverRef,
            propsContainerFactoryRef,
            transObjFactoryRef,
            transMgrProviderRef
        );
    }

    /**
     * Creates a new {@link VolumeData} without persisting it to the database
     * @throws DatabaseException
     */
    public static VolumeData createVolumeData(
        UUID uuidRef,
        ResourceData resRef,
        VolumeDefinitionData volDfnRef,
        long flagValueRef,
        VolumeDataGenericDbDriver driverRef,
        PropsContainerFactory propsContainerFactoryRef,
        TransactionObjectFactory transObjFactoryRef,
        Provider<? extends TransactionMgr> transMgrProviderRef,
        Map<Volume.Key, VolumeConnection> vlmConnsMapRef
    )
        throws DatabaseException
    {
        return new VolumeData(
            uuidRef,
            resRef,
            volDfnRef,
            flagValueRef,
            driverRef,
            propsContainerFactoryRef,
            transObjFactoryRef,
            transMgrProviderRef,
            vlmConnsMapRef
        );
    }

    /**
     * Creates a new {@link VolumeDefinitionData} without persisting it to the database
     * @throws MdException
     * @throws DatabaseException
     */
    public static VolumeDefinitionData VolumeDefinitionData(
        UUID uuidRef,
        ResourceDefinition resDfnRef,
        VolumeNumber volNrRef,
        long volSizeRef,
        long flagValueRef,
        VolumeDefinitionDataGenericDbDriver driverRef,
        PropsContainerFactory propsContainerFactoryRef,
        TransactionObjectFactory transObjFactoryRef,
        Provider<? extends TransactionMgr> transMgrProviderRef,
        Map<String, Volume> vlmMapRef,
        Map<Pair<DeviceLayerKind, String>, VlmDfnLayerObject> layerDataMapRef
    )
        throws DatabaseException, MdException
    {
        return new VolumeDefinitionData(
            uuidRef,
            resDfnRef,
            volNrRef,
            volSizeRef,
            flagValueRef,
            driverRef,
            propsContainerFactoryRef,
            transObjFactoryRef,
            transMgrProviderRef,
            vlmMapRef,
            layerDataMapRef
        );
    }

}
