package org.scm4j.wf;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.VCSCommit;
import org.scm4j.vcs.api.workingcopy.IVCSWorkspace;
import org.scm4j.wf.actions.ActionError;
import org.scm4j.wf.actions.ActionNone;
import org.scm4j.wf.actions.IAction;
import org.scm4j.wf.conf.*;
import org.scm4j.wf.exceptions.EDepConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class SCMWorkflowGetActionTest {

	private static final String TEST_DEP = "test:dep";
	private static final String TEST_MASTER_BRANCH = "test master branch";
	private static final VCSCommit COMMIT_FEATURE = new VCSCommit("test revision", "feature commit", null);
	private static final VCSCommit COMMIT_VER = new VCSCommit("test revision", LogTag.SCM_VER, null);
	private static final VCSCommit COMMIT_IGNORE = new VCSCommit("test revision", LogTag.SCM_IGNORE, null);
	
	@Mock
	IVCS mockedVcs;
	
	@Mock
	VCSRepositories mockedRepos;

	@Mock
	IVCSWorkspace ws;
	
	private VCSRepository testRepo;
	private SCMWorkflow wf;
	
	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		testRepo = new VCSRepository(TEST_DEP, null, new Credentials(null, null, false), VCSType.GIT,
				TEST_MASTER_BRANCH, ws, mockedVcs);
		Mockito.doReturn(testRepo).when(mockedRepos).getByComponent(testRepo.getName());
		wf = new SCMWorkflow(new Component(TEST_DEP, testRepo), mockedRepos, ws);
		Mockito.doReturn(true).when(mockedVcs).fileExists(testRepo.getDevBranch(), SCMWorkflow.VER_FILE_NAME);
	}
	
	@Test
	public void testProductionReleaseNewFeatures() {
		Mockito.doReturn(COMMIT_FEATURE).when(mockedVcs).getHeadCommit(TEST_MASTER_BRANCH);
		IAction action = wf.getProductionReleaseActionRoot(new ArrayList<IAction>());
		assertTrue(action instanceof SCMActionProductionRelease);
		SCMActionProductionRelease r = (SCMActionProductionRelease) action;
		assertEquals(r.getReason(), ProductionReleaseReason.NEW_FEATURES);
	}
	
	@Test 
	public void testActionNoneIfNoVersionFile() {
		Mockito.doReturn(false).when(mockedVcs).fileExists(testRepo.getDevBranch(), SCMWorkflow.VER_FILE_NAME);
		Mockito.doReturn(COMMIT_FEATURE).when(mockedVcs).getHeadCommit(TEST_MASTER_BRANCH);
		try {
			wf.getProductionReleaseActionRoot(new ArrayList<IAction>());
			fail();
		} catch (EDepConfig e) {
			
		}
	}
	
	@Test
	public void testActionNoneIfHasErrors() {
		List<Component> testMDeps = Collections.singletonList(new Component(TEST_DEP + ":1.0.0", mockedRepos));
		wf.setMDeps(testMDeps);
		List<IAction> childActions = new ArrayList<>();
		childActions.add(new ActionError(new Component(TEST_DEP + ":1.0.0", mockedRepos), Collections.<IAction>emptyList(), TEST_MASTER_BRANCH, "test error cause", ws));
		
		IAction res = wf.getProductionReleaseActionRoot(childActions);
		assertTrue(res instanceof ActionNone);
	}
	
	@Test
	public void testProductionReleaseNewDependencies() {
		Mockito.doReturn("0.0.0").when(mockedVcs).getFileContent(TEST_MASTER_BRANCH, SCMWorkflow.VER_FILE_NAME);
		Mockito.doReturn(COMMIT_VER).when(mockedVcs).getHeadCommit(TEST_MASTER_BRANCH);
		List<Component> testMDeps = Collections.singletonList(new Component(TEST_DEP + ":1.0.0", mockedRepos));
		wf.setMDeps(testMDeps);
		List<IAction> childActions = new ArrayList<>();
		childActions.add(new SCMActionUseLastReleaseVersion(new Component(TEST_DEP + ":1.0.0", mockedRepos), Collections.<IAction>emptyList(), TEST_MASTER_BRANCH, ws));
		IAction action = wf.getProductionReleaseActionRoot(childActions);
		assertTrue(action instanceof SCMActionProductionRelease);
		SCMActionProductionRelease pr = (SCMActionProductionRelease) action;
		assertEquals(pr.getReason(), ProductionReleaseReason.NEW_DEPENDENCIES);
	}

	@Test
	public void testUseLastReleaseIfNoFeatures() {
		Mockito.doReturn(COMMIT_VER).when(mockedVcs).getHeadCommit(TEST_MASTER_BRANCH);
		Version ver = new Version("1.0.0");
		Mockito.doReturn("1.0.0").when(mockedVcs).getFileContent(TEST_MASTER_BRANCH, SCMWorkflow.VER_FILE_NAME);
		IAction action = wf.getProductionReleaseActionRoot(new ArrayList<IAction>());
		assertTrue(action instanceof SCMActionUseLastReleaseVersion);
		SCMActionUseLastReleaseVersion lastRelease = (SCMActionUseLastReleaseVersion) action;
		assertTrue(lastRelease.getVersion().equals(ver));
	}

	@Test
	public void testUseLastReleaseIfIgnore() {
		Mockito.doReturn(COMMIT_IGNORE).when(mockedVcs).getHeadCommit(TEST_MASTER_BRANCH);
		Version ver = new Version("1.0.0");
		Mockito.doReturn("1.0.0").when(mockedVcs).getFileContent(TEST_MASTER_BRANCH, SCMWorkflow.VER_FILE_NAME);
		IAction action = wf.getProductionReleaseActionRoot(new ArrayList<IAction>());
		assertTrue(action instanceof SCMActionUseLastReleaseVersion);
		SCMActionUseLastReleaseVersion lastRelease = (SCMActionUseLastReleaseVersion) action;
		assertTrue(lastRelease.getVersion().equals(ver));
	}
}
