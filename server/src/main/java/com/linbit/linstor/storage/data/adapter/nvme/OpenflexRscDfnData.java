package com.linbit.linstor.storage.data.adapter.nvme;

import com.linbit.linstor.api.pojo.OpenflexRscPojo.OpenflexRscDfnPojo;
import com.linbit.linstor.core.identifier.ResourceName;
import com.linbit.linstor.core.identifier.SnapshotName;
import com.linbit.linstor.core.objects.AbsResource;
import com.linbit.linstor.dbdrivers.DatabaseException;
import com.linbit.linstor.dbdrivers.interfaces.OpenflexLayerDatabaseDriver;
import com.linbit.linstor.security.AccessContext;
import com.linbit.linstor.storage.interfaces.layers.nvme.OpenflexRscDfnObject;
import com.linbit.linstor.storage.kinds.DeviceLayerKind;
import com.linbit.linstor.transaction.BaseTransactionObject;
import com.linbit.linstor.transaction.TransactionList;
import com.linbit.linstor.transaction.TransactionObjectFactory;
import com.linbit.linstor.transaction.TransactionSimpleObject;
import com.linbit.linstor.transaction.manager.TransactionMgr;

import javax.inject.Provider;

import java.util.Arrays;
import java.util.List;

public class OpenflexRscDfnData<RSC extends AbsResource<RSC>>
    extends BaseTransactionObject
    implements OpenflexRscDfnObject
{
    // unmodifiable data, once initialized
    private final ResourceName rscName;
    private final String resourceNameSuffix;
    private final OpenflexLayerDatabaseDriver ofDbDriver;

    private final String suffixedResourceName;

    // persisted, serialized, ctrl and stlt
    private final TransactionList<OpenflexRscDfnData<?>, OpenflexRscData<RSC>> ofRscDataList;
    private final TransactionSimpleObject<OpenflexRscDfnData<?>, String> nqn;

    public OpenflexRscDfnData(
        ResourceName rscNameRef,
        String rscNameSuffixRef,
        List<OpenflexRscData<RSC>> ofRscDataListRef,
        String nqnRef,
        OpenflexLayerDatabaseDriver dbDriverRef,
        TransactionObjectFactory transObjFactoryRef,
        Provider<? extends TransactionMgr> transMgrProviderRef
    )
        throws DatabaseException
    {
        super(transMgrProviderRef);
        rscName = rscNameRef;
        resourceNameSuffix = rscNameSuffixRef;
        ofDbDriver = dbDriverRef;

        suffixedResourceName = rscNameRef.displayValue + rscNameSuffixRef;

        nqn = transObjFactoryRef.createTransactionSimpleObject(this, nqnRef, dbDriverRef.getNqnDriver());
        ofRscDataList = transObjFactoryRef.createTransactionList(this, ofRscDataListRef, null);

        transObjs = Arrays.asList(
            nqn,
            ofRscDataList
        );
    }

    public List<OpenflexRscData<RSC>> getOfRscDataList()
    {
        return ofRscDataList;
    }

    @Override
    public String getRscNameSuffix()
    {
        return resourceNameSuffix;
    }

    @Override
    public ResourceName getResourceName()
    {
        return rscName;
    }

    @Override
    public SnapshotName getSnapshotName()
    {
        return null; // not supported
    }

    @Override
    public void delete() throws DatabaseException
    {
        ofDbDriver.delete(this);
    }

    @Override
    public OpenflexRscDfnPojo getApiData(AccessContext accCtxRef)
    {
        return new OpenflexRscDfnPojo(
            resourceNameSuffix,
            nqn.get()
        );
    }

    @Override
    public DeviceLayerKind getLayerKind()
    {
        return DeviceLayerKind.OPENFLEX;
    }

    @Override
    public String getNqn()
    {
        return nqn.get();
    }

    public void setNqn(String nqnRef) throws DatabaseException
    {
        nqn.set(nqnRef);
    }

}
