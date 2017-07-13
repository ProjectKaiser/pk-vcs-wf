package org.scm4j.wf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.commons.progress.ProgressConsole;
import org.scm4j.vcs.api.VCSTag;
import org.scm4j.wf.actions.IAction;
import org.scm4j.wf.actions.results.ActionResultTag;
import org.scm4j.wf.actions.results.ActionResultVersion;
import org.scm4j.wf.conf.Version;

@PrepareForTest(SCMWorkflow.class)
@RunWith(PowerMockRunner.class)
public class SCMWorkflowTest {

	public static final String PRODUCT_UNTILL = "eu.untill:unTill";
	public static final String PRODUCT_UBL = "eu.untill:UBL";
	public static final String PRODUCT_UNTILLDB = "eu.untill:unTillDb";
	
	private TestEnvironment env;

	@Before
	public void setUp() throws Exception {
		env = new TestEnvironment();
		env.generateTestEnvironment();
		PowerMockito.mockStatic(System.class);
		PowerMockito.when(System.getenv(SCMWorkflow.CREDENTIALS_LOCATION_ENV_VAR))
				.thenReturn("file://localhost/" + env.getCredsFile().getPath().replace("\\", "/"));
		PowerMockito.when(System.getenv(SCMWorkflow.REPOS_LOCATION_ENV_VAR))
				.thenReturn("file://localhost/" + env.getReposFile().getPath().replace("\\", "/"));
		PowerMockito.when(System.getProperty(Matchers.anyString()))
				.thenCallRealMethod();
	}

	@After
	public void tearDown() throws Exception {
		if (env != null) {
			env.clean();
		}
	}
	
	@Test
	public void testUseLastVersions() throws Exception {
		env.generateCommitWithVERTag(env.getUnTillVCS());
		env.generateCommitWithVERTag(env.getUnTillDbVCS());
		env.generateCommitWithVERTag(env.getUblVCS());
		
		SCMWorkflow wf = new SCMWorkflow(PRODUCT_UNTILL);
		
		IAction actionUnTill = wf.getProductionReleaseAction(null);
		checkUseLastReleaseAction(actionUnTill, null, PRODUCT_UNTILL, env.getUnTillVer());
		assertTrue(actionUnTill.getChildActions().size() == 2);
		
		IAction actionUBL = actionUnTill.getChildActions().get(0);
		checkUseLastReleaseAction(actionUBL, actionUnTill, PRODUCT_UBL, env.getUblVer());
		assertTrue(actionUBL.getChildActions().size() == 1);
		
		IAction actionUnTillDb = actionUnTill.getChildActions().get(1);
		checkUseLastReleaseAction(actionUnTillDb, actionUnTill, PRODUCT_UNTILLDB, env.getUnTillDbVer());
		
		IAction actionUBLUnTillDb = actionUBL.getChildActions().get(0);
		checkUseLastReleaseAction(actionUBLUnTillDb, actionUBL, PRODUCT_UNTILLDB, env.getUnTillDbVer());
	}
	
	@Test
	public void testProductionReleaseNewFeatures() {
		env.generateFeatureCommit(env.getUblVCS(), "feature commit");
		env.generateFeatureCommit(env.getUnTillVCS(), "feature commit");
		env.generateFeatureCommit(env.getUnTillDbVCS(), "feature commit");
		
		SCMWorkflow wf = new SCMWorkflow(PRODUCT_UNTILL);
		
		IAction actionUnTill = wf.getProductionReleaseAction(null);
		checkProductionReleaseAction(actionUnTill, null, ProductionReleaseReason.NEW_FEATURES, PRODUCT_UNTILL);
		assertTrue(actionUnTill.getChildActions().size() == 2);
		
		IAction actionUBL = actionUnTill.getChildActions().get(0);
		checkProductionReleaseAction(actionUBL, actionUnTill, ProductionReleaseReason.NEW_FEATURES, PRODUCT_UBL);
		assertTrue(actionUBL.getChildActions().size() == 1);
		
		IAction actionUnTillDb = actionUnTill.getChildActions().get(1);
		checkProductionReleaseAction(actionUnTillDb, actionUnTill, ProductionReleaseReason.NEW_FEATURES, PRODUCT_UNTILLDB);
		
		IAction actionUBLUnTillDb = actionUBL.getChildActions().get(0);
		checkProductionReleaseAction(actionUBLUnTillDb, actionUBL, ProductionReleaseReason.NEW_FEATURES, PRODUCT_UNTILLDB);
	}
	
	@Test
	public void testProductionReleaseHasNewFeaturedDependencies() {
		env.generateCommitWithVERTag(env.getUnTillVCS());
		env.generateCommitWithVERTag(env.getUblVCS());
		env.generateFeatureCommit(env.getUnTillDbVCS(), "feature commit");

		SCMWorkflow wf = new SCMWorkflow(PRODUCT_UNTILL);

		IAction actionUnTill = wf.getProductionReleaseAction(null);
		checkProductionReleaseAction(actionUnTill, null, ProductionReleaseReason.NEW_DEPENDENCIES, PRODUCT_UNTILL);
		assertTrue(actionUnTill.getChildActions().size() == 2);

		IAction actionUBL = actionUnTill.getChildActions().get(0);
		checkProductionReleaseAction(actionUBL, actionUnTill, ProductionReleaseReason.NEW_DEPENDENCIES, PRODUCT_UBL);
		assertTrue(actionUBL.getChildActions().size() == 1);

		IAction actionUnTillDb = actionUnTill.getChildActions().get(1);
		checkProductionReleaseAction(actionUnTillDb, actionUnTill, ProductionReleaseReason.NEW_FEATURES, PRODUCT_UNTILLDB);

		IAction actionUBLUnTillDb = actionUBL.getChildActions().get(0);
		checkProductionReleaseAction(actionUBLUnTillDb, actionUBL, ProductionReleaseReason.NEW_FEATURES, PRODUCT_UNTILLDB);
	}

	@Test
	public void testProductionReleaseHasNewerDependencyVersions() throws Exception {
		env.generateCommitWithVERTag(env.getUnTillVCS());
		env.generateCommitWithVERTag(env.getUnTillDbVCS());
		env.generateCommitWithVERTag(env.getUblVCS());
		env.getUnTillVCS().setFileContent(null, SCMWorkflow.MDEPS_FILE_NAME,
				SCMWorkflowTest.PRODUCT_UBL + ":" + env.getUblVer().toString() + "\r\n" +
				SCMWorkflowTest.PRODUCT_UNTILLDB + ":1.0.0" + "\r\n",
				SCMActionProductionRelease.VCS_TAG_SCM_IGNORE + " old unTillDb version is used in mdeps file");

		SCMWorkflow wf = new SCMWorkflow(PRODUCT_UNTILL);

		IAction actionUnTill = wf.getProductionReleaseAction(null);
		checkProductionReleaseAction(actionUnTill, null, ProductionReleaseReason.NEW_DEPENDENCIES, PRODUCT_UNTILL);
		assertTrue(actionUnTill.getChildActions().size() == 2);

		IAction actionUBL = actionUnTill.getChildActions().get(0);
		checkUseLastReleaseAction(actionUBL, actionUnTill, PRODUCT_UBL, env.getUblVer());
		assertTrue(actionUBL.getChildActions().size() == 1);

		IAction actionUnTillDb = actionUnTill.getChildActions().get(1);
		checkUseLastReleaseAction(actionUnTillDb, actionUnTill, PRODUCT_UNTILLDB, env.getUnTillDbVer());

		IAction actionUBLUnTillDb = actionUBL.getChildActions().get(0);
		checkUseLastReleaseAction(actionUBLUnTillDb, actionUBL, PRODUCT_UNTILLDB, env.getUnTillDbVer());

	}
	
	private void checkAction(IAction action, IAction parentAction, String expectedName) {
		assertNotNull(action);
		assertEquals(action.getParent(), parentAction);
		assertEquals(action.getName(), expectedName);
		assertNotNull(action.getExecutionResults());
		assertTrue(action.getExecutionResults().isEmpty()); // because is not executed yet
	}
	
	private void checkUseLastReleaseAction(IAction action, IAction parentAction, String expectedName, Version expectedVersion) throws Exception {
		checkAction(action, parentAction, expectedName);
		assertTrue(action instanceof SCMActionUseLastReleaseVersion);
		SCMActionUseLastReleaseVersion lv = (SCMActionUseLastReleaseVersion) action;
		assertEquals(lv.getVer(), expectedVersion);
		checkActionResultVersion(action, expectedName, expectedVersion, false);
	}
	
	private void checkActionResultVersion(IAction action, String expectedName, Version expectedVersion, Boolean isNewBuildExpected) throws Exception {
		Object res;
		try (IProgress progress = new NullProgress()) {
			res = action.execute(progress);
		}
		assertTrue(res instanceof ActionResultVersion);
		ActionResultVersion arv = (ActionResultVersion) res;
		assertEquals(arv.getIsNewBuild(), isNewBuildExpected);
		assertEquals(arv.getName(), expectedName);
		if (isNewBuildExpected) {
			assertNotNull(arv.getNewBranchName());
		} else {
			assertNull(arv.getNewBranchName());
		}
		assertEquals(arv.getVersion(), isNewBuildExpected ? expectedVersion.toReleaseString() : expectedVersion.toPreviousMinorRelease());
	}

	private void checkProductionReleaseAction(IAction action, IAction parentAction, ProductionReleaseReason expectedReason, 
			String expectedName) {
		checkAction(action, parentAction, expectedName);
		
		assertTrue(action instanceof SCMActionProductionRelease);
		SCMActionProductionRelease pr = (SCMActionProductionRelease) action;
		assertEquals(pr.getReason(), expectedReason);
	}
	
	@Test
	@Ignore
	public void testTagRelease() throws Exception {
		env.generateFeatureCommit(env.getUblVCS(), "feature commit");
		env.generateFeatureCommit(env.getUnTillVCS(), "feature commit");
		env.generateFeatureCommit(env.getUnTillDbVCS(), "feature commit");
		
		SCMWorkflow wf = new SCMWorkflow(PRODUCT_UNTILL);
		
		IAction actionReleaseUnTill = wf.getProductionReleaseAction(null);
		ActionResultVersion resultVersion;
		try (IProgress progress = new ProgressConsole("releasing " + actionReleaseUnTill.getName(), ">>> ", "<<< ")) {
			Object result = actionReleaseUnTill.execute(progress);
			assertFalse(result instanceof Throwable);
			resultVersion = (ActionResultVersion) result; 
		}
		
		IAction actionTagUnTill = wf.getTagReleaseAction(null);
		ActionResultTag resultTag;
		try (IProgress progress = new ProgressConsole("tagging " + actionReleaseUnTill.getName(), ">>> ", "<<< ")) {
			Object result = actionTagUnTill.execute(progress);
			assertFalse(result instanceof Throwable);
			resultTag = (ActionResultTag) result;
		}
		
		List<VCSTag> tags = env.getUnTillVCS().getTags();
		assertNotNull(tags);
		assertTrue(tags.size() == 1);
		VCSTag tag = tags.get(0);
		assertEquals(tag.getTagName(), resultTag.getTag().getTagName());
		assertEquals(tag.getTagMessage(), "tagMessage");
		assertEquals(tag.getRelatedCommit(), env.getUnTillVCS().getHeadCommit(resultVersion.getNewBranchName()));
	}

	@Test
	public void testProductionReleaseExecute() throws Exception {
		env.generateFeatureCommit(env.getUblVCS(), "feature commit");
		env.generateFeatureCommit(env.getUnTillVCS(), "feature commit");
		env.generateFeatureCommit(env.getUnTillDbVCS(), "feature commit");
		
		SCMWorkflow wf = new SCMWorkflow(PRODUCT_UNTILL);
		IAction actionUnTill = wf.getProductionReleaseAction(null);
		
		checkActionResultVersion(actionUnTill, PRODUCT_UNTILL, env.getUnTillVer(), true);
	}
}