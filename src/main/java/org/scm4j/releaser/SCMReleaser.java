package org.scm4j.releaser;

import org.scm4j.commons.progress.IProgress;
import org.scm4j.commons.progress.ProgressConsole;
import org.scm4j.releaser.actions.ActionSet;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.actions.PrintAction;
import org.scm4j.releaser.branch.DevelopBranch;
import org.scm4j.releaser.branch.ReleaseBranch;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.Options;
import org.scm4j.releaser.conf.TagDesc;
import org.scm4j.releaser.scmactions.SCMActionRelease;
import org.scm4j.releaser.scmactions.SCMActionTag;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SCMReleaser {

	public static final String MDEPS_FILE_NAME = "mdeps";
	public static final String VER_FILE_NAME = "version";
	public static final String DELAYED_TAGS_FILE_NAME = "delayed-tags.yml";
	public static final File BASE_WORKING_DIR = new File(System.getProperty("user.home"), ".scm4j");

	public IAction getActionTree(Component comp) {
		CachedStatuses cache = new CachedStatuses();
		ExtendedStatusTreeBuilder statusBuilder = new ExtendedStatusTreeBuilder();
		ExtendedStatusTreeNode node = statusBuilder.getExtendedStatusTreeNode(comp, cache);
		
		ActionTreeBuilder actionBuilder = new ActionTreeBuilder();
		return actionBuilder.getActionTree(node, cache, ActionSet.FULL);
	}
	
	public IAction getTagActionTree(Component comp) {
		List<IAction> childActions = new ArrayList<>();
		DevelopBranch db = new DevelopBranch(comp);
		List<Component> mDeps = db.getMDeps();

		for (Component mDep : mDeps) {
			childActions.add(getTagActionTree(mDep));
		}
		return new SCMActionTag(new ReleaseBranch(comp), comp, childActions);
	}

	public IAction getActionTree(String coords) {
		return getActionTree(new Component(coords));
	}
}
