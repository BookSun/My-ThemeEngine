#include <sys/cdefs.h>
#include <sys/param.h>
#include <sys/stat.h>

#include <assert.h>
#include <err.h>
#include <errno.h>
#include <fts.h>
#include <locale.h>
#include <signal.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>

#include "extern.h"
#include <android/log.h>

#define LOG_TAG "Shell_cp"
#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

#define	STRIP_TRAILING_SLASH(p) {					\
        while ((p).p_end > (p).p_path + 1 && (p).p_end[-1] == '/')	\
                *--(p).p_end = '\0';					\
}

static char empty[] = "";
PATH_T to = { .p_end = to.p_path, .target_end = empty  };

uid_t myuid;
int Hflag, Lflag, Rflag, Pflag, fflag, iflag, lflag, pflag, rflag, vflag, Nflag;
mode_t myumask;
sig_atomic_t pinfo;

enum op { FILE_TO_FILE, FILE_TO_DIR, DIR_TO_DNE };

static int copy(char *[], enum op, int);

static void
progress(int sig __unused)
{

	pinfo++;
}

int
cp_main(int argc, char *argv[])
{
	struct stat to_stat, tmp_stat;
	enum op type;
	int ch, fts_options, r, have_trailing_slash;
	char *target, **src;

#ifndef ANDROID
	setprogname(argv[0]);
#endif
	(void)setlocale(LC_ALL, "");

	Rflag = 1;

	fts_options = FTS_NOCHDIR | FTS_PHYSICAL;
	if (rflag) {
		if (Rflag) {
			LOGE("the -R and -r options may not be specified together.");
			return -1;
		}
		if (Hflag || Lflag || Pflag) {
			LOGE("the -H, -L, and -P options may not be specified with the -r option.");
			return -1;
		}
		fts_options &= ~FTS_PHYSICAL;
		fts_options |= FTS_LOGICAL;
	}

	if (Rflag) {
		if (Hflag)
			fts_options |= FTS_COMFOLLOW;
		if (Lflag) {
			fts_options &= ~FTS_PHYSICAL;
			fts_options |= FTS_LOGICAL;
		}
	} else if (!Pflag) {
		fts_options &= ~FTS_PHYSICAL;
		fts_options |= FTS_LOGICAL | FTS_COMFOLLOW;
	}

	myuid = getuid();

	/* Copy the umask for explicit mode setting. */
	myumask = umask(0);
	(void)umask(myumask);

	/* Save the target base in "to". */
	target = argv[--argc];
	if (strlcpy(to.p_path, target, sizeof(to.p_path)) >= sizeof(to.p_path))
		LOGE("%s: name too long", target);
	to.p_end = to.p_path + strlen(to.p_path);
	have_trailing_slash = (to.p_end[-1] == '/');
	if (have_trailing_slash)
		STRIP_TRAILING_SLASH(to);
	to.target_end = to.p_end;

	/* Set end of argument list for fts(3). */
	argv[argc] = NULL;

#ifndef ANDROID
	(void)signal(SIGINFO, progress);
#endif

	/*
	 * Cp has two distinct cases:
	 *
	 * cp [-R] source target
	 * cp [-R] source1 ... sourceN directory
	 *
	 * In both cases, source can be either a file or a directory.
	 *
	 * In (1), the target becomes a copy of the source. That is, if the
	 * source is a file, the target will be a file, and likewise for
	 * directories.
	 *
	 * In (2), the real target is not directory, but "directory/source".
	 */
	if (Pflag)
		r = lstat(to.p_path, &to_stat);
	else
		r = stat(to.p_path, &to_stat);
	if (r == -1 && errno != ENOENT) {
		LOGE("%s", to.p_path);
		return -1;
	}
	if (r == -1 || !S_ISDIR(to_stat.st_mode)) {
		/*
		 * Case (1).  Target is not a directory.
		 */
		if (argc > 1)
			cp_usage();
		/*
		 * Need to detect the case:
		 *	cp -R dir foo
		 * Where dir is a directory and foo does not exist, where
		 * we want pathname concatenations turned on but not for
		 * the initial mkdir().
		 */
		if (r == -1) {
			if (rflag || (Rflag && (Lflag || Hflag)))
				r = stat(*argv, &tmp_stat);
			else
				r = lstat(*argv, &tmp_stat);
			if (r == -1) {
				LOGE("%s", *argv);
				return -1;
			}

			if (S_ISDIR(tmp_stat.st_mode) && (Rflag || rflag))
				type = DIR_TO_DNE;
			else
				type = FILE_TO_FILE;
		} else
			type = FILE_TO_FILE;

		if (have_trailing_slash && type == FILE_TO_FILE) {
			if (r == -1)
				LOGE("directory %s does not exist",
				     to.p_path);
			else
				LOGE("%s is not a directory", to.p_path);
		}
	} else {
		/*
		 * Case (2).  Target is a directory.
		 */
		type = FILE_TO_DIR;
	}

	/*
	 * make "cp -rp src/ dst" behave like "cp -rp src dst" not
	 * like "cp -rp src/. dst"
	 */
	for (src = argv; *src; src++) {
		size_t len = strlen(*src);
		while (len-- > 1 && (*src)[len] == '/')
			(*src)[len] = '\0';
	}

	return copy(argv, type, fts_options) >= 0;
}

static int dnestack[MAXPATHLEN]; /* unlikely we'll have more nested dirs */
static ssize_t dnesp;
static void
pushdne(int dne)
{

	dnestack[dnesp++] = dne;
	assert(dnesp < MAXPATHLEN);
}

static int
popdne(void)
{
	int rv;

	rv = dnestack[--dnesp];
	assert(dnesp >= 0);
	return rv;
}

static int
copy(char *argv[], enum op type, int fts_options)
{
	struct stat to_stat;
	FTS *ftsp;
	FTSENT *curr;
	int base, dne, sval;
	int this_failed, any_failed;
	size_t nlen;
	char *p, *target_mid;

	base = 0;	/* XXX gcc -Wuninitialized (see comment below) */

	if ((ftsp = fts_open(argv, fts_options, NULL)) == NULL) {
		LOGE("%s", argv[0]);
		return -1;
	}
	for (any_failed = 0; (curr = fts_read(ftsp)) != NULL;) {
		this_failed = 0;
		switch (curr->fts_info) {
		case FTS_NS:
		case FTS_DNR:
		case FTS_ERR:
			LOGI("%s: %s", curr->fts_path,
					strerror(curr->fts_errno));
			this_failed = any_failed = 1;
			continue;
		case FTS_DC:			/* Warn, continue. */
			LOGI("%s: directory causes a cycle", curr->fts_path);
			this_failed = any_failed = 1;
			continue;
		}

		/*
		 * If we are in case (2) or (3) above, we need to append the
                 * source name to the target name.
                 */
		if (type != FILE_TO_FILE) {
			if ((curr->fts_namelen +
			    to.target_end - to.p_path + 1) > MAXPATHLEN) {
				LOGI("%s/%s: name too long (not copied)",
						to.p_path, curr->fts_name);
				this_failed = any_failed = 1;
				continue;
			}

			/*
			 * Need to remember the roots of traversals to create
			 * correct pathnames.  If there's a directory being
			 * copied to a non-existent directory, e.g.
			 *	cp -R a/dir noexist
			 * the resulting path name should be noexist/foo, not
			 * noexist/dir/foo (where foo is a file in dir), which
			 * is the case where the target exists.
			 *
			 * Also, check for "..".  This is for correct path
			 * concatentation for paths ending in "..", e.g.
			 *	cp -R .. /tmp
			 * Paths ending in ".." are changed to ".".  This is
			 * tricky, but seems the easiest way to fix the problem.
			 *
			 * XXX
			 * Since the first level MUST be FTS_ROOTLEVEL, base
			 * is always initialized.
			 */
			if (curr->fts_level == FTS_ROOTLEVEL) {
				if (type != DIR_TO_DNE) {
					p = strrchr(curr->fts_path, '/');
					base = (p == NULL) ? 0 :
					    (int)(p - curr->fts_path + 1);

					if (!strcmp(&curr->fts_path[base],
					    ".."))
						base += 1;
				} else
					base = curr->fts_pathlen;
			}

			p = &curr->fts_path[base];
			nlen = curr->fts_pathlen - base;
			target_mid = to.target_end;
			if (*p != '/' && target_mid[-1] != '/')
				*target_mid++ = '/';
			*target_mid = 0;

			if (target_mid - to.p_path + nlen >= PATH_MAX) {
				LOGI("%s%s: name too long (not copied)",
				    to.p_path, p);
				this_failed = any_failed = 1;
				continue;
			}
			(void)strncat(target_mid, p, nlen);
			to.p_end = target_mid + nlen;
			*to.p_end = 0;
			STRIP_TRAILING_SLASH(to);
		}

		sval = Pflag ? lstat(to.p_path, &to_stat) : stat(to.p_path, &to_stat);
		/* Not an error but need to remember it happened */
		if (sval == -1)
			dne = 1;
		else {
			if (to_stat.st_dev == curr->fts_statp->st_dev &&
			    to_stat.st_ino == curr->fts_statp->st_ino) {
				LOGI("%s and %s are identical (not copied).",
				    to.p_path, curr->fts_path);
				this_failed = any_failed = 1;
				if (S_ISDIR(curr->fts_statp->st_mode))
					(void)fts_set(ftsp, curr, FTS_SKIP);
				continue;
			}
			if (!S_ISDIR(curr->fts_statp->st_mode) &&
			    S_ISDIR(to_stat.st_mode)) {
		LOGI("cannot overwrite directory %s with non-directory %s",
				    to.p_path, curr->fts_path);
				this_failed = any_failed = 1;
				continue;
			}
			dne = 0;
		}

		switch (curr->fts_statp->st_mode & S_IFMT) {
		case S_IFLNK:
			/* Catch special case of a non dangling symlink */
			if((fts_options & FTS_LOGICAL) ||
			   ((fts_options & FTS_COMFOLLOW) && curr->fts_level == 0)) {
				if (copy_file(curr, dne))
					this_failed = any_failed = 1;
			} else {
				if (copy_link(curr, !dne))
					this_failed = any_failed = 1;
			}
			break;
		case S_IFDIR:
			if (!Rflag && !rflag) {
				if (curr->fts_info == FTS_D)
					LOGI("%s is a directory (not copied).",
					    curr->fts_path);
				(void)fts_set(ftsp, curr, FTS_SKIP);
				this_failed = any_failed = 1;
				break;
			}

                        /*
                         * Directories get noticed twice:
                         *  In the first pass, create it if needed.
                         *  In the second pass, after the children have been copied, set the permissions.
                         */
			if (curr->fts_info == FTS_D) /* First pass */
			{
				/*
				 * If the directory doesn't exist, create the new
				 * one with the from file mode plus owner RWX bits,
				 * modified by the umask.  Trade-off between being
				 * able to write the directory (if from directory is
				 * 555) and not causing a permissions race.  If the
				 * umask blocks owner writes, we fail..
				 */
				pushdne(dne);
				if (dne) {
					if (mkdir(to.p_path,
					    curr->fts_statp->st_mode | S_IRWXU) < 0) {
						LOGE("%s", to.p_path);
						return -1;
					}
				} else if (!S_ISDIR(to_stat.st_mode)) {
					errno = ENOTDIR;
					LOGE("%s", to.p_path);
					return -1;
				}
			}
			else if (curr->fts_info == FTS_DP) /* Second pass */
			{
	                        /*
				 * If not -p and directory didn't exist, set it to be
				 * the same as the from directory, umodified by the
				 * umask; arguably wrong, but it's been that way
				 * forever.
				 */
				if (pflag && setfile(curr->fts_statp, 0))
					this_failed = any_failed = 1;
				else if ((dne = popdne()))
					(void)chmod(to.p_path,
					    curr->fts_statp->st_mode);
			}
			else
			{
				LOGI("directory %s encountered when not expected.",
				    curr->fts_path);
				this_failed = any_failed = 1;
				break;
			}

			break;
		case S_IFBLK:
		case S_IFCHR:
			if (Rflag) {
				if (copy_special(curr->fts_statp, !dne))
					this_failed = any_failed = 1;
			} else
				if (copy_file(curr, dne))
					this_failed = any_failed = 1;
			break;
		case S_IFIFO:
			if (Rflag) {
				if (copy_fifo(curr->fts_statp, !dne))
					this_failed = any_failed = 1;
			} else
				if (copy_file(curr, dne))
					this_failed = any_failed = 1;
			break;
		default:
			if (copy_file(curr, dne))
				this_failed = any_failed = 1;
			break;
		}
		if (vflag && !this_failed)
			(void)printf("%s -> %s\n", curr->fts_path, to.p_path);
	}
	if (errno) {
		LOGE("fts_read");
		return -1;
	}
	(void)fts_close(ftsp);
	return (any_failed);
}
