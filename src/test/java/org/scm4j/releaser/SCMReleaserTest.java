package org.scm4j.releaser;

import org.junit.Test;
import org.scm4j.releaser.actions.ActionKind;
import org.scm4j.releaser.actions.ActionNone;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.branch.ReleaseBranch;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

public class SCMReleaserTest extends WorkflowTestBase {
	
	private final SCMReleaser releaser = new SCMReleaser();

	@Test
	public void testUnsupportedBuildStatus() {
		SCMReleaser releaser = spy(new SCMReleaser());
		doReturn(BuildStatus.ERROR).when(releaser).getBuildStatus(any(ReleaseBranch.class));

		try {
			releaser.getActionTree(TestEnvironment.PRODUCT_UNTILL, ActionKind.AUTO);
			fail();
		} catch (IllegalArgumentException e) {

		}
	}
	
	@Test
	public void testGetActionTreeUsingActionKind() {
		IAction action = releaser.getActionTree(UNTILLDB, ActionKind.AUTO);
		assertIsGoingToFork(action, compUnTillDb);
		
		action = releaser.getActionTree(UNTILLDB, ActionKind.BUILD);
		assertTrue(action instanceof ActionNone);
		
		action = releaser.getActionTree(UNTILLDB, ActionKind.FORK);
		assertIsGoingToFork(action, compUnTillDb);
		action.execute(getProgress(action));
		
		action = releaser.getActionTree(UNTILLDB, ActionKind.AUTO);
		assertIsGoingToBuild(action, compUnTillDb);
		
		action = releaser.getActionTree(UNTILLDB, ActionKind.BUILD);
		assertIsGoingToBuild(action, compUnTillDb);
		
		action = releaser.getActionTree(UNTILLDB, ActionKind.FORK);
		assertTrue(action instanceof ActionNone);
	}
}