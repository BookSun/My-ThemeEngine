#include <jni.h>
#include <dirent.h>
#include <errno.h>
#include <math.h>
#include <string.h>
#include <stdio.h>
#include <unistd.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/limits.h>
#include <android/log.h>

#define LOG_TAG "Shell"
#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

jboolean native_mkdirs(JNIEnv* env, jclass clazz, jstring name) {
    if (!name)
        return JNI_FALSE;
    const char *path = (*env)->GetStringUTFChars(env, name, NULL);
    LOGI("native_mkdirs %s", path);
    return mkdir_recursive(path) >= 0;
}

jboolean native_remove(JNIEnv* env, jclass clazz, jstring name) {
    if (!name)
        return JNI_FALSE;
    const char *path = (*env)->GetStringUTFChars(env, name, NULL);
    LOGI("native_remove %s", path);
    return unlink_recursive(path) >= 0;
}

jboolean native_remove_cmd(JNIEnv* env, jclass clazz, jstring name) {
    if (!name)
        return JNI_FALSE;
    const char *path = (*env)->GetStringUTFChars(env, name, NULL);
    return run_command("rm -r %s", path) >= 0;
}

jboolean native_chown(JNIEnv* env, jclass clazz, jstring name, jint uid, jint gid, jboolean recursive) {
    if (!name)
        return JNI_FALSE;
    const char *path = (*env)->GetStringUTFChars(env, name, NULL);
    LOGI("native_chown %s %d %d", path, uid, gid);
    if(chown(path, uid, gid) < 0)
        return -1;
    if(recursive) {
        return chown_recursive(path, (uid_t)uid, (uid_t)gid) >= 0;
    }
    return 0;
}

jboolean native_chmod(JNIEnv* env, jclass clazz, jstring name, jint mode, jboolean recursive) {
    if (!name)
        return JNI_FALSE;
    const char *path = (*env)->GetStringUTFChars(env, name, NULL);
    LOGI("native_chmod %s %d", path, mode);
    if(chmod(path, mode) < 0)
        return -1;
    if(recursive) {
        return chmod_recursive(path, (int)mode) >= 0;
    }
    return 0;
}

jboolean native_link(JNIEnv* env, jclass clazz, jstring srcStr, jstring destStr) {
    if (!srcStr && !destStr)
        return JNI_FALSE;
    const char *src = (*env)->GetStringUTFChars(env, srcStr, NULL);
    const char *dest = (*env)->GetStringUTFChars(env, destStr, NULL);
    LOGI("native_link %s %s", src, dest);
    return symlink(src, dest) >= 0;
}

jboolean native_move(JNIEnv* env, jclass clazz, jstring srcStr, jstring destStr) {
    if (!srcStr && !destStr)
        return JNI_FALSE;
    const char *src = (*env)->GetStringUTFChars(env, srcStr, NULL);
    const char *dest = (*env)->GetStringUTFChars(env, destStr, NULL);
    LOGI("native_move %s %s", src, dest);
    return mv_main(src, dest) >= 0;
}

jboolean native_copy_cmd(JNIEnv* env, jclass clazz, jstring srcStr, jstring destStr) {
    if (!srcStr && !destStr)
        return JNI_FALSE;
    const char *src = (*env)->GetStringUTFChars(env, srcStr, NULL);
    const char *dest = (*env)->GetStringUTFChars(env, destStr, NULL);
    const char *exec = "cp -R %s %s";
    LOGI("native_copy_cmd %s %s", src, dest);
    char *cmd = malloc(strlen(exec) + strlen(src) + strlen(dest));
    sprintf(cmd, exec, src, dest);
    int ret = system(cmd) >= 0;
    free(cmd);
    return ret;
}

jboolean native_copy(JNIEnv* env, jclass clazz, jstring srcStr, jstring destStr) {
    if (!srcStr && !destStr)
        return JNI_FALSE;
    const char *src = (*env)->GetStringUTFChars(env, srcStr, NULL);
    const char *dest = (*env)->GetStringUTFChars(env, destStr, NULL);
    LOGI("native_copy %s %s", src, dest);
    const char *args[] = {src, dest};
    return cp_main(2, args);
}

jboolean native_run(JNIEnv* env, jclass clazz, jstring cmdStr) {
    if (!cmdStr)
        return JNI_FALSE;
    const char *cmd = (*env)->GetStringUTFChars(env, cmdStr, NULL);
    LOGI("native_run %s", cmd);
    return system(cmd) >= 0;
}

jboolean native_run_shell(JNIEnv* env, jclass clazz, jstring cmdStr) {
    if (!cmdStr)
        return JNI_FALSE;
    const char *cmd = (*env)->GetStringUTFChars(env, cmdStr, NULL);
    return run_command("sh -c \"%s\"", cmd) >= 0;
}

jboolean native_write(JNIEnv* env, jclass clazz, jstring pathStr, jstring strStr) {
	if (!pathStr)
		return JNI_FALSE;
	const char *path = (*env)->GetStringUTFChars(env, pathStr, NULL);
	LOGI("native_write %s", path);
	FILE *fp = fopen(path, "w");
	if (fp != NULL) {
		if(strStr) {
			const char *str = (*env)->GetStringUTFChars(env, strStr, NULL);
			fputs(str, fp);
		}
		fclose(fp);
		return JNI_TRUE;
	}
	return JNI_FALSE;
}

int run_command(const char *exec, const char *arg) {
    char *cmd = malloc(strlen(exec) + strlen(arg));
    sprintf(cmd, exec, arg);
    LOGI("native_run %s", cmd);
    int ret = system(cmd) >= 0;
    free(cmd);
    return ret;
}

int chmod_recursive(const char* path, int mode)
{
    struct dirent *dp;
    DIR *dir = opendir(path);
    if (dir == NULL) {
        // not a directory, carry on
        return -1;
    }

    char *subpath = malloc(sizeof(char)*PATH_MAX);
    int pathlen = strlen(path);

    while ((dp = readdir(dir)) != NULL) {
        if (strcmp(dp->d_name, ".") == 0 ||
            strcmp(dp->d_name, "..") == 0) continue;

        if (strlen(dp->d_name) + pathlen + 2/*NUL and slash*/ > PATH_MAX) {
            return -1;
        }

        strcpy(subpath, path);
        strcat(subpath, "/");
        strcat(subpath, dp->d_name);

        if (chmod(subpath, mode) < 0) {
            return -1;
        }
        chmod_recursive(subpath, mode);
    }
    free(subpath);
    closedir(dir);
    return 0;
}

int chown_recursive(const char* path, uid_t uid, gid_t gid)
{
    struct dirent *dp;
    DIR *dir = opendir(path);
    if (dir == NULL) {
        // not a directory, carry on
        return -1;
    }

    char *subpath = malloc(sizeof(char)*PATH_MAX);
    int pathlen = strlen(path);

    while ((dp = readdir(dir)) != NULL) {
        if (strcmp(dp->d_name, ".") == 0 ||
            strcmp(dp->d_name, "..") == 0) continue;

        if (strlen(dp->d_name) + pathlen + 2/*NUL and slash*/ > PATH_MAX) {
            return -1;
        }

        strcpy(subpath, path);
        strcat(subpath, "/");
        strcat(subpath, dp->d_name);

        if(chown(subpath, uid, gid) < 0) {
            return -1;
        }
        chown_recursive(subpath, uid, gid);
    }
    free(subpath);
    closedir(dir);
    return 0;
}

int mkdir_recursive(const char* path)
{
    int ret;
    char currpath[PATH_MAX], *pathpiece;
    struct stat st;

    // reset path
    strcpy(currpath, "");
    // create the pieces of the path along the way
    pathpiece = strtok((char*)path, "/");
    if(path[0] == '/') {
        // prepend / if needed
        strcat(currpath, "/");
    }
    while(pathpiece != NULL) {
        if(strlen(currpath) + strlen(pathpiece) + 2/*NUL and slash*/ > PATH_MAX) {
            return -1;
        }
        strcat(currpath, pathpiece);
        strcat(currpath, "/");
        if(stat(currpath, &st) != 0) {
            if(mkdir(currpath, 0755) != 0)
                return -1;
        }
        pathpiece = strtok(NULL, "/");
    }
    return 0;
}

int unlink_recursive(const char* name)
{
    struct stat st;
    DIR *dir;
    struct dirent *de;
    int fail = 0;

    /* is it a file or directory? */
    if (lstat(name, &st) < 0)
        return errno == ENOENT ? 0 : -1;

    /* a file, so unlink it */
    if (!S_ISDIR(st.st_mode))
        return unlink(name);

    /* a directory, so open handle */
    dir = opendir(name);
    if (dir == NULL)
        return -1;

    /* recurse over components */
    errno = 0;
    while ((de = readdir(dir)) != NULL) {
        char dn[PATH_MAX];
        if (!strcmp(de->d_name, "..") || !strcmp(de->d_name, "."))
            continue;
        sprintf(dn, "%s/%s", name, de->d_name);
        if (unlink_recursive(dn) < 0) {
            fail = 1;
            break;
        }
        errno = 0;
    }
    /* in case readdir or unlink_recursive failed */
    if (fail || errno < 0) {
        int save = errno;
        closedir(dir);
        errno = save;
        return -1;
    }

    /* close directory handle */
    if (closedir(dir) < 0)
        return -1;

    /* delete target directory */
    return rmdir(name);
}

int mv_main(const char *src, const char *dest) {
    struct stat st;
    if (stat(dest, &st)) {
        /* an error, unless the destination was missing */
        if (errno != ENOENT) {
        	LOGE("failed on %s - %s", dest, strerror(errno));
            return -1;
        }
        st.st_mode = 0;
    }
    char fullDest[PATH_MAX + 1 + PATH_MAX + 1];
    /* assume we build "dest/source", and let rename() fail on pathsize */
    if (strlen(dest) + 1 + strlen(src) + 1 > sizeof(fullDest)) {
    	LOGE("path too long");
        return -1;
    }
    strcpy(fullDest, dest);

    /* if destination is a directory, concat the src file name */
    if (S_ISDIR(st.st_mode)) {
        const char *fileName = strrchr(src, '/');
        if (fullDest[strlen(fullDest)-1] != '/') {
            strcat(fullDest, "/");
        }
        strcat(fullDest, fileName ? fileName + 1 : src);
    }

    /* attempt to move it */
	LOGI("move %s to %s", src, fullDest);
    if (rename(src, fullDest)) {
    	LOGE("failed on '%s' - %s", src, strerror(errno));
        return -1;
    }

    return 0;
}

static JNINativeMethod gNativeMethods[] = {
    { "native_mkdirs", "(Ljava/lang/String;)Z", (void*) native_mkdirs },
    { "native_remove", "(Ljava/lang/String;)Z", (void*) native_remove_cmd },
    { "native_chown", "(Ljava/lang/String;IIZ)Z", (void*) native_chown },
    { "native_chmod", "(Ljava/lang/String;IZ)Z", (void*) native_chmod },
    { "native_link", "(Ljava/lang/String;Ljava/lang/String;)Z", (void*) native_link },
    { "native_move", "(Ljava/lang/String;Ljava/lang/String;)Z", (void*) native_move },
    { "native_copy", "(Ljava/lang/String;Ljava/lang/String;)Z", (void*) native_copy },
    { "native_run", "(Ljava/lang/String;)Z", (void*) native_run },
    { "native_run_shell", "(Ljava/lang/String;)Z", (void*) native_run_shell },
    { "native_write", "(Ljava/lang/String;Ljava/lang/String;)Z", (void*) native_write },
};

static int registerNativeMethods(JNIEnv* env, const char* className, JNINativeMethod* gMethods, int numMethods) {
    jclass clazz;
    clazz = (*env)->FindClass(env, className);
    if (clazz == NULL) {
        return JNI_FALSE;
    }
    if ((*env)->RegisterNatives(env, clazz, gMethods, numMethods) < 0) {
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    JNIEnv* env = NULL;
    if ((*vm)->GetEnv(vm, (void**) &env, JNI_VERSION_1_4) != JNI_OK) {
        return -1;
    }
    if(!registerNativeMethods(env, "com/lewa/support/util/Shell", gNativeMethods, sizeof(gNativeMethods) / sizeof(gNativeMethods[0]))) {
        return -1;
    }
    return JNI_VERSION_1_4;
}
