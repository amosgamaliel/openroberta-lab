package de.fhg.iais.roberta.persistence.util;

import java.io.Serializable;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.iais.roberta.factory.IRobotFactory;
import de.fhg.iais.roberta.util.Key;
import de.fhg.iais.roberta.util.RandomUrlPostfix;
import de.fhg.iais.roberta.util.ServerProperties;
import de.fhg.iais.roberta.util.dbc.Assert;
import de.fhg.iais.roberta.util.dbc.DbcKeyException;

/**
 * Objects of this class store the server-related state of the user's work with the openroberta-lab.
 */
public class HttpSessionState implements Serializable {
    private static final long serialVersionUID = 5423413372044585392L;
    private static final Logger LOG = LoggerFactory.getLogger(HttpSessionState.class);

    public final static int NO_USER = -1;

    private Map<String, IRobotFactory> robotPluginMap;
    private String defaultRobotName;
    private long sessionNumber;

    /**
     * the REST-service in ClientInit will set this token. The token is returned with each response and the front end must supply it with each request. The
     * token is needed to detect two problematic cases in the client-server communication:
     * <ul>
     * <li>if it is already SET when ClientInit is called, it is likely, that the user opened a second tab and accessed the lab (again). This is a problem, as
     * we cannot sync the two clients. <i>Thus we will invalidate the old, previous state.</i>
     * <li>if it is NOT SET and another function than init is called, it is likely, that either the server was restarted or a user session moved from one server
     * to another server. In the latter case the session must be migrated to the new server. <i>This is not yet implemented. Actually we abort the request.</i>
     * </ul>
     * The token value of "" is used for debugging purposes and will deactivate all consistency checks (see {@linkplain initOnlyLegalForDebugging})
     */
    private String tokenSetOnInit;
    private int userId;
    private String robotName;
    private String token;
    private String programName;
    private String program;
    private String configurationName;
    private String configuration;
    private String toolboxName;
    private String toolbox;
    private boolean processing;

    private String countryCode;

    public HttpSessionState(Map<String, IRobotFactory> robotPluginMap, ServerProperties serverProperties, long sessionNumber) //
    {
        this.robotPluginMap = robotPluginMap;
        this.defaultRobotName = serverProperties.getDefaultRobot();
        this.sessionNumber = sessionNumber;
        reset();
    }

    public void reset() {
        this.tokenSetOnInit = null;
        this.userId = HttpSessionState.NO_USER;
        this.robotName = defaultRobotName;
        this.token = RandomUrlPostfix.generate(12, 12, 3, 3, 3);
        this.programName = null;
        this.program = null;
        this.configurationName = null;
        this.configuration = null;
        this.toolboxName = null;
        this.toolbox = null;
        this.setProcessing(false);
    }

    /**
     * factory method to create and initialize a {@linkplain HttpSessionState} object. This object stores the server-related state of the user's work with the
     * openroberta-lab.
     *
     * @param robotPluginMap
     * @param serverProperties
     * @param sessionNumber
     * @return
     */
    public static HttpSessionState init(Map<String, IRobotFactory> robotPluginMap, ServerProperties serverProperties, long sessionNumber) //
    {
        return new HttpSessionState(robotPluginMap, serverProperties, sessionNumber);
    }

    /**
     * factory method to create and initialize a {@linkplain HttpSessionState} object for debugging. Calling this method will disable consistency checks as
     * multiple tabs detection, bypassing the /init REST-call.<br>
     * <b>It must not be used outside of tests</b>
     *
     * @param robotPluginMap
     * @param serverProperties
     * @param sessionNumber
     * @return
     */
    public static HttpSessionState initOnlyLegalForDebugging(Map<String, IRobotFactory> robotPluginMap, ServerProperties serverProperties, long sessionNumber) //
    {
        HttpSessionState state = new HttpSessionState(robotPluginMap, serverProperties, sessionNumber);
        state.tokenSetOnInit = "";
        return state;
    }

    public int getUserId() {
        return this.userId;
    }

    public boolean isUserLoggedIn() {
        return this.userId >= 1;
    }

    public void setUserClearDataKeepTokenAndRobotId(int userId) {
        Assert.isTrue(userId >= 1 || userId == HttpSessionState.NO_USER);
        // token is not cleared & robotId is not cleared: this would annoy the user.
        this.userId = userId;
        this.programName = null;
        this.program = null;
        this.configurationName = null;
        this.configuration = null;
    }

    public String getRobotName() {
        return this.robotName;
    }

    public void setRobotName(String robotName) {
        Assert.notNull(robotName);
        this.robotName = robotName;
    }

    public boolean isInitTokenInitialized() {
        return tokenSetOnInit != null;
    }

    public String getInitToken() {
        Assert.notNull(tokenSetOnInit, "init token not initialized. This is a SEVERE error");
        return tokenSetOnInit;
    }

    public void setInitToken() {
        Assert.isFalse("".equals(tokenSetOnInit));
        this.tokenSetOnInit = RandomUrlPostfix.generate(12, 12, 3, 3, 3);
    }

    /**
     * validate the init-token from the frontend-request and the init-token from the state stored in this object.<br>
     * If an error is detected a {@linkplain DbcKeyException} is thrown.<br>
     * <i>Only for debugging:</i> if the init-token in this object is set to "", all checks are disabled. This should <i>NEVER</i> happen, when a real server is
     * started
     *
     * @param initToken the init token from the frontend-request
     */
    public void validateInitToken(String initToken) {
        if ( "".equals(tokenSetOnInit) ) {
            return; // DEBUG session
        }
        String errorMsgIfError;
        Key errorKey;
        if ( initToken == null ) {
            errorMsgIfError = "frontend request has no initToken";
            errorKey = Key.INIT_FAIL_HTTPSESSION_EXPECTED_BUT_NOT_FOUND;
        } else if ( !isInitTokenInitialized() ) {
            errorMsgIfError = "initToken is not initialized in the session";
            errorKey = Key.INIT_FAIL_MULTIPLE_FRONTENDS_ONE_HTTPSESSION;
        } else if ( !getInitToken().equals(initToken) ) {
            errorMsgIfError = "initToken from frontend and from session are different";
            errorKey = Key.INIT_FAIL_MULTIPLE_FRONTENDS_ONE_HTTPSESSION;
        } else {
            errorMsgIfError = null;
            errorKey = null;
        }
        if ( errorMsgIfError != null || errorKey != null ) {
            LOG.error(errorMsgIfError);
            throw new DbcKeyException(errorMsgIfError, errorKey, null);
        }

    }

    public String getToken() {
        return this.token;
    }

    public void setToken(String token) {
        Assert.notNull(token);
        this.token = token;
    }

    public String getProgramName() {
        return this.programName;
    }

    public String getProgram() {
        return this.program;
    }

    public void setProgramNameAndProgramText(String programName, String program) {
        this.programName = programName;
        this.program = program;
    }

    public String getConfigurationName() {
        return this.configurationName;
    }

    public String getConfiguration() {
        return this.configuration;
    }

    public void setConfigurationNameAndConfiguration(String configurationName, String configuration) {
        this.configurationName = configurationName;
        this.configuration = configuration;
    }

    public String getToolboxName() {
        return this.toolboxName;
    }

    public String getToolbox() {
        return this.toolbox;
    }

    public void setToolboxNameAndToolbox(String toolboxName, String toolbox) {
        this.toolboxName = toolboxName;
        this.toolbox = toolbox;
    }

    public IRobotFactory getRobotFactory() {
        return this.robotPluginMap.get(this.robotName);
    }

    public IRobotFactory getRobotFactory(String robotName) {
        return this.robotPluginMap.get(robotName);
    }

    public long getSessionNumber() {
        return this.sessionNumber;
    }

    public boolean isProcessing() {
        return processing;
    }

    public void setProcessing(boolean processing) {
        this.processing = processing;
    }

    public String getCountryCode() {
        return this.countryCode;
    }

    public void setCountryCode(String code) {
        this.countryCode = code;
    }
}