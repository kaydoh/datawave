package datawave.webservice.query.logic;

import datawave.audit.SelectorExtractor;
import datawave.marking.MarkingFunctions;
import datawave.validation.ParameterValidator;
import datawave.webservice.common.audit.Auditor.AuditType;
import datawave.webservice.common.connection.AccumuloConnectionFactory;
import datawave.webservice.query.Query;
import datawave.webservice.query.cache.ResultsPage;
import datawave.webservice.query.configuration.GenericQueryConfiguration;
import datawave.webservice.query.exception.DatawaveErrorCode;
import datawave.webservice.query.exception.QueryException;
import datawave.webservice.query.result.event.ResponseObjectFactory;
import datawave.webservice.result.BaseResponse;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.commons.collections4.iterators.TransformIterator;

import java.security.Principal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface QueryLogic<T> extends Iterable<T>, Cloneable, ParameterValidator {
    
    /**
     * A mechanism to get the normalized query without actually setting up the query. This can be called with having to call initialize.
     *
     * The default implementation is to return the query string as the normalized query
     *
     * @param connection
     *            - Accumulo connector to use for this query
     * @param settings
     *            - query settings (query, begin date, end date, etc.)
     * @param runtimeQueryAuthorizations
     *            - authorizations that have been calculated for this query based on the caller and server.
     * @param expandFields
     *            - should unfielded terms be expanded
     * @param expandValues
     *            - should regex/ranges be expanded into discrete values
     */
    String getPlan(Connector connection, Query settings, Set<Authorizations> runtimeQueryAuthorizations, boolean expandFields, boolean expandValues)
                    throws Exception;
    
    /**
     * Implementations create a configuration using the connection, settings, and runtimeQueryAuthorizations.
     * 
     * @param connection
     *            - Accumulo connector to use for this query
     * @param settings
     *            - query settings (query, begin date, end date, etc.)
     * @param runtimeQueryAuthorizations
     *            - authorizations that have been calculated for this query based on the caller and server.
     * @throws Exception
     */
    GenericQueryConfiguration initialize(Connector connection, Query settings, Set<Authorizations> runtimeQueryAuthorizations) throws Exception;
    
    /**
     *
     * @param settings
     *            - query settings (query, begin date, end date, etc.)
     * @return list of selectors used in the Query
     */
    List<String> getSelectors(Query settings);
    
    SelectorExtractor getSelectorExtractor();
    
    /**
     * Implementations use the configuration to run their query. It is expected that initialize has already been called.
     * 
     * @param configuration
     *            Encapsulates all information needed to run a query (whether the query is a BatchScanner, a MapReduce job, etc)
     */
    void setupQuery(GenericQueryConfiguration configuration) throws Exception;
    
    /**
     * @return a copy of this instance
     */
    Object clone() throws CloneNotSupportedException;
    
    /**
     * @return priority from AccumuloConnectionFactory
     */
    AccumuloConnectionFactory.Priority getConnectionPriority();
    
    /**
     * @return Transformer that will convert Key,Value to a Result object
     */
    QueryLogicTransformer getTransformer(Query settings);
    
    QueryLogicTransformer getEnrichedTransformer(Query settings);
    
    default String getResponseClass(Query query) throws QueryException {
        try {
            QueryLogicTransformer t = this.getEnrichedTransformer(query);
            BaseResponse refResponse = t.createResponse(new ResultsPage());
            return refResponse.getClass().getCanonicalName();
        } catch (RuntimeException e) {
            throw new QueryException(DatawaveErrorCode.QUERY_TRANSFORM_ERROR);
        }
    }
    
    /**
     * Allows for the customization of handling query results, e.g. allows for aggregation of query results before returning to the client.
     * 
     * @param settings
     *            The query settings object
     * @return Return a TransformIterator for the QueryLogic implementation
     */
    TransformIterator getTransformIterator(Query settings);
    
    /**
     * Whether the query is a type that should be allowed to be run long (exceed the short circuit timeout)
     *
     * @return Return whether the query is a type that should be allowed to be run long (exceed the short circuit timeout)
     */
    boolean isLongRunningQuery();
    
    /**
     * release resources
     */
    void close();
    
    /** @return the tableName */
    String getTableName();
    
    /**
     * @return max number of results to pass back to the caller
     */
    long getMaxResults();
    
    /**
     * @return the results of getMaxWork
     */
    @Deprecated
    long getMaxRowsToScan();
    
    /**
     * @return max number of nexts + seeks performed by the underlying iterators in total
     */
    long getMaxWork();
    
    /**
     * @return max number of records to return in a page (max pagesize allowed)
     */
    int getMaxPageSize();
    
    /**
     * @return the number of bytes at which a page will be returned, even if pagesize has not been reached
     */
    long getPageByteTrigger();
    
    /**
     * Returns the base iterator priority.
     * 
     * @return base iterator priority
     */
    int getBaseIteratorPriority();
    
    /**
     * @param tableName
     *            the name of the table
     */
    void setTableName(String tableName);
    
    /**
     * @param maxResults
     *            max number of results to pass back to the caller
     */
    void setMaxResults(long maxResults);
    
    /**
     * @param maxRowsToScan
     *            This is now deprecated and setMaxWork should be used instead. This is equivalent to setMaxWork.
     */
    @Deprecated
    void setMaxRowsToScan(long maxRowsToScan);
    
    /**
     * @param maxWork
     *            max work which is normally calculated as the number of next + seek calls made by the underlying iterators
     */
    void setMaxWork(long maxWork);
    
    /**
     * @param maxPageSize
     *            max number of records in a page (max pagesize allowed)
     */
    void setMaxPageSize(int maxPageSize);
    
    /**
     * @param pageByteTrigger
     *            the number of bytes at which a page will be returned, even if pagesize has not been reached
     */
    void setPageByteTrigger(long pageByteTrigger);
    
    /**
     * Sets the base iterator priority
     * 
     * @param priority
     *            base iterator priority
     */
    void setBaseIteratorPriority(final int priority);
    
    /**
     * @param logicName
     *            name of the query logic
     */
    void setLogicName(String logicName);
    
    /**
     * @return name of the query logic
     */
    String getLogicName();
    
    /**
     * @param logicDescription
     *            a brief description of this logic type
     */
    void setLogicDescription(String logicDescription);
    
    /**
     * @return the audit level for this logic
     */
    AuditType getAuditType(Query query);
    
    /**
     * @return the audit level for this logic for a specific query
     */
    AuditType getAuditType();
    
    /**
     * @param auditType
     *            the audit level for this logic
     */
    void setAuditType(AuditType auditType);
    
    /**
     * @return a brief description of this logic type
     */
    String getLogicDescription();
    
    /**
     * @return should query metrics be collected for this query logic
     */
    boolean getCollectQueryMetrics();
    
    /**
     * @param collectQueryMetrics
     *            whether query metrics be collected for this query logic
     */
    void setCollectQueryMetrics(boolean collectQueryMetrics);
    
    void setRoleManager(RoleManager roleManager);
    
    RoleManager getRoleManager();
    
    /**
     * List of parameters that can be used in the 'params' parameter to Query/create
     * 
     * @return the supported parameters
     */
    Set<String> getOptionalQueryParameters();
    
    /**
     * @param connPoolName
     *            The name of the connection pool to set.
     */
    void setConnPoolName(String connPoolName);
    
    /** @return the connPoolName */
    String getConnPoolName();
    
    /**
     * Check that the user has one of the required roles principal my be null when there is no intent to control access to QueryLogic
     * 
     * @param principal
     * @return true/false
     */
    boolean canRunQuery(Principal principal);
    
    boolean canRunQuery(); // uses member Principal
    
    void setPrincipal(Principal principal);
    
    Principal getPrincipal();
    
    MarkingFunctions getMarkingFunctions();
    
    void setMarkingFunctions(MarkingFunctions markingFunctions);
    
    ResponseObjectFactory getResponseObjectFactory();
    
    void setResponseObjectFactory(ResponseObjectFactory responseObjectFactory);
    
    /**
     * List of parameters that must be passed from the client for this query logic to work
     * 
     * @return the required parameters
     */
    Set<String> getRequiredQueryParameters();
    
    /**
     * 
     * @return set of example queries
     */
    Set<String> getExampleQueries();
    
    /**
     * Return the DNs authorized access to this query logic.
     *
     * @return the set of DNs authorized access to this query logic, possibly null or empty
     */
    Set<String> getAuthorizedDNs();
    
    /**
     * Set the DNs authorized access to this query logic.
     *
     * @param allowedDNs
     *            the DNs authorized access
     */
    void setAuthorizedDNs(Set<String> allowedDNs);
    
    /**
     * Return whether or not the provided collection of DNs contains at least oneDN that is authorized for access to this query logic. This will return true in
     * the following cases:
     * <ul>
     * <li>The set of authorized DNs for this query logic is null or empty.</li>
     * <li>The set of authorized DNs is not empty, and the provided collection contains a DN that is also found within the set of authorized DNs.</li>
     * </ul>
     *
     * @param dns
     *            the DNs to determine access rights for
     * @return true if the collection contains at least one DN that has access to this query logic, or false otherwise
     */
    default boolean containsDNWithAccess(Collection<String> dns) {
        Set<String> authorizedDNs = getAuthorizedDNs();
        return authorizedDNs == null || authorizedDNs.isEmpty() || (dns != null && dns.stream().anyMatch(authorizedDNs::contains));
    }
    
    /**
     * Set the map of DNs to query result limits. This should override the default limit returned by {@link #getMaxResults()} for any included DNs.
     *
     * @param dnResultLimits
     *            the map of DNs to query result limits
     */
    void setDnResultLimits(Map<String,Long> dnResultLimits);
    
    /**
     * Return the map of DNs to result limits.
     *
     * @return the map of DNs to query result limits
     */
    Map<String,Long> getDnResultLimits();
    
    /**
     * Return the maximum number of results to include for the query for any DN present in the specified collection. If limits are found for multiple DNs in the
     * collection, the smallest value will be returned. If the provided collection is null or empty, or if no limits are found for any DN, the value of
     * {@link #getMaxResults()} will be returned.
     *
     * @param dns
     *            the DNs to determine the maximum number of results to include for the query. It's expected that this list represents all the DNs in the DN
     *            chain for an individual user.
     * @return the maximum number of results to include
     */
    default long getResultLimit(Collection<String> dns) {
        Map<String,Long> dnResultLimits = getDnResultLimits();
        if (dnResultLimits == null || dns == null) {
            return getMaxResults();
        }
        return dns.stream().filter(dnResultLimits::containsKey).map(dnResultLimits::get).min(Long::compareTo).orElseGet(this::getMaxResults);
    }
    
    /**
     * inform the logic of when a new page of results processing starts. Logics may ignore this information, or pass it to interested transformers, but the
     * logics shouldn't store this property.
     *
     * @param pageProcessingStartTime
     */
    void setPageProcessingStartTime(long pageProcessingStartTime);
}
