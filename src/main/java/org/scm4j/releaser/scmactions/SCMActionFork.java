package org.scm4j.releaser.scmactions;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.scm4j.commons.Version;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.releaser.Build;
import org.scm4j.releaser.BuildStatus;
import org.scm4j.releaser.LogTag;
import org.scm4j.releaser.SCMReleaser;
import org.scm4j.releaser.actions.ActionAbstract;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.branch.DevelopBranch;
import org.scm4j.releaser.branch.ReleaseBranch;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.MDepsFile;
import org.scm4j.releaser.exceptions.EReleaserException;
import org.scm4j.vcs.api.IVCS;

public class SCMActionFork extends ActionAbstract {
	
	private final DevelopBranch db;
	private final ReleaseBranch rb;
	private final BuildStatus mbs;
	private final IVCS vcs;

	public SCMActionFork(ReleaseBranch rb, List<IAction> childActions, BuildStatus mbs) {
		super(rb.getComponent(), childActions);
		db = new DevelopBranch(comp);
		this.mbs = mbs;
		vcs = getVCS();
		this.rb = new ReleaseBranch(comp, rb.getVersion().toNextMinor().toReleaseZeroPatch());
	}
	
	@Override
	public void execute(IProgress progress)  {
		if (isUrlProcessed(comp.getVcsRepository().getUrl())) {
			progress.reportStatus("already executed");
			return;
		}
		try {
			super.executeChilds(progress);
			switch(mbs) {
			case FORK:
				createBranch(progress);
				truncateSnapshotReleaseVersion(progress);
				raiseTrunkMinorVersion(progress);
			case FREEZE:
				freezeMDeps(progress);
				break;
			default:
				throw new IllegalStateException(mbs + " target action is occured when fork only is expected");
			}
			addProcessedUrl(comp.getVcsRepository().getUrl());
		} catch (EReleaserException e) {
			throw e;
		} catch (Exception e) {
			progress.error("execution error: " + e.toString());
			throw new EReleaserException(e);
		}
	}
	
	private void createBranch(IProgress progress) {
		String newBranchName = rb.getName();
		vcs.createBranch(db.getName(), newBranchName, "release branch created");
		progress.reportStatus("branch " + newBranchName + " created");
	}
	
	private void freezeMDeps(IProgress progress) {
		MDepsFile currentMDepsFile = rb.getMDepsFile();
		if (!currentMDepsFile.hasMDeps()) {
			progress.reportStatus("no mdeps to freeze");
			return;
		}
		StringBuilder sb = new StringBuilder();
		ReleaseBranch rbMDep;
		Version newVersion;
		boolean hasChanges = false;
		for (Component currentMDep : currentMDepsFile.getMDeps()) {
			rbMDep = new ReleaseBranch(currentMDep);
			// untilldb is built -> rbMDep.getVersion is 2.59.1, but we need 2.59.0
			newVersion = rbMDep.getVersion();
			if (!newVersion.getPatch().equals(Build.ZERO_PATCH)) {
				newVersion = newVersion.toPreviousPatch();
			}
			sb.append("" + currentMDep.getName() + ": " + currentMDep.getVersion() + " -> " + newVersion + "\r\n");
			currentMDepsFile.replaceMDep(currentMDep.clone(newVersion));
			hasChanges = true;
		}
		if (hasChanges) {
			vcs.setFileContent(rb.getName(), SCMReleaser.MDEPS_FILE_NAME, currentMDepsFile.toFileContent(), LogTag.SCM_MDEPS);
			progress.reportStatus("mdeps frozen" + (sb.length() == 0 ? "" : ":\r\n" + StringUtils.removeEnd(sb.toString(), "\r\n")));
		}
	}

	private void truncateSnapshotReleaseVersion(IProgress progress) {
		String noSnapshotVersion = rb.getVersion().toString();
		String newBranchName = rb.getName();
		getVCS().setFileContent(newBranchName, SCMReleaser.VER_FILE_NAME, noSnapshotVersion, LogTag.SCM_VER + " " + noSnapshotVersion);
		progress.reportStatus("truncated snapshot: " + noSnapshotVersion + " in branch " + newBranchName);
	}

	private void raiseTrunkMinorVersion(IProgress progress) {
		Version newMinorVersion = db.getVersion().toNextMinor();
		getVCS().setFileContent(db.getName(), SCMReleaser.VER_FILE_NAME, newMinorVersion.toString(), LogTag.SCM_VER + " " + newMinorVersion);
		progress.reportStatus("change to version " + newMinorVersion + " in trunk");
	}

	@Override
	public String toString() {
		return mbs.toString() + " " + comp.getCoords() + ", " + rb.getVersion();
	}
	
	public ReleaseBranch getReleaseBranch() {
		return rb;
	}
}
