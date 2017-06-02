package com.linbit.drbdmanage.security;

import com.linbit.ImplementationError;
import com.linbit.InvalidNameException;
import com.linbit.drbdmanage.ControllerDatabase;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Represents the type of an object protected by access controls
 * or the domain of a subject
 *
 * @author Robert Altnoeder &lt;robert.altnoeder@linbit.com&gt;
 */
public final class SecurityType implements Comparable<SecurityType>
{
    private static final Map<SecTypeName, SecurityType> GLOBAL_TYPE_MAP = new TreeMap<>();
    private static final ReadWriteLock GLOBAL_TYPE_MAP_LOCK = new ReentrantReadWriteLock();

    // Name of this security type
    public final SecTypeName name;

    // Access control rules for this type
    private final Map<SecTypeName, AccessType> rules;

    static final SecurityType SYSTEM_TYPE;
    static final SecurityType PUBLIC_TYPE;

    static
    {
        try
        {
            SYSTEM_TYPE = new SecurityType(new SecTypeName("SYSTEM"));
            PUBLIC_TYPE = new SecurityType(new SecTypeName("PUBLIC"));
        }
        catch (InvalidNameException nameExc)
        {
            throw new ImplementationError(
                "The name constant of a builtin security type/domain is invalid",
                nameExc
            );
        }
    }

    SecurityType(SecTypeName typeName)
    {
        name = typeName;
        rules = Collections.synchronizedMap(new TreeMap<SecTypeName, AccessType>());
    }

    public static SecurityType create(AccessContext accCtx, SecTypeName typeName)
        throws AccessDeniedException
    {
        accCtx.privEffective.requirePrivileges(Privilege.PRIV_SYS_ALL);

        Lock writeLock = GLOBAL_TYPE_MAP_LOCK.writeLock();

        SecurityType secTypeObj;
        try
        {
            writeLock.lock();
            secTypeObj = GLOBAL_TYPE_MAP.get(typeName);
            if (secTypeObj == null)
            {
                secTypeObj = new SecurityType(typeName);
                GLOBAL_TYPE_MAP.put(typeName, secTypeObj);
            }
        }
        finally
        {
            writeLock.unlock();
        }
        return secTypeObj;
    }

    public static SecurityType get(SecTypeName typeName)
    {
        Lock readLock = GLOBAL_TYPE_MAP_LOCK.readLock();

        SecurityType secTypeObj;
        try
        {
            readLock.lock();
            secTypeObj = GLOBAL_TYPE_MAP.get(typeName);
        }
        finally
        {
            readLock.unlock();
        }
        return secTypeObj;
    }

    public static Set<SecurityType> getAll()
    {
        Lock readLock = GLOBAL_TYPE_MAP_LOCK.readLock();

        Set<SecurityType> result = new TreeSet<>();
        try
        {
            readLock.lock();
            result.addAll(GLOBAL_TYPE_MAP.values());
        }
        finally
        {
            readLock.unlock();
        }
        return result;
    }

    static void load(ControllerDatabase ctrlDb, DbAccessor secDb)
        throws SQLException, InvalidNameException
    {
        Connection dbConn = ctrlDb.getConnection();
        if (dbConn == null)
        {
            throw new SQLException(
                "The controller database connection pool failed to provide a database connection"
            );
        }

        Lock writeLock = GLOBAL_TYPE_MAP_LOCK.writeLock();

        try
        {
            writeLock.lock();
            GLOBAL_TYPE_MAP.clear();

            GLOBAL_TYPE_MAP.put(SYSTEM_TYPE.name, SYSTEM_TYPE);
            GLOBAL_TYPE_MAP.put(PUBLIC_TYPE.name, PUBLIC_TYPE);

            // Load all security types
            {
                ResultSet loadData = secDb.loadSecurityTypes(dbConn);
                while (loadData.next())
                {
                    String dspName = loadData.getString(SecurityDbFields.TYPE_DSP_NAME);
                    SecTypeName typeName = new SecTypeName(dspName);
                    if (!typeName.equals(SYSTEM_TYPE.name) &&
                        !typeName.equals(PUBLIC_TYPE.name))
                    {
                        SecurityType secType = new SecurityType(typeName);
                        GLOBAL_TYPE_MAP.put(typeName, secType);
                    }
                }
            }

            // Load type enforcement rules
            {
                ResultSet loadData = secDb.loadTeRules(dbConn);
                while (loadData.next())
                {
                    String domainNameStr = loadData.getString(SecurityDbFields.DOMAIN_NAME);
                    String typeNameStr = loadData.getString(SecurityDbFields.TYPE_NAME);
                    String accTypeStr = loadData.getString(SecurityDbFields.ACCESS_TYPE);

                    SecurityType secDomain = get(new SecTypeName(domainNameStr));
                    SecurityType secType = get(new SecTypeName(typeNameStr));
                    AccessType accType = AccessType.get(accTypeStr);

                    secType.rules.put(secDomain.name, accType);
                }
            }
        }
        finally
        {
            writeLock.unlock();
            ctrlDb.returnConnection(dbConn);
        }
    }

    /**
     * Checks whether the subject domain has the requested type of access
     * to objects with the security type of this instance
     *
     * @param context The security context of the subject requesting access
     * @param requested The type of access requested by the subject
     * @throws AccessDeniedException If access is denied
     */
    public void requireAccess(AccessContext context, AccessType requested)
        throws AccessDeniedException
    {
        SecurityLevel globalSecLevel = SecurityLevel.get();
        switch (globalSecLevel)
        {
            case NO_SECURITY:
                break;
            case RBAC:
                break;
            case MAC:
                {
                    boolean allowFlag = false;

                    // Get the name of the subject's security domain
                    SecTypeName typeName = context.subjectDomain.name;

                    // Look for a rule allowing a certain type of access
                    // between from the subject's security domain to this
                    // security type
                    AccessType accType = rules.get(typeName);

                    // If a rule entry was found, check whether the requested type
                    // of access is within the bounds of the type of access
                    // allowed by the rule entry
                    if (accType != null)
                    {
                        allowFlag = accType.hasAccess(requested);
                    }

                    // Allow access if the current context has MAC_OVRD privileges
                    if (!allowFlag)
                    {
                        allowFlag = context.privEffective.hasPrivileges(Privilege.PRIV_MAC_OVRD);
                    }

                    if (!allowFlag)
                    {
                        throw new AccessDeniedException(
                            "Access of type '" + requested +
                            "' not allowed by mandatory access control rules",
                            // Description
                            "Access was denied by mandatory access control rules",
                            // Cause
                            "No rule is present that allows access of type " + requested.name() +
                            " by a subject in security domain " + context.subjectDomain.name +
                            " to an object of security type " + name,
                            // Correction
                            "A rule defining the allowed level of access from the " +
                            "subject domain to the object type must be defined.\n" +
                            "Mandatory access control rules can only be defined by a role " +
                            "with administrative privileges.",
                            // No error details
                            null
                        );
                    }
                }
                break;
            default:
                throw new ImplementationError(
                    "Missing case label for enum constant " + globalSecLevel.name(),
                    null
                );
        }
    }

    /**
     * Returns the level of access to an object with the security type of this instance
     * that is granted to the specified security context
     *
     * @param context The security context of the subject requesting access
     * @return Allowed AccessType, or null if access is denied
     */
    public AccessType queryAccess(AccessContext context)
    {
        AccessType result = null;
        SecurityLevel globalSecLevel = SecurityLevel.get();
        switch (globalSecLevel)
        {
            case NO_SECURITY:
                // fall-through
            case RBAC:
                result = AccessType.CONTROL;
                break;
            case MAC:
                // Query the level of access allowed by privileges
                AccessType privAccess = context.privEffective.toMacAccess();

                // Get the name of the subject's security domain
                SecTypeName typeName = context.subjectDomain.name;

                // Look for a rule allowing a certain type of access
                // between from the subject's security domain to this
                // security type
                AccessType ruleAccess = rules.get(typeName);

                // Combine access permissions
                result = AccessType.union(privAccess, ruleAccess);
                break;
            default:
                throw new AssertionError(globalSecLevel.name());
        }
        return result;
    }

    /**
     * Returns the level of access granted to an object of the security type of
     * this instance by an access control rule
     * @param context Security context specifying the subject domain
     * @return Allowed level of access, or null if access is denied
     */
    public AccessType getRule(AccessContext context)
    {
        SecurityType domain = context.subjectDomain;
        return SecurityType.this.getRule(domain);
    }

    /**
     * Returns the level of access granted to an object of the security type
     * of this instance to the specified security domain by an access control rule
     *
     * @param domain The security domain to find an access control rule for
     * @return Allowed level of access, or null if access is denied
     */
    public AccessType getRule(SecurityType domain)
    {
        return rules.get(domain.name);
    }

    public void addRule(AccessContext context, SecurityType domain, AccessType grantedAccess)
        throws AccessDeniedException
    {
        PrivilegeSet privs = context.getEffectivePrivs();
        privs.requirePrivileges(Privilege.PRIV_SYS_ALL);
        rules.put(domain.name, grantedAccess);
    }

    public void delRule(AccessContext context, SecurityType domain)
        throws AccessDeniedException
    {
        PrivilegeSet privs = context.getEffectivePrivs();
        privs.requirePrivileges(Privilege.PRIV_SYS_ALL);
        rules.remove(domain.name);
    }

    public Map<SecTypeName, AccessType> getAllRules(AccessContext context)
        throws AccessDeniedException
    {
        context.getEffectivePrivs().requirePrivileges(Privilege.PRIV_SYS_ALL);

        Map<SecTypeName, AccessType> result = new TreeMap<>();
        result.putAll(rules);
        return result;
    }

    @Override
    public String toString()
    {
        return name.displayValue;
    }

    @Override
    public int compareTo(SecurityType other)
    {
        return this.name.compareTo(other.name);
    }

    @Override
    public int hashCode()
    {
       return this.name.hashCode();
    }

    @Override
    public boolean equals(Object other)
    {
        boolean equals = false;
        if (other != null && other instanceof SecurityType)
        {
            equals = this.name.equals(((SecurityType) other).name);
        }
        return equals;
    }
}
