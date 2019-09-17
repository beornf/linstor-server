package com.linbit.linstor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.linbit.InvalidNameException;
import com.linbit.linstor.core.identifier.NodeName;
import com.linbit.linstor.core.identifier.ResourceName;
import com.linbit.linstor.core.objects.Node;
import com.linbit.linstor.core.objects.ResourceDefinition.InitMaps;
import com.linbit.linstor.core.objects.ResourceDefinition.RscDfnFlags;
import com.linbit.linstor.core.objects.ResourceDefinition.TransportType;
import com.linbit.linstor.core.objects.ResourceDefinitionData;
import com.linbit.linstor.core.objects.ResourceDefinitionDataGenericDbDriver;
import com.linbit.linstor.core.objects.ResourceGroupData;
import com.linbit.linstor.core.objects.TestFactory;
import com.linbit.linstor.core.types.NodeId;
import com.linbit.linstor.dbdrivers.SQLUtils;
import com.linbit.linstor.propscon.Props;
import com.linbit.linstor.propscon.PropsContainer;
import com.linbit.linstor.security.GenericDbBase;
import com.linbit.linstor.security.ObjectProtection;
import com.linbit.linstor.security.ObjectProtectionDatabaseDriver;
import com.linbit.linstor.storage.kinds.DeviceLayerKind;

import javax.inject.Inject;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Before;
import org.junit.Test;

public class ResourceDefinitionDataGenericDbDriverTest extends GenericDbBase
{
    private static final String SELECT_ALL_RESOURCE_DEFINITIONS =
        " SELECT " + UUID + ", " + RESOURCE_NAME + ", " + RESOURCE_DSP_NAME + ", " +
                     RESOURCE_FLAGS + ", " + LAYER_STACK +
        " FROM " + TBL_RESOURCE_DEFINITIONS;

    private final ResourceName resName;
    private final int port;
    private final NodeName nodeName;
    private final String secret;
    private final TransportType transportType;

    private java.util.UUID rscDfnUuid;
    private ObjectProtection rscDfnObjProt;

    private NodeId node1Id;
    private Node node1;

    private ResourceDefinitionData resDfn;
    @Inject private ResourceDefinitionDataGenericDbDriver driver;
    @Inject private ObjectProtectionDatabaseDriver objProtDriver;

    private ResourceGroupData dfltRscGrp;

    @SuppressWarnings("checkstyle:magicnumber")
    public ResourceDefinitionDataGenericDbDriverTest() throws InvalidNameException
    {
        resName = new ResourceName("TestResName");
        port = 4242;
        nodeName = new NodeName("TestNodeName1");
        secret = "secret";
        transportType = TransportType.IP;
    }

    @SuppressWarnings("checkstyle:magicnumber")
    @Before
    public void setUp() throws Exception
    {
        super.setUpAndEnterScope();
        assertEquals(
            TBL_RESOURCE_DEFINITIONS + " table's column count has changed. Update tests accordingly!",
            7,
            TBL_COL_COUNT_RESOURCE_DEFINITIONS
        );

        rscDfnUuid = randomUUID();

        rscDfnObjProt = createTestObjectProtection(
            SYS_CTX,
            ObjectProtection.buildPath(resName)
        );

        dfltRscGrp = createDefaultResourceGroup(SYS_CTX);

        node1Id = new NodeId(1);
        node1 = nodeFactory.create(
            SYS_CTX,
            nodeName,
            null,
            null
        );
        nodesMap.put(node1.getName(), node1);
        resDfn = TestFactory.createResourceDefinitionData(
            rscDfnUuid,
            rscDfnObjProt,
            resName,
            null,
            RscDfnFlags.DELETE.flagValue,
            new ArrayList<>(),
            driver,
            propsContainerFactory,
            transObjFactory,
            transMgrProvider,
            new TreeMap<>(),
            new TreeMap<>(),
            new TreeMap<>(),
            new TreeMap<>(),
            dfltRscGrp
        );
    }

    @Test
    public void testPersist() throws Exception
    {
        driver.create(resDfn);
        commit();

        PreparedStatement stmt = getConnection().prepareStatement(SELECT_ALL_RESOURCE_DEFINITIONS);
        ResultSet resultSet = stmt.executeQuery();

        assertTrue("Database did not persist resourceDefinition", resultSet.next());
        assertEquals(rscDfnUuid, java.util.UUID.fromString(resultSet.getString(UUID)));
        assertEquals(resName.value, resultSet.getString(RESOURCE_NAME));
        assertEquals(resName.displayValue, resultSet.getString(RESOURCE_DSP_NAME));
        assertEquals(RscDfnFlags.DELETE.flagValue, resultSet.getLong(RESOURCE_FLAGS));
        assertFalse("Database persisted too many resourceDefinitions", resultSet.next());

        resultSet.close();
        stmt.close();
    }

    @Test
    public void testPersistGetInstance() throws Exception
    {
        resourceDefinitionDataFactory.create(
            SYS_CTX,
            resName,
            null,
            port,
            new RscDfnFlags[] {RscDfnFlags.DELETE},
            secret,
            transportType,
            Arrays.asList(DeviceLayerKind.DRBD, DeviceLayerKind.STORAGE),
            null,
            dfltRscGrp
        );

        commit();

        PreparedStatement stmt = getConnection().prepareStatement(SELECT_ALL_RESOURCE_DEFINITIONS);
        ResultSet resultSet = stmt.executeQuery();

        assertTrue("Database did not persist resource / resourceDefinition", resultSet.next());
        // uuid is now != resUuid because getInstance create a new resData object
        assertEquals(resName.value, resultSet.getString(RESOURCE_NAME));
        assertEquals(resName.displayValue, resultSet.getString(RESOURCE_DSP_NAME));
        assertEquals(RscDfnFlags.DELETE.flagValue, resultSet.getLong(RESOURCE_FLAGS));
        assertThat(SQLUtils.getAsStringListFromVarchar(resultSet, LAYER_STACK))
            .containsExactly(DeviceLayerKind.DRBD.name(), DeviceLayerKind.STORAGE.name());
        assertFalse("Database persisted too many resources / resourceDefinitions", resultSet.next());

        resultSet.close();
        stmt.close();
    }

    @Test
    public void testDelete() throws Exception
    {
        driver.create(resDfn);
        commit();

        PreparedStatement stmt = getConnection().prepareStatement(SELECT_ALL_RESOURCE_DEFINITIONS);
        ResultSet resultSet = stmt.executeQuery();

        assertTrue("Database did not persist resourceDefinition", resultSet.next());
        resultSet.close();

        driver.delete(resDfn);
        commit();

        resultSet = stmt.executeQuery();
        assertFalse("Database did not delete resourceDefinition", resultSet.next());

        resultSet.close();
        stmt.close();
    }

    @Test
    public void testPersistProps() throws Exception
    {
        driver.create(resDfn);

        Props props = resDfn.getProps(SYS_CTX);
        String testKey = "TestKey";
        String testValue = "TestValue";
        props.setProp(testKey, testValue);

        commit();

        Map<String, String> testMap = new HashMap<>();
        testMap.put(testKey, testValue);
        testProps(PropsContainer.buildPath(resName), testMap);
    }

    @Test
    public void testStateFlagPersistence() throws Exception
    {
        driver.create(resDfn);

        resDfn.getFlags().disableAllFlags(SYS_CTX);

        commit();

        PreparedStatement stmt = getConnection().prepareStatement(SELECT_ALL_RESOURCE_DEFINITIONS);
        ResultSet resultSet = stmt.executeQuery();

        assertTrue(resultSet.next());
        assertEquals(0, resultSet.getLong(RESOURCE_FLAGS));

        resultSet.close();
        stmt.close();
    }

    private ResourceDefinitionData findResourceDefinitionDatabyName(
        Map<ResourceDefinitionData, InitMaps> resourceDefDataMap,
        ResourceName spName)
    {
        ResourceDefinitionData rscDfnData = null;
        for (ResourceDefinitionData rdd : resourceDefDataMap.keySet())
        {
            if (rdd.getName().equals(spName))
            {
                rscDfnData = rdd;
                break;
            }
        }
        return rscDfnData;
    }

    @Test
    public void testLoadAll() throws Exception
    {
        driver.create(resDfn);
        ResourceName resName2 = new ResourceName("ResName2");
        resourceDefinitionDataFactory.create(
            SYS_CTX,
            resName2,
            null,
            port + 1, // prevent tcp-port-conflict
            null,
            "secret",
            transportType,
            new ArrayList<>(),
            null,
            dfltRscGrp
        );
        objProtDriver.insertOp(rscDfnObjProt);

        Map<ResourceDefinitionData, InitMaps> resourceDefDataList = driver.loadAll(
            Collections.singletonMap(dfltRscGrp.getName(), dfltRscGrp)
        );

        ResourceDefinitionData res1 = findResourceDefinitionDatabyName(resourceDefDataList, resName);
        ResourceDefinitionData res2 = findResourceDefinitionDatabyName(resourceDefDataList, resName2);
        assertNotNull(res1);
        assertNotNull(res2);
        assertNotEquals(res1, res2);
    }

    @Test (expected = LinStorDataAlreadyExistsException.class)
    public void testAlreadyExists() throws Exception
    {
        driver.create(resDfn);
        objProtDriver.insertOp(rscDfnObjProt);
        rscDfnMap.put(resName, resDfn);

        resourceDefinitionDataFactory.create(
            SYS_CTX, resName, null, port, null, "secret", transportType, new ArrayList<>(), null, dfltRscGrp
        );
    }
}
