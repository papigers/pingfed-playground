#!/usr/bin/env bash
### ====================================================================== ###
##                                                                          ##
## PingFederate Startup Script                                              ##
##                                                                          ##
### ====================================================================== ###

### $Id: run.sh,v 1.9.2.5 2004/01/01 01:20:38 starksm Exp $ ###

DIRNAME=`dirname "$0"`

cygwin=false;
sunos=false;
case "`uname`" in
    CYGWIN*)
        cygwin=true
        ;;

    SunOS*)
        sunos=true
        ;;
esac

# For Cygwin, ensure paths are in UNIX format before anything is touched
if $cygwin ; then
    [ -n "$PF_HOME" ] &&
        PF_HOME=`cygpath --unix "$PF_HOME"`
    [ -n "$JAVA_HOME" ] &&
        JAVA_HOME=`cygpath --unix "$JAVA_HOME"`
    [ -n "$JAVAC_JAR" ] &&
        JAVAC_JAR=`cygpath --unix "$JAVAC_JAR"`
fi

# Setup PF_HOME
if [ "x$PF_HOME" = "x" ]; then
    # get the full path (without any relative bits)
    PF_HOME=`cd "$DIRNAME/.."; pwd`
fi
export PF_HOME

if ! [ -n "$JAVA_HOME" ] || ! [ -x "$JAVA_HOME/bin/java" ];  then
    echo "No executable java found in JAVA_HOME for the user '$USER', please correct and start PingFederate again. Exiting."
    exit 1
fi

JAVA_FULL_VERSION=`"$JAVA_HOME/bin/java" -version 2>&1 | head -1 | cut -d '"' -f2`
JAVA_MINOR_VERSION=`/bin/echo ${JAVA_FULL_VERSION} | cut -d "." -f2`

if [ "$JAVA_MINOR_VERSION" -le "7" ]; then
    /bin/echo "PingFederate must be run using Java 1.8 or higher.  Exiting."
    exit 1
fi

# Create log folder
LOG_FOLDER="$PF_HOME/log"
if [ ! -d "$LOG_FOLDER" ]; then
  mkdir $LOG_FOLDER
fi
# Console output files.
STDOUT_FILE="$PF_HOME/log/stdout.log"
STDERR_FILE="$PF_HOME/log/stderr.log"
# Check for currently running instance of PingFederate
RUNFILE="$PF_HOME/bin/pingfederate.pid"
if $sunos ; then
    if [ ! -f "$RUNFILE" ] ; then
        touch "$RUNFILE"
        chmod 664 "$RUNFILE"
    fi
    # STDOUT
    if [ ! -f "$STDOUT_FILE" ]; then
        touch "$STDOUT_FILE"
        chmod 664 "$STDOUT_FILE"
    fi
    # STDERR
    if [ ! -f "$STDERR_FILE" ]; then
        touch "$STDERR_FILE"
        chmod 664 "$STDERR_FILE"
    fi
else
    if [ ! -e "$RUNFILE" ] ; then
        touch "$RUNFILE"
        chmod 664 "$RUNFILE"
    fi
    # STDOUT
    if [ ! -e "$STDOUT_FILE" ]; then
        touch "$STDOUT_FILE"
        chmod 664 "$STDOUT_FILE"
    fi
    # STDERR
    if [ ! -e "$STDERR_FILE" ]; then
        touch "$STDERR_FILE"
        chmod 664 "$STDERR_FILE"
    fi
fi
CURRENT_PID=`cat "$RUNFILE"`
if [ -n "$CURRENT_PID" ] ; then
    kill -0 $CURRENT_PID 2>/dev/null
    if [ $? -eq 0 ] ; then
        /bin/echo "Another PingFederate instance with pid $CURRENT_PID is already running. Exiting."
        exit 1
    fi
fi

# Execute the run.sh script
/bin/echo "PingFederate is starting ..."
bash "$PF_HOME/bin/run.sh" 1>$STDOUT_FILE 2>$STDERR_FILE &
status=$?
exit $status