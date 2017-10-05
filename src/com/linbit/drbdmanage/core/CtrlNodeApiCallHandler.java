package com.linbit.drbdmanage.core;

import java.sql.SQLException;
import java.util.Map;

import com.linbit.ImplementationError;
import com.linbit.InvalidNameException;
import com.linbit.TransactionMgr;
import com.linbit.drbdmanage.ApiCallRc;
import com.linbit.drbdmanage.ApiCallRcConstants;
import com.linbit.drbdmanage.ApiCallRcImpl;
import com.linbit.drbdmanage.DrbdDataAlreadyExistsException;
import com.linbit.drbdmanage.Node;
import com.linbit.drbdmanage.NodeData;
import com.linbit.drbdmanage.NodeName;
import com.linbit.drbdmanage.ApiCallRcImpl.ApiCallRcEntry;
import com.linbit.drbdmanage.Node.NodeFlag;
import com.linbit.drbdmanage.Node.NodeType;
import com.linbit.drbdmanage.api.ApiConsts;
import com.linbit.drbdmanage.netcom.Peer;
import com.linbit.drbdmanage.security.AccessContext;
import com.linbit.drbdmanage.security.AccessDeniedException;
import com.linbit.drbdmanage.security.AccessType;

class CtrlNodeApiCallHandler
{
    private final Controller controller;

    CtrlNodeApiCallHandler(Controller controllerRef)
    {
        controller = controllerRef;
    }

    public ApiCallRc createNode(AccessContext accCtx, Peer client, String nodeNameStr, Map<String, String> props)
    {
        /*
         * Usually its better to handle exceptions "close" to their appearance.
         * However, as in this method almost every other line throws an exception,
         * the code would get completely unreadable; thus, unmaintainable.
         *
         * For that reason there is (almost) only one try block with many catches, and
         * those catch blocks handle the different cases (commented as <some>Exc<count> in
         * the try block and a matching "handle <some>Exc<count>" in the catch block)
         */

        ApiCallRcImpl apiCallRc = new ApiCallRcImpl();

        TransactionMgr transMgr = null;
        Node node = null;
        try
        {
            controller.nodesMapProt.requireAccess(accCtx, AccessType.CHANGE);// accDeniedExc1
            transMgr = new TransactionMgr(controller.dbConnPool.getConnection()); // sqlExc1
            NodeName nodeName = new NodeName(nodeNameStr); // invalidNameExc1

            NodeType type = NodeType.valueOfIgnoreCase(props.get(ApiConsts.KEY_NODE_TYPE), NodeType.SATELLITE);
            NodeFlag[] flags = NodeFlag.valuesOfIgnoreCase(props.get(ApiConsts.KEY_NODE_FLAGS));
            node = NodeData.getInstance( // sqlExc2, accDeniedExc2, alreadyExists1
                accCtx,
                nodeName,
                type,
                flags,
                transMgr,
                true,
                true
            );

            transMgr.commit(); // sqlExc3

            ApiCallRcEntry entry = new ApiCallRcEntry();
            entry.setReturnCodeBit(ApiCallRcConstants.RC_NODE_CREATED);
            entry.setMessageFormat("Node ${" + ApiConsts.KEY_NODE_NAME + "} successfully created");
            entry.putVariable(ApiConsts.KEY_NODE_NAME, nodeNameStr);
            entry.putObjRef(ApiConsts.KEY_NODE, nodeNameStr);

            apiCallRc.addEntry(entry);
            controller.nodesMap.put(nodeName, node);
            controller.getErrorReporter().logInfo(
                "Node [%s] successfully created",
                nodeNameStr
            );
        }
        catch (SQLException sqlExc)
        {
            if (transMgr == null)
            { // handle sqlExc1
                controller.getErrorReporter().reportError(
                    sqlExc,
                    accCtx,
                    client,
                    "A database error occured while trying to create a new transaction."
                );

                ApiCallRcEntry entry = new ApiCallRcEntry();
                entry.setReturnCodeBit(ApiCallRcConstants.RC_NODE_CREATION_FAILED);
                entry.setMessageFormat("Failed to create database transaction");
                entry.setCauseFormat(sqlExc.getMessage());
                entry.putObjRef(ApiConsts.KEY_NODE, nodeNameStr);

                apiCallRc.addEntry(entry);
            }
            else
            if (node == null)
            { // handle sqlExc2
                controller.getErrorReporter().reportError(
                    sqlExc,
                    accCtx,
                    client,
                    "A database error occured while trying to persist the node."
                );

                ApiCallRcEntry entry = new ApiCallRcEntry();
                entry.setReturnCodeBit(ApiCallRcConstants.RC_NODE_CREATION_FAILED);
                entry.setMessageFormat("Failed to persist node.");
                entry.setCauseFormat(sqlExc.getMessage());
                entry.putObjRef(ApiConsts.KEY_NODE, nodeNameStr);

                apiCallRc.addEntry(entry);
            }
            else
            {
                // handle sqlExc3

                controller.getErrorReporter().reportError(
                    sqlExc,
                    accCtx,
                    client,
                    "A database error occured while trying to commit the transaction."
                );

                ApiCallRcEntry entry = new ApiCallRcEntry();
                entry.setReturnCodeBit(ApiCallRcConstants.RC_NODE_CREATION_FAILED);
                entry.setMessageFormat("Failed to commit transaction");
                entry.setCauseFormat(sqlExc.getMessage());
                entry.putObjRef(ApiConsts.KEY_NODE, nodeNameStr);

                apiCallRc.addEntry(entry);
            }
        }
        catch (InvalidNameException invalidNameExc)
        {
            // handle invalidNameExc1

            controller.getErrorReporter().reportError(
                invalidNameExc,
                accCtx,
                client,
                "The given name for the node is invalid"
            );

            ApiCallRcEntry entry = new ApiCallRcEntry();
            entry.setReturnCodeBit(ApiCallRcConstants.RC_NODE_CREATION_FAILED);
            entry.setMessageFormat("The given node name '${" + ApiConsts.KEY_NODE_NAME + "}' is invalid");
            entry.setCauseFormat(invalidNameExc.getMessage());

            entry.putObjRef(ApiConsts.KEY_NODE, nodeNameStr);
            entry.putVariable(ApiConsts.KEY_NODE_NAME, nodeNameStr);

            apiCallRc.addEntry(entry);
        }
        catch (AccessDeniedException accDeniedExc)
        {
            // handle accDeniedExc1 && accDeniedExc2
            controller.getErrorReporter().reportError(
                accDeniedExc,
                accCtx,
                client,
                "The given access context has no permission to create a new node"
            );

            ApiCallRcEntry entry = new ApiCallRcEntry();
            entry.setReturnCodeBit(ApiCallRcConstants.RC_NODE_CREATION_FAILED);
            entry.setMessageFormat("The given access context has no permission to create a new node");
            entry.setCauseFormat(accDeniedExc.getMessage());
            entry.putObjRef(ApiConsts.KEY_NODE, nodeNameStr);

            apiCallRc.addEntry(entry);
        }
        catch (DrbdDataAlreadyExistsException alreadyExistsExc)
        {
            // handle alreadyExists1

            controller.getErrorReporter().reportError(
                alreadyExistsExc,
                accCtx,
                client,
                "The node which should be created already exists"
            );

            ApiCallRcEntry entry = new ApiCallRcEntry();
            entry.setReturnCodeBit(ApiCallRcConstants.RC_NODE_CREATION_FAILED);
            entry.setMessageFormat("The node already exists");
            entry.setCauseFormat(alreadyExistsExc.getMessage());
            entry.putObjRef(ApiConsts.KEY_NODE, nodeNameStr);

            apiCallRc.addEntry(entry);
        }

        if (transMgr != null)
        {
            if (transMgr.isDirty())
            {
                try
                {
                    transMgr.rollback();
                }
                catch (SQLException sqlExc)
                {
                    controller.getErrorReporter().reportError(
                        sqlExc,
                        accCtx,
                        client,
                        "A database error occured while trying to rollback the transaction."
                    );

                    ApiCallRcEntry entry = new ApiCallRcEntry();
                    entry.setReturnCodeBit(ApiCallRcConstants.RC_NODE_CREATION_FAILED);
                    entry.setMessageFormat("Failed to rollback database transaction");
                    entry.setCauseFormat(sqlExc.getMessage());
                    entry.putObjRef(ApiConsts.KEY_NODE, nodeNameStr);

                    apiCallRc.addEntry(entry);
                }
            }
            controller.dbConnPool.returnConnection(transMgr.dbCon);
        }
        return apiCallRc;
    }

    public ApiCallRc deleteNode(AccessContext accCtx, Peer client, String nodeNameStr)
    {
        ApiCallRcImpl apiCallRc = new ApiCallRcImpl();

        TransactionMgr transMgr = null;
        NodeData nodeData = null;

        try
        {
            controller.nodesMapProt.requireAccess(accCtx, AccessType.CHANGE);// accDeniedExc1
            transMgr = new TransactionMgr(controller.dbConnPool.getConnection());// sqlExc1
            NodeName nodeName = new NodeName(nodeNameStr); // invalidNameExc1
            nodeData = NodeData.getInstance( // sqlExc2, accDeniedExc2, dataAlreadyExistsExc1
                accCtx,
                nodeName,
                null, null,
                transMgr,
                false,
                false
            );
            if (nodeData != null)
            {
                nodeData.setConnection(transMgr);
                nodeData.markDeleted(accCtx); // sqlExc3, accDeniedExc3
                transMgr.commit(); // sqlExc4

                ApiCallRcEntry entry = new ApiCallRcEntry();
                entry.setReturnCodeBit(ApiCallRcConstants.RC_NODE_DELETED);
                entry.setMessageFormat("Node ${" + ApiConsts.KEY_NODE_NAME + "} successfully deleted");
                entry.putObjRef(ApiConsts.KEY_NODE, nodeNameStr);
                entry.putVariable(ApiConsts.KEY_NODE_NAME, nodeNameStr);
                apiCallRc.addEntry(entry);
                controller.getErrorReporter().logInfo(
                    "Node [%s] marked to be deleted",
                    nodeNameStr
                );
            }
            else
            {
                ApiCallRcEntry entry = new ApiCallRcEntry();
                entry.setReturnCodeBit(ApiCallRcConstants.RC_NODE_NOT_FOUND);
                entry.setMessageFormat("Node ${" + ApiConsts.KEY_NODE_NAME + "} was not deleted as it was not found");
                entry.putObjRef(ApiConsts.KEY_NODE, nodeNameStr);
                entry.putVariable(ApiConsts.KEY_NODE_NAME, nodeNameStr);
                apiCallRc.addEntry(entry);
                controller.getErrorReporter().logInfo(
                    "Non existing Node [%s] could not be deleted",
                    nodeNameStr
                );
            }
        }
        catch (SQLException sqlExc)
        {
            if (transMgr == null)
            { // handle sqlExc1
                controller.getErrorReporter().reportError(
                    sqlExc,
                    accCtx,
                    client,
                    "A database error occured while trying to create a new transaction."
                );

                ApiCallRcEntry entry = new ApiCallRcEntry();
                entry.setReturnCodeBit(ApiCallRcConstants.RC_NODE_DELETION_FAILED);
                entry.setMessageFormat("Failed to create database transaction");
                entry.setCauseFormat(sqlExc.getMessage());
                entry.putObjRef(ApiConsts.KEY_NODE, nodeNameStr);

                apiCallRc.addEntry(entry);
            }
            else
            if (nodeData == null)
            { // handle sqlExc2
                controller.getErrorReporter().reportError(
                    sqlExc,
                    accCtx,
                    client,
                    "A database error occured while trying to load the node."
                );

                ApiCallRcEntry entry = new ApiCallRcEntry();
                entry.setReturnCodeBit(ApiCallRcConstants.RC_NODE_CREATION_FAILED);
                entry.setMessageFormat("Failed to load node ${" + ApiConsts.KEY_NODE_NAME + "} for deletion.");
                entry.setCauseFormat(sqlExc.getMessage());
                entry.putObjRef(ApiConsts.KEY_NODE, nodeNameStr);
                entry.putVariable(ApiConsts.KEY_NODE_NAME, nodeNameStr);

                apiCallRc.addEntry(entry);
            }
            else
            if (!nodeData.isDeleted())
            { // handle sqlExc3
                controller.getErrorReporter().reportError(
                    sqlExc,
                    accCtx,
                    client,
                    "A database error occured while trying to delete the node."
                );

                ApiCallRcEntry entry = new ApiCallRcEntry();
                entry.setReturnCodeBit(ApiCallRcConstants.RC_NODE_DELETION_FAILED);
                entry.setMessageFormat("Failed to delete the node ${" + ApiConsts.KEY_NODE_NAME + "}.");
                entry.setCauseFormat(sqlExc.getMessage());
                entry.putObjRef(ApiConsts.KEY_NODE, nodeNameStr);
                entry.putVariable(ApiConsts.KEY_NODE_NAME, nodeNameStr);

                apiCallRc.addEntry(entry);
            }
            else
            { // handle sqlExc4
                controller.getErrorReporter().reportError(
                    sqlExc,
                    accCtx,
                    client,
                    "A database error occured while trying to commit the transaction."
                );

                ApiCallRcEntry entry = new ApiCallRcEntry();
                entry.setReturnCodeBit(ApiCallRcConstants.RC_NODE_DELETION_FAILED);
                entry.setMessageFormat("Failed to commit transaction");
                entry.setCauseFormat(sqlExc.getMessage());
                entry.putObjRef(ApiConsts.KEY_NODE, nodeNameStr);

                apiCallRc.addEntry(entry);
            }
        }
        catch (InvalidNameException invalidNameExc)
        {
            // handle invalidNameExc1

            controller.getErrorReporter().reportError(
                invalidNameExc,
                accCtx,
                client,
                "The given name for the node is invalid"
            );

            ApiCallRcEntry entry = new ApiCallRcEntry();
            entry.setReturnCodeBit(ApiCallRcConstants.RC_NODE_DELETION_FAILED);
            entry.setMessageFormat("The given node name '${" + ApiConsts.KEY_NODE_NAME + "}' is invalid");
            entry.setCauseFormat(invalidNameExc.getMessage());

            entry.putObjRef(ApiConsts.KEY_NODE, nodeNameStr);
            entry.putVariable(ApiConsts.KEY_NODE_NAME, nodeNameStr);

            apiCallRc.addEntry(entry);
        }
        catch (AccessDeniedException accDeniedExc)
        {
            // handle accDeniedExc1 && accDeniedExc2 && accDeniedExc3
            controller.getErrorReporter().reportError(
                accDeniedExc,
                accCtx,
                client,
                "The given access context has no permission to create a new node"
            );

            ApiCallRcEntry entry = new ApiCallRcEntry();
            entry.setReturnCodeBit(ApiCallRcConstants.RC_NODE_DELETION_FAILED);
            entry.setMessageFormat("The given access context has no permission to delete the node ${" + ApiConsts.KEY_NODE_NAME + "}.");
            entry.setCauseFormat(accDeniedExc.getMessage());
            entry.putObjRef(ApiConsts.KEY_NODE, nodeNameStr);
            entry.putVariable(ApiConsts.KEY_NODE_NAME, nodeNameStr);

            apiCallRc.addEntry(entry);
        }
        catch (DrbdDataAlreadyExistsException dataAlreadyExistsExc)
        {
            controller.getErrorReporter().reportError(
                new ImplementationError(
                    ".getInstance was called with failIfExists=false, still threw an AlreadyExistsException",
                    dataAlreadyExistsExc
                )
            );

            ApiCallRcEntry entry = new ApiCallRcEntry();
            entry.setReturnCodeBit(ApiCallRcConstants.RC_NODE_DELETION_FAILED);
            entry.setMessageFormat("Failed to delete the node ${" + ApiConsts.KEY_NODE_NAME + "} due to an implementation error.");
            entry.setCauseFormat(dataAlreadyExistsExc.getMessage());
            entry.putObjRef(ApiConsts.KEY_NODE, nodeNameStr);
            entry.putVariable(ApiConsts.KEY_NODE_NAME, nodeNameStr);

            apiCallRc.addEntry(entry);
        }

        if (transMgr != null)
        {
            if (transMgr.isDirty())
            {
                try
                {
                    transMgr.rollback();
                }
                catch (SQLException sqlExc)
                {
                    controller.getErrorReporter().reportError(
                        sqlExc,
                        accCtx,
                        client,
                        "A database error occured while trying to rollback the transaction."
                    );

                    ApiCallRcEntry entry = new ApiCallRcEntry();
                    entry.setReturnCodeBit(ApiCallRcConstants.RC_NODE_DELETION_FAILED);
                    entry.setMessageFormat("Failed to rollback database transaction");
                    entry.setCauseFormat(sqlExc.getMessage());
                    entry.putObjRef(ApiConsts.KEY_NODE, nodeNameStr);

                    apiCallRc.addEntry(entry);
                }
            }
            controller.dbConnPool.returnConnection(transMgr.dbCon);
        }

        return apiCallRc;
    }

}
