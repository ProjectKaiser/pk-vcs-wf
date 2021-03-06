package org.scm4j.releaser.conf;

import org.scm4j.vcs.GitVCS;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.workingcopy.IVCSWorkspace;
import org.scm4j.vcs.svn.SVNVCS;

public final class VCSFactory {

	public static IVCS getVCS(VCSType type, Credentials creds, String url, IVCSWorkspace ws) {
		IVCS vcs = null;
		switch (type) {
		case GIT: {
			vcs = new GitVCS(ws.getVCSRepositoryWorkspace(url));
			if (creds.getName() != null) {
					vcs.setCredentials(
							creds.getName(),
							creds.getPassword());
			}
			break;
		}
		case SVN: {
			vcs = new SVNVCS(ws.getVCSRepositoryWorkspace(url),
					creds == null ? null : creds.getName(),
					creds == null ? null : creds.getPassword());
			break;
		}
		}
		return vcs;
	}
}
